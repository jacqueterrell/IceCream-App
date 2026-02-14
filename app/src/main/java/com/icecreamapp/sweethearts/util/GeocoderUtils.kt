package com.icecreamapp.sweethearts.util

import android.content.Context
import android.location.Geocoder
import java.util.Locale

/**
 * Reverse geocode coordinates to an address string. Runs on caller's thread; call from Dispatchers.IO.
 */
fun reverseGeocode(context: Context, latitude: Double, longitude: Double): String {
    return try {
        val geocoder = Geocoder(context, Locale.getDefault())
        @Suppress("DEPRECATION")
        val addresses = geocoder.getFromLocation(latitude, longitude, 1)
        addresses?.firstOrNull()?.let { addr ->
            val parts = listOfNotNull(
                addr.thoroughfare,
                addr.subThoroughfare,
                addr.locality,
                addr.adminArea,
                addr.countryCode,
            )
            parts.joinToString(", ").ifEmpty { "%.6f, %.6f".format(latitude, longitude) }
        } ?: "%.6f, %.6f".format(latitude, longitude)
    } catch (e: Exception) {
        "%.6f, %.6f".format(latitude, longitude)
    }
}
