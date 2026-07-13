package com.otpbox.data.repo

import com.otpbox.domain.model.OtpEntry
import kotlinx.coroutines.flow.Flow

interface OtpRepository {
    fun observeEntries(): Flow<List<OtpEntry>>
    suspend fun getActiveEntries(): List<OtpEntry>
    suspend fun getAllIncludingDeleted(): List<OtpEntry>
    suspend fun getById(id: String): OtpEntry?
    suspend fun add(entry: OtpEntry)
    suspend fun addAll(entries: List<OtpEntry>)
    suspend fun update(entry: OtpEntry)
    suspend fun delete(id: String)
    suspend fun replaceAll(entries: List<OtpEntry>)
    suspend fun purgeOldTombstones()
}
