package com.eblansoft.flashlight2.security

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/** Small wrapper around AndroidX BiometricPrompt used by the flashlight lock. */
object BiometricAuth {

    // Weak biometrics only: combining with DEVICE_CREDENTIAL is rejected on API < 30.
    private const val AUTHENTICATORS = BiometricManager.Authenticators.BIOMETRIC_WEAK

    /** True if the device can actually do biometric (or device-credential) auth. */
    fun isAvailable(activity: FragmentActivity): Boolean {
        val status = BiometricManager.from(activity).canAuthenticate(AUTHENTICATORS)
        return status == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun prompt(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit = {},
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                onError(errString.toString())
            }
        }

        val prompt = BiometricPrompt(activity, executor, callback)
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Разблокировка фонарика")
            .setSubtitle("Еблан Софт проверяет, точно ли это вы")
            .setAllowedAuthenticators(AUTHENTICATORS)
            .setNegativeButtonText("Отмена")
            .build()
        prompt.authenticate(info)
    }
}
