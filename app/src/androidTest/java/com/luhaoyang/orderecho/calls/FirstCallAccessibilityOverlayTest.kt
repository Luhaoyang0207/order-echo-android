package com.luhaoyang.orderecho.calls

import android.view.WindowManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FirstCallAccessibilityOverlayTest {
    @Test fun layoutIsAnUntouchableAccessibilityOverlay() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val params = FirstCallAccessibilityOverlay.createLayoutParams(context)
        val nonInteractive = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE

        assertEquals(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY, params.type)
        assertEquals(nonInteractive, params.flags and nonInteractive)
        assertTrue(params.y > 0)
    }
}