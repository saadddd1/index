package com.example.ledger.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "items")
data class Item(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val price: Double,
    val purchaseDateMillis: Long,
    val residualValue: Double = 0.0, // 预期或真实的残值
    val isSold: Boolean = false,     // 是否已售出/闲置处理
    val soldDateMillis: Long? = null // 处理/售出日期
)
