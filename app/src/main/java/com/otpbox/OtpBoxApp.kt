package com.otpbox

import android.app.Application
import com.otpbox.data.crypto.CrashLogger
import dagger.hilt.android.HiltAndroidApp
import net.sqlcipher.database.SQLiteDatabase

@HiltAndroidApp
class OtpBoxApp : Application() {
    override fun onCreate() {
        super.onCreate()
        SQLiteDatabase.loadLibs(this)
        CrashLogger.install(this)
    }
}
