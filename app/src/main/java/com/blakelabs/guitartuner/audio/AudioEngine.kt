package com.blakelabs.guitartuner.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Owns the complete lifecycle of one microphone capture session.
 *
 * AudioRecord is vendor-sensitive and blocking reads do not fail uniformly across Android builds.
 * The engine therefore uses atomic ownership, idempotent start/stop operations and a single cleanup
 * path so an unexpected capture failure cannot leave the microphone allocated in the background.
 */
class AudioEngine(
    private val onPitch: (PitchDetector.Result?) -> Unit,
    private val onSignal: (Float) -> Unit,
    private val onError: (String) -> Unit,
) {
    private data class RecorderConfig(
        val recorder: AudioRecord,
        val sampleRate: Int,
        val source: Int,
    )

    private val running = AtomicBoolean(false)
    private val recorder = AtomicReference<AudioRecord?>(null)
    private val worker = AtomicReference<Thread?>(null)

    @SuppressLint("MissingPermission")
    @Synchronized
    fun start() {
        if (!running.compareAndSet(false, true)) return

        val config = try {
            selectRecorder()
        } catch (error: Exception) {
            failStart("Could not configure microphone capture.", error)
            return
        }

        if (config == null) {
            failStart("Could not open the microphone on a supported sample rate.")
            return
        }

        val activeRecorder = config.recorder
        recorder.set(activeRecorder)

        try {
            activeRecorder.startRecording()
            if (activeRecorder.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                throw IllegalStateException("Android opened the microphone but did not start recording.")
            }
        } catch (error: Exception) {
            failStart("Could not start microphone capture.", error, activeRecorder)
            return
        }

        Log.i(
            TAG,
            "Audio capture started source=${sourceName(config.source)} " +
                "sampleRate=${config.sampleRate} analysis=$ANALYSIS_SIZE hop=$HOP_SIZE",
        )

        try {
            val captureThread = thread(
                start = false,
                name = "blake-tuner-audio",
                isDaemon = true,
            ) {
                captureLoop(config)
            }
            worker.set(captureThread)
            captureThread.start()
        } catch (error: Exception) {
            worker.set(null)
            failStart("Could not create the audio processing thread.", error, activeRecorder)
        }
    }

    @Synchronized
    fun stop() {
        val wasRunning = running.getAndSet(false)
        val activeRecorder = recorder.get()
        stopRecorder(activeRecorder)

        val activeWorker = worker.get()
        if (activeWorker != null && activeWorker !== Thread.currentThread()) {
            try {
                activeWorker.join(STOP_JOIN_TIMEOUT_MS)
            } catch (error: InterruptedException) {
                Thread.currentThread().interrupt()
                Log.w(TAG, "Interrupted while stopping audio capture", error)
            }
        }

        worker.compareAndSet(activeWorker, null)
        releaseRecorder(activeRecorder)
        if (wasRunning || activeRecorder != null) Log.i(TAG, "Audio capture stopped")
    }

    private fun captureLoop(config: RecorderConfig) {
        val activeRecorder = config.recorder
        val detector = PitchDetector(config.sampleRate)
        val readBuffer = ShortArray(HOP_SIZE)
        val analysisBuffer = ShortArray(ANALYSIS_SIZE)
        var filled = 0
        var failure: Exception? = null

        try {
            while (running.get()) {
                val count = activeRecorder.read(
                    readBuffer,
                    0,
                    readBuffer.size,
                    AudioRecord.READ_BLOCKING,
                )

                if (count < 0) {
                    throw IllegalStateException("Microphone read failed with AudioRecord code $count.")
                }
                if (count == 0) continue

                onSignal(calculateRms(readBuffer, count))
                filled = appendSamples(
                    destination = analysisBuffer,
                    filled = filled,
                    source = readBuffer,
                    count = count,
                )

                if (filled == ANALYSIS_SIZE) {
                    onPitch(detector.detect(analysisBuffer))
                }
            }
        } catch (error: Exception) {
            failure = error
            if (running.get()) {
                Log.e(TAG, "Audio capture loop failed", error)
            } else {
                Log.d(TAG, "Audio capture loop stopped", error)
            }
        } finally {
            val failedWhileRunning = running.getAndSet(false) && failure != null
            releaseRecorder(activeRecorder)
            worker.compareAndSet(Thread.currentThread(), null)

            if (failedWhileRunning) {
                onError("Microphone capture stopped unexpectedly. Toggle MIC to retry.")
            }
        }
    }

    /**
     * Appends one read into a fixed rolling analysis window.
     *
     * The current hop divides the window exactly, but this implementation also preserves data if a
     * vendor returns an irregular read length.
     */
    private fun appendSamples(
        destination: ShortArray,
        filled: Int,
        source: ShortArray,
        count: Int,
    ): Int {
        val safeCount = count.coerceIn(0, source.size)
        if (safeCount == 0) return filled

        if (safeCount >= destination.size) {
            source.copyInto(
                destination = destination,
                destinationOffset = 0,
                startIndex = safeCount - destination.size,
                endIndex = safeCount,
            )
            return destination.size
        }

        val currentSize = filled.coerceIn(0, destination.size)
        val overflow = (currentSize + safeCount - destination.size).coerceAtLeast(0)
        if (overflow > 0) {
            destination.copyInto(
                destination = destination,
                destinationOffset = 0,
                startIndex = overflow,
                endIndex = currentSize,
            )
        }

        val retained = currentSize - overflow
        source.copyInto(
            destination = destination,
            destinationOffset = retained,
            startIndex = 0,
            endIndex = safeCount,
        )
        return (retained + safeCount).coerceAtMost(destination.size)
    }

    private fun failStart(
        userMessage: String,
        error: Exception? = null,
        activeRecorder: AudioRecord? = recorder.get(),
    ) {
        running.set(false)
        if (error == null) {
            Log.e(TAG, userMessage)
        } else {
            Log.e(TAG, userMessage, error)
        }
        releaseRecorder(activeRecorder)
        onError(userMessage)
    }

    @SuppressLint("MissingPermission")
    private fun selectRecorder(): RecorderConfig? {
        for (sampleRate in SAMPLE_RATES) {
            val config = buildRecorder(sampleRate)
            if (config != null) return config
        }
        return null
    }

    @SuppressLint("MissingPermission")
    private fun buildRecorder(sampleRate: Int): RecorderConfig? {
        val minBufferBytes = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        if (minBufferBytes <= 0) return null

        val bufferBytes = max(minBufferBytes * 2, ANALYSIS_SIZE * BYTES_PER_SAMPLE)

        // MIC first is intentional. Some vendors initialize UNPROCESSED but return silence or a
        // severely attenuated stream. The remaining sources are compatibility fallbacks.
        for (source in AUDIO_SOURCES) {
            val candidate = createRecorder(source, sampleRate, bufferBytes) ?: continue
            if (candidate.state == AudioRecord.STATE_INITIALIZED) {
                Log.i(TAG, "AudioRecord initialized source=${sourceName(source)} sampleRate=$sampleRate")
                return RecorderConfig(candidate, sampleRate, source)
            }
            candidate.release()
        }

        return null
    }

    @SuppressLint("MissingPermission")
    private fun createRecorder(source: Int, sampleRate: Int, bufferBytes: Int): AudioRecord? = try {
        AudioRecord.Builder()
            .setAudioSource(source)
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                    .build(),
            )
            .setBufferSizeInBytes(bufferBytes)
            .build()
    } catch (error: Exception) {
        Log.w(TAG, "AudioRecord rejected source=${sourceName(source)} sampleRate=$sampleRate", error)
        null
    }

    private fun stopRecorder(activeRecorder: AudioRecord?) {
        if (activeRecorder == null) return
        try {
            if (activeRecorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                activeRecorder.stop()
            }
        } catch (error: IllegalStateException) {
            Log.d(TAG, "AudioRecord was already stopped", error)
        }
    }

    private fun releaseRecorder(activeRecorder: AudioRecord?) {
        if (activeRecorder == null || !recorder.compareAndSet(activeRecorder, null)) return
        stopRecorder(activeRecorder)
        activeRecorder.release()
    }

    private fun calculateRms(samples: ShortArray, count: Int): Float {
        if (count <= 0) return 0f
        var sum = 0.0
        for (index in 0 until count.coerceAtMost(samples.size)) {
            val normalized = samples[index].toDouble() / Short.MAX_VALUE
            sum += normalized * normalized
        }
        return sqrt(sum / count.coerceAtMost(samples.size)).toFloat()
    }

    private fun sourceName(source: Int): String = when (source) {
        MediaRecorder.AudioSource.MIC -> "MIC"
        MediaRecorder.AudioSource.UNPROCESSED -> "UNPROCESSED"
        MediaRecorder.AudioSource.VOICE_RECOGNITION -> "VOICE_RECOGNITION"
        MediaRecorder.AudioSource.DEFAULT -> "DEFAULT"
        else -> source.toString()
    }

    private companion object {
        const val TAG = "BlakeTunerAudio"
        val SAMPLE_RATES = intArrayOf(48_000, 44_100)
        val AUDIO_SOURCES = intArrayOf(
            MediaRecorder.AudioSource.MIC,
            MediaRecorder.AudioSource.UNPROCESSED,
            MediaRecorder.AudioSource.DEFAULT,
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
        )
        const val ANALYSIS_SIZE = 4096
        const val HOP_SIZE = 2048
        const val BYTES_PER_SAMPLE = 2
        const val STOP_JOIN_TIMEOUT_MS = 500L
    }
}
