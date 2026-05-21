/**
 * 图标选择对话框。
 *
 * 在添加/编辑分类时弹出，以网格形式展示所有可选的 Material 图标。
 * 点击图标后回调给上层更新选中的图标名称。
 */
package com.liuhy.myaccount.feature.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.DinnerDining
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FreeBreakfast
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalTaxi
import androidx.compose.material.icons.filled.LunchDining
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Commute
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.EmojiObjects
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.LocalLaundryService
import androidx.compose.material.icons.filled.LocalMall
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// 可用图标列表
val AVAILABLE_ICONS = listOf(
    "restaurant" to Icons.Default.Restaurant,
    "free_breakfast" to Icons.Default.FreeBreakfast,
    "lunch_dining" to Icons.Default.LunchDining,
    "dinner_dining" to Icons.Default.DinnerDining,
    "cookie" to Icons.Default.Cookie,
    "directions_car" to Icons.Default.DirectionsCar,
    "directions_bus" to Icons.Default.DirectionsBus,
    "local_taxi" to Icons.Default.LocalTaxi,
    "local_gas_station" to Icons.Default.LocalGasStation,
    "shopping_cart" to Icons.Default.ShoppingCart,
    "shopping_bag" to Icons.Default.ShoppingBag,
    "store" to Icons.Default.Store,
    "movie" to Icons.Default.Movie,
    "sports_esports" to Icons.Default.SportsEsports,
    "pool" to Icons.Default.Pool,
    "home" to Icons.Default.Home,
    "hotel" to Icons.Default.Hotel,
    "cleaning_services" to Icons.Default.CleaningServices,
    "local_hospital" to Icons.Default.LocalHospital,
    "pharmacy" to Icons.Default.LocalPharmacy,
    "attach_money" to Icons.Default.AttachMoney,
    "card_giftcard" to Icons.Default.CardGiftcard,
    "trending_up" to Icons.Default.TrendingUp,
    "work" to Icons.Default.Work,
    "checkroom" to Icons.Default.Checkroom,
    "devices" to Icons.Default.Devices,
    // 交通出行
    "flight" to Icons.Default.Flight,
    "train" to Icons.Default.Train,
    "directions_bike" to Icons.Default.DirectionsBike,
    "commute" to Icons.Default.Commute,
    // 餐饮美食
    "local_cafe" to Icons.Default.LocalCafe,
    "fastfood" to Icons.Default.Fastfood,
    "local_bar" to Icons.Default.LocalBar,
    "cake" to Icons.Default.Cake,
    // 购物消费
    "local_mall" to Icons.Default.LocalMall,
    "redeem" to Icons.Default.Redeem,
    "local_offer" to Icons.Default.LocalOffer,
    // 休闲娱乐
    "music_note" to Icons.Default.MusicNote,
    "tv" to Icons.Default.Tv,
    "menu_book" to Icons.Default.MenuBook,
    "photo_camera" to Icons.Default.PhotoCamera,
    "local_florist" to Icons.Default.LocalFlorist,
    // 生活居家
    "pets" to Icons.Default.Pets,
    "child_care" to Icons.Default.ChildCare,
    "fitness_center" to Icons.Default.FitnessCenter,
    "local_laundry_service" to Icons.Default.LocalLaundryService,
    "wifi" to Icons.Default.Wifi,
    "phone_android" to Icons.Default.PhoneAndroid,
    "build" to Icons.Default.Build,
    "brush" to Icons.Default.Brush,
    "lightbulb" to Icons.Default.Lightbulb,
    "emoji_objects" to Icons.Default.EmojiObjects,
    // 教育医疗
    "school" to Icons.Default.School,
    "health_and_safety" to Icons.Default.HealthAndSafety,
    // 学习
    "assignment" to Icons.Default.Assignment,
    // 工作
    "business" to Icons.Default.Business,
    "business_center" to Icons.Default.BusinessCenter,
    "computer" to Icons.Default.Computer,
    "people" to Icons.Default.People,
    "calendar_today" to Icons.Default.CalendarToday,
    "schedule" to Icons.Default.Schedule,
    "email" to Icons.Default.Email,
    "folder" to Icons.Default.Folder,
    "description" to Icons.Default.Description,
    "print" to Icons.Default.Print,
    "timer" to Icons.Default.Timer,
    // 社交人际
    "person" to Icons.Default.Person,
    "chat" to Icons.Default.Chat,
    "forum" to Icons.Default.Forum,
    "call" to Icons.Default.Call,
    "contacts" to Icons.Default.Contacts,
    "mood" to Icons.Default.Mood,
    "thumb_up" to Icons.Default.ThumbUp,
    "share" to Icons.Default.Share,
    "celebration" to Icons.Default.Celebration,
    // 金融理财
    "savings" to Icons.Default.Savings,
    "account_balance" to Icons.Default.AccountBalance,
    "credit_card" to Icons.Default.CreditCard,
    "payments" to Icons.Default.Payments,
    // 其他
    "favorite" to Icons.Default.Favorite,
    "category" to Icons.Default.Category,
    "add" to Icons.Default.Add
)

/**
 * 图标选择对话框
 * 
 * @param selectedIcon 当前选中的图标名称
 * @param onIconSelected 图标选中回调
 * @param onDismiss 关闭对话框回调
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun IconPickerDialog(
    selectedIcon: String,
    onIconSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var tempSelectedIcon by remember { mutableStateOf(selectedIcon) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "选择图标",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "点击选择一个图标",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AVAILABLE_ICONS.forEach { (iconName, iconVector) ->
                        IconItem(
                            icon = iconVector,
                            isSelected = tempSelectedIcon == iconName,
                            onClick = { tempSelectedIcon = iconName }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onIconSelected(tempSelectedIcon)
                    onDismiss()
                }
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

/**
 * 单个图标选项
 */
@Composable
private fun IconItem(
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        Color.Transparent
    }

    Box(
        modifier = Modifier
            .size(48.dp)
            .border(2.dp, borderColor, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.size(24.dp)
        )
    }
}
