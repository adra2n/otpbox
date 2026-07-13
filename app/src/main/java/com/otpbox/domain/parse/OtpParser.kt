package com.otpbox.domain.parse

import com.otpbox.domain.model.OtpEntry

/** Dispatches a raw scanned/pasted string to the correct parser. */
object OtpParser {

    sealed interface Result {
        data class Success(val entries: List<OtpEntry>) : Result
        data class Error(val message: String) : Result
    }

    fun parse(raw: String): Result {
        val value = raw.trim()
        return try {
            when {
                GoogleAuthMigrationParser.isMigrationUri(value) ->
                    Result.Success(GoogleAuthMigrationParser.parse(value))
                OtpAuthUriParser.isOtpAuthUri(value) ->
                    Result.Success(listOf(OtpAuthUriParser.parse(value)))
                else -> Result.Error("Unrecognized QR code")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to parse")
        }
    }
}
