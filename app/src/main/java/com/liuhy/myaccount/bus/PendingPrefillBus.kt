package com.liuhy.myaccount.bus

import com.liuhy.myaccount.core.data.model.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 一次性事件总线：用于把"系统通知监听服务"解析出来的预填数据
 * 传递给 Compose UI 层。
 *
 * - 通知监听服务通过 PendingIntent 启动 MainActivity；
 * - MainActivity 从 Intent 中拿到金额/类型/备注后调用 [publish]；
 * - Compose 的 MainApp 通过 [state] 监听并自动跳转记账页；
 * - TransactionScreen 使用后调用 [consume] 清空，避免重复预填。
 */
data class PendingPrefill(
    val amount: Double,
    val type: TransactionType,
    val note: String
)

object PendingPrefillBus {
    private val _state = MutableStateFlow<PendingPrefill?>(null)
    val state: StateFlow<PendingPrefill?> = _state

    fun publish(prefill: PendingPrefill) {
        _state.value = prefill
    }

    /**
     * 取出当前待消费的预填数据，并把总线状态清零。
     * 同一笔通知只会被消费一次，避免离开记账页再回来又被自动填一遍。
     */
    fun consume(): PendingPrefill? {
        val v = _state.value
        _state.value = null
        return v
    }
}
