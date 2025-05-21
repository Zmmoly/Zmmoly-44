package com.zamouli.aiassistant.core

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import android.util.Log

/**
 * مدير الذاكرة المسؤول عن تحسين استخدام الذاكرة وإدارة موارد التطبيق
 */
class MemoryManager private constructor(private val context: Context) {
    private val TAG = "MemoryManager"
    
    // معلومات الذاكرة
    var totalMemoryMB: Long = 0
        private set
    var availableMemoryMB: Long = 0
        private set
    var thresholdLowMemoryMB: Long = 200 // حد الذاكرة المنخفضة بالميجابايت
        private set
    
    // عدد النماذج المقترح تحميلها بناءً على الذاكرة المتاحة
    var recommendedModelCount: Int = 3
        private set
    
    companion object {
        @Volatile
        private var instance: MemoryManager? = null
        
        /**
         * تهيئة مدير الذاكرة
         */
        fun initialize(context: Context) {
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) {
                        instance = MemoryManager(context.applicationContext)
                    }
                }
            }
        }
        
        /**
         * الحصول على مثيل مدير الذاكرة
         */
        fun getInstance(): MemoryManager {
            return instance ?: throw IllegalStateException("MemoryManager must be initialized first")
        }
    }
    
    /**
     * تكوين الذاكرة وتحديد القيم المناسبة بناءً على الجهاز
     */
    fun configureMemory() {
        try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)
            
            // حساب إجمالي الذاكرة المتاحة بالميجابايت
            totalMemoryMB = memoryInfo.totalMem / (1024 * 1024)
            
            // حساب الذاكرة المتاحة حاليًا بالميجابايت
            availableMemoryMB = memoryInfo.availMem / (1024 * 1024)
            
            // ضبط حد الذاكرة المنخفضة
            thresholdLowMemoryMB = totalMemoryMB / 10 // 10% من إجمالي الذاكرة
            
            // تحديد عدد النماذج الموصى بها بناءً على الذاكرة المتاحة
            recommendedModelCount = calculateRecommendedModelCount()
            
            Log.d(TAG, "Memory configured - Total: $totalMemoryMB MB, Available: $availableMemoryMB MB")
            Log.d(TAG, "Recommended model count: $recommendedModelCount")
        } catch (e: Exception) {
            Log.e(TAG, "Error configuring memory", e)
        }
    }
    
    /**
     * حساب عدد النماذج الموصى به بناءً على الذاكرة المتاحة
     */
    private fun calculateRecommendedModelCount(): Int {
        return when {
            // أجهزة منخفضة الموارد (أقل من 3 جيجابايت)
            totalMemoryMB < 3000 -> 1
            
            // أجهزة متوسطة (3-6 جيجابايت)
            totalMemoryMB < 6000 -> 3
            
            // أجهزة عالية (6-8 جيجابايت)
            totalMemoryMB < 8000 -> 4
            
            // أجهزة ممتازة (أكثر من 8 جيجابايت)
            else -> 6
        }
    }
    
    /**
     * التحقق مما إذا كان الجهاز في وضع الذاكرة المنخفضة
     */
    fun isLowMemory(): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        
        val availableMemoryMB = memoryInfo.availMem / (1024 * 1024)
        return availableMemoryMB < thresholdLowMemoryMB
    }
    
    /**
     * تحديث معلومات الذاكرة
     */
    fun updateMemoryInfo() {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        
        // تحديث الذاكرة المتاحة
        availableMemoryMB = memoryInfo.availMem / (1024 * 1024)
    }
    
    /**
     * تقدير حجم الذاكرة الذي سيستهلكه نموذج معين
     */
    fun estimateModelMemoryUsage(modelName: String): Long {
        // تقدير تقريبي لحجم الذاكرة المستخدمة لكل نموذج بالميجابايت
        return when (modelName) {
            "speech_recognition.tflite" -> 30
            "text_analysis.tflite" -> 50
            "sentiment_analysis.tflite" -> 20
            "voice_tone_analysis.tflite" -> 60
            "conversation_model.tflite" -> 120
            "translation_model.tflite" -> 80
            else -> 20 // افتراضي
        }
    }
    
    /**
     * تحرير الذاكرة عند الحاجة
     */
    fun releaseMemoryIfNeeded() {
        if (isLowMemory()) {
            Log.d(TAG, "Low memory detected, releasing resources...")
            
            // تنفيذ جامع النفايات
            System.gc()
            
            // إشعار لتفريغ الذاكرة المؤقتة
            // يمكن إضافة رمز إضافي لتفريغ الذاكرة المؤقتة للتطبيق
        }
    }
}