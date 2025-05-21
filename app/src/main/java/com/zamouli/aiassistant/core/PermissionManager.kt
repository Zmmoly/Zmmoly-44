package com.zamouli.aiassistant.core

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * مدير الأذونات المسؤول عن التحقق وطلب أذونات التطبيق
 */
class PermissionManager(private val context: Context) {
    private val TAG = "PermissionManager"
    
    // رموز طلب الأذونات
    private val PERMISSION_REQUEST_CODE = 1000
    
    // قائمة الأذونات الأساسية
    private val essentialPermissions = listOf(
        Manifest.permission.INTERNET,
        Manifest.permission.ACCESS_NETWORK_STATE,
        Manifest.permission.RECORD_AUDIO
    )
    
    // قائمة الأذونات الإضافية
    private val additionalPermissions = listOf(
        Manifest.permission.CALL_PHONE,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.SEND_SMS,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.CAMERA,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
    
    // قائمة أذونات أندرويد الأحدث
    private val modernPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        listOf(
            Manifest.permission.FOREGROUND_SERVICE,
            Manifest.permission.SYSTEM_ALERT_WINDOW
        )
    } else {
        emptyList()
    }
    
    /**
     * التحقق من الأذونات الأساسية وطلبها إذا لزم الأمر
     */
    fun checkAndRequestEssentialPermissions(activity: Activity): Boolean {
        val permissionsToRequest = mutableListOf<String>()
        
        // التحقق من كل إذن أساسي
        for (permission in essentialPermissions) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission)
            }
        }
        
        // طلب الأذونات المفقودة
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                activity,
                permissionsToRequest.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
            return false
        }
        
        return true
    }
    
    /**
     * التحقق من الأذونات الإضافية وطلبها
     */
    fun checkAndRequestAdditionalPermissions(activity: Activity) {
        val permissionsToRequest = mutableListOf<String>()
        
        // التحقق من كل إذن إضافي
        for (permission in additionalPermissions) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission)
            }
        }
        
        // طلب الأذونات المفقودة
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                activity,
                permissionsToRequest.toTypedArray(),
                PERMISSION_REQUEST_CODE + 1
            )
        }
    }
    
    /**
     * التحقق من أذونات أندرويد الأحدث
     */
    fun checkAndRequestModernPermissions(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val permissionsToRequest = mutableListOf<String>()
            
            // التحقق من كل إذن حديث
            for (permission in modernPermissions) {
                if (permission == Manifest.permission.SYSTEM_ALERT_WINDOW) {
                    // هذا الإذن يتطلب معالجة خاصة
                    if (!Settings.canDrawOverlays(context)) {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        activity.startActivity(intent)
                    }
                } else if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(permission)
                }
            }
            
            // طلب الأذونات المفقودة
            if (permissionsToRequest.isNotEmpty()) {
                ActivityCompat.requestPermissions(
                    activity,
                    permissionsToRequest.toTypedArray(),
                    PERMISSION_REQUEST_CODE + 2
                )
            }
        }
    }
    
    /**
     * التحقق من إذن محدد
     */
    fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * التحقق من مجموعة من الأذونات
     */
    fun hasPermissions(permissions: List<String>): Boolean {
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }
    
    /**
     * عرض إعدادات التطبيق لتمكين المستخدم من تغيير الأذونات يدويًا
     */
    fun openAppPermissionSettings(activity: Activity) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri = Uri.fromParts("package", context.packageName, null)
            intent.data = uri
            activity.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening app permission settings", e)
        }
    }
}