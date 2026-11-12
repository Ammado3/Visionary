package com.o3.vanguard.visionary

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.graphics.toArgb
import com.o3.vanguard.visionary.core.designsystem.VisionaryColors
import androidx.lifecycle.lifecycleScope
import com.o3.vanguard.visionary.domain.repository.IndexRepository
import kotlinx.coroutines.launch
import com.o3.vanguard.visionary.platform.android.AndroidBiometricAuthenticator
import com.o3.vanguard.visionary.platform.android.AndroidMediaDeleteGateway
import androidx.fragment.app.FragmentActivity
import org.koin.android.ext.android.inject

class MainActivity : AppCompatActivity(){
    private val indexRepository: IndexRepository by inject()

    private val deleteRequestLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        AndroidMediaDeleteGateway.handleActivityResult(result.resultCode)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidBiometricAuthenticator.currentActivityProvider = { this }
        AndroidMediaDeleteGateway.currentLauncher = { intentSender ->
            deleteRequestLauncher.launch(
                IntentSenderRequest.Builder(intentSender).build()
            )
        }
        val systemBarColor = VisionaryColors.colorF4FDF7.toArgb()
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(systemBarColor, systemBarColor),
            navigationBarStyle = SystemBarStyle.light(systemBarColor, systemBarColor)
        )
        setContent {
            App()
        }
        // Only trigger indexing on a true first launch of this process, not on
        // Activity re-creation (e.g. screen rotation or system-initiated restart).
        if (savedInstanceState == null) {
            requestMediaAccessIfNeeded()
        }
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
    override fun onDestroy() {
        if (isFinishing) {
            AndroidMediaDeleteGateway.currentLauncher = null
            AndroidBiometricAuthenticator.currentActivityProvider = null
        }
        super.onDestroy()
    }

    private fun startIndexing() {
        lifecycleScope.launch { indexRepository.rebuild() }
    }

    private companion object {
        const val MEDIA_PERMISSION_REQUEST = 1001
    }
}
