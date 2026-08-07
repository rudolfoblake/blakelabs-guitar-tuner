package com.blakelabs.guitartuner.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.math.max

class AudioEngine(
    private val onPitch: (PitchDetector.Result?) -> Unit,
    private val onError: (String) -> Unit,
) {
    private data class RecorderConfig(
        val recorder: AudioRecord,
        val sampleRate: Int,
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
        } catch (error: Exception) {
            activeConfig.recorder.release()
            recorder = null
            running.set(false)
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
                    if (count <= 0) continue

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
        val sources = intArrayOf(
            MediaRecorder.AudioSource.UNPROCESSED,
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            MediaRecorder.AudioSource.DEFAULT,
        )

        for (source in sources) {
            val candidate = createRecorder(source, sampleRate, bufferBytes) ?: continue
            if (candidate.state == AudioRecord.STATE_INITIALIZED) {
                return RecorderConfig(candidate, sampleRate)
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
    } catch (_: Exception) {
        null
    }

    private companion object {
        val SAMPLE_RATES = intArrayOf(48_000, 44_100)
        const val ANALYSIS_SIZE = 4096
        const val HOP_SIZE = 2048
        const val STOP_JOIN_TIMEOUT_MS = 300L
    }
}
