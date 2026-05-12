package com.ifsvivek.nammahomestay.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = LeafGreenOnDark,
    onPrimary = Color(0xFF06280B),
    secondary = ClayBrownOnDark,
    onSecondary = Color(0xFF231811),
    tertiary = HarvestGoldOnDark,
    onTertiary = Color(0xFF2A1D00),
    background = NightBrown,
    onBackground = NightOnSurface,
    surface = NightSurface,
    onSurface = NightOnSurface,
    surfaceVariant = Color(0xFF3A2E25),
    onSurfaceVariant = Color(0xFFD8C9BB),
)

private val LightColorScheme = lightColorScheme(
    primary = LeafGreen,
    onPrimary = Color.White,
    primaryContainer = LeafGreenLight,
    onPrimaryContainer = LeafGreenDark,
    secondary = ClayBrown,
    onSecondary = Color.White,
    secondaryContainer = ClayBrownLight,
    onSecondaryContainer = ClayBrownDark,
    tertiary = HarvestGoldDark,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE69A),
    onTertiaryContainer = Color(0xFF3A2A00),
    background = Cream,
    onBackground = InkBrown,
    surface = CreamSurface,
    onSurface = InkBrown,
    surfaceVariant = ClayBrownLight,
    onSurfaceVariant = ClayBrownDark,
    outline = Color(0xFF8C7B6B),
)

@Composable
fun NammaHomeStayTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Earth-tone branding stays consistent for low-literacy users, so dynamic
    // (wallpaper-based) colour is OFF by default. Flip to true to opt in.
    dynamicColor: Boolean = false,
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
