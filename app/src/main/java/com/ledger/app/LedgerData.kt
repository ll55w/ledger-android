package com.ledger.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 本地数据读写工具：把 assets 里的 ledger-phone.html / ledger_data.js
 * 复制到应用私有目录（可写），并提供追加一笔记录的接口。
 *
 * ledger_data.js 的格式固定为：  window.LD={"records":[...],"funds":[...]};
 * 网页加载时会自动读取并与自己的 localStorage 合并显示。
 */
object LedgerData {

    private const val MARKER = "window.LD="

    fun ledgerDir(ctx: Context): File {
        val d = File(ctx.filesDir, "ledger")
        if (!d.exists()) d.mkdirs()
        return d
    }

    fun htmlFile(ctx: Context): File = File(ledgerDir(ctx), "ledger-phone.html")
    fun dataFile(ctx: Context): File = File(ledgerDir(ctx), "ledger_data.js")

    /** 首次启动把 assets 里的文件落到可写目录；已在则跳过。 */
    fun ensureInit(ctx: Context) {
        val dir = ledgerDir(ctx)
        val html = htmlFile(ctx)
        val js = dataFile(ctx)
        if (!html.exists()) {
            ctx.assets.open("ledger-phone.html").use { it.copyTo(html.outputStream()) }
        }
        if (!js.exists()) {
            ctx.assets.open("ledger_data.js").use { it.copyTo(js.outputStream()) }
        }
    }

    /** 追加一笔自动抓取到的消费记录，返回是否成功写入。 */
    fun appendRecord(ctx: Context, amount: Double, category: String, note: String): Boolean {
        if (amount <= 0) return false
        val raw = try { dataFile(ctx).readText() } catch (e: Exception) { "" }

        val records = ArrayList<JSONObject>()
        val funds = ArrayList<JSONObject>()

        val s = raw.indexOf(MARKER)
        if (s >= 0) {
            val e = raw.lastIndexOf("}")
            if (e > s) {
                val json = raw.substring(s + MARKER.length, e + 1)
                try {
                    val obj = JSONObject(json)
                    val ra = obj.optJSONArray("records")
                    if (ra != null) for (i in 0 until ra.length()) records.add(ra.getJSONObject(i))
                    val fa = obj.optJSONArray("funds")
                    if (fa != null) for (i in 0 until fa.length()) funds.add(fa.getJSONObject(i))
                } catch (e: Exception) { /* 损坏则忽略旧数据重新建 */ }
            }
        }

        val d = Date()
        val ds = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(d)
        val rec = JSONObject().apply {
            put("id", "t" + d.time)
            put("date", ds)
            put("type", "expense")
            put("amount", amount)
            put("category", category)
            put("note", note)
            put("ts", d.time)
            put("isDemo", false)
        }
        records.add(rec)

        val out = JSONObject().apply {
            put("records", JSONArray(records))
            put("funds", JSONArray(funds))
        }
        try {
            dataFile(ctx).writeText(MARKER + out.toString() + ";")
            return true
        } catch (e: Exception) {
            return false
        }
    }
}
