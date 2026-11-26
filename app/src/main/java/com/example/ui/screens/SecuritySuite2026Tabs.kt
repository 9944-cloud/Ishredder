package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsOff
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppSettings
import com.example.engine.ExifPhotoReport
import com.example.engine.HardwareBenchmarkResult
import com.example.engine.PrivacyAuditReport
import com.example.engine.ScrubbedPhotoResult
import com.example.model.ErasureCatalog
import com.example.ui.components.themedCardBorder
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalAppThemeSpec
import com.example.util.FormatUtils

@Composable
fun PrivacyAuditTab(
    audit: PrivacyAuditReport,
    isBusy: Boolean,
    onRunSweep: (includeClipboard: Boolean) -> Unit
) {
    val spec = LocalAppThemeSpec.current
    val animScore by animateFloatAsState(
        targetValue = audit.score / 100f,
        animationSpec = tween(550),
        label = "audit_gauge"
    )
    val gaugeColor = when {
        audit.score >= 85 -> spec.accentBright
        audit.score >= 60 -> spec.warning
        else -> spec.danger
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = spec.cardShape,
            color = spec.surfaceCard,
            border = themedCardBorder(accentFocused = true),
            tonalElevation = spec.tonalElevation,
            shadowElevation = spec.shadowElevation,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(96.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeW = 10.dp.toPx()
                        drawArc(
                            color = spec.surfaceElevated,
                            startAngle = 135f,
                            sweepAngle = 270f,
                            useCenter = false,
                            style = Stroke(width = strokeW, cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = gaugeColor,
                            startAngle = 135f,
                            sweepAngle = 270f * animScore,
                            useCenter = false,
                            style = Stroke(width = strokeW, cap = StrokeCap.Round)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${audit.score}",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = spec.textPrimary
                        )
                        Text(
                            text = "SCORE",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 9.sp,
                            color = spec.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = spec.badgeShape,
                        color = gaugeColor.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, gaugeColor.copy(alpha = 0.55f))
                    ) {
                        Text(
                            text = audit.grade,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = gaugeColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Text(
                        text = "2026 Privacy Radar",
                        style = MaterialTheme.typography.titleMedium,
                        color = spec.textPrimary
                    )
                    Text(
                        text = "Live on-device telemetry across sandbox cache, clipboard RAM, PIN lock & audit history.",
                        style = MaterialTheme.typography.bodySmall,
                        color = spec.textSecondary
                    )
                }
            }
        }

        audit.items.forEach { item ->
            val itemTint = when (item.severity) {
                "SAFE" -> spec.accentBright
                "WARN" -> spec.warning
                else -> spec.danger
            }
            Surface(
                shape = spec.cardShape,
                color = spec.surfaceCard,
                border = themedCardBorder(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (item.severity == "SAFE") Icons.Filled.CheckCircle else Icons.Filled.WarningAmber,
                        contentDescription = null,
                        tint = itemTint,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.title, style = MaterialTheme.typography.titleSmall, color = spec.textPrimary)
                        Text(item.detail, style = MaterialTheme.typography.bodySmall, color = spec.textSecondary)
                    }
                    if (item.pointsDeducted > 0) {
                        Text(
                            text = "-${item.pointsDeducted}",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = itemTint
                        )
                    }
                }
            }
        }

        Button(
            onClick = { onRunSweep(true) },
            enabled = !isBusy,
            shape = spec.buttonShape,
            colors = ButtonDefaults.buttonColors(containerColor = spec.accentPrimary, contentColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("run_2026_hygiene_sweep_button")
        ) {
            if (isBusy) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Text("SANITIZING...", fontWeight = FontWeight.Bold)
            } else {
                Icon(Icons.Filled.CleaningServices, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("RUN 2026 DEEP HYGIENE SWEEP", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
fun ExifScrubberTab(
    photos: List<ExifPhotoReport>,
    lastScrubbed: ScrubbedPhotoResult?,
    isBusy: Boolean,
    onPickPhotos: (List<Uri>) -> Unit,
    onScrubToUri: (ExifPhotoReport, Uri) -> Unit,
    onClear: () -> Unit
) {
    val spec = LocalAppThemeSpec.current
    var pendingPhotoForSave by remember { mutableStateOf<ExifPhotoReport?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris ->
        if (uris.isNotEmpty()) onPickPhotos(uris)
    }

    val createCleanDoc = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/jpeg")
    ) { destUri ->
        val source = pendingPhotoForSave
        pendingPhotoForSave = null
        if (destUri != null && source != null) {
            onScrubToUri(source, destUri)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = spec.cardShape,
            color = spec.surfaceCard,
            border = themedCardBorder(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.GpsOff, contentDescription = null, tint = spec.accentBright, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("EXIF & GPS Privacy Scrubber", style = MaterialTheme.typography.titleMedium, color = spec.textPrimary)
                        Text(
                            "Inspect hidden GPS location, camera model & timestamps via Android Photo Picker, then export zero-EXIF clean copies.",
                            style = MaterialTheme.typography.bodySmall,
                            color = spec.textSecondary
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        shape = spec.buttonShape,
                        colors = ButtonDefaults.buttonColors(containerColor = spec.accentPrimary, contentColor = Color.White),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("pick_exif_photos_button")
                    ) {
                        Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("INSPECT PHOTOS", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    if (photos.isNotEmpty()) {
                        TextButton(
                            onClick = onClear,
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text("CLEAR", color = spec.textSecondary)
                        }
                    }
                }
            }
        }

        if (lastScrubbed != null) {
            Surface(
                shape = spec.cardShape,
                color = spec.accentTintBg,
                border = themedCardBorder(accentFocused = true),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Sanitized Export Verified", style = MaterialTheme.typography.titleSmall, color = spec.accentBright)
                    Text(
                        "${lastScrubbed.originalName} • ${FormatUtils.formatBytes(lastScrubbed.outputBytes)} • ${lastScrubbed.strippedTagCount} risk tags stripped",
                        style = MaterialTheme.typography.bodySmall,
                        color = spec.textPrimary
                    )
                    Text(
                        "SHA-256: ${lastScrubbed.sha256.take(28)}...",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        color = spec.textSecondary
                    )
                }
            }
        }

        if (photos.isEmpty()) {
            Surface(
                shape = spec.cardShape,
                color = spec.surfaceCard,
                border = themedCardBorder(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Filled.Shield, contentDescription = null, tint = spec.textMuted, modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Zero-Permission Photo Audit", style = MaterialTheme.typography.titleSmall, color = spec.textPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Select photos above to detect leaked GPS coordinates, phone model identifiers, and capture timestamps.",
                        style = MaterialTheme.typography.bodySmall,
                        color = spec.textSecondary
                    )
                }
            }
        } else {
            photos.forEach { photo ->
                Surface(
                    shape = spec.cardShape,
                    color = spec.surfaceCard,
                    border = themedCardBorder(accentFocused = photo.hasGps),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (photo.hasGps) Icons.Filled.GpsFixed else Icons.Filled.GpsOff,
                                contentDescription = null,
                                tint = if (photo.hasGps) spec.danger else spec.accentBright,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    photo.displayName,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = spec.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "${photo.resolutionLabel} • ${FormatUtils.formatBytes(photo.sizeBytes)}",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 10.sp,
                                    color = spec.textSecondary
                                )
                            }
                            Surface(
                                shape = spec.badgeShape,
                                color = if (photo.hasGps) spec.dangerTint else spec.accentTintBg
                            ) {
                                Text(
                                    text = if (photo.hasGps) "GPS EXPOSED" else "${photo.riskTagCount} TAGS",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (photo.hasGps) spec.danger else spec.accentBright,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        if (photo.gpsSummary != null) {
                            Text("• Location: ${photo.gpsSummary}", style = MaterialTheme.typography.bodySmall, color = spec.danger)
                        }
                        if (photo.cameraMakeModel != null) {
                            Text("• Device: ${photo.cameraMakeModel}", style = MaterialTheme.typography.bodySmall, color = spec.textSecondary)
                        }
                        if (photo.captureDateTime != null) {
                            Text("• Captured: ${photo.captureDateTime}", style = MaterialTheme.typography.bodySmall, color = spec.textSecondary)
                        }
                        if (photo.softwareTag != null) {
                            Text("• Software: ${photo.softwareTag}", style = MaterialTheme.typography.bodySmall, color = spec.textSecondary)
                        }

                        OutlinedButton(
                            onClick = {
                                pendingPhotoForSave = photo
                                val cleanBase = photo.displayName.substringBeforeLast('.')
                                createCleanDoc.launch("${cleanBase}_clean.jpg")
                            },
                            enabled = !isBusy,
                            shape = spec.buttonShape,
                            border = BorderStroke(1.dp, spec.accentBright),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text("STRIP EXIF & EXPORT CLEAN PHOTO", color = spec.accentBright, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
fun AutoGuardTab(
    settings: AppSettings,
    isBusy: Boolean,
    onUpdateConfig: (enabled: Boolean, intervalHours: Int, includeClipboard: Boolean, methodId: String) -> Unit,
    onRunNow: () -> Unit
) {
    val spec = LocalAppThemeSpec.current
    val intervals = listOf(24 to "Every 24h", 72 to "Every 3 Days", 168 to "Weekly")
    val guardMethods = remember {
        listOf("q_shred_2026", "nist_800_88_r2_2026", "eu_cra_2026", "bsi_vsitr")
            .map { ErasureCatalog.findById(it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = spec.cardShape,
            color = spec.surfaceCard,
            border = themedCardBorder(accentFocused = settings.autoShredEnabled),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onUpdateConfig(
                            !settings.autoShredEnabled,
                            settings.autoShredIntervalHours,
                            settings.autoShredIncludeClipboard,
                            settings.autoShredMethodId
                        )
                    }
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Schedule, contentDescription = null, tint = spec.accentBright, modifier = Modifier.size(26.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("2026 Auto-Shred Guard", style = MaterialTheme.typography.titleMedium, color = spec.textPrimary)
                    Text(
                        "Android JobScheduler periodic wipe for app cache, residual temp blocks & audit trail.",
                        style = MaterialTheme.typography.bodySmall,
                        color = spec.textSecondary
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Switch(
                    checked = settings.autoShredEnabled,
                    onCheckedChange = {
                        onUpdateConfig(
                            it,
                            settings.autoShredIntervalHours,
                            settings.autoShredIncludeClipboard,
                            settings.autoShredMethodId
                        )
                    },
                    modifier = Modifier.testTag("auto_guard_switch"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = spec.accentPrimary,
                        uncheckedThumbColor = spec.textSecondary,
                        uncheckedTrackColor = spec.surfaceElevated
                    )
                )
            }
        }

        // Interval Selection
        Surface(
            shape = spec.cardShape,
            color = spec.surfaceCard,
            border = themedCardBorder(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("SCHEDULE CADENCE", style = MaterialTheme.typography.labelLarge, color = spec.textSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    intervals.forEach { (hours, label) ->
                        val selected = settings.autoShredIntervalHours == hours
                        Surface(
                            onClick = {
                                onUpdateConfig(
                                    settings.autoShredEnabled,
                                    hours,
                                    settings.autoShredIncludeClipboard,
                                    settings.autoShredMethodId
                                )
                            },
                            shape = spec.pillShape,
                            color = if (selected) spec.accentPrimary else spec.surfaceElevated,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    label,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (selected) Color.White else spec.textPrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2026 Algorithm Selector
        Surface(
            shape = spec.cardShape,
            color = spec.surfaceCard,
            border = themedCardBorder(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("2026 GUARD ALGORITHM", style = MaterialTheme.typography.labelLarge, color = spec.textSecondary)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    guardMethods.forEach { method ->
                        val selected = settings.autoShredMethodId == method.id
                        Surface(
                            onClick = {
                                onUpdateConfig(
                                    settings.autoShredEnabled,
                                    settings.autoShredIntervalHours,
                                    settings.autoShredIncludeClipboard,
                                    method.id
                                )
                            },
                            shape = spec.badgeShape,
                            color = if (selected) spec.accentTintBg else spec.surfaceElevated,
                            border = BorderStroke(
                                1.dp,
                                if (selected) spec.accentBright else spec.surfaceDivider
                            ),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(method.name, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = spec.textPrimary)
                                Text("${method.cycles} passes • ${method.tier.badge}", fontFamily = JetBrainsMonoFontFamily, fontSize = 9.5.sp, color = spec.accentBright)
                            }
                        }
                    }
                }
            }
        }

        // Clipboard inclusion toggle
        Surface(
            shape = spec.cardShape,
            color = spec.surfaceCard,
            border = themedCardBorder(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onUpdateConfig(
                            settings.autoShredEnabled,
                            settings.autoShredIntervalHours,
                            !settings.autoShredIncludeClipboard,
                            settings.autoShredMethodId
                        )
                    }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Include System Clipboard Scrub", style = MaterialTheme.typography.titleSmall, color = spec.textPrimary)
                    Text(
                        if (settings.lastAutoShredMillis > 0L) {
                            "Last guard sweep: ${FormatUtils.formatTimestamp(settings.lastAutoShredMillis)}"
                        } else {
                            "No scheduled sweep executed yet"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = spec.textSecondary
                    )
                }
                Switch(
                    checked = settings.autoShredIncludeClipboard,
                    onCheckedChange = {
                        onUpdateConfig(
                            settings.autoShredEnabled,
                            settings.autoShredIntervalHours,
                            it,
                            settings.autoShredMethodId
                        )
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = spec.accentPrimary,
                        uncheckedThumbColor = spec.textSecondary,
                        uncheckedTrackColor = spec.surfaceElevated
                    )
                )
            }
        }

        Button(
            onClick = onRunNow,
            enabled = !isBusy,
            shape = spec.buttonShape,
            colors = ButtonDefaults.buttonColors(containerColor = spec.accentPrimary, contentColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("auto_guard_run_now_button")
        ) {
            Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("RUN SCHEDULED SWEEP NOW", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
fun HardwareBenchmarkTab(
    result: HardwareBenchmarkResult?,
    isBusy: Boolean,
    onRunBenchmark: (Int) -> Unit,
    onApplyMethod: (String) -> Unit
) {
    val spec = LocalAppThemeSpec.current
    var blockSizeMb by remember { mutableIntStateOf(4) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = spec.cardShape,
            color = spec.surfaceCard,
            border = themedCardBorder(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Speed, contentDescription = null, tint = spec.accentBright, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("2026 Flash & CSPRNG Speed Lab", style = MaterialTheme.typography.titleMedium, color = spec.textPrimary)
                        Text(
                            "Measures real device SecureRandom entropy, fsync NAND write throughput, and SHA-256 verify rate.",
                            style = MaterialTheme.typography.bodySmall,
                            color = spec.textSecondary
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf(4, 8, 16).forEach { mb ->
                        val selected = blockSizeMb == mb
                        Surface(
                            onClick = { blockSizeMb = mb },
                            shape = spec.pillShape,
                            color = if (selected) spec.accentPrimary else spec.surfaceElevated,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    "$mb MB Probe",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (selected) Color.White else spec.textPrimary
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = { onRunBenchmark(blockSizeMb) },
                    enabled = !isBusy,
                    shape = spec.buttonShape,
                    colors = ButtonDefaults.buttonColors(containerColor = spec.accentPrimary, contentColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("run_hardware_benchmark_button")
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("BENCHMARKING...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Filled.Speed, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("RUN HARDWARE FLASH BENCHMARK", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (result != null) {
            val recMethod = ErasureCatalog.findById(result.recommendedMethodId)
            Surface(
                shape = spec.cardShape,
                color = spec.surfaceCard,
                border = themedCardBorder(accentFocused = true),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("MEASURED HARDWARE TELEMETRY (${result.testSizeMb} MB)", style = MaterialTheme.typography.labelLarge, color = spec.accentBright)
                    BenchStatRow("CSPRNG Entropy Rate", "${String.format("%.1f", result.csprngMbPerSec)} MB/s")
                    BenchStatRow("Flash Write + fsync", "${String.format("%.1f", result.flashWriteMbPerSec)} MB/s")
                    BenchStatRow("Read-Back SHA-256 Verify", "${String.format("%.1f", result.readVerifyMbPerSec)} MB/s")
                    BenchStatRow("Total Elapsed", "${result.elapsedMillis} ms")
                    BenchStatRow("Probe Digest", "${result.verificationDigest.take(20)}...")

                    Surface(
                        shape = spec.badgeShape,
                        color = spec.accentTintBg,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Recommended 2026 Standard: ${recMethod.name}", style = MaterialTheme.typography.titleSmall, color = spec.textPrimary)
                            Text(recMethod.description, style = MaterialTheme.typography.bodySmall, color = spec.textSecondary)
                            OutlinedButton(
                                onClick = { onApplyMethod(recMethod.id) },
                                shape = spec.buttonShape,
                                border = BorderStroke(1.dp, spec.accentBright),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Text("SET AS DEFAULT METHOD", color = spec.accentBright, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun BenchStatRow(label: String, value: String) {
    val spec = LocalAppThemeSpec.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = spec.textSecondary)
        Text(value, fontFamily = JetBrainsMonoFontFamily, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = spec.textPrimary)
    }
}
