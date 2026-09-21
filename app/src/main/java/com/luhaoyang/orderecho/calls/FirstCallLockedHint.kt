package com.luhaoyang.orderecho.calls

import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.luhaoyang.orderecho.R

/** Uses a system-owned, number-free notification when keyguard can cover app overlays. */
internal object FirstCallLockedHint {
    fun isLocked(context: Context): Boolean =
        context.applicationContext.getSystemService(KeyguardManager::class.java).isKeyguardLocked

    fun show(context: Context): Boolean {
        val appContext = context.applicationContext
        return try {
            val notifications = appContext.getSystemService(NotificationManager::class.java)
            notifications.createNotificationChannel(NotificationChannel(
                CHANNEL, appContext.getString(R.string.first_call_title), NotificationManager.IMPORTANCE_HIGH
            ).apply {
                setSound(null, null)
                enableVibration(false)
            })
            notifications.notify(NOTIFICATION_ID, Notification.Builder(appContext, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_menu_info_details)
                .setContentTitle(appContext.getString(R.string.first_call_label))
                .setCategory(Notification.CATEGORY_CALL)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setOnlyAlertOnce(true)
                .setAutoCancel(false)
                .build())
            true
        } catch (_: RuntimeException) {
            false
        }
    }

    fun hide(context: Context) {
        context.applicationContext.getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
    }

    private const val CHANNEL = "first-call-locked-hint"
    private const val NOTIFICATION_ID = 104
}
