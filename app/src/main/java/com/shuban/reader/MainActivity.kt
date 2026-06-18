package com.shuban.reader

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.shuban.reader.service.FloatingWindowService

class MainActivity : AppCompatActivity() {

    private lateinit var btnToggleService: Button
    private lateinit var tvServiceStatus: TextView
    private var isServiceRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        btnToggleService = findViewById(R.id.btn_toggle_service)
        tvServiceStatus = findViewById(R.id.tv_service_status)

        btnToggleService.setOnClickListener {
            if (!isServiceRunning) {
                checkOverlayPermission()
            } else {
                stopFloatingWindowService()
            }
        }
    }

    private fun checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE)
            } else {
                startFloatingWindowService()
            }
        } else {
            startFloatingWindowService()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == OVERLAY_PERMISSION_REQUEST_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(this)) {
                    startFloatingWindowService()
                } else {
                    Toast.makeText(this, "需要悬浮窗权限才能使用", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun startFloatingWindowService() {
        val intent = Intent(this, FloatingWindowService::class.java)
        startService(intent)
        isServiceRunning = true
        updateServiceStatus()
        Toast.makeText(this, "悬浮窗服务已启动", Toast.LENGTH_SHORT).show()
    }

    private fun stopFloatingWindowService() {
        val intent = Intent(this, FloatingWindowService::class.java)
        stopService(intent)
        isServiceRunning = false
        updateServiceStatus()
        Toast.makeText(this, "悬浮窗服务已停止", Toast.LENGTH_SHORT).show()
    }

    private fun updateServiceStatus() {
        if (isServiceRunning) {
            tvServiceStatus.text = getString(R.string.service_running)
            btnToggleService.text = getString(R.string.btn_stop_service)
        } else {
            tvServiceStatus.text = getString(R.string.service_stopped)
            btnToggleService.text = getString(R.string.btn_start_service)
        }
    }

    companion object {
        private const val OVERLAY_PERMISSION_REQUEST_CODE = 1001
    }
}