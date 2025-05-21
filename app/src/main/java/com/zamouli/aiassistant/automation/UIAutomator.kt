package com.zamouli.aiassistant.automation

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.util.Log

/**
 * فئة أتمتة واجهة المستخدم المسؤولة عن تنفيذ الإجراءات التلقائية مثل فتح التطبيقات
 */
class UIAutomator(private val context: Context) {
    private val TAG = "UIAutomator"
    
    /**
     * التحقق مما إذا كانت خدمة الأتمتة متصلة
     */
    fun isAutomationServiceConnected(): Boolean {
        // تحقق ما إذا كان المستخدم قد فعل خدمة إمكانية الوصول
        val accessibilityEnabled = isAccessibilityServiceEnabled()
        Log.d(TAG, "Accessibility service enabled: $accessibilityEnabled")
        return accessibilityEnabled
    }
    
    /**
     * التحقق من تمكين خدمة إمكانية الوصول
     */
    private fun isAccessibilityServiceEnabled(): Boolean {
        val packageName = context.packageName
        val serviceName = "$packageName.AutomationService"
        
        val accessibilityEnabled = try {
            Settings.Secure.getInt(context.contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED)
        } catch (e: Settings.SettingNotFoundException) {
            Log.e(TAG, "Error finding accessibility setting", e)
            return false
        }
        
        if (accessibilityEnabled == 1) {
            val settingValue = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            
            val splitter = TextUtils.SimpleStringSplitter(':')
            splitter.setString(settingValue)
            
            while (splitter.hasNext()) {
                val accessibilityService = splitter.next()
                if (accessibilityService.equals(serviceName, ignoreCase = true)) {
                    return true
                }
            }
        }
        
        return false
    }
    
    /**
     * فتح إعدادات إمكانية الوصول
     */
    fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
    
    /**
     * فتح تطبيق بالاسم
     */
    fun openApplication(appName: String) {
        try {
            Log.d(TAG, "Attempting to open application: $appName")
            
            // الحصول على قائمة جميع التطبيقات المثبتة
            val pm = context.packageManager
            val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            
            // البحث عن التطبيق بالاسم
            val matchingApps = installedApps.filter { 
                it.loadLabel(pm).toString().contains(appName, ignoreCase = true) 
            }
            
            if (matchingApps.isNotEmpty()) {
                // استخدام التطبيق الأول المطابق
                val packageName = matchingApps[0].packageName
                Log.d(TAG, "Found application package: $packageName")
                
                // فتح التطبيق
                val launchIntent = pm.getLaunchIntentForPackage(packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    Log.d(TAG, "Application launched successfully")
                    return
                }
            }
            
            // إذا وصلنا إلى هنا، فلم نجد التطبيق أو نتمكن من فتحه
            Log.e(TAG, "Application not found or cannot be launched: $appName")
        } catch (e: Exception) {
            Log.e(TAG, "Error opening application: $appName", e)
        }
    }
    
    /**
     * إدخال نص
     */
    fun inputText(text: String) {
        // يتطلب هذا استخدام خدمة إمكانية الوصول
        Log.d(TAG, "Input text requested: $text")
    }
    
    /**
     * النقر على موقع معين
     */
    fun clickAt(x: Int, y: Int) {
        // يتطلب هذا استخدام خدمة إمكانية الوصول
        Log.d(TAG, "Click requested at position: $x, $y")
    }
    
    /**
     * البحث عن عنصر في الشاشة والنقر عليه
     */
    fun findAndClickElement(elementDescription: String) {
        // يتطلب هذا استخدام خدمة إمكانية الوصول
        Log.d(TAG, "Find and click element requested: $elementDescription")
    }
}

/**
 * فئة مساعدة لتقسيم النصوص (مستنسخة من android.text.TextUtils لتبسيط الاعتمادات)
 */
private class TextUtils {
    class SimpleStringSplitter(private var mDelimiter: Char) : Iterator<String> {
        private var mString: String? = null
        private var mPosition = 0
        
        fun setString(string: String) {
            mString = string
            mPosition = 0
        }
        
        override fun hasNext(): Boolean {
            return mString != null && mPosition < mString!!.length
        }
        
        override fun next(): String {
            if (mString == null) {
                throw IllegalStateException()
            }
            
            var end = mString!!.indexOf(mDelimiter, mPosition)
            if (end == -1) {
                end = mString!!.length
            }
            
            val nextToken = mString!!.substring(mPosition, end)
            mPosition = end + 1
            
            return nextToken
        }
    }
}