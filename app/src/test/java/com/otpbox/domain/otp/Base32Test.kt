package com.otpbox.domain.otp

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class Base32Test {

    @Test
    fun decodesRfc4648Vectors() {
        assertArrayEquals("f".toByteArray(), Base32.decode("MY"))
        assertArrayEquals("fo".toByteArray(), Base32.decode("MZXQ"))
        assertArrayEquals("foo".toByteArray(), Base32.decode("MZXW6"))
        assertArrayEquals("foob".toByteArray(), Base32.decode("MZXW6YQ"))
        assertArrayEquals("fooba".toByteArray(), Base32.decode("MZXW6YTB"))
        assertArrayEquals("foobar".toByteArray(), Base32.decode("MZXW6YTBOI"))
    }

    @Test
    fun decodeToleratesLowercaseSpacesAndPadding() {
        assertArrayEquals("foobar".toByteArray(), Base32.decode("mzxw6ytboi"))
        assertArrayEquals("foobar".toByteArray(), Base32.decode("MZXW 6YTB OI"))
        assertArrayEquals("foob".toByteArray(), Base32.decode("MZXW6YQ="))
    }

    @Test
    fun roundTripEncodeDecode() {
        val data = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
        assertArrayEquals(data, Base32.decode(Base32.encode(data)))
    }

    @Test
    fun invalidCharacterThrows() {
        assertThrows(IllegalArgumentException::class.java) { Base32.decode("MZXW6YTB1I") }
    }

    @Test
    fun emptyStringThrows() {
        assertThrows(IllegalArgumentException::class.java) { Base32.decode("") }
    }

    @Test
    fun isValidReportsCorrectly() {
        assertTrue(Base32.isValid("JBSWY3DPEHPK3PXP"))
        assertFalse(Base32.isValid("not base32 !!!"))
    }
}
