package com.luhaoyang.orderecho.calls

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import com.luhaoyang.orderecho.R

/** A small, visible full-screen-notification target used only while Keyguard is active. */
class LockedFirstCallActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private val dismiss = Runnable { finishAndRemoveTask() }
    private var receiverRegistered = false
    private val dismissReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = finishAndRemoveTask()
    }

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CallDiagnostics.record(this, CallDiagnosticEvent.LOCKED_ACTIVITY_CREATED)
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )
        if (!FirstCallLockedHint.isActive()) {
            finishAndRemoveTask()
            return
        }
        setContentView(R.layout.activity_locked_first_call_hint)
        window.setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT)
        window.setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL)
        window.attributes = window.attributes.apply { y = 48.dp }
    }

    override fun onStart() {
        super.onStart()
        CallDiagnostics.record(this, CallDiagnosticEvent.LOCKED_ACTIVITY_STARTED)
        if (!FirstCallLockedHint.isActive()) {
            finishAndRemoveTask()
            return
        }
        registerReceiver(dismissReceiver, IntentFilter(ACTION_DISMISS))
        receiverRegistered = true
        handler.postDelayed(dismiss, DISPLAY_TIMEOUT_MS)
    }

    override fun onStop() {
        CallDiagnostics.record(this, CallDiagnosticEvent.LOCKED_ACTIVITY_STOPPED)
        handler.removeCallbacks(dismiss)
        if (receiverRegistered) {
            unregisterReceiver(dismissReceiver)
            receiverRegistered = false
        }
        super.onStop()
    }

    private val Int.dp: Int get() = (this * resources.displayMetrics.density).toInt()

    companion object {
        const val ACTION_DISMISS = "com.luhaoyang.orderecho.calls.DISMISS_LOCKED_FIRST_CALL"
        private const val DISPLAY_TIMEOUT_MS = 12_000L
    }
}
