package com.blakelabs.guitartuner.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.math.max
import kotlin.math.sqrt

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
    private var recorder: AudioRecord? = null
    private var worker: Thread? = null

    @SuppressLint("MissingPermission")
    fun start() {
        if (!running.compareAndSet(false, true)) return

        var config: RecorderConfig? = null
        for (sampleRate in SAMPLE_RATES) {
            config = buildRecorder(sampleRate)
            if (config != null) break
        }
        if (config == null) {
            running.set(false)
            onError("Could not open the microphone on a supported sample rate.")
            return
        }

        val activeConfig = config
        recorder = activeConfig.recorder
        val detector = PitchDetector(activeConfig.sampleRate)
        val analysisBuffer = ShortArray(ANALYSIS_SIZE)
        var filled = 0

        try {
            activeConfig.recorder.startRecording()
            if (activeConfig.recorder.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                throw IllegalStateException("Android opened the microphone but did not start recording.")
            }
            Log.i(
                TAG,
                "Audio capture started source=${sourceName(activeConfig.source)} " +
                    "sampleRate=${activeConfig.sampleRate} analysis=$ANALYSIS_SIZE hop=$HOP_SIZE",
            )
        } catch (error: Exception) {
            activeConfig.recorder.release()
            recorder = null
            running.set(false)
            Log.e(TAG, "Could not start microphone capture", error)
            onError(error.message ?: "Could not start microphone capture.")
            return
        }

        worker = thread(name = "blake-tuner-audio", isDaemon = true) {
            val readBuffer = ShortArray(HOP_SIZE)

            try {
                while (running.get()) {
                    val count = activeConfig.recorder.read(
                        readBuffer,
                        0,
                        readBuffer.size,
                        AudioRecord.READ_BLOCKING,
                    )

                    if (count < 0) {
                        throw IllegalStateException("Microphone read failed with AudioRecord code $count.")
                    }
                    if (count == 0) continue

                    // Raw level is intentionally independent from pitch detection. A moving SIGNAL
                    // meter proves that the phone is actually delivering PCM even before YIN locks.
                    onSignal(calculateRms(readBuffer, count))

                    if (filled < ANALYSIS_SIZE) {
                        val copyCount = count.coerceAtMost(ANALYSIS_SIZE - filled)
                        readBuffer.copyInto(analysisBuffer, filled, 0, copyCount)
                        filled += copyCount
                    } else {
                        val shift = count.coerceAtMost(ANALYSIS_SIZE)
                        analysisBuffer.copyInto(
                            destination = analysisBuffer,
                            destinationOffset = 0,
                            startIndex = shift,
                            endIndex = ANALYSIS_SIZE,
                        )
                        readBuffer.copyInto(
                            destination = analysisBuffer,
                            destinationOffset = ANALYSIS_SIZE - shift,
                            startIndex = 0,
                            endIndex = shift,
                        )
                    }

                    if (filled == ANALYSIS_SIZE) {
                        onPitch(detector.detect(analysisBuffer))
                    }
                }
            } catch (error: Throwable) {
                Log.e(TAG, "Audio capture loop failed", error)
                if (running.get()) onError(error.message ?: "Audio capture failed.")
            }
        }
    }

    fun stop() {
        if (!running.compareAndSet(true, false)) return

        try {
            recorder?.stop()
        } catch (_: IllegalStateException) {
            // Recorder may already be stopped by the OS. Cleanup still continues.
        }

        worker?.join(STOP_JOIN_TIMEOUT_MS)
        worker = null
        recorder?.release()
        recorder = null
        Log.i(TAG, "Audio capture stopped")
    }

    @SuppressLint("MissingPermission")
    private fun buildRecorder(sampleRate: Int): RecorderConfig? {
        val minBufferBytes = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        if (minBufferBytes <= 0) return null

        val bufferBytes = max(minBufferBytes * 2, ANALYSIS_SIZE * 4)

        // MIC first is intentional. Several Android vendors initialize UNPROCESSED but return
        // silence or an extremely attenuated stream. Regular MIC is the safest physical-device path.
        val sources = intArrayOf(
            MediaRecorder.AudioSource.MIC,
            MediaRecorder.AudioSource.UNPROCESSED,
            MediaRecorder.AudioSource.DEFAULT,
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
        )

        for (source in sources) {
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

    private fun calculateRms(samples: ShortArray, count: Int): Float {
        if (count <= 0) return 0f
        var sum = 0.0
        for (index in 0 until count) {
            val normalized = samples[index].toDouble() / Short.MAX_VALUE
            sum += normalized * normalized
        }
        return sqrt(sum / count).toFloat()
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
        const val ANALYSIS_SIZE = 8192
        const val HOP_SIZE = 2048
        const val STOP_JOIN_TIMEOUT_MS = 300L
    }
}
