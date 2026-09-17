package com.luhaoyang.orderecho.calls

import org.junit.Assert.*
import org.junit.Test

class IncomingCallSessionTest {
    private val session = IncomingCallSession(CallNumber::canonical)

    @Test fun duplicateRingingUsesOneLookupAndOriginalCutoff() {
        val request = session.ringing("91234567", 100_000)!!
        assertEquals(95_000L, request.beforeTimestamp)
        assertNull(session.ringing("+4791234567", 110_000))
        assertTrue(session.acceptResult(request, HistoryResult.FIRST))
        assertFalse(session.acceptResult(request, HistoryResult.FIRST))
    }

    @Test fun blankBroadcastCanPrecedeNumberWithoutMovingTheCutoff() {
        assertNull(session.ringing(null, 100_000))
        val request = session.ringing("91234567", 102_000)!!
        assertEquals(95_000L, request.beforeTimestamp)
        assertNull(session.ringing(null, 103_000))
        assertTrue(session.acceptResult(request, HistoryResult.FIRST))
    }

    @Test fun hiddenNumberNeverStartsALookup() {
        assertNull(session.ringing("Private", 100_000))
        assertNull(session.ringing("", 101_000))
    }

    @Test fun offhookInvalidatesLateResultAndDoesNotRehandleTheCall() {
        val request = session.ringing("91234567", 100_000)!!
        session.offhook()
        assertFalse(session.acceptResult(request, HistoryResult.FIRST))
        assertNull(session.ringing("91234567", 110_000))
    }

    @Test fun idleAllowsTheNextCallButRejectsTheOldQueryResult() {
        val old = session.ringing("91234567", 100_000)!!
        session.idle()
        val next = session.ringing("91234567", 200_000)!!
        assertFalse(session.acceptResult(old, HistoryResult.FIRST))
        assertTrue(session.acceptResult(next, HistoryResult.FIRST))
        assertEquals(195_000L, next.beforeTimestamp)
    }

    @Test fun timeoutDoesNotPermitDuplicateRingingToShowAgain() {
        val request = session.ringing("91234567", 100_000)!!
        session.dismiss(request)
        assertFalse(session.acceptResult(request, HistoryResult.FIRST))
        assertNull(session.ringing("91234567", 120_000))
    }

    @Test fun oldServiceCleanupCannotDismissANewCall() {
        val old = session.ringing("91234567", 100_000)!!
        session.idle()
        val next = session.ringing("92345678", 200_000)!!
        session.dismiss(old)
        assertTrue(session.acceptResult(next, HistoryResult.FIRST))
    }

    @Test fun previousOrUnavailableHistoryNeverShowsAnOverlay() {
        for (result in listOf(HistoryResult.PREVIOUS, HistoryResult.UNKNOWN)) {
            session.idle()
            val request = session.ringing("91234567", 100_000)!!
            assertFalse(session.acceptResult(request, result))
            assertFalse(session.acceptResult(request, HistoryResult.FIRST))
        }
    }
}
