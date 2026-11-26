package com.example.ui.screens

import android.net.Uri
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.example.ui.components.StandardTitleTopBar
import com.example.ui.components.themedCardBorder
import com.example.ui.components.themedScreenBackground
import com.example.ui.theme.LocalAppThemeSpec

private enum class Suite2026Tab(val label: String, val tag: String) {
    AUDIT("Audit", "tab_2026_audit"),
    EXIF_LAB("EXIF Lab", "tab_2026_exif"),
    AUTO_GUARD("Auto-Guard", "tab_2026_guard"),
    BENCHMARK("Speed Lab", "tab_2026_bench")
}

@Composable
fun SecuritySuite2026Screen(
    settings: AppSettings,
    privacyAudit: PrivacyAuditReport,
    exifScanResults: List<ExifPhotoReport>,
    lastScrubbedResult: ScrubbedPhotoResult?,
    benchmarkResult: HardwareBenchmarkResult?,
    isBusy: Boolean,
    onRunDeepHygieneSweep: (includeClipboard: Boolean) -> Unit,
    onInspectExifUris: (List<Uri>) -> Unit,
    onScrubExifToUri: (ExifPhotoReport, Uri) -> Unit,
    onClearExifResults: () -> Unit,
    onUpdateAutoShred: (enabled: Boolean, intervalHours: Int, includeClipboard: Boolean, methodId: String) -> Unit,
    onRunBenchmark: (Int) -> Unit,
    onApplyRecommendedMethod: (String) -> Unit,
    onBack: () -> Unit
) {
    val spec = LocalAppThemeSpec.current
    var activeTab by remember { mutableStateOf(Suite2026Tab.AUDIT) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .themedScreenBackground()
            .navigationBarsPadding()
    ) {
        StandardTitleTopBar(
            title = "Excellent 2026 Suite",
            onBack = onBack
        )

        // OS-themed Segmented Tab Bar
        Surface(
            shape = spec.stepperShape,
            color = spec.surfaceCard,
            border = themedCardBorder(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Suite2026Tab.entries.forEach { tab ->
                    val selected = tab == activeTab
                    Surface(
                        onClick = { activeTab = tab },
                        shape = spec.stepperShape,
                        color = if (selected) spec.accentPrimary else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag(tab.tag)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = tab.label,
                                fontSize = 11.5.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) Color.White else spec.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (activeTab) {
                Suite2026Tab.AUDIT -> PrivacyAuditTab(
                    audit = privacyAudit,
                    isBusy = isBusy,
                    onRunSweep = onRunDeepHygieneSweep
                )
                Suite2026Tab.EXIF_LAB -> ExifScrubberTab(
                    photos = exifScanResults,
                    lastScrubbed = lastScrubbedResult,
                    isBusy = isBusy,
                    onPickPhotos = onInspectExifUris,
                    onScrubToUri = onScrubExifToUri,
                    onClear = onClearExifResults
                )
                Suite2026Tab.AUTO_GUARD -> AutoGuardTab(
                    settings = settings,
                    isBusy = isBusy,
                    onUpdateConfig = onUpdateAutoShred,
                    onRunNow = { onRunDeepHygieneSweep(settings.autoShredIncludeClipboard) }
                )
                Suite2026Tab.BENCHMARK -> HardwareBenchmarkTab(
                    result = benchmarkResult,
                    isBusy = isBusy,
                    onRunBenchmark = onRunBenchmark,
                    onApplyMethod = onApplyRecommendedMethod
                )
            }
        }
    }
}
