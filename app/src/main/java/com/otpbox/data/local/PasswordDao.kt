package com.otpbox.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PasswordDao {

    @Query("SELECT * FROM password_entry WHERE deleted = 0 ORDER BY sortOrder ASC, title COLLATE NOCASE ASC")
    fun observeActive(): Flow<List<PasswordEntryEntity>>

    @Query("SELECT * FROM password_entry WHERE deleted = 0 ORDER BY sortOrder ASC, title COLLATE NOCASE ASC")
    suspend fun getActive(): List<PasswordEntryEntity>

    @Query("SELECT * FROM password_entry")
    suspend fun getAllIncludingDeleted(): List<PasswordEntryEntity>

    @Query("SELECT * FROM password_entry WHERE id = :id")
    suspend fun getById(id: String): PasswordEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PasswordEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<PasswordEntryEntity>)

    @Query("UPDATE password_entry SET deleted = 1, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)

    @Query("SELECT COALESCE(MAX(sortOrder), 0) FROM password_entry")
    suspend fun maxSortOrder(): Int

    @Query("DELETE FROM password_entry WHERE deleted = 1 AND updatedAt < :threshold")
    suspend fun purgeTombstones(threshold: Long)
}
