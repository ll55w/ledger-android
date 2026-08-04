package com.ledger.app

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.content.Intent
import java.util.regex.Pattern

/**
 * 原生通知监听服务：监听支付宝 / 微信 / 银行 的支付类通知，
 * 用正则抓取金额，按商户关键词猜分类，写入 ledger_data.js。
 * 全程本地，不发任何网络请求 —— 流量 / WiFi 都能用，不依赖电脑。
 */
class LedgerNotificationService : NotificationListenerService() {

    // 金额正则：匹配 ¥12.5 / 88 / 1,280.00 等，捕获第一组为纯数字
    private val amountPattern: Pattern = Pattern.compile(
        "(?<!\\d)(?:¥|￥|RMB)?\\s*(\\d{1,3}(?:,\\d{3})*(?:\\.\\d{1,2})|\\d+(?:\\.\\d{1,2})?)"
    )

    // 只处理这些 App 的通知（按需增删）
    private val pkgWhitelist = setOf(
        "com.eg.android.AlipayGphone",       // 支付宝
        "com.tencent.mm",                    // 微信
        "com.nuts.app",                      // 云闪付
        "com.icbc",                          // 工行
        "com.chinamworld.boc",               // 中行
        "com.cmbchina.ccd",                  // 招行
        "com.spdb.mobilebank",               // 浦发
        "cn.com.spdb.mobilebank.per",
        "com.ylz.yilian",                    // 建行
        "com.ccb.finance",                   // 建行生活
        "com.bankcomm.bankmob",              // 交行
        "com.cmbc.card",                     // 民生
        "com.xyzq.welfare"                   // 兴业
    )

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        try {
            sbn ?: return
            val pkg = sbn.packageName
            if (pkg !in pkgWhitelist) return

            val extras = sbn.notification.extras
            val title = extras?.getCharSequence("android.title")?.toString() ?: ""
            val text = extras?.getCharSequence("android.text")?.toString() ?: ""
            val subText = extras?.getCharSequence("android.subText")?.toString() ?: ""
            val ticker = sbn.notification.tickerText?.toString() ?: ""
            val full = "$title $text $subText $ticker"

            if (!isPayment(full)) return

            val m = amountPattern.matcher(full)
            var amt: Double? = null
            while (m.find()) {
                val cand = m.group(1).replace(",", "")
                val v = cand.toDoubleOrNull()
                if (v != null && v > 0) { amt = v; break }
            }
            if (amt == null) return

            val cat = guessCategory(full, pkg)
            val note = guessMerchant(full, pkg)
            val ok = LedgerData.appendRecord(this, amt, cat, "$note｜$full".take(60))

            if (ok) {
                // 通知前台页面刷新
                val i = Intent("com.ledger.app.REFRESH")
                i.setPackage(packageName)
                sendBroadcast(i)
            }
        } catch (e: Exception) {
            // 单条解析出错不应 crash 整个服务
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {}

    private fun isPayment(s: String): Boolean {
        val keys = listOf(
            "支付", "付款", "消费", "支出", "扣款", "成功付款", "支付成功",
            "微信支付", "支付宝", "已付", "到账", "交易", "收款", "Purchase", "Paid"
        )
        return keys.any { s.contains(it) }
    }

    private fun guessCategory(s: String, pkg: String): String {
        return when {
            listOf("美团", "饿了么", "外卖", "餐饮", "饭", "餐", "咖啡", "奶茶").any { s.contains(it) } -> "餐饮"
            listOf("滴滴", "高德", "地铁", "公交", "打车", "铁路", "12306", "加油", "出行", "出租车", "Uber", "共享单车").any { s.contains(it) } -> "交通"
            listOf("淘宝", "京东", "拼多多", "购物", "天猫", "商城", "超市", "便利店").any { s.contains(it) } -> "购物"
            listOf("电影", "视频", "会员", "娱乐", "游戏", "音乐").any { s.contains(it) } -> "娱乐"
            listOf("水电", "物业", "房租", "话费", "宽带", "燃气").any { s.contains(it) } -> "居住"
            else -> "其他"
        }
    }

    private fun guessMerchant(s: String, pkg: String): String {
        return when (pkg) {
            "com.eg.android.AlipayGphone" -> "支付宝"
            "com.tencent.mm" -> "微信支付"
            "com.nuts.app" -> "云闪付"
            else -> pkg.substringAfterLast(".")
        }
    }
}
