package com.hnpage.ghinomobile.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext


private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,         // TopBar
    onPrimary = LightOnPrimary,     // Chữ trên TopBar
    surface = LightSurface,         // Nền
    onSurface = LightOnSurface,     // Chữ trên nền
    error = LightDebt,             // "Nợ"
    onError = LightOnDebt,         // Chữ trên "Nợ"
    secondary = LightCredit,       // "Có"
    onSecondary = LightOnCredit,   // Chữ trên "Có"
    tertiary = LightFab,           // FAB
    onTertiary = LightOnFab        // Chữ trên FAB
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    error = DarkDebt,
    onError = DarkOnDebt,
    secondary = DarkCredit,
    onSecondary = DarkOnCredit,
    tertiary = DarkFab,
    onTertiary = DarkOnFab
)

@Composable
fun GhinomobileTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}