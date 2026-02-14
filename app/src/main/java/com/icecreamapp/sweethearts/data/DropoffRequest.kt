package com.icecreamapp.sweethearts.data

/**
 * Ice cream dropoff request from Firestore (done == false).
 */
data class DropoffRequest(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val latitude: Double,
    val longitude: Double,
)

/**
 * Dropoff request with address and distance for display.
 */
data class DropoffRequestDisplay(
    val request: DropoffRequest,
    val address: String,
    val distanceMeters: Double,
)
