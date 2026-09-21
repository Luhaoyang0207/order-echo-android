package com.luhaoyang.orderecho.calls

import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.luhaoyang.orderecho.R
import java.util.concurrent.atomic.AtomicBoolean

/** Posts a silent, number-free heads-up notification while Keyguard covers app overlays. */
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
            ensureChannel(appContext, notifications)
            when {
                !notifications.areNotificationsEnabled() ->
                    CallDiagnostics.record(appContext, CallDiagnosticEvent.LOCKED_HINT_NOTIFICATIONS_BLOCKED)
                notifications.getNotificationChannel(CHANNEL)?.importance ?: NotificationManager.IMPORTANCE_NONE <
                    NotificationManager.IMPORTANCE_HIGH ->
                    CallDiagnostics.record(appContext, CallDiagnosticEvent.LOCKED_HINT_CHANNEL_NOT_HIGH)
                else -> CallDiagnostics.record(appContext, CallDiagnosticEvent.LOCKED_HINT_CHANNEL_READY)
            }
            notifications.notify(NOTIFICATION_ID, Notification.Builder(appContext, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_menu_info_details)
                .setContentTitle(appContext.getString(R.string.first_call_label))
                .setCategory(Notification.CATEGORY_CALL)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .build())
            true
        } catch (_: RuntimeException) {
            active.set(false)
            false
        }
    }

    /** Opens the exact Android 8 channel used for the locked heads-up hint. */
    fun notificationSettingsIntent(context: Context): Intent {
        val appContext = context.applicationContext
        ensureChannel(appContext, appContext.getSystemService(NotificationManager::class.java))
        return Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, appContext.packageName)
            .putExtra(Settings.EXTRA_CHANNEL_ID, CHANNEL)
    }

    private fun ensureChannel(context: Context, notifications: NotificationManager) {
        notifications.createNotificationChannel(NotificationChannel(
            CHANNEL, context.getString(R.string.first_call_locked_hint_channel_title), NotificationManager.IMPORTANCE_HIGH
        ).apply {
            setLockscreenVisibility(Notification.VISIBILITY_PUBLIC)
            setSound(null, null)
            enableVibration(false)
        })
    }

    fun hide(context: Context) {
        val appContext = context.applicationContext
        active.set(false)
        appContext.getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
    }

    private const val CHANNEL = "first-call-locked-full-screen-hint"
    private const val NOTIFICATION_ID = 104
}
