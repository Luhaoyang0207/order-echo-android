package com.luhaoyang.orderecho.calls

import org.junit.Assert.assertEquals
import org.junit.Test

class FirstCallPresentationTest {
    @Test fun lockedFirstCallsUseAccessibilityOverlayWhenServiceIsConnected() {
        assertEquals(
            FirstCallPresentation.ACCESSIBILITY_OVERLAY,
            FirstCallPresentation.forKeyguard(isLocked = true, isAccessibilityOverlayAvailable = true)
        )
    }

    @Test fun unlockedFirstCallsKeepTheNonInteractiveOverlay() {
        assertEquals(
            FirstCallPresentation.OVERLAY,
            FirstCallPresentation.forKeyguard(isLocked = false, isAccessibilityOverlayAvailable = true)
        )
    }

    @Test fun lockedFirstCallsFallBackToSilentNotificationWithoutService() {
        assertEquals(
            FirstCallPresentation.HEADS_UP_NOTIFICATION,
            FirstCallPresentation.forKeyguard(isLocked = true, isAccessibilityOverlayAvailable = false)
        )
    }
}