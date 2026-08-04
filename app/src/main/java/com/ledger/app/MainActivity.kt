package com.ledger.app

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.provider.Settings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var btnPerm: Button

    private val refreshReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, i: Intent?) {
            webView.reload()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 把 assets 里的网页复制到可写目录（仅首次）
        LedgerData.ensureInit(this)

        webView = findViewById(R.id.webview)
        btnPerm = findViewById(R.id.btnPerm)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowFileAccessFromFileURLs = true
            allowUniversalAccessFromFileURLs = true
        }
        webView.webViewClient = WebViewClient()
        val path = File(LedgerData.ledgerDir(this), "ledger-phone.html").absolutePath
        webView.loadUrl("file://$path")

        btnPerm.setOnClickListener { openListenerSettings() }

        // 注册刷新广播（自动记账后实时更新页面）
        ContextCompat.registerReceiver(
            this,
            refreshReceiver,
            IntentFilter("com.ledger.app.REFRESH"),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        checkAndPromptPermission()
    }

    /** 是否已获得通知读取权限；没有则显示引导按钮并跳转设置。 */
    private fun checkAndPromptPermission() {
        val enabled = isListenerEnabled()
        if (enabled) {
            btnPerm.visibility = Button.GONE
        } else {
            btnPerm.visibility = Button.VISIBLE
            Toast.makeText(this, "请授权「通知读取」以开启自动记账", Toast.LENGTH_LONG).show()
            openListenerSettings()
        }
    }

    private fun isListenerEnabled(): Boolean {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val cmp = "$packageName/com.ledger.app.LedgerNotificationService"
        return nm.enabledNotificationListeners?.any { it == cmp } ?: false
    }

    private fun openListenerSettings() {
        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }

    override fun onResume() {
        super.onResume()
        // 回到页面时刷新一次（捕获授权后/后台写入的数据）
        webView.reload()
        if (isListenerEnabled()) btnPerm.visibility = Button.GONE
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }

    override fun onDestroy() {
        try { unregisterReceiver(refreshReceiver) } catch (e: Exception) {}
        super.onDestroy()
    }
}
