package com.luhaoyang.orderecho.calls

data class CallLookup(val token: Long, val number: String, val beforeTimestamp: Long)

/** Main-thread only. No disk storage; survives service stops to reject duplicate broadcasts. */
class IncomingCallSession(private val canonicalize: (String?) -> String?) {
    private var generation = 0L
    private var ringStartedAt: Long? = null
    private var ringing = false
    private var ended = false
    private var handled = false
    private var resultConsumed = false
    private var current: CallLookup? = null

    fun ringing(number: String?, receivedAt: Long): CallLookup? {
        if (ended) return null
        if (ringStartedAt == null) {
            ringStartedAt = receivedAt
            ringing = true
            generation++
        }
        if (handled) return null
        val normalized = canonicalize(number) ?: return null
        handled = true
        return CallLookup(generation, normalized, ringStartedAt!! - SAFETY_MARGIN_MILLIS)
            .also { current = it }
    }

    fun isCurrent(request: CallLookup): Boolean = ringing && current == request

    fun acceptResult(request: CallLookup, result: HistoryResult): Boolean {
        if (!isCurrent(request) || resultConsumed) return false
        resultConsumed = true
        return result == HistoryResult.FIRST
    }

    fun dismiss(request: CallLookup) {
        if (current == request) {
            current = null
            resultConsumed = true
        }
    }

    fun offhook() {
        ringing = false
        ended = true
        current = null
    }

    fun idle() {
        ringing = false
        ended = false
        handled = false
        resultConsumed = false
        ringStartedAt = null
        current = null
    }

    private companion object {
        const val SAFETY_MARGIN_MILLIS = 5_000L
    }
}
