package com.otpbox.data.repo

import com.otpbox.data.local.PasswordDao
import com.otpbox.data.local.toDomain
import com.otpbox.data.local.toEntity
import com.otpbox.domain.model.PasswordEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit

class PasswordRepositoryImpl(private val dao: PasswordDao) : PasswordRepository {

    override fun observeEntries(): Flow<List<PasswordEntry>> =
        dao.observeActive().map { list -> list.map { it.toDomain() } }

    override suspend fun getActiveEntries(): List<PasswordEntry> =
        dao.getActive().map { it.toDomain() }

    override suspend fun getAllIncludingDeleted(): List<PasswordEntry> =
        dao.getAllIncludingDeleted().map { it.toDomain() }

    override suspend fun getById(id: String): PasswordEntry? =
        dao.getById(id)?.toDomain()

    override suspend fun add(entry: PasswordEntry) {
        val order = dao.maxSortOrder() + 1
        dao.upsert(entry.copy(sortOrder = order, updatedAt = System.currentTimeMillis()).toEntity())
    }

    override suspend fun addAll(entries: List<PasswordEntry>) {
        var order = dao.maxSortOrder()
        val now = System.currentTimeMillis()
        val prepared = entries.map { entry ->
            order += 1
            entry.copy(sortOrder = order, updatedAt = now).toEntity()
        }
        dao.upsertAll(prepared)
    }

    override suspend fun update(entry: PasswordEntry) {
        dao.upsert(entry.copy(updatedAt = System.currentTimeMillis()).toEntity())
    }

    override suspend fun delete(id: String) {
        dao.softDelete(id, System.currentTimeMillis())
    }

    override suspend fun replaceAll(entries: List<PasswordEntry>) {
        dao.upsertAll(entries.map { it.toEntity() })
    }

    override suspend fun purgeOldTombstones() {
        val threshold = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30)
        dao.purgeTombstones(threshold)
    }
}
