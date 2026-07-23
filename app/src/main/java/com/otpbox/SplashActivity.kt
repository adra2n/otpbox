package com.otpbox

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.umeng.analytics.MobclickAgent
import com.umeng.commonsdk.UMConfigure
import com.umeng.union.UMSplashAD
import com.umeng.union.UMUnionSdk
import com.umeng.union.api.UMAdConfig
import com.umeng.union.api.UMUnionApi
import com.otpbox.data.settings.PrivacyStore
import kotlinx.coroutines.runBlocking

@SuppressLint("CustomSplashScreen")
class SplashActivity : Activity() {

    private var splashContainer: FrameLayout? = null
    private var fallbackLayout: LinearLayout? = null
    private var skipTextView: TextView? = null
    private var countDownTimer: CountDownTimer? = null
    private var splashAd: UMSplashAD? = null

    companion object {
        private const val TAG = "SplashActivity"
        private const val SKIP_DELAY = 5000L
        private const val COUNT_DOWN_INTERVAL = 1000L
        private const val AD_SLOT_ID = "100012565"
        private const val AD_TIMEOUT = 5000
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        splashContainer = findViewById(R.id.splash_container)
        fallbackLayout = findViewById(R.id.fallback_layout)
        skipTextView = findViewById(R.id.skip_text)

        skipTextView?.setOnClickListener {
            navigateToNext()
        }

        // 先检查隐私协议状态
        val privacyGranted = runBlocking {
            runCatching {
                PrivacyStore(this@SplashActivity).isAgreed()
            }.getOrDefault(false)
        }

        Log.d(TAG, "privacyGranted: $privacyGranted")

        if (privacyGranted) {
            // 已同意隐私协议，加载广告
            loadSplashAd()
        } else {
            // 未同意隐私协议，直接跳转
            Log.d(TAG, "隐私协议未同意，跳转到隐私协议页面")
            navigateToNext()
        }
    }

    private fun loadSplashAd() {
        // 初始化友盟统计 SDK
        UMConfigure.preInit(this, BuildConfig.UMENG_APPKEY, BuildConfig.UMENG_CHANNEL)
        UMConfigure.submitPolicyGrantResult(this, true)
        UMConfigure.init(this, BuildConfig.UMENG_APPKEY, BuildConfig.UMENG_CHANNEL, UMConfigure.DEVICE_TYPE_PHONE, null)
        MobclickAgent.setPageCollectionMode(MobclickAgent.PageMode.AUTO)

        // 初始化广告 SDK
        UMUnionSdk.init(this)

        // 构建广告配置
        val adConfig = UMAdConfig.Builder()
            .setSlotId(AD_SLOT_ID)
            .build()

        // 加载开屏广告
        UMUnionSdk.loadSplashAd(adConfig, object : UMUnionApi.AdLoadListener<UMSplashAD> {
            override fun onSuccess(adType: UMUnionApi.AdType?, ad: UMSplashAD?) {
                Log.d(TAG, "开屏广告加载成功")
                splashAd = ad
                ad?.setAdCloseListener {
                    Log.d(TAG, "广告关闭")
                    navigateToNext()
                }
                // 隐藏兜底布局，显示广告
                fallbackLayout?.visibility = View.GONE
                splashContainer?.let { container ->
                    ad?.show(container)
                }
            }

            override fun onFailure(adType: UMUnionApi.AdType?, msg: String?) {
                Log.e(TAG, "开屏广告加载失败: $msg")
                // 显示兜底布局
                showFallback()
            }
        }, AD_TIMEOUT)

        // 启动倒计时兜底
        startCountDown()
    }

    private fun showFallback() {
        fallbackLayout?.visibility = View.VISIBLE
        splashContainer?.visibility = View.GONE
    }

    private fun startCountDown() {
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(SKIP_DELAY, COUNT_DOWN_INTERVAL) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = millisUntilFinished / 1000
                skipTextView?.text = "跳过 ${secondsLeft}s"
            }

            override fun onFinish() {
                navigateToNext()
            }
        }.start()
        skipTextView?.visibility = View.VISIBLE
    }

    private fun navigateToNext() {
        countDownTimer?.cancel()
        val nextIntent = Intent(this, MainActivity::class.java)
        startActivity(nextIntent)
        finish()
    }

    override fun onDestroy() {
        countDownTimer?.cancel()
        super.onDestroy()
    }
}