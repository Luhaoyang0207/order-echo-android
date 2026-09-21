package com.luhaoyang.orderecho.calls

import android.app.Notification
import android.app.NotificationManager
import android.provider.Settings
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.luhaoyang.orderecho.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FirstCallLockedHintTest {
    @Test fun lockedFirstCallNotificationNeverLaunchesAnActivity() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        try {
            instrumentation.runOnMainSync { assertTrue(FirstCallLockedHint.show(context)) }
            val hint = findHint(context)
            assertNotNull(hint)
            assertNull(
                "D9 must not launch an Activity over Huawei's incoming-call UI",
                hint?.notification?.fullScreenIntent
            )
            assertNull("D9 must not open the app when the notification is tapped", hint?.notification?.contentIntent)
        } finally {
            instrumentation.runOnMainSync { FirstCallLockedHint.hide(context) }
        }
    }

    @Test fun hidingTheHintRemovesItsNotification() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        instrumentation.runOnMainSync { assertTrue(FirstCallLockedHint.show(context)) }
        assertNotNull(findHint(context))
        instrumentation.runOnMainSync { FirstCallLockedHint.hide(context) }
        assertFalse(context.getSystemService(NotificationManager::class.java).activeNotifications.any {
            it.notification.extras.getCharSequence(Notification.EXTRA_TITLE) == context.getString(R.string.first_call_label)
        })
    }

    @Test fun lockedHintSettingsIntentTargetsOnlyItsChannel() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = FirstCallLockedHint.notificationSettingsIntent(context)
        assertEquals(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS, intent.action)
        assertEquals(context.packageName, intent.getStringExtra(Settings.EXTRA_APP_PACKAGE))
        assertEquals("first-call-locked-full-screen-hint", intent.getStringExtra(Settings.EXTRA_CHANNEL_ID))
    }

    @Test fun lockedHintUsesItsOwnNamedNotificationChannel() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        try {
            instrumentation.runOnMainSync { assertTrue(FirstCallLockedHint.show(context)) }
            val manager = context.getSystemService(NotificationManager::class.java)
            assertTrue(manager.notificationChannels.any { it.name == "首次来电识别（锁屏提示）" })
        } finally {
            instrumentation.runOnMainSync { FirstCallLockedHint.hide(context) }
        }
    }

    private fun findHint(context: android.content.Context) =
        context.getSystemService(NotificationManager::class.java).activeNotifications.firstOrNull {
            it.notification.extras.getCharSequence(Notification.EXTRA_TITLE) == context.getString(R.string.first_call_label)
        }
}
