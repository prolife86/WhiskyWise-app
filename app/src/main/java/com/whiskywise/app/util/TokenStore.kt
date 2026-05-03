package com.whiskywise.app.util

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Stores the server URL and Bearer token in EncryptedSharedPreferences.
 * The token is never written to plain storage.
 */
class TokenStore(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "whiskywise_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun saveCredentials(serverUrl: String, token: String) {
        prefs.edit()
            .putString(KEY_SERVER_URL, serverUrl)
            .putString(KEY_TOKEN, token)
            .apply()
    }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)
    fun getServerUrl(): String? = prefs.getString(KEY_SERVER_URL, null)

    fun isLoggedIn(): Boolean = getToken() != null && getServerUrl() != null

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_TOKEN      = "token"
    }
}
