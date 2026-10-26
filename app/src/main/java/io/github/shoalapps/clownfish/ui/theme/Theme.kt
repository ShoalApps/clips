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
    primary = Color(0xFFAB3142), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDADA), onPrimaryContainer = Color(0xFF8A172C),
    secondary = Color(0xFF765657), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFBD0D1), onSecondaryContainer = Color(0xFF5D3F40),
    tertiary = Color(0xFF8E6F43), onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFF8E6F43), onTertiaryContainer = Color(0xFFFFFFFF),
    background = Color(0xFFFFF8F7), onBackground = Color(0xFF22191A),
    surface = Color(0xFFFFF8F7), onSurface = Color(0xFF22191A), onSurfaceVariant = Color(0xFF524343),
    surfaceContainerLow = Color(0xFFFFF0F0), surfaceContainer = Color(0xFFFCEAE9),
    surfaceContainerHigh = Color(0xFFF6E4E4), surfaceContainerHighest = Color(0xFFF0DEDE),
    outline = Color(0xFF827071), outlineVariant = Color(0xFFD7C1C1), inversePrimary = Color(0xFFFFB3B6),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFFFB3B6), onPrimary = Color(0xFF620D1D),
    primaryContainer = Color(0xFF8A172C), onPrimaryContainer = Color(0xFFFFDADA),
    secondary = Color(0xFFE6BDBD), onSecondary = Color(0xFF44292A),
    secondaryContainer = Color(0xFF5D3F40), onSecondaryContainer = Color(0xFFFFDADA),
    tertiary = Color(0xFFE6C08D), onTertiary = Color(0xFF432C06),
    tertiaryContainer = Color(0xFFAD8B5C), onTertiaryContainer = Color(0xFF000000),
    background = Color(0xFF1A1111), onBackground = Color(0xFFF0DEDE),
    surface = Color(0xFF1A1111), onSurface = Color(0xFFF0DEDE), onSurfaceVariant = Color(0xFFD7C1C1),
    surfaceContainerLow = Color(0xFF22191A), surfaceContainer = Color(0xFF271D1D),
    surfaceContainerHigh = Color(0xFF322828), surfaceContainerHighest = Color(0xFF3D3232),
    outline = Color(0xFF9F8C8C), outlineVariant = Color(0xFF524343), inversePrimary = Color(0xFFAB3142),
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
