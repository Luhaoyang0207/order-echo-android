package com.luhaoyang.orderecho.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.luhaoyang.orderecho.R
import com.luhaoyang.orderecho.model.RecordingFile
import com.luhaoyang.orderecho.playback.PlaybackState
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class RecordingListAdapter(
    private val onPlay: (RecordingFile) -> Unit,
    private val onStop: () -> Unit,
    private val onDelete: (RecordingFile) -> Unit,
    private val onToggleDate: (LocalDate) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var items: List<Row> = emptyList()
    private var playbackState: PlaybackState = PlaybackState.Idle

    fun submit(groups: List<VisibleMonthGroup>, playbackState: PlaybackState) {
        this.playbackState = playbackState
        items = buildList {
            groups.forEach { month ->
                add(Row.Month(month.month.year, month.month.monthValue))
                month.dates.forEach { date ->
                    add(Row.Date(date.date, date.recordings.size, date.expanded))
                    if (date.expanded) date.recordings.forEach { add(Row.Recording(it)) }
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
            DATE -> DateHolder(inflater.inflate(R.layout.item_date_header, parent, false))
            else -> RecordingHolder(inflater.inflate(R.layout.item_recording, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is Row.Month -> (holder as TextHolder).text.text = holder.itemView.context.getString(R.string.month_header, item.year, item.month)
            is Row.Date -> bindDate(holder as DateHolder, item)
            is Row.Recording -> bindRecording(holder as RecordingHolder, item.recording)
        }
    }

    override fun getItemCount() = items.size

    private fun bindDate(holder: DateHolder, item: Row.Date) {
        val context = holder.itemView.context
        holder.text.text = when (item.date) {
            LocalDate.now() -> context.getString(R.string.today_date_header, item.count)
            LocalDate.now().minusDays(1) -> context.getString(R.string.yesterday_date_header, item.count)
            else -> context.getString(R.string.date_header, item.date.monthValue, item.date.dayOfMonth, item.count)
        }
        holder.indicator.setText(if (item.expanded) R.string.collapse_date else R.string.expand_date)
        val toggle = View.OnClickListener { onToggleDate(item.date) }
        holder.itemView.setOnClickListener(toggle)
        holder.text.setOnClickListener(toggle)
    }

    private fun bindRecording(holder: RecordingHolder, recording: RecordingFile) {
        val context = holder.itemView.context
        holder.number.text = recording.phoneNumber ?: context.getString(R.string.unknown_number)
        val active = playbackState.takeIf { it.fileOrNull() == recording.file }
        val position = active?.positionMillis() ?: 0
        val duration = active?.durationMillis()?.takeIf { it > 0 }
            ?: recording.durationMillis
            ?: 0
        holder.metadata.text = "${recording.recordedAt.format(TIME_FORMAT)} · ${if (duration > 0) formatDuration(duration) else context.getString(R.string.duration_unknown)}"
        val isActive = active != null
        holder.progress.visibility = if (isActive) View.VISIBLE else View.GONE
        holder.time.visibility = if (isActive) View.VISIBLE else View.GONE
        holder.stop.visibility = if (isActive) View.VISIBLE else View.GONE
        if (isActive) {
            holder.progress.max = duration.coerceAtLeast(1)
            holder.progress.progress = position.coerceAtMost(holder.progress.max)
            holder.time.text = "${formatDuration(position)} / ${if (duration > 0) formatDuration(duration) else context.getString(R.string.duration_unknown)}"
        }
        holder.playPause.text = if (active is PlaybackState.Playing) context.getString(R.string.pause) else context.getString(R.string.play)
        holder.playPause.setOnClickListener { onPlay(recording) }
        holder.stop.setOnClickListener { onStop() }
        holder.delete.setOnClickListener { onDelete(recording) }
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
    private class DateHolder(view: View) : RecyclerView.ViewHolder(view) {
        val indicator: TextView = view.findViewById(R.id.expand_indicator)
        val text: TextView = view.findViewById(R.id.date_header)
    }
    private class RecordingHolder(view: View) : RecyclerView.ViewHolder(view) {
        val number: TextView = view.findViewById(R.id.phone_number)
        val metadata: TextView = view.findViewById(R.id.recording_metadata)
        val progress: ProgressBar = view.findViewById(R.id.playback_progress)
        val time: TextView = view.findViewById(R.id.playback_time)
        val playPause: Button = view.findViewById(R.id.play_pause)
        val stop: Button = view.findViewById(R.id.stop_playback)
        val delete: Button = view.findViewById(R.id.delete_recording)
    }

    private sealed interface Row {
        data class Month(val year: Int, val month: Int) : Row
        data class Date(val date: LocalDate, val count: Int, val expanded: Boolean) : Row
        data class Recording(val recording: RecordingFile) : Row
    }

    private companion object {
        const val MONTH = 0
        const val DATE = 1
        const val RECORDING = 2
        val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}
