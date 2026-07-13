package com.otpbox.data.repo

import com.otpbox.data.local.OtpDao
import com.otpbox.data.local.toDomain
import com.otpbox.data.local.toEntity
import com.otpbox.domain.model.OtpEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit

class OtpRepositoryImpl(private val dao: OtpDao) : OtpRepository {

    override fun observeEntries(): Flow<List<OtpEntry>> =
        dao.observeActive().map { list -> list.map { it.toDomain() } }

    override suspend fun getActiveEntries(): List<OtpEntry> =
        dao.getActive().map { it.toDomain() }

    override suspend fun getAllIncludingDeleted(): List<OtpEntry> =
        dao.getAllIncludingDeleted().map { it.toDomain() }

    override suspend fun getById(id: String): OtpEntry? =
        dao.getById(id)?.toDomain()

    override suspend fun add(entry: OtpEntry) {
        val order = dao.maxSortOrder() + 1
        dao.upsert(entry.copy(sortOrder = order, updatedAt = System.currentTimeMillis()).toEntity())
    }

    override suspend fun addAll(entries: List<OtpEntry>) {
        var order = dao.maxSortOrder()
        val now = System.currentTimeMillis()
        val prepared = entries.map { entry ->
            order += 1
            entry.copy(sortOrder = order, updatedAt = now).toEntity()
        }
        dao.upsertAll(prepared)
    }

    override suspend fun update(entry: OtpEntry) {
        dao.upsert(entry.copy(updatedAt = System.currentTimeMillis()).toEntity())
    }

    override suspend fun delete(id: String) {
        dao.softDelete(id, System.currentTimeMillis())
    }

    override suspend fun replaceAll(entries: List<OtpEntry>) {
        dao.upsertAll(entries.map { it.toEntity() })
    }

    override suspend fun purgeOldTombstones() {
        val threshold = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30)
        dao.purgeTombstones(threshold)
    }
}
