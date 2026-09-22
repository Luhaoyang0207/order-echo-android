package com.luhaoyang.orderecho.calls

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.TextView
import com.luhaoyang.orderecho.R

/**
 * User-enabled only. This service never reads AccessibilityEvent data or controls the phone.
 * Its sole purpose is to own a fixed, noninteractive lock-screen first-call label.
 */
class FirstCallAccessibilityService : AccessibilityService() {
    override fun onServiceConnected() {
        super.onServiceConnected()
        FirstCallAccessibilityOverlay.connect(this)
        CallDiagnostics.record(this, CallDiagnosticEvent.ACCESSIBILITY_SERVICE_CONNECTED)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        FirstCallAccessibilityOverlay.disconnect(this)
        CallDiagnostics.record(this, CallDiagnosticEvent.ACCESSIBILITY_SERVICE_DISCONNECTED)
        super.onDestroy()
    }
}

/** Main-thread window owner available only while [FirstCallAccessibilityService] is connected. */
internal object FirstCallAccessibilityOverlay {
    private var service: FirstCallAccessibilityService? = null
    private var windowManager: WindowManager? = null
    private var view: TextView? = null

    @Synchronized fun connect(connectedService: FirstCallAccessibilityService) {
        hide()
        service = connectedService
        windowManager = connectedService.getSystemService(WindowManager::class.java)
    }

    @Synchronized fun disconnect(disconnectedService: FirstCallAccessibilityService) {
        if (service !== disconnectedService) return
        hide()
        windowManager = null
        service = null
    }

    @Synchronized fun isAvailable(): Boolean = service != null && windowManager != null

    @Synchronized fun isShowing(): Boolean = view != null

    @Synchronized fun show(): Boolean {
        if (view != null) return true
        val context = service ?: return false
        val manager = windowManager ?: return false
        val label = createLabel(context)
        return try {
            manager.addView(label, createLayoutParams(context))
            view = label
            true
        } catch (_: RuntimeException) {
            runCatching { manager.removeViewImmediate(label) }
            false
        }
    }

    @Synchronized fun hide() {
        val current = view ?: return
        view = null
        val manager = windowManager ?: return
        runCatching { manager.removeViewImmediate(current) }
    }

    internal fun createLayoutParams(context: Context): WindowManager.LayoutParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        y = dp(context, 64)
        alpha = 0.9f
    }

    private fun createLabel(context: Context): TextView = TextView(context).apply {
        setText(R.string.first_call_label)
        textSize = 22f
        setTextColor(Color.WHITE)
        setTypeface(typeface, Typeface.BOLD)
        gravity = Gravity.CENTER
        setPadding(dp(context, 18), dp(context, 8), dp(context, 18), dp(context, 8))
        background = GradientDrawable().apply {
            setColor(Color.rgb(35, 53, 42))
            cornerRadius = dp(context, 12).toFloat()
        }
        importantForAccessibility = android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    private fun dp(context: Context, value: Int): Int =
        (value * context.resources.displayMetrics.density).toInt()
}