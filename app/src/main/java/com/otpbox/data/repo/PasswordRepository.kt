package com.otpbox.data.repo

import com.otpbox.domain.model.PasswordEntry
import kotlinx.coroutines.flow.Flow

interface PasswordRepository {
    fun observeEntries(): Flow<List<PasswordEntry>>
    suspend fun getActiveEntries(): List<PasswordEntry>
    suspend fun getAllIncludingDeleted(): List<PasswordEntry>
    suspend fun getById(id: String): PasswordEntry?
    suspend fun add(entry: PasswordEntry)
    suspend fun addAll(entries: List<PasswordEntry>)
    suspend fun update(entry: PasswordEntry)
    suspend fun delete(id: String)
    suspend fun replaceAll(entries: List<PasswordEntry>)
    suspend fun purgeOldTombstones()
}
