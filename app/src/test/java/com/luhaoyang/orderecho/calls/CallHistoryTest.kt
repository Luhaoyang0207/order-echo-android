package com.luhaoyang.orderecho.calls

import org.junit.Assert.*
import org.junit.Test

class CallHistoryTest {
    private fun previous(vararg calls: HistoricalCall) = CallHistory.hasPreviousIncomingCall(
        calls.asSequence(), "+4791234567", 95_000, CallNumber::canonical
    )

    @Test fun emptyOrOutgoingOnlyHistoryMeansNoPreviousIncomingCall() {
        assertFalse(previous())
        assertFalse(previous(HistoricalCall("91234567", 10_000, 2)))
    }

    @Test fun answeredMissedRejectedAndBlockedInboundCallsCount() {
        listOf(1, 3, 5, 6).forEach { type ->
            assertTrue(previous(HistoricalCall("0047 912 34 567", 10_000, type)))
        }
    }

    @Test fun currentCallAndSafetyMarginRowsAreNeverHistory() {
        listOf(95_000L, 99_999L, 100_000L, 100_001L).forEach { date ->
            assertFalse(previous(HistoricalCall("91234567", date, 1)))
        }
        assertTrue(previous(HistoricalCall("91234567", 94_999, 1)))
    }

    @Test fun unrelatedUnknownAndNonIncomingRowsDoNotHideFirstCall() {
        assertFalse(previous(
            HistoricalCall("+4691234567", 10_000, 1),
            HistoricalCall(null, 10_000, 3),
            HistoricalCall("91234567", 10_000, 4),
            HistoricalCall("91234567", 10_000, 7)
        ))
    }

    @Test fun matchingRecordStopsReadingTheCursorSequenceImmediately() {
        val calls = sequence {
            yield(HistoricalCall("91234567", 10_000, 3))
            error("Must stop before reading another record")
        }
        assertTrue(CallHistory.hasPreviousIncomingCall(calls, "+4791234567", 95_000, CallNumber::canonical))
    }

    @Test fun thousandsOfUnrelatedRecordsStillFindAnOlderMatch() {
        val calls = (1..5000).asSequence().map { HistoricalCall("+442079460123", 10_000, 1) } +
            sequenceOf(HistoricalCall("91234567", 9_000, 5))
        assertTrue(CallHistory.hasPreviousIncomingCall(calls, "+4791234567", 95_000, CallNumber::canonical))
    }
}
