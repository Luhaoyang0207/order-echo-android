package com.luhaoyang.orderecho.calls

import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.view.WindowManager
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import com.luhaoyang.orderecho.R
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FirstCallLockedHintTest {
    @Test fun lockedHintWindowLeavesFocusAndTouchesToThePhoneWindow() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        try {
            instrumentation.runOnMainSync { FirstCallLockedHint.show(context) }
            ActivityScenario.launch<LockedFirstCallActivity>(
                Intent(context, LockedFirstCallActivity::class.java)
            ).use { scenario ->
                scenario.onActivity { activity ->
                    val flags = activity.window.attributes.flags
                    assertTrue(flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE != 0)
                    assertTrue(flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE != 0)
                }
            }
        } finally {
            instrumentation.runOnMainSync { FirstCallLockedHint.hide(context) }
        }
    }

    @Test fun firstCallHintUsesTheFullScreenRouteWhileKeyguardIsShowing() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val keyguard = context.getSystemService(KeyguardManager::class.java)
        assumeTrue("Run this check with the test device locked", keyguard.isKeyguardLocked)
        val preferences = context.getSharedPreferences("first_call_diagnostics", 0)
        val original = preferences.getString("events", null)
        try {
            CallDiagnostics.clear(context)
            instrumentation.runOnMainSync { FirstCallLockedHint.show(context) }
            assertTrue(waitForDiagnostic(context, "锁屏提示页面已启动"))
        } finally {
            instrumentation.runOnMainSync { FirstCallLockedHint.hide(context) }
            preferences.edit().putString("events", original).commit()
        }
    }

    @Test fun fullScreenHintActivityRendersOnlyTheFirstCallLabel() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        try {
            instrumentation.runOnMainSync { FirstCallLockedHint.show(context) }
            ActivityScenario.launch<LockedFirstCallActivity>(
                Intent(context, LockedFirstCallActivity::class.java)
            ).use { scenario ->
                scenario.onActivity { activity ->
                    val label = activity.findViewById<TextView>(R.id.locked_first_call_hint)
                    val location = IntArray(2)
                    label.getLocationOnScreen(location)
                    assertTrue(label.isShown)
                    assertEquals(context.getString(R.string.first_call_label), label.text)
                    assertTrue(
                        "The hint must remain in the top quarter, away from call controls",
                        location[1] + label.height < activity.resources.displayMetrics.heightPixels / 4
                    )
                }
            }
        } finally {
            instrumentation.runOnMainSync { FirstCallLockedHint.hide(context) }
        }
    }
    @Test fun lockedFirstCallNotificationUsesAFullScreenIntent() {
        assumeTrue(
            "This assertion covers the Android 8 full-screen notification contract",
            Build.VERSION.SDK_INT <= Build.VERSION_CODES.O_MR1
        )
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        try {
            instrumentation.runOnMainSync { assertTrue(FirstCallLockedHint.show(context)) }
            val hint = context.getSystemService(NotificationManager::class.java).activeNotifications.firstOrNull {
                it.notification.extras.getCharSequence(Notification.EXTRA_TITLE) == context.getString(R.string.first_call_label)
            }
            assertNotNull("The locked hint must use Android's full-screen notification route", hint?.notification?.fullScreenIntent)
        } finally {
            instrumentation.runOnMainSync { FirstCallLockedHint.hide(context) }
        }
    }

    @Test fun hidingTheHintClosesItsVisibleActivity() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val preferences = context.getSharedPreferences("first_call_diagnostics", 0)
        val original = preferences.getString("events", null)
        try {
            CallDiagnostics.clear(context)
            instrumentation.runOnMainSync {
                FirstCallLockedHint.show(context)
                context.startActivity(Intent(context, LockedFirstCallActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            assertTrue(waitForDiagnostic(context, "锁屏提示页面已启动"))
            instrumentation.runOnMainSync { FirstCallLockedHint.hide(context) }
            assertTrue(waitForDiagnostic(context, "锁屏提示页面已停止"))
        } finally {
            instrumentation.runOnMainSync { FirstCallLockedHint.hide(context) }
            preferences.edit().putString("events", original).commit()
        }
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
    @Test fun visibleLockedHintActivityRecordsItsLaunch() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val preferences = context.getSharedPreferences("first_call_diagnostics", 0)
        val original = preferences.getString("events", null)
        try {
            CallDiagnostics.clear(context)
            instrumentation.runOnMainSync {
                FirstCallLockedHint.show(context)
                context.startActivity(Intent(context, LockedFirstCallActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            assertTrue(waitForDiagnostic(context, "锁屏提示页面已创建"))
            assertTrue(waitForDiagnostic(context, "锁屏提示页面已启动"))
        } finally {
            instrumentation.runOnMainSync { FirstCallLockedHint.hide(context) }
            preferences.edit().putString("events", original).commit()
        }
    }

    private fun waitForDiagnostic(context: Context, text: String): Boolean {
        repeat(20) {
            if (CallDiagnostics.report(context).contains(text)) return true
            Thread.sleep(250L)
        }
        return false
    }
}
