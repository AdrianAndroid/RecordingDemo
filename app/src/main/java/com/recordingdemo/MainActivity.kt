package com.recordingdemo

import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.*
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var mRecorder: MediaRecorder
    private  var mMaxDbTextView: TextView?=null
    private  var mMinDbTextView: TextView?=null
    private  var mAvgDbTextView: TextView?=null
    private var mMaxDb: Double = 0.0
    private var mMinDb: Double = 0.0
    private var mAvgDb: Double = 0.0
    private var mNumSamples: Int = 0
    private val UPDATE_UI = 1
    private val MAX_DURATION = 10000 // 10 seconds
    private var isInitialized = false

    private val mHandler: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when(msg.what) {
                UPDATE_UI -> {
                    val max = String.format("%.2f dB", mMaxDb)
                    val min = String.format("%.2f dB", mMinDb)
                    val avg = String.format("%.2f dB", mAvgDb)
                    mMaxDbTextView?.text = max
                    mMinDbTextView?.text = min
                    mAvgDbTextView?.text = avg
                    val sb = StringBuilder()
                    sb.append("max:").append(max).append(",")
                    sb.append("min:").append(min).append(",")
                    sb.append("avg:").append(avg)
                    Log.i("MainActivity", sb.toString())
                }
                else -> {
                    super.handleMessage(msg)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mMaxDbTextView = findViewById(R.id.mMaxDbTextView)
        mMinDbTextView = findViewById(R.id.mMinDbTextView)
        mAvgDbTextView = findViewById(R.id.mAvgDbTextView)

        findViewById<Button>(R.id.btnStartRecord).setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED
            ) {
                if (!isInitialized) {
                    startRecording()
                }
            } else {
                ActivityCompat.requestPermissions(this,
                    arrayOf(android.Manifest.permission.RECORD_AUDIO),
                    1)
            }
        }
    }

    private fun startRecording() {
        isInitialized = true

        mRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this)
        } else {
            MediaRecorder()
        }
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
        mRecorder.setOutputFile("/dev/null")

        kotlin.runCatching {
            mRecorder.prepare()
            mRecorder.start()
            Thread {
                while (true) {
                    kotlin.runCatching { Thread.sleep(100) }
                    val db: Double = 20 * Math.log10(mRecorder.maxAmplitude / 32767.0)
                    if (db > mMaxDb) {
                        mMaxDb = db
                    }
                    if (db < mMinDb || mNumSamples == 0) {
                        mMinDb = db
                    }
                    mAvgDb = ((mAvgDb * mNumSamples) + db) / (mNumSamples + 1)
                    mNumSamples++

                    if (mNumSamples * 100 > MAX_DURATION) {
                        mHandler.sendEmptyMessage(UPDATE_UI)
                    }
                }
            }.start()

        }.onFailure {
            it.printStackTrace()
        }
    }

    private fun stopRecording() {

    }
}