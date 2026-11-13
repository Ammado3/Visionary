package com.o3.vanguard.visionary.feature.teleport.platform

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability

internal object AndroidReadiness {

    @Suppress("InlinedApi")
    fun requiredPermissions(sdk: Int = Build.VERSION.SDK_INT): List<String> = buildList {
        when {
            sdk >= Build.VERSION_CODES.TIRAMISU -> {
                add(Manifest.permission.BLUETOOTH_ADVERTISE)
                add(Manifest.permission.BLUETOOTH_CONNECT)
                add(Manifest.permission.BLUETOOTH_SCAN)
                add(Manifest.permission.NEARBY_WIFI_DEVICES)
                add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            sdk >= Build.VERSION_CODES.S -> {
                add(Manifest.permission.BLUETOOTH_ADVERTISE)
                add(Manifest.permission.BLUETOOTH_CONNECT)
                add(Manifest.permission.BLUETOOTH_SCAN)
                add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            sdk >= Build.VERSION_CODES.Q -> add(Manifest.permission.ACCESS_FINE_LOCATION)
            else -> {
                add(Manifest.permission.ACCESS_COARSE_LOCATION)
                @Suppress("DEPRECATION")
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    fun check(context: Context): ReadinessResult {
        val missing = requiredPermissions().filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) return ReadinessResult.MissingPermissions(missing)

        if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) != ConnectionResult.SUCCESS) {
            return ReadinessResult.PlayServicesUnavailable
        }
        val bluetooth = context.getSystemService(BluetoothManager::class.java)?.adapter
        if (bluetooth == null || !bluetooth.isEnabled) return ReadinessResult.BluetoothDisabled

        val wifi = context.applicationContext.getSystemService(WifiManager::class.java)
        if (wifi == null || !wifi.isWifiEnabled) return ReadinessResult.WifiDisabled

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            val location = context.getSystemService(LocationManager::class.java)
            if (location == null || !LocationManagerCompat.isLocationEnabled(location)) return ReadinessResult.LocationDisabled
        }
        return ReadinessResult.Ready
    }
}
