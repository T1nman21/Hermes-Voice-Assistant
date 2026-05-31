//
// OpenRocky — Voice-first AI Agent
// https://github.com/openrocky
//
// Developed by everettjf with the assistance of Claude Code and Codex.
// Date: 2026-03-25
// Copyright (c) 2026 everettjf. All rights reserved.
//

package com.xnu.rocky.runtime.tools

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.LocationManager
import com.xnu.rocky.runtime.PermissionHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

class LocationService(private val context: Context) {

    /** Best-effort last known fix. Returns null when permission is missing or no provider has a cached fix. */
    @SuppressLint("MissingPermission")
    fun lastKnownLatLng(): Pair<Double, Double>? {
        if (!PermissionHelper.hasLocation(context)) return null
        return try {
            val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val loc = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                ?: return null
            loc.latitude to loc.longitude
        } catch (_: Exception) {
            null
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): String = withContext(Dispatchers.IO) {
        if (!PermissionHelper.hasLocation(context)) {
            return@withContext "Location permission not granted. Please enable location access in Settings."
        }
        try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                ?: return@withContext "Location not available. Please enable location services."

            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            val address = addresses?.firstOrNull()

            buildString {
                appendLine("Latitude: ${location.latitude}")
                appendLine("Longitude: ${location.longitude}")
                if (address != null) {
                    appendLine("Address: ${address.getAddressLine(0) ?: "Unknown"}")
                    appendLine("City: ${address.locality ?: "Unknown"}")
                    appendLine("Country: ${address.countryName ?: "Unknown"}")
                }
            }
        } catch (e: Exception) {
            "Location error: ${e.message}"
        }
    }

    suspend fun geocode(address: String): String = withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val results = geocoder.getFromLocationName(address, 1)
            val location = results?.firstOrNull()
                ?: return@withContext "Could not geocode: $address"

            buildString {
                appendLine("Address: ${location.getAddressLine(0) ?: address}")
                appendLine("Latitude: ${location.latitude}")
                appendLine("Longitude: ${location.longitude}")
            }
        } catch (e: Exception) {
            "Geocoding error: ${e.message}"
        }
    }
}
