package com.icecreamapp.sweethearts.util

import android.content.Context

/**
 * Persists that this install should reopen the Admin (dropoff) screen after restarts
 * until the user explicitly leaves Admin mode.
 */
object AdminSessionPrefs {

    private const val PREFS = "admin_session_prefs"
    private const val KEY_PERSIST_ADMIN = "persist_admin_session"

    fun isPersistedAdminSession(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_PERSIST_ADMIN, false)

    fun setPersistedAdminSession(context: Context, active: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_PERSIST_ADMIN, active)
            .apply()
    }
}
