package com.zamouli.aiassistant.core

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * مدير نماذج الذكاء الاصطناعي
 * مسؤول عن تحميل وإدارة نماذج TensorFlow Lite المختلفة
 */
class ModelManager(private val context: Context) {
    private val TAG = "ModelManager"
    
    // قائمة المفسرين النشطة
    private val activeInterpreters = mutableMapOf<String, Interpreter>()
    
    // أسماء النماذج المتاحة
    private val availableModels = listOf(
        "speech_recognition.tflite",
        "text_analysis.tflite",
        "sentiment_analysis.tflite",
        "voice_tone_analysis.tflite",
        "conversation_model.tflite",
        "translation_model.tflite"
    )
    
    /**
     * تحميل نموذج بناءً على الاسم
     */
    fun loadModel(modelName: String): Interpreter? {
        try {
            // التحقق مما إذا كان المفسر محملًا بالفعل
            if (activeInterpreters.containsKey(modelName)) {
                return activeInterpreters[modelName]
            }
            
            // التحقق من وجود ملف النموذج
            val modelFile = File(context.getExternalFilesDir(null), "models/$modelName")
            if (!modelFile.exists()) {
                Log.e(TAG, "Model file not found: $modelName")
                return null
            }
            
            // تحميل النموذج
            val modelBuffer = loadModelFile(modelFile)
            val interpreter = Interpreter(modelBuffer)
            
            // حفظ المفسر في القائمة النشطة
            activeInterpreters[modelName] = interpreter
            
            Log.d(TAG, "Model loaded successfully: $modelName")
            return interpreter
        } catch (e: Exception) {
            Log.e(TAG, "Error loading model: $modelName", e)
            return null
        }
    }
    
    /**
     * إغلاق مفسر وإزالته من الذاكرة
     */
    fun unloadModel(modelName: String) {
        try {
            activeInterpreters[modelName]?.close()
            activeInterpreters.remove(modelName)
            Log.d(TAG, "Model unloaded: $modelName")
        } catch (e: Exception) {
            Log.e(TAG, "Error unloading model: $modelName", e)
        }
    }
    
    /**
     * تحميل ملف النموذج إلى الذاكرة
     */
    private fun loadModelFile(modelFile: File): MappedByteBuffer {
        val fileDescriptor = FileInputStream(modelFile).fd
        val fileChannel = FileInputStream(modelFile).channel
        val startOffset = 0L
        val length = modelFile.length()
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, length)
    }
    
    /**
     * الحصول على قائمة النماذج المتاحة
     */
    fun getAvailableModels(): List<String> {
        return availableModels
    }
    
    /**
     * الحصول على قائمة النماذج المحملة حاليًا
     */
    fun getLoadedModels(): List<String> {
        return activeInterpreters.keys.toList()
    }
    
    /**
     * تنظيف جميع الموارد
     */
    fun cleanup() {
        for ((name, interpreter) in activeInterpreters) {
            try {
                interpreter.close()
                Log.d(TAG, "Model closed: $name")
            } catch (e: Exception) {
                Log.e(TAG, "Error closing model: $name", e)
            }
        }
        activeInterpreters.clear()
    }
}