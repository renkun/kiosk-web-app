package com.grusio.kiosk

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
import com.grusio.adminlocktask.BuildConfig
import com.grusio.adminlocktask.R
import com.microsoft.appcenter.AppCenter
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.crashes.Crashes
import com.microsoft.appcenter.distribute.Distribute
import com.microsoft.appcenter.distribute.UpdateTrack


class KioskActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var reloadOnConnected: ReloadOnConnected
    private lateinit var adminComponentName: ComponentName
    private lateinit var policyManager: DevicePolicyManager

    private val REQUEST_ENABLE_DEVICE_ADMIN = 10032

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var startUrl: String
    private lateinit var serverUrl: String

    private var clickCounter = 0
    private val MAX_CLICK_COUNT = 10
    private val resetDelayMs: Long = 2000 // 2秒
    private val resetClickCountRunnable = Runnable {
        clickCounter = 0
    }

    private val resetClickCountHandler = android.os.Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        showInFullScreen(findViewById(R.id.root))
        initVars()

        sharedPreferences = getSharedPreferences("config", MODE_PRIVATE)
        startUrl = sharedPreferences.getString("startUrl", "") ?: ""
        serverUrl = sharedPreferences.getString("serverUrl", "") ?: ""

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        setupWebView()
        listenToConnectionChange()

        initAppCenter()

        setupKiosk()
    }

    override fun onResume() {
        super.onResume()
        startLockTask()
    }

    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack())
            webView.goBack()
        else
            super.onBackPressed()
    }

    override fun onDestroy() {
        if (::reloadOnConnected.isInitialized)
            reloadOnConnected.onActivityDestroy()

        resetClickCountHandler.removeCallbacks(resetClickCountRunnable)
        super.onDestroy()
    }

    private fun showInFullScreen(rootView: View) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, rootView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            window.attributes.layoutInDisplayCutoutMode = LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
    }

    private fun initVars() {
        webView = findViewById(R.id.webView)
        reloadOnConnected = ReloadOnConnected(webView)
        adminComponentName = AdminReceiver.getComponentName(this)
        policyManager = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
    }

    private fun initAppCenter() {

        val appSecret: String? = BuildConfig.APPCENTER_SECRET as? String
        if (appSecret != null) {
            AppCenter.start(
                application, appSecret,
                Analytics::class.java, Crashes::class.java, Distribute::class.java
            )

            // 配置自动更新的设置
            Distribute.setEnabledForDebuggableBuild(true) // 允许在调试版本中使用自动更新（可选）
//            Distribute.setInstallUrl("<Your-Install-URL>") // 设置自定义的安装 URL（可选）
            Distribute.setUpdateTrack(UpdateTrack.PUBLIC) // 设置更新跟踪策略，可以根据需要修改
            Distribute.setEnabled(true) // 启用自动更新
        } else {
            println("AppCenter Secret not found.")
        }
    }
    private fun setupWebView() {
        webView.webViewClient = WebViewClient()
        with(webView.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            useWideViewPort = true
            userAgentString = "Android"
            loadWithOverviewMode = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
        }
        webView.scrollBarStyle = WebView.SCROLLBARS_OUTSIDE_OVERLAY
        webView.isScrollbarFadingEnabled = false
        webView.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                if (event?.action == MotionEvent.ACTION_DOWN) {
                    clickCounter++
                    resetClickCountHandler.removeCallbacks(resetClickCountRunnable)
                    resetClickCountHandler.postDelayed(resetClickCountRunnable, resetDelayMs)
                    if (clickCounter >= MAX_CLICK_COUNT) {
                        showSettingsActivity()
                        resetClickCountHandler.removeCallbacks(resetClickCountRunnable)
                        clickCounter = 0
                    }
                }
                return false
            }
        })
        webView.loadUrl(startUrl)
    }

    private fun listenToConnectionChange() = reloadOnConnected.onActivityCreate(this)

    private fun setupKiosk() {
        if (policyManager.isDeviceOwnerApp(packageName)) {
            setupAutoStart()
            stayAwake()
            policyManager.setLockTaskPackages(adminComponentName, arrayOf(packageName))
        }
        else {
            requestDeviceAdmin()
        }
    }

    private fun setupAutoStart() {
        val intentFilter = IntentFilter(Intent.ACTION_MAIN)
        intentFilter.addCategory(Intent.CATEGORY_HOME)
        intentFilter.addCategory(Intent.CATEGORY_DEFAULT)
        policyManager.addPersistentPreferredActivity(
            adminComponentName,
            intentFilter, ComponentName(packageName, KioskActivity::class.java.name)
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            policyManager.setKeyguardDisabled(adminComponentName, true)
    }

    private fun stayAwake() {
        policyManager.setGlobalSetting(
            adminComponentName,
            Settings.Global.STAY_ON_WHILE_PLUGGED_IN,
            (BatteryManager.BATTERY_PLUGGED_AC
                    or BatteryManager.BATTERY_PLUGGED_USB
                    or BatteryManager.BATTERY_PLUGGED_WIRELESS).toString()
        )
    }

    private fun requestDeviceAdmin(){
        /**
         * @todo 需要深度获测试取权限后的操作。目前暂时使用指令来设置
         * 	adb  shell dpm set-device-owner com.grusio.kiosk/com.grusio.kiosk.AdminReceiver
         * 	删除的指令是：
         * 	adb shell dpm remove-active-admin com.grusio.kiosk/com.grusio.kiosk.AdminReceiver
         */
//        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
//        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponentName)
//        intent.putExtra(
//            DevicePolicyManager.EXTRA_ADD_EXPLANATION,
//            "Enable device administration"
//        )
//        startActivityForResult(intent, REQUEST_ENABLE_DEVICE_ADMIN)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ENABLE_DEVICE_ADMIN) {
            if (resultCode == RESULT_OK) {
                // 设备管理器已启用
            } else {
                // 用户取消或拒绝启用设备管理器
            }
        }
    }

    private fun showSettingsActivity() {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }

}