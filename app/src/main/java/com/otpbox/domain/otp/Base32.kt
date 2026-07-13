package com.otpbox.domain.otp

/**
 * RFC 4648 Base32 codec (no padding required on decode; tolerant of
 * lowercase and whitespace). Used for OTP secrets.
 */
object Base32 {

    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
    private val DECODE = IntArray(128) { -1 }.also { table ->
        ALPHABET.forEachIndexed { index, c ->
            table[c.code] = index
            table[c.lowercaseChar().code] = index
        }
    }

    fun decode(input: String): ByteArray {
        val cleaned = input.trim()
            .replace(" ", "")
            .replace("-", "")
            .trimEnd('=')
        if (cleaned.isEmpty()) throw IllegalArgumentException("Empty Base32 secret")

        val output = ArrayList<Byte>(cleaned.length * 5 / 8 + 1)
        var buffer = 0
        var bitsLeft = 0
        for (c in cleaned) {
            if (c.code >= 128 || DECODE[c.code] < 0) {
                throw IllegalArgumentException("Invalid Base32 character: '$c'")
            }
            buffer = (buffer shl 5) or DECODE[c.code]
            bitsLeft += 5
            if (bitsLeft >= 8) {
                bitsLeft -= 8
                output.add(((buffer shr bitsLeft) and 0xFF).toByte())
            }
        }
        return output.toByteArray()
    }

    fun encode(data: ByteArray): String {
        if (data.isEmpty()) return ""
        val sb = StringBuilder()
        var buffer = 0
        var bitsLeft = 0
        for (b in data) {
            buffer = (buffer shl 8) or (b.toInt() and 0xFF)
            bitsLeft += 8
            while (bitsLeft >= 5) {
                bitsLeft -= 5
                sb.append(ALPHABET[(buffer shr bitsLeft) and 0x1F])
            }
        }
        if (bitsLeft > 0) {
            sb.append(ALPHABET[(buffer shl (5 - bitsLeft)) and 0x1F])
        }
        return sb.toString()
    }

    fun isValid(input: String): Boolean = try {
        decode(input)
        true
    } catch (e: IllegalArgumentException) {
        false
    }
}
