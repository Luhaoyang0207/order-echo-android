package com.luhaoyang.orderecho.calls

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
import com.luhaoyang.orderecho.BuildConfig

/** PHONE_STATE is exempt from Android 8's manifest implicit-broadcast restriction. */
class IncomingCallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val receivedAt = System.currentTimeMillis()
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return
        when (intent.getStringExtra(TelephonyManager.EXTRA_STATE)) {
            TelephonyManager.EXTRA_STATE_IDLE -> {
                CallDiagnostics.record(context, CallDiagnosticEvent.IDLE)
                IncomingCalls.session.idle()
                IncomingCalls.service?.finishCall()
                callDebug("Call state IDLE")
            }
            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                CallDiagnostics.record(context, CallDiagnosticEvent.OFFHOOK)
                IncomingCalls.session.offhook()
                IncomingCalls.service?.finishCall()
                callDebug("Call state OFFHOOK")
            }
            TelephonyManager.EXTRA_STATE_RINGING -> {
                CallDiagnostics.record(context, CallDiagnosticEvent.RINGING)
                if (!FirstCallPermissions.ready(context)) {
                    CallDiagnostics.record(context, CallDiagnosticEvent.PERMISSIONS_MISSING)
                    return
                }
                // A blank broadcast may arrive before the numbered one. Keep its earlier time.
                @Suppress("DEPRECATION")
                val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
                val request = IncomingCalls.session.ringing(number, receivedAt)
                if (request == null) {
                    CallDiagnostics.record(context, when {
                        number.isNullOrBlank() -> CallDiagnosticEvent.NUMBER_MISSING
                        AndroidCallNumber.canonical(number) == null -> CallDiagnosticEvent.NUMBER_INVALID
                        else -> CallDiagnosticEvent.SESSION_IGNORED
                    })
                    return
                }
                callDebug("Incoming call detected; incoming number normalized")
                try {
                    context.startForegroundService(IncomingCallService.intent(context, request))
                    CallDiagnostics.record(context, CallDiagnosticEvent.SERVICE_REQUESTED)
                } catch (_: RuntimeException) {
                    IncomingCalls.session.dismiss(request)
                    CallDiagnostics.record(context, CallDiagnosticEvent.SERVICE_FAILED)
                    callDebug("Call identification service unavailable")
                }
            }
        }
    }
}

/** All access is on the main thread; no Activity, database or persisted phone numbers. */
internal object IncomingCalls {
    val session = IncomingCallSession(AndroidCallNumber::canonical)
    var service: IncomingCallService? = null
}

internal fun callDebug(message: String) {
    if (BuildConfig.DEBUG) Log.d("OrderEchoCalls", message)
}
