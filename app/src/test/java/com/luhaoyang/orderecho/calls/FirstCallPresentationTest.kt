package com.luhaoyang.orderecho.calls

import org.junit.Assert.assertEquals
import org.junit.Test

class FirstCallPresentationTest {
    @Test fun lockedFirstCallsUseOnlyTheSystemNotification() {
        assertEquals(FirstCallPresentation.SYSTEM_NOTIFICATION, FirstCallPresentation.forKeyguard(true))
    }

    @Test fun unlockedFirstCallsKeepTheNonInteractiveOverlay() {
        assertEquals(FirstCallPresentation.OVERLAY, FirstCallPresentation.forKeyguard(false))
    }
}
