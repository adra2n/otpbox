package com.otpbox.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.otpbox.domain.model.OtpEntry

@Entity(tableName = "otp_entries")
data class OtpEntryEntity(
    @PrimaryKey val id: String,
    val issuer: String,
    val account: String,
    val secret: String,
    val algorithm: String,
    val digits: Int,
    val period: Int,
    val type: String,
    val color: Int?,
    val note: String?,
    val icon: String?,
    val sortOrder: Int,
    val deleted: Boolean,
    val updatedAt: Long,
    val createdAt: Long
)

fun OtpEntryEntity.toDomain(): OtpEntry = OtpEntry(
    id = id,
    issuer = issuer,
    account = account,
    secret = secret,
    algorithm = algorithm,
    digits = digits,
    period = period,
    type = type,
    color = color,
    note = note,
    icon = icon,
    sortOrder = sortOrder,
    deleted = deleted,
    updatedAt = updatedAt,
    createdAt = createdAt
)

fun OtpEntry.toEntity(): OtpEntryEntity = OtpEntryEntity(
    id = id,
    issuer = issuer,
    account = account,
    secret = secret,
    algorithm = algorithm,
    digits = digits,
    period = period,
    type = type,
    color = color,
    note = note,
    icon = icon,
    sortOrder = sortOrder,
    deleted = deleted,
    updatedAt = updatedAt,
    createdAt = createdAt
)
