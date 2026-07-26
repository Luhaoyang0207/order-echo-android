package com.luhaoyang.orderecho.data

import com.luhaoyang.orderecho.model.RecordingFile
import java.io.File
import java.time.LocalDateTime
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Test

class RecordingGrouperTest {
    @Test
    fun groupingSortsNewestMonthThenNewestDate() {
        val files = listOf(
            recording("june", LocalDateTime.of(2026, 6, 30, 12, 0)),
            recording("july-old", LocalDateTime.of(2026, 7, 2, 12, 0)),
            recording("july-new", LocalDateTime.of(2026, 7, 26, 12, 0))
        )

        val groups = RecordingGrouper().group(files)

        assertEquals(YearMonth.of(2026, 7), groups.first().month)
        assertEquals(26, groups.first().dates.first().date.dayOfMonth)
        assertEquals("july-new", groups.first().dates.first().recordings.single().file.name)
    }

    private fun recording(name: String, recordedAt: LocalDateTime) = RecordingFile(
        file = File(name),
        phoneNumber = null,
        recordedAt = recordedAt,
        sizeBytes = 0
    )
}
