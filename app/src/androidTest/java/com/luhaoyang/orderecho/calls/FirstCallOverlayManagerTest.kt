package com.luhaoyang.orderecho.calls

import android.provider.Settings
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FirstCallOverlayManagerTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    @Test fun repeatedShowAndHideUseOneWindowWithoutLeakingOrCrashing() {
        assumeTrue("Grant overlay permission on the test device", Settings.canDrawOverlays(context))
        instrumentation.runOnMainSync {
            val overlay = FirstCallOverlayManager(context) {}
            try {
                assertTrue(overlay.show())
                assertTrue(overlay.show())
                assertTrue(overlay.isShowing())
                overlay.hide()
                overlay.hide()
                assertFalse(overlay.isShowing())
                assertTrue(overlay.show())
            } finally {
                overlay.hide()
            }
        }
    }

    @Test fun timeoutRemovesWindowAndNotifiesItsOwner() {
        assumeTrue("Grant overlay permission on the test device", Settings.canDrawOverlays(context))
        val expired = CountDownLatch(1)
        lateinit var overlay: FirstCallOverlayManager
        instrumentation.runOnMainSync {
            overlay = FirstCallOverlayManager(context) { expired.countDown() }
            assertTrue(overlay.show())
        }
        try {
            assertTrue(expired.await(15, TimeUnit.SECONDS))
            instrumentation.runOnMainSync { assertFalse(overlay.isShowing()) }
        } finally {
            instrumentation.runOnMainSync { overlay.hide() }
        }
    }
}
