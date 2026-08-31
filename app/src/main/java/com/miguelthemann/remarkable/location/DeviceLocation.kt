/*
 * Copyright (c) 2026 Miguel Guerra
 * SPDX-License-Identifier: MIT
 */
package com.miguelthemann.remarkable.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
    val label: String,
)

class DeviceLocation(private val context: Context) {
    @SuppressLint("MissingPermission")
    fun lastKnown(): Location? {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = listOf(
            LocationManager.NETWORK_PROVIDER,
            LocationManager.GPS_PROVIDER,
            LocationManager.PASSIVE_PROVIDER,
        )
        return providers
            .filter { manager.isProviderEnabled(it) }
            .mapNotNull { provider ->
                runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
            }
            .maxByOrNull { it.time }
    }

    suspend fun reverseLabel(location: Location): String = withContext(Dispatchers.IO) {
        if (!Geocoder.isPresent()) {
            return@withContext "%.2f, %.2f".format(location.latitude, location.longitude)
        }
        val geocoder = Geocoder(context, Locale.getDefault())
        val address = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { cont ->
                geocoder.getFromLocation(location.latitude, location.longitude, 1) { list ->
                    cont.resume(list.firstOrNull())
                }
            }
        } else {
            @Suppress("DEPRECATION")
            geocoder.getFromLocation(location.latitude, location.longitude, 1)?.firstOrNull()
        }
        address?.locality
            ?: address?.subAdminArea
            ?: address?.adminArea
            ?: "%.2f, %.2f".format(location.latitude, location.longitude)
    }
}
