package com.zamouli.aiassistant.speech

import android.content.Context
import android.util.Log

/**
 * مدير التعرف على الكلام المسؤول عن إدارة وتنسيق مكونات الكلام المختلفة
 */
class SpeechRecognizerManager(private val context: Context) {
    private val TAG = "SpeechRecognizerManager"
    
    // مكونات الكلام
    private var speechRecognizer: SpeechRecognizer? = null
    private var voiceToneAnalyzer: VoiceToneAnalyzer? = null
    
    // واجهة استدعاء النتائج
    private var callback: SpeechRecognizerManagerCallback? = null
    
    /**
     * تهيئة مدير التعرف على الكلام
     */
    fun initialize(callback: SpeechRecognizerManagerCallback) {
        this.callback = callback
        
        // إنشاء معرِّف الكلام
        createSpeechRecognizer()
        
        // إنشاء محلل نبرة الصوت
        createVoiceToneAnalyzer()
        
        Log.d(TAG, "Speech recognizer manager initialized")
    }
    
    /**
     * إنشاء معرِّف الكلام
     */
    private fun createSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer(context, object : SpeechRecognizer.SpeechRecognizerCallback {
            override fun onResult(text: String) {
                // معالجة نتيجة التعرف على الكلام
                callback?.onSpeechRecognized(text)
            }
            
            override fun onError(errorMessage: String) {
                // معالجة خطأ التعرف على الكلام
                callback?.onSpeechError(errorMessage)
            }
        })
    }
    
    /**
     * إنشاء محلل نبرة الصوت
     */
    private fun createVoiceToneAnalyzer() {
        voiceToneAnalyzer = VoiceToneAnalyzer(context)
    }
    
    /**
     * بدء الاستماع للكلام
     */
    fun startListening() {
        speechRecognizer?.startListening()
    }
    
    /**
     * إيقاف الاستماع للكلام
     */
    fun stopListening() {
        speechRecognizer?.stopListening()
    }
    
    /**
     * تحليل نبرة الصوت من البيانات الصوتية
     */
    fun analyzeVoiceTone(audioData: ByteArray): String? {
        return voiceToneAnalyzer?.analyzeVoiceTone(audioData)
    }
    
    /**
     * تحرير الموارد
     */
    fun release() {
        speechRecognizer?.release()
        speechRecognizer = null
        
        voiceToneAnalyzer?.release()
        voiceToneAnalyzer = null
        
        callback = null
        
        Log.d(TAG, "Speech recognizer manager released")
    }
    
    /**
     * واجهة استدعاء لنتائج التعرف على الكلام وتحليل نبرة الصوت
     */
    interface SpeechRecognizerManagerCallback {
        fun onSpeechRecognized(text: String)
        fun onSpeechError(errorMessage: String)
        fun onVoiceToneAnalyzed(emotion: String)
    }
}