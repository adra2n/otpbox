package com.otpbox.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [OtpEntryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class OtpDatabase : RoomDatabase() {
    abstract fun otpDao(): OtpDao

    companion object {
        const val NAME = "otpbox.db"
    }
}
