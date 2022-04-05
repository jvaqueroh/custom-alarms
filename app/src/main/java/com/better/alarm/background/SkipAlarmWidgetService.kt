package com.better.alarm.background

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import com.better.alarm.R

class SkipAlarmWidgetService : Service() {
    private lateinit var widgetFloatingView: View
    private var layoutFlag = 0

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutFlag = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        }
        else{
            layoutFlag = WindowManager.LayoutParams.TYPE_PHONE
        }
        
        widgetFloatingView = LayoutInflater.from(this).inflate(R.layout.skip_widget, null)
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT)
        layoutParams.gravity = Gravity.TOP or Gravity.END
        layoutParams.x = 0
        layoutParams.y = 300

        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager.apply {
            addView(widgetFloatingView, layoutParams)
        }
        widgetFloatingView.visibility = View.VISIBLE
        return START_STICKY
    }
}
