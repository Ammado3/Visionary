package com.o3.vanguard.visionary

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.graphics.toArgb
import com.o3.vanguard.visionary.core.designsystem.VisionaryColors
import androidx.lifecycle.lifecycleScope
import com.o3.vanguard.visionary.domain.repository.IndexRepository
import kotlinx.coroutines.launch
import com.o3.vanguard.visionary.platform.android.AndroidBiometricAuthenticator
import androidx.fragment.app.FragmentActivity
import org.koin.android.ext.android.inject

class MainActivity : AppCompatActivity(){
    private val indexRepository: IndexRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidBiometricAuthenticator.currentActivityProvider = { this }
        val systemBarColor = VisionaryColors.colorF4FDF7.toArgb()
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(systemBarColor, systemBarColor),
            navigationBarStyle = SystemBarStyle.light(systemBarColor, systemBarColor)
        )
        setContent {
            App()
        }
        requestMediaAccessIfNeeded()
    }

    private fun requestMediaAccessIfNeeded() {
        val permissions = if (Build.VERSION.SDK_INT >= 33) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        if (permissions.all { checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }) {
            startIndexing()
        } else {
            requestPermissions(permissions, MEDIA_PERMISSION_REQUEST)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == MEDIA_PERMISSION_REQUEST && grantResults.any { it == PackageManager.PERMISSION_GRANTED }) {
            startIndexing()
        }
    }

    private fun startIndexing() {
        lifecycleScope.launch { indexRepository.rebuild() }
    }

    private companion object {
        const val MEDIA_PERMISSION_REQUEST = 1001
    }
}
