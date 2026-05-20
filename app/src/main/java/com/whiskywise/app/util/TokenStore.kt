package com.whiskywise.app.util

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Stores the server URL and Bearer token in EncryptedSharedPreferences.
 * The token is never written to plain storage.
 *
 * On first launch after a new install, Google Backup may restore an encrypted
 * preferences file from a previous install. The Keystore key from the old install
 * no longer exists, so decryption throws AEADBadTagException and crashes the app.
 * We detect this, delete the corrupted file, and start fresh — requiring the user
 * to log in again, which is the correct behaviour after a reinstall anyway.
 */
class TokenStore(private val context: Context) {

    private val prefs = createPrefs()

    private fun createPrefs() = try {
        buildPrefs(context)
    } catch (e: Exception) {
        Log.w("TokenStore", "EncryptedSharedPreferences failed to open — wiping and recreating. Cause: ${e.javaClass.simpleName}")
        // Delete the corrupted file (typically caused by Google Backup restoring
        // preferences encrypted with a Keystore key that no longer exists on this device).
        context.deleteSharedPreferences("whiskywise_secure_prefs")
        // Also remove the master key entry so MasterKey doesn't pick up a stale alias.
        try {
            val ks = java.security.KeyStore.getInstance("AndroidKeyStore").also { it.load(null) }
            ks.deleteEntry("_androidx_security_master_key")
        } catch (ignored: Exception) { /* best-effort */ }
        buildPrefs(context)
    }

    private fun buildPrefs(context: Context) = EncryptedSharedPreferences.create(
        context,
        "whiskywise_secure_prefs",
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
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

    fun saveCurrencySymbol(symbol: String) = prefs.edit().putString(KEY_CURRENCY_SYMBOL, symbol).apply()
    fun getCurrencySymbol(): String = prefs.getString(KEY_CURRENCY_SYMBOL, "€") ?: "€"

    fun isLoggedIn(): Boolean = getToken() != null && getServerUrl() != null

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_SERVER_URL      = "server_url"
        private const val KEY_TOKEN           = "token"
        private const val KEY_CURRENCY_SYMBOL = "currency_symbol"
    }
}
