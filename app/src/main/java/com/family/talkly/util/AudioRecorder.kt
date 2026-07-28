package com.family.talkly.util

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File

class AudioRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    fun startRecording(): File? {
        val file = File(context.cacheDir, "voice_note_${System.currentTimeMillis()}.m4a")
        outputFile = file

        return try {
            val recordingContext = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.createAttributionContext("default")
            } else {
                context
            }
            val mr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(recordingContext)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            recorder = mr
            mr.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            Log.d("AudioRecorder", "Recording started successfully to ${file.absolutePath}")
            file
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Failed to start recording: ${e.localizedMessage}", e)
            recorder?.release()
            recorder = null
            outputFile?.delete()
            outputFile = null
            null
        }
    }

    fun stopRecording(): File? {
        return try {
            recorder?.apply {
                stop()
                release()
            }
            recorder = null
            Log.d("AudioRecorder", "Recording stopped. File size: ${outputFile?.length() ?: 0} bytes")
            outputFile
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Failed to stop recording cleanly: ${e.localizedMessage}", e)
            recorder?.release()
            recorder = null
            outputFile
        }
    }

    fun cancelRecording() {
        try {
            recorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            Log.w("AudioRecorder", "Error releasing recorder on cancel: ${e.localizedMessage}")
        } finally {
            recorder = null
            outputFile?.delete()
            outputFile = null
        }
    }
}
