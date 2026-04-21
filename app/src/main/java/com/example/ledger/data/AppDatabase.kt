package com.example.ledger.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [Item::class, AutoBill::class], version = 5, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao
    abstract fun autoBillDao(): AutoBillDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ledger_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance

                // 首次创建时自动注入极其丰富逼真的测试数据
                CoroutineScope(Dispatchers.IO).launch {
                    if (instance.itemDao().getCount() == 0) {
                        val now = System.currentTimeMillis()
                        val dayMs = 24L * 60 * 60 * 1000L
                        val monthMs = 30L * dayMs

                        // 注入资产预置数据 (包括在役与已结清脱手)
                        instance.itemDao().insertItem(Item(name = "MacBook Pro M2 Max", price = 18999.0, purchaseDateMillis = now - 320 * dayMs, residualValue = 9500.0, isSold = false))
                        instance.itemDao().insertItem(Item(name = "Sony A7M4 相机单机身", price = 16500.0, purchaseDateMillis = now - 180 * dayMs, residualValue = 13000.0, isSold = false))
                        instance.itemDao().insertItem(Item(name = "戴森 V12 吸尘器", price = 3599.0, purchaseDateMillis = now - 450 * dayMs, residualValue = 800.0, isSold = false))
                        instance.itemDao().insertItem(Item(name = "哈曼卡顿 琉璃4 音箱", price = 2299.0, purchaseDateMillis = now - 60 * dayMs, residualValue = 1500.0, isSold = false))
                        
                        // 已结清出二手的资产
                        instance.itemDao().insertItem(Item(name = "AirPods Pro 1代", price = 1899.0, purchaseDateMillis = now - 900 * dayMs, residualValue = 450.0, isSold = true, soldDateMillis = now - 100 * dayMs))
                        instance.itemDao().insertItem(Item(name = "Switch OLED 塞尔达限定", price = 2599.0, purchaseDateMillis = now - 500 * dayMs, residualValue = 1450.0, isSold = true, soldDateMillis = now - 50 * dayMs))
                        instance.itemDao().insertItem(Item(name = "iPhone 13 Pro 256G", price = 8799.0, purchaseDateMillis = now - 1000 * dayMs, residualValue = 3200.0, isSold = true, soldDateMillis = now - 200 * dayMs))

                        // 注入自动记账抓取流水 (包含未处理和已处理历史归档)
                        instance.autoBillDao().insertAutoBill(AutoBill(appSource = "Alipay (支付宝)", merchantName = "瑞幸咖啡-生椰拿铁", amount = 14.9, timestampMillis = now - 2 * 60 * 60 * 1000L, isProcessed = false)) // 2小时前
                        instance.autoBillDao().insertAutoBill(AutoBill(appSource = "Wechat (微信)", merchantName = "世纪联华超市(中心店)", amount = 125.4, timestampMillis = now - 1 * dayMs, isProcessed = false)) // 昨天
                        instance.autoBillDao().insertAutoBill(AutoBill(appSource = "Alipay (支付宝)", merchantName = "Steam 游戏内购", amount = 298.0, timestampMillis = now - 3 * dayMs, isProcessed = false))
                        instance.autoBillDao().insertAutoBill(AutoBill(appSource = "Wechat (微信)", merchantName = "滴滴出行", amount = 35.5, timestampMillis = now - 5 * dayMs, isProcessed = false))
                        
                        // 以下是已处理历史数据（用于支撑月度账单卡片）
                        instance.autoBillDao().insertAutoBill(AutoBill(appSource = "Wechat (微信)", merchantName = "优衣库实体店", amount = 599.0, timestampMillis = now - 20 * dayMs, isProcessed = true))
                        instance.autoBillDao().insertAutoBill(AutoBill(appSource = "Alipay (支付宝)", merchantName = "淘宝买菜/买水果", amount = 85.8, timestampMillis = now - 1 * monthMs, isProcessed = true)) // 1个月前
                        instance.autoBillDao().insertAutoBill(AutoBill(appSource = "Alipay (支付宝)", merchantName = "高铁12306 车票", amount = 432.0, timestampMillis = now - 1 * monthMs - 15 * dayMs, isProcessed = true))
                        instance.autoBillDao().insertAutoBill(AutoBill(appSource = "Wechat (微信)", merchantName = "山姆会员商店", amount = 1288.0, timestampMillis = now - 2 * monthMs, isProcessed = true)) // 2个月前
                        instance.autoBillDao().insertAutoBill(AutoBill(appSource = "Alipay (支付宝)", merchantName = "天猫国际-海蓝之谜", amount = 2650.0, timestampMillis = now - 3 * monthMs, isProcessed = true)) // 3个月前
                        instance.autoBillDao().insertAutoBill(AutoBill(appSource = "Wechat (微信)", merchantName = "医院门诊挂号", amount = 50.0, timestampMillis = now - 4 * monthMs, isProcessed = true)) // 4个月前
                    }
                }

                return instance
            }
        }
    }
}
