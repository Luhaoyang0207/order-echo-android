package com.luhaoyang.orderecho.calls

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.CancellationSignal
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.telephony.TelephonyManager
import com.luhaoyang.orderecho.R
import com.luhaoyang.orderecho.ui.MainActivity
import java.util.concurrent.Executors
import java.util.concurrent.Future

/** Exists only for a lookup/overlay, never records or controls the phone call. */
class IncomingCallService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var overlay: FirstCallOverlayManager
    private var request: CallLookup? = null
    private var cancellation: CancellationSignal? = null
    private var query: Future<*>? = null
    private var destroyed = false
    private var latestStartId = 0
    private val deadline = Runnable { finishCall() }
    private val checkState = object : Runnable {
        override fun run() {
            if (request == null) return
            if (!isStillRinging() || !FirstCallPermissions.ready(this@IncomingCallService)) {
                finishCall()
            } else {
                handler.postDelayed(this, 500L)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        IncomingCalls.service = this
        overlay = FirstCallOverlayManager(applicationContext, ::finishCall)
        showServiceNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        latestStartId = startId
        // Even an already-ended queued start must satisfy startForegroundService's deadline.
        showServiceNotification()
        val incoming = intent?.let {
            val number = it.getStringExtra(NUMBER) ?: return@let null
            CallLookup(it.getLongExtra(TOKEN, -1), number, it.getLongExtra(BEFORE, 0))
        }
        if (incoming == null || !IncomingCalls.session.isCurrent(incoming) ||
            !FirstCallPermissions.ready(this) || !isStillRinging()) {
            // A stale queued start must not tear down a newer active call.
            if (request?.let(IncomingCalls.session::isCurrent) != true) finishCall()
            return START_NOT_STICKY
        }
        if (request == incoming) return START_NOT_STICKY
        clearWork()
        request = incoming
        val signal = CancellationSignal().also { cancellation = it }
        handler.postDelayed(deadline, 15_000L)
        handler.post(checkState)
        callDebug("Checking previous calls")
        query = executor.submit {
            val result = CallHistoryChecker(contentResolver).check(incoming.number, incoming.beforeTimestamp, signal)
            if (!signal.isCanceled) handler.post {
                if (!destroyed && request == incoming) {
                    val show = IncomingCalls.session.acceptResult(incoming, result)
                    callDebug(when (result) {
                        HistoryResult.FIRST -> "First incoming call detected"
                        HistoryResult.PREVIOUS -> "Previous incoming call found"
                        HistoryResult.UNKNOWN -> "Call history unavailable"
                    })
                    if (!show || !FirstCallPermissions.ready(this) || !isStillRinging() || !overlay.show()) {
                        finishCall()
                    }
                }
            }
        }
        return START_NOT_STICKY
    }

    internal fun finishCall() {
        request?.let(IncomingCalls.session::dismiss)
        request = null
        clearWork()
        stopForeground(STOP_FOREGROUND_REMOVE)
        // Do not stop a newer start which Android has queued but not delivered yet.
        stopSelfResult(latestStartId)
    }

    private fun clearWork() {
        handler.removeCallbacksAndMessages(null)
        cancellation?.cancel()
        cancellation = null
        query?.cancel(true)
        query = null
        overlay.hide()
    }

    @Suppress("DEPRECATION")
    private fun isStillRinging(): Boolean = try {
        getSystemService(TelephonyManager::class.java).callState == TelephonyManager.CALL_STATE_RINGING
    } catch (_: RuntimeException) {
        false
    }

    private fun showServiceNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(
            CHANNEL, getString(R.string.first_call_title), NotificationManager.IMPORTANCE_LOW
        ))
        val openApp = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        startForeground(NOTIFICATION_ID, Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentTitle(getString(R.string.first_call_title))
            .setContentText(getString(R.string.first_call_notification))
            .setContentIntent(openApp)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setVisibility(Notification.VISIBILITY_SECRET)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .build())
    }

    override fun onDestroy() {
        destroyed = true
        request?.let(IncomingCalls.session::dismiss)
        request = null
        clearWork()
        executor.shutdownNow()
        if (IncomingCalls.service === this) IncomingCalls.service = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL = "first-call-check"
        private const val NOTIFICATION_ID = 101
        private const val NUMBER = "number"
        private const val TOKEN = "token"
        private const val BEFORE = "before"

        fun intent(context: Context, request: CallLookup): Intent =
            Intent(context, IncomingCallService::class.java)
                .putExtra(NUMBER, request.number)
                .putExtra(TOKEN, request.token)
                .putExtra(BEFORE, request.beforeTimestamp)
    }
}
