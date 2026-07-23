package com.otpbox.util

import android.content.Context
import com.umeng.analytics.MobclickAgent
import com.umeng.commonsdk.UMConfigure
import com.umeng.union.UMUnionSdk

object UmengInit {
    fun init(context: Context) {
        val appKey = com.otpbox.BuildConfig.UMENG_APPKEY
        val channel = com.otpbox.BuildConfig.UMENG_CHANNEL
        if (appKey.isBlank()) return
        UMConfigure.preInit(context, appKey, channel)
        // 声明隐私政策已授权，否则友盟 9.x 拒绝上报并告警
        UMConfigure.submitPolicyGrantResult(context, true)
        UMConfigure.init(
            context,
            appKey,
            channel,
            UMConfigure.DEVICE_TYPE_PHONE,
            null
        )
        MobclickAgent.setPageCollectionMode(MobclickAgent.PageMode.AUTO)
        
        // 初始化友盟 Union SDK（广告）
        UMUnionSdk.init(context)
    }
}
