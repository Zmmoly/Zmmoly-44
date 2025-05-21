package com.zamouli.aiassistant.services

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

/**
 * خدمة الاستماع للإشعارات التي تسمح للتطبيق بالوصول إلى إشعارات النظام والتطبيقات الأخرى
 */
class NotificationListenerService : NotificationListenerService() {
    private val TAG = "NotificationListener"
    
    companion object {
        private var instance: NotificationListenerService? = null
        
        /**
         * التحقق مما إذا كانت الخدمة متصلة
         */
        fun isServiceConnected(): Boolean {
            return instance != null
        }
        
        /**
         * التحقق مما إذا كان المستخدم قد منح إذن الوصول إلى الإشعارات
         */
        fun isNotificationAccessEnabled(context: Context): Boolean {
            val packageName = context.packageName
            val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
            
            return flat != null && flat.contains(packageName)
        }
        
        /**
         * فتح إعدادات الوصول إلى الإشعارات
         */
        fun openNotificationAccessSettings(context: Context) {
            val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.d(TAG, "Notification listener service created")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        instance = null
        Log.d(TAG, "Notification listener service destroyed")
    }
    
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        try {
            Log.d(TAG, "Notification posted: ${sbn.packageName}")
            
            // استخراج معلومات الإشعار
            val notification = sbn.notification
            val extras = notification.extras
            
            // استخراج العنوان والمحتوى
            val title = extras.getString("android.title")
            val text = extras.getCharSequence("android.text")?.toString()
            
            Log.d(TAG, "Title: $title, Text: $text")
            
            // معالجة الإشعار بناءً على نوعه أو مصدره
            processNotification(sbn.packageName, title, text)
        } catch (e: Exception) {
            Log.e(TAG, "Error processing notification", e)
        }
    }
    
    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        Log.d(TAG, "Notification removed: ${sbn.packageName}")
    }
    
    /**
     * معالجة الإشعار بناءً على نوعه أو مصدره
     */
    private fun processNotification(packageName: String, title: String?, text: String?) {
        // يمكن تنفيذ منطق مخصص هنا لمعالجة الإشعارات من تطبيقات محددة
        // مثل الرسائل أو المكالمات أو البريد الإلكتروني
        
        when {
            packageName.contains("whatsapp") -> {
                // معالجة إشعارات واتساب
                Log.d(TAG, "WhatsApp notification: $title, $text")
            }
            packageName.contains("gmail") -> {
                // معالجة إشعارات البريد الإلكتروني
                Log.d(TAG, "Gmail notification: $title, $text")
            }
            packageName.contains("phone") || packageName.contains("dialer") -> {
                // معالجة إشعارات المكالمات
                Log.d(TAG, "Call notification: $title, $text")
            }
        }
    }
}