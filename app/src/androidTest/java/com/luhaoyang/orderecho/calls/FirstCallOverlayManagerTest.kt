package com.luhaoyang.orderecho.calls

import android.provider.Settings
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
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
            val overlay = FirstCallOverlayManager(context)
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

    @Test fun staysVisibleUntilItsOwnerHidesIt() {
        assumeTrue("Grant overlay permission on the test device", Settings.canDrawOverlays(context))
        lateinit var overlay: FirstCallOverlayManager
        instrumentation.runOnMainSync {
            overlay = FirstCallOverlayManager(context)
            assertTrue(overlay.show())
        }
        try {
            Thread.sleep(13_000)
            instrumentation.runOnMainSync { assertTrue(overlay.isShowing()) }
        } finally {
            instrumentation.runOnMainSync { overlay.hide() }
        }
    }
}
