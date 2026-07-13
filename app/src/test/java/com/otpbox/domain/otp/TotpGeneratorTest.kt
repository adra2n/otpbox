package com.otpbox.domain.otp

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * RFC 6238 Appendix B test vectors.
 * Seeds are ASCII strings; we Base32-encode them to feed the generator.
 */
class TotpGeneratorTest {

    private val seedSha1 = "12345678901234567890".toByteArray()
    private val seedSha256 = "12345678901234567890123456789012".toByteArray()
    private val seedSha512 =
        "1234567890123456789012345678901234567890123456789012345678901234".toByteArray()

    private fun code(seed: ByteArray, algo: String, timeSec: Long): String =
        TotpGenerator.generate(
            base32Secret = Base32.encode(seed),
            algorithm = algo,
            digits = 8,
            period = 30,
            timeMillis = timeSec * 1000L
        )

    @Test
    fun sha1Vectors() {
        assertEquals("94287082", code(seedSha1, "SHA1", 59))
        assertEquals("07081804", code(seedSha1, "SHA1", 1111111109))
        assertEquals("14050471", code(seedSha1, "SHA1", 1111111111))
        assertEquals("89005924", code(seedSha1, "SHA1", 1234567890))
        assertEquals("69279037", code(seedSha1, "SHA1", 2000000000))
        assertEquals("65353130", code(seedSha1, "SHA1", 20000000000))
    }

    @Test
    fun sha256Vectors() {
        assertEquals("46119246", code(seedSha256, "SHA256", 59))
        assertEquals("68084774", code(seedSha256, "SHA256", 1111111109))
        assertEquals("91819424", code(seedSha256, "SHA256", 1234567890))
    }

    @Test
    fun sha512Vectors() {
        assertEquals("90693936", code(seedSha512, "SHA512", 59))
        assertEquals("25091201", code(seedSha512, "SHA512", 1111111109))
        assertEquals("93441116", code(seedSha512, "SHA512", 1234567890))
    }

    @Test
    fun sixDigitCodeIsPaddedAndTruncated() {
        val c = TotpGenerator.generate(Base32.encode(seedSha1), "SHA1", 6, 30, 59_000L)
        assertEquals(6, c.length)
        assertEquals("287082", c)
    }

    @Test
    fun countdownProgressComputed() {
        val entry = com.otpbox.domain.model.OtpEntry(
            id = "1", issuer = "T", account = "a",
            secret = Base32.encode(seedSha1)
        )
        val result = TotpGenerator.codeFor(entry, 45_000L)
        assertEquals(15, result.remainingSeconds)
        assertEquals(0.5f, result.progress, 0.0001f)
    }
}
