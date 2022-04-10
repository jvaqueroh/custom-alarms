package com.better.alarm.background

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.ImageView
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
    private lateinit var closeIconView: ImageView
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

        val layoutParams = setupLayoutParams()
        setupWidgetView(layoutParams)
        setupCloseIconView()

        val alarmLabel = intent!!.getStringExtra(Intents.EXTRA_LABEL)
        setupWidgetText(alarmLabel)

        val alarmId = intent.getIntExtra(Intents.EXTRA_ID, -1)
        setupTouchAndDrag(layoutParams, alarmId)

        return START_STICKY
    }

    private fun setupCloseIconView() {
        closeIconView = ImageView(this)
        closeIconView.setImageResource(R.drawable.ic_baseline_delete_24)
        closeIconView.visibility = View.INVISIBLE
        val height = windowManager.defaultDisplay.height
        val width = windowManager.defaultDisplay.width
        val closeIconParams = WindowManager.LayoutParams(
            140,
            140,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        closeIconParams.gravity = Gravity.BOTTOM or Gravity.CENTER
        closeIconParams.y = 100
        windowManager.addView(closeIconView, closeIconParams)
    }

    private fun setupWidgetView(layoutParams: WindowManager.LayoutParams) {
        widgetFloatingView = LayoutInflater.from(this).inflate(R.layout.skip_widget, null)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager.apply {
            addView(widgetFloatingView, layoutParams)
        }
        widgetFloatingView.visibility = View.VISIBLE
    }

    private fun setupLayoutParams(): WindowManager.LayoutParams {
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
        return layoutParams
    }

    private fun setupWidgetText(alarmLabel: String?) {
        var widgetText = getString(R.string.skip)
        if (!alarmLabel.isNullOrBlank()) {
            widgetText += "\n" + alarmLabel
        }
        val widgetTextView = widgetFloatingView.findViewById<TextView>(R.id.text_widget)
        widgetTextView.text = widgetText
    }

    private fun setupTouchAndDrag(
        layoutParams: WindowManager.LayoutParams,
        alarmId: Int) {
        mGestureDetector = GestureDetectorCompat(
            this,
            TouchAndScrollGestureListener(windowManager, layoutParams, widgetFloatingView, this, alarmId, "", closeIconView)
        )

        widgetFloatingView.setOnTouchListener { _, event ->
            mGestureDetector.onTouchEvent(event)

            when(event.action){
                MotionEvent.ACTION_UP->{
                    closeIconView.visibility = View.GONE
                    false
                }
            }

            true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (widgetFloatingView != null)
            windowManager.removeView(widgetFloatingView)
        if(closeIconView != null)
            windowManager.removeView(closeIconView)
    }
}

private class TouchAndScrollGestureListener(
    val windowManager: WindowManager,
    val layoutParams: WindowManager.LayoutParams,
    val widgetFloatingView: View,
    val context: Context,
    val alarmId: Int,
    val widgetText: String,
    val closeImageView: ImageView
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

        closeImageView.visibility = View.VISIBLE

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
