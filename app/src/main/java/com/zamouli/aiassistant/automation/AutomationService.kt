package com.zamouli.aiassistant.automation

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.annotation.RequiresApi

/**
 * خدمة إمكانية الوصول التي تتيح التحكم التلقائي في التطبيقات الأخرى
 * على الجهاز من خلال API تسهيل الوصول
 */
class AutomationService : AccessibilityService() {
    private val TAG = "AutomationService"
    
    companion object {
        private var instance: AutomationService? = null
        
        fun getInstance(): AutomationService? {
            return instance
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.d(TAG, "Automation service created")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        instance = null
        Log.d(TAG, "Automation service destroyed")
    }
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Automation service connected")
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        try {
            val packageName = event.packageName?.toString() ?: return
            val eventType = event.eventType
            
            Log.d(TAG, "Received accessibility event, package: $packageName, type: $eventType")
            
            // معالجة الحدث بناءً على نوعه
            when (eventType) {
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                    // تغيير حالة النافذة، مثل فتح تطبيق جديد
                    Log.d(TAG, "Window state changed: $packageName")
                }
                
                AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                    // النقر على عنصر
                    Log.d(TAG, "View clicked: ${event.className}")
                }
                
                AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                    // تغيير نص في حقل إدخال
                    Log.d(TAG, "Text changed: ${event.text}")
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing accessibility event", e)
        }
    }
    
    override fun onInterrupt() {
        Log.d(TAG, "Automation service interrupted")
    }
    
    /**
     * البحث عن عنصر بواسطة النص
     */
    fun findElementByText(text: String): AccessibilityNodeInfo? {
        try {
            val rootNode = rootInActiveWindow ?: return null
            
            val nodes = ArrayList<AccessibilityNodeInfo>()
            findNodesByText(rootNode, text, nodes)
            
            if (nodes.isNotEmpty()) {
                return nodes[0]
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error finding element by text: $text", e)
        }
        
        return null
    }
    
    /**
     * البحث عن العقد بواسطة النص
     */
    private fun findNodesByText(node: AccessibilityNodeInfo, text: String, result: ArrayList<AccessibilityNodeInfo>) {
        try {
            // التحقق من النص في العقدة الحالية
            val nodeText = node.text?.toString()
            val nodeDescription = node.contentDescription?.toString()
            
            if ((nodeText != null && nodeText.contains(text, ignoreCase = true)) ||
                (nodeDescription != null && nodeDescription.contains(text, ignoreCase = true))) {
                result.add(node)
            }
            
            // البحث في العقد الفرعية
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                findNodesByText(child, text, result)
                child.recycle()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error finding nodes by text", e)
        }
    }
    
    /**
     * النقر على عنصر
     */
    fun clickOnElement(node: AccessibilityNodeInfo): Boolean {
        try {
            return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        } catch (e: Exception) {
            Log.e(TAG, "Error clicking on element", e)
            return false
        }
    }
    
    /**
     * إدخال نص في حقل
     */
    fun inputText(node: AccessibilityNodeInfo, text: String): Boolean {
        try {
            val arguments = Bundle()
            arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
        } catch (e: Exception) {
            Log.e(TAG, "Error inputting text", e)
            return false
        }
    }
    
    /**
     * النقر على إحداثيات محددة
     */
    @RequiresApi(Build.VERSION_CODES.N)
    fun clickAtPosition(x: Float, y: Float): Boolean {
        try {
            val path = Path()
            path.moveTo(x, y)
            
            val gestureBuilder = GestureDescription.Builder()
            gestureBuilder.addStroke(GestureDescription.StrokeDescription(path, 0, 100))
            
            val gesture = gestureBuilder.build()
            return dispatchGesture(gesture, null, null)
        } catch (e: Exception) {
            Log.e(TAG, "Error clicking at position", e)
            return false
        }
    }
    
    /**
     * التمرير للأعلى
     */
    @RequiresApi(Build.VERSION_CODES.N)
    fun scrollUp(): Boolean {
        try {
            val displayMetrics = resources.displayMetrics
            val width = displayMetrics.widthPixels.toFloat()
            val height = displayMetrics.heightPixels.toFloat()
            
            val path = Path()
            path.moveTo(width / 2, height * 0.7f)
            path.lineTo(width / 2, height * 0.3f)
            
            val gestureBuilder = GestureDescription.Builder()
            gestureBuilder.addStroke(GestureDescription.StrokeDescription(path, 0, 500))
            
            val gesture = gestureBuilder.build()
            return dispatchGesture(gesture, null, null)
        } catch (e: Exception) {
            Log.e(TAG, "Error scrolling up", e)
            return false
        }
    }
    
    /**
     * التمرير للأسفل
     */
    @RequiresApi(Build.VERSION_CODES.N)
    fun scrollDown(): Boolean {
        try {
            val displayMetrics = resources.displayMetrics
            val width = displayMetrics.widthPixels.toFloat()
            val height = displayMetrics.heightPixels.toFloat()
            
            val path = Path()
            path.moveTo(width / 2, height * 0.3f)
            path.lineTo(width / 2, height * 0.7f)
            
            val gestureBuilder = GestureDescription.Builder()
            gestureBuilder.addStroke(GestureDescription.StrokeDescription(path, 0, 500))
            
            val gesture = gestureBuilder.build()
            return dispatchGesture(gesture, null, null)
        } catch (e: Exception) {
            Log.e(TAG, "Error scrolling down", e)
            return false
        }
    }
}