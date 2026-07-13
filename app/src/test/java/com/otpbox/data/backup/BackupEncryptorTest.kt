package com.otpbox.data.backup

import com.otpbox.domain.model.OtpEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupEncryptorTest {

    private val encryptor = BackupEncryptor()

    private val sample = BackupContent(
        entries = listOf(
            OtpEntry(id = "1", issuer = "GitHub", account = "me", secret = "JBSWY3DPEHPK3PXP"),
            OtpEntry(id = "2", issuer = "Google", account = "you", secret = "GEZDGNBVGY3TQOJQ", digits = 8)
        )
    )

    @Test
    fun roundTripRestoresContent() {
        val envelope = encryptor.encrypt(sample, "correct horse battery staple")
        val restored = encryptor.decrypt(envelope, "correct horse battery staple")
        assertEquals(sample.entries, restored.entries)
    }

    @Test
    fun envelopeContainsNoPlaintextSecret() {
        val envelope = encryptor.encrypt(sample, "pw")
        assertTrue(!envelope.contains("JBSWY3DPEHPK3PXP"))
        assertTrue(envelope.contains("otpbox"))
    }

    @Test
    fun wrongPasswordThrows() {
        val envelope = encryptor.encrypt(sample, "right")
        assertThrows(BackupEncryptor.WrongPasswordException::class.java) {
            encryptor.decrypt(envelope, "wrong")
        }
    }

    @Test
    fun emptyPasswordRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            encryptor.encrypt(sample, "")
        }
    }
}
