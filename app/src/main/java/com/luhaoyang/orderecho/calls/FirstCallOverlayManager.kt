package com.luhaoyang.orderecho.calls

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView
import com.luhaoyang.orderecho.R

/** Main-thread owned window, with application context so it cannot retain an Activity. */
class FirstCallOverlayManager(context: Context) {
    private val appContext = context.applicationContext
    private val windowManager = appContext.getSystemService(WindowManager::class.java)
    private var view: TextView? = null

    fun isShowing(): Boolean = view != null

    fun show(@androidx.annotation.StringRes labelResource: Int = R.string.first_call_label): Boolean {
        if (isShowing()) return true
        if (!Settings.canDrawOverlays(appContext)) return false
        val label = TextView(appContext).apply {
            setText(labelResource)
            textSize = 24f
            setTextColor(Color.WHITE)
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(dp(20), dp(10), dp(20), dp(10))
            background = GradientDrawable().apply {
                setColor(Color.rgb(35, 53, 42))
                cornerRadius = dp(12).toFloat()
            }
            importantForAccessibility = android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            // Leave room for the dialer's caller heading and number above the hint.
            y = dp(128)
            // Also avoids opaque untrusted-window touch blocking on newer Android versions.
            alpha = 0.8f
        }
        return try {
            windowManager.addView(label, params)
            view = label
            callDebug("Overlay shown")
            true
        } catch (_: RuntimeException) {
            // Includes permission revocation and invalid window tokens; leave no retained view.
            runCatching { windowManager.removeViewImmediate(label) }
            false
        }
    }

    fun hide() {
        val current = view ?: return
        view = null
        runCatching { windowManager.removeViewImmediate(current) }
        callDebug("Overlay hidden")
    }

    private fun dp(value: Int): Int = (value * appContext.resources.displayMetrics.density).toInt()
}
