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

        // --- SETTING BARU ---
        val CAPTURE_MODE = stringPreferencesKey("capture_mode") // "AUTO" atau "MANUAL"
        val AUTO_DELAY = intPreferencesKey("auto_delay")        // Jeda antar foto (Mode Auto)
    }

    // --- FLOWS (Baca Data) ---
    val framePathFlow: Flow<String?> = context.dataStore.data.map { it[FRAME_PATH] }
    val adminPinFlow: Flow<String> = context.dataStore.data.map { it[ADMIN_PIN] ?: "1234" }

    val photoCountFlow: Flow<Int> = context.dataStore.data.map { it[PHOTO_COUNT] ?: 6 }
    val timerDurationFlow: Flow<Int> = context.dataStore.data.map { it[TIMER_DURATION] ?: 3 }

    // Default Mode: AUTO, Default Delay: 2 Detik
    val captureModeFlow: Flow<String> = context.dataStore.data.map { it[CAPTURE_MODE] ?: "AUTO" }
    val autoDelayFlow: Flow<Int> = context.dataStore.data.map { it[AUTO_DELAY] ?: 2 }

    // --- SAVE FUNCTIONS ---
    suspend fun saveFramePath(path: String) {
        context.dataStore.edit { it[FRAME_PATH] = path }
    }

    // Update fungsi save untuk handle parameter baru
    suspend fun saveSessionSettings(count: Int, timer: Int, mode: String, delay: Int) {
        context.dataStore.edit {
            it[PHOTO_COUNT] = count
            it[TIMER_DURATION] = timer
            it[CAPTURE_MODE] = mode
            it[AUTO_DELAY] = delay
        }
    }
}