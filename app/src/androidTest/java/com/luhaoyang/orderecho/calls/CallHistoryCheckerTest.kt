package com.luhaoyang.orderecho.calls

import android.database.MatrixCursor
import android.os.CancellationSignal
import android.provider.CallLog
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Synthetic cursors only: never reads or changes the device's real Call Log. */
@RunWith(AndroidJUnit4::class)
class CallHistoryCheckerTest {
    @Test fun androidNormalizationMatchesNorwayWithoutForeignSuffixCollision() {
        listOf("91234567", "+4791234567", "004791234567", "+47 912 34 567").forEach {
            assertEquals("+4791234567", AndroidCallNumber.canonical(it))
        }
        assertNotEquals(AndroidCallNumber.canonical("+4691234567"), AndroidCallNumber.canonical("91234567"))
        assertNull(AndroidCallNumber.canonical("Private"))
        assertNull(AndroidCallNumber.canonical("-1"))
    }

    @Test fun queryUsesOriginalCutoffIncomingTypesAndClosesAfterEarlyMatch() {
        val cursor = cursor().apply {
            addRow(arrayOf("91234567", 10_000L, CallLog.Calls.MISSED_TYPE))
            addRow(arrayOf("92345678", 5_000L, CallLog.Calls.INCOMING_TYPE))
        }
        val checker = CallHistoryChecker { selection, args, _ ->
            assertEquals("date < ? AND type IN (1,3,5,6)", selection)
            assertArrayEquals(arrayOf("95000"), args)
            cursor
        }
        assertEquals(HistoryResult.PREVIOUS, checker.check("+4791234567", 95_000, CancellationSignal()))
        assertTrue(cursor.isClosed)
        assertEquals(0, cursor.position)
    }

    @Test fun currentAndOutgoingRowsCannotCountAsPreviousCalls() {
        val cursor = cursor().apply {
            addRow(arrayOf("91234567", 100_000L, CallLog.Calls.INCOMING_TYPE))
            addRow(arrayOf("91234567", 10_000L, CallLog.Calls.OUTGOING_TYPE))
        }
        assertEquals(HistoryResult.FIRST, CallHistoryChecker { _, _, _ -> cursor }
            .check("+4791234567", 95_000, CancellationSignal()))
        assertTrue(cursor.isClosed)
    }

    @Test fun nullDeniedBrokenAndCancelledQueriesAreUnknownNeverFirst() {
        assertEquals(HistoryResult.UNKNOWN, CallHistoryChecker { _, _, _ -> null }
            .check("+4791234567", 95_000, CancellationSignal()))
        assertEquals(HistoryResult.UNKNOWN, CallHistoryChecker { _, _, _ -> throw SecurityException() }
            .check("+4791234567", 95_000, CancellationSignal()))
        val malformed = MatrixCursor(arrayOf("wrong_column"))
        assertEquals(HistoryResult.UNKNOWN, CallHistoryChecker { _, _, _ -> malformed }
            .check("+4791234567", 95_000, CancellationSignal()))
        assertTrue(malformed.isClosed)
        val cancelled = CancellationSignal().apply { cancel() }
        assertEquals(HistoryResult.UNKNOWN, CallHistoryChecker { _, _, _ -> error("Do not query after cancellation") }
            .check("+4791234567", 95_000, cancelled))
    }

    private fun cursor() = MatrixCursor(arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.DATE, CallLog.Calls.TYPE))
}
