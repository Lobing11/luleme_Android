package com.luleme.android.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "luleme_settings"
)

class SettingsStore private constructor(private val dataStore: DataStore<Preferences>) {

    private object Keys {
        val LOCK_ENABLED = booleanPreferencesKey("lock_enabled")
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val PIN_SALT = stringPreferencesKey("pin_salt")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
        val REMINDER_TEXT = stringPreferencesKey("reminder_text")
        val REMINDER_IDS = stringPreferencesKey("reminder_ids")
        val GESHEN_ENABLED = booleanPreferencesKey("geshen_enabled")
        val GESHEN_REMINDER_ID = intPreferencesKey("geshen_reminder_id")
        val PRIVACY_MODE = booleanPreferencesKey("privacy_mode_enabled")
    }

    val settingsFlow: Flow<SettingsSnapshot> = dataStore.data.map { prefs ->
        SettingsSnapshot(
            lockEnabled = prefs[Keys.LOCK_ENABLED] ?: false,
            pinHash = prefs[Keys.PIN_HASH] ?: "",
            pinSalt = prefs[Keys.PIN_SALT] ?: "",
            biometricEnabled = prefs[Keys.BIOMETRIC_ENABLED] ?: false,
            reminderEnabled = prefs[Keys.REMINDER_ENABLED] ?: false,
            reminderHour = prefs[Keys.REMINDER_HOUR] ?: 21,
            reminderMinute = prefs[Keys.REMINDER_MINUTE] ?: 0,
            reminderText = prefs[Keys.REMINDER_TEXT] ?: "是时候鹿了！",
            reminderIds = parseIds(prefs[Keys.REMINDER_IDS] ?: ""),
            geshenEnabled = prefs[Keys.GESHEN_ENABLED] ?: false,
            geshenReminderId = prefs[Keys.GESHEN_REMINDER_ID] ?: 0,
            privacyModeEnabled = prefs[Keys.PRIVACY_MODE] ?: true
        )
    }

    suspend fun snapshot(): SettingsSnapshot = settingsFlow.first()

    suspend fun isLockEnabled(): Boolean = snapshot().lockEnabled
    suspend fun setLockEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.LOCK_ENABLED] = enabled }
    }

    suspend fun getPinHash(): String = snapshot().pinHash
    suspend fun setPinHash(hash: String) {
        dataStore.edit { it[Keys.PIN_HASH] = hash }
    }

    suspend fun getPinSalt(): String = snapshot().pinSalt
    suspend fun setPinSalt(salt: String) {
        dataStore.edit { it[Keys.PIN_SALT] = salt }
    }

    suspend fun isBiometricEnabled(): Boolean = snapshot().biometricEnabled
    suspend fun setBiometricEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.BIOMETRIC_ENABLED] = enabled }
    }

    suspend fun isReminderEnabled(): Boolean = snapshot().reminderEnabled
    suspend fun setReminderEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.REMINDER_ENABLED] = enabled }
    }

    suspend fun getReminderHour(): Int = snapshot().reminderHour
    suspend fun setReminderHour(hour: Int) {
        dataStore.edit { it[Keys.REMINDER_HOUR] = hour }
    }

    suspend fun getReminderMinute(): Int = snapshot().reminderMinute
    suspend fun setReminderMinute(minute: Int) {
        dataStore.edit { it[Keys.REMINDER_MINUTE] = minute }
    }

    suspend fun getReminderText(): String = snapshot().reminderText
    suspend fun setReminderText(text: String) {
        dataStore.edit { it[Keys.REMINDER_TEXT] = text }
    }

    suspend fun getReminderIds(): List<Int> = snapshot().reminderIds
    suspend fun setReminderIds(ids: List<Int>) {
        dataStore.edit { it[Keys.REMINDER_IDS] = ids.joinToString(",") }
    }

    suspend fun isGeshenEnabled(): Boolean = snapshot().geshenEnabled
    suspend fun setGeshenEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.GESHEN_ENABLED] = enabled }
    }

    suspend fun getGeshenReminderId(): Int = snapshot().geshenReminderId
    suspend fun setGeshenReminderId(id: Int) {
        dataStore.edit { it[Keys.GESHEN_REMINDER_ID] = id }
    }

    suspend fun isPrivacyModeEnabled(): Boolean = snapshot().privacyModeEnabled
    suspend fun setPrivacyModeEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.PRIVACY_MODE] = enabled }
    }

    suspend fun hasPin(): Boolean {
        val s = snapshot()
        return s.pinHash.isNotEmpty() && s.pinSalt.isNotEmpty()
    }

    suspend fun resetLock() {
        dataStore.edit { prefs ->
            prefs.remove(Keys.PIN_HASH)
            prefs.remove(Keys.PIN_SALT)
            prefs[Keys.LOCK_ENABLED] = false
            prefs[Keys.BIOMETRIC_ENABLED] = false
        }
    }

    suspend fun savePin(hash: String, salt: String) {
        dataStore.edit { prefs ->
            prefs[Keys.PIN_HASH] = hash
            prefs[Keys.PIN_SALT] = salt
            prefs[Keys.LOCK_ENABLED] = true
        }
    }

    companion object {
        @Volatile
        private var instance: SettingsStore? = null

        fun getInstance(context: Context): SettingsStore {
            return instance ?: synchronized(this) {
                instance ?: SettingsStore(context.applicationContext.settingsDataStore)
                    .also { instance = it }
            }
        }

        private fun parseIds(raw: String): List<Int> {
            if (raw.isEmpty()) return emptyList()
            return raw.split(",")
                .mapNotNull { part ->
                    val value = part.toIntOrNull()
                    if (value != null && value > 0) value else null
                }
        }
    }
}

data class SettingsSnapshot(
    val lockEnabled: Boolean,
    val pinHash: String,
    val pinSalt: String,
    val biometricEnabled: Boolean,
    val reminderEnabled: Boolean,
    val reminderHour: Int,
    val reminderMinute: Int,
    val reminderText: String,
    val reminderIds: List<Int>,
    val geshenEnabled: Boolean,
    val geshenReminderId: Int,
    val privacyModeEnabled: Boolean
) {
    val hasPin: Boolean get() = pinHash.isNotEmpty() && pinSalt.isNotEmpty()
}
