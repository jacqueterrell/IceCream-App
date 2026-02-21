package com.icecreamapp.sweethearts.util

/**
 * Decodes a Google encoded polyline string into a list of [lat, lng] pairs.
 * See: https://developers.google.com/maps/documentation/utilities/polylinealgorithm
 */
fun decodePolyline(encoded: String): List<Pair<Double, Double>> {
    if (encoded.isEmpty()) return emptyList()
    val points = mutableListOf<Pair<Double, Double>>()
    var index = 0
    var lat = 0
    var lng = 0
    while (index < encoded.length) {
        var b: Int
        var shift = 0
        var result = 0
        do {
            b = encoded[index++].code - 63
            result = result or (b and 0x1f) shl shift
            shift += 5
        } while (b >= 0x20)
        val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
        lat += dlat
        shift = 0
        result = 0
        do {
            b = encoded[index++].code - 63
            result = result or (b and 0x1f) shl shift
            shift += 5
        } while (b >= 0x20)
        val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
        lng += dlng
        points.add(lat / 1e5 to lng / 1e5)
    }
    return points
}
