package com.luhaoyang.orderecho.data

import com.luhaoyang.orderecho.model.RecordingFile
import java.time.LocalDate
import java.time.YearMonth

data class DateGroup(val date: LocalDate, val recordings: List<RecordingFile>)

data class MonthGroup(val month: YearMonth, val dates: List<DateGroup>)

class RecordingGrouper {
    fun group(recordings: List<RecordingFile>): List<MonthGroup> {
        return recordings
            .sortedByDescending { it.recordedAt }
            .groupBy { YearMonth.from(it.recordedAt) }
            .entries
            .sortedByDescending { it.key }
            .map { (month, monthRecordings) ->
                MonthGroup(
                    month = month,
                    dates = monthRecordings
                        .groupBy { it.recordedAt.toLocalDate() }
                        .entries
                        .sortedByDescending { it.key }
                        .map { (date, dateRecordings) -> DateGroup(date, dateRecordings) }
                )
            }
    }
}
