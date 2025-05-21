package com.zamouli.aiassistant.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.zamouli.aiassistant.R
import com.zamouli.aiassistant.core.ModelManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * خدمة الكشف عن الكلمة المفتاحية التي تعمل في الخلفية للاستماع المستمر للكلمة المفتاحية
 */
class HotwordDetectionService : Service() {
    private val TAG = "HotwordDetectionService"
    
    // معالج للمهام المجدولة
    private val handler = Handler(Looper.getMainLooper())
    
    // مسجل الصوت
    private var audioRecord: AudioRecord? = null
    
    // تكوين تسجيل الصوت
    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
    
    // حجم النافذة للكشف (بالثواني)
    private val windowSizeSeconds = 2
    private val frameSize = sampleRate * windowSizeSeconds
    
    // مفسر نموذج الكشف عن الكلمة المفتاحية
    private var interpreter: Interpreter? = null
    
    // مدير النماذج
    private lateinit var modelManager: ModelManager
    
    // اسم نموذج الكشف عن الكلمة المفتاحية
    private val hotwordModelName = "hotword_detection.tflite"
    
    // مؤشر للتحكم في عملية الكشف
    private var isRunning = false
    
    // مستمعون للكلمة المفتاحية
    private val listeners = mutableListOf<HotwordListener>()
    
    // نطاق العمل للمهام المتزامنة
    private var detectionJob: Job? = null
    
    // معرّف قناة الإشعارات
    private val NOTIFICATION_CHANNEL_ID = "hotword_detection_channel"
    private val NOTIFICATION_ID = 1002
    
    /**
     * واجهة مستمع الكلمة المفتاحية
     */
    interface HotwordListener {
        fun onHotwordDetected()
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Hotword detection service created")
        
        // تهيئة مدير النماذج
        modelManager = ModelManager(this)
        
        // تحميل نموذج الكشف عن الكلمة المفتاحية
        loadHotwordModel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Hotword detection service start command")
        
        // إنشاء قناة إشعارات للخدمة الأمامية
        createNotificationChannel()
        
        // بدء الخدمة في المقدمة
        startForeground(NOTIFICATION_ID, createNotification())
        
        // بدء الكشف عن الكلمة المفتاحية
        startHotwordDetection()
        
        return START_STICKY
    }
    
    /**
     * تحميل نموذج الكشف عن الكلمة المفتاحية
     */
    private fun loadHotwordModel() {
        try {
            // تحميل النموذج باستخدام مدير النماذج
            interpreter = modelManager.loadModel(hotwordModelName)
            
            if (interpreter != null) {
                Log.d(TAG, "Hotword model loaded successfully")
            } else {
                Log.e(TAG, "Failed to load hotword model")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading hotword model", e)
        }
    }
    
    /**
     * بدء الكشف عن الكلمة المفتاحية
     */
    private fun startHotwordDetection() {
        if (isRunning) {
            Log.d(TAG, "Hotword detection already running")
            return
        }
        
        if (interpreter == null) {
            Log.e(TAG, "Cannot start hotword detection, model not loaded")
            return
        }
        
        try {
            // تهيئة تسجيل الصوت
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )
            
            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord initialization failed")
                return
            }
            
            // بدء تسجيل الصوت
            audioRecord?.startRecording()
            isRunning = true
            
            // بدء عملية الكشف في الخلفية
            detectionJob = CoroutineScope(Dispatchers.IO).launch {
                runDetection()
            }
            
            Log.d(TAG, "Hotword detection started")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting hotword detection", e)
            stopHotwordDetection()
        }
    }
    
    /**
     * تنفيذ عملية الكشف عن الكلمة المفتاحية
     */
    private suspend fun runDetection() {
        try {
            val buffer = ByteBuffer.allocateDirect(frameSize * 2)  // 16 بت لكل عينة
            buffer.order(ByteOrder.LITTLE_ENDIAN)
            
            val outputBuffer = ByteBuffer.allocateDirect(4)  // float للنتيجة
            outputBuffer.order(ByteOrder.LITTLE_ENDIAN)
            
            // الاستمرار في الاستماع والتحليل
            while (isRunning) {
                // قراءة البيانات الصوتية
                buffer.clear()
                
                val readResult = audioRecord?.read(buffer, buffer.capacity())
                if (readResult != null && readResult > 0) {
                    // معالجة البيانات الصوتية
                    buffer.rewind()
                    
                    // تحويل البيانات إلى التنسيق المناسب للنموذج
                    val inputBuffer = preprocessAudio(buffer)
                    
                    // تنفيذ التوقعات
                    outputBuffer.rewind()
                    interpreter?.run(inputBuffer, outputBuffer)
                    
                    // معالجة النتيجة
                    outputBuffer.rewind()
                    val confidence = outputBuffer.getFloat()
                    
                    Log.d(TAG, "Hotword confidence: $confidence")
                    
                    // إذا تجاوزت الثقة العتبة، قم بإرسال إشعار الكشف
                    if (confidence > 0.8f) {
                        withContext(Dispatchers.Main) {
                            onHotwordDetected()
                        }
                        
                        // توقف مؤقتًا لتجنب كشف متكرر
                        withContext(Dispatchers.IO) {
                            Thread.sleep(3000)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in hotword detection loop", e)
        }
    }
    
    /**
     * معالجة البيانات الصوتية قبل إدخالها إلى النموذج
     */
    private fun preprocessAudio(buffer: ByteBuffer): ByteBuffer {
        // في التنفيذ الفعلي، يجب معالجة البيانات الصوتية واستخراج الميزات المناسبة
        // مثل MFCC أو استخراج الميزات الطيفية
        
        // هذا مثال مبسط فقط
        return buffer
    }
    
    /**
     * معالجة اكتشاف الكلمة المفتاحية
     */
    private fun onHotwordDetected() {
        Log.d(TAG, "Hotword detected!")
        
        // إرسال إشعار إلى جميع المستمعين
        for (listener in listeners) {
            listener.onHotwordDetected()
        }
    }
    
    /**
     * إيقاف الكشف عن الكلمة المفتاحية
     */
    private fun stopHotwordDetection() {
        isRunning = false
        
        // إيقاف تسجيل الصوت
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        
        // إلغاء مهمة الكشف
        detectionJob?.cancel()
        detectionJob = null
        
        Log.d(TAG, "Hotword detection stopped")
    }
    
    /**
     * إضافة مستمع للكلمة المفتاحية
     */
    fun addHotwordListener(listener: HotwordListener) {
        listeners.add(listener)
    }
    
    /**
     * إزالة مستمع للكلمة المفتاحية
     */
    fun removeHotwordListener(listener: HotwordListener) {
        listeners.remove(listener)
    }
    
    /**
     * إنشاء قناة إشعارات للخدمة الأمامية
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Hotword Detection"
            val descriptionText = "Used for detecting hotwords"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * إنشاء إشعار للخدمة الأمامية
     */
    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("الاستماع للكلمة المفتاحية")
            .setContentText("جاري الاستماع للكلمة المفتاحية...")
            .setSmallIcon(R.drawable.ic_mic)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        // إيقاف الكشف عن الكلمة المفتاحية
        stopHotwordDetection()
        
        // تحرير الموارد
        modelManager.unloadModel(hotwordModelName)
        
        Log.d(TAG, "Hotword detection service destroyed")
    }
}