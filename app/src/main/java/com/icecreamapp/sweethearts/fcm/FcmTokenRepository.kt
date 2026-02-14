package com.icecreamapp.sweethearts.fcm

import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase

/**
 * Sends the device FCM token to Firebase (Firestore via callable) once push is permitted.
 */
object FcmTokenRepository {

    private val functions: FirebaseFunctions
        get() = Firebase.functions

    /**
     * Register the current FCM token with the backend. Call when permission is granted or token refreshes.
     */
    fun registerToken(token: String) {
        val data = hashMapOf(
            "fcmToken" to token,
            "platform" to "android",
        )
        functions.getHttpsCallable("registerDeviceToken").call(data)
            .addOnFailureListener {
                // Optionally retry or log
            }
    }
}
