package com.ledger.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * 开机后确保数据目录初始化。
 * 注意：NotificationListenerService 在用户已授权的情况下，系统会自动随开机拉起，
 * 这里不需要手动 startService。
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED && context != null) {
            LedgerData.ensureInit(context)
        }
    }
}
