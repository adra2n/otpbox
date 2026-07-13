package com.otpbox.domain.parse

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class OtpAuthUriParserTest {

    @Test
    fun parsesFullUri() {
        val entry = OtpAuthUriParser.parse(
            "otpauth://totp/GitHub:me@example.com?secret=JBSWY3DPEHPK3PXP&issuer=GitHub&algorithm=SHA256&digits=8&period=60"
        )
        assertEquals("GitHub", entry.issuer)
        assertEquals("me@example.com", entry.account)
        assertEquals("JBSWY3DPEHPK3PXP", entry.secret)
        assertEquals("SHA256", entry.algorithm)
        assertEquals(8, entry.digits)
        assertEquals(60, entry.period)
    }

    @Test
    fun appliesDefaults() {
        val entry = OtpAuthUriParser.parse("otpauth://totp/Acme?secret=JBSWY3DPEHPK3PXP")
        assertEquals("SHA1", entry.algorithm)
        assertEquals(6, entry.digits)
        assertEquals(30, entry.period)
    }

    @Test
    fun issuerFromLabelWhenNoParam() {
        val entry = OtpAuthUriParser.parse("otpauth://totp/Acme:bob?secret=JBSWY3DPEHPK3PXP")
        assertEquals("Acme", entry.issuer)
        assertEquals("bob", entry.account)
    }

    @Test
    fun urlEncodedLabelDecoded() {
        val entry = OtpAuthUriParser.parse(
            "otpauth://totp/Big%20Corp%3Aalice%40big.com?secret=JBSWY3DPEHPK3PXP"
        )
        assertEquals("Big Corp", entry.issuer)
        assertEquals("alice@big.com", entry.account)
    }

    @Test
    fun missingSecretThrows() {
        assertThrows(IllegalArgumentException::class.java) {
            OtpAuthUriParser.parse("otpauth://totp/Acme?issuer=Acme")
        }
    }

    @Test
    fun invalidSecretThrows() {
        assertThrows(IllegalArgumentException::class.java) {
            OtpAuthUriParser.parse("otpauth://totp/Acme?secret=not-base32!!")
        }
    }

    @Test
    fun hotpRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            OtpAuthUriParser.parse("otpauth://hotp/Acme?secret=JBSWY3DPEHPK3PXP&counter=0")
        }
    }

    @Test
    fun detectsUri() {
        assertTrue(OtpAuthUriParser.isOtpAuthUri("otpauth://totp/x?secret=JBSWY3DPEHPK3PXP"))
    }
}
