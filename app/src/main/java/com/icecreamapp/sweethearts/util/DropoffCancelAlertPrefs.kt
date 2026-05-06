package com.icecreamapp.sweethearts.util

import android.content.Context

/**
 * Remembers that the user has acknowledged the in-app cancel alert for a dropoff id
 * so we do not show it again on every poll.
 */
object DropoffCancelAlertPrefs {

    private const val PREFS = "dropoff_cancel_alerts"
    private const val KEY_ACK_PREFIX = "ack_"

    fun isCancelAlertAcknowledged(context: Context, dropoffId: String): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_ACK_PREFIX + dropoffId, false)

    fun acknowledgeCancelAlert(context: Context, dropoffId: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ACK_PREFIX + dropoffId, true)
            .apply()
    }
}
