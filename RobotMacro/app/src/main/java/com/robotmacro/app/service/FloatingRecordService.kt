package com.robotmacro.app.service

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import com.google.android.material.button.MaterialButton

class FloatingRecordService : Service() {
    private lateinit var windowManager: WindowManager
    private var recordButton: MaterialButton? = null
    private var isRecording = false

    override fun onCreate() {
        super.onCreate()
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        showFloatingButton()
    }

    private fun showFloatingButton() {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 24
            y = 160
        }
        recordButton = MaterialButton(this).apply {
            text = "● REC"
            setOnClickListener { toggleRecording() }
            setOnTouchListener(DragTouchListener(params))
        }
        windowManager.addView(recordButton, params)
    }

    private fun toggleRecording() {
        val service = MacroAccessibilityService.instance ?: return
        if (isRecording) {
            service.stopRecording()
            recordButton?.text = "● REC"
        } else {
            service.startRecording("Overlay_${System.currentTimeMillis()}")
            recordButton?.text = "■ STOP"
        }
        isRecording = !isRecording
    }

    override fun onDestroy() {
        recordButton?.let { windowManager.removeView(it) }
        recordButton = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private inner class DragTouchListener(private val params: WindowManager.LayoutParams) : android.view.View.OnTouchListener {
        private var downX = 0
        private var downY = 0
        private var touchX = 0f
        private var touchY = 0f
        override fun onTouch(v: android.view.View, event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> { downX = params.x; downY = params.y; touchX = event.rawX; touchY = event.rawY }
                MotionEvent.ACTION_MOVE -> {
                    params.x = downX - (event.rawX - touchX).toInt()
                    params.y = downY + (event.rawY - touchY).toInt()
                    windowManager.updateViewLayout(v, params)
                    return true
                }
            }
            return false
        }
    }
}
