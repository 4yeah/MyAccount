package com.liuhy.myaccount.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.liuhy.myaccount.bus.PendingPrefill
import com.liuhy.myaccount.bus.PendingPrefillBus
import com.liuhy.myaccount.core.data.model.TransactionType
import java.util.regex.Pattern

/**
 * 无障碍服务：用于读取微信/支付宝的通知内容。
 * 相比 NotificationListenerService，AccessibilityService 在小米系统上更稳定。
 */
class PaymentAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "PaymentAccessibilityService 服务启动")
        
        // 不再启动前台服务，避免被小米系统杀死
        // 小米系统会自动保活已启用的无障碍服务
        
        // 配置服务监听通知事件
        val info = serviceInfo
        info.eventTypes = AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
        info.notificationTimeout = 100
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        
        // 只处理通知事件
        if (event.eventType != AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) return
        
        val packageName = event.packageName?.toString().orEmpty()
        
        // 只关心微信和支付宝
        if (packageName !in TARGET_PACKAGES) return
        
        // 获取通知文本
        val textList = event.text
        if (textList.isNullOrEmpty()) return
        
        val combinedText = textList.joinToString(" ")
        Log.d(TAG, "收到通知 pkg=$packageName text=$combinedText")
        
        // 解析支付信息
        val parsed = parsePayment(packageName, combinedText) ?: return
        
        Log.d(TAG, "解析到支付记录 amount=${parsed.amount} type=${parsed.type} note=${parsed.note}")
        
        // 发送预填数据到总线
        PendingPrefillBus.publish(
            PendingPrefill(
                amount = parsed.amount,
                type = parsed.type,
                note = parsed.note
            )
        )
    }

    override fun onInterrupt() {
        // 不需要处理
    }

    private fun parsePayment(pkg: String, text: String): ParsedPayment? {
        // 匹配金额：¥100、￥100、100元、100.00
        val amountPattern = Pattern.compile("[¥￥]\\s*(\\d+(?:\\.\\d+)?)|(\\d+(?:\\.\\d+)?)\\s*元")
        val matcher = amountPattern.matcher(text)
        
        if (!matcher.find()) return null
        
        val amountText = matcher.group(1) ?: matcher.group(2) ?: return null
        val amount = amountText.toDoubleOrNull() ?: return null
        if (amount <= 0.0) return null
        
        // 判断收支类型
        val isIncome = INCOME_KEYWORDS.any { it in text }
        val type = if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE
        
        // 备注
        val noteSource = pkgDisplayName(pkg)
        val note = (noteSource + " " + text.take(30)).trim().take(60)
        
        return ParsedPayment(amount = amount, type = type, note = note)
    }

    private fun pkgDisplayName(pkg: String): String = when (pkg) {
        PKG_WECHAT -> "微信支付"
        PKG_ALIPAY -> "支付宝"
        else -> pkg
    }

    private data class ParsedPayment(
        val amount: Double,
        val type: TransactionType,
        val note: String
    )

    companion object {
        private const val TAG = "PaymentAccessibility"
        const val PKG_WECHAT = "com.tencent.mm"
        const val PKG_ALIPAY = "com.eg.android.AlipayGphone"
        private val TARGET_PACKAGES = setOf(PKG_WECHAT, PKG_ALIPAY)
        private val INCOME_KEYWORDS = listOf("收款", "到账", "收到", "已入账", "退款")
    }
}
