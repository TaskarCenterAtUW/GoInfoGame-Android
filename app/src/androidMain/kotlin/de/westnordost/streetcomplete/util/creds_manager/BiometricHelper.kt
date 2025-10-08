package de.westnordost.streetcomplete.util.creds_manager


import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

class BiometricHelper(
    private val context: Context,
    private val activity: FragmentActivity,
    private val onSuccess: () -> Unit,
    private val onFailure: (() -> Unit)? = null,
) {
    private val executor = ContextCompat.getMainExecutor(context)

    private val promptInfo: BiometricPrompt.PromptInfo =
        BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric Authentication")
            .setSubtitle("Authenticate using your biometrics")
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    // API 30+ can use device credential fallback
                    setAllowedAuthenticators(
                        BiometricManager.Authenticators.BIOMETRIC_STRONG or
                            BiometricManager.Authenticators.DEVICE_CREDENTIAL
                    )
                } else {
                    // Use fallback cancel button for pre-API 30
                    setNegativeButtonText("Cancel")
                }
            }.build()

    private val biometricPrompt = BiometricPrompt(
        activity,
        executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                Toast.makeText(context, "Authentication error: $errString", Toast.LENGTH_SHORT)
                    .show()
                onFailure?.invoke()
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Toast.makeText(context, "Authentication failed", Toast.LENGTH_SHORT).show()
                onFailure?.invoke()
            }
        }
    )

    private fun canAuthenticate(): Boolean {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun authenticate() {
        if (!canAuthenticate()) {
            Toast.makeText(context, "Biometric authentication not available", Toast.LENGTH_SHORT)
                .show()
            onFailure?.invoke()
            return
        }

        biometricPrompt.authenticate(promptInfo)
    }
}

