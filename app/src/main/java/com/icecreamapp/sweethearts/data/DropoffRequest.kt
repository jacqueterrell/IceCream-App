package com.icecreamapp.sweethearts.data

import java.util.Locale

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
) {
    /**
     * Whether this request should appear on the shared customer map and route list.
     * Admin-resolved or finished requests stay in Firestore for history but are hidden here.
     */
    fun shouldShowOnCustomerRouteMap(): Boolean {
        val s = status?.trim()?.lowercase(Locale.ROOT) ?: return true
        if (s.isEmpty()) return true
        return s !in TerminalStatuses
    }

    fun isCanceledStatus(): Boolean {
        val s = status?.trim()?.lowercase(Locale.ROOT) ?: return false
        return s == "canceled" || s == "cancelled"
    }

    companion object {
        private val TerminalStatuses = setOf(
            "approved",
            "canceled",
            "cancelled",
            "done",
            "completed",
        )
    }
}

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
