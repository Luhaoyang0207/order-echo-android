package com.luhaoyang.orderecho.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.luhaoyang.orderecho.R
import com.luhaoyang.orderecho.cleanup.CleanupStartup
import com.luhaoyang.orderecho.cleanup.CleanupResult
import com.luhaoyang.orderecho.cleanup.RetentionCleaner
import com.luhaoyang.orderecho.data.AppSettings
import com.luhaoyang.orderecho.data.RecordingGrouper
import com.luhaoyang.orderecho.data.RecordingRepository
import com.luhaoyang.orderecho.playback.PlaybackController
import com.luhaoyang.orderecho.playback.RecordingDurationReader
import java.io.File

class MainActivity : AppCompatActivity(), SettingsHost {
    private lateinit var content: FrameLayout
    private lateinit var viewModel: RecordingListViewModel
    private lateinit var adapter: RecordingListAdapter
    private val progressHandler = Handler(Looper.getMainLooper())
    private var showingSettings = false
    private val progressRefresh = object : Runnable {
        override fun run() {
            if (!::viewModel.isInitialized) return
            render(viewModel.refreshPlayback())
            if (viewModel.playbackState() is com.luhaoyang.orderecho.playback.PlaybackState.Playing) {
                progressHandler.postDelayed(this, PROGRESS_REFRESH_MILLIS)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        content = findViewById(R.id.content)
        findViewById<Button>(R.id.recordings_tab).setOnClickListener { showRecordingList() }
        findViewById<Button>(R.id.settings_tab).setOnClickListener { showSettings() }
        if (hasStoragePermission()) showRecordingList() else requestStoragePermission()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_REQUEST && hasStoragePermission()) showRecordingList() else showPermissionRequired()
    }

    override fun onResume() {
        super.onResume()
        if (hasStoragePermission() && !showingSettings) {
            if (::viewModel.isInitialized) render(viewModel.refresh()) else showRecordingList()
        }
    }

    override fun onDestroy() {
        progressHandler.removeCallbacks(progressRefresh)
        if (::viewModel.isInitialized) viewModel.release()
        super.onDestroy()
    }

    private fun requestStoragePermission() {
        ActivityCompat.requestPermissions(this, STORAGE_PERMISSIONS, STORAGE_PERMISSION_REQUEST)
    }

    private fun hasStoragePermission(): Boolean = STORAGE_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun showPermissionRequired() {
        content.removeAllViews()
        LayoutInflater.from(this).inflate(R.layout.view_permission_required, content, true)
        content.findViewById<Button>(R.id.open_system_settings).setOnClickListener {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)))
        }
    }

    private fun showRecordingList() {
        showingSettings = false
        supportFragmentManager.findFragmentById(R.id.content)?.let {
            supportFragmentManager.beginTransaction().remove(it).commitNow()
        }
        if (!::viewModel.isInitialized) {
            CleanupStartup().initialize(applicationContext)
            val repository = RecordingRepository(
                baseDirectory = recordingDirectoryForTesting ?: File("/storage/emulated/0/Sounds/Callrecord/"),
                durationReader = RecordingDurationReader()::read
            )
            val settings = AppSettings(applicationContext)
            val retentionCleaner = RetentionCleaner(repository, settings)
            viewModel = RecordingListViewModel(
                repository,
                RecordingGrouper(),
                PlaybackController(repository),
                retentionCleaner::clean
            )
        }
        content.removeAllViews()
        LayoutInflater.from(this).inflate(R.layout.fragment_recording_list, content, true)
        adapter = RecordingListAdapter(
            onPlay = { recording ->
                render(viewModel.play(recording))
                scheduleProgressRefresh()
            },
            onStop = {
                progressHandler.removeCallbacks(progressRefresh)
                render(viewModel.stopPlayback())
            },
            onDelete = ::confirmDelete,
            onToggleDate = { date -> render(viewModel.toggleDate(date)) }
        )
        content.findViewById<RecyclerView>(R.id.recording_list).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
        }
        content.findViewById<Button>(R.id.refresh).setOnClickListener { render(viewModel.refresh()) }
        content.findViewById<EditText>(R.id.search_number).setOnEditorActionListener { view, _, _ ->
            render(viewModel.setQuery(view.text.toString()))
            true
        }
        content.findViewById<Button>(R.id.clear_search).setOnClickListener {
            content.findViewById<EditText>(R.id.search_number).text.clear()
            render(viewModel.clearQuery())
        }
        render(viewModel.runCleanup().listState)
    }

    private fun showSettings() {
        if (!::viewModel.isInitialized) return
        showingSettings = true
        progressHandler.removeCallbacks(progressRefresh)
        viewModel.release()
        content.removeAllViews()
        supportFragmentManager.beginTransaction()
            .replace(R.id.content, SettingsFragment())
            .commitNow()
    }

    private fun confirmDelete(recording: com.luhaoyang.orderecho.model.RecordingFile) {
        val number = recording.phoneNumber ?: getString(R.string.unknown_number)
        AlertDialog.Builder(this)
            .setMessage(getString(R.string.delete_confirmation, number))
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.confirm) { _, _ -> render(viewModel.delete(recording)) }
            .show()
    }

    override fun recordingStatistics(): RecordingStatistics = viewModel.recordingStatistics()

    override fun runCleanupFromSettings(): CleanupResult? {
        val run = viewModel.runCleanup()
        return run.cleanupResult
    }

    private fun render(state: RecordingListState) {
        when (state) {
            is RecordingListState.Content -> {
                content.findViewById<RecyclerView>(R.id.recording_list).visibility = View.VISIBLE
                content.findViewById<View>(R.id.no_matches).visibility = View.GONE
                content.findViewById<TextView>(R.id.scan_warning).apply {
                    visibility = if (state.scanFailedCount > 0) View.VISIBLE else View.GONE
                    text = getString(R.string.recordings_scan_warning, state.scanFailedCount)
                }
                adapter.submit(state.groups, viewModel.playbackState())
                content.findViewById<TextView>(R.id.recording_count_header).text = getString(
                    R.string.recording_count_header,
                    state.groups.sumOf { month -> month.dates.sumOf { it.recordings.size } }
                )
                val playbackError = viewModel.playbackState() as? com.luhaoyang.orderecho.playback.PlaybackState.Error
                if (playbackError != null) Toast.makeText(this, playbackError.message, Toast.LENGTH_SHORT).show()
            }
            RecordingListState.Empty -> showMessage(R.string.recordings_empty)
            RecordingListState.NoMatches -> {
                content.findViewById<RecyclerView>(R.id.recording_list).visibility = View.GONE
                content.findViewById<TextView>(R.id.scan_warning).visibility = View.GONE
                content.findViewById<View>(R.id.no_matches).visibility = View.VISIBLE
            }
            RecordingListState.MissingDirectory -> showMessage(R.string.recordings_missing_directory)
            RecordingListState.Error -> showMessage(R.string.recordings_load_error)
        }
    }

    private fun showMessage(messageRes: Int) {
        content.removeAllViews()
        LayoutInflater.from(this).inflate(R.layout.view_empty_recordings, content, true)
        content.findViewById<TextView>(R.id.empty_message).setText(messageRes)
        content.findViewById<Button>(R.id.retry).setOnClickListener {
            if (hasStoragePermission()) showRecordingList() else requestStoragePermission()
        }
    }

    private fun scheduleProgressRefresh() {
        progressHandler.removeCallbacks(progressRefresh)
        if (viewModel.playbackState() is com.luhaoyang.orderecho.playback.PlaybackState.Playing) {
            progressHandler.postDelayed(progressRefresh, PROGRESS_REFRESH_MILLIS)
        }
    }

    companion object {
        const val STORAGE_PERMISSION_REQUEST = 41
        const val PROGRESS_REFRESH_MILLIS = 500L
        val STORAGE_PERMISSIONS = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        internal var recordingDirectoryForTesting: File? = null
    }
}
