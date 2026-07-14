package com.otpbox.domain.parse

import com.otpbox.domain.model.OtpEntry
import com.otpbox.domain.otp.Base32
import java.net.URLDecoder
import java.util.UUID

/**
 * Parses `otpauth://totp/Issuer:account?secret=...&issuer=...&algorithm=...&digits=...&period=...`
 */
object OtpAuthUriParser {

    private val SUPPORTED_ALGORITHMS = setOf("SHA1", "SHA256", "SHA512")

    fun isOtpAuthUri(value: String): Boolean =
        value.trim().startsWith("otpauth://", ignoreCase = true)

    fun parse(uri: String): OtpEntry {
        val trimmed = uri.trim()
        require(trimmed.startsWith("otpauth://", ignoreCase = true)) { "Not an otpauth URI" }
        val rest = trimmed.substring("otpauth://".length)

        val slash = rest.indexOf('/')
        require(slash > 0) { "Missing OTP type" }
        val type = rest.substring(0, slash).lowercase()
        require(type == "totp") { "Unsupported OTP type: '$type' (only TOTP is supported)" }
        val isHotp = false

        val afterType = rest.substring(slash + 1)
        val q = afterType.indexOf('?')
        val labelEncoded = if (q >= 0) afterType.substring(0, q) else afterType
        val label = urlDecode(labelEncoded)
        val params = if (q >= 0) parseQuery(afterType.substring(q + 1)) else emptyMap()

        val secret = params["secret"]?.replace(" ", "")
            ?: throw IllegalArgumentException("Missing secret")
        require(Base32.isValid(secret)) { "Invalid Base32 secret" }

        val (issuerFromLabel, account) = parseLabel(label)
        val issuer = params["issuer"]?.takeIf { it.isNotBlank() } ?: issuerFromLabel ?: ""

        val algorithm = (params["algorithm"] ?: "SHA1").uppercase()
        require(algorithm in SUPPORTED_ALGORITHMS) { "Unsupported algorithm: $algorithm" }

        val digits = params["digits"]?.toIntOrNull() ?: 6
        require(digits in 4..10) { "Unsupported digits: $digits" }

        val period = if (isHotp) 30 else (params["period"]?.toIntOrNull() ?: 30)
        if (!isHotp) require(period in 1..300) { "Invalid period: $period" }

        val counter = if (isHotp) (params["counter"]?.toLongOrNull() ?: 0L) else 0L

        val now = System.currentTimeMillis()
        return OtpEntry(
            id = UUID.randomUUID().toString(),
            issuer = issuer,
            account = account,
            secret = secret,
            algorithm = algorithm,
            digits = digits,
            period = period,
            type = if (isHotp) "HOTP" else "TOTP",
            counter = counter,
            updatedAt = now,
            createdAt = now
        )
    }

    private fun parseLabel(label: String): Pair<String?, String> {
        val colon = label.indexOf(':')
        return if (colon >= 0) {
            label.substring(0, colon).trim().ifEmpty { null } to
                label.substring(colon + 1).trim()
        } else {
            null to label.trim()
        }
    }

    private fun parseQuery(query: String): Map<String, String> =
        query.split('&')
            .filter { it.isNotEmpty() }
            .associate {
                val eq = it.indexOf('=')
                if (eq >= 0) urlDecode(it.substring(0, eq)) to urlDecode(it.substring(eq + 1))
                else urlDecode(it) to ""
            }

    private fun urlDecode(value: String): String =
        try {
            URLDecoder.decode(value, "UTF-8")
        } catch (e: Exception) {
            value
        }
}
