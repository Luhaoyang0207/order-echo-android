package com.luhaoyang.orderecho.calls

import android.app.AppOpsManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Process
import android.telephony.PhoneStateListener
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.luhaoyang.orderecho.R
import com.luhaoyang.orderecho.ui.MainActivity

/** Explicit, debug-only observation. Never feeds the production session or queries Call Log. */
@Suppress("DEPRECATION")
class CallReceptionProbeService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val listeners = mutableListOf<Pair<TelephonyManager, PhoneStateListener>>()
    private var active = false
    private var receiverRegistered = false
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (!active || intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return
            when (intent.getStringExtra(TelephonyManager.EXTRA_STATE)) {
                TelephonyManager.EXTRA_STATE_RINGING -> {
                    record(CallDiagnosticEvent.PROBE_RECEIVER_RINGING)
                    recordNumberPresence(intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER))
                }
                TelephonyManager.EXTRA_STATE_IDLE -> record(CallDiagnosticEvent.PROBE_RECEIVER_IDLE)
                TelephonyManager.EXTRA_STATE_OFFHOOK -> record(CallDiagnosticEvent.PROBE_RECEIVER_OFFHOOK)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        showNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        showNotification()
        if (intent?.action == ACTION_STOP) {
            stopSelfResult(startId)
            return START_NOT_STICKY
        }
        if (active) {
            record(CallDiagnosticEvent.PROBE_ALREADY_RUNNING)
            return START_NOT_STICKY
        }
        active = true
        CallDiagnostics.clear(this)
        record(CallDiagnosticEvent.PROBE_STARTED)
        handler.postDelayed({
            record(CallDiagnosticEvent.PROBE_TIMEOUT)
            stopSelf()
        }, 60_000L)
        recordPhoneAccess()
        try {
            ContextCompat.registerReceiver(this, receiver,
                IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED), ContextCompat.RECEIVER_EXPORTED)
            receiverRegistered = true
            record(CallDiagnosticEvent.PROBE_RECEIVER_READY)
        } catch (_: RuntimeException) {
            record(CallDiagnosticEvent.PROBE_RECEIVER_FAILED)
        }
        val manager = getSystemService(TelephonyManager::class.java)
        if (manager != null) {
            listen(manager, CallDiagnosticEvent.PROBE_LISTENER_RINGING)
            // A different active SIM may not be the default subscription. IDs remain transient.
            try {
                val subscriptions = getSystemService(SubscriptionManager::class.java).activeSubscriptionInfoList.orEmpty()
                subscriptions.distinctBy { it.subscriptionId }.take(2).forEach {
                    val ringingEvent = when (it.simSlotIndex) {
                        0 -> CallDiagnosticEvent.PROBE_SIM1_RINGING
                        1 -> CallDiagnosticEvent.PROBE_SIM2_RINGING
                        else -> CallDiagnosticEvent.PROBE_LISTENER_RINGING
                    }
                    listen(manager.createForSubscriptionId(it.subscriptionId), ringingEvent)
                }
            } catch (_: RuntimeException) {
                record(CallDiagnosticEvent.PROBE_SIM_FAILED)
            }
        } else record(CallDiagnosticEvent.PROBE_LISTENER_FAILED)
        return START_NOT_STICKY
    }

    private fun listen(manager: TelephonyManager, ringingEvent: CallDiagnosticEvent) {
        val listener = object : PhoneStateListener() {
            override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                if (!active) return
                when (state) {
                    TelephonyManager.CALL_STATE_RINGING -> {
                        record(ringingEvent)
                        recordNumberPresence(phoneNumber)
                    }
                    TelephonyManager.CALL_STATE_IDLE -> record(CallDiagnosticEvent.PROBE_LISTENER_IDLE)
                    TelephonyManager.CALL_STATE_OFFHOOK -> record(CallDiagnosticEvent.PROBE_LISTENER_OFFHOOK)
                }
            }
        }
        // Retain even if registration throws, so a partially registered listener is cleaned up.
        listeners += manager to listener
        try {
            manager.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
            record(CallDiagnosticEvent.PROBE_LISTENER_READY)
        } catch (_: RuntimeException) {
            record(CallDiagnosticEvent.PROBE_LISTENER_FAILED)
        }
    }

    private fun recordNumberPresence(number: String?) {
        record(if (number.isNullOrBlank()) CallDiagnosticEvent.PROBE_NUMBER_MISSING else CallDiagnosticEvent.PROBE_NUMBER_PRESENT)
    }

    private fun recordPhoneAccess() {
        try {
            val mode = getSystemService(AppOpsManager::class.java)
                .checkOpNoThrow(AppOpsManager.OPSTR_READ_PHONE_STATE, Process.myUid(), packageName)
            record(when (mode) {
                AppOpsManager.MODE_ALLOWED -> CallDiagnosticEvent.PROBE_PHONE_ACCESS_ALLOWED
                AppOpsManager.MODE_IGNORED, AppOpsManager.MODE_ERRORED -> CallDiagnosticEvent.PROBE_PHONE_ACCESS_BLOCKED
                AppOpsManager.MODE_DEFAULT -> CallDiagnosticEvent.PROBE_PHONE_ACCESS_DEFAULT
                else -> CallDiagnosticEvent.PROBE_PHONE_ACCESS_UNKNOWN
            })
        } catch (_: RuntimeException) {
            record(CallDiagnosticEvent.PROBE_PHONE_ACCESS_UNKNOWN)
        }
    }

    private fun record(event: CallDiagnosticEvent) = CallDiagnostics.record(this, event)

    private fun showNotification() {
        val channel = "call-reception-probe"
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(channel, getString(R.string.first_call_probe_title), NotificationManager.IMPORTANCE_LOW))
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val stop = PendingIntent.getService(this, 0, Intent(this, CallReceptionProbeService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        startForeground(102, Notification.Builder(this, channel)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentTitle(getString(R.string.first_call_probe_title))
            .setContentText(getString(R.string.first_call_probe_notification))
            .setContentIntent(open)
            .addAction(Notification.Action.Builder(null, getString(R.string.first_call_probe_stop), stop).build())
            .setVisibility(Notification.VISIBILITY_SECRET)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .build())
    }

    override fun onDestroy() {
        val wasActive = active
        active = false
        handler.removeCallbacksAndMessages(null)
        var cleanupFailed = false
        if (receiverRegistered) runCatching { unregisterReceiver(receiver) }.onFailure { cleanupFailed = true }
        listeners.forEach { (manager, listener) ->
            runCatching { manager.listen(listener, PhoneStateListener.LISTEN_NONE) }.onFailure { cleanupFailed = true }
        }
        listeners.clear()
        stopForeground(STOP_FOREGROUND_REMOVE)
        if (cleanupFailed) record(CallDiagnosticEvent.PROBE_CLEANUP_FAILED)
        if (wasActive) record(CallDiagnosticEvent.PROBE_STOPPED)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private companion object {
        const val ACTION_STOP = "com.luhaoyang.orderecho.STOP_CALL_RECEPTION_PROBE"
    }
}
