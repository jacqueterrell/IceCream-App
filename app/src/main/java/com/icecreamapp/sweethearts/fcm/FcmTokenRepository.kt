package com.icecreamapp.sweethearts.fcm

import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase

/**
 * Sends the device FCM token to Firebase (Firestore via callable) once push is permitted.
 * [receivesVendorAlerts] must be true for devices that should get new-request alerts; the
 * backend should either target FCM topic [FcmTopics.VENDOR_ALERTS] or query stored tokens
 * with this flag.
 */
object FcmTokenRepository {

    private val functions: FirebaseFunctions
        get() = Firebase.functions

    /**
     * Register the current FCM token with the backend. Call when permission is granted or token refreshes.
     */
    fun registerToken(token: String, receivesVendorAlerts: Boolean = false) {
        val data = hashMapOf<String, Any>(
            "fcmToken" to token,
            "platform" to "android",
            "receivesVendorAlerts" to receivesVendorAlerts,
        )
        functions.getHttpsCallable("registerDeviceToken").call(data)
            .addOnFailureListener {
                // Optionally retry or log
            }
    }
}
