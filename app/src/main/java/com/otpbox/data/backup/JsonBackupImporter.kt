package com.otpbox.data.backup

import com.otpbox.domain.model.OtpEntry
import com.otpbox.domain.model.PasswordEntry
import com.otpbox.domain.otp.Base32
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID

/**
 * Imports account entries from external JSON backups. Supports:
 *  - OTPBox own plaintext schema: { "version":1, "entries":[ ... ] }
 *  - Aegis unencrypted export:    { "db": { "entries":[ { "type","issuer","name","info":{...} } ] } }
 */
object JsonBackupImporter {

    private val json = Json { ignoreUnknownKeys = true }

    sealed interface Result {
        data class Success(
            val entries: List<OtpEntry>,
            val passwords: List<PasswordEntry> = emptyList()
        ) : Result
        data class Error(val message: String) : Result
    }

    fun import(text: String): Result = try {
        val root = json.parseToJsonElement(text).jsonObject
        when {
            root.containsKey("db") ->
                Result.Success(importAegis(root))
            root.containsKey("entries") -> {
                val own = importOwn(text)
                Result.Success(own.entries, own.passwords)
            }
            else -> Result.Error("Unrecognized backup file")
        }
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to import backup")
    }

    private fun importOwn(text: String): Result.Success {
        val content = json.decodeFromString<BackupContent>(text)
        val entries = content.entries.filter {
            it.type.equals("TOTP", ignoreCase = true) && !it.deleted
        }
        val passwords = content.passwords.filter { !it.deleted }
        return Result.Success(entries = entries, passwords = passwords)
    }

    private fun importAegis(root: kotlinx.serialization.json.JsonObject): List<OtpEntry> {
        val db = root["db"]
            ?: throw IllegalArgumentException("Missing db")
        val dbObj = db as? kotlinx.serialization.json.JsonObject
            ?: throw IllegalArgumentException(
                "Aegis backup is encrypted. Please export an unencrypted vault from Aegis first."
            )
        val entriesArray = dbObj["entries"]?.jsonArray ?: return emptyList()
        val now = System.currentTimeMillis()
        val result = mutableListOf<OtpEntry>()
        for (element in entriesArray) {
            val obj = element.jsonObject
            val type = obj["type"]?.jsonPrimitive?.content ?: continue
            if (!type.equals("totp", ignoreCase = true)) continue
            val info = obj["info"]?.jsonObject ?: continue
            val secret = info["secret"]?.jsonPrimitive?.content ?: continue
            if (!Base32.isValid(secret)) continue
            val issuer = obj["issuer"]?.jsonPrimitive?.content ?: ""
            val name = obj["name"]?.jsonPrimitive?.content ?: ""
            val note = obj["note"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
            val algo = info["algo"]?.jsonPrimitive?.content?.uppercase() ?: "SHA1"
            val digits = info["digits"]?.jsonPrimitive?.content?.toIntOrNull() ?: 6
            val period = info["period"]?.jsonPrimitive?.content?.toIntOrNull() ?: 30
            result.add(
                OtpEntry(
                    id = UUID.randomUUID().toString(),
                    issuer = issuer,
                    account = name,
                    secret = secret,
                    algorithm = if (algo in setOf("SHA1", "SHA256", "SHA512")) algo else "SHA1",
                    digits = digits,
                    period = period,
                    type = "TOTP",
                    note = note,
                    updatedAt = now,
                    createdAt = now
                )
            )
        }
        return result
    }
}
