package com.luhaoyang.orderecho.playback

import android.media.MediaMetadataRetriever
import java.io.File

class RecordingDurationReader(
    private val retrieverFactory: () -> MediaMetadataRetriever = { MediaMetadataRetriever() }
) {
    fun read(file: File): Int? {
        val retriever = retrieverFactory()
        return try {
            retriever.setDataSource(file.path)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?.takeIf { it > 0L }
                ?.coerceAtMost(Int.MAX_VALUE.toLong())
                ?.toInt()
        } catch (_: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }
}
