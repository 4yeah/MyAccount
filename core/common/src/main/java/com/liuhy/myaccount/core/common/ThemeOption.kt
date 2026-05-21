/**
 * 应用主题选项枚举。
 *
 * 每个主题包含显示名称、emoji、亮色/暗色两套 ColorScheme。
 * 在 [ThemeSettingsScreen] 中供用户切换，全局通过 [ThemePreferences] 持久化。
 */
package com.liuhy.myaccount.core.common

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Material Default Colors
private val Purple80 = Color(0xFFD0BCFF)
private val PurpleGrey80 = Color(0xFFCCC2DC)
private val Pink80 = Color(0xFFEFB8C8)
private val Purple40 = Color(0xFF6650a4)
private val PurpleGrey40 = Color(0xFF625b71)
private val Pink40 = Color(0xFF7D5260)

// 草莓粉主题
private val StrawberryPrimary = Color(0xFFFF6B8A)
private val StrawberryOnPrimary = Color(0xFFFFFFFF)
private val StrawberryContainer = Color(0xFFFFD6E0)
private val StrawberrySurface = Color(0xFFFFF5F7)
private val StrawberryOnSurface = Color(0xFF4A2C33)

// 薄荷绿主题
private val MintPrimary = Color(0xFF4ECDC4)
private val MintOnPrimary = Color(0xFFFFFFFF)
private val MintContainer = Color(0xFFB2F5EA)
private val MintSurface = Color(0xFFF0FFF4)
private val MintOnSurface = Color(0xFF2C4A3E)

// 蓝莓紫主题
private val BlueberryPrimary = Color(0xFF845EC2)
private val BlueberryOnPrimary = Color(0xFFFFFFFF)
private val BlueberryContainer = Color(0xFFD6C3FF)
private val BlueberrySurface = Color(0xFFF8F4FF)
private val BlueberryOnSurface = Color(0xFF3A2C5C)

// 奶油黄主题
private val CreamPrimary = Color(0xFFFFC75F)
private val CreamOnPrimary = Color(0xFF4A3B2A)
private val CreamContainer = Color(0xFFFFE5B4)
private val CreamSurface = Color(0xFFFFFCF2)
private val CreamOnSurface = Color(0xFF4A3B2A)

enum class ThemeOption(
    val displayName: String,
    val emoji: String,
    val lightScheme: ColorScheme,
    val darkScheme: ColorScheme
) {
    MATERIAL(
        displayName = "默认",
        emoji = "🎨",
        lightScheme = lightColorScheme(
            primary = Purple40,
            onPrimary = Color.White,
            primaryContainer = Color(0xFFE8D5FF),
            secondary = PurpleGrey40,
            tertiary = Pink40,
            surface = Color(0xFFFFFBFE),
            onSurface = Color(0xFF1C1B1F),
            background = Color(0xFFFEF7FF)
        ),
        darkScheme = darkColorScheme(
            primary = Purple80,
            onPrimary = Color(0xFF36005C),
            primaryContainer = Color(0xFF4F378B),
            secondary = PurpleGrey80,
            tertiary = Pink80,
            surface = Color(0xFF141218),
            onSurface = Color(0xFFE6E0E9),
            background = Color(0xFF141218)
        )
    ),
    STRAWBERRY(
        displayName = "草莓粉",
        emoji = "🍓",
        lightScheme = lightColorScheme(
            primary = StrawberryPrimary,
            onPrimary = StrawberryOnPrimary,
            primaryContainer = StrawberryContainer,
            surface = StrawberrySurface,
            onSurface = StrawberryOnSurface
        ),
        darkScheme = darkColorScheme(
            primary = StrawberryPrimary,
            onPrimary = StrawberryOnPrimary,
            primaryContainer = StrawberryContainer,
            surface = Color(0xFF2A1A1F),
            onSurface = Color(0xFFFFB3C6)
        )
    ),
    MINT(
        displayName = "薄荷绿",
        emoji = "",
        lightScheme = lightColorScheme(
            primary = MintPrimary,
            onPrimary = MintOnPrimary,
            primaryContainer = MintContainer,
            surface = MintSurface,
            onSurface = MintOnSurface
        ),
        darkScheme = darkColorScheme(
            primary = MintPrimary,
            onPrimary = MintOnPrimary,
            primaryContainer = MintContainer,
            surface = Color(0xFF1A2A24),
            onSurface = Color(0xFFB2F5EA)
        )
    ),
    BLUEBERRY(
        displayName = "蓝莓紫",
        emoji = "",
        lightScheme = lightColorScheme(
            primary = BlueberryPrimary,
            onPrimary = BlueberryOnPrimary,
            primaryContainer = BlueberryContainer,
            surface = BlueberrySurface,
            onSurface = BlueberryOnSurface
        ),
        darkScheme = darkColorScheme(
            primary = BlueberryPrimary,
            onPrimary = BlueberryOnPrimary,
            primaryContainer = BlueberryContainer,
            surface = Color(0xFF1A1428),
            onSurface = Color(0xFFC9B3FF)
        )
    ),
    CREAM(
        displayName = "奶油黄",
        emoji = "",
        lightScheme = lightColorScheme(
            primary = CreamPrimary,
            onPrimary = CreamOnPrimary,
            primaryContainer = CreamContainer,
            surface = CreamSurface,
            onSurface = CreamOnSurface
        ),
        darkScheme = darkColorScheme(
            primary = CreamPrimary,
            onPrimary = CreamOnPrimary,
            primaryContainer = CreamContainer,
            surface = Color(0xFF2A2418),
            onSurface = Color(0xFFFFE5B4)
        )
    )
}
