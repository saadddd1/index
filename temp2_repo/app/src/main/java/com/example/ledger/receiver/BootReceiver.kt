package com.example.ledger.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || 
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            Log.d("LedgerBootReceiver", "Device booted. Ledger background process woken up.")
            // 收到开机广播后，不仅唤醒了App进程，由于系统自带的无障碍服务管理机制，
            // 只要我们进程存活，之前授予过权限的 AutoRecordService 将会更容易被系统恢复绑定和常驻。
        }
    }
}
