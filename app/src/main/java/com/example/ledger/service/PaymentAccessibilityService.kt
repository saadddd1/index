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

class PaymentAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(Dispatchers.IO)
    private var lastRecordedAmount = 0.0
    private var lastRecordedTime = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val rootNode = rootInActiveWindow ?: return
        
        val packageName = event.packageName?.toString() ?: return
        
        when (packageName) {
            "com.tencent.mm" -> parseWeChat(rootNode)
            "com.eg.android.AlipayGphone" -> parseAlipay(rootNode)
        }
    }

    private fun parseWeChat(rootNode: AccessibilityNodeInfo) {
        // Implementation for parsing WeChat payment success, red packet, and transfer screens
        val allTexts = mutableListOf<String>()
        extractTextFromNodes(rootNode, allTexts)
        
        // Very basic initial logic to be fleshed out based on WeChat's actual UI structure.
        val combinedText = allTexts.joinToString(" || ")
        
        if (combinedText.contains("支付成功") || combinedText.contains("红包") || combinedText.contains("转账")) {
             // Logic to find exact amount and merchant from the view hierarchy.
             Log.d("A11y", "Found WeChat Payment Context: $combinedText")
        }
    }

    private fun parseAlipay(rootNode: AccessibilityNodeInfo) {
        val allTexts = mutableListOf<String>()
        extractTextFromNodes(rootNode, allTexts)
        
        val combinedText = allTexts.joinToString(" || ")
        if (combinedText.contains("支付成功")) {
            Log.d("A11y", "Found Alipay Payment Context: $combinedText")
        }
    }

    private fun extractTextFromNodes(node: AccessibilityNodeInfo?, list: MutableList<String>) {
        if (node == null) return
        if (node.text != null) {
            list.add(node.text.toString())
        }
        for (i in 0 until node.childCount) {
            extractTextFromNodes(node.getChild(i), list)
        }
    }

    override fun onInterrupt() {
        // Required method
    }
}
