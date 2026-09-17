package com.luhaoyang.orderecho.calls

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.core.content.ContextCompat

object FirstCallPermissions {
    val runtime = arrayOf(Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_CALL_LOG)

    fun granted(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    fun hasRuntime(context: Context): Boolean = runtime.all { granted(context, it) }

    fun ready(context: Context): Boolean = hasRuntime(context) && Settings.canDrawOverlays(context)
}
