package com.icecreamapp.sweethearts.util

import android.content.Context

/**
 * Remembers that this install should receive vendor-facing FCM (topic + token registration flag).
 */
object VendorNotificationPrefs {

    private const val PREFS = "ice_cream_vendor_push"
    private const val KEY_VENDOR_ALERTS_OPT_IN = "vendor_alerts_opt_in"

    fun isVendorAlertsOptIn(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_VENDOR_ALERTS_OPT_IN, false)

    fun setVendorAlertsOptIn(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_VENDOR_ALERTS_OPT_IN, enabled)
            .apply()
    }
}
