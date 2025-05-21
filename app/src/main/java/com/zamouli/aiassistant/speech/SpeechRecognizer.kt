package com.zamouli.aiassistant.speech

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import java.util.Locale

/**
 * مكون معرِّف الكلام المسؤول عن تحويل الصوت إلى نص
 */
class SpeechRecognizer(private val context: Context, private val callback: SpeechRecognizerCallback) {
    private val TAG = "SpeechRecognizer"
    
    // معرِّف الكلام من Android
    private var speechRecognizer: android.speech.SpeechRecognizer? = null
    
    // حالة الاستماع
    private var isListening = false
    
    init {
        initialize()
    }
    
    /**
     * تهيئة معرِّف الكلام
     */
    private fun initialize() {
        try {
            // التحقق من دعم التعرف على الكلام
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
                speechRecognizer?.setRecognitionListener(createRecognitionListener())
            } else {
                Log.e(TAG, "Speech recognition is not available on this device")
                callback.onError("التعرف على الكلام غير متاح على هذا الجهاز")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing speech recognizer", e)
            callback.onError("خطأ في تهيئة معرِّف الكلام: ${e.message}")
        }
    }
    
    /**
     * بدء الاستماع للكلام
     */
    fun startListening() {
        try {
            if (isListening) {
                stopListening()
            }
            
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-SA") // اللغة العربية
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500)
            }
            
            speechRecognizer?.startListening(intent)
            isListening = true
            
            Log.d(TAG, "Speech recognition started")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting speech recognition", e)
            callback.onError("خطأ في بدء التعرف على الكلام: ${e.message}")
        }
    }
    
    /**
     * إيقاف الاستماع للكلام
     */
    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            isListening = false
            
            Log.d(TAG, "Speech recognition stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping speech recognition", e)
        }
    }
    
    /**
     * إنشاء مستمع للتعرف على الكلام
     */
    private fun createRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d(TAG, "Ready for speech")
            }

            override fun onBeginningOfSpeech() {
                Log.d(TAG, "Beginning of speech")
            }

            override fun onRmsChanged(rmsdB: Float) {
                // تغيير في مستوى الصوت
            }

            override fun onBufferReceived(buffer: ByteArray?) {
                // استلام البيانات المؤقتة
            }

            override fun onEndOfSpeech() {
                Log.d(TAG, "End of speech")
                isListening = false
            }

            override fun onError(error: Int) {
                isListening = false
                val errorMessage = getErrorMessage(error)
                Log.e(TAG, "Error in speech recognition: $errorMessage")
                callback.onError(errorMessage)
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val bestMatch = matches[0]
                    Log.d(TAG, "Speech recognition result: $bestMatch")
                    callback.onResult(bestMatch)
                } else {
                    Log.d(TAG, "No speech recognition results")
                    callback.onError("لم يتم التعرف على أي كلام")
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val bestMatch = matches[0]
                    Log.d(TAG, "Partial speech recognition result: $bestMatch")
                    // يمكن إضافة معالجة النتائج الجزئية هنا
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {
                // أحداث أخرى
            }
        }
    }
    
    /**
     * الحصول على رسالة الخطأ بناءً على رمز الخطأ
     */
    private fun getErrorMessage(errorCode: Int): String {
        return when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO -> "خطأ في تسجيل الصوت"
            SpeechRecognizer.ERROR_CLIENT -> "خطأ في التطبيق"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "أذونات غير كافية"
            SpeechRecognizer.ERROR_NETWORK -> "خطأ في الشبكة"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "انتهت مهلة الاتصال بالشبكة"
            SpeechRecognizer.ERROR_NO_MATCH -> "لم يتم التعرف على الكلام"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "خدمة التعرف على الكلام مشغولة"
            SpeechRecognizer.ERROR_SERVER -> "خطأ في الخادم"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "لم يتم اكتشاف أي كلام"
            else -> "خطأ غير معروف"
        }
    }
    
    /**
     * تحرير الموارد
     */
    fun release() {
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
            isListening = false
            
            Log.d(TAG, "Speech recognizer released")
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing speech recognizer", e)
        }
    }
    
    /**
     * واجهة استدعاء لنتائج التعرف على الكلام
     */
    interface SpeechRecognizerCallback {
        fun onResult(text: String)
        fun onError(errorMessage: String)
    }
}