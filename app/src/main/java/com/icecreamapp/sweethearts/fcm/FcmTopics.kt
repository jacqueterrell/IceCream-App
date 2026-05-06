package com.icecreamapp.sweethearts.fcm

/**
 * FCM topics used by this app. Cloud Functions that notify vendors must use the same
 * topic name when calling [com.google.firebase.messaging.FirebaseMessaging.send] with a topic,
 * or store tokens where [receivesVendorAlerts] is true from [FcmTokenRepository.registerToken].
 */
object FcmTopics {
    const val ALL_USERS = "all-users"

    /** Subscribe after vendor unlocks admin once on this device. */
    const val VENDOR_ALERTS = "vendor-alerts"
}
