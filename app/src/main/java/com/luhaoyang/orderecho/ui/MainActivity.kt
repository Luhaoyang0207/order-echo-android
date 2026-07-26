package com.luhaoyang.orderecho.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.luhaoyang.orderecho.R
import com.luhaoyang.orderecho.cleanup.CleanupStartup
import com.luhaoyang.orderecho.cleanup.RetentionCleaner
import com.luhaoyang.orderecho.data.AppSettings
import com.luhaoyang.orderecho.data.RecordingGrouper
import com.luhaoyang.orderecho.data.RecordingRepository
import com.luhaoyang.orderecho.playback.PlaybackController
import java.io.File

class MainActivity : AppCompatActivity() {
    private lateinit var content: FrameLayout
    private lateinit var viewModel: RecordingListViewModel
    private lateinit var adapter: RecordingListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        content = findViewById(R.id.content)
        if (hasStoragePermission()) showRecordingList() else requestStoragePermission()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_REQUEST && hasStoragePermission()) showRecordingList() else showPermissionRequired()
    }

    override fun onDestroy() {
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
        CleanupStartup().initialize(applicationContext)
        val repository = RecordingRepository(File("/storage/emulated/0/Sounds/Callrecord/"))
        val settings = AppSettings(applicationContext)
        viewModel = RecordingListViewModel(
            repository,
            RecordingGrouper(),
            PlaybackController(repository),
            RetentionCleaner(repository, settings)
        )
        content.removeAllViews()
        LayoutInflater.from(this).inflate(R.layout.fragment_recording_list, content, true)
        adapter = RecordingListAdapter(
            onPlay = { recording -> render(viewModel.play(recording)) },
            onDelete = { recording -> render(viewModel.delete(recording)) }
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
        render(viewModel.runCleanup())
    }

    private fun render(state: RecordingListState) {
        when (state) {
            is RecordingListState.Content -> {
                content.findViewById<RecyclerView>(R.id.recording_list).visibility = View.VISIBLE
                adapter.submit(state.groups, viewModel.playbackState())
            }
            RecordingListState.Empty -> showMessage(R.string.recordings_empty)
            RecordingListState.MissingDirectory -> showMessage(R.string.recordings_missing_directory)
            RecordingListState.Error -> showMessage(R.string.recordings_load_error)
        }
    }

    private fun showMessage(messageRes: Int) {
        content.removeAllViews()
        LayoutInflater.from(this).inflate(R.layout.view_empty_recordings, content, true)
        content.findViewById<TextView>(R.id.empty_message).setText(messageRes)
        content.findViewById<Button>(R.id.retry).setOnClickListener { showRecordingList() }
    }

    private companion object {
        const val STORAGE_PERMISSION_REQUEST = 41
        val STORAGE_PERMISSIONS = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }
}
