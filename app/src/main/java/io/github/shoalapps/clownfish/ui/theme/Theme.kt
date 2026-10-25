@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package io.github.shoalapps.clownfish.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import io.github.shoalapps.clownfish.data.Settings

// Fallback palette: Clownfish's colour, generated with Material's colour utilities.
// Used on Android 11 and below, or with dynamic colour turned off.
private val Light = lightColorScheme(
    primary = Color(0xFF8E4D31), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDBCE), onPrimaryContainer = Color(0xFF71361C),
    secondary = Color(0xFF77574B), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFED4C4), onSecondaryContainer = Color(0xFF5D4035),
    tertiary = Color(0xFF7F7544), onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFF7F7544), onTertiaryContainer = Color(0xFFFFFFFF),
    background = Color(0xFFFFF8F6), onBackground = Color(0xFF231A16),
    surface = Color(0xFFFFF8F6), onSurface = Color(0xFF231A16), onSurfaceVariant = Color(0xFF53433E),
    surfaceContainerLow = Color(0xFFFFF1EC), surfaceContainer = Color(0xFFFCEAE4),
    surfaceContainerHigh = Color(0xFFF6E4DE), surfaceContainerHighest = Color(0xFFF1DFD9),
    outline = Color(0xFF83716A), outlineVariant = Color(0xFFD8C2BA), inversePrimary = Color(0xFFFFB598),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFFFB598), onPrimary = Color(0xFF552008),
    primaryContainer = Color(0xFF71361C), onPrimaryContainer = Color(0xFFFFDBCE),
    secondary = Color(0xFFE7BEAF), onSecondary = Color(0xFF442A20),
    secondaryContainer = Color(0xFF5D4035), onSecondaryContainer = Color(0xFFFFDBCE),
    tertiary = Color(0xFFD4C78E), onTertiary = Color(0xFF383006),
    tertiaryContainer = Color(0xFF9C915D), onTertiaryContainer = Color(0xFF000000),
    background = Color(0xFF1A110E), onBackground = Color(0xFFF1DFD9),
    surface = Color(0xFF1A110E), onSurface = Color(0xFFF1DFD9), onSurfaceVariant = Color(0xFFD8C2BA),
    surfaceContainerLow = Color(0xFF231A16), surfaceContainer = Color(0xFF271E1A),
    surfaceContainerHigh = Color(0xFF322824), surfaceContainerHighest = Color(0xFF3D322F),
    outline = Color(0xFFA08D86), outlineVariant = Color(0xFF53433E), inversePrimary = Color(0xFF8E4D31),
)

@Composable
fun TubeTheme(settings: Settings, content: @Composable () -> Unit) {
    val dark = when (settings.theme) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }
    val context = LocalContext.current
    val scheme = when {
        settings.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> Dark
        else -> Light
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }

    MaterialExpressiveTheme(
        colorScheme = scheme,
        motionScheme = MotionScheme.expressive(),
        content = content,
    )
}
