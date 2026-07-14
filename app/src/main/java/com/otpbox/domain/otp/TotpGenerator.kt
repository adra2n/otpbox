package com.otpbox.domain.otp

import com.otpbox.domain.model.OtpCode
import com.otpbox.domain.model.OtpEntry
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.pow

/**
 * RFC 6238 TOTP generator (also supports the RFC 4226 HOTP truncation used
 * internally). Algorithms: SHA1 / SHA256 / SHA512. Digits: 6 or 8.
 */
object TotpGenerator {

    private fun macAlgorithm(algorithm: String): String = when (algorithm.uppercase()) {
        "SHA1" -> "HmacSHA1"
        "SHA256" -> "HmacSHA256"
        "SHA512" -> "HmacSHA512"
        else -> throw IllegalArgumentException("Unsupported algorithm: $algorithm")
    }

    /** Generate a code for an explicit counter (HOTP core). */
    fun generateForCounter(
        secret: ByteArray,
        counter: Long,
        algorithm: String,
        digits: Int
    ): String {
        val counterBytes = ByteArray(8)
        var value = counter
        for (i in 7 downTo 0) {
            counterBytes[i] = (value and 0xFF).toByte()
            value = value shr 8
        }
        val macAlgo = macAlgorithm(algorithm)
        val mac = Mac.getInstance(macAlgo)
        mac.init(SecretKeySpec(secret, macAlgo))
        val hash = mac.doFinal(counterBytes)

        val offset = hash[hash.size - 1].toInt() and 0x0F
        val binary = ((hash[offset].toInt() and 0x7F) shl 24) or
            ((hash[offset + 1].toInt() and 0xFF) shl 16) or
            ((hash[offset + 2].toInt() and 0xFF) shl 8) or
            (hash[offset + 3].toInt() and 0xFF)

        val modulo = 10.0.pow(digits).toInt()
        val otp = binary % modulo
        return otp.toString().padStart(digits, '0')
    }

    /** Generate a TOTP code from a Base32 secret at the given time. */
    fun generate(
        base32Secret: String,
        algorithm: String = "SHA1",
        digits: Int = 6,
        period: Int = 30,
        timeMillis: Long = System.currentTimeMillis()
    ): String {
        val secret = Base32.decode(base32Secret)
        val counter = timeMillis / 1000L / period
        return generateForCounter(secret, counter, algorithm, digits)
    }

    /** Convenience: generate code + countdown info for an entry. */
    fun codeFor(entry: OtpEntry, timeMillis: Long = System.currentTimeMillis()): OtpCode {
        return if (entry.type.equals("HOTP", ignoreCase = true)) {
            val code = generateForCounter(
                Base32.decode(entry.secret),
                entry.counter,
                entry.algorithm,
                entry.digits
            )
            OtpCode(code = code, remainingSeconds = 0, progress = 1f)
        } else {
            val code = generate(entry.secret, entry.algorithm, entry.digits, entry.period, timeMillis)
            val seconds = timeMillis / 1000L
            val remaining = (entry.period - (seconds % entry.period)).toInt()
            val progress = remaining.toFloat() / entry.period.toFloat()
            OtpCode(code = code, remainingSeconds = remaining, progress = progress)
        }
    }
}
