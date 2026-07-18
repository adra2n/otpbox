package com.otpbox

import android.app.Application
import com.otpbox.data.crypto.CrashLogger
import com.otpbox.util.initUmengIfAllowed
import dagger.hilt.android.HiltAndroidApp
import net.sqlcipher.database.SQLiteDatabase

@HiltAndroidApp
class OtpBoxApp : Application() {
    override fun onCreate() {
        super.onCreate()
        SQLiteDatabase.loadLibs(this)
        CrashLogger.install(this)
        // 若用户此前已同意隐私政策，冷启动时即初始化友盟统计
        initUmengIfAllowed(this)
    }
}
