package com.example.ui.theme

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

val IShredderShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
fun IShredderTheme(
    themeStyle: UiThemeStyle = UiThemeStyle.M3_EXPRESSIVE,
    dynamicColorEnabled: Boolean = false,
    oledModeEnabled: Boolean = false,
    accentId: String = "os_default",
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val baseSpec = remember(themeStyle, oledModeEnabled, accentId) {
        AppThemePresets.resolveSpec(
            style = themeStyle,
            oledEnabled = oledModeEnabled,
            accentId = accentId
        )
    }

    val resolvedSpec = if (
        dynamicColorEnabled &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
        (themeStyle == UiThemeStyle.ANDROID_17 || themeStyle == UiThemeStyle.M3_EXPRESSIVE)
    ) {
        val dynamic = dynamicDarkColorScheme(context)
        if (baseSpec.isOledBlack) {
            baseSpec.copy(
                accentPrimary = dynamic.primary,
                accentBright = dynamic.primary,
                accentGlow = dynamic.tertiary,
                accentTintBg = dynamic.primary.copy(alpha = 0.14f),
                tertiaryAccent = dynamic.secondary,
                cardBorderColor = dynamic.primary.copy(alpha = 0.28f)
            )
        } else {
            baseSpec.copy(
                bgBase = dynamic.background,
                bgGradientTop = dynamic.surfaceContainerLow,
                bgGradientBottom = dynamic.background,
                surfaceCard = dynamic.surfaceContainer,
                surfaceElevated = dynamic.surfaceContainerHigh,
                surfaceDivider = dynamic.outlineVariant,
                accentPrimary = dynamic.primary,
                accentBright = dynamic.primary,
                accentGlow = dynamic.tertiary,
                accentTintBg = dynamic.primaryContainer,
                tertiaryAccent = dynamic.secondary,
                cardBorderColor = dynamic.outline.copy(alpha = 0.45f)
            )
        }
    } else {
        baseSpec
    }

    val animBg by animateColorAsState(resolvedSpec.bgBase, tween(240), label = "theme_bg")
    val animGradTop by animateColorAsState(resolvedSpec.bgGradientTop, tween(240), label = "theme_gtop")
    val animGradBottom by animateColorAsState(resolvedSpec.bgGradientBottom, tween(240), label = "theme_gbot")
    val animCard by animateColorAsState(resolvedSpec.surfaceCard, tween(240), label = "theme_card")
    val animElevated by animateColorAsState(resolvedSpec.surfaceElevated, tween(240), label = "theme_elev")
    val animPrimary by animateColorAsState(resolvedSpec.accentPrimary, tween(240), label = "theme_pri")
    val animBright by animateColorAsState(resolvedSpec.accentBright, tween(240), label = "theme_bri")
    val animTint by animateColorAsState(resolvedSpec.accentTintBg, tween(240), label = "theme_tint")

    val animatedSpec = resolvedSpec.copy(
        bgBase = animBg,
        bgGradientTop = animGradTop,
        bgGradientBottom = animGradBottom,
        surfaceCard = animCard,
        surfaceElevated = animElevated,
        accentPrimary = animPrimary,
        accentBright = animBright,
        accentTintBg = animTint
    )

    val colorScheme = darkColorScheme(
        primary = animatedSpec.accentPrimary,
        onPrimary = Color.White,
        primaryContainer = animatedSpec.accentTintBg,
        onPrimaryContainer = animatedSpec.accentGlow,
        secondary = animatedSpec.accentBright,
        onSecondary = Color.White,
        secondaryContainer = animatedSpec.surfaceElevated,
        onSecondaryContainer = animatedSpec.textPrimary,
        tertiary = animatedSpec.tertiaryAccent,
        onTertiary = animatedSpec.bgDeep,
        background = animatedSpec.bgBase,
        onBackground = animatedSpec.textPrimary,
        surface = animatedSpec.surfaceCard,
        onSurface = animatedSpec.textPrimary,
        surfaceVariant = animatedSpec.surfaceElevated,
        onSurfaceVariant = animatedSpec.textSecondary,
        outline = animatedSpec.surfaceDivider,
        outlineVariant = animatedSpec.cardBorderColor,
        error = animatedSpec.danger,
        onError = Color.White,
        errorContainer = animatedSpec.dangerTint,
        onErrorContainer = Color(0xFFFFB4AB)
    )

    val m3Shapes = remember(themeStyle) {
        when (themeStyle) {
            UiThemeStyle.IOS_27 -> Shapes(
                extraSmall = RoundedCornerShape(10.dp),
                small = RoundedCornerShape(14.dp),
                medium = RoundedCornerShape(22.dp),
                large = RoundedCornerShape(26.dp),
                extraLarge = RoundedCornerShape(28.dp)
            )
            UiThemeStyle.ANDROID_17 -> Shapes(
                extraSmall = RoundedCornerShape(8.dp),
                small = RoundedCornerShape(12.dp),
                medium = RoundedCornerShape(18.dp),
                large = RoundedCornerShape(22.dp),
                extraLarge = RoundedCornerShape(24.dp)
            )
            UiThemeStyle.HYPER_OS_4 -> Shapes(
                extraSmall = RoundedCornerShape(12.dp),
                small = RoundedCornerShape(16.dp),
                medium = RoundedCornerShape(26.dp),
                large = RoundedCornerShape(28.dp),
                extraLarge = RoundedCornerShape(30.dp)
            )
            UiThemeStyle.COLOR_OS_17 -> Shapes(
                extraSmall = RoundedCornerShape(12.dp),
                small = RoundedCornerShape(18.dp),
                medium = RoundedCornerShape(24.dp),
                large = RoundedCornerShape(28.dp),
                extraLarge = RoundedCornerShape(32.dp)
            )
            UiThemeStyle.M3_EXPRESSIVE -> Shapes(
                extraSmall = RoundedCornerShape(topStart = 12.dp, bottomEnd = 12.dp, topEnd = 4.dp, bottomStart = 4.dp),
                small = RoundedCornerShape(topStart = 16.dp, bottomEnd = 16.dp, topEnd = 8.dp, bottomStart = 8.dp),
                medium = RoundedCornerShape(topStart = 26.dp, topEnd = 10.dp, bottomStart = 10.dp, bottomEnd = 26.dp),
                large = RoundedCornerShape(topStart = 28.dp, topEnd = 12.dp, bottomStart = 12.dp, bottomEnd = 28.dp),
                extraLarge = RoundedCornerShape(topStart = 30.dp, topEnd = 14.dp, bottomStart = 14.dp, bottomEnd = 30.dp)
            )
            UiThemeStyle.OLED -> Shapes(
                extraSmall = RoundedCornerShape(6.dp),
                small = RoundedCornerShape(12.dp),
                medium = RoundedCornerShape(16.dp),
                large = RoundedCornerShape(18.dp),
                extraLarge = RoundedCornerShape(22.dp)
            )
        }
    }

    CompositionLocalProvider(LocalAppThemeSpec provides animatedSpec) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = m3Shapes,
            content = content
        )
    }
}
