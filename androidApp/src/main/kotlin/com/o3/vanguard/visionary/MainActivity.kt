package com.o3.vanguard.visionary

import android.Manifest
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
import com.o3.vanguard.visionary.platform.android.AndroidBiometricAuthenticator
import com.o3.vanguard.visionary.platform.android.AndroidMediaDeleteGateway

class MainActivity : AppCompatActivity(){
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
        // Ask for media access only on a true first launch of this process, not on Activity
        // re-creation (rotation, system restart). Indexing is started by App and onboarding.
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
        if (!permissions.all { checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }) {
            requestPermissions(permissions, MEDIA_PERMISSION_REQUEST)
        }
    }

    override fun onDestroy() {
        if (isFinishing) {
            AndroidMediaDeleteGateway.currentLauncher = null
            AndroidBiometricAuthenticator.currentActivityProvider = null
        }
        super.onDestroy()
    }

    private companion object {
        const val MEDIA_PERMISSION_REQUEST = 1001
    }
}
