package com.example.ledger.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.ledger.data.AppDatabase
import com.example.ledger.data.AutoBill
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationMonitorService : NotificationListenerService() {
    
    private val scope = CoroutineScope(Dispatchers.IO)
    // 用于防抖
    private var lastAmount = 0.0
    private var lastTime = 0L

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName
        val notification = sbn.notification
        val extras = notification.extras

        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getString(Notification.EXTRA_TEXT) ?: ""
        val combinedText = "$title $text".replace("\n", " ")

        // 我们关注主流支付软件
        val isPaymentApp = when (packageName) {
            "com.tencent.mm" -> true // 微信
            "com.eg.android.AlipayGphone" -> true // 支付宝
            "com.unionpay" -> true // 云闪付
            else -> packageName.contains("bank", ignoreCase = true) // 银行App
        }

        if (!isPaymentApp) return

        // 关键字过滤，确保这是一笔支出通知
        val successKeywords = listOf("支付款项", "支出", "交易成功", "支付成功", "付款成功", "消费", "转账给", "成功付款", "完成付款", "付款金额")
        if (!successKeywords.any { combinedText.contains(it) }) return

        scope.launch {
            try {
                // 提取金额：兼容不同格式例如 "￥10.00", "消费 10.00", "人民币10.00元" 等
                val amountRegex = Regex("""(￥|¥|人民币金额|支出金额|消费|支出|金额|向.*转账)\s*:?\s*([0-9]+\.[0-9]{2})""")
                val amountMatch = amountRegex.find(combinedText)
                var amount = amountMatch?.groupValues?.get(2)?.toDoubleOrNull()
                
                // Fallback: 直接找 xx.xx元
                if (amount == null) {
                    val fallbackRegex = Regex("""([0-9]+\.[0-9]{2})\s*元""")
                    amount = fallbackRegex.find(combinedText)?.groupValues?.get(1)?.toDoubleOrNull()
                }

                val finalAmount = amount ?: return@launch

                // 防抖逻辑：防止同一笔账单因为状态更新导致多条通知
                val now = sbn.postTime
                if (finalAmount == lastAmount && (now - lastTime < 5000)) {
                    return@launch
                }
                lastAmount = finalAmount
                lastTime = now

                val appSource = when (packageName) {
                    "com.tencent.mm" -> "Wechat (微信)"
                    "com.eg.android.AlipayGphone" -> "Alipay (支付宝)"
                    "com.unionpay" -> "UnionPay (云闪付)"
                    else -> "Bank App (银行)"
                }

                // 启发式提取商户名
                var merchantName = "未命名账单"
                
                if (packageName == "com.tencent.mm") {
                   val wxMatch = Regex("""支付给(.*?)\s*[0-9]""").find(text)
                   if (wxMatch != null) merchantName = wxMatch.groupValues[1].trim()
                } else if (packageName == "com.eg.android.AlipayGphone") {
                   val aliMatch = Regex("""在(.*?)成功支付""").find(text)
                   if (aliMatch != null) {
                       merchantName = aliMatch.groupValues[1].trim()
                   } else {
                       val aliTransferMatch = Regex("""向(.*?)转账""").find(text)
                       if (aliTransferMatch != null) merchantName = aliTransferMatch.groupValues[1].trim()
                   }
                }
                
                // 兜底逻辑：如果标题有内容且不是纯通用词，就用标题
                if (merchantName == "未命名账单" && title.isNotBlank() && title != "微信支付" && title != "支付结果通知") {
                   merchantName = title.trim()
                }
                // 兜底逻辑2：从正文中剥离掉冗余词
                if (merchantName == "未命名账单" && text.isNotBlank()) {
                   merchantName = text.replace(Regex("[0-9]|\\.|元|￥|¥|支付成功|成功支付|支出|消费|转账给|付款给|付款"), "").trim()
                   if (merchantName.isBlank()) merchantName = "未命名账单"
                }

                val db = AppDatabase.getDatabase(applicationContext)
                db.autoBillDao().insertAutoBill(
                    AutoBill(
                        appSource = appSource,
                        merchantName = merchantName,
                        amount = finalAmount,
                        paymentMethod = "通知读取",
                        fullPayeeName = merchantName,
                        timestampMillis = now
                    )
                )

                Log.d("NotificationMonitor", "Saved Bill via Notification: $appSource - $finalAmount")

            } catch (e: Exception) {
                Log.e("NotificationMonitor", "Error parsing notification", e)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
    }
}
