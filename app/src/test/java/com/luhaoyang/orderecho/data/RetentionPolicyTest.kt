package com.luhaoyang.orderecho.data

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RetentionPolicyTest {
    @Test
    fun thirtyDaysIncludesTodayAndPreviousTwentyNineDays() {
        val today = LocalDate.of(2026, 7, 26)

        assertEquals(LocalDate.of(2026, 6, 27), RetentionPolicy.cutoffDate(today, 30))
        assertFalse(RetentionPolicy.isExpired(LocalDate.of(2026, 6, 27), today, 30))
        assertTrue(RetentionPolicy.isExpired(LocalDate.of(2026, 6, 26), today, 30))
    }

    @Test
    fun permitsOnlyTheFiveConfiguredRetentionValues() {
        assertEquals(setOf(7, 30, 60, 90, 180), RetentionPolicy.allowedDays)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsUnsupportedRetentionValue() {
        RetentionPolicy.cutoffDate(LocalDate.of(2026, 7, 26), 14)
    }
}
