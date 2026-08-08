// ====================================================================
// File: app/src/main/java/com/lias/remote/core/store/SettingsRepository.kt
// Version: 12.0.0
//
// Purpose:
//   Persistent non-sensitive preferences + secure credential storage.
//
// Changes:
//   - Auth token moved out of ordinary DataStore.
//   - Existing plaintext auth_token is migrated once to Keystore-backed
//     storage and then removed.
//   - Adds persistent advanced-controls disclosure.
//   - Keeps all existing public settings APIs compatible.
// ====================================================================

package com.lias.remote.core.store

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.dataStore:
    DataStore<Preferences>
    by preferencesDataStore(
        name =
            "lias_settings"
    )

class SettingsRepository(
    private val context: Context
) {

    private val secureTokenStore =
        SecureTokenStore(
            context
        )

    private val migrationScope =
        CoroutineScope(
            SupervisorJob() +
                Dispatchers.IO
        )

    companion object {

        val SERVER_URL =
            stringPreferencesKey(
                "server_url"
            )

        /**
         * Legacy plaintext preference.
         *
         * Do not use for new writes.
         * Retained solely for one-time migration.
         */
        private val LEGACY_AUTH_TOKEN =
            stringPreferencesKey(
                "auth_token"
            )

        val THEME_MODE =
            stringPreferencesKey(
                "theme_mode"
            )

        val ONBOARDED =
            booleanPreferencesKey(
                "onboarded"
            )

        val ADVANCED_MODE =
            booleanPreferencesKey(
                "advanced_mode"
            )
    }

    init {
        migrateLegacyToken()
    }

    val serverUrl:
        Flow<String> =
        context.dataStore.data
            .map { preferences ->

                preferences[
                    SERVER_URL
                ] ?: ""
            }

    /**
     * Credential source is now Keystore-backed.
     */
    val authToken:
        Flow<String?> =
        secureTokenStore.token

    val themeMode:
        Flow<String> =
        context.dataStore.data
            .map { preferences ->

                preferences[
                    THEME_MODE
                ] ?: "system"
            }

    val isOnboarded:
        Flow<Boolean> =
        context.dataStore.data
            .map { preferences ->

                preferences[
                    ONBOARDED
                ] ?: false
            }

    val advancedMode:
        Flow<Boolean> =
        context.dataStore.data
            .map { preferences ->

                preferences[
                    ADVANCED_MODE
                ] ?: false
            }

    suspend fun saveServerUrl(
        url: String
    ) {

        context.dataStore.edit {
                preferences ->

            preferences[
                SERVER_URL
            ] =
                url.trim()
        }
    }

    suspend fun saveAuthToken(
        token: String?
    ) {

        secureTokenStore.saveToken(
            token
        )
    }

    suspend fun saveThemeMode(
        mode: String
    ) {

        val normalized =
            when (
                mode
                    .trim()
                    .lowercase()
            ) {
                "light" ->
                    "light"

                "dark" ->
                    "dark"

                else ->
                    "system"
            }

        context.dataStore.edit {
                preferences ->

            preferences[
                THEME_MODE
            ] =
                normalized
        }
    }

    suspend fun setOnboarded(
        value: Boolean
    ) {

        context.dataStore.edit {
                preferences ->

            preferences[
                ONBOARDED
            ] =
                value
        }
    }

    suspend fun setAdvancedMode(
        value: Boolean
    ) {

        context.dataStore.edit {
                preferences ->

            preferences[
                ADVANCED_MODE
            ] =
                value
        }
    }

    private fun migrateLegacyToken() {

        migrationScope.launch {

            try {
                val preferences =
                    context.dataStore
                        .data
                        .first()

                val legacyToken =
                    preferences[
                        LEGACY_AUTH_TOKEN
                    ]
                        ?.trim()
                        ?.takeIf {
                            it.isNotBlank()
                        }

                if (
                    legacyToken !=
                        null &&
                    !secureTokenStore
                        .hasToken()
                ) {
                    secureTokenStore
                        .saveToken(
                            legacyToken
                        )
                }

                /*
                 * Remove plaintext storage regardless of whether a token
                 * was present. Future app versions must not recreate it.
                 */
                context.dataStore.edit {
                        mutablePreferences ->

                    mutablePreferences
                        .remove(
                            LEGACY_AUTH_TOKEN
                        )
                }

            } catch (
                _: Exception
            ) {
                /*
                 * Migration failure must not destroy the rest of
                 * application settings.
                 *
                 * The user can re-enter their token from Connection.
                 */
            }
        }
    }
}
