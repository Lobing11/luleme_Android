package com.luleme.android.logic

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.security.MessageDigest
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

fun sha256Hex(input: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val bytes = digest.digest(input.toByteArray(Charsets.UTF_8))
    return bytes.joinToString("") { b -> "%02x".format(b) }
}

fun randomSalt(): String = UUID.randomUUID().toString()

fun hashPin(pin: String, salt: String): String = sha256Hex("$salt:$pin")

fun verifyPin(pin: String, salt: String, storedHash: String): Boolean =
    hashPin(pin, salt) == storedHash

data class BiometricStatus(
    val hardwareSupported: Boolean,
    val enrolled: Boolean
)

fun getBiometricStatus(context: Context): BiometricStatus {
    val manager = BiometricManager.from(context)
    val authenticators = BiometricManager.Authenticators.BIOMETRIC_WEAK
    return when (manager.canAuthenticate(authenticators)) {
        BiometricManager.BIOMETRIC_SUCCESS ->
            BiometricStatus(hardwareSupported = true, enrolled = true)
        BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
            BiometricStatus(hardwareSupported = true, enrolled = false)
        BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
        BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE,
        BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED ->
            BiometricStatus(hardwareSupported = false, enrolled = false)
        else ->
            BiometricStatus(hardwareSupported = true, enrolled = false)
    }
}

fun isBiometricAvailable(context: Context): Boolean {
    val st = getBiometricStatus(context)
    return st.hardwareSupported && st.enrolled
}

suspend fun startBiometricAuth(activity: FragmentActivity, title: String): Boolean =
    suspendCoroutine { cont ->
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    cont.resume(true)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    cont.resume(false)
                }

                override fun onAuthenticationFailed() {
                    // Keep waiting for success/error; do not resume here.
                }
            }
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setNegativeButtonText("使用 PIN")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .build()
        prompt.authenticate(info)
    }
