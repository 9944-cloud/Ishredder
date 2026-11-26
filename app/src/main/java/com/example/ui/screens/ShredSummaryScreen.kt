package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ErasureMethod
import com.example.model.ShredExecutionState
import com.example.model.ShredTargetItem
import com.example.ui.components.BrandTopBar
import com.example.ui.components.PrimaryShredButton
import com.example.ui.components.ShredderHeroGraphic
import com.example.ui.components.WizardStepperBar
import com.example.ui.components.themedScreenBackground
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalAppThemeSpec
import com.example.util.FormatUtils

@Composable
fun ShredSummaryScreen(
    method: ErasureMethod,
    queuedTargets: List<ShredTargetItem>,
    executionState: ShredExecutionState,
    reportEnabled: Boolean,
    onStepNavigate: (Int) -> Unit,
    onBack: () -> Unit,
    onStartShredding: () -> Unit,
    onCancelShredding: () -> Unit,
    onViewReport: (Long) -> Unit,
    onDoneHome: () -> Unit
) {
    val spec = LocalAppThemeSpec.current
    var showConfirmDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .themedScreenBackground()
    ) {
        BrandTopBar(
            showBack = executionState !is ShredExecutionState.Running,
            onBack = onBack
        )

        WizardStepperBar(
            activeStep = 3,
            onStepClick = if (executionState is ShredExecutionState.Idle) onStepNavigate else null
        )

        Spacer(modifier = Modifier.height(14.dp))

        when (executionState) {
            is ShredExecutionState.Idle -> {
                val totalSize = queuedTargets.sumOf { it.sizeBytes }
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Selected Method Summary Card
                    item {
                        Surface(
                            shape = spec.cardShape,
                            color = spec.accentTintBg,
                            border = BorderStroke(1.dp, spec.accentBright.copy(alpha = 0.7f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "ERASURE ALGORITHM",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 11.sp,
                                        color = spec.accentBright
                                    )
                                    Text(
                                        text = "${method.tier.badge} • ${method.cycles} ${if (method.cycles == 1) "Cycle" else "Cycles"}",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 11.sp,
                                        color = spec.accentBright
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = method.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = spec.textPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = method.passPatternSummary,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 11.sp,
                                    color = spec.textSecondary
                                )
                            }
                        }
                    }

                    // Warning Banner
                    item {
                        Surface(
                            shape = spec.cardShape,
                            color = spec.dangerTint,
                            border = BorderStroke(1.dp, spec.danger.copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.WarningAmber,
                                    contentDescription = "Permanent deletion warning",
                                    tint = spec.danger,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Irreversible Destruction: All queued data will be overwritten ${method.cycles}x and permanently unlinked.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFFFDAD6)
                                )
                            }
                        }
                    }

                    // Targets Header
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Targets (${queuedTargets.size})",
                                style = MaterialTheme.typography.titleMedium,
                                color = spec.textPrimary
                            )
                            Text(
                                text = "${FormatUtils.formatBytes(totalSize)} (${FormatUtils.formatBytes(totalSize * method.cycles)} total I/O)",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                color = spec.textSecondary
                            )
                        }
                    }

                    items(queuedTargets, key = { it.id }) { item ->
                        Surface(
                            shape = spec.cardShape,
                            color = spec.surfaceCard,
                            border = BorderStroke(spec.cardBorderWidth, spec.cardBorderColor),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Security,
                                    contentDescription = null,
                                    tint = spec.accentBright,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = spec.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = item.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = spec.textSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = FormatUtils.formatBytes(item.sizeBytes),
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 12.sp,
                                    color = spec.textPrimary
                                )
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(12.dp)) }
                }

                PrimaryShredButton(
                    text = "START SHREDDING NOW",
                    enabled = queuedTargets.isNotEmpty(),
                    onClick = { showConfirmDialog = true },
                    testTag = "start_shredding_button"
                )
            }

            is ShredExecutionState.Running -> {
                val fraction = if (executionState.totalBytesPlanned > 0L) {
                    (executionState.bytesProcessedInRun.toFloat() / executionState.totalBytesPlanned.toFloat())
                        .coerceIn(0f, 1f)
                } else 0f

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    ShredderHeroGraphic(isAnimated = true, sizeDp = 180.dp)

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "${(fraction * 100).toInt()}%",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = spec.accentBright
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = { fraction },
                        color = spec.accentBright,
                        trackColor = spec.surfaceElevated,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(spec.pillShape)
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Surface(
                        shape = spec.cardShape,
                        color = spec.surfaceCard,
                        border = BorderStroke(spec.cardBorderWidth, spec.cardBorderColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = executionState.currentItemName,
                                style = MaterialTheme.typography.titleSmall,
                                color = spec.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Item ${executionState.currentItemIndex}/${executionState.totalItems} • Pass ${executionState.currentPass}/${executionState.totalPasses}",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 12.sp,
                                color = spec.accentBright
                            )
                            Text(
                                text = "Pattern: ${executionState.passLabel}",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                color = spec.textSecondary
                            )
                            HorizontalDivider(color = spec.surfaceDivider.copy(alpha = 0.5f))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Speed: ${FormatUtils.formatBytes(executionState.speedBytesPerSec)}/s",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 11.sp,
                                    color = spec.textSecondary
                                )
                                Text(
                                    text = "ETA: ${FormatUtils.formatEta(executionState.etaSeconds)}",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 11.sp,
                                    color = spec.textSecondary
                                )
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancelShredding,
                        shape = spec.buttonShape,
                        border = BorderStroke(1.dp, spec.danger),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("cancel_shred_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Cancel,
                            contentDescription = null,
                            tint = spec.danger
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ABORT ERASURE", color = spec.danger, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            is ShredExecutionState.Completed -> {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (executionState.wasCancelled) Icons.Filled.WarningAmber else Icons.Filled.Verified,
                        contentDescription = null,
                        tint = if (executionState.wasCancelled) spec.warning else spec.accentBright,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (executionState.wasCancelled) "Erasure Cancelled" else "Secure Erasure Complete",
                        style = MaterialTheme.typography.headlineMedium,
                        color = spec.textPrimary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${executionState.methodName} (${executionState.cycles} passes)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = spec.accentBright
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Surface(
                        shape = spec.cardShape,
                        color = spec.surfaceCard,
                        border = BorderStroke(spec.cardBorderWidth, spec.cardBorderColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SummaryStatRow("Certificate ID", executionState.reportCode)
                            SummaryStatRow("Targets Shredded", "${executionState.itemsShredded} (Failed: ${executionState.itemsFailed})")
                            SummaryStatRow("Total Bytes Overwritten", FormatUtils.formatBytes(executionState.totalBytesShredded))
                            SummaryStatRow("Duration", FormatUtils.formatDuration(executionState.durationMillis))
                            HorizontalDivider(color = spec.surfaceDivider.copy(alpha = 0.5f))
                            Text(
                                text = "SHA-256 Verification Digest",
                                style = MaterialTheme.typography.labelSmall,
                                color = spec.textSecondary
                            )
                            Text(
                                text = executionState.verificationSha256,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                color = spec.accentBright
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (reportEnabled && executionState.reportId != null) {
                        OutlinedButton(
                            onClick = { onViewReport(executionState.reportId) },
                            shape = spec.buttonShape,
                            border = BorderStroke(1.dp, spec.accentBright),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("view_report_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ReceiptLong,
                                contentDescription = null,
                                tint = spec.accentBright
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("VIEW ERASURE REPORT", color = spec.accentBright, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Button(
                        onClick = onDoneHome,
                        shape = spec.buttonShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = spec.accentPrimary,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("done_home_button")
                    ) {
                        Text("DONE", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            shape = spec.dialogShape,
            containerColor = spec.surfaceCard,
            titleContentColor = spec.textPrimary,
            textContentColor = spec.textSecondary,
            title = { Text("Confirm Permanent Shredding") },
            text = {
                Text(
                    "You are about to permanently overwrite ${queuedTargets.size} target(s) using ${method.name} (${method.cycles} passes). This cannot be undone."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmDialog = false
                        onStartShredding()
                    }
                ) {
                    Text("SHRED FOREVER", color = spec.danger, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("CANCEL", color = spec.textSecondary)
                }
            }
        )
    }
}

@Composable
private fun SummaryStatRow(label: String, value: String) {
    val spec = LocalAppThemeSpec.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = spec.textSecondary)
        Text(
            text = value,
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = spec.textPrimary
        )
    }
}
