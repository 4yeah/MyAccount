/**
 * 设置首页。
 *
 * 汇集所有设置入口：主题切换、分类管理、预算设置、数据备份、
 * 通知监听权限检查、版本信息等。
 */
package com.liuhy.myaccount.feature.settings

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.liuhy.myaccount.core.common.ThemeOption

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ThemeSettingsScreen(
    onBack: () -> Unit,
    onCategoryManage: () -> Unit,
    onThemeSelect: () -> Unit,
    onBackup: () -> Unit,
    viewModel: ThemeSettingsViewModel = viewModel(factory = ThemeSettingsViewModel.Factory)
) {

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                }
                Text(
                    text = "设置",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // 外观
            SettingItem(
                title = "外观",
                subtitle = "主题、配色",
                icon = Icons.Default.Palette,
                onClick = onThemeSelect
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 分类管理
            SettingItem(
                title = "分类管理",
                subtitle = "添加、编辑、排序分类",
                icon = Icons.Default.Category,
                onClick = onCategoryManage
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 数据备份
            SettingItem(
                title = "数据备份",
                subtitle = "云端备份与恢复",
                icon = Icons.Default.Cloud,
                onClick = onBackup
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 自动捕获
            AutoCaptureCard()
        }
    }
}

/**
 * 自动捕获微信/支付宝支付通知的设置卡片。
 *
 * 显示当前授权状态，并提供按钮跳转到系统的"通知使用权"设置页。
 * 卡片重新进入前台时会自动刷新授权状态（用户从系统设置返回后能立刻看到变化）。
 */
@Composable
private fun AutoCaptureCard() {
    val context = LocalContext.current
    var notifListenerEnabled by remember { mutableStateOf(isNotificationListenerEnabled(context)) }
    var notifPermissionGranted by remember { mutableStateOf(isPostNotificationsGranted(context)) }
    var accessibilityEnabled by remember { mutableStateOf(isAccessibilityServiceEnabled(context)) }

    // 监听 ON_RESUME，从系统设置页返回时刷新授权状态
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notifListenerEnabled = isNotificationListenerEnabled(context)
                notifPermissionGranted = isPostNotificationsGranted(context)
                accessibilityEnabled = isAccessibilityServiceEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "自动捕获支付通知 🔔",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "开启后，当微信、支付宝或银行 App 产生支付/动账通知时，自动解析金额并弹出快速记账提示。",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(12.dp))

            // 1. 通知使用权
            PermissionRow(
                label = "通知使用权",
                granted = notifListenerEnabled,
                onClick = {
                    runCatching {
                        context.startActivity(
                            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                }
            )
            Spacer(modifier = Modifier.height(8.dp))

            // 2. 通知权限（Android 13+）
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                PermissionRow(
                    label = "发送通知权限",
                    granted = notifPermissionGranted,
                    onClick = {
                        runCatching {
                            context.startActivity(
                                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 3. 无障碍服务
            PermissionRow(
                label = "无障碍服务（备用）",
                granted = accessibilityEnabled,
                onClick = {
                    runCatching {
                        context.startActivity(
                            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 支持的应用列表
            Text(
                text = "已支持",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(4.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val supportedApps = listOf(
                    "微信", "支付宝",
                    "工商银行", "建设银行", "农业银行", "中国银行",
                    "招商银行", "交通银行", "邮储银行",
                    "中信银行", "光大银行", "平安银行",
                    "浦发银行", "兴业银行", "民生银行"
                )
                supportedApps.forEach { app ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = app,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // MIUI 额外项：省电策略 + 自启动管理
            if (isMiui()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "小米/红米用户必读",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "MIUI 系统会激进地杀后台、限制自启动，会让通知监听静默失效。请把以下两项调整好：",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        MiuiActionRow(
                            label = "省电策略：无限制",
                            description = "防止后台被杀导致通知监听失效",
                            onClick = { openMiuiBatterySaver(context) }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        MiuiActionRow(
                            label = "自启动管理：允许",
                            description = "允许应用在后台被通知拉起",
                            onClick = { openMiuiAutoStart(context) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionRow(
    label: String,
    granted: Boolean,
    onClick: () -> Unit
) {
    // 整行点击就能跳到对应系统设置页；不区分授权与否——
    // 已授权用户也可能想去关闭/复核，按钮一直在更利于发现。
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = if (granted) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (granted) Color(0xFF4CAF50) else Color(0xFFFF9800),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = if (granted) "已开启" else "未开启",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (granted) Color(0xFF4CAF50) else Color(0xFFFF9800)
                )
            }
        }
        OutlinedButton(onClick = onClick) {
            Text(if (granted) "管理" else "去开启")
        }
    }
}

/**
 * MIUI 设置行：与 [PermissionRow] 形态对齐，但不显示授权状态徽标——
 * MIUI 的省电策略/自启动管理不暴露查询 API，无法判断是否已开启，因此只提供入口。
 */
@Composable
private fun MiuiActionRow(
    label: String,
    description: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
            )
        }
        OutlinedButton(onClick = onClick) {
            Text("去设置")
        }
    }
}

/**
 * 依次尝试打开候选 Intent，第一个能成功 startActivity 的就停下；
 * 全部失败时回退到本应用的「应用详情」页。
 */
private fun startFirstAvailable(context: Context, candidates: List<Intent>) {
    for (intent in candidates) {
        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            return
        } catch (_: Throwable) {
            // 当前候选不存在或不允许启动，继续尝试下一个
        }
    }
    // 兜底：跳本应用的"应用详情"页，那里通常能找到电池/自启动等入口
    runCatching {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.fromParts("package", context.packageName, null))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

/**
 * 跳转到 MIUI 省电策略（应用电池保护）页面。
 * 不同 MIUI 版本里 Activity 名字会变，按可用性顺序尝试。
 */
private fun openMiuiBatterySaver(context: Context) {
    startFirstAvailable(
        context,
        listOf(
            // 直接进单个应用的电池保护配置（部分版本可用，能直接定位到本应用）
            Intent().setComponent(
                ComponentName(
                    "com.miui.powerkeeper",
                    "com.miui.powerkeeper.ui.HiddenAppsConfigActivity"
                )
            ).putExtra("package_name", context.packageName)
                .putExtra("package_label", "豪账本"),
            // 进电池保护应用列表（用户自己找豪账本）
            Intent().setComponent(
                ComponentName(
                    "com.miui.powerkeeper",
                    "com.miui.powerkeeper.ui.HiddenAppsContainerManagementActivity"
                )
            )
        )
    )
}

/**
 * 跳转到 MIUI 自启动管理页面。
 */
private fun openMiuiAutoStart(context: Context) {
    startFirstAvailable(
        context,
        listOf(
            Intent().setComponent(
                ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"
                )
            ),
            // 旧版安全中心包名
            Intent().setComponent(
                ComponentName(
                    "com.android.settings",
                    "com.android.settings.BackgroundApplicationsManager"
                )
            )
        )
    )
}

private fun isMiui(): Boolean {
    return runCatching {
        Class.forName("android.os.SystemProperties")
            .getMethod("get", String::class.java)
            .invoke(null, "ro.miui.ui.version.name")
            ?.toString()
            ?.isNotBlank() == true
    }.getOrDefault(false)
}

private fun isNotificationListenerEnabled(context: android.content.Context): Boolean {
    val flat = Settings.Secure.getString(
        context.contentResolver,
        "enabled_notification_listeners"
    ) ?: return false
    if (flat.isEmpty()) return false
    val expected = ComponentName(
        context.packageName,
        "com.liuhy.myaccount.service.PaymentNotificationListener"
    )
    return flat.split(":").any { token ->
        ComponentName.unflattenFromString(token) == expected
    }
}

private fun isPostNotificationsGranted(context: android.content.Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else true
}

private fun isAccessibilityServiceEnabled(context: android.content.Context): Boolean {
    val flat = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false
    if (flat.isEmpty()) return false
    val expected = ComponentName(
        context.packageName,
        "com.liuhy.myaccount.service.PaymentAccessibilityService"
    )
    return flat.split(":").any { token ->
        ComponentName.unflattenFromString(token) == expected
    }
}

/**
 * 分类管理入口卡片
 */
@Composable
private fun CategoryManageCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "分类管理",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "管理记账分类，支持一级分类和二级分类的增删改查。",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
internal fun ThemeCard(
    option: ThemeOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)

    Card(
        onClick = onClick,
        modifier = Modifier.size(120.dp, 100.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(if (isSelected) 3.dp else 1.dp, borderColor),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(option.emoji, style = MaterialTheme.typography.displaySmall)
                Text(
                    text = option.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )
                if (isSelected) {
                    Text(
                        text = "✓",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * 数据备份入口卡片。
 */
@Composable
internal fun BackupCard(
    uiState: BackupUiState,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    onManageConfig: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "☁️ 数据备份",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "将账本数据备份到阿里云 OSS，换机或重装时快速恢复。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(12.dp))

            // 配置状态
            if (!uiState.isConfigured) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.clickable(onClick = onManageConfig)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "️ 尚未配置 OSS",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "点击此处配置阿里云 OSS 参数",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                            )
                        }
                        Icon(
                            imageVector = Icons.Filled.Cloud,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            } else {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.clickable(onClick = onManageConfig)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "✅ OSS 已配置",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "点击此处修改或删除配置",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        Icon(
                            imageVector = Icons.Filled.Cloud,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 错误提示
            uiState.error?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // 备份进度
            if (uiState.isBackingUp) {
                Text(
                    text = "正在备份... ${(uiState.backupProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                androidx.compose.material3.LinearProgressIndicator(
                    progress = { uiState.backupProgress.toFloat() },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 恢复进度
            if (uiState.isRestoring) {
                Text(
                    text = "正在恢复... ${(uiState.restoreProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                androidx.compose.material3.LinearProgressIndicator(
                    progress = { uiState.restoreProgress.toFloat() },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 上次备份时间
            uiState.lastBackupTime?.let { time ->
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                Text(
                    text = "上次备份：${sdf.format(java.util.Date(time))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onRestore,
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isBackingUp && !uiState.isRestoring
                ) {
                    Text("恢复数据")
                }
                Button(
                    onClick = onBackup,
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isBackingUp && !uiState.isRestoring
                ) {
                    Text("立即备份")
                }
            }
        }
    }
}

/**
 * OSS 配置管理对话框
 */
@Composable
internal fun OSSConfigDialog(
    viewModel: BackupViewModel,
    onDismiss: () -> Unit
) {
    val form = viewModel.getConfigForm()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "️ OSS 配置",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // 模式切换
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = form.useDefaultConfig,
                        onClick = { viewModel.setUseDefaultConfig(true) },
                        label = { Text("使用默认配置") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = !form.useDefaultConfig,
                        onClick = { viewModel.setUseDefaultConfig(false) },
                        label = { Text("自定义配置") },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                if (form.useDefaultConfig) {
                    // 默认配置模式：只显示用户标识和密码（必填）
                    Text(
                        text = "使用内置阿里云 OSS 配置，只需填写用户标识和密码",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    OutlinedTextField(
                        value = form.userId,
                        onValueChange = { viewModel.updateConfigField("userId", it) },
                        label = { Text("用户标识 *") },
                        placeholder = { Text("手机号后4位") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = form.password,
                        onValueChange = { viewModel.updateConfigField("password", it) },
                        label = { Text("备份密码 *") },
                        placeholder = { Text("用于加密备份数据") },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    // 自定义配置模式：显示所有字段
                    OutlinedTextField(
                        value = form.endpoint,
                        onValueChange = { viewModel.updateConfigField("endpoint", it) },
                        label = { Text("Endpoint") },
                        placeholder = { Text("oss-cn-beijing.aliyuncs.com") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = form.bucketName,
                        onValueChange = { viewModel.updateConfigField("bucketName", it) },
                        label = { Text("Bucket Name") },
                        placeholder = { Text("haozhangben-backup-8865") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = form.accessKeyId,
                        onValueChange = { viewModel.updateConfigField("accessKeyId", it) },
                        label = { Text("AccessKey ID") },
                        placeholder = { Text("LTAI...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = form.accessKeySecret,
                        onValueChange = { viewModel.updateConfigField("accessKeySecret", it) },
                        label = { Text("AccessKey Secret") },
                        placeholder = { Text("••••••••••••") },
                        visualTransformation = VisualTransformation.None,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = form.userId,
                        onValueChange = { viewModel.updateConfigField("userId", it) },
                        label = { Text("用户标识（可选）") },
                        placeholder = { Text("手机号后4位，如：8865") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = form.password,
                        onValueChange = { viewModel.updateConfigField("password", it) },
                        label = { Text("备份密码（可选）") },
                        placeholder = { Text("不填则不加密") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { viewModel.saveConfig() }) {
                Text("保存")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { viewModel.clearConfig() }) {
                    Text("删除配置", color = MaterialTheme.colorScheme.error)
                }
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
            }
        }
    )
}

/**
 * 设置列表项
 */
@Composable
private fun SettingItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * 恢复密码输入对话框
 */
@Composable
internal fun RestorePasswordDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "输入恢复密码",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("备份密码") },
                placeholder = { Text("请输入备份时设置的密码") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(password) },
                enabled = password.isNotBlank()
            ) {
                Text("确认恢复")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
