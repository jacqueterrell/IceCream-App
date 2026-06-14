package com.icecreamapp.sweethearts.util

import android.content.Context

/**
 * Remembers that the user acknowledged the in-app alert for an approved dropoff.
 */
object DropoffApproveAlertPrefs {

    private const val PREFS = "dropoff_approve_alerts"
    private const val KEY_ACK_PREFIX = "ack_"

    fun isApproveAlertAcknowledged(context: Context, dropoffId: String): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_ACK_PREFIX + dropoffId, false)

    fun acknowledgeApproveAlert(context: Context, dropoffId: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ACK_PREFIX + dropoffId, true)
            .apply()
    }
}
