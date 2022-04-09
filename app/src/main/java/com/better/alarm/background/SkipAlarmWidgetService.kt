package com.better.alarm.background

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.*
import android.widget.TextView
import androidx.core.view.GestureDetectorCompat
import com.better.alarm.R
import com.better.alarm.interfaces.Intents
import com.better.alarm.interfaces.PresentationToModelIntents

class SkipAlarmWidgetService : Service() {
    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): SkipAlarmWidgetService = this@SkipAlarmWidgetService
    }

    private lateinit var widgetFloatingView: View
    private lateinit var windowManager: WindowManager
    private var layoutFlag = 0
    private lateinit var mGestureDetector: GestureDetectorCompat

    override fun onBind(p0: Intent?): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutFlag = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            layoutFlag = WindowManager.LayoutParams.TYPE_PHONE
        }

        widgetFloatingView = LayoutInflater.from(this).inflate(R.layout.skip_widget, null)
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        layoutParams.gravity = Gravity.TOP or Gravity.END
        layoutParams.x = 0
        layoutParams.y = 300
        val widgetTextView = widgetFloatingView.findViewById<TextView>(R.id.text_widget)
        val alarmId = intent!!.getIntExtra(Intents.EXTRA_ID, -1)
        val alarmLabel = intent.getStringExtra(Intents.EXTRA_LABEL)
        var widgetText = getString(R.string.skip)
        if(!alarmLabel.isNullOrBlank()){
            widgetText += "\n" + alarmLabel
        }
        widgetTextView.setText(widgetText)

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager.apply {
            addView(widgetFloatingView, layoutParams)
        }
        widgetFloatingView.visibility = View.VISIBLE

        mGestureDetector = GestureDetectorCompat(
            this,
            TouchAndScrollGestureListener(windowManager, layoutParams, widgetFloatingView, this, alarmId, widgetText)
        )

        widgetFloatingView.setOnTouchListener { _, event ->
            mGestureDetector.onTouchEvent(event)
            true
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        if (widgetFloatingView != null)
            windowManager.removeView(widgetFloatingView)
    }
}

private class TouchAndScrollGestureListener(
    val windowManager: WindowManager,
    val layoutParams: WindowManager.LayoutParams,
    val widgetFloatingView: View,
    val context: Context,
    val alarmId: Int,
    val widgetText: String
) :
    GestureDetector.SimpleOnGestureListener() {
    private var initialX: Int = 0
    private var initialY: Int = 0
    private var initialTouchX: Float = 0f
    private var initialTouchY: Float = 0f

    override fun onDown(e: MotionEvent): Boolean {
        initialX = layoutParams.x
        initialY = layoutParams.y
        initialTouchX = e.rawX
        initialTouchY = e.rawY
        return true
    }

    override fun onSingleTapUp(event: MotionEvent): Boolean {
        val pendingSkip =
                PresentationToModelIntents.createPendingIntent(
                    context,
                    PresentationToModelIntents.ACTION_REQUEST_SKIP,
                    alarmId,
                    widgetText
                )
            pendingSkip.send()
        return true
    }

    override fun onScroll(
        initialEvent: MotionEvent,
        finalEvent: MotionEvent,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        layoutParams.x = initialX + (initialTouchX-finalEvent.rawX).toInt()
        layoutParams.y = initialY + (finalEvent.rawY-initialTouchY).toInt()
        windowManager.updateViewLayout(widgetFloatingView, layoutParams)
        return true
    }
}
