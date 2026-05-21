package com.liuhy.myaccount.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import com.liuhy.myaccount.MainActivity
import com.liuhy.myaccount.R
import java.util.concurrent.atomic.AtomicInteger

/**
 * 监听微信、支付宝等应用的支付通知，自动解析金额并推送一条
 * "快速记账"通知。用户点击通知后跳转到记账页（金额/备注已预填）。
 *
 * 由于 Android 10+ 限制后台启动 Activity，这里采用"高优先级 heads-up
 * 通知 + PendingIntent"的方式，效果接近于"自动弹出记账"。
 *
 * 启用前提：用户必须在系统设置 → 通知 → 通知使用权 中授予本应用权限。
 */
class PaymentNotificationListener : NotificationListenerService() {

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "PaymentNotificationListener 服务启动")
        // 服务启动时一次性建好通知通道，避免每条通知都重建（虽然 createNotificationChannel
        // 是幂等的，但反复调用会每次都走 binder 到 system_server，没必要）
        ensureChannel()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "通知监听器已连接（用户授权生效）")
        ensureChannel()
    }

    /**
     * 创建（或更新）支付自动记账的通知通道。
     *
     * 注意：如果通道已经在设备上创建过，**优先级（IMPORTANCE）一旦由用户调过就锁死**，
     * 系统不会因为代码里改了 IMPORTANCE_HIGH 而把它升回来。如果你之前装过低优先级版本，
     * 需要在系统通知设置里手动把"支付自动记账"通道改为"紧急/高"，或卸载重装。
     */
    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "支付自动记账",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "检测到微信/支付宝支付时弹出快速记账提示"
            enableVibration(true)
            setShowBadge(true)
        }
        nm.createNotificationChannel(channel)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        val pkg = sbn.packageName
        
        // 调试：打印所有通知信息，方便查看实际格式
        Log.d(TAG, "=== 收到通知 ===")
        Log.d(TAG, "pkg=$pkg")
        Log.d(TAG, "是否在目标列表中: ${pkg in TARGET_PACKAGES}")
        
        if (pkg !in TARGET_PACKAGES) {
            Log.d(TAG, "非目标应用，忽略")
            return
        }

        val extras = sbn.notification.extras ?: return
        val title = extras.getCharSequence("android.title")?.toString().orEmpty()
        val text = extras.getCharSequence("android.text")?.toString().orEmpty()
        val bigText = extras.getCharSequence("android.bigText")?.toString().orEmpty()
        val subText = extras.getCharSequence("android.subText")?.toString().orEmpty()
        val ticker = extras.getCharSequence("android.tickerText")?.toString().orEmpty()
        
        // 尝试获取 textLines（多行文本）
        val textLines = extras.getCharSequenceArray("android.textLines")?.joinToString(" | ") ?: ""
        
        val combined = listOf(title, text, bigText, subText, ticker, textLines).joinToString(" | ")

        Log.d(TAG, "title=$title")
        Log.d(TAG, "text=$text")
        Log.d(TAG, "bigText=$bigText")
        Log.d(TAG, "subText=$subText")
        Log.d(TAG, "ticker=$ticker")
        Log.d(TAG, "textLines=$textLines")
        Log.d(TAG, "combined=$combined")
        Log.d(TAG, "=== 通知结束 ===")

        // 过滤提醒类通知（水费出账、账单提醒等），但保留含金额的支付通知
        if (isReminderNotification(title, combined) && parsePayment(pkg, title, combined) == null) {
            Log.d(TAG, "过滤提醒类通知，忽略")
            return
        }

        val parsed = parsePayment(pkg, title, combined)
        if (parsed == null) {
            Log.d(TAG, "解析金额失败，通知内容未匹配到金额正则")
            return
        }
        Log.d(TAG, "解析成功: amount=${parsed.amount}, type=${parsed.type}, note=${parsed.note}")

        Log.d(TAG, "解析到支付记录 amount=${parsed.amount} type=${parsed.type} note=${parsed.note}")

        showQuickRecordNotification(parsed)
    }

    /**
     * 判断是否为提醒类通知（水费出账、账单提醒等非支付通知）
     */
    private fun isReminderNotification(title: String, body: String): Boolean {
        val reminderKeywords = listOf(
            "出账", "账单", "缴费", "扣费", "代扣", "自动扣", 
            "提醒", "待缴费", "未缴费", "逾期", "欠费",
            "水费", "电费", "燃气费", "物业费", "话费",
            "红包", "失效", "过期", "限时", "今晚", "即将到期"
        )
        return reminderKeywords.any { it in title || it in body }
    }

    /**
     * 根据通知文本提取金额、收支类型与备注。匹配不到则返回 null。
     */
    private fun parsePayment(pkg: String, title: String, body: String): ParsedPayment? {
        val amountRegex = Regex("""[¥￥]\s*(\d+(?:\.\d+)?)|(\d+(?:\.\d+)?)\s*元|人民币\s*(\d+(?:\.\d+)?)""")
        val match = amountRegex.find(body) ?: return null
        val amountText = match.groupValues.drop(1).firstOrNull { it.isNotEmpty() } ?: return null
        val amount = amountText.toDoubleOrNull() ?: return null
        if (amount <= 0.0) return null

        // 简单判断收/支：标题或正文里有"收款"/"到账"/"收到"等字眼则视为收入，否则当作支出
        val isIncome = INCOME_KEYWORDS.any { it in title || it in body }
        val type = if (isIncome) "INCOME" else "EXPENSE"

        // 备注：用通知标题 + 截取内容前 30 字，方便用户记忆
        val noteSource = if (title.isNotBlank()) title else pkgDisplayName(pkg)
        val note = (noteSource + " " + body.take(30)).trim().take(60)

        return ParsedPayment(amount = amount, type = type, note = note, sourcePkg = pkg)
    }

    private fun pkgDisplayName(pkg: String): String = when (pkg) {
        PKG_WECHAT -> "微信支付"
        PKG_ALIPAY -> "支付宝"
        else -> pkg
    }

    /**
     * 弹出本应用的高优先级 heads-up 通知，点击后打开 MainActivity 并把预填数据通过 Intent 传过去。
     */
    private fun showQuickRecordNotification(parsed: ParsedPayment) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_PREFILL_AMOUNT, parsed.amount)
            putExtra(EXTRA_PREFILL_TYPE, parsed.type)
            putExtra(EXTRA_PREFILL_NOTE, parsed.note)
        }
        // 用全局自增 ID 作为 requestCode：保证短时间内连续两条通知的 PendingIntent
        // 不会被系统视为"同一个"而互相覆盖 extras。比时间戳截断更可靠。
        val requestId = REQUEST_ID_GENERATOR.incrementAndGet()
        val pi = PendingIntent.getActivity(
            this,
            requestId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val typeLabel = if (parsed.type == "INCOME") "收入" else "支出"
        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("检测到一笔$typeLabel：¥${"%.2f".format(parsed.amount)}")
            .setContentText("点此快速记账（来源：${pkgDisplayName(parsed.sourcePkg)}）")
            .setStyle(NotificationCompat.BigTextStyle().bigText(parsed.note))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            // CATEGORY_MESSAGE：在多数 ROM 上会被认为是即时性强、需要 heads-up 弹出的消息；
            // 之前用的 CATEGORY_REMINDER 在部分系统会被静默放进通知抽屉，看上去就像"延迟"。
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setDefaults(NotificationCompat.DEFAULT_VIBRATE)
            // 让系统按当前真实时间显示通知（heads-up 排序更靠前）
            .setWhen(System.currentTimeMillis())
            .setShowWhen(true)
            // 同 ID 替换时仍要再次发出 heads-up（默认 false 即每次都 alert，这里显式说明）
            .setOnlyAlertOnce(false)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()

        nm.notify(requestId, notif)
    }

    private data class ParsedPayment(
        val amount: Double,
        val type: String,
        val note: String,
        val sourcePkg: String
    )

    companion object {
        private const val TAG = "PaymentNotifListener"
        private const val CHANNEL_ID = "payment_auto_capture"

        const val PKG_WECHAT = "com.tencent.mm"
        const val PKG_ALIPAY = "com.eg.android.AlipayGphone"

        const val EXTRA_PREFILL_AMOUNT = "extra_prefill_amount"
        const val EXTRA_PREFILL_TYPE = "extra_prefill_type"
        const val EXTRA_PREFILL_NOTE = "extra_prefill_note"

        // 主流银行 App 包名（支持储蓄卡/信用卡动账通知）
        private val BANK_PACKAGES = setOf(
            "com.icbc",                    // 工商银行
            "com.chinamworld.main",        // 建设银行
            "com.android.bankabc",         // 农业银行
            "com.bankofchina.bocmbci",     // 中国银行
            "cmb.pb",                      // 招商银行
            "com.bankcomm.Bankcomm",       // 交通银行
            "com.yitong.mbank.psbc",       // 邮储银行
            "com.ecitic.bank.mobile",      // 中信银行
            "com.cebbank.mobile.cemb",     // 光大银行
            "com.pingan.bank",             // 平安银行
            "com.spdb.mobilebank",         // 浦发银行
            "com.cib.cbip",                // 兴业银行
            "com.cmbc.ccbm"                // 民生银行
        )

        private val TARGET_PACKAGES = setOf(PKG_WECHAT, PKG_ALIPAY) + BANK_PACKAGES
        private val INCOME_KEYWORDS = listOf(
            "收款", "到账", "收到", "已入账", "退款",
            "存入", "收入", "来账", "入账", "转入", "工资",
            "返现", "退款成功"
        )

        // 进程级单调自增的 requestCode 生成器，保证 PendingIntent 唯一
        private val REQUEST_ID_GENERATOR = AtomicInteger(1)
    }
}
