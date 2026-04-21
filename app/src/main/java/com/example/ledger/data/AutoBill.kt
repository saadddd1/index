package com.example.ledger.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "auto_bills")
data class AutoBill(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val appSource: String, // "WeChat" 或 "Alipay"
    val merchantName: String,
    val amount: Double,
    val paymentMethod: String? = null,
    val fullPayeeName: String? = null,
    val timestampMillis: Long,
    val isProcessed: Boolean = false // 若已添加为账本物品或被忽略，则标记为true
)
