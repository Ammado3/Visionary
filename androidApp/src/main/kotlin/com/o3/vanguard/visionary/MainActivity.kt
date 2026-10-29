package com.o3.vanguard.visionary

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.o3.vanguard.visionary.domain.repository.IndexRepository
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val indexRepository: IndexRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            App()
        }
        requestMediaAccessIfNeeded()
    }

    private fun requestMediaAccessIfNeeded() {
        val permission = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_IMAGES
        else Manifest.permission.READ_EXTERNAL_STORAGE
        if (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) {
            startIndexing()
        } else {
            requestPermissions(arrayOf(permission), MEDIA_PERMISSION_REQUEST)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == MEDIA_PERMISSION_REQUEST && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
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
