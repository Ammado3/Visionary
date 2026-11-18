package com.o3.vanguard.visionary.platform.permission

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat

/** Check if a single permission is granted */
fun Context.isPermissionGranted(permission: String): Boolean {
    return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}

/** Check if a list of permissions are all granted */
fun Context.hasPermissions(permissions: List<String>): Boolean {
    return permissions.all { isPermissionGranted(it) }
}

/** Check if at least one required media permission is granted (Full or Partial media access) */
fun Context.hasMediaAccess(sdk: Int = Build.VERSION.SDK_INT): Boolean {
    return when {
        sdk >= 34 -> {
            isPermissionGranted(Manifest.permission.READ_MEDIA_IMAGES) ||
            isPermissionGranted(Manifest.permission.READ_MEDIA_VIDEO) ||
            isPermissionGranted("android.permission.READ_MEDIA_VISUAL_USER_SELECTED")
        }
        sdk >= 33 -> {
            isPermissionGranted(Manifest.permission.READ_MEDIA_IMAGES) ||
            isPermissionGranted(Manifest.permission.READ_MEDIA_VIDEO)
        }
        else -> {
            isPermissionGranted(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }
}

/** Check if device Location / GPS service is active */
fun Context.isLocationGpsEnabled(): Boolean {
    val locationManager = getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
    return LocationManagerCompat.isLocationEnabled(locationManager)
}

/** Open System Location Settings screen */
fun Context.openLocationSettings() {
    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    startActivity(intent)
}

/** Open App Details / Permissions Settings screen */
fun Context.openAppSettings() {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    startActivity(intent)
}

/** Standard Location permissions for Android depending on SDK level */
fun getRequiredLocationPermissions(sdk: Int = Build.VERSION.SDK_INT): List<String> = buildList {
    add(Manifest.permission.ACCESS_FINE_LOCATION)
    add(Manifest.permission.ACCESS_COARSE_LOCATION)
    if (sdk >= Build.VERSION_CODES.TIRAMISU) {
        add(Manifest.permission.POST_NOTIFICATIONS)
    }
}

/** Standard Media / Storage permissions depending on SDK level */
fun getRequiredMediaPermissions(sdk: Int = Build.VERSION.SDK_INT): List<String> = buildList {
    if (sdk >= Build.VERSION_CODES.TIRAMISU) {
        add(Manifest.permission.READ_MEDIA_IMAGES)
        add(Manifest.permission.READ_MEDIA_VIDEO)
        if (sdk >= 34) {
            add("android.permission.READ_MEDIA_VISUAL_USER_SELECTED")
        }
    } else {
        add(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
}
