package com.example.ui.screens

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness2
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppSettings
import com.example.ui.components.StandardTitleTopBar
import com.example.ui.components.themedCardBorder
import com.example.ui.components.themedScreenBackground
import com.example.ui.theme.AppThemePresets
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalAppThemeSpec
import com.example.ui.theme.ThemeAccentPalette
import com.example.ui.theme.UiThemeStyle

@Composable
fun ThemeUiScreen(
    settings: AppSettings,
    onSelectThemeStyle: (String) -> Unit,
    onSetOledMode: (Boolean) -> Unit,
    onSelectAccentId: (String) -> Unit,
    onSetDynamicColor: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    val activeSpec = LocalAppThemeSpec.current
    val activeStyle = activeSpec.style
    val supportsDynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    Column(
        modifier = Modifier
            .fillMaxSize()
            .themedScreenBackground()
            .navigationBarsPadding()
    ) {
        StandardTitleTopBar(title = "Theme UI Studio", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Live Active Theme + OLED Studio Preview Card
            Surface(
                shape = activeSpec.cardShape,
                color = if (activeSpec.isOledBlack) activeSpec.surfaceCard else activeSpec.accentTintBg,
                border = themedCardBorder(accentFocused = true),
                tonalElevation = activeSpec.tonalElevation,
                shadowElevation = activeSpec.shadowElevation,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.ColorLens,
                                contentDescription = null,
                                tint = activeSpec.accentBright,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = buildString {
                                        append(activeStyle.title)
                                        if (activeSpec.isOledBlack && activeStyle != UiThemeStyle.OLED) {
                                            append(" + OLED")
                                        }
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = activeSpec.textPrimary
                                )
                                Text(
                                    text = "${activeSpec.chromeStyleLabel} • ${activeStyle.shapeSummary}",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 10.5.sp,
                                    color = activeSpec.accentBright
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (activeSpec.isOledBlack) {
                                Surface(
                                    shape = activeSpec.badgeShape,
                                    color = Color.Black,
                                    border = BorderStroke(1.dp, activeSpec.accentBright)
                                ) {
                                    Text(
                                        text = "#000000",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = activeSpec.accentBright,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            Surface(
                                shape = activeSpec.badgeShape,
                                color = activeSpec.accentPrimary
                            ) {
                                Text(
                                    text = activeStyle.osBadge,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Live Component Shape & Telemetry Preview Strip
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = activeSpec.tileShape,
                            color = activeSpec.surfaceElevated,
                            border = themedCardBorder(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Security,
                                    contentDescription = null,
                                    tint = activeSpec.accentBright,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Q-Shred 2026 • 6 Passes",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 11.sp,
                                    color = activeSpec.textPrimary
                                )
                            }
                        }

                        Button(
                            onClick = {},
                            shape = activeSpec.buttonShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = activeSpec.accentPrimary,
                                contentColor = Color.White
                            ),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text("SHRED", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Pure OLED Black (#000000) Master Switch
            Surface(
                shape = activeSpec.cardShape,
                color = activeSpec.surfaceCard,
                border = themedCardBorder(accentFocused = settings.oledModeEnabled || activeStyle == UiThemeStyle.OLED),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (activeStyle != UiThemeStyle.OLED) {
                                onSetOledMode(!settings.oledModeEnabled)
                            }
                        }
                        .padding(horizontal = 18.dp, vertical = 14.dp)
                        .testTag("theme_oled_toggle_row"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = activeSpec.badgeShape,
                        color = Color.Black,
                        border = BorderStroke(1.dp, activeSpec.accentBright.copy(alpha = 0.7f)),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Brightness2,
                            contentDescription = null,
                            tint = activeSpec.accentBright,
                            modifier = Modifier.padding(9.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Pure OLED Black (#000000)",
                            style = MaterialTheme.typography.titleMedium,
                            color = activeSpec.textPrimary
                        )
                        Text(
                            text = if (activeStyle == UiThemeStyle.OLED) {
                                "Always active on OLED Obsidian preset (0-nit pixel-off)"
                            } else {
                                "Turns canvas pitch black while preserving ${activeStyle.title} shapes"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = activeSpec.textSecondary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Switch(
                        checked = settings.oledModeEnabled || activeStyle == UiThemeStyle.OLED,
                        enabled = activeStyle != UiThemeStyle.OLED,
                        onCheckedChange = onSetOledMode,
                        modifier = Modifier.testTag("theme_oled_switch"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = activeSpec.accentPrimary,
                            uncheckedThumbColor = activeSpec.textSecondary,
                            uncheckedTrackColor = activeSpec.surfaceElevated
                        )
                    )
                }
            }

            // Accent Palette Override Strip
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "ACCENT SPECTRUM OVERRIDE",
                    style = MaterialTheme.typography.labelLarge,
                    color = activeSpec.textSecondary
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemeAccentPalette.entries.forEach { palette ->
                        val isSelected = settings.uiAccentId == palette.id
                        val dotColor = palette.bright ?: AppThemePresets.specFor(activeStyle).accentBright
                        Surface(
                            onClick = { onSelectAccentId(palette.id) },
                            shape = activeSpec.pillShape,
                            color = if (isSelected) activeSpec.accentTintBg else activeSpec.surfaceCard,
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else activeSpec.cardBorderWidth,
                                color = if (isSelected) dotColor else activeSpec.cardBorderColor
                            ),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("theme_accent_${palette.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(dotColor)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = palette.label,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) activeSpec.textPrimary else activeSpec.textSecondary
                                )
                            }
                        }
                    }
                }
            }

            // Dynamic Material You toggle (API 31+)
            Surface(
                shape = activeSpec.cardShape,
                color = activeSpec.surfaceCard,
                border = themedCardBorder(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = supportsDynamicColor) {
                            onSetDynamicColor(!settings.dynamicColorEnabled)
                        }
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Android System Dynamic Color",
                            style = MaterialTheme.typography.titleMedium,
                            color = activeSpec.textPrimary
                        )
                        Text(
                            text = if (supportsDynamicColor) {
                                "Adapts Android 17 & M3 Expressive tokens from wallpaper"
                            } else {
                                "Requires Android 12+ (falls back cleanly on Android 5.0–11)"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = activeSpec.textSecondary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Switch(
                        checked = settings.dynamicColorEnabled && supportsDynamicColor,
                        enabled = supportsDynamicColor,
                        onCheckedChange = onSetDynamicColor,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = activeSpec.accentPrimary,
                            uncheckedThumbColor = activeSpec.textSecondary,
                            uncheckedTrackColor = activeSpec.surfaceElevated
                        )
                    )
                }
            }

            Text(
                text = "OS THEME ARCHITECTURES (${UiThemeStyle.entries.size})",
                style = MaterialTheme.typography.labelLarge,
                color = activeSpec.textSecondary,
                modifier = Modifier.padding(top = 2.dp)
            )

            // All 6 Theme UI Presets (including OLED Obsidian)
            UiThemeStyle.entries.forEach { style ->
                val previewSpec = AppThemePresets.resolveSpec(
                    style = style,
                    oledEnabled = settings.oledModeEnabled && style != UiThemeStyle.OLED,
                    accentId = settings.uiAccentId
                )
                val isSelected = style == activeStyle

                Surface(
                    shape = previewSpec.cardShape,
                    color = previewSpec.surfaceCard,
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else previewSpec.cardBorderWidth,
                        color = if (isSelected) previewSpec.accentBright else previewSpec.cardBorderColor
                    ),
                    tonalElevation = previewSpec.tonalElevation,
                    shadowElevation = previewSpec.shadowElevation,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectThemeStyle(style.id) }
                        .testTag("theme_preset_${style.id}")
                ) {
                    Column(
                        modifier = Modifier
                            .background(previewSpec.glassHighlightColor)
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(previewSpec.bgBase)
                                        .border(1.dp, previewSpec.surfaceDivider, CircleShape)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(previewSpec.accentPrimary)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(previewSpec.tertiaryAccent)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = style.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = previewSpec.textPrimary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = previewSpec.badgeShape,
                                        color = previewSpec.accentTintBg,
                                        border = BorderStroke(1.dp, previewSpec.accentBright.copy(alpha = 0.5f))
                                    ) {
                                        Text(
                                            text = style.osBadge,
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = previewSpec.accentBright,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = style.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = previewSpec.textSecondary
                                )
                            }

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = previewSpec.accentBright,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = previewSpec.stepperShape,
                                color = previewSpec.surfaceElevated,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = style.shapeSummary,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 10.sp,
                                    color = previewSpec.textPrimary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }

                            Surface(
                                shape = previewSpec.buttonShape,
                                color = previewSpec.accentPrimary
                            ) {
                                Text(
                                    text = if (isSelected) "APPLIED" else "APPLY",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
