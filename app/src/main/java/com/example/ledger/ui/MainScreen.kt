@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
package com.example.ledger.ui

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ledger.data.AppDatabase
import com.example.ledger.data.AutoBill
import com.example.ledger.data.Item
import com.example.ledger.viewmodel.ItemViewModel
import com.example.ledger.viewmodel.ItemViewModelFactory
import kotlinx.coroutines.launch
import androidx.compose.animation.animateColorAsState
import androidx.compose.ui.draw.scale
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.math.max

val IosBlue = Color(0xFF007AFF)
val IosRed = Color(0xFFFF3B30)
val IosScreenBg = Color(0xFFF2F2F7)
val IosBg = Color(0xFFFFFFFF)
val IosTextPrimary = Color(0xFF000000)
val IosTextSecondary = Color(0xFF8E8E93)
val IosDivider = Color(0xFFE5E5EA)
val IosCardBg = Color(0xFFFFFFFF)

fun isNotificationListenerEnabled(context: Context): Boolean {
    val packageName = context.packageName
    val enabledListeners = Settings.Secure.getString(
        context.contentResolver,
        "enabled_notification_listeners"
    ) ?: return false
    return enabledListeners.contains(packageName)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainScreen(database: AppDatabase) {
    val viewModel: ItemViewModel = viewModel(factory = ItemViewModelFactory(database))
    val items by viewModel.items.collectAsState()
    val pendingBills by viewModel.pendingBills.collectAsState()
    val allBills by viewModel.allBills.collectAsState()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var itemToSell by remember { mutableStateOf<Item?>(null) }
    var itemToEdit by remember { mutableStateOf<Item?>(null) }
    var billToConvert by remember { mutableStateOf<AutoBill?>(null) }
    
    val pagerState = rememberPagerState(pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // 请求通知权限，用于在 Android 13 及以上设备上显示通知
    val notificationPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            android.widget.Toast.makeText(context, "请在设置中开启通知权限以接收流水弹窗", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val permissionCheck = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                "android.permission.POST_NOTIFICATIONS"
            )
            if (permissionCheck != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch("android.permission.POST_NOTIFICATIONS")
            }
        }
    }

    Scaffold(
        containerColor = IosScreenBg,
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                CenterAlignedTopAppBar(
                    title = { 
                        Text(
                            text = "账本", 
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.SemiBold, 
                            fontSize = 17.sp,
                            color = IosTextPrimary
                        ) 
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = IosBg
                    ),
                    actions = {
                        if (pagerState.currentPage == 0) {
                            IconButton(onClick = { showAddDialog = true }) {
                                Icon(
                                    Icons.Outlined.Add, 
                                    contentDescription = "添加", 
                                    tint = IosBlue,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                )
                TabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = IosBg,
                    contentColor = IosBlue,
                    divider = { Divider(color = IosDivider, thickness = 0.5.dp) }
                ) {
                    val tabs = listOf("物品列表", "自动记账", "统计概览")
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = { coroutineScope.launch { pagerState.animateScrollToPage(index) } },
                            text = { 
                                Text(
                                    title, 
                                    fontFamily = FontFamily.SansSerif,
                                    color = if (pagerState.currentPage == index) IosBlue else IosTextSecondary,
                                    fontWeight = if (pagerState.currentPage == index) FontWeight.SemiBold else FontWeight.Normal
                                ) 
                            }
                        )
                    }
                }
            }
        }
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.padding(padding).fillMaxSize()
        ) { page ->
            when (page) {
                0 -> ItemListContent(
                        items = items,
                        onDelete = { viewModel.deleteItem(it) },
                        onEdit = { itemToEdit = it },
                        onSell = { itemToSell = it }
                     )
                1 -> AutoRecordContent(
                        pendingBills = pendingBills,
                        onDismiss = { viewModel.dismissAutoBill(it) },
                        onConvert = { billToConvert = it },
                        context = context
                     )
                2 -> OverviewContent(items = items, allBills = allBills)
            }
        }

        if (showAddDialog) {
            AddItemDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { name, price, dateMillis, residual ->
                    viewModel.addItem(name, price, dateMillis, residual)
                    showAddDialog = false
                }
            )
        }

        if (itemToEdit != null) {
            EditItemDialog(
                item = itemToEdit!!,
                onDismiss = { itemToEdit = null },
                onEdit = { name, price, dateMillis ->
                    viewModel.updateItemDetails(itemToEdit!!, name, price, dateMillis)
                    itemToEdit = null
                }
            )
        }

        if (itemToSell != null) {
            SellItemDialog(
                item = itemToSell!!,
                onDismiss = { itemToSell = null },
                onSell = { soldPrice, soldDateMillis ->
                    viewModel.sellItem(itemToSell!!, soldPrice, soldDateMillis)
                    itemToSell = null
                }
            )
        }
        
        if (billToConvert != null) {
            ConvertBillDialog(
                bill = billToConvert!!,
                onDismiss = { billToConvert = null },
                onConvert = { name, residual ->
                    viewModel.convertBillToItem(billToConvert!!, name, residual)
                    billToConvert = null
                }
            )
        }
    }
}

@Composable
fun AutoRecordContent(
    pendingBills: List<AutoBill>,
    onDismiss: (AutoBill) -> Unit,
    onConvert: (AutoBill) -> Unit,
    context: Context,
    modifier: Modifier = Modifier
) {
    var isEnabled by remember { mutableStateOf(isNotificationListenerEnabled(context)) }

    // Resume effect to refresh the state when coming back from settings
    DisposableEffect(context) {
        val observer = object : android.database.ContentObserver(android.os.Handler(android.os.Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                isEnabled = isNotificationListenerEnabled(context)
            }
        }
        context.contentResolver.registerContentObserver(
            Settings.Secure.getUriFor("enabled_notification_listeners"),
            false,
            observer
        )
        onDispose {
            context.contentResolver.unregisterContentObserver(observer)
        }
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        if (!isEnabled) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE5E5EA)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "需开启通知读取权限以自动记录主流平台账单",
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 14.sp,
                        color = IosTextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { 
                            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IosBlue)
                    ) {
                        Text("前往系统设置激活", fontFamily = FontFamily.SansSerif)
                    }
                }
            }
        }

        if (pendingBills.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("暂无捕获到的新账单", color = IosTextSecondary, fontFamily = FontFamily.SansSerif)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(pendingBills, key = { it.id }) { bill ->
                    Box(modifier = Modifier.animateItemPlacement(tween(250))) {
                        AutoBillCard(bill, onDismiss = { onDismiss(bill) }, onConvert = { onConvert(bill) })
                    }
                }
            }
        }
    }
}

@Composable
fun AutoBillCard(bill: AutoBill, onDismiss: () -> Unit, onConvert: () -> Unit) {
    val dateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    val dateStr = dateFormat.format(Date(bill.timestampMillis))
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp, 
                shape = RoundedCornerShape(16.dp), 
                spotColor = Color(0x0C000000), 
                ambientColor = Color.Transparent
            )
            .animateContentSize(animationSpec = tween(400)),
        colors = CardDefaults.cardColors(containerColor = IosCardBg),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = bill.merchantName,
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = IosTextPrimary
                )
                Box(contentAlignment = Alignment.TopEnd) {
                    var expanded by remember { mutableStateOf(false) }
                    IconButton(onClick = { expanded = true }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = "更多", tint = IosTextSecondary)
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(IosCardBg)
                    ) {
                        if (!bill.paymentMethod.isNullOrBlank()) {
                            DropdownMenuItem(
                                text = { Text("付款方式：${bill.paymentMethod}", fontFamily = FontFamily.SansSerif, color = IosTextSecondary, fontSize = 13.sp) },
                                onClick = { expanded = false }
                            )
                        }
                        if (!bill.fullPayeeName.isNullOrBlank()) {
                            DropdownMenuItem(
                                text = { Text("收款方全称：${bill.fullPayeeName}", fontFamily = FontFamily.SansSerif, color = IosTextSecondary, fontSize = 13.sp) },
                                onClick = { expanded = false }
                            )
                            Divider(color = IosDivider, thickness = 0.5.dp)
                        } else if (!bill.paymentMethod.isNullOrBlank()) {
                            Divider(color = IosDivider, thickness = 0.5.dp)
                        }
                        
                        DropdownMenuItem(
                            text = { Text("忽略此流水", fontFamily = FontFamily.SansSerif, color = IosRed) },
                            onClick = { expanded = false; onDismiss() },
                            leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = "忽略", tint = IosRed) }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "来源: ${bill.appSource}",
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 14.sp,
                    color = IosTextSecondary
                )
                Text(
                    text = dateStr,
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 14.sp,
                    color = IosTextSecondary
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "¥${String.format("%.2f", bill.amount)}",
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = IosTextPrimary
                )
                Button(
                    onClick = onConvert,
                    colors = ButtonDefaults.buttonColors(containerColor = IosBlue.copy(alpha = 0.1f), contentColor = IosBlue),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Outlined.Check, contentDescription = "导入", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("转为资产", fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun ConvertBillDialog(bill: AutoBill, onDismiss: () -> Unit, onConvert: (String, Double) -> Unit) {
    // 自动补全的智能化预推理 (Smart Auto-Complete)
    val guessedName = bill.merchantName.replace("账单", "").replace("支付", "").trim()
    var name by remember(bill) { mutableStateOf(guessedName) }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = IosBg,
        shape = RoundedCornerShape(14.dp),
        title = { 
            Text(
                "将自动记账转化为资产", 
                fontFamily = FontFamily.SansSerif, 
                fontWeight = FontWeight.SemiBold,
                color = IosTextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            ) 
        },
        text = {
            Column {
                Text("从 ${bill.appSource} 消费 ¥${bill.amount}", color = IosTextSecondary, fontSize = 13.sp, modifier = Modifier.padding(bottom = 12.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("物品真实名称", fontFamily = FontFamily.SansSerif) },
                    singleLine = true,
                    isError = isError && name.isBlank(),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onConvert(name, 0.0)
                    } else {
                        isError = true
                    }
                }
            ) {
                Text("确定添加", color = IosBlue, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.SansSerif)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = IosBlue, fontFamily = FontFamily.SansSerif) }
        }
    )
}

// ========================
// Existing Content Functions (Unchanged logic, just UI matching)
// ========================

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ItemListContent(
    items: List<Item>,
    onDelete: (Int) -> Unit,
    onEdit: (Item) -> Unit,
    onSell: (Item) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("还没有录入任何物品", color = IosTextSecondary, fontFamily = FontFamily.SansSerif)
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(items, key = { it.id }) { item ->
                Box(modifier = Modifier.animateItemPlacement(tween(250))) {
                    ItemCard(
                        item = item, 
                        onDelete = { onDelete(item.id) },
                        onEdit = { onEdit(item) },
                        onSell = { onSell(item) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OverviewContent(items: List<Item>, allBills: List<AutoBill>, modifier: Modifier = Modifier) {
    val totalSpent = items.sumOf { it.price }
    val totalRecovered = items.filter { it.isSold }.sumOf { it.residualValue }
    val netSpend = max(totalSpent - totalRecovered, 0.0)
    
    val activeItems = items.count { !it.isSold }
    val soldItems = items.count { it.isSold }

    val nowMillis = System.currentTimeMillis()
    var totalDailyCost = 0.0
    items.forEach { item ->
        val endDate = if (item.isSold) item.soldDateMillis ?: nowMillis else nowMillis
        val diffMillis = max(endDate - item.purchaseDateMillis, 0)
        var daysPassed = TimeUnit.MILLISECONDS.toDays(diffMillis)
        if (daysPassed < 1L) daysPassed = 1L
        val netCost = if (item.isSold) max(item.price - item.residualValue, 0.0) else item.price
        totalDailyCost += (netCost / daysPassed)
    }

    // 账单统计：恢复高性能的按区间总和映射表
    val dailyFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val monthFormat = remember { SimpleDateFormat("yyyy-MM", Locale.getDefault()) }
    val yearFormat = remember { SimpleDateFormat("yyyy", Locale.getDefault()) }

    val dailySums = remember(allBills) {
        val map = mutableMapOf<String, Double>()
        allBills.forEach { bill ->
            val key = dailyFormat.format(Date(bill.timestampMillis))
            map[key] = (map[key] ?: 0.0) + bill.amount
        }
        map
    }

    val monthlySums = remember(allBills) {
        val map = mutableMapOf<String, Double>()
        allBills.forEach { bill ->
            val key = monthFormat.format(Date(bill.timestampMillis))
            map[key] = (map[key] ?: 0.0) + bill.amount
        }
        map
    }

    val yearlySums = remember(allBills) {
        val map = mutableMapOf<String, Double>()
        allBills.forEach { bill ->
            val key = yearFormat.format(Date(bill.timestampMillis))
            map[key] = (map[key] ?: 0.0) + bill.amount
        }
        map
    }

    // 步进器状态
    var dayOffset by remember { mutableStateOf(0) }
    var monthOffset by remember { mutableStateOf(0) }
    var yearOffset by remember { mutableStateOf(0) }

    val cal = Calendar.getInstance()

    // 日计算
    cal.timeInMillis = nowMillis
    cal.add(Calendar.DAY_OF_YEAR, dayOffset)
    val currentDayKey = dailyFormat.format(cal.time)
    val currentDaySum = dailySums[currentDayKey] ?: 0.0
    val dayLabel = if (dayOffset == 0) "今日新增记账" else SimpleDateFormat("MM-dd 支出", Locale.getDefault()).format(cal.time)

    // 月计算
    cal.timeInMillis = nowMillis
    cal.add(Calendar.MONTH, monthOffset)
    val currentMonthKey = monthFormat.format(cal.time)
    val currentMonthSum = monthlySums[currentMonthKey] ?: 0.0
    val monthLabel = if (monthOffset == 0) "本月累计支出" else SimpleDateFormat("yyyy-MM 支出", Locale.getDefault()).format(cal.time)

    // 年计算
    cal.timeInMillis = nowMillis
    cal.add(Calendar.YEAR, yearOffset)
    val currentYearKey = yearFormat.format(cal.time)
    val currentYearSum = yearlySums[currentYearKey] ?: 0.0
    val yearLabel = if (yearOffset == 0) "本年度总花销" else "${currentYearKey}年 总花销"

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            StandardOverviewCard("资产总账簿") {
                OverviewRow("总计投入金额", "¥${String.format("%.2f", totalSpent)}", IosTextPrimary, 18.sp)
                Spacer(modifier = Modifier.height(16.dp))
                OverviewRow("已变现回血", "¥${String.format("%.2f", totalRecovered)}", IosBlue, 18.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = IosDivider, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(16.dp))
                OverviewRow("实际净消耗", "¥${String.format("%.2f", netSpend)}", IosRed, 26.sp, true)
                Spacer(modifier = Modifier.height(16.dp))
                OverviewRow("整体日均折旧金", "¥${String.format("%.2f", totalDailyCost)}", IosTextPrimary, 16.sp)
            }
        }

        item {
            StandardOverviewCard("设备库存状态") {
                OverviewRow("在役吃灰中", "$activeItems 件", IosTextPrimary, 18.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = IosDivider, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(16.dp))
                OverviewRow("已结清完美脱手", "$soldItems 件", IosBlue, 18.sp)
            }
        }

        item {
            StandardOverviewCard("日常流水大盘") {
                SteppableOverviewRow(
                    label = dayLabel, 
                    value = "¥${String.format("%.2f", currentDaySum)}", 
                    valueColor = IosTextPrimary, 
                    fontSize = 18.sp,
                    onPrevious = { dayOffset-- },
                    onNext = if (dayOffset < 0) { { dayOffset++ } } else null
                )
                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = IosDivider, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(16.dp))
                SteppableOverviewRow(
                    label = monthLabel, 
                    value = "¥${String.format("%.2f", currentMonthSum)}", 
                    valueColor = IosRed, 
                    fontSize = 26.sp, 
                    isBold = true,
                    onPrevious = { monthOffset-- },
                    onNext = if (monthOffset < 0) { { monthOffset++ } } else null
                )
                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = IosDivider, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(16.dp))
                SteppableOverviewRow(
                    label = yearLabel, 
                    value = "¥${String.format("%.2f", currentYearSum)}", 
                    valueColor = IosTextPrimary, 
                    fontSize = 18.sp,
                    onPrevious = { yearOffset-- },
                    onNext = if (yearOffset < 0) { { yearOffset++ } } else null
                )
            }
            Spacer(modifier = Modifier.height(24.dp)) // 底部留白
        }
    }
}

@Composable
fun SteppableOverviewRow(
    label: String, 
    value: String, 
    valueColor: Color, 
    fontSize: androidx.compose.ui.unit.TextUnit, 
    isBold: Boolean = false,
    onPrevious: () -> Unit,
    onNext: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(), 
        horizontalArrangement = Arrangement.SpaceBetween, 
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrevious, modifier = Modifier.size(28.dp)) {
                Text("◀", fontSize = 14.sp, color = IosTextSecondary)
            }
            Text(
                text = label, 
                fontFamily = FontFamily.SansSerif, 
                fontSize = 15.sp, 
                color = IosTextPrimary,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            IconButton(
                onClick = { onNext?.invoke() }, 
                modifier = Modifier.size(28.dp),
                enabled = onNext != null
            ) {
                if (onNext != null) {
                    Text("▶", fontSize = 14.sp, color = IosTextSecondary)
                }
            }
        }
        Text(
            text = value, 
            fontFamily = FontFamily.SansSerif, 
            fontWeight = if (isBold) FontWeight.Black else FontWeight.SemiBold, 
            fontSize = fontSize, 
            color = valueColor
        )
    }
}

@Composable
fun StandardOverviewCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp, 
                shape = RoundedCornerShape(16.dp), 
                spotColor = Color(0x0C000000), 
                ambientColor = Color.Transparent
            ),
        colors = CardDefaults.cardColors(containerColor = IosCardBg),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp).fillMaxWidth()) {
            Text(
                text = title, 
                fontFamily = FontFamily.SansSerif, 
                fontSize = 14.sp, 
                fontWeight = FontWeight.SemiBold,
                color = IosTextSecondary
            )
            Spacer(modifier = Modifier.height(20.dp))
            content()
        }
    }
}

@Composable
fun OverviewRow(label: String, value: String, valueColor: Color, fontSize: androidx.compose.ui.unit.TextUnit, isBold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(), 
        horizontalArrangement = Arrangement.SpaceBetween, 
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label, 
            fontFamily = FontFamily.SansSerif, 
            fontSize = 15.sp, 
            color = IosTextPrimary
        )
        Text(
            text = value, 
            fontFamily = FontFamily.SansSerif, 
            fontWeight = if (isBold) FontWeight.Black else FontWeight.SemiBold, 
            fontSize = fontSize, 
            color = valueColor
        )
    }
}

@Composable
fun ItemCard(item: Item, onDelete: () -> Unit, onEdit: () -> Unit, onSell: () -> Unit) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val dateStr = dateFormat.format(Date(item.purchaseDateMillis))
    val soldDateStr = item.soldDateMillis?.let { dateFormat.format(Date(it)) }
    
    val now = System.currentTimeMillis()
    val endDate = if (item.isSold) item.soldDateMillis ?: now else now
    val diffMillis = max(endDate - item.purchaseDateMillis, 0)
    var daysPassed = TimeUnit.MILLISECONDS.toDays(diffMillis)
    if (daysPassed < 1L) daysPassed = 1L
    
    val netCost = if (item.isSold) max(item.price - item.residualValue, 0.0) else item.price
    val dailyCost = netCost / daysPassed

    val alphaFactor by animateFloatAsState(targetValue = if (item.isSold) 0.5f else 1f, label = "alpha", animationSpec = tween(400))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp, 
                shape = RoundedCornerShape(16.dp), 
                spotColor = Color(0x0C000000), 
                ambientColor = Color.Transparent
            )
            .animateContentSize(animationSpec = tween(400)),
        colors = CardDefaults.cardColors(containerColor = IosCardBg),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.name, 
                        fontFamily = FontFamily.SansSerif, 
                        fontSize = 19.sp, 
                        fontWeight = FontWeight.Bold,
                        color = IosTextPrimary.copy(alpha = alphaFactor)
                    )
                    if (item.isSold) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(color = Color(0xFFF2F2F7).copy(alpha = alphaFactor), shape = RoundedCornerShape(6.dp)) {
                            Text("已结清", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontFamily = FontFamily.SansSerif, fontSize = 11.sp, color = IosTextSecondary.copy(alpha = alphaFactor))
                        }
                    }
                }
                Box(contentAlignment = Alignment.TopEnd) {
                    var expanded by remember { mutableStateOf(false) }
                    IconButton(onClick = { expanded = true }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = "更多", tint = IosTextSecondary)
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(IosCardBg)
                    ) {
                        DropdownMenuItem(
                            text = { Text("编辑", fontFamily = FontFamily.SansSerif) },
                            onClick = { expanded = false; onEdit() },
                            leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = "编辑", tint = IosBlue) }
                        )
                        DropdownMenuItem(
                            text = { Text("删除", fontFamily = FontFamily.SansSerif, color = IosRed) },
                            onClick = { expanded = false; onDelete() },
                            leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = "删除", tint = IosRed) }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("总价: ¥${String.format("%.2f", item.price)}", fontFamily = FontFamily.SansSerif, fontSize = 15.sp, color = IosTextPrimary.copy(alpha = alphaFactor))
                Text("购买: $dateStr", fontFamily = FontFamily.SansSerif, fontSize = 14.sp, color = IosTextSecondary.copy(alpha = alphaFactor))
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                if (item.isSold) {
                    Text("卖出回血: ¥${String.format("%.2f", item.residualValue)}", fontFamily = FontFamily.SansSerif, fontSize = 15.sp, color = IosTextSecondary.copy(alpha = alphaFactor))
                    if (soldDateStr != null) {
                        Text("结清: $soldDateStr", fontFamily = FontFamily.SansSerif, fontSize = 14.sp, color = IosTextSecondary.copy(alpha = alphaFactor))
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Column {
                    Text("共计使用: $daysPassed 天", fontFamily = FontFamily.SansSerif, fontSize = 14.sp, color = IosTextSecondary.copy(alpha = alphaFactor))
                    if (!item.isSold) {
                        TextButton(onClick = onSell, contentPadding = PaddingValues(0.dp), modifier = Modifier.height(30.dp)) {
                            Text("二手残值", color = IosBlue, fontFamily = FontFamily.SansSerif, fontSize = 14.sp)
                        }
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("日均成本", fontFamily = FontFamily.SansSerif, fontSize = 12.sp, color = IosTextSecondary.copy(alpha = alphaFactor))
                    Text(
                        "¥${String.format("%.2f", dailyCost)}",
                        fontFamily = FontFamily.SansSerif, fontSize = 28.sp,
                        color = if(item.isSold) IosTextPrimary.copy(alpha = alphaFactor) else IosBlue,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

@Composable
fun AddItemDialog(onDismiss: () -> Unit, onAdd: (String, Double, Long, Double) -> Unit) {
    var name by remember { mutableStateOf("") }
    var priceStr by remember { mutableStateOf("") }
    var residualStr by remember { mutableStateOf("") }
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    var dateStr by remember { mutableStateOf(dateFormat.format(Date())) }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = IosBg,
        shape = RoundedCornerShape(14.dp),
        title = { Text("添加新物品", fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, color = IosTextPrimary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("物品名称", fontFamily = FontFamily.SansSerif) }, singleLine = true, isError = isError && name.isBlank(), modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
                OutlinedTextField(value = priceStr, onValueChange = { priceStr = it }, label = { Text("入手总价 (￥)", fontFamily = FontFamily.SansSerif) }, singleLine = true, isError = isError && (priceStr.toDoubleOrNull() ?: 0.0) <= 0, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
                OutlinedTextField(value = dateStr, onValueChange = { dateStr = it }, label = { Text("购买日期 (YYYY-MM-DD)", fontFamily = FontFamily.SansSerif) }, singleLine = true, isError = isError && runCatching { dateFormat.parse(dateStr) }.isFailure, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val price = priceStr.toDoubleOrNull() ?: 0.0
                val dateMillis = try { dateFormat.parse(dateStr)?.time ?: 0L } catch (e: Exception) { 0L }
                if (name.isNotBlank() && price > 0 && dateMillis > 0) onAdd(name, price, dateMillis, 0.0) else isError = true
            }) { Text("保存", color = IosBlue, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.SansSerif) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = IosBlue, fontFamily = FontFamily.SansSerif) } }
    )
}

@Composable
fun EditItemDialog(item: Item, onDismiss: () -> Unit, onEdit: (String, Double, Long) -> Unit) {
    var name by remember { mutableStateOf(item.name) }
    var priceStr by remember { mutableStateOf(if (item.price > 0) item.price.toString() else "") }
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    var dateStr by remember { mutableStateOf(dateFormat.format(Date(item.purchaseDateMillis))) }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = IosBg,
        shape = RoundedCornerShape(14.dp),
        title = { Text("编辑物品信息", fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, color = IosTextPrimary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("物品名称", fontFamily = FontFamily.SansSerif) }, singleLine = true, isError = isError && name.isBlank(), modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
                OutlinedTextField(value = priceStr, onValueChange = { priceStr = it }, label = { Text("入手总价 (￥)", fontFamily = FontFamily.SansSerif) }, singleLine = true, isError = isError && (priceStr.toDoubleOrNull() ?: 0.0) <= 0, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
                OutlinedTextField(value = dateStr, onValueChange = { dateStr = it }, label = { Text("购买日期 (YYYY-MM-DD)", fontFamily = FontFamily.SansSerif) }, singleLine = true, isError = isError && runCatching { dateFormat.parse(dateStr) }.isFailure, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val price = priceStr.toDoubleOrNull() ?: 0.0
                val dateMillis = try { dateFormat.parse(dateStr)?.time ?: 0L } catch (e: Exception) { 0L }
                if (name.isNotBlank() && price > 0 && dateMillis > 0) onEdit(name, price, dateMillis) else isError = true
            }) { Text("保存更改", color = IosBlue, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.SansSerif) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = IosBlue, fontFamily = FontFamily.SansSerif) } }
    )
}

@Composable
fun SellItemDialog(item: Item, onDismiss: () -> Unit, onSell: (Double, Long) -> Unit) {
    var priceStr by remember { mutableStateOf(if(item.residualValue > 0) item.residualValue.toString() else "") }
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    var dateStr by remember { mutableStateOf(dateFormat.format(Date())) }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = IosBg,
        shape = RoundedCornerShape(14.dp),
        title = { Text("登记售出", fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, color = IosTextPrimary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        text = {
            Column {
                Text("核算【${item.name}】的真实最终净成本。", fontFamily = FontFamily.SansSerif, fontSize = 13.sp, color = IosTextSecondary, modifier = Modifier.padding(bottom = 12.dp), textAlign = TextAlign.Center)
                OutlinedTextField(value = priceStr, onValueChange = { priceStr = it }, label = { Text("回血/卖出金额 (￥)", fontFamily = FontFamily.SansSerif) }, singleLine = true, isError = isError && priceStr.isBlank(), modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
                OutlinedTextField(value = dateStr, onValueChange = { dateStr = it }, label = { Text("结清日期 (YYYY-MM-DD)", fontFamily = FontFamily.SansSerif) }, singleLine = true, isError = isError && runCatching { dateFormat.parse(dateStr) }.isFailure, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val price = priceStr.toDoubleOrNull() ?: 0.0
                val dateMillis = try { dateFormat.parse(dateStr)?.time ?: 0L } catch (e: Exception) { 0L }
                if (dateMillis > 0) onSell(price, dateMillis) else isError = true
            }) { Text("确认核算", color = IosBlue, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.SansSerif) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = IosBlue, fontFamily = FontFamily.SansSerif) } }
    )
}
