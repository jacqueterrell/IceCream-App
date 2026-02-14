package com.icecreamapp.sweethearts.util

import android.location.Location

/**
 * Approximate distance in meters between two WGS84 points (Haversine).
 */
fun distanceMeters(
    fromLat: Double,
    fromLng: Double,
    toLat: Double,
    toLng: Double,
): Double {
    val results = FloatArray(1)
    Location.distanceBetween(fromLat, fromLng, toLat, toLng, results)
    return results[0].toDouble()
}

fun formatDistance(meters: Double): String {
    return when {
        meters < 1000 -> "%.0f m".format(meters)
        else -> "%.1f km".format(meters / 1000)
    }
}
