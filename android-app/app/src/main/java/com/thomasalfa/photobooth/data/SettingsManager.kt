package com.thomasalfa.photobooth.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "kubik_settings")

class SettingsManager(private val context: Context) {
    companion object {
        val FRAME_PATH = stringPreferencesKey("custom_frame_path")
        val ADMIN_PIN = stringPreferencesKey("admin_pin")

        // Session Settings
        val PHOTO_COUNT = intPreferencesKey("photo_count")      // Default: 6
        val TIMER_DURATION = intPreferencesKey("timer_duration") // Default: 3 detik

        // Mode & Delay
        val CAPTURE_MODE = stringPreferencesKey("capture_mode") // "AUTO" atau "MANUAL"
        val AUTO_DELAY = intPreferencesKey("auto_delay")        // Jeda antar foto (Mode Auto)

        // --- SETTING BARU (Pindahkan ke sini biar rapi) ---
        val ACTIVE_EVENT = stringPreferencesKey("active_event")

        val DEVICE_ID = stringPreferencesKey("auth_device_id")
        val DEVICE_NAME = stringPreferencesKey("auth_device_name")
        val DEVICE_TYPE = stringPreferencesKey("auth_device_type") // 'RENTAL' atau 'VENDING'
        val IS_LOGGED_IN = booleanPreferencesKey("auth_is_logged_in")
    }

    // --- FLOWS (Baca Data) ---
    val framePathFlow: Flow<String?> = context.dataStore.data.map { it[FRAME_PATH] }
    val adminPinFlow: Flow<String> = context.dataStore.data.map { it[ADMIN_PIN] ?: "1234" }

    val photoCountFlow: Flow<Int> = context.dataStore.data.map { it[PHOTO_COUNT] ?: 6 }
    val timerDurationFlow: Flow<Int> = context.dataStore.data.map { it[TIMER_DURATION] ?: 3 }

    val captureModeFlow: Flow<String> = context.dataStore.data.map { it[CAPTURE_MODE] ?: "AUTO" }
    val autoDelayFlow: Flow<Int> = context.dataStore.data.map { it[AUTO_DELAY] ?: 2 }

    // Logic Active Event (Default "ALL")
    val activeEventFlow: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[ACTIVE_EVENT] ?: "ALL" }

    val deviceIdFlow: Flow<String?> = context.dataStore.data.map { it[DEVICE_ID] }
    val deviceNameFlow: Flow<String> = context.dataStore.data.map { it[DEVICE_NAME] ?: "Unknown Device" }
    val deviceTypeFlow: Flow<String> = context.dataStore.data.map { it[DEVICE_TYPE] ?: "RENTAL" }
    val isLoggedInFlow: Flow<Boolean> = context.dataStore.data.map { it[IS_LOGGED_IN] ?: false }


    suspend fun saveLoginSession(id: String, name: String, type: String) {
        context.dataStore.edit {
            it[DEVICE_ID] = id
            it[DEVICE_NAME] = name
            it[DEVICE_TYPE] = type
            it[IS_LOGGED_IN] = true
        }
    }

    suspend fun logout() {
        context.dataStore.edit {
            it[IS_LOGGED_IN] = false
            it[DEVICE_ID] = ""
            // Kita tidak perlu hapus data lain agar setup lebih cepat kalau login ulang
        }
    }

    // --- SAVE FUNCTIONS ---
    suspend fun saveFramePath(path: String) {
        context.dataStore.edit { it[FRAME_PATH] = path }
    }

    suspend fun saveSessionSettings(count: Int, timer: Int, mode: String, delay: Int) {
        context.dataStore.edit {
            it[PHOTO_COUNT] = count
            it[TIMER_DURATION] = timer
            it[CAPTURE_MODE] = mode
            it[AUTO_DELAY] = delay
        }
    }

    // Fungsi simpan Active Event
    suspend fun saveActiveEvent(event: String) {
        context.dataStore.edit { preferences ->
            preferences[ACTIVE_EVENT] = event
        }
    }
}