package com.otpbox.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.otpbox.domain.model.PasswordEntry

@Entity(tableName = "password_entry")
data class PasswordEntryEntity(
    @PrimaryKey val id: String,
    val title: String,
    val username: String,
    val password: String,
    val url: String?,
    val note: String?,
    val sortOrder: Int,
    val deleted: Boolean,
    val updatedAt: Long,
    val createdAt: Long
)

fun PasswordEntryEntity.toDomain(): PasswordEntry = PasswordEntry(
    id = id, title = title, username = username, password = password,
    url = url, note = note, sortOrder = sortOrder, deleted = deleted,
    updatedAt = updatedAt, createdAt = createdAt
)

fun PasswordEntry.toEntity(): PasswordEntryEntity = PasswordEntryEntity(
    id = id, title = title, username = username, password = password,
    url = url, note = note, sortOrder = sortOrder, deleted = deleted,
    updatedAt = updatedAt, createdAt = createdAt
)
