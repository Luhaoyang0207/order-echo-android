package com.luhaoyang.orderecho.ui

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.luhaoyang.orderecho.R
import com.luhaoyang.orderecho.data.AppSettings
import java.text.DateFormat
import java.util.Date
import java.util.Locale

class SettingsFragment : Fragment(R.layout.fragment_settings) {
    private lateinit var settings: AppSettings
    private lateinit var retentionChoices: RadioGroup
    private lateinit var status: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        settings = AppSettings(requireContext().applicationContext)
        retentionChoices = view.findViewById(R.id.retention_choices)
        status = view.findViewById(R.id.cleanup_status)
        retentionChoices.check(retentionId(settings.retentionDays()))
        retentionChoices.setOnCheckedChangeListener { _, checkedId ->
            settings.setRetentionDays(retentionDays(checkedId))
            refreshStatus()
        }
        view.findViewById<Button>(R.id.run_cleanup).setOnClickListener { confirmCleanup() }
        refreshStatus()
    }

    override fun onResume() {
        super.onResume()
        if (::status.isInitialized) refreshStatus()
    }

    private fun confirmCleanup() {
        val days = settings.retentionDays()
        AlertDialog.Builder(requireContext())
            .setMessage(getString(R.string.cleanup_confirmation, days))
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.confirm) { _, _ ->
                if ((activity as? SettingsHost)?.runCleanupFromSettings() == true) refreshStatus()
                else Toast.makeText(requireContext(), R.string.cleanup_failed, Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun refreshStatus() {
        val statistics = (activity as? SettingsHost)?.recordingStatistics() ?: RecordingStatistics(0, 0L, null)
        val oldest = statistics.oldestRecordedAt?.toLocalDate()?.let { getString(R.string.oldest_recording, it.year, it.monthValue, it.dayOfMonth) }
            ?: getString(R.string.no_recordings)
        val lastCleanup = settings.lastCleanupAt().takeIf { it > 0L }?.let {
            DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.CHINA).format(Date(it))
        } ?: getString(R.string.never_cleaned)
        status.text = getString(R.string.cleanup_status, statistics.count, formatSize(statistics.occupiedBytes), oldest, lastCleanup)
    }

    private fun retentionDays(id: Int): Int = when (id) {
        R.id.retention_7 -> 7
        R.id.retention_30 -> 30
        R.id.retention_60 -> 60
        R.id.retention_90 -> 90
        R.id.retention_180 -> 180
        else -> error("Unsupported retention selection")
    }

    private fun retentionId(days: Int): Int = when (days) {
        7 -> R.id.retention_7
        30 -> R.id.retention_30
        60 -> R.id.retention_60
        90 -> R.id.retention_90
        180 -> R.id.retention_180
        else -> R.id.retention_30
    }

    private fun formatSize(bytes: Long): String = when {
        bytes < 1024L -> "$bytes B"
        bytes < 1024L * 1024L -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
        else -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
    }
}

interface SettingsHost {
    fun recordingStatistics(): RecordingStatistics
    fun runCleanupFromSettings(): Boolean
}
