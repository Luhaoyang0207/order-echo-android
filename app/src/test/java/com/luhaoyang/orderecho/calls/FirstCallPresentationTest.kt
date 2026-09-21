package com.luhaoyang.orderecho.calls

import org.junit.Assert.assertEquals
import org.junit.Test

class FirstCallPresentationTest {
    @Test fun lockedFirstCallsUseOnlyTheHeadsUpNotification() {
        assertEquals(FirstCallPresentation.HEADS_UP_NOTIFICATION, FirstCallPresentation.forKeyguard(true))
    }

    @Test fun unlockedFirstCallsKeepTheNonInteractiveOverlay() {
        assertEquals(FirstCallPresentation.OVERLAY, FirstCallPresentation.forKeyguard(false))
    }
}
