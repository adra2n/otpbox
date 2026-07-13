package com.otpbox.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface OtpDao {

    @Query("SELECT * FROM otp_entries WHERE deleted = 0 ORDER BY sortOrder ASC, issuer COLLATE NOCASE ASC")
    fun observeActive(): Flow<List<OtpEntryEntity>>

    @Query("SELECT * FROM otp_entries WHERE deleted = 0 ORDER BY sortOrder ASC, issuer COLLATE NOCASE ASC")
    suspend fun getActive(): List<OtpEntryEntity>

    @Query("SELECT * FROM otp_entries")
    suspend fun getAllIncludingDeleted(): List<OtpEntryEntity>

    @Query("SELECT * FROM otp_entries WHERE id = :id")
    suspend fun getById(id: String): OtpEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: OtpEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<OtpEntryEntity>)

    @Query("UPDATE otp_entries SET deleted = 1, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)

    @Query("SELECT COALESCE(MAX(sortOrder), 0) FROM otp_entries")
    suspend fun maxSortOrder(): Int

    @Query("DELETE FROM otp_entries WHERE deleted = 1 AND updatedAt < :threshold")
    suspend fun purgeTombstones(threshold: Long)
}
