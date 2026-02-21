package com.icecreamapp.sweethearts.data

/**
 * Ice cream dropoff request from Firestore.
 * status is null for pending; "Approved" or "Canceled" when done.
 */
data class DropoffRequest(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val latitude: Double,
    val longitude: Double,
    val status: String? = null,
)

/**
 * Dropoff request with address and distance for display.
 */
data class DropoffRequestDisplay(
    val request: DropoffRequest,
    val address: String,
    val distanceMeters: Double,
)

/** Dropoff in optimized route order with ETA (seconds from now). */
data class DropoffWithEta(
    val display: DropoffRequestDisplay,
    val etaSecondsFromNow: Long,
)
