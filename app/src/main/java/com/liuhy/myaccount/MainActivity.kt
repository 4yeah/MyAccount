package com.liuhy.myaccount

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.liuhy.myaccount.bus.PendingPrefill
import com.liuhy.myaccount.bus.PendingPrefillBus
import com.liuhy.myaccount.core.common.ThemeOption
import com.liuhy.myaccount.core.data.ThemePreferences
import com.liuhy.myaccount.core.data.di.RepositoryProvider
import com.liuhy.myaccount.core.data.model.TransactionType
import com.liuhy.myaccount.feature.accounting.AccountBookScreen
import com.liuhy.myaccount.feature.accounting.CalendarScreen
import com.liuhy.myaccount.feature.accounting.TransactionScreen
import com.liuhy.myaccount.feature.note.NoteEditScreen
import com.liuhy.myaccount.feature.note.NoteListScreen
import com.liuhy.myaccount.feature.settings.BudgetSettingsScreen
import com.liuhy.myaccount.feature.settings.CategoryManageScreen
import com.liuhy.myaccount.feature.settings.ThemeSettingsScreen
import com.liuhy.myaccount.feature.settings.ThemeSelectScreen
import com.liuhy.myaccount.feature.settings.BackupScreen
import com.liuhy.myaccount.feature.statistics.StatisticsScreen
import com.liuhy.myaccount.service.PaymentNotificationListener
import com.liuhy.myaccount.ui.theme.MyAppTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch



/**
 * 应用主入口 Activity。
 * 
 * 职责：
 * 1. 初始化数据仓库 `RepositoryProvider` 与系统主题适配。
 * 2. 启动 `KeepAliveService` 前台服务，保障在小米等系统中通知监听不被杀。
 * 3. 处理由通知点击触发的预填数据（金额/类型/备注），通过事件总线下发给记账页。
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize repositories for data access
        RepositoryProvider.initialize(this)
        enableEdgeToEdge()
        
        // 启动前台保活服务，防止小米系统杀死进程
        startKeepAliveService()
        
        // 调试：打印数据库分类信息
        debugPrintCategories()
        
        // 通过通知点击进入时，把 Intent 里的预填数据丢到事件总线
        consumeIntentPrefill(intent)
        setContent {
            MyAppTheme {
                MainApp()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // launchMode=singleTask 时复用同一个实例，需要在这里再次处理预填 Intent
        setIntent(intent)
        consumeIntentPrefill(intent)
    }
    
    /**
     * 启动前台保活服务，防止小米系统杀死进程导致通知监听失效
     */
    private fun startKeepAliveService() {
        // Create an intent for the foreground service
        val serviceIntent = Intent(this, com.liuhy.myaccount.service.KeepAliveService::class.java)
        // Start the service based on Android version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    private fun consumeIntentPrefill(intent: Intent?) {
        // Return early if intent is null
        if (intent == null) return
        // Return early if no prefill amount is present
        if (!intent.hasExtra(PaymentNotificationListener.EXTRA_PREFILL_AMOUNT)) return
        // Get the prefill amount, defaulting to -1.0 if not found
        val amount = intent.getDoubleExtra(PaymentNotificationListener.EXTRA_PREFILL_AMOUNT, -1.0)
        // Return early if amount is invalid
        if (amount <= 0.0) return
        // Get the transaction type string, defaulting to "EXPENSE" if not found
        val typeStr = intent.getStringExtra(PaymentNotificationListener.EXTRA_PREFILL_TYPE)
        // Parse the transaction type, defaulting to EXPENSE if parsing fails
        val type = runCatching { TransactionType.valueOf(typeStr ?: "EXPENSE") }
            .getOrDefault(TransactionType.EXPENSE)
        // Get the note, defaulting to empty string if not found
        val note = intent.getStringExtra(PaymentNotificationListener.EXTRA_PREFILL_NOTE).orEmpty()
        // Publish the prefill data to the event bus
        PendingPrefillBus.publish(PendingPrefill(amount = amount, type = type, note = note))
        // 消费完毕后清掉 extras，避免重复触发
        intent.removeExtra(PaymentNotificationListener.EXTRA_PREFILL_AMOUNT)
        intent.removeExtra(PaymentNotificationListener.EXTRA_PREFILL_TYPE)
        intent.removeExtra(PaymentNotificationListener.EXTRA_PREFILL_NOTE)
    }
    
    /**
     * 调试：打印数据库中的分类信息
     */
    private fun debugPrintCategories() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = RepositoryProvider.categoryRepository()
                val categories = repo.getAllCategories().first()
                
                Log.d("[DB_DEBUG]", "========== 数据库分类信息 ==========")
                Log.d("[DB_DEBUG]", "总计: ${categories.size} 个分类")
                
                val level1Categories = categories.filter { it.parentId == null }
                Log.d("[DB_DEBUG]", "一级分类: ${level1Categories.size} 个")
                
                level1Categories.forEach { parent ->
                    val children = categories.filter { it.parentId == parent.id }
                    Log.d("[DB_DEBUG]", "  ├─ ${parent.name} (id=${parent.id})")
                    if (children.isNotEmpty()) {
                        Log.d("[DB_DEBUG]", "  │  二级分类 (${children.size}个):")
                        children.forEach { child ->
                            Log.d("[DB_DEBUG]", "  │    └─ ${child.name} (id=${child.id})")
                        }
                    }
                }
                Log.d("[DB_DEBUG]", "=====================================")
            } catch (e: Exception) {
                Log.e("[DB_DEBUG]", "读取分类失败", e)
            }
        }
    }
}

@Composable
fun MainApp() {
    val themePrefs = remember { ThemePreferences(RepositoryProvider.getContext()) }
    val currentTheme by themePrefs.themeFlow.collectAsState(initial = ThemeOption.MATERIAL)

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in listOf("account_book", "calendar", "statistics", "note_list")
    var isInBatchMode by remember { mutableStateOf(false) }
    val effectiveShowBottomBar = showBottomBar && !isInBatchMode

    // 监听通知监听服务推送过来的预填数据：
    // 一旦有新数据，立即跳转到记账页（新增模式，保留在 AccountBookScreen 之上）。
    // TransactionScreen 会在自己 LaunchedEffect 中消费 PendingPrefillBus 的当前值。
    val pendingPrefill by PendingPrefillBus.state.collectAsState()
    LaunchedEffect(pendingPrefill) {
        // currentRoute 拿到的是路由模板 "transaction?id={id}"，所以用 startsWith 判断
        val onTransactionScreen = currentRoute?.startsWith("transaction") == true
        if (pendingPrefill != null && !onTransactionScreen) {
            navController.navigate(ROUTE_TRANSACTION_NEW) {
                launchSingleTop = true
            }
        }
    }

    MyAppTheme(themeOption = currentTheme) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                if (effectiveShowBottomBar) {
                    BottomNavigationBar(
                        currentRoute = currentRoute,
                        onNavigate = { route ->
                            navController.navigate(route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        ) { innerPadding ->
            AppNavHost(
                navController = navController,
                onBatchModeChange = { isInBatchMode = it },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        }
    }
}

@Composable
fun BottomNavigationBar(currentRoute: String?, onNavigate: (String) -> Unit) {
    val items = listOf(
        Triple("account_book", "记账", Icons.Default.Home),
        Triple("calendar", "日历", Icons.Default.DateRange),
        Triple("statistics", "统计", Icons.Default.Assessment),
        Triple("note_list", "笔记", Icons.Default.Edit),
        Triple("theme_settings", "设置", Icons.Default.Settings)
    )
    NavigationBar {
        items.forEach { (route, label, icon) ->
            NavigationBarItem(
                selected = currentRoute == route,
                onClick = { onNavigate(route) },
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label) }
            )
        }
    }
}

/**
 * 路由命名约定：
 *
 * 新增 / 编辑记账只对应一个页面 `TransactionScreen`，因此只用一条路由
 * `transaction?id={id}`：
 *   - 不带 id   → 新增模式（[ROUTE_TRANSACTION_NEW]）
 *   - 带 id    → 编辑模式（用 [routeTransactionEdit] 构造）
 *
 * 这样避免了之前 add_transaction / edit_transaction 两条路由指向同一个
 * Composable 的冗余写法。
 */
const val ROUTE_TRANSACTION = "transaction?id={id}"
const val ROUTE_TRANSACTION_NEW = "transaction"
fun routeTransactionEdit(id: Long): String = "transaction?id=$id"

@Composable
fun AppNavHost(navController: NavHostController, onBatchModeChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = "account_book",
        modifier = modifier,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None }
    ) {
        composable("account_book") {
            AccountBookScreen(
                onAddTransaction = { navController.navigate(ROUTE_TRANSACTION_NEW) },
                onEditTransaction = { id -> navController.navigate(routeTransactionEdit(id)) },
                onBudgetSettings = { navController.navigate("budget_settings") },
                onBatchModeChange = onBatchModeChange
            )
        }
        composable(
            route = ROUTE_TRANSACTION,
            arguments = listOf(
                navArgument("id") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val transactionId = backStackEntry.arguments?.getString("id")?.toLongOrNull()
            // 仅在新增模式下消费一次预填数据；编辑模式不预填
            val prefill = if (transactionId == null) PendingPrefillBus.consume() else null
            TransactionScreen(
                transactionId = transactionId,
                onBack = { navController.popBackStack() },
                onNavigateToCategoryManage = { parentId ->
                    navController.navigate("category_manage?parentId=${parentId ?: -1}")
                },
                prefillAmount = prefill?.amount,
                prefillType = prefill?.type,
                prefillNote = prefill?.note
            )
        }
        composable("statistics") {
            StatisticsScreen()
        }
        composable("calendar") {
            CalendarScreen(
                onAddTransaction = { navController.navigate(ROUTE_TRANSACTION_NEW) },
                onEditTransaction = { id -> navController.navigate(routeTransactionEdit(id)) }
            )
        }
        composable("note_list") {
            NoteListScreen(
                onAddNote = { navController.navigate("note_edit/-1") },
                onNoteClick = { id -> navController.navigate("note_edit/$id") }
            )
        }
        composable("note_edit/{noteId}") { backStackEntry ->
            val noteId = backStackEntry.arguments?.getString("noteId")
                ?.toLongOrNull()
                ?.takeIf { it > 0 }
            NoteEditScreen(
                noteId = noteId,
                onBack = { navController.popBackStack() }
            )
        }
        composable("budget_settings") {
            BudgetSettingsScreen(onBack = { navController.popBackStack() })
        }
        composable("theme_settings") {
            ThemeSettingsScreen(
                onBack = { navController.popBackStack() },
                onCategoryManage = { navController.navigate("category_manage") },
                onThemeSelect = { navController.navigate("theme_select") },
                onBackup = { navController.navigate("backup") }
            )
        }
        composable("theme_select") {
            ThemeSelectScreen(onBack = { navController.popBackStack() })
        }
        composable("backup") {
            BackupScreen(onBack = { navController.popBackStack() })
        }
        composable("category_manage?parentId={parentId}") { backStackEntry ->
            val parentId = backStackEntry.arguments?.getString("parentId")?.toLongOrNull()
            CategoryManageScreen(
                onBack = { navController.popBackStack() },
                initialParentId = parentId
            )
        }
    }
}