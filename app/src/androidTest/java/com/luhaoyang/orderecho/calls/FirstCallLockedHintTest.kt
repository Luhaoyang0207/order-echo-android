package com.luhaoyang.orderecho.calls

import android.app.KeyguardManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FirstCallLockedHintTest {
    @Test fun firstCallHintUsesASystemNotificationWhileKeyguardIsShowing() {
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
}
