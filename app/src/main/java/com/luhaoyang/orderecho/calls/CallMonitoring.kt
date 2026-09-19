package com.luhaoyang.orderecho.calls

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences

/** User intent only; never persist live call state or a caller number. Call on the main thread. */
object CallMonitoring {
    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences("call_monitoring", Context.MODE_PRIVATE).getBoolean("enabled", false)

    fun observe(context: Context, changed: () -> Unit): () -> Unit {
        val prefs = context.getSharedPreferences("call_monitoring", Context.MODE_PRIVATE)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "enabled" || key == null) changed()
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        return { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    fun enable(context: Context): Boolean {
        if (!FirstCallPermissions.ready(context)) return false
        context.getSharedPreferences("call_monitoring", Context.MODE_PRIVATE).edit().putBoolean("enabled", true).apply()
        return restoreIfEnabled(context).also { if (!it) disable(context) }
    }

    fun restoreIfEnabled(context: Context): Boolean {
        if (!isEnabled(context)) return false
        if (!FirstCallPermissions.ready(context)) {
            context.stopService(Intent(context, CallMonitoringService::class.java))
            return false
        }
        return try {
            val started = context.startForegroundService(Intent(context, CallMonitoringService::class.java)) != null
            if (!started) CallDiagnostics.record(context, CallDiagnosticEvent.MONITOR_FAILED)
            started
        } catch (_: RuntimeException) {
            CallDiagnostics.record(context, CallDiagnosticEvent.MONITOR_FAILED)
            false
        }
    }

    fun disable(context: Context) {
        context.getSharedPreferences("call_monitoring", Context.MODE_PRIVATE).edit().putBoolean("enabled", false).apply()
        IncomingCalls.session.idle()
        IncomingCalls.service?.finishCall()
        context.stopService(Intent(context, CallMonitoringService::class.java))
    }
}

/** Restore only an explicitly enabled choice after unlock/reboot or an APK update. */
class CallMonitoringRecoveryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            CallMonitoring.restoreIfEnabled(context)
        }
    }
}
