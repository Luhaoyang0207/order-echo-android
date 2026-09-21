package com.luhaoyang.orderecho.calls

import org.junit.Assert.assertEquals
import org.junit.Test

class FirstCallPresentationTest {
    @Test fun lockedFirstCallsUseOnlyTheFullScreenNotification() {
        assertEquals(FirstCallPresentation.FULL_SCREEN_NOTIFICATION, FirstCallPresentation.forKeyguard(true))
    }

    @Test fun unlockedFirstCallsKeepTheNonInteractiveOverlay() {
        assertEquals(FirstCallPresentation.OVERLAY, FirstCallPresentation.forKeyguard(false))
    }
}
