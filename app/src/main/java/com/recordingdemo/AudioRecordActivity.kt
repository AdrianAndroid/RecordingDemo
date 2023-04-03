package com.recordingdemo

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat


class AudioRecordActivity : AppCompatActivity() {
    private val SAMPLE_RATE = 44100
    private val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
    private val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT

    private var recorder: AudioRecord? = null
    private var recordingThread: Thread? = null
    private var isRecording = false

    private var dbTextView: TextView? = null

    private val handler = Handler(Looper.getMainLooper())

    private val updateDbRunnable = object : Runnable {
        override fun run() {
            if (isRecording) {
                val db = getDb()
                dbTextView?.text = String.format("%.2f dB", db)
                handler.postDelayed(this, 100)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio_record)
        dbTextView = findViewById(R.id.dbTextView)
        val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        recorder = AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, bufferSize)
        recordingThread = Thread {
            recorder?.startRecording()
//            while(isRecording) {
                // do something
//            }
        }
        isRecording = true
        recordingThread?.start()
        handler.post(updateDbRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        isRecording = false
        recordingThread?.interrupt()
        recorder?.stop()
        recorder?.release()
        recorder = null
        recordingThread = null
    }

    private fun getDb(): Double {
        val buffer = ShortArray(1024)
        val read: Int = recorder?.read(buffer, 0, buffer.size) ?: 0
        var sum: Double = 0.0
        for (i in 0 until read) {
            sum += buffer[i] * buffer[i]
        }
        val rms = Math.sqrt(sum / read)
        val db = 20 * Math.log10(rms / 32767.0)
        return db
    }
}