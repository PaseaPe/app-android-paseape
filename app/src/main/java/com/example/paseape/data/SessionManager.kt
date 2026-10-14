package com.example.paseape.data

import android.content.Context

object SessionManager {

    private const val PREFS_NAME = "paseape_prefs"
    private const val LEGACY_PREFS_NAME = "PaseaPePrefs"
    private const val KEY_AUTH_TOKEN = "auth_token"
    private const val KEY_JWT_TOKEN = "JWT_TOKEN"
    private const val KEY_USER_UUID = "user_uuid"

    fun getToken(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val legacyPrefs = context.getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_AUTH_TOKEN, null)
            ?: prefs.getString(KEY_JWT_TOKEN, null)
            ?: legacyPrefs.getString(KEY_AUTH_TOKEN, null)
            ?: legacyPrefs.getString(KEY_JWT_TOKEN, null)
    }

    fun getAuthHeader(context: Context): String {
        val token = getToken(context).orEmpty()
        return if (token.startsWith("Bearer ")) token else "Bearer $token"
    }

    fun getUserUuid(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val legacyPrefs = context.getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_USER_UUID, null)
            ?: legacyPrefs.getString(KEY_USER_UUID, null)
    }
}
