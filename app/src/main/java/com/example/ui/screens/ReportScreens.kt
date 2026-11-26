package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ErasureReportEntity
import com.example.ui.components.StandardTitleTopBar
import com.example.ui.components.themedScreenBackground
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalAppThemeSpec
import com.example.util.FormatUtils

@Composable
fun ReportHistoryScreen(
    reports: List<ErasureReportEntity>,
    onBack: () -> Unit,
    onOpenReport: (Long) -> Unit,
    onClearAll: () -> Unit
) {
    val spec = LocalAppThemeSpec.current
    var showClearConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .themedScreenBackground()
            .navigationBarsPadding()
    ) {
        StandardTitleTopBar(
            title = "Erasure Report History",
            onBack = onBack,
            actions = {
                if (reports.isNotEmpty()) {
                    IconButton(
                        onClick = { showClearConfirm = true },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteSweep,
                            contentDescription = "Clear all reports",
                            tint = spec.danger
                        )
                    }
                }
            }
        )

        if (reports.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.ReceiptLong,
                        contentDescription = null,
                        tint = spec.textSecondary,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "No Erasure Reports Yet",
                        style = MaterialTheme.typography.titleLarge,
                        color = spec.textPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Completed secure shredding runs will generate verified local SHA-256 certificates here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = spec.textSecondary
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(reports, key = { it.id }) { report ->
                    Surface(
                        shape = spec.cardShape,
                        color = spec.surfaceCard,
                        border = BorderStroke(spec.cardBorderWidth, spec.cardBorderColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenReport(report.id) }
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = report.reportCode,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = spec.accentBright
                                )
                                Surface(
                                    shape = spec.badgeShape,
                                    color = if (report.status == "VERIFIED") spec.accentTintBg else spec.surfaceElevated
                                ) {
                                    Text(
                                        text = report.status,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 10.sp,
                                        color = if (report.status == "VERIFIED") spec.accentBright else spec.warning,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${report.methodName} (${report.cycles} passes)",
                                style = MaterialTheme.typography.titleSmall,
                                color = spec.textPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = report.targetSummary,
                                style = MaterialTheme.typography.bodySmall,
                                color = spec.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${report.itemsShreddedCount} item(s) • ${FormatUtils.formatBytes(report.totalBytesShredded)}",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 11.sp,
                                    color = spec.textSecondary
                                )
                                Text(
                                    text = FormatUtils.formatShortDate(report.timestampMillis),
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 11.sp,
                                    color = spec.textSecondary
                                )
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            shape = spec.dialogShape,
            containerColor = spec.surfaceCard,
            titleContentColor = spec.textPrimary,
            textContentColor = spec.textSecondary,
            title = { Text("Clear Report History") },
            text = { Text("Delete all locally stored erasure audit reports?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearAll()
                        showClearConfirm = false
                    }
                ) {
                    Text("DELETE ALL", color = spec.danger, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("CANCEL", color = spec.textSecondary)
                }
            }
        )
    }
}

@Composable
fun ReportDetailScreen(
    reportId: Long,
    onLoadReport: suspend (Long) -> ErasureReportEntity?,
    buildShareText: (ErasureReportEntity) -> String,
    onDeleteReport: (Long) -> Unit,
    onBack: () -> Unit
) {
    val spec = LocalAppThemeSpec.current
    val context = LocalContext.current
    var report by remember(reportId) { mutableStateOf<ErasureReportEntity?>(null) }

    LaunchedEffect(reportId) {
        report = onLoadReport(reportId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .themedScreenBackground()
            .navigationBarsPadding()
    ) {
        StandardTitleTopBar(
            title = "Erasure Certificate",
            onBack = onBack,
            actions = {
                report?.let { item ->
                    IconButton(
                        onClick = {
                            onDeleteReport(item.id)
                            onBack()
                        },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete report",
                            tint = spec.danger
                        )
                    }
                }
            }
        )

        val current = report
        if (current == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Loading certificate...", color = spec.textSecondary)
            }
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = spec.cardShape,
                    color = spec.accentTintBg,
                    border = BorderStroke(1.dp, spec.accentBright),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Verified,
                            contentDescription = null,
                            tint = spec.accentBright,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = current.reportCode,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = spec.accentBright
                            )
                            Text(
                                text = "Status: ${current.status} • ${FormatUtils.formatTimestamp(current.timestampMillis)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = spec.textPrimary
                            )
                        }
                    }
                }

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
                        CertificateRow("Algorithm", "${current.methodName} [${current.methodTier}]")
                        CertificateRow("Overwrite Passes", "${current.cycles}")
                        CertificateRow("Items Shredded", "${current.itemsShreddedCount} (Failed: ${current.itemsFailedCount})")
                        CertificateRow("Total Bytes Written", FormatUtils.formatBytes(current.totalBytesShredded))
                        CertificateRow("Duration", FormatUtils.formatDuration(current.durationMillis))
                        HorizontalDivider(color = spec.surfaceDivider.copy(alpha = 0.5f))
                        Text("Targets Summary", style = MaterialTheme.typography.labelSmall, color = spec.textSecondary)
                        Text(current.targetSummary, style = MaterialTheme.typography.bodySmall, color = spec.textPrimary)
                        HorizontalDivider(color = spec.surfaceDivider.copy(alpha = 0.5f))
                        Text("SHA-256 Verification Hash", style = MaterialTheme.typography.labelSmall, color = spec.textSecondary)
                        Text(
                            text = current.verificationSha256,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            color = spec.accentBright
                        )
                    }
                }

                Surface(
                    shape = spec.cardShape,
                    color = spec.surfaceCard,
                    border = BorderStroke(spec.cardBorderWidth, spec.cardBorderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Execution Log",
                            style = MaterialTheme.typography.titleSmall,
                            color = spec.textPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = current.itemsDetailLog,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            color = spec.textSecondary,
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Button(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "iShredder Erasure Report ${current.reportCode}")
                            putExtra(Intent.EXTRA_TEXT, buildShareText(current))
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Export Erasure Report"))
                    },
                    shape = spec.buttonShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = spec.accentPrimary,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("EXPORT / SHARE REPORT", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun CertificateRow(label: String, value: String) {
    val spec = LocalAppThemeSpec.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = spec.textSecondary)
        Text(
            text = value,
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 12.sp,
            color = spec.textPrimary
        )
    }
}
