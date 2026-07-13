package com.otpbox.domain.parse

import com.otpbox.domain.otp.Base32
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream

class GoogleAuthMigrationParserTest {

    private fun tag(field: Int, wire: Int) = (field shl 3) or wire

    private fun writeVarint(out: ByteArrayOutputStream, value: Long) {
        var v = value
        while (true) {
            val b = (v and 0x7F).toInt()
            v = v ushr 7
            if (v != 0L) out.write(b or 0x80) else { out.write(b); break }
        }
    }

    private fun writeLenField(out: ByteArrayOutputStream, field: Int, bytes: ByteArray) {
        out.write(tag(field, 2))
        writeVarint(out, bytes.size.toLong())
        out.write(bytes)
    }

    private fun writeVarintField(out: ByteArrayOutputStream, field: Int, value: Long) {
        out.write(tag(field, 0))
        writeVarint(out, value)
    }

    private fun buildOtpParameters(
        secret: ByteArray,
        name: String,
        issuer: String,
        algorithm: Int,
        digits: Int,
        type: Int
    ): ByteArray {
        val out = ByteArrayOutputStream()
        writeLenField(out, 1, secret)
        writeLenField(out, 2, name.toByteArray())
        writeLenField(out, 3, issuer.toByteArray())
        writeVarintField(out, 4, algorithm.toLong())
        writeVarintField(out, 5, digits.toLong())
        writeVarintField(out, 6, type.toLong())
        return out.toByteArray()
    }

    private fun buildPayload(vararg params: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        params.forEach { writeLenField(out, 1, it) }
        writeVarintField(out, 2, 1) // version
        return out.toByteArray()
    }

    @Test
    fun parsesSingleTotpEntry() {
        val secret = byteArrayOf(0x48, 0x65, 0x6C, 0x6C, 0x6F, 0x21)
        val payload = buildPayload(
            buildOtpParameters(secret, "alice@google.com", "Example", 1, 1, 2)
        )
        val entries = GoogleAuthMigrationParser.parsePayload(payload)
        assertEquals(1, entries.size)
        val e = entries[0]
        assertEquals("Example", e.issuer)
        assertEquals("alice@google.com", e.account)
        assertEquals(Base32.encode(secret), e.secret)
        assertEquals("SHA1", e.algorithm)
        assertEquals(6, e.digits)
    }

    @Test
    fun parsesMultipleEntriesAndAlgorithms() {
        val payload = buildPayload(
            buildOtpParameters(byteArrayOf(1, 2, 3), "a@x", "IssuerA", 2, 2, 2),
            buildOtpParameters(byteArrayOf(4, 5, 6), "b@y", "IssuerB", 3, 1, 2)
        )
        val entries = GoogleAuthMigrationParser.parsePayload(payload)
        assertEquals(2, entries.size)
        assertEquals("SHA256", entries[0].algorithm)
        assertEquals(8, entries[0].digits)
        assertEquals("SHA512", entries[1].algorithm)
        assertEquals(6, entries[1].digits)
    }

    @Test
    fun skipsNonTotpEntries() {
        val payload = buildPayload(
            buildOtpParameters(byteArrayOf(1, 2, 3), "hotp@x", "H", 1, 1, 1) // type=HOTP
        )
        val entries = GoogleAuthMigrationParser.parsePayload(payload)
        assertTrue(entries.isEmpty())
    }

    @Test
    fun splitsNameWithIssuerPrefix() {
        val payload = buildPayload(
            buildOtpParameters(byteArrayOf(9), "Corp:carol@corp.com", "", 1, 1, 2)
        )
        val entries = GoogleAuthMigrationParser.parsePayload(payload)
        assertEquals("Corp", entries[0].issuer)
        assertEquals("carol@corp.com", entries[0].account)
    }
}
