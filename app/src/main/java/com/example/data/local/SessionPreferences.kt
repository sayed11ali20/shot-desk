package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "shotdesk_session")

data class UserSession(
    val isLoggedIn: Boolean = false,
    val localUserId: String = "",
    val fullName: String = "",
    val nickname: String = "",
    val passwordHash: String = "",
    val passwordSalt: String = "",
    val isDarkMode: Boolean = true,
    val languageCode: String = "en",
    val lastSyncMillis: Long = 0L,
)

class SessionPreferences(private val context: Context) {

    private object Keys {
        val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        val LOCAL_USER_ID = stringPreferencesKey("local_user_id")
        val FULL_NAME = stringPreferencesKey("full_name")
        val NICKNAME = stringPreferencesKey("nickname")
        val PASSWORD_HASH = stringPreferencesKey("password_hash")
        val PASSWORD_SALT = stringPreferencesKey("password_salt")
        val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
        val LANGUAGE_CODE = stringPreferencesKey("language_code")
        val LAST_SYNC_MILLIS = longPreferencesKey("last_sync_millis")
    }

    val sessionFlow: Flow<UserSession> = context.sessionDataStore.data.map { prefs ->
        UserSession(
            isLoggedIn = prefs[Keys.IS_LOGGED_IN] ?: false,
            localUserId = prefs[Keys.LOCAL_USER_ID] ?: "",
            fullName = prefs[Keys.FULL_NAME] ?: "",
            nickname = prefs[Keys.NICKNAME] ?: "",
            passwordHash = prefs[Keys.PASSWORD_HASH] ?: "",
            passwordSalt = prefs[Keys.PASSWORD_SALT] ?: "",
            isDarkMode = prefs[Keys.IS_DARK_MODE] ?: true,
            languageCode = prefs[Keys.LANGUAGE_CODE] ?: "en",
            lastSyncMillis = prefs[Keys.LAST_SYNC_MILLIS] ?: 0L,
        )
    }

    suspend fun createLocalAccount(
        fullName: String,
        nickname: String,
        password: String,
    ): UserSession {
        val salt = PasswordHasher.generateSalt()
        val hash = PasswordHasher.hash(password, salt)
        val existingId = sessionFlow.first().localUserId.ifBlank {
            "local_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        }
        context.sessionDataStore.edit { prefs ->
            prefs[Keys.IS_LOGGED_IN] = true
            prefs[Keys.LOCAL_USER_ID] = existingId
            prefs[Keys.FULL_NAME] = fullName.trim()
            prefs[Keys.NICKNAME] = nickname.trim()
            prefs[Keys.PASSWORD_HASH] = hash
            prefs[Keys.PASSWORD_SALT] = salt
        }
        return sessionFlow.first()
    }

    suspend fun verifyAndLogInLocalAccount(
        nickname: String,
        password: String,
    ): Result<UserSession> {
        val current = sessionFlow.first()
        if (current.nickname.isBlank() || current.passwordHash.isBlank()) {
            return Result.failure(IllegalStateException("No local account found. Please create an account or sign in with Google."))
        }
        if (!current.nickname.equals(nickname.trim(), ignoreCase = true)) {
            return Result.failure(IllegalArgumentException("Nickname does not match the saved profile on this device."))
        }
        val expectedHash = PasswordHasher.hash(password, current.passwordSalt)
        if (expectedHash != current.passwordHash) {
            return Result.failure(IllegalArgumentException("Incorrect password. Please try again."))
        }
        context.sessionDataStore.edit { prefs ->
            prefs[Keys.IS_LOGGED_IN] = true
        }
        return Result.success(sessionFlow.first())
    }

    suspend fun activateGoogleSession(
        firebaseUid: String,
        displayName: String?,
        email: String?,
    ) {
        val current = sessionFlow.first()
        val resolvedFullName = when {
            current.fullName.isNotBlank() -> current.fullName
            !displayName.isNullOrBlank() -> displayName
            !email.isNullOrBlank() -> email.substringBefore("@")
            else -> "Director"
        }
        val resolvedNickname = when {
            current.nickname.isNotBlank() -> current.nickname
            !email.isNullOrBlank() -> email.substringBefore("@")
            !displayName.isNullOrBlank() -> displayName.lowercase().replace(" ", "_")
            else -> "director_${firebaseUid.take(6)}"
        }
        context.sessionDataStore.edit { prefs ->
            prefs[Keys.IS_LOGGED_IN] = true
            if (prefs[Keys.LOCAL_USER_ID].isNullOrBlank()) {
                prefs[Keys.LOCAL_USER_ID] = firebaseUid
            }
            prefs[Keys.FULL_NAME] = resolvedFullName
            prefs[Keys.NICKNAME] = resolvedNickname
        }
    }

    suspend fun updateProfile(
        fullName: String,
        nickname: String,
        newPassword: String?,
    ) {
        context.sessionDataStore.edit { prefs ->
            prefs[Keys.FULL_NAME] = fullName.trim()
            prefs[Keys.NICKNAME] = nickname.trim()
            if (!newPassword.isNullOrBlank()) {
                val salt = PasswordHasher.generateSalt()
                val hash = PasswordHasher.hash(newPassword, salt)
                prefs[Keys.PASSWORD_SALT] = salt
                prefs[Keys.PASSWORD_HASH] = hash
            }
        }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.sessionDataStore.edit { prefs ->
            prefs[Keys.IS_DARK_MODE] = enabled
        }
    }

    suspend fun setLanguageCode(code: String) {
        context.sessionDataStore.edit { prefs ->
            prefs[Keys.LANGUAGE_CODE] = if (code == "ar") "ar" else "en"
        }
    }

    suspend fun updateLastSyncMillis(millis: Long) {
        context.sessionDataStore.edit { prefs ->
            prefs[Keys.LAST_SYNC_MILLIS] = millis
        }
    }

    suspend fun logOutKeepCredentials() {
        context.sessionDataStore.edit { prefs ->
            prefs[Keys.IS_LOGGED_IN] = false
        }
    }
}

object PasswordHasher {
    fun generateSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun hash(password: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val input = "$salt:$password".toByteArray(Charsets.UTF_8)
        return digest.digest(input).joinToString("") { "%02x".format(it) }
    }
}
