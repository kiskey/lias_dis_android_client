// ====================================================================
// File: app/src/main/java/com/lias/remote/core/store/SecureTokenStore.kt
// Version: 12.0.0
//
// Purpose:
//   Store the LIAS bearer token encrypted with an Android Keystore key.
//
// Security model:
//   - AES-256/GCM key lives in AndroidKeyStore.
//   - Only ciphertext + IV are placed in SharedPreferences.
//   - The plaintext token is never persisted in DataStore.
//   - If the Keystore key becomes invalid/unavailable, unreadable
//     credentials are discarded rather than exposed or guessed.
//
// No additional androidx.security dependency is required.
// Compatible with the project's minSdk 26.
// ====================================================================

package com.lias.remote.core.store

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SecureTokenStore(
    context: Context
) {

    private val preferences =
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

    private val keyStore =
        KeyStore.getInstance(
            ANDROID_KEYSTORE
        ).apply {
            load(null)
        }

    private val _token =
        MutableStateFlow(
            loadToken()
        )

    val token:
        StateFlow<String?> =
        _token.asStateFlow()

    @Synchronized
    fun saveToken(
        token: String?
    ) {
        val normalized =
            token
                ?.trim()
                ?.takeIf {
                    it.isNotBlank()
                }

        if (
            normalized == null
        ) {
            clear()
            return
        }

        try {
            val cipher =
                Cipher.getInstance(
                    TRANSFORMATION
                )

            cipher.init(
                Cipher.ENCRYPT_MODE,
                getOrCreateKey()
            )

            val ciphertext =
                cipher.doFinal(
                    normalized.toByteArray(
                        StandardCharsets.UTF_8
                    )
                )

            val iv =
                cipher.iv

            preferences
                .edit()
                .putString(
                    KEY_CIPHERTEXT,
                    Base64.encodeToString(
                        ciphertext,
                        Base64.NO_WRAP
                    )
                )
                .putString(
                    KEY_IV,
                    Base64.encodeToString(
                        iv,
                        Base64.NO_WRAP
                    )
                )
                .apply()

            _token.value =
                normalized

        } catch (
            error: Exception
        ) {
            /*
             * Never silently persist a plaintext fallback.
             *
             * A broken Keystore means the credential cannot safely be
             * persisted. Clear any partially written credential state.
             */
            clear()

            throw IllegalStateException(
                "Unable to securely store LIAS authentication token.",
                error
            )
        }
    }

    @Synchronized
    fun clear() {
        preferences
            .edit()
            .remove(
                KEY_CIPHERTEXT
            )
            .remove(
                KEY_IV
            )
            .apply()

        _token.value =
            null
    }

    fun hasToken(): Boolean =
        !_token.value
            .isNullOrBlank()

    private fun loadToken():
        String? {

        val ciphertextBase64 =
            preferences.getString(
                KEY_CIPHERTEXT,
                null
            )
                ?: return null

        val ivBase64 =
            preferences.getString(
                KEY_IV,
                null
            )
                ?: return null

        return try {
            val ciphertext =
                Base64.decode(
                    ciphertextBase64,
                    Base64.NO_WRAP
                )

            val iv =
                Base64.decode(
                    ivBase64,
                    Base64.NO_WRAP
                )

            val cipher =
                Cipher.getInstance(
                    TRANSFORMATION
                )

            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateKey(),
                GCMParameterSpec(
                    GCM_TAG_BITS,
                    iv
                )
            )

            String(
                cipher.doFinal(
                    ciphertext
                ),
                StandardCharsets.UTF_8
            )
                .trim()
                .takeIf {
                    it.isNotBlank()
                }

        } catch (
            _: Exception
        ) {
            /*
             * This commonly occurs after restore to another device,
             * Keystore invalidation, or corrupt application data.
             *
             * The encrypted bytes are useless without the original key.
             */
            preferences
                .edit()
                .remove(
                    KEY_CIPHERTEXT
                )
                .remove(
                    KEY_IV
                )
                .apply()

            null
        }
    }

    private fun getOrCreateKey():
        SecretKey {

        val existing =
            keyStore.getKey(
                KEY_ALIAS,
                null
            ) as? SecretKey

        if (
            existing != null
        ) {
            return existing
        }

        val generator =
            KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )

        val specification =
            KeyGenParameterSpec
                .Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or
                        KeyProperties.PURPOSE_DECRYPT
                )
                .setBlockModes(
                    KeyProperties.BLOCK_MODE_GCM
                )
                .setEncryptionPaddings(
                    KeyProperties.ENCRYPTION_PADDING_NONE
                )
                .setKeySize(
                    256
                )
                .setRandomizedEncryptionRequired(
                    true
                )
                .build()

        generator.init(
            specification
        )

        return generator
            .generateKey()
    }

    private companion object {

        const val PREFS_NAME =
            "lias_secure_credentials"

        const val KEY_ALIAS =
            "lias_remote_auth_token_v1"

        const val KEY_CIPHERTEXT =
            "auth_token_ciphertext"

        const val KEY_IV =
            "auth_token_iv"

        const val ANDROID_KEYSTORE =
            "AndroidKeyStore"

        const val TRANSFORMATION =
            "AES/GCM/NoPadding"

        const val GCM_TAG_BITS =
            128
    }
}
