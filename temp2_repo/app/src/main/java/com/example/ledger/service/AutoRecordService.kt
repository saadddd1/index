package com.example.ledger.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.ledger.data.AppDatabase
import com.example.ledger.data.AutoBill
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AutoRecordService : AccessibilityService() {
    private var lastRecordTime = 0L
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        
        // 性能优化：只监听窗口变化或内容变动
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED && 
            event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return
        }

        val rootNode = rootInActiveWindow ?: return
        
        // 放入子线程解析节点树，避免阻塞系统主线程引发卡顿
        scope.launch {
            try {
                val texts = mutableListOf<String>()
                extractText(rootNode, texts)
                val joinedText = texts.joinToString(" ")
                
                // 第一层过滤：关键词判断 (大幅提高运行效率，加入对转账和主流电商平台的支持)
                val successKeywords = listOf(
                    "支付成功", "已付款", "付款成功", "转账给", "转账成功", 
                    "交易成功", "交易单号", "订单完成", "买家已付款", "支付凭证",
                    "发红包", "发了一个红包"
                )
                if (!successKeywords.any { joinedText.contains(it) }) {
                    return@launch
                }

                // 提取金额 (正则匹配 ￥ 或 ¥ 或 - 后的数字)
                val amountRegex = Regex("""(￥|¥|-)\s*([0-9]+\.[0-9]{2})""")
                val amountMatch = amountRegex.find(joinedText)
                val amount = amountMatch?.groupValues?.get(2)?.toDoubleOrNull() ?: return@launch
                
                // 防抖机制：避免在同一页面的几秒内重复采集同一笔账单
                val now = System.currentTimeMillis()
                if (now - lastRecordTime < 10000) return@launch
                lastRecordTime = now

                val pkgName = event.packageName?.toString() ?: ""
                val appSource = when {
                    pkgName.contains("alipay", ignoreCase = true) -> "Alipay (支付宝)"
                    pkgName.contains("tencent.mm", ignoreCase = true) -> "Wechat (微信)"
                    pkgName.contains("aweme", ignoreCase = true) -> "Douyin (抖音)"
                    pkgName.contains("taobao", ignoreCase = true) -> "Taobao (淘宝)"
                    pkgName.contains("pinduoduo", ignoreCase = true) -> "Pinduoduo (拼多多)"
                    pkgName.contains("meituan", ignoreCase = true) -> "Meituan (美团)"
                    else -> "Unknown App"
                }
                
                // 智能化提取收款方：搜寻常见支付界面特征词汇
                var merchantName = "未命名账单"
                val merchantKeywords = listOf("收款方", "商户", "商户名称", "付款给", "商户全称", "收款人", "商品", "店铺")
                for (i in texts.indices) {
                    val text = texts[i]
                    if (merchantKeywords.any { text.contains(it) }) {
                        // 提取关键词后面跟的具体商户名，或剥离标签后缀
                        merchantName = texts.getOrNull(i + 1)?.takeIf { it.isNotBlank() } 
                            ?: text.replace(Regex("收款方|商户名称|商户|付款给|商户全称|收款人|：|:"), "").trim().takeIf { it.isNotBlank() } 
                            ?: merchantName
                        break
                    }
                }
                
                // 启发式猜测：如果实在没找到商户标签，拿金额前面大概率是标的物的词作为猜测
                if (merchantName.contains("账单")) {
                    val amountIndex = texts.indexOfFirst { it.contains(amountMatch?.groupValues?.get(2) ?: "") }
                    if (amountIndex > 0) {
                        val guess = texts[amountIndex - 1].trim()
                        if (guess.length > 1 && !guess.contains("¥") && !guess.contains("支付") && !guess.contains("成功")) {
                            merchantName = guess
                        }
                    }
                }

                var paymentMethod: String? = null
                val paymentKeywords = listOf("付款方式", "支付方式")
                for (i in texts.indices) {
                    val text = texts[i]
                    if (paymentKeywords.any { text.contains(it) }) {
                        paymentMethod = texts.getOrNull(i + 1)?.takeIf { it.isNotBlank() }
                            ?: text.replace(Regex("付款方式|支付方式|：|:"), "").trim().takeIf { it.isNotBlank() }
                        break
                    }
                }

                var fullPayeeName: String? = null
                val fullPayeeKeywords = listOf("收款方全称", "商户全称")
                for (i in texts.indices) {
                    val text = texts[i]
                    if (fullPayeeKeywords.any { text.contains(it) }) {
                        fullPayeeName = texts.getOrNull(i + 1)?.takeIf { it.isNotBlank() }
                            ?: text.replace(Regex("收款方全称|商户全称|：|:"), "").trim().takeIf { it.isNotBlank() }
                        break
                    }
                }

                val db = AppDatabase.getDatabase(applicationContext)
                db.autoBillDao().insertAutoBill(
                    AutoBill(
                        appSource = appSource,
                        merchantName = merchantName,
                        amount = amount,
                        paymentMethod = paymentMethod,
                        fullPayeeName = fullPayeeName,
                        timestampMillis = now
                    )
                )
                
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    android.widget.Toast.makeText(applicationContext, "账本：已捕获 ${appSource} 消费 ¥$amount", android.widget.Toast.LENGTH_SHORT).show()
                }
                
                try {
                    val notifManager = getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        val channel = android.app.NotificationChannel(
                            "ledger_auto_record",
                            "自动记账通知",    
                            android.app.NotificationManager.IMPORTANCE_HIGH
                        ).apply {
                            description = "后台自动捕获流水时的弹窗通知"
                        }
                        notifManager.createNotificationChannel(channel)
                    }
                    val notification = androidx.core.app.NotificationCompat.Builder(applicationContext, "ledger_auto_record")
                        .setSmallIcon(android.R.drawable.ic_menu_agenda)
                        .setContentTitle("账本自动记账成功")
                        .setContentText("已捕获 ${appSource} 流水 ¥$amount")
                        .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true)
                        .build()
                    notifManager.notify(System.currentTimeMillis().toInt(), notification)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                Log.d("AutoRecord", "Saved Bill: $appSource - $amount")
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                // 必须释放节点以防止内存泄漏
                rootNode.recycle()
            }
        }
    }

    private fun extractText(node: AccessibilityNodeInfo?, texts: MutableList<String>) {
        if (node == null) return
        node.text?.let { texts.add(it.toString()) }
        node.contentDescription?.let { texts.add(it.toString()) }
        for (i in 0 until node.childCount) {
            extractText(node.getChild(i), texts)
        }
    }

    override fun onInterrupt() {}
}
