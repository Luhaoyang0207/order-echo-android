package com.luhaoyang.orderecho.data

import com.luhaoyang.orderecho.model.RecordingFile
import java.io.File
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class RecordingRepository(val baseDirectory: File) {
    fun list(): List<RecordingFile> {
        return baseDirectory.listFiles()
            ?.asSequence()
            ?.filter(::isSafeRecordingFile)
            ?.map(::toRecordingFile)
            ?.toList()
            ?: emptyList()
    }

    fun delete(recording: RecordingFile): Boolean {
        val file = recording.file
        return isSafeRecordingFile(file) && file.delete()
    }

    private fun isSafeRecordingFile(file: File): Boolean {
        val canonicalBaseDirectory = baseDirectory.canonicalFile
        val canonicalFile = file.canonicalFile
        val directChildPrefix = canonicalBaseDirectory.path + File.separator

        return canonicalFile.path.startsWith(directChildPrefix) &&
            canonicalFile.parentFile == canonicalBaseDirectory &&
            canonicalFile.isFile &&
            canonicalFile.name.endsWith(AMR_EXTENSION, ignoreCase = true)
    }

    private fun toRecordingFile(file: File): RecordingFile {
        val match = HUAWEI_FILE_NAME.matchEntire(file.name)
        val parsedDateTime = match?.let {
            runCatching {
                LocalDateTime.parse(
                    "${it.groupValues[2]}_${it.groupValues[3]}",
                    HUAWEI_DATE_TIME_FORMAT
                )
            }.getOrNull()
        }

        return RecordingFile(
            file = file,
            phoneNumber = match?.groupValues?.get(1)?.takeIf(String::isNotBlank),
            recordedAt = parsedDateTime ?: lastModifiedDateTime(file),
            sizeBytes = file.length()
        )
    }

    private fun lastModifiedDateTime(file: File): LocalDateTime {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(file.lastModified()), ZoneId.systemDefault())
    }

    private companion object {
        const val AMR_EXTENSION = ".amr"
        val HUAWEI_FILE_NAME = Regex("^(.+?)_(\\d{8})_(\\d{6})\\.amr$", RegexOption.IGNORE_CASE)
        val HUAWEI_DATE_TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
    }
}
