package com.zamouli.aiassistant.utils

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

/**
 * أداة مساعدة للتعامل مع إيماءات اللمس
 */
class GestureUtil(context: Context) {

    /**
     * معالج الإيماءات الذي يستجيب لإيماءات مختلفة مثل النقر والتمرير والضغط الطويل
     */
    private val gestureDetector = GestureDetector(context, GestureListener())

    /**
     * مستمع للإيماءات المخصصة
     */
    private var listener: OnGestureListener? = null

    /**
     * تعيين مستمع للإيماءات
     */
    fun setOnGestureListener(listener: OnGestureListener) {
        this.listener = listener
    }

    /**
     * إضافة إمكانية الاستماع للإيماءات إلى عرض معين
     */
    fun attachToView(view: View) {
        view.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
        }
    }

    /**
     * مستمع الإيماءات الداخلي
     */
    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        private val SWIPE_THRESHOLD = 100
        private val SWIPE_VELOCITY_THRESHOLD = 100

        override fun onDown(e: MotionEvent): Boolean {
            return true
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            listener?.onTap(e.x, e.y)
            return true
        }

        override fun onLongPress(e: MotionEvent) {
            listener?.onLongPress(e.x, e.y)
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            listener?.onDoubleTap(e.x, e.y)
            return true
        }

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            if (e1 == null) return false

            val diffY = e2.y - e1.y
            val diffX = e2.x - e1.x

            if (abs(diffX) > abs(diffY)) {
                // التمرير الأفقي
                if (abs(diffX) > SWIPE_THRESHOLD && abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffX > 0) {
                        // تمرير لليمين
                        listener?.onSwipeRight()
                    } else {
                        // تمرير لليسار
                        listener?.onSwipeLeft()
                    }
                }
            } else {
                // التمرير الرأسي
                if (abs(diffY) > SWIPE_THRESHOLD && abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffY > 0) {
                        // تمرير للأسفل
                        listener?.onSwipeDown()
                    } else {
                        // تمرير للأعلى
                        listener?.onSwipeUp()
                    }
                }
            }

            return true
        }
    }

    /**
     * واجهة مستمع الإيماءات
     */
    interface OnGestureListener {
        fun onTap(x: Float, y: Float) {}
        fun onDoubleTap(x: Float, y: Float) {}
        fun onLongPress(x: Float, y: Float) {}
        fun onSwipeUp() {}
        fun onSwipeDown() {}
        fun onSwipeLeft() {}
        fun onSwipeRight() {}
    }

    /**
     * مستمع الإيماءات الافتراضي الذي يمكن توسيعه لتجاوز طرق معينة فقط
     */
    open class SimpleGestureListener : OnGestureListener
}