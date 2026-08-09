package com.lias.remote.ui.components

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity

object DestructiveBiometricAuth {
    fun authenticate(
        activity: FragmentActivity?,
        objectLabel: String,
        onAuthenticated: () -> Unit,
        onUnavailable: (String) -> Unit
    ) {
        if (activity == null) {
            onUnavailable("Biometric verification is unavailable in the current app window.")
            return
        }

        val authenticators =
            BiometricManager.Authenticators.BIOMETRIC_STRONG

        when (
            BiometricManager.from(activity)
                .canAuthenticate(authenticators)
        ) {
            BiometricManager.BIOMETRIC_SUCCESS -> Unit
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                onUnavailable(
                    "Set up a strong fingerprint or biometric in Android Settings before deleting saved LIAS configuration."
                )
                return
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                onUnavailable("This device does not provide strong biometric authentication.")
                return
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                onUnavailable("Biometric hardware is temporarily unavailable. Try again.")
                return
            }
            else -> {
                onUnavailable("Strong biometric authentication is currently unavailable.")
                return
            }
        }

        val prompt =
            BiometricPrompt(
                activity,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(
                        result: BiometricPrompt.AuthenticationResult
                    ) {
                        super.onAuthenticationSucceeded(result)
                        onAuthenticated()
                    }

                    override fun onAuthenticationError(
                        errorCode: Int,
                        errString: CharSequence
                    ) {
                        super.onAuthenticationError(errorCode, errString)

                        if (
                            errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                            errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                            errorCode == BiometricPrompt.ERROR_CANCELED
                        ) {
                            return
                        }

                        onUnavailable(
                            errString.toString().ifBlank {
                                "Biometric verification failed."
                            }
                        )
                    }
                }
            )

        val info =
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Verify identity")
                .setSubtitle("Authenticate to permanently delete $objectLabel.")
                .setAllowedAuthenticators(authenticators)
                .setNegativeButtonText("Cancel")
                .setConfirmationRequired(true)
                .build()

        prompt.authenticate(info)
    }
}

fun Context.findFragmentActivity(): FragmentActivity? {
    var current: Context? = this

    while (current != null) {
        when (current) {
            is FragmentActivity -> return current
            is ContextWrapper -> current = current.baseContext
            else -> return null
        }
    }

    return null
}

fun requiresProtectedDelete(
    serverId: String
): Boolean = serverId.isNotBlank()
