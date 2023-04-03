package com.recordingdemo

import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import org.w3c.dom.Text
import kotlin.math.log10

class Db2Activity : AppCompatActivity() {

    private val REQUEST_RECORD_AUDIO_PERMISSION = 200
    private var permissionToRecordAccepted = false
    private var recorder: MediaRecorder? = null
    private var recordingThread: Thread? = null
    private var isRecording = false

    private var maxDbTextView: TextView? = null
    private var minDbTextView: TextView? = null
    private var avgDbTextView: TextView? = null

    private var maxDb = Double.MIN_VALUE
    private var minDb = Double.MAX_VALUE
    private var totalDb: Double  = 0.0
    private var count = 0

    private val handler = Handler(Looper.getMainLooper())
    private val timerRunnable = object : Runnable {
        override fun run() {
            if (isRecording) {
                val avgDb = totalDb / count
                val max = String.format("Max dB: %.2f", maxDb)
                val min = String.format("Min dB: %.2f", minDb)
                val avg = String.format("Avg dB: %.2f", avgDb)
                maxDbTextView?.text = max
                minDbTextView?.text = min
                avgDbTextView?.text = avg
                val sb = StringBuilder()
                sb.append("max:").append(max).append(",")
                sb.append("min:").append(min).append(",")
                sb.append("avg:").append(avg)
                Log.i("Db2Activity", sb.toString())
                handler.postDelayed(this, 1000)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_db2)
        maxDbTextView = findViewById(R.id.mMaxDbTextView)
        minDbTextView = findViewById(R.id.mMinDbTextView)
        avgDbTextView = findViewById(R.id.mAvgDbTextView)

        ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.RECORD_AUDIO), REQUEST_RECORD_AUDIO_PERMISSION)

        findViewById<View>(R.id.btnStartRecord).setOnClickListener {
            startRecording()
        }
    }

    private fun startRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            recorder = MediaRecorder(this)
        } else {
            recorder = MediaRecorder()
        }
        recorder ?: return
        recorder?.setAudioSource(MediaRecorder.AudioSource.MIC)
        recorder?.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
        recorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
        recorder?.setOutputFile("/dev/null")

        kotlin.runCatching {
            recorder?.prepare()
        }.onFailure {
            it.printStackTrace()
        }

        recorder?.start()
        isRecording = true

        recordingThread = Thread{
            while (isRecording) {
                val amplitude = recorder?.maxAmplitude ?: 0
                val db = 20 * log10(amplitude / 32767.0)
                if (db > maxDb) {
                    maxDb = db
                }
                if (db < minDb) {
                    minDb = db
                }
                totalDb += db
                count++
            }
        }
        recordingThread?.start()
        handler.postDelayed(timerRunnable, 1000)
    }
}