package com.icecreamapp.sweethearts.util

import android.content.Context
import android.content.SharedPreferences

object UserProfilePrefs {
    private const val PREFS_NAME = "user_profile_prefs"
    private const val KEY_NAME = "customer_name"
    private const val KEY_PHONE = "customer_phone"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveProfile(context: Context, name: String, phone: String) {
        getPrefs(context).edit()
            .putString(KEY_NAME, name)
            .putString(KEY_PHONE, phone)
            .apply()
    }

    fun getName(context: Context): String? = getPrefs(context).getString(KEY_NAME, null)
    fun getPhone(context: Context): String? = getPrefs(context).getString(KEY_PHONE, null)

    fun hasProfile(context: Context): Boolean {
        return !getName(context).isNullOrEmpty() && !getPhone(context).isNullOrEmpty()
    }

    fun clearProfile(context: Context) {
        getPrefs(context).edit().clear().apply()
    }
}
