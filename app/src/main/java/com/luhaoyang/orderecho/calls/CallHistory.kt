package com.luhaoyang.orderecho.calls

import android.provider.CallLog

enum class HistoryResult { FIRST, PREVIOUS, UNKNOWN }

data class HistoricalCall(val number: String?, val date: Long, val type: Int)

object CallHistory {
    val incomingTypes = setOf(
        CallLog.Calls.INCOMING_TYPE,
        CallLog.Calls.MISSED_TYPE,
        CallLog.Calls.REJECTED_TYPE,
        CallLog.Calls.BLOCKED_TYPE
    )

    fun hasPreviousIncomingCall(
        calls: Sequence<HistoricalCall>,
        canonicalNumber: String,
        beforeTimestamp: Long,
        canonicalize: (String?) -> String?
    ): Boolean = calls.any {
        it.date < beforeTimestamp && it.type in incomingTypes &&
            canonicalize(it.number) == canonicalNumber
    }
}
