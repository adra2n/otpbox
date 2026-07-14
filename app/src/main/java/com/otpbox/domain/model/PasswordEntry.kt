package com.otpbox.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class PasswordEntry(
    val id: String,
    val title: String,
    val username: String,
    val password: String,
    val url: String? = null,
    val note: String? = null,
    val sortOrder: Int = 0,
    val deleted: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)
