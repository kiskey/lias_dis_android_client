// ====================================================================
// File: app/src/main/java/com/lias/remote/core/store/SettingsRepository.kt
// Version: 1.1.0
// Audit Fixes: 
//   1. Removed hardcoded default server URL to force first-run configuration (GAP-S03).
// ====================================================================

package com.lias.remote.core.store

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
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
    }

    // GAP-S03 Fix: Default to empty string instead of hardcoded IP
    val serverUrl: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[SERVER_URL] ?: "" }

    val authToken: Flow<String?> = context.dataStore.data
        .map { preferences -> preferences[AUTH_TOKEN] }

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
}
