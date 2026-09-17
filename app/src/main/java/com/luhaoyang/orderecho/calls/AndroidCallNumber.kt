package com.luhaoyang.orderecho.calls

import android.telephony.PhoneNumberUtils

object AndroidCallNumber {
    fun canonical(raw: String?): String? {
        // Validate before PhoneNumberUtils can translate letters to keypad digits.
        val candidate = CallNumber.canonical(raw) ?: return null
        val normalized = PhoneNumberUtils.normalizeNumber(candidate)
        return PhoneNumberUtils.formatNumberToE164(normalized, "NO")
            ?: CallNumber.canonical(normalized)
    }
}
