package com.otpbox.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JsonBackupImporterTest {

    @Test
    fun importsOwnSchemaWithPasswords() {
        val text = """
            {"version":2,"entries":[
              {"id":"1","issuer":"GitHub","account":"me","secret":"JBSWY3DPEHPK3PXP",
               "algorithm":"SHA1","digits":6,"period":30,"type":"TOTP","sortOrder":0,
               "deleted":false,"updatedAt":1,"createdAt":1}
            ],"passwords":[
              {"id":"p1","title":"Email","username":"me@x.com","password":"secret",
               "sortOrder":0,"deleted":false,"updatedAt":1,"createdAt":1}
            ]}
        """.trimIndent()
        val result = JsonBackupImporter.import(text)
        assertTrue(result is JsonBackupImporter.Result.Success)
        result as JsonBackupImporter.Result.Success
        assertEquals(1, result.entries.size)
        assertEquals(1, result.passwords.size)
        assertEquals("Email", result.passwords[0].title)
    }

    @Test
    fun oldOwnSchemaWithoutPasswordsStillImports() {
        val text = """
            {"version":1,"entries":[
              {"id":"1","issuer":"GitHub","account":"me","secret":"JBSWY3DPEHPK3PXP",
               "algorithm":"SHA1","digits":6,"period":30,"type":"TOTP","sortOrder":0,
               "deleted":false,"updatedAt":1,"createdAt":1}
            ]}
        """.trimIndent()
        val result = JsonBackupImporter.import(text)
        assertTrue(result is JsonBackupImporter.Result.Success)
        result as JsonBackupImporter.Result.Success
        assertEquals(1, result.entries.size)
        assertEquals(0, result.passwords.size)
    }

    @Test
    fun importsOwnSchema() {
        val text = """
            {"version":1,"entries":[
              {"id":"1","issuer":"GitHub","account":"me","secret":"JBSWY3DPEHPK3PXP",
               "algorithm":"SHA1","digits":6,"period":30,"type":"TOTP","sortOrder":0,
               "deleted":false,"updatedAt":1,"createdAt":1}
            ]}
        """.trimIndent()
        val result = JsonBackupImporter.import(text)
        assertTrue(result is JsonBackupImporter.Result.Success)
        val entries = (result as JsonBackupImporter.Result.Success).entries
        assertEquals(1, entries.size)
        assertEquals("GitHub", entries[0].issuer)
    }

    @Test
    fun importsAegisUnencrypted() {
        val text = """
            {"version":1,"header":{"slots":null,"params":null},
             "db":{"version":2,"entries":[
               {"type":"totp","uuid":"x","name":"alice@x.com","issuer":"Example","note":"work",
                "info":{"secret":"JBSWY3DPEHPK3PXP","algo":"SHA256","digits":8,"period":30}},
               {"type":"hotp","uuid":"y","name":"skip","issuer":"H",
                "info":{"secret":"JBSWY3DPEHPK3PXP","algo":"SHA1","digits":6,"counter":0}}
             ]}}
        """.trimIndent()
        val result = JsonBackupImporter.import(text)
        assertTrue(result is JsonBackupImporter.Result.Success)
        val entries = (result as JsonBackupImporter.Result.Success).entries
        assertEquals(1, entries.size)
        assertEquals("Example", entries[0].issuer)
        assertEquals("alice@x.com", entries[0].account)
        assertEquals("SHA256", entries[0].algorithm)
        assertEquals(8, entries[0].digits)
        assertEquals("work", entries[0].note)
    }

    @Test
    fun encryptedAegisReturnsError() {
        val text = """{"version":1,"header":{"slots":[{}]},"db":"base64cipher=="}"""
        val result = JsonBackupImporter.import(text)
        assertTrue(result is JsonBackupImporter.Result.Error)
    }

    @Test
    fun unrecognizedReturnsError() {
        val result = JsonBackupImporter.import("""{"foo":"bar"}""")
        assertTrue(result is JsonBackupImporter.Result.Error)
    }
}
