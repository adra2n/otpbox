package com.otpbox

import android.app.Application
import com.otpbox.data.crypto.CrashLogger
import com.otpbox.data.settings.PrivacyStore
import com.otpbox.util.UmengInit
import dagger.hilt.android.HiltAndroidApp
import net.sqlcipher.database.SQLiteDatabase

@HiltAndroidApp
class OtpBoxApp : Application() {
    override fun onCreate() {
        super.onCreate()
        SQLiteDatabase.loadLibs(this)
        CrashLogger.install(this)
        // 已同意过隐私政策（冷启动跳过门控）时直接初始化友盟
        if (PrivacyStore(this).isAgreedSync()) {
            UmengInit.init(this)
        }
    }
}
