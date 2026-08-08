// ====================================================================
// File: SettingsRepository.kt
// Version: 3.1.0 (HIG Redesign)
// Purpose: Added ONBOARDED preference key for first-run onboarding flow.
// ====================================================================

package com.lias.remote.core.store

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "lias_settings")

class SettingsRepository(private val context: Context) {

    companion object {
        val SERVER_URL = stringPreferencesKey("server_url")
        val AUTH_TOKEN = stringPreferencesKey("auth_token")
        val THEME_MODE = stringPreferencesKey("theme_mode") // "system", "light", "dark"
        val ONBOARDED = booleanPreferencesKey("onboarded")
    }

    val serverUrl: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[SERVER_URL] ?: "" }

    val authToken: Flow<String?> = context.dataStore.data
        .map { preferences -> preferences[AUTH_TOKEN] }

    val themeMode: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[THEME_MODE] ?: "system" }

    val isOnboarded: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[ONBOARDED] ?: false }

    suspend fun saveServerUrl(url: String) {
        context.dataStore.edit { preferences ->
            preferences[SERVER_URL] = url
        }
    }

    suspend fun saveAuthToken(token: String?) {
        context.dataStore.edit { preferences ->
            if (token.isNullOrBlank()) {
                preferences.remove(AUTH_TOKEN)
            } else {
                preferences[AUTH_TOKEN] = token
            }
        }
    }

    suspend fun saveThemeMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE] = mode
        }
    }

    suspend fun setOnboarded(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ONBOARDED] = value
        }
    }
}
