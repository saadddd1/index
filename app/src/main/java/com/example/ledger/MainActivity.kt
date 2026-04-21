package com.example.ledger

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import com.example.ledger.ui.MainScreen
import com.example.ledger.data.AppDatabase
import com.example.ledger.ui.IosBg
import com.example.ledger.ui.IosTextPrimary
import com.example.ledger.ui.IosTextSecondary
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 强制开启硬件最高刷新率 (突破默认的 60Hz，开启 90Hz/120Hz/144Hz 等高刷支持)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.let { win ->
                val display = win.windowManager.defaultDisplay
                val modes = display.supportedModes
                // 找出设备支持的最高刷新率的屏幕模式
                val maxRefreshRateMode = modes.maxByOrNull { it.refreshRate }
                if (maxRefreshRateMode != null) {
                    val lp = win.attributes
                    lp.preferredDisplayModeId = maxRefreshRateMode.modeId
                    win.attributes = lp
                }
            }
        }

        val database = AppDatabase.getDatabase(this)
        
        setContent {
            var showSplash by remember { mutableStateOf(true) }

            LaunchedEffect(Unit) { // 组件进入重组树时触发一次
                delay(1800L) // 高颜值的启动页滞空 1.8 秒，从容填平底层冷启动、协程与 Room 跑初始化的大量耗时
                showSplash = false
            }

            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = IosBg // 采用纯白底色，规避系统多余的背景层绘制以提升性能
                ) {
                    if (showSplash) {
                        SplashScreen()
                    } else {
                        MainScreen(database = database)
                    }
                }
            }
        }
    }
}

@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Ledger", 
                fontFamily = FontFamily.SansSerif, 
                fontWeight = FontWeight.Bold, 
                fontSize = 42.sp, 
                color = IosTextPrimary,
                letterSpacing = (-1).sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Hardcore Financial Control", 
                fontFamily = FontFamily.SansSerif, 
                fontSize = 14.sp, 
                color = IosTextSecondary
            )
        }
    }
}
