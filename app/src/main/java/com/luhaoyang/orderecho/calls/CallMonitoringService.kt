package com.luhaoyang.orderecho.calls

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.luhaoyang.orderecho.R
import com.luhaoyang.orderecho.ui.MainActivity

/** Keeps the proven runtime receiver registered while the user enables identification. */
class CallMonitoringService : Service() {
    private var registered = false
    private val incomingReceiver = IncomingCallReceiver()
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return
            if (!registered || !CallMonitoring.isEnabled(this@CallMonitoringService)) return
            if (intent.getStringExtra(TelephonyManager.EXTRA_STATE) == TelephonyManager.EXTRA_STATE_RINGING) {
                CallDiagnostics.record(this@CallMonitoringService, CallDiagnosticEvent.MONITOR_RINGING)
            }
            incomingReceiver.onReceive(this@CallMonitoringService, intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        showNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Satisfy the foreground deadline even for a queued start after the user disabled it.
        showNotification()
        if (intent?.action == ACTION_STOP) CallMonitoring.disable(this)
        if (!CallMonitoring.isEnabled(this) || !FirstCallPermissions.ready(this)) {
            stopSelfResult(startId)
            return START_NOT_STICKY
        }
        if (!registered) {
            try {
                ContextCompat.registerReceiver(this, receiver,
                    IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED), ContextCompat.RECEIVER_EXPORTED)
                registered = true
                isRunning = true
                CallDiagnostics.record(this, CallDiagnosticEvent.MONITOR_READY)
            } catch (_: RuntimeException) {
                CallDiagnostics.record(this, CallDiagnosticEvent.MONITOR_FAILED)
                stopSelfResult(startId)
                return START_NOT_STICKY
            }
        }
        return START_STICKY
    }

    private fun showNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = "call-monitoring"
        manager.createNotificationChannel(NotificationChannel(channel,
            getString(R.string.call_monitor_title), NotificationManager.IMPORTANCE_LOW))
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val stop = PendingIntent.getService(this, 0, Intent(this, CallMonitoringService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        startForeground(103, Notification.Builder(this, channel)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentTitle(getString(R.string.call_monitor_title))
            .setContentText(getString(R.string.call_monitor_notification))
            .setContentIntent(open)
            .addAction(Notification.Action.Builder(null, getString(R.string.call_monitor_disable), stop).build())
            .setVisibility(Notification.VISIBILITY_SECRET)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .build())
    }

    override fun onDestroy() {
        isRunning = false
        if (registered) runCatching { unregisterReceiver(receiver) }
        registered = false
        IncomingCalls.session.idle()
        IncomingCalls.service?.finishCall()
        stopForeground(STOP_FOREGROUND_REMOVE)
        CallDiagnostics.record(this, CallDiagnosticEvent.MONITOR_STOPPED)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        @Volatile var isRunning: Boolean = false
            private set
        private const val ACTION_STOP = "com.luhaoyang.orderecho.STOP_CALL_MONITORING"
    }
}
