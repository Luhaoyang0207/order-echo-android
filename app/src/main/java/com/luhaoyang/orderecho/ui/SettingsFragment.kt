package com.luhaoyang.orderecho.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import com.luhaoyang.orderecho.calls.FirstCallPermissions
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.luhaoyang.orderecho.R
import com.luhaoyang.orderecho.cleanup.CleanupResult
import com.luhaoyang.orderecho.data.AppSettings
import java.text.DateFormat
import java.util.Date
import java.util.Locale

class SettingsFragment : Fragment(R.layout.fragment_settings) {
    private val requestCallPermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        refreshCallPermissions()
    }
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
        view.findViewById<Button>(R.id.grant_call_permissions).setOnClickListener {
            requestCallPermissions.launch(FirstCallPermissions.runtime.filter {
                !FirstCallPermissions.granted(requireContext(), it)
            }.toTypedArray())
        }
        view.findViewById<Button>(R.id.grant_overlay_permission).setOnClickListener {
            openPermissionSettings(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
        }
        view.findViewById<Button>(R.id.first_call_app_settings).setOnClickListener {
            openPermissionSettings(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        }
        refreshStatus()
        refreshCallPermissions()
    }

    override fun onResume() {
        super.onResume()
        if (view != null) {
            refreshStatus()
            refreshCallPermissions()
        }
    }

    private fun refreshCallPermissions() {
        val root = view ?: return
        val context = requireContext()
        fun state(granted: Boolean) = getString(if (granted) R.string.permission_granted else R.string.permission_missing)
        root.findViewById<TextView>(R.id.first_call_permission_status).text = getString(
            R.string.first_call_permission_status,
            state(FirstCallPermissions.granted(context, Manifest.permission.READ_PHONE_STATE)),
            state(FirstCallPermissions.granted(context, Manifest.permission.READ_CALL_LOG)),
            state(Settings.canDrawOverlays(context))
        )
        root.findViewById<Button>(R.id.grant_call_permissions).isEnabled = !FirstCallPermissions.hasRuntime(context)
        root.findViewById<Button>(R.id.grant_overlay_permission).isEnabled = !Settings.canDrawOverlays(context)
        root.findViewById<Button>(R.id.first_call_app_settings).visibility =
            if (FirstCallPermissions.ready(context)) View.GONE else View.VISIBLE
        root.findViewById<Button>(R.id.run_cleanup).isEnabled = MainActivity.STORAGE_PERMISSIONS.all {
            FirstCallPermissions.granted(context, it)
        }
    }

    private fun openPermissionSettings(action: String) {
        val uri = Uri.fromParts("package", requireContext().packageName, null)
        try {
            startActivity(Intent(action, uri))
        } catch (_: RuntimeException) {
            try {
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri))
            } catch (_: RuntimeException) {
                Toast.makeText(requireContext(), R.string.first_call_settings_unavailable, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun confirmCleanup() {
        val days = settings.retentionDays()
        AlertDialog.Builder(requireContext())
            .setMessage(getString(R.string.cleanup_confirmation, days))
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.confirm) { _, _ ->
                val result = (activity as? SettingsHost)?.runCleanupFromSettings()
                when {
                    result == null ->
                        Toast.makeText(requireContext(), R.string.cleanup_failed, Toast.LENGTH_SHORT).show()
                    result.failedCount > 0 ->
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.cleanup_partial_failure, result.failedCount),
                            Toast.LENGTH_SHORT
                        ).show()
                }
                refreshStatus()
            }
            .show()
    }

    private fun refreshStatus() {
        val statistics = (activity as? SettingsHost)?.recordingStatistics() ?: RecordingStatistics(0, 0L, null)
        val oldest = statistics.oldestRecordedAt?.toLocalDate()?.let { getString(R.string.oldest_recording, it.year, it.monthValue, it.dayOfMonth) }
            ?: getString(R.string.no_recordings)
        val cleanupResult = settings.lastCleanupResult()
        val lastCleanup = cleanupResult.completedAtMillis.takeIf { it > 0L }?.let {
            DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.CHINA).format(Date(it))
        } ?: getString(R.string.never_cleaned)
        val lastResult = cleanupResult.completedAtMillis.takeIf { it > 0L }?.let {
            getString(R.string.cleanup_result, cleanupResult.deletedCount, cleanupResult.failedCount)
        } ?: getString(R.string.never_cleaned)
        status.text = getString(
            R.string.cleanup_status,
            statistics.count,
            formatSize(statistics.occupiedBytes),
            oldest,
            lastCleanup,
            lastResult
        )
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
    fun runCleanupFromSettings(): CleanupResult?
}
