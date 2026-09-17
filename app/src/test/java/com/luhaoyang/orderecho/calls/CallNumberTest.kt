package com.luhaoyang.orderecho.calls

import org.junit.Assert.*
import org.junit.Test

class CallNumberTest {
    @Test fun norwegianFormatsHaveTheSameFullIdentity() {
        listOf("91234567", "+4791234567", "004791234567", "+47 912 34 567", "(912) 34-567")
            .forEach { assertEquals("+4791234567", CallNumber.canonical(it)) }
    }

    @Test fun differentCountriesAndSimilarSuffixesAreNotTheSameCaller() {
        assertNotEquals(CallNumber.canonical("+4691234567"), CallNumber.canonical("91234567"))
        assertNotEquals(CallNumber.canonical("191234567"), CallNumber.canonical("91234567"))
        assertNotEquals(CallNumber.canonical("91234568"), CallNumber.canonical("91234567"))
        assertEquals("+442079460123", CallNumber.canonical("0044 20 7946 0123"))
    }

    @Test fun unknownAndMalformedCallerIdsAreIgnored() {
        listOf(null, "", "   ", "Private", "Unknown", "-1", "-2", "-3", "0", "123", "*31#91234567", "sip:91234567", "123abc4567", "++4791234567", "+").forEach {
            assertNull("Must ignore $it", CallNumber.canonical(it))
        }
    }
}
