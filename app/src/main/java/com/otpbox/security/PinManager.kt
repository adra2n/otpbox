package com.otpbox.security

import android.util.Base64
import com.otpbox.data.crypto.SecurePrefs
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/** Sets and verifies the 6-digit backup PIN using a salted PBKDF2 hash. */
@Singleton
class PinManager @Inject constructor(
    private val securePrefs: SecurePrefs
) {

    val isPinSet: Boolean get() = !securePrefs.pinHash.isNullOrBlank()

    fun setPin(pin: String) {
        require(pin.length in 4..8 && pin.all { it.isDigit() }) { "PIN must be 4-8 digits" }
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = derive(pin, salt)
        securePrefs.pinHash = "${b64(salt)}:${b64(hash)}"
    }

    fun clearPin() {
        securePrefs.pinHash = null
    }

    fun verify(pin: String): Boolean {
        val stored = securePrefs.pinHash ?: return false
        val parts = stored.split(":")
        if (parts.size != 2) return false
        val salt = runCatching { Base64.decode(parts[0], Base64.NO_WRAP) }.getOrNull() ?: return false
        val expected = runCatching { Base64.decode(parts[1], Base64.NO_WRAP) }.getOrNull() ?: return false
        val actual = derive(pin, salt)
        return constantTimeEquals(expected, actual)
    }

    private fun derive(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }

    private fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var result = 0
        for (i in a.indices) result = result or (a[i].toInt() xor b[i].toInt())
        return result == 0
    }

    private fun b64(bytes: ByteArray) = Base64.encodeToString(bytes, Base64.NO_WRAP)

    companion object {
        private const val ITERATIONS = 120_000
        private const val KEY_BITS = 256
    }
}
