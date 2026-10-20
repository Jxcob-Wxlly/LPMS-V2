package com.teampointblank.lpms.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson

data class StoredSession(val token: String, val expiresAtUtc: String, val profile: UserProfile)

/**
 * Keeps the sign-in token in EncryptedSharedPreferences (AES-256, key held in the Android Keystore).
 * If the keystore is unavailable the session is held in memory only, so the user simply signs in again.
 */
class TokenStore(context: Context) {

    private val gson = Gson()
    private val prefs: SharedPreferences? = createPrefs(context.applicationContext)
    private var memory: StoredSession? = null

    fun load(): StoredSession? {
        val p = prefs ?: return memory
        return try {
            val token = p.getString(KEY_TOKEN, null)
            val expires = p.getString(KEY_EXPIRES, null)
            val profileJson = p.getString(KEY_PROFILE, null)
            if (token.isNullOrBlank() || expires == null || profileJson == null) {
                null
            } else {
                val profile = gson.fromJson(profileJson, UserProfile::class.java)
                if (profile == null) null else StoredSession(token, expires, profile)
            }
        } catch (e: Exception) {
            null
        }
    }

    fun save(session: StoredSession) {
        memory = session
        val p = prefs ?: return
        try {
            p.edit()
                .putString(KEY_TOKEN, session.token)
                .putString(KEY_EXPIRES, session.expiresAtUtc)
                .putString(KEY_PROFILE, gson.toJson(session.profile))
                .apply()
        } catch (e: Exception) {
            // Memory copy is still valid for this run.
        }
    }

    fun clear() {
        memory = null
        try {
            prefs?.edit()?.clear()?.apply()
        } catch (e: Exception) {
            // Nothing else to do.
        }
    }

    private fun createPrefs(context: Context): SharedPreferences? {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            null
        }
    }

    private companion object {
        const val FILE_NAME = "lpms_secure"
        const val KEY_TOKEN = "token"
        const val KEY_EXPIRES = "expires"
        const val KEY_PROFILE = "profile"
    }
}
