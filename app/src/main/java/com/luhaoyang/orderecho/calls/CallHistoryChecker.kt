package com.luhaoyang.orderecho.calls

import android.content.ContentResolver
import android.database.Cursor
import android.os.CancellationSignal
import android.provider.CallLog

/** Blocking, read-only query. The service runs this on its own worker, never on the UI thread. */
class CallHistoryChecker(
    private val query: (String, Array<String>, CancellationSignal) -> Cursor?
) {
    constructor(resolver: ContentResolver) : this({ selection, args, cancellation ->
        resolver.query(
            CallLog.Calls.CONTENT_URI,
            arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.DATE, CallLog.Calls.TYPE),
            selection, args, "${CallLog.Calls.DATE} DESC", cancellation
        )
    })

    fun check(number: String, beforeTimestamp: Long, cancellation: CancellationSignal): HistoryResult {
        return try {
            cancellation.throwIfCanceled()
            val canonical = AndroidCallNumber.canonical(number) ?: return HistoryResult.UNKNOWN
            val types = CallHistory.incomingTypes.joinToString(",")
            query(
                "${CallLog.Calls.DATE} < ? AND ${CallLog.Calls.TYPE} IN ($types)",
                arrayOf(beforeTimestamp.toString()), cancellation
            )?.use { cursor ->
                val numberColumn = cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
                val dateColumn = cursor.getColumnIndexOrThrow(CallLog.Calls.DATE)
                val typeColumn = cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE)
                val records = sequence {
                    while (true) {
                        cancellation.throwIfCanceled()
                        if (!cursor.moveToNext()) break
                        yield(HistoricalCall(cursor.getString(numberColumn), cursor.getLong(dateColumn), cursor.getInt(typeColumn)))
                    }
                }
                val previous = CallHistory.hasPreviousIncomingCall(records, canonical, beforeTimestamp, AndroidCallNumber::canonical)
                cancellation.throwIfCanceled()
                if (previous) HistoryResult.PREVIOUS else HistoryResult.FIRST
            } ?: HistoryResult.UNKNOWN
        } catch (_: RuntimeException) {
            // Permission revocation, cancelled/failed provider and malformed cursor are not FIRST.
            // Do not log the exception: provider messages can include phone numbers.
            HistoryResult.UNKNOWN
        }
    }
}
