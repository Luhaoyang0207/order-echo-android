package com.luhaoyang.orderecho.data

import java.time.LocalDate

object RetentionPolicy {
    val allowedDays: Set<Int> = setOf(7, 30, 60, 90, 180)

    fun cutoffDate(today: LocalDate, days: Int): LocalDate {
        require(days in allowedDays)
        return today.minusDays((days - 1).toLong())
    }

    fun isExpired(recordedDate: LocalDate, today: LocalDate, days: Int): Boolean {
        return recordedDate.isBefore(cutoffDate(today, days))
    }
}
