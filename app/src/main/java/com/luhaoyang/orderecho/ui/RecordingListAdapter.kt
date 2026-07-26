package com.luhaoyang.orderecho.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.luhaoyang.orderecho.R
import com.luhaoyang.orderecho.data.MonthGroup
import com.luhaoyang.orderecho.model.RecordingFile
import com.luhaoyang.orderecho.playback.PlaybackState
import java.time.format.DateTimeFormatter
import java.util.Locale

class RecordingListAdapter(
    private val onPlay: (RecordingFile) -> Unit,
    private val onDelete: (RecordingFile) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var items: List<Row> = emptyList()
    private var playbackState: PlaybackState = PlaybackState.Idle

    fun submit(groups: List<MonthGroup>, playbackState: PlaybackState) {
        this.playbackState = playbackState
        items = buildList {
            groups.forEach { month ->
                add(Row.Month(month.month.year, month.month.monthValue))
                month.dates.forEach { date ->
                    add(Row.Date(date.date.monthValue, date.date.dayOfMonth, date.recordings.size))
                    date.recordings.forEach { add(Row.Recording(it)) }
                }
            }
        }
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int) = when (items[position]) {
        is Row.Month -> MONTH
        is Row.Date -> DATE
        is Row.Recording -> RECORDING
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            MONTH -> TextHolder(inflater.inflate(R.layout.item_month_header, parent, false))
            DATE -> TextHolder(inflater.inflate(R.layout.item_date_header, parent, false))
            else -> RecordingHolder(inflater.inflate(R.layout.item_recording, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is Row.Month -> (holder as TextHolder).text.text = holder.itemView.context.getString(R.string.month_header, item.year, item.month)
            is Row.Date -> (holder as TextHolder).text.text = holder.itemView.context.getString(R.string.date_header, item.month, item.day, item.count)
            is Row.Recording -> bindRecording(holder as RecordingHolder, item.recording)
        }
    }

    override fun getItemCount() = items.size

    private fun bindRecording(holder: RecordingHolder, recording: RecordingFile) {
        val context = holder.itemView.context
        holder.number.text = recording.phoneNumber ?: context.getString(R.string.unknown_number)
        holder.metadata.text = "${recording.recordedAt.format(TIME_FORMAT)} · ${formatSize(recording.sizeBytes)}"
        val active = playbackState.takeIf { it.fileOrNull() == recording.file }
        val position = active?.positionMillis() ?: 0
        val duration = active?.durationMillis() ?: 0
        holder.progress.max = duration.coerceAtLeast(1)
        holder.progress.progress = position.coerceAtMost(holder.progress.max)
        holder.time.text = "${formatDuration(position)} / ${if (duration > 0) formatDuration(duration) else context.getString(R.string.duration_unknown)}"
        holder.playPause.text = if (active is PlaybackState.Playing) context.getString(R.string.pause) else context.getString(R.string.play)
        holder.playPause.setOnClickListener { onPlay(recording) }
        holder.delete.setOnClickListener { onDelete(recording) }
    }

    private fun formatSize(bytes: Long): String = when {
        bytes < 1024L -> "$bytes B"
        bytes < 1024L * 1024L -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
        else -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
    }

    private fun formatDuration(millis: Int): String = "%d:%02d".format(millis / 60000, (millis / 1000) % 60)

    private fun PlaybackState.fileOrNull() = when (this) {
        is PlaybackState.Playing -> file
        is PlaybackState.Paused -> file
        else -> null
    }

    private fun PlaybackState.positionMillis() = when (this) {
        is PlaybackState.Playing -> positionMillis
        is PlaybackState.Paused -> positionMillis
        else -> 0
    }

    private fun PlaybackState.durationMillis() = when (this) {
        is PlaybackState.Playing -> durationMillis
        is PlaybackState.Paused -> durationMillis
        else -> 0
    }

    private class TextHolder(view: View) : RecyclerView.ViewHolder(view) { val text: TextView = view as TextView }
    private class RecordingHolder(view: View) : RecyclerView.ViewHolder(view) {
        val number: TextView = view.findViewById(R.id.phone_number)
        val metadata: TextView = view.findViewById(R.id.recording_metadata)
        val progress: ProgressBar = view.findViewById(R.id.playback_progress)
        val time: TextView = view.findViewById(R.id.playback_time)
        val playPause: Button = view.findViewById(R.id.play_pause)
        val delete: Button = view.findViewById(R.id.delete_recording)
    }

    private sealed interface Row {
        data class Month(val year: Int, val month: Int) : Row
        data class Date(val month: Int, val day: Int, val count: Int) : Row
        data class Recording(val recording: RecordingFile) : Row
    }

    private companion object {
        const val MONTH = 0
        const val DATE = 1
        const val RECORDING = 2
        val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}
