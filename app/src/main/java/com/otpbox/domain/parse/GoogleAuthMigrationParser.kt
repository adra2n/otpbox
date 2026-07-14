package com.otpbox.domain.parse

import com.otpbox.domain.model.OtpEntry
import com.otpbox.domain.otp.Base32
import java.net.URLDecoder
import java.util.UUID

/**
 * Parses Google Authenticator export URIs:
 * `otpauth-migration://offline?data=<base64 protobuf MigrationPayload>`
 *
 * Uses a minimal protobuf wire-format reader (no generated classes).
 */
object GoogleAuthMigrationParser {

    fun isMigrationUri(value: String): Boolean =
        value.trim().startsWith("otpauth-migration://", ignoreCase = true)

    fun parse(uri: String): List<OtpEntry> {
        val trimmed = uri.trim()
        require(isMigrationUri(trimmed)) { "Not a migration URI" }
        val q = trimmed.indexOf("data=")
        require(q >= 0) { "Missing data parameter" }
        var dataParam = trimmed.substring(q + "data=".length)
        val amp = dataParam.indexOf('&')
        if (amp >= 0) dataParam = dataParam.substring(0, amp)
        val base64 = URLDecoder.decode(dataParam, "UTF-8")
        val payload = android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
        return parsePayload(payload)
    }

    /** Exposed for testing with a raw protobuf byte array. */
    fun parsePayload(payload: ByteArray): List<OtpEntry> {
        val reader = ProtoReader(payload)
        val entries = mutableListOf<OtpEntry>()
        while (reader.hasMore()) {
            val tag = reader.readVarint().toInt()
            val field = tag ushr 3
            val wireType = tag and 0x7
            if (field == 1 && wireType == 2) {
                val bytes = reader.readLengthDelimited()
                parseOtpParameters(bytes)?.let { entries.add(it) }
            } else {
                reader.skip(wireType)
            }
        }
        return entries
    }

    private fun parseOtpParameters(bytes: ByteArray): OtpEntry? {
        val reader = ProtoReader(bytes)
        var secret = ByteArray(0)
        var name = ""
        var issuer = ""
        var algorithm = 1 // default SHA1
        var digits = 1    // default SIX
        var type = 2      // default TOTP

        while (reader.hasMore()) {
            val tag = reader.readVarint().toInt()
            val field = tag ushr 3
            val wireType = tag and 0x7
            when (field) {
                1 -> secret = reader.readLengthDelimited()
                2 -> name = String(reader.readLengthDelimited(), Charsets.UTF_8)
                3 -> issuer = String(reader.readLengthDelimited(), Charsets.UTF_8)
                4 -> algorithm = reader.readVarint().toInt()
                5 -> digits = reader.readVarint().toInt()
                6 -> type = reader.readVarint().toInt()
                else -> reader.skip(wireType)
            }
        }

        if (type != 2) return null // only TOTP(2); HOTP(1) is not supported
        if (secret.isEmpty()) return null

        val algoName = when (algorithm) {
             2 -> "SHA256"
             3 -> "SHA512"
             else -> "SHA1"
        }
        val digitCount = if (digits == 2) 8 else 6

        val (issuerFromName, account) = splitName(name, issuer)
        val finalIssuer = issuer.ifBlank { issuerFromName }

        val now = System.currentTimeMillis()
        return OtpEntry(
            id = UUID.randomUUID().toString(),
            issuer = finalIssuer,
            account = account,
            secret = Base32.encode(secret),
            algorithm = algoName,
            digits = digitCount,
            period = 30,
            type = "TOTP",
            counter = 0L,
            updatedAt = now,
            createdAt = now
        )
    }

    private fun splitName(name: String, issuer: String): Pair<String, String> {
        val colon = name.indexOf(':')
        return if (colon >= 0) {
            name.substring(0, colon).trim() to name.substring(colon + 1).trim()
        } else {
            issuer to name.trim()
        }
    }

    /** Minimal protobuf wire-format reader. */
    private class ProtoReader(private val data: ByteArray) {
        private var pos = 0

        fun hasMore(): Boolean = pos < data.size

        fun readVarint(): Long {
            var result = 0L
            var shift = 0
            while (true) {
                require(pos < data.size) { "Truncated varint" }
                val b = data[pos++].toInt() and 0xFF
                result = result or ((b and 0x7F).toLong() shl shift)
                if (b and 0x80 == 0) break
                shift += 7
            }
            return result
        }

        fun readLengthDelimited(): ByteArray {
            val length = readVarint().toInt()
            require(pos + length <= data.size) { "Truncated length-delimited field" }
            val out = data.copyOfRange(pos, pos + length)
            pos += length
            return out
        }

        fun skip(wireType: Int) {
            when (wireType) {
                0 -> readVarint()
                1 -> pos += 8
                2 -> { val len = readVarint().toInt(); pos += len }
                5 -> pos += 4
                else -> throw IllegalArgumentException("Unknown wire type: $wireType")
            }
        }
    }
}
