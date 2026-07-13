package com.otpbox.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class OtpEntry(
    val id: String,
    val issuer: String,
    val account: String,
    val secret: String,
    val algorithm: String = "SHA1",
    val digits: Int = 6,
    val period: Int = 30,
    val type: String = "TOTP",
    val color: Int? = null,
    val note: String? = null,
    val icon: String? = null,
    val sortOrder: Int = 0,
    val deleted: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)

/** Result of computing a code for display. */
data class OtpCode(
    val code: String,
    val remainingSeconds: Int,
    val progress: Float
)
