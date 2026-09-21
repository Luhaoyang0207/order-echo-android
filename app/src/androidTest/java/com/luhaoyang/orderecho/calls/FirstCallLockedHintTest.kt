package com.luhaoyang.orderecho.calls

import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationManager
import android.content.Intent
import com.luhaoyang.orderecho.R
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FirstCallLockedHintTest {
    @Test fun firstCallHintUsesTheFullScreenRouteWhileKeyguardIsShowing() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val keyguard = context.getSystemService(KeyguardManager::class.java)
        assumeTrue("Run this check with the test device locked", keyguard.isKeyguardLocked)

        try {
            instrumentation.runOnMainSync { FirstCallLockedHint.show(context) }
            assertTrue(
                UiDevice.getInstance(instrumentation).wait(Until.hasObject(By.text("第一次来电")), 5_000L)
            )
        } finally {
            instrumentation.runOnMainSync { FirstCallLockedHint.hide(context) }
        }
    }
    @Test fun fullScreenHintActivityRendersOnlyTheFirstCallLabel() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        try {
            instrumentation.runOnMainSync {
                FirstCallLockedHint.show(context)
                context.startActivity(Intent().setClassName(context.packageName,
                    "com.luhaoyang.orderecho.calls.LockedFirstCallActivity")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            val device = UiDevice.getInstance(instrumentation)
            assertTrue(device.wait(
                Until.hasObject(By.res(context.packageName, "locked_first_call_hint")), 5_000L
            ))
            assertTrue(
                "The hint must remain in the top quarter, away from call controls",
                device.findObject(By.res(context.packageName, "locked_first_call_hint")).visibleBounds.bottom <
                    device.displayHeight / 4
            )
        } finally {
            instrumentation.runOnMainSync { FirstCallLockedHint.hide(context) }
        }
    }
    @Test fun lockedFirstCallNotificationUsesAFullScreenIntent() {
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
        val device = UiDevice.getInstance(instrumentation)
        try {
            instrumentation.runOnMainSync {
                FirstCallLockedHint.show(context)
                context.startActivity(Intent().setClassName(context.packageName,
                    "com.luhaoyang.orderecho.calls.LockedFirstCallActivity")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            assertTrue(device.wait(Until.hasObject(By.res(context.packageName, "locked_first_call_hint")), 5_000L))
            instrumentation.runOnMainSync { FirstCallLockedHint.hide(context) }
            assertTrue(device.wait(Until.gone(By.res(context.packageName, "locked_first_call_hint")), 5_000L))
        } finally {
            instrumentation.runOnMainSync { FirstCallLockedHint.hide(context) }
        }
    }
}
