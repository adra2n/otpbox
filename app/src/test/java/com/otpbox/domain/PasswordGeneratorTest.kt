package com.otpbox.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordGeneratorTest {

    @Test fun lengthMatchesRequest() {
        val r = PasswordGenerator.generate(PasswordGenerator.Config(length = 20))
        assertTrue(r is PasswordGenerator.Result.Success)
        assertEquals(20, (r as PasswordGenerator.Result.Success).password.length)
    }

    @Test fun eachEnabledClassAppearsAtLeastOnce() {
        val pw = (PasswordGenerator.generate(
            PasswordGenerator.Config(useUpper = true, useLower = true, useDigits = true, useSymbols = true, excludeAmbiguous = false)
        ) as PasswordGenerator.Result.Success).password
        assertTrue(pw.any { it in 'A'..'Z' })
        assertTrue(pw.any { it in 'a'..'z' })
        assertTrue(pw.any { it in '0'..'9' })
        assertTrue(pw.any { "!@#\$%^&*()-_=+[]{};:,.<>?".contains(it) })
    }

    @Test fun onlySingleClassWhenOthersDisabled() {
        val pw = (PasswordGenerator.generate(
            PasswordGenerator.Config(useUpper = false, useLower = false, useDigits = true, useSymbols = false)
        ) as PasswordGenerator.Result.Success).password
        assertTrue(pw.all { it in '0'..'9' })
        assertFalse(pw.any { it in 'A'..'Z' || it in 'a'..'z' })
    }

    @Test fun excludeAmbiguousOmitsAmbiguousChars() {
        val pw = (PasswordGenerator.generate(
            PasswordGenerator.Config(useUpper = true, useLower = true, useDigits = true, useSymbols = true, excludeAmbiguous = true)
        ) as PasswordGenerator.Result.Success).password
        val ambiguous = setOf('O', '0', 'l', '1', 'I', '|')
        assertTrue(pw.none { it in ambiguous })
    }

    @Test fun errorWhenNoClassEnabled() {
        val r = PasswordGenerator.generate(
            PasswordGenerator.Config(useUpper = false, useLower = false, useDigits = false, useSymbols = false)
        )
        assertTrue(r is PasswordGenerator.Result.Error)
    }

    @Test fun lengthCoercedToMinimum() {
        val pw = (PasswordGenerator.generate(PasswordGenerator.Config(length = 1)) as PasswordGenerator.Result.Success).password
        assertEquals(8, pw.length)
    }
}
