package com.noteandrecall.util

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicBoolean

object AudioRecorder {
    private const val TAG = "AudioRecorder"
    private const val SAMPLE_RATE = 44100
    private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
    private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT

    private val isRecording = AtomicBoolean(false)
    private var lastWavFile: File? = null

    data class RecordingResult(val filePath: String, val durationMs: Long)

    fun stop() {
        isRecording.set(false)
    }

    /**
     * Record audio until [stop] is called.
     * Writes WAV to [audioDir]/recording.wav and returns the result.
     * Blocks the calling coroutine (IO dispatcher) until stopped.
     */
    suspend fun record(audioDir: File): RecordingResult = withContext(Dispatchers.IO) {
        isRecording.set(true)

        val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            throw Exception("Invalid buffer size: $bufferSize")
        }

        // Use 2× buffer for smoother recording
        val audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            bufferSize * 4
        )

        if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
            throw Exception("AudioRecord failed to initialize")
        }

        val buffer = ShortArray(bufferSize)
        val pcmData = mutableListOf<Short>()
        val startTime = System.currentTimeMillis()

        audioRecord.startRecording()

        while (isRecording.get()) {
            val bytesRead = audioRecord.read(buffer, 0, buffer.size)
            if (bytesRead > 0) {
                pcmData.addAll(buffer.take(bytesRead))
            }
        }

        audioRecord.stop()
        audioRecord.release()

        val durationMs = System.currentTimeMillis() - startTime
        val wavFile = File(audioDir, "recording.wav")
        writeWavFile(wavFile, pcmData.toShortArray(), SAMPLE_RATE)
        lastWavFile = wavFile

        Log.d(TAG, "Recording saved: ${wavFile.absolutePath}, duration: ${durationMs}ms")
        RecordingResult(wavFile.absolutePath, durationMs)
    }

    fun getLastRecording(): File? = lastWavFile

    private fun writeWavFile(file: File, data: ShortArray, sampleRate: Int) {
        FileOutputStream(file).use { out ->
            val dataSize = data.size * 2
            val fileSize = 44 + dataSize

            writeBytes(out, "RIFF".toByteArray())
            writeInt(out, fileSize - 8)
            writeBytes(out, "WAVE".toByteArray())
            writeBytes(out, "fmt ".toByteArray())
            writeInt(out, 16)
            writeShort(out, 1)
            writeShort(out, 1)
            writeInt(out, sampleRate)
            writeInt(out, sampleRate * 2)
            writeShort(out, 2)
            writeShort(out, 16)
            writeBytes(out, "data".toByteArray())
            writeInt(out, dataSize)

            val byteBuffer = ByteArray(data.size * 2)
            for (i in data.indices) {
                val v = data[i].toInt()
                byteBuffer[i * 2] = (v and 0xFF).toByte()
                byteBuffer[i * 2 + 1] = ((v shr 8) and 0xFF).toByte()
            }
            out.write(byteBuffer)
        }
    }

    private fun writeBytes(out: FileOutputStream, data: ByteArray) = out.write(data)

    private fun writeInt(out: FileOutputStream, value: Int) {
        out.write((value and 0xFF))
        out.write((value shr 8) and 0xFF)
        out.write((value shr 16) and 0xFF)
        out.write((value shr 24) and 0xFF)
    }

    private fun writeShort(out: FileOutputStream, value: Int) {
        out.write((value and 0xFF))
        out.write((value shr 8) and 0xFF)
    }
}
