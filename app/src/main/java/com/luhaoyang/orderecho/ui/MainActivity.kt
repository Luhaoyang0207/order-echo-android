package com.luhaoyang.orderecho.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.luhaoyang.orderecho.R
import com.luhaoyang.orderecho.cleanup.CleanupStartup

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        CleanupStartup().initialize(applicationContext)
    }
}
