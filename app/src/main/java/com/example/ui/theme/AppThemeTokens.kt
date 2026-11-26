package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class UiThemeStyle(
    val id: String,
    val title: String,
    val subtitle: String,
    val osBadge: String,
    val shapeSummary: String
) {
    IOS_27(
        id = "ios_27",
        title = "iOS 27",
        subtitle = "Liquid Glass • Frosted specular rims & continuous squircles",
        osBadge = "APPLE",
        shapeSummary = "Squircle 22dp • Liquid Glass"
    ),
    ANDROID_17(
        id = "android_17",
        title = "Android 17",
        subtitle = "System Dynamic • Tonal depth & adaptive Material containers",
        osBadge = "AOSP",
        shapeSummary = "Tonal 18dp • Dynamic Accent"
    ),
    HYPER_OS_4(
        id = "hyperos_4",
        title = "HyperOS 4",
        subtitle = "Spatial Alive • Floating depth cards & mint bezel glow",
        osBadge = "XIAOMI",
        shapeSummary = "Spatial 26dp • Floating Depth"
    ),
    COLOR_OS_17(
        id = "coloros_17",
        title = "ColorOS 17",
        subtitle = "Aquamorphic • Midnight teal surfaces & full fluid capsules",
        osBadge = "OPPO",
        shapeSummary = "Fluid 24dp • Stadium Pills"
    ),
    M3_EXPRESSIVE(
        id = "m3_expressive",
        title = "M3 Expressive",
        subtitle = "Expressive Shapes • Asymmetric organic corners & bold pills",
        osBadge = "M3E",
        shapeSummary = "Asymmetric 26/10dp • Pill CTA"
    ),
    OLED(
        id = "oled",
        title = "OLED Obsidian",
        subtitle = "True #000000 • 0-nit pixel-off AMOLED canvas & phosphor hairlines",
        osBadge = "AMOLED",
        shapeSummary = "Razor 16dp • 0-Nit Pure Black"
    );

    companion object {
        fun fromId(id: String): UiThemeStyle =
            entries.firstOrNull { it.id == id } ?: M3_EXPRESSIVE
    }
}

enum class ThemeAccentPalette(
    val id: String,
    val label: String,
    val primary: Color?,
    val bright: Color?,
    val glow: Color?,
    val tintBg: Color?,
    val tertiary: Color?
) {
    OS_DEFAULT("os_default", "OS Native", null, null, null, null, null),
    PROTECTSTAR_MINT(
        id = "mint",
        label = "Mint",
        primary = Color(0xFF3FB950),
        bright = Color(0xFF56E368),
        glow = Color(0xFF9BFFAA),
        tintBg = Color(0xFF0E2914),
        tertiary = Color(0xFF4DD0E1)
    ),
    CYBER_CYAN(
        id = "cyan",
        label = "Cyan",
        primary = Color(0xFF00A3BF),
        bright = Color(0xFF22DDF8),
        glow = Color(0xFF8CF3FF),
        tintBg = Color(0xFF0A262E),
        tertiary = Color(0xFF69F0AE)
    ),
    QUANTUM_VIOLET(
        id = "violet",
        label = "Quantum",
        primary = Color(0xFF7B52FF),
        bright = Color(0xFFA084FF),
        glow = Color(0xFFD2C4FF),
        tintBg = Color(0xFF1D1438),
        tertiary = Color(0xFF40E0D0)
    ),
    TITANIUM_GOLD(
        id = "gold",
        label = "Amber",
        primary = Color(0xFFD89614),
        bright = Color(0xFFFFBC38),
        glow = Color(0xFFFFE094),
        tintBg = Color(0xFF2E2209),
        tertiary = Color(0xFF64B5F6)
    ),
    CRIMSON_OPS(
        id = "crimson",
        label = "Crimson",
        primary = Color(0xFFD93F3C),
        bright = Color(0xFFFF6663),
        glow = Color(0xFFFFAAA8),
        tintBg = Color(0xFF331315),
        tertiary = Color(0xFFFFB74D)
    );

    companion object {
        fun fromId(id: String): ThemeAccentPalette =
            entries.firstOrNull { it.id == id } ?: OS_DEFAULT
    }
}

@Immutable
data class AppThemeSpec(
    val style: UiThemeStyle,
    val bgBase: Color,
    val bgDeep: Color,
    val bgGradientTop: Color,
    val bgGradientBottom: Color,
    val surfaceCard: Color,
    val surfaceElevated: Color,
    val surfaceDivider: Color,
    val accentPrimary: Color,
    val accentBright: Color,
    val accentGlow: Color,
    val accentTintBg: Color,
    val tertiaryAccent: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val danger: Color,
    val dangerTint: Color,
    val warning: Color,
    val cardBorderWidth: Dp,
    val cardBorderColor: Color,
    val glassHighlightColor: Color,
    val cardShape: Shape,
    val tileShape: Shape,
    val buttonShape: Shape,
    val pillShape: Shape,
    val dialogShape: Shape,
    val stepperShape: Shape,
    val badgeShape: Shape,
    val rowTopShape: Shape,
    val rowBottomShape: Shape,
    val tonalElevation: Dp,
    val shadowElevation: Dp,
    val isOledBlack: Boolean = false,
    val hasSpecularRim: Boolean = false,
    val hasSpatialGlow: Boolean = false,
    val chromeStyleLabel: String = "Standard"
)

object AppThemePresets {
    fun specFor(style: UiThemeStyle): AppThemeSpec = when (style) {
        UiThemeStyle.IOS_27 -> AppThemeSpec(
            style = UiThemeStyle.IOS_27,
            bgBase = Color(0xFF0E1118),
            bgDeep = Color(0xFF07090D),
            bgGradientTop = Color(0xFF141A26),
            bgGradientBottom = Color(0xFF090B10),
            surfaceCard = Color(0xFF1B212E),
            surfaceElevated = Color(0xFF252D3E),
            surfaceDivider = Color(0xFF394358),
            accentPrimary = Color(0xFF28B94E),
            accentBright = Color(0xFF34D760),
            accentGlow = Color(0xFF70F092),
            accentTintBg = Color(0xFF172F22),
            tertiaryAccent = Color(0xFF64B5F6),
            textPrimary = Color(0xFFF5F7FA),
            textSecondary = Color(0xFFA6B0C2),
            textMuted = Color(0xFF778296),
            danger = Color(0xFFFF5E5B),
            dangerTint = Color(0xFF3A1D22),
            warning = Color(0xFFFFB74D),
            cardBorderWidth = 1.dp,
            cardBorderColor = Color.White.copy(alpha = 0.18f),
            glassHighlightColor = Color.White.copy(alpha = 0.08f),
            cardShape = RoundedCornerShape(22.dp),
            tileShape = RoundedCornerShape(24.dp),
            buttonShape = RoundedCornerShape(22.dp),
            pillShape = RoundedCornerShape(50),
            dialogShape = RoundedCornerShape(28.dp),
            stepperShape = RoundedCornerShape(18.dp),
            badgeShape = RoundedCornerShape(8.dp),
            rowTopShape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
            rowBottomShape = RoundedCornerShape(bottomStart = 22.dp, bottomEnd = 22.dp),
            tonalElevation = 2.dp,
            shadowElevation = 8.dp,
            isOledBlack = false,
            hasSpecularRim = true,
            hasSpatialGlow = false,
            chromeStyleLabel = "Liquid Glass"
        )

        UiThemeStyle.ANDROID_17 -> AppThemeSpec(
            style = UiThemeStyle.ANDROID_17,
            bgBase = Color(0xFF1C2024),
            bgDeep = Color(0xFF14171A),
            bgGradientTop = Color(0xFF21262B),
            bgGradientBottom = Color(0xFF191D20),
            surfaceCard = Color(0xFF2B3036),
            surfaceElevated = Color(0xFF353B42),
            surfaceDivider = Color(0xFF474F58),
            accentPrimary = Color(0xFF5A8D23),
            accentBright = Color(0xFF72B02D),
            accentGlow = Color(0xFF8EE03B),
            accentTintBg = Color(0xFF2B3926),
            tertiaryAccent = Color(0xFF7CB7FF),
            textPrimary = Color(0xFFF3F5F7),
            textSecondary = Color(0xFFA3ABB3),
            textMuted = Color(0xFF7B838C),
            danger = Color(0xFFE05A5A),
            dangerTint = Color(0xFF3E292B),
            warning = Color(0xFFE6A23C),
            cardBorderWidth = 1.dp,
            cardBorderColor = Color(0xFF474F58).copy(alpha = 0.48f),
            glassHighlightColor = Color.White.copy(alpha = 0.03f),
            cardShape = RoundedCornerShape(18.dp),
            tileShape = RoundedCornerShape(18.dp),
            buttonShape = RoundedCornerShape(16.dp),
            pillShape = RoundedCornerShape(50),
            dialogShape = RoundedCornerShape(24.dp),
            stepperShape = RoundedCornerShape(14.dp),
            badgeShape = RoundedCornerShape(6.dp),
            rowTopShape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
            rowBottomShape = RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp),
            tonalElevation = 4.dp,
            shadowElevation = 4.dp,
            isOledBlack = false,
            hasSpecularRim = false,
            hasSpatialGlow = false,
            chromeStyleLabel = "Material Tonal"
        )

        UiThemeStyle.HYPER_OS_4 -> AppThemeSpec(
            style = UiThemeStyle.HYPER_OS_4,
            bgBase = Color(0xFF11141B),
            bgDeep = Color(0xFF0A0C11),
            bgGradientTop = Color(0xFF161C28),
            bgGradientBottom = Color(0xFF0D1017),
            surfaceCard = Color(0xFF1E2430),
            surfaceElevated = Color(0xFF283040),
            surfaceDivider = Color(0xFF3A4459),
            accentPrimary = Color(0xFF00B86B),
            accentBright = Color(0xFF19DA87),
            accentGlow = Color(0xFF6BF5B7),
            accentTintBg = Color(0xFF123226),
            tertiaryAccent = Color(0xFF54A8FF),
            textPrimary = Color(0xFFF4F7FB),
            textSecondary = Color(0xFFA0ADC0),
            textMuted = Color(0xFF727F93),
            danger = Color(0xFFF25C5C),
            dangerTint = Color(0xFF3B1F24),
            warning = Color(0xFFF5A623),
            cardBorderWidth = 1.dp,
            cardBorderColor = Color(0xFF5B76A3).copy(alpha = 0.28f),
            glassHighlightColor = Color(0xFF77B7FF).copy(alpha = 0.05f),
            cardShape = RoundedCornerShape(26.dp),
            tileShape = RoundedCornerShape(26.dp),
            buttonShape = RoundedCornerShape(20.dp),
            pillShape = RoundedCornerShape(50),
            dialogShape = RoundedCornerShape(30.dp),
            stepperShape = RoundedCornerShape(20.dp),
            badgeShape = RoundedCornerShape(10.dp),
            rowTopShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            rowBottomShape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
            tonalElevation = 3.dp,
            shadowElevation = 6.dp,
            isOledBlack = false,
            hasSpecularRim = true,
            hasSpatialGlow = true,
            chromeStyleLabel = "Spatial Glass"
        )

        UiThemeStyle.COLOR_OS_17 -> AppThemeSpec(
            style = UiThemeStyle.COLOR_OS_17,
            bgBase = Color(0xFF0E181C),
            bgDeep = Color(0xFF080F12),
            bgGradientTop = Color(0xFF14252C),
            bgGradientBottom = Color(0xFF0A1317),
            surfaceCard = Color(0xFF182A31),
            surfaceElevated = Color(0xFF213740),
            surfaceDivider = Color(0xFF314D58),
            accentPrimary = Color(0xFF05AD80),
            accentBright = Color(0xFF1FD4A2),
            accentGlow = Color(0xFF6EF5CE),
            accentTintBg = Color(0xFF10332D),
            tertiaryAccent = Color(0xFF4DD0E1),
            textPrimary = Color(0xFFF1F8FA),
            textSecondary = Color(0xFF9EBBC4),
            textMuted = Color(0xFF6F8C96),
            danger = Color(0xFFEB6060),
            dangerTint = Color(0xFF3B2226),
            warning = Color(0xFFEFAC41),
            cardBorderWidth = 1.dp,
            cardBorderColor = Color(0xFF45D9C4).copy(alpha = 0.24f),
            glassHighlightColor = Color(0xFF3BE3C6).copy(alpha = 0.05f),
            cardShape = RoundedCornerShape(24.dp),
            tileShape = RoundedCornerShape(28.dp),
            buttonShape = RoundedCornerShape(50),
            pillShape = RoundedCornerShape(50),
            dialogShape = RoundedCornerShape(28.dp),
            stepperShape = RoundedCornerShape(50),
            badgeShape = RoundedCornerShape(50),
            rowTopShape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
            rowBottomShape = RoundedCornerShape(bottomStart = 22.dp, bottomEnd = 22.dp),
            tonalElevation = 4.dp,
            shadowElevation = 6.dp,
            isOledBlack = false,
            hasSpecularRim = false,
            hasSpatialGlow = true,
            chromeStyleLabel = "Aquamorphic"
        )

        UiThemeStyle.M3_EXPRESSIVE -> AppThemeSpec(
            style = UiThemeStyle.M3_EXPRESSIVE,
            bgBase = Color(0xFF212529),
            bgDeep = Color(0xFF181B1E),
            bgGradientTop = Color(0xFF282D32),
            bgGradientBottom = Color(0xFF1D2124),
            surfaceCard = Color(0xFF31373D),
            surfaceElevated = Color(0xFF3B424A),
            surfaceDivider = Color(0xFF4D5660),
            accentPrimary = Color(0xFF5D9423),
            accentBright = Color(0xFF79BC30),
            accentGlow = Color(0xFF9AF042),
            accentTintBg = Color(0xFF2E3B27),
            tertiaryAccent = Color(0xFF7CB7FF),
            textPrimary = Color(0xFFF3F5F7),
            textSecondary = Color(0xFFA2ABB3),
            textMuted = Color(0xFF7B838D),
            danger = Color(0xFFE05A5A),
            dangerTint = Color(0xFF3E292B),
            warning = Color(0xFFE6A23C),
            cardBorderWidth = 1.1.dp,
            cardBorderColor = Color(0xFF79BC30).copy(alpha = 0.30f),
            glassHighlightColor = Color(0xFF79BC30).copy(alpha = 0.04f),
            cardShape = RoundedCornerShape(topStart = 26.dp, topEnd = 10.dp, bottomStart = 10.dp, bottomEnd = 26.dp),
            tileShape = RoundedCornerShape(topStart = 28.dp, topEnd = 12.dp, bottomStart = 12.dp, bottomEnd = 28.dp),
            buttonShape = RoundedCornerShape(50),
            pillShape = RoundedCornerShape(50),
            dialogShape = RoundedCornerShape(topStart = 30.dp, topEnd = 14.dp, bottomStart = 14.dp, bottomEnd = 30.dp),
            stepperShape = RoundedCornerShape(50),
            badgeShape = RoundedCornerShape(topStart = 12.dp, bottomEnd = 12.dp, topEnd = 4.dp, bottomStart = 4.dp),
            rowTopShape = RoundedCornerShape(topStart = 22.dp, topEnd = 10.dp),
            rowBottomShape = RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 22.dp),
            tonalElevation = 4.dp,
            shadowElevation = 6.dp,
            isOledBlack = false,
            hasSpecularRim = false,
            hasSpatialGlow = false,
            chromeStyleLabel = "M3 Asymmetric"
        )

        UiThemeStyle.OLED -> AppThemeSpec(
            style = UiThemeStyle.OLED,
            bgBase = Color(0xFF000000),
            bgDeep = Color(0xFF000000),
            bgGradientTop = Color(0xFF000000),
            bgGradientBottom = Color(0xFF000000),
            surfaceCard = Color(0xFF08090C),
            surfaceElevated = Color(0xFF111319),
            surfaceDivider = Color(0xFF252A34),
            accentPrimary = Color(0xFF00C853),
            accentBright = Color(0xFF00F076),
            accentGlow = Color(0xFF7CFFB4),
            accentTintBg = Color(0xFF071E11),
            tertiaryAccent = Color(0xFF00E5FF),
            textPrimary = Color(0xFFFFFFFF),
            textSecondary = Color(0xFFA0A8B8),
            textMuted = Color(0xFF6B7382),
            danger = Color(0xFFFF4D4D),
            dangerTint = Color(0xFF24090B),
            warning = Color(0xFFFFB300),
            cardBorderWidth = 0.9.dp,
            cardBorderColor = Color(0xFF282E3A),
            glassHighlightColor = Color.Transparent,
            cardShape = RoundedCornerShape(16.dp),
            tileShape = RoundedCornerShape(18.dp),
            buttonShape = RoundedCornerShape(14.dp),
            pillShape = RoundedCornerShape(50),
            dialogShape = RoundedCornerShape(22.dp),
            stepperShape = RoundedCornerShape(14.dp),
            badgeShape = RoundedCornerShape(6.dp),
            rowTopShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            rowBottomShape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            isOledBlack = true,
            hasSpecularRim = false,
            hasSpatialGlow = false,
            chromeStyleLabel = "AMOLED 0-Nit"
        )
    }

    fun resolveSpec(
        style: UiThemeStyle,
        oledEnabled: Boolean = false,
        accentId: String = "os_default"
    ): AppThemeSpec {
        var spec = specFor(style)
        val palette = ThemeAccentPalette.fromId(accentId)
        if (palette != ThemeAccentPalette.OS_DEFAULT &&
            palette.primary != null &&
            palette.bright != null &&
            palette.glow != null &&
            palette.tintBg != null &&
            palette.tertiary != null
        ) {
            spec = spec.copy(
                accentPrimary = palette.primary,
                accentBright = palette.bright,
                accentGlow = palette.glow,
                accentTintBg = palette.tintBg,
                tertiaryAccent = palette.tertiary
            )
        }
        val forceOled = oledEnabled || style == UiThemeStyle.OLED
        if (forceOled) {
            spec = spec.copy(
                bgBase = Color(0xFF000000),
                bgDeep = Color(0xFF000000),
                bgGradientTop = Color(0xFF000000),
                bgGradientBottom = Color(0xFF000000),
                surfaceCard = Color(0xFF08090B),
                surfaceElevated = Color(0xFF111318),
                surfaceDivider = Color(0xFF232730),
                accentTintBg = spec.accentPrimary.copy(alpha = 0.14f),
                dangerTint = spec.danger.copy(alpha = 0.14f),
                cardBorderWidth = 0.9.dp,
                cardBorderColor = if (style == UiThemeStyle.IOS_27 || style == UiThemeStyle.HYPER_OS_4) {
                    Color.White.copy(alpha = 0.16f)
                } else {
                    spec.accentBright.copy(alpha = 0.24f)
                },
                glassHighlightColor = if (spec.hasSpecularRim) Color.White.copy(alpha = 0.03f) else Color.Transparent,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                isOledBlack = true
            )
        }
        return spec
    }
}

val LocalAppThemeSpec = staticCompositionLocalOf { AppThemePresets.specFor(UiThemeStyle.M3_EXPRESSIVE) }
