package com.luhaoyang.orderecho.model

import java.io.File
import java.time.LocalDateTime

data class RecordingFile(
    val file: File,
    val phoneNumber: String?,
    val recordedAt: LocalDateTime,
    val sizeBytes: Long
)
