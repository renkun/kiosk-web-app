package com.grusio.kiosk

import android.content.Context
import android.app.admin.DevicePolicyManager
import android.content.SharedPreferences
import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
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
import com.grusio.adminlocktask.R


class KioskActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var reloadOnConnected: ReloadOnConnected
    private lateinit var adminComponentName: ComponentName
    private lateinit var policyManager: DevicePolicyManager

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var startUrl: String
    private lateinit var serverUrl: String

    private var clickCounter = 0
    private val MAX_CLICK_COUNT = 10
    private val resetDelayMs: Long = 3000 // 3秒
    private val resetClickCountRunnable = Runnable {
        clickCounter = 0
    }

    private val resetClickCountHandler = android.os.Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        showInFullScreen(findViewById(R.id.root))
        initVars()

        sharedPreferences = getSharedPreferences("config", Context.MODE_PRIVATE)
        startUrl = sharedPreferences.getString("startUrl", "") ?: ""
        serverUrl = sharedPreferences.getString("serverUrl", "") ?: ""

        setupWebView()
        listenToConnectionChange()
    }

    override fun onResume() {
        super.onResume()
        setupKiosk()
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
        startLockTask()
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

    private fun showSettingsActivity() {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }

}