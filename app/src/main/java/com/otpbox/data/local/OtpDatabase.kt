package com.otpbox.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [OtpEntryEntity::class],
    version = 2,
    exportSchema = false
)
abstract class OtpDatabase : RoomDatabase() {
    abstract fun otpDao(): OtpDao

    companion object {
        const val NAME = "otpbox.db"

        /**
         * Reconciles a pre-1.11 on-device database (which carried a stray
         * `counter` column from an abandoned HOTP experiment) to the current
         * TOTP-only schema. Recreates `otp_entries` without the `counter` column
         * (preserving all TOTP data) and ensures `password_entry` exists.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `password_entry` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `username` TEXT NOT NULL, `password` TEXT NOT NULL, `url` TEXT, `note` TEXT, `sortOrder` INTEGER NOT NULL, `deleted` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                db.execSQL(
                    """
                    CREATE TABLE `otp_entries_new` (
                        `id` TEXT NOT NULL,
                        `issuer` TEXT NOT NULL,
                        `account` TEXT NOT NULL,
                        `secret` TEXT NOT NULL,
                        `algorithm` TEXT NOT NULL,
                        `digits` INTEGER NOT NULL,
                        `period` INTEGER NOT NULL,
                        `type` TEXT NOT NULL,
                        `counter` INTEGER NOT NULL,
                        `color` INTEGER,
                        `note` TEXT,
                        `icon` TEXT,
                        `sortOrder` INTEGER NOT NULL,
                        `deleted` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `otp_entries_new` (
                        id, issuer, account, secret, algorithm, digits, period, type, counter,
                        color, note, icon, sortOrder, deleted, updatedAt, createdAt
                    )
                    SELECT
                        id, issuer, account, secret, algorithm, digits, period, type, 0,
                        color, note, icon, sortOrder, deleted, updatedAt, createdAt
                    FROM `otp_entries`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `otp_entries`")
                db.execSQL("ALTER TABLE `otp_entries_new` RENAME TO `otp_entries`")
            }
        }
    }
}
