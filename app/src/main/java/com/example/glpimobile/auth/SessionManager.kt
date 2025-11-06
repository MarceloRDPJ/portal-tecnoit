package com.example.glpimobile.auth

import android.content.Context
import android.content.SharedPreferences

// Manages the user's session token.
object SessionManager {

    private const val PREFS_NAME = "glpi_session"
    private const val KEY_SESSION_TOKEN = "session_token"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveSessionToken(context: Context, sessionToken: String) {
        getPrefs(context).edit()
            .putString(KEY_SESSION_TOKEN, sessionToken)
            .apply()
    }

    fun getSessionToken(context: Context): String? {
        return getPrefs(context).getString(KEY_SESSION_TOKEN, null)
    }

    fun clearSession(context: Context) {
        getPrefs(context).edit().clear().apply()
    }
}
