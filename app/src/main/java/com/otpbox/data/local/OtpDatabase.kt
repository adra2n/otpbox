package com.otpbox.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [OtpEntryEntity::class, PasswordEntryEntity::class],
    version = 2,
    exportSchema = false
)
abstract class OtpDatabase : RoomDatabase() {
    abstract fun otpDao(): OtpDao
    abstract fun passwordDao(): PasswordDao

    companion object {
        const val NAME = "otpbox.db"
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `password_entry` (
                        `id` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `username` TEXT NOT NULL,
                        `password` TEXT NOT NULL,
                        `url` TEXT,
                        `note` TEXT,
                        `sortOrder` INTEGER NOT NULL,
                        `deleted` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }
    }
}
