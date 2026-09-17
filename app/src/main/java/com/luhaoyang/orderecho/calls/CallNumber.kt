package com.luhaoyang.orderecho.calls

/** Full caller identity, deliberately separate from recording substring search. */
object CallNumber {
    fun canonical(raw: String?): String? {
        val value = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        // Do not convert letters (Private/Unknown/SIP) to keypad digits or accept CLIR codes.
        if (value.any { it !in '0'..'9' && it !in "+-()." && !it.isWhitespace() }) return null
        val compact = value.filter { it in '0'..'9' || it == '+' }
        val international = if (compact.startsWith("00")) "+${compact.drop(2)}" else compact
        val digits = international.removePrefix("+")
        if (digits.length !in 7..15 || digits.any { it !in '0'..'9' }) return null
        return when {
            international.startsWith("+") && digits.startsWith("0") -> null
            international.startsWith("+") -> international
            digits.length == 8 -> "+47$digits"
            else -> digits
        }
    }
}
