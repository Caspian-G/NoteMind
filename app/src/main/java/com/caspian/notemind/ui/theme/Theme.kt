package com.caspian.notemind.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val NoteMindColors = lightColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    primaryContainer = PanelDeep,
    onPrimaryContainer = Ink,
    secondary = InkSoft,
    onSecondary = Color.White,
    secondaryContainer = Panel,
    onSecondaryContainer = Ink,
    tertiary = InkSoft,
    onTertiary = Color.White,
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = Panel,
    onSurfaceVariant = InkSoft,
    surfaceContainer = PaperDim,
    surfaceContainerHigh = Panel,
    surfaceContainerLow = PaperDim,
    surfaceContainerLowest = Paper,
    outline = Hairline,
    outlineVariant = HairlineSoft,
    error = AlertRed,
    onError = Color.White,
    scrim = Color(0x33000000)
)

/**
 * 界面固定使用浅色白灰配色，不跟随系统深色模式，也不启用动态取色，
 * 以保证三个模块的观感完全一致。
 */
@Composable
fun NoteMindTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NoteMindColors,
        typography = Typography,
        shapes = NoteMindShapes,
        content = content
    )
}
