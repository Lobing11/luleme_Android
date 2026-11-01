package com.luleme.android.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class LulemeExtraColors(
    val heat: List<Color>,
    val card: Color,
    val border: Color,
    val danger: Color,
    val primarySoft: Color
)

val LocalExtraColors = staticCompositionLocalOf {
    LulemeExtraColors(
        heat = HeatLight,
        card = CardLight,
        border = BorderLight,
        danger = DangerLight,
        primarySoft = PinkPrimarySoftLight
    )
}

private val LightColors = lightColorScheme(
    primary = PinkPrimary,
    onPrimary = Color.White,
    secondary = PinkPrimarySoftLight,
    background = BgLight,
    onBackground = TextPrimaryLight,
    surface = CardLight,
    onSurface = TextPrimaryLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = BorderLight,
    error = DangerLight
)

private val DarkColors = darkColorScheme(
    primary = PinkPrimary,
    onPrimary = Color.White,
    secondary = PinkPrimarySoftDark,
    background = BgDark,
    onBackground = TextPrimaryDark,
    surface = CardDark,
    onSurface = TextPrimaryDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = BorderDark,
    error = DangerDark
)

@Composable
fun LulemeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val extras = if (darkTheme) {
        LulemeExtraColors(
            heat = HeatDark,
            card = CardDark,
            border = BorderDark,
            danger = DangerDark,
            primarySoft = PinkPrimarySoftDark
        )
    } else {
        LulemeExtraColors(
            heat = HeatLight,
            card = CardLight,
            border = BorderLight,
            danger = DangerLight,
            primarySoft = PinkPrimarySoftLight
        )
    }
    CompositionLocalProvider(LocalExtraColors provides extras) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            content = content
        )
    }
}
