package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.ErasureCatalog
import com.example.model.StorageOverview
import com.example.ui.components.BrandTopBar
import com.example.ui.components.ShredderHeroGraphic
import com.example.ui.components.themedCardBorder
import com.example.ui.components.themedScreenBackground
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalAppThemeSpec
import com.example.util.FormatUtils

@Composable
fun HomeScreen(
    storageOverview: StorageOverview,
    reportCount: Int,
    privacyScore: Int,
    onSelectiveEraseClick: () -> Unit,
    onFileExplorerClick: () -> Unit,
    onSecuritySuite2026Click: () -> Unit,
    onFactoryResetClick: () -> Unit,
    onThemeClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onReportsClick: () -> Unit
) {
    val spec = LocalAppThemeSpec.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .themedScreenBackground()
            .navigationBarsPadding()
    ) {
        BrandTopBar(
            showBack = false,
            showThemeButton = true,
            onThemeClick = onThemeClick,
            showSettings = true,
            onSettingsClick = onSettingsClick
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            ShredderHeroGraphic(
                isAnimated = false,
                sizeDp = 204.dp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.home_welcome_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = spec.textPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = spec.badgeShape,
                    color = spec.accentTintBg,
                    border = BorderStroke(1.dp, spec.accentBright.copy(alpha = 0.55f))
                ) {
                    Text(
                        text = "2026",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = spec.accentBright,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.home_welcome_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = spec.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 320.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Primary 2x2 Action Grid
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 460.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                HomeActionTile(
                    title = stringResource(R.string.action_selective_erase),
                    subtitle = null,
                    accentBorder = false,
                    onClick = onSelectiveEraseClick,
                    testTag = "selective_erase_card",
                    modifier = Modifier.weight(1f)
                ) {
                    SelectivePartitionIcon(
                        color = spec.textPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                HomeActionTile(
                    title = stringResource(R.string.action_file_explorer),
                    subtitle = null,
                    accentBorder = false,
                    onClick = onFileExplorerClick,
                    testTag = "file_explorer_card",
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.FindInPage,
                        contentDescription = stringResource(R.string.action_file_explorer),
                        tint = spec.textPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 460.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                HomeActionTile(
                    title = stringResource(R.string.action_security_2026),
                    subtitle = "Score $privacyScore/100 • EXIF • Guard",
                    accentBorder = true,
                    onClick = onSecuritySuite2026Click,
                    testTag = "excellent_2026_card",
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Shield,
                        contentDescription = stringResource(R.string.action_security_2026),
                        tint = spec.accentBright,
                        modifier = Modifier.size(34.dp)
                    )
                }

                HomeActionTile(
                    title = stringResource(R.string.action_factory_reset),
                    subtitle = "Pre-reset & app wipe",
                    accentBorder = false,
                    dangerBorder = true,
                    onClick = onFactoryResetClick,
                    testTag = "factory_reset_card",
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.RestartAlt,
                        contentDescription = stringResource(R.string.action_factory_reset),
                        tint = spec.danger,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Storage, Algorithm Count, Theme/OLED & Reports status pill
            Surface(
                shape = spec.pillShape,
                color = if (spec.isOledBlack) Color(0xFF090A0D) else spec.surfaceCard.copy(alpha = 0.84f),
                border = themedCardBorder(),
                modifier = Modifier
                    .clickable(onClick = onReportsClick)
                    .testTag("home_status_pill")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.VerifiedUser,
                        contentDescription = null,
                        tint = spec.accentBright,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = buildString {
                            append("Excellent 2026 • ")
                            if (storageOverview.availableBytes > 0L) {
                                append("${FormatUtils.formatBytes(storageOverview.availableBytes)} Free")
                            } else {
                                append("${ErasureCatalog.allMethods.size} Algorithms Ready")
                            }
                            if (reportCount > 0) {
                                append(" • $reportCount Reports")
                            }
                        },
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        color = spec.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HomeActionTile(
    title: String,
    subtitle: String?,
    accentBorder: Boolean,
    dangerBorder: Boolean = false,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
    iconContent: @Composable () -> Unit
) {
    val spec = LocalAppThemeSpec.current
    val border = when {
        dangerBorder -> BorderStroke(spec.cardBorderWidth, spec.danger.copy(alpha = 0.55f))
        accentBorder -> themedCardBorder(accentFocused = true)
        else -> themedCardBorder()
    }
    Surface(
        onClick = onClick,
        shape = spec.tileShape,
        color = spec.surfaceCard,
        border = border,
        tonalElevation = spec.tonalElevation,
        shadowElevation = spec.shadowElevation,
        modifier = modifier
            .height(126.dp)
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(spec.glassHighlightColor)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            iconContent()
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = spec.textPrimary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 9.5.sp,
                    color = spec.textSecondary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SelectivePartitionIcon(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val r = size.minDimension * 0.42f
        val cx = size.width / 2f
        val cy = size.height / 2f
        val gap = size.minDimension * 0.05f

        drawArc(
            color = color,
            startAngle = 90f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = Offset(cx - r - gap / 2f, cy - r),
            size = Size(r * 2f, r * 2f)
        )
        drawArc(
            color = color,
            startAngle = 270f,
            sweepAngle = 90f,
            useCenter = true,
            topLeft = Offset(cx - r + gap / 2f, cy - r - gap / 2f),
            size = Size(r * 2f, r * 2f)
        )
        drawArc(
            color = color,
            startAngle = 0f,
            sweepAngle = 90f,
            useCenter = true,
            topLeft = Offset(cx - r + gap / 2f, cy - r + gap / 2f),
            size = Size(r * 2f, r * 2f)
        )
    }
}
