package com.zamouli.aiassistant.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.zamouli.aiassistant.R
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * خدمة التقاط الشاشة التي تسمح بالتقاط صور الشاشة في الخلفية
 */
class ScreenCaptureService : Service() {
    private val TAG = "ScreenCaptureService"
    
    // معلمات عرض الشاشة
    private var width = 720
    private var height = 1280
    private var density = 1
    
    // مكونات التقاط الشاشة
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    
    // النافذة المدير
    private var windowManager: WindowManager? = null
    
    // معالج للمهام المجدولة
    private val handler = Handler(Looper.getMainLooper())
    
    // معرّف قناة الإشعارات
    private val NOTIFICATION_CHANNEL_ID = "screen_capture_channel"
    private val NOTIFICATION_ID = 1001
    
    companion object {
        // النية المشتركة للتقاط الشاشة
        var screenCaptureIntent: Intent? = null
        
        // مستمع لصور الشاشة الملتقطة
        private var screenshotListener: ScreenshotListener? = null
        
        /**
         * واجهة المستمع لصور الشاشة الملتقطة
         */
        interface ScreenshotListener {
            fun onScreenshotTaken(file: File)
            fun onError(errorMessage: String)
        }
        
        /**
         * تعيين مستمع لصور الشاشة
         */
        fun setScreenshotListener(listener: ScreenshotListener) {
            screenshotListener = listener
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Screen capture service created")
        
        // تهيئة مدير النافذة
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        
        // الحصول على أبعاد الشاشة
        val metrics = DisplayMetrics()
        windowManager?.defaultDisplay?.getMetrics(metrics)
        density = metrics.densityDpi
        width = metrics.widthPixels
        height = metrics.heightPixels
        
        // إنشاء قارئ الصور
        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Screen capture service start command")
        
        // إنشاء قناة إشعارات للخدمة الأمامية
        createNotificationChannel()
        
        // بدء الخدمة في المقدمة
        startForeground(NOTIFICATION_ID, createNotification())
        
        // تهيئة التقاط الشاشة
        initScreenCapture()
        
        return START_STICKY
    }
    
    /**
     * تهيئة التقاط الشاشة
     */
    private fun initScreenCapture() {
        try {
            if (screenCaptureIntent == null) {
                Log.e(TAG, "Screen capture intent is null")
                screenshotListener?.onError("لم يتم منح إذن التقاط الشاشة")
                stopSelf()
                return
            }
            
            // الحصول على مدير إسقاط الوسائط
            val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            
            // إنشاء إسقاط الوسائط
            mediaProjection = projectionManager.getMediaProjection(RESULT_OK, screenCaptureIntent!!)
            
            // إنشاء العرض الافتراضي
            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "ScreenCapture",
                width,
                height,
                density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader?.surface,
                null,
                null
            )
            
            // جدولة التقاط للشاشة
            handler.postDelayed({ captureScreen() }, 1000)
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing screen capture", e)
            screenshotListener?.onError("خطأ في تهيئة التقاط الشاشة: ${e.message}")
            stopSelf()
        }
    }
    
    /**
     * التقاط صورة الشاشة
     */
    private fun captureScreen() {
        try {
            val image = imageReader?.acquireLatestImage()
            if (image != null) {
                // معالجة الصورة وتحويلها إلى ملف صورة
                val planes = image.planes
                val buffer = planes[0].buffer
                val pixelStride = planes[0].pixelStride
                val rowStride = planes[0].rowStride
                val rowPadding = rowStride - pixelStride * width
                
                // إنشاء صورة نقطية من البيانات
                val bitmap = Bitmap.createBitmap(
                    width + rowPadding / pixelStride,
                    height,
                    Bitmap.Config.ARGB_8888
                )
                bitmap.copyPixelsFromBuffer(buffer)
                
                // حفظ الصورة إلى ملف
                val screenshotFile = saveScreenshot(bitmap)
                
                // استدعاء المستمع
                screenshotListener?.onScreenshotTaken(screenshotFile)
                
                // تحرير الموارد
                image.close()
                bitmap.recycle()
            }
            
            // إيقاف الخدمة بعد التقاط الصورة
            stopSelf()
        } catch (e: Exception) {
            Log.e(TAG, "Error capturing screen", e)
            screenshotListener?.onError("خطأ في التقاط الشاشة: ${e.message}")
            stopSelf()
        }
    }
    
    /**
     * حفظ لقطة الشاشة إلى ملف
     */
    private fun saveScreenshot(bitmap: Bitmap): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "Screenshot_$timeStamp.jpg"
        val storageDir = getExternalFilesDir(null)
        val imageFile = File(storageDir, imageFileName)
        
        FileOutputStream(imageFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        
        Log.d(TAG, "Screenshot saved: ${imageFile.absolutePath}")
        return imageFile
    }
    
    /**
     * إنشاء قناة إشعارات للخدمة الأمامية
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Screen Capture"
            val descriptionText = "Used for capturing screen"
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
            .setContentTitle("التقاط الشاشة")
            .setContentText("جاري التقاط الشاشة...")
            .setSmallIcon(R.drawable.ic_mic)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        // تحرير الموارد
        virtualDisplay?.release()
        mediaProjection?.stop()
        imageReader?.close()
        
        Log.d(TAG, "Screen capture service destroyed")
    }
}