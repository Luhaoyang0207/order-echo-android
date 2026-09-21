package com.luhaoyang.orderecho.calls

import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.luhaoyang.orderecho.R
import java.util.concurrent.atomic.AtomicBoolean

/** Uses Android's full-screen notification route when Keyguard can cover app overlays. */
internal object FirstCallLockedHint {
    private val active = AtomicBoolean(false)

    fun isLocked(context: Context): Boolean =
        context.applicationContext.getSystemService(KeyguardManager::class.java).isKeyguardLocked

    fun isActive(): Boolean = active.get()

    fun show(context: Context): Boolean {
        val appContext = context.applicationContext
        active.set(true)
        return try {
            val notifications = appContext.getSystemService(NotificationManager::class.java)
            notifications.createNotificationChannel(NotificationChannel(
                CHANNEL, appContext.getString(R.string.first_call_title), NotificationManager.IMPORTANCE_HIGH
            ).apply {
                setLockscreenVisibility(Notification.VISIBILITY_PUBLIC)
                setSound(null, null)
                enableVibration(false)
            })
            when {
                !notifications.areNotificationsEnabled() ->
                    CallDiagnostics.record(appContext, CallDiagnosticEvent.LOCKED_HINT_NOTIFICATIONS_BLOCKED)
                notifications.getNotificationChannel(CHANNEL)?.importance ?: NotificationManager.IMPORTANCE_NONE <
                    NotificationManager.IMPORTANCE_HIGH ->
                    CallDiagnostics.record(appContext, CallDiagnosticEvent.LOCKED_HINT_CHANNEL_NOT_HIGH)
                else -> CallDiagnostics.record(appContext, CallDiagnosticEvent.LOCKED_HINT_CHANNEL_READY)
            }
            val fullScreenIntent = PendingIntent.getActivity(appContext, NOTIFICATION_ID,
                Intent(appContext, LockedFirstCallActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            notifications.notify(NOTIFICATION_ID, Notification.Builder(appContext, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_menu_info_details)
                .setContentTitle(appContext.getString(R.string.first_call_label))
                .setCategory(Notification.CATEGORY_CALL)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .setFullScreenIntent(fullScreenIntent, true)
                .build())
            true
        } catch (_: RuntimeException) {
            active.set(false)
            false
        }
    }

    fun hide(context: Context) {
        val appContext = context.applicationContext
        active.set(false)
        appContext.getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
        appContext.sendBroadcast(Intent(LockedFirstCallActivity.ACTION_DISMISS).setPackage(appContext.packageName))
    }

    private const val CHANNEL = "first-call-locked-full-screen-hint"
    private const val NOTIFICATION_ID = 104
}
