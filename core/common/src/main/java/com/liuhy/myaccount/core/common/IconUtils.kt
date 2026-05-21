package com.liuhy.myaccount.core.common

import androidx.compose.ui.graphics.Color
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 根据图标名称获取 ImageVector
 */
@Composable
fun getIconVector(iconName: String): ImageVector = Icons.Default.run {
    when (iconName) {
        "restaurant" -> Restaurant
        "free_breakfast" -> FreeBreakfast
        "lunch_dining" -> LunchDining
        "dinner_dining" -> DinnerDining
        "cookie" -> Cookie
        "directions_car" -> DirectionsCar
        "directions_bus" -> DirectionsBus
        "local_taxi" -> LocalTaxi
        "local_gas_station" -> LocalGasStation
        "shopping_cart" -> ShoppingCart
        "shopping_bag" -> ShoppingBag
        "store" -> Store
        "movie" -> Movie
        "sports_esports" -> SportsEsports
        "pool" -> Pool
        "home" -> Home
        "hotel" -> Hotel
        "cleaning_services" -> CleaningServices
        "local_hospital" -> LocalHospital
        "pharmacy" -> LocalPharmacy
        "attach_money" -> AttachMoney
        "card_giftcard" -> CardGiftcard
        "trending_up" -> TrendingUp
        "work" -> Work
        "checkroom" -> Checkroom
        "devices" -> Devices
        // 交通出行
        "flight" -> Flight
        "train" -> Train
        "directions_bike" -> DirectionsBike
        "commute" -> Commute
        // 餐饮美食
        "local_cafe" -> LocalCafe
        "fastfood" -> Fastfood
        "local_bar" -> LocalBar
        "cake" -> Cake
        // 购物消费
        "local_mall" -> LocalMall
        "redeem" -> Redeem
        "local_offer" -> LocalOffer
        // 休闲娱乐
        "music_note" -> MusicNote
        "tv" -> Tv
        "menu_book" -> MenuBook
        "photo_camera" -> PhotoCamera
        "local_florist" -> LocalFlorist
        // 生活居家
        "pets" -> Pets
        "child_care" -> ChildCare
        "fitness_center" -> FitnessCenter
        "local_laundry_service" -> LocalLaundryService
        "wifi" -> Wifi
        "phone_android" -> PhoneAndroid
        "build" -> Build
        "brush" -> Brush
        "lightbulb" -> Lightbulb
        "emoji_objects" -> EmojiObjects
        // 教育医疗
        "school" -> School
        "health_and_safety" -> HealthAndSafety
        // 学习
        "assignment" -> Assignment
        // 工作
        "business" -> Business
        "business_center" -> BusinessCenter
        "computer" -> Computer
        "people" -> People
        "calendar_today" -> CalendarToday
        "schedule" -> Schedule
        "email" -> Email
        "folder" -> Folder
        "description" -> Description
        "print" -> Print
        "timer" -> Timer
        // 社交人际
        "person" -> Person
        "chat" -> Chat
        "forum" -> Forum
        "call" -> Call
        "contacts" -> Contacts
        "mood" -> Mood
        "thumb_up" -> ThumbUp
        "share" -> Share
        "celebration" -> Celebration
        // 金融理财
        "savings" -> Savings
        "account_balance" -> AccountBalance
        "credit_card" -> CreditCard
        "payments" -> Payments
        // 其他
        "favorite" -> Favorite
        "category" -> Category
        else -> Add
    }
}

/**
 * 将十六进制颜色字符串解析为 Compose Color
 * 支持格式: #FF5722, #2196F3 等
 */
fun parseColorHex(colorHex: String): Color {
    return Color(android.graphics.Color.parseColor(colorHex))
}
