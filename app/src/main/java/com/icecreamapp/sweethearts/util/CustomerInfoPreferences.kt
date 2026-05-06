package com.icecreamapp.sweethearts.util

import android.content.Context

/**
 * Persists customer name and phone (digits only) locally for dropoff requests.
 */
object CustomerInfoPreferences {

    private const val PREFS_NAME = "customer_dropoff_info"
    private const val KEY_NAME = "customer_name"
    private const val KEY_PHONE_DIGITS = "customer_phone_digits"

    fun loadName(context: Context): String =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_NAME, null)?.trim().orEmpty()

    fun loadPhoneDigits(context: Context): String =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_PHONE_DIGITS, null)
            ?.filter { it.isDigit() }
            ?.take(10)
            .orEmpty()

    fun save(context: Context, name: String, phoneDigits: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_NAME, name.trim())
            .putString(KEY_PHONE_DIGITS, phoneDigits.filter { it.isDigit() }.take(10))
            .apply()
    }
}
