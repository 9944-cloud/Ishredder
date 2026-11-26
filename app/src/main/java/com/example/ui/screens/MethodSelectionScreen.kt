package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.model.ErasureCatalog
import com.example.model.ErasureMethod
import com.example.model.MethodTier
import com.example.model.SelectiveCategory
import com.example.model.ShredTargetItem
import com.example.ui.components.BrandTopBar
import com.example.ui.components.PrimaryShredButton
import com.example.ui.components.WizardStepperBar
import com.example.ui.components.themedScreenBackground
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalAppThemeSpec
import com.example.ui.theme.ShredTierMil
import com.example.ui.theme.ShredTierPro
import com.example.ui.theme.ShredTierStd
import com.example.util.FormatUtils

@Composable
fun MethodSelectionScreen(
    selectedMethodId: String,
    recommendedMethod: ErasureMethod,
    queuedTargets: List<ShredTargetItem>,
    onSelectMethod: (String) -> Unit,
    onStepNavigate: (Int) -> Unit,
    onBack: () -> Unit,
    onConfirmShred: () -> Unit
) {
    val spec = LocalAppThemeSpec.current
    var inspectedMethod by remember { mutableStateOf<ErasureMethod?>(null) }
    var showSummaryModal by remember { mutableStateOf(false) }
    val allMethods = remember { ErasureCatalog.allMethods }
    val activeMethod = remember(selectedMethodId) { ErasureCatalog.findById(selectedMethodId) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .themedScreenBackground()
    ) {
        BrandTopBar(showBack = true, onBack = onBack)

        WizardStepperBar(
            activeStep = 2,
            onStepClick = onStepNavigate
        )

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.recommended_method_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = spec.textPrimary
                    )
                    Text(
                        text = stringResource(R.string.recommended_method_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = spec.textSecondary
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            item {
                val isSelected = selectedMethodId == recommendedMethod.id
                Surface(
                    shape = spec.cardShape,
                    color = if (isSelected) spec.accentTintBg else spec.surfaceCard,
                    border = BorderStroke(
                        if (isSelected) 1.5.dp else spec.cardBorderWidth,
                        if (isSelected) spec.accentBright.copy(alpha = 0.75f) else spec.cardBorderColor
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectMethod(recommendedMethod.id) }
                        .testTag("recommended_method_card")
                ) {
                    MethodRowContent(
                        method = recommendedMethod,
                        isSelected = isSelected,
                        isRecommendedHighlight = true,
                        onSelect = { onSelectMethod(recommendedMethod.id) },
                        onInfoClick = { inspectedMethod = recommendedMethod }
                    )
                }
                Spacer(modifier = Modifier.height(18.dp))
            }

            item {
                Text(
                    text = stringResource(R.string.all_methods_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = spec.textPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width(68.dp))
                    Text(
                        text = stringResource(R.string.col_method),
                        style = MaterialTheme.typography.bodySmall,
                        color = spec.textSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = stringResource(R.string.col_cycles),
                        style = MaterialTheme.typography.bodySmall,
                        color = spec.textSecondary,
                        modifier = Modifier.padding(end = 52.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            itemsIndexed(allMethods, key = { _, item -> item.id }) { index, method ->
                val isSelected = selectedMethodId == method.id
                val rowShape = when {
                    index == 0 && allMethods.size == 1 -> spec.cardShape
                    index == 0 -> spec.rowTopShape
                    index == allMethods.lastIndex -> spec.rowBottomShape
                    else -> RoundedCornerShape(0.dp)
                }

                Surface(
                    shape = rowShape,
                    color = if (isSelected) spec.accentTintBg else spec.surfaceCard,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectMethod(method.id) }
                        .testTag("method_row_${method.id}")
                ) {
                    Column {
                        MethodRowContent(
                            method = method,
                            isSelected = isSelected,
                            isRecommendedHighlight = false,
                            onSelect = { onSelectMethod(method.id) },
                            onInfoClick = { inspectedMethod = method }
                        )
                        if (index < allMethods.lastIndex) {
                            HorizontalDivider(
                                color = spec.surfaceDivider.copy(alpha = 0.45f),
                                thickness = 0.8.dp
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }

        PrimaryShredButton(
            text = stringResource(R.string.cta_open_summary),
            enabled = queuedTargets.isNotEmpty(),
            onClick = { showSummaryModal = true },
            testTag = "open_summary_button"
        )
    }

    if (showSummaryModal) {
        ShredSummaryConfirmModal(
            method = activeMethod,
            queuedTargets = queuedTargets,
            onDismiss = { showSummaryModal = false },
            onConfirmShred = {
                showSummaryModal = false
                onConfirmShred()
            }
        )
    }

    inspectedMethod?.let { method ->
        MethodSpecificationDialog(
            method = method,
            isSelected = selectedMethodId == method.id,
            onSelect = {
                onSelectMethod(method.id)
                inspectedMethod = null
            },
            onDismiss = { inspectedMethod = null }
        )
    }
}

private data class SummaryGroupRow(
    val title: String,
    val subtitle: String,
    val count: Int
)

@Composable
private fun ShredSummaryConfirmModal(
    method: ErasureMethod,
    queuedTargets: List<ShredTargetItem>,
    onDismiss: () -> Unit,
    onConfirmShred: () -> Unit
) {
    val spec = LocalAppThemeSpec.current
    var awarenessChecked by remember { mutableStateOf(false) }

    val groups = remember(queuedTargets) {
        buildList {
            val freeSpace = queuedTargets.filter { it.isFreeSpaceWipe }
            if (freeSpace.isNotEmpty()) {
                val bytes = freeSpace.sumOf { it.sizeBytes }
                add(SummaryGroupRow("Free Space", "Overwrites ${FormatUtils.formatBytes(bytes)} unallocated space.", freeSpace.size))
            }
            val files = queuedTargets.filter {
                it.category == SelectiveCategory.DOCUMENTS_FILES ||
                    it.category == SelectiveCategory.MOUNTED_FOLDERS ||
                    it.category == SelectiveCategory.RESIDUAL_FILES
            }
            if (files.isNotEmpty()) {
                add(SummaryGroupRow("Selected Files & Folders", "Deletes selected files & folders.", files.size))
            }
            val privacy = queuedTargets.filter {
                it.category == SelectiveCategory.APP_CACHE || it.isClipboardWipe
            }
            if (privacy.isNotEmpty()) {
                add(SummaryGroupRow("Privacy Data", "Wipes app cache & clipboard buffers.", privacy.size))
            }
            val media = queuedTargets.filter { it.category == SelectiveCategory.PHOTOS_VIDEOS }
            if (media.isNotEmpty()) {
                add(SummaryGroupRow("Selected Media", "Deletes selected photos & videos.", media.size))
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = spec.dialogShape,
            color = spec.surfaceCard,
            border = BorderStroke(spec.cardBorderWidth, spec.cardBorderColor),
            tonalElevation = 8.dp,
            shadowElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("summary_confirm_modal")
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 22.dp)
            ) {
                Text(
                    text = "Shred with ${method.name}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Normal,
                    color = spec.textPrimary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    groups.forEach { group ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = group.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = spec.textPrimary
                                )
                                Text(
                                    text = group.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = spec.textSecondary
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Surface(
                                shape = spec.badgeShape,
                                color = spec.accentPrimary,
                                modifier = Modifier.size(30.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = group.count.toString(),
                                        color = Color.White,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { awarenessChecked = !awarenessChecked }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = awarenessChecked,
                        onCheckedChange = { awarenessChecked = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = spec.accentPrimary,
                            uncheckedColor = spec.textPrimary,
                            checkmarkColor = Color.White
                        ),
                        modifier = Modifier.testTag("summary_awareness_checkbox")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "I am aware that all selected data will be securely erased and cannot be restored.",
                        style = MaterialTheme.typography.bodySmall,
                        color = spec.textPrimary,
                        lineHeight = 18.sp
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("modal_cancel_button")
                    ) {
                        Text(
                            text = "CANCEL",
                            color = spec.textPrimary,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.6.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    TextButton(
                        onClick = onConfirmShred,
                        enabled = awarenessChecked,
                        modifier = Modifier.testTag("modal_shred_button")
                    ) {
                        Text(
                            text = "SHRED",
                            color = if (awarenessChecked) spec.accentBright else spec.textMuted,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.6.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MethodRowContent(
    method: ErasureMethod,
    isSelected: Boolean,
    isRecommendedHighlight: Boolean,
    onSelect: () -> Unit,
    onInfoClick: () -> Unit
) {
    val spec = LocalAppThemeSpec.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onInfoClick,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("info_method_${method.id}")
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = stringResource(R.string.cd_method_info),
                tint = if (isRecommendedHighlight) spec.accentBright else spec.textPrimary,
                modifier = Modifier.size(22.dp)
            )
        }

        val tierColor = when (method.tier) {
            MethodTier.STD -> ShredTierStd
            MethodTier.PRO -> ShredTierPro
            MethodTier.MIL -> ShredTierMil
        }
        Box(
            modifier = Modifier.width(22.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = method.tier.badge,
                color = tierColor,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.rotate(-90f)
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        Text(
            text = method.name,
            color = if (isRecommendedHighlight) spec.accentBright else spec.textPrimary,
            fontSize = 15.sp,
            fontWeight = if (isSelected || isRecommendedHighlight) FontWeight.Medium else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = method.cycles.toString(),
            fontFamily = JetBrainsMonoFontFamily,
            color = if (isRecommendedHighlight) spec.accentBright else spec.textPrimary,
            fontSize = 14.sp,
            textAlign = TextAlign.End,
            modifier = Modifier.width(28.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .clickable(onClick = onSelect),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .border(
                        width = 2.dp,
                        color = when {
                            isSelected -> spec.accentBright
                            isRecommendedHighlight -> spec.accentBright
                            else -> spec.textPrimary
                        },
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(11.dp)
                            .clip(CircleShape)
                            .background(spec.accentBright)
                    )
                }
            }
        }
    }
}

@Composable
private fun MethodSpecificationDialog(
    method: ErasureMethod,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDismiss: () -> Unit
) {
    val spec = LocalAppThemeSpec.current
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = spec.dialogShape,
        containerColor = spec.surfaceCard,
        titleContentColor = spec.textPrimary,
        textContentColor = spec.textSecondary,
        title = {
            Column {
                Surface(
                    shape = spec.badgeShape,
                    color = spec.accentPrimary.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, spec.accentBright)
                ) {
                    Text(
                        text = "${method.tier.badge} • ${method.cycles} ${if (method.cycles == 1) "Cycle" else "Cycles"}",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        color = spec.accentBright,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = method.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = spec.textPrimary
                )
            }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("Authority / Origin", style = MaterialTheme.typography.labelLarge, color = spec.textPrimary)
                Text(method.origin, style = MaterialTheme.typography.bodyMedium, color = spec.textSecondary)
                Spacer(modifier = Modifier.height(10.dp))
                Text("Technical Specification", style = MaterialTheme.typography.labelLarge, color = spec.textPrimary)
                Text(method.description, style = MaterialTheme.typography.bodyMedium, color = spec.textSecondary)
                Spacer(modifier = Modifier.height(12.dp))
                Text("Overwrite Pass Sequence", style = MaterialTheme.typography.labelLarge, color = spec.textPrimary)
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = spec.badgeShape,
                    color = spec.surfaceElevated,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        method.passes.take(12).forEachIndexed { idx, pass ->
                            Text(
                                text = "Pass ${idx + 1}: ${pass.label}",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                color = spec.textPrimary
                            )
                        }
                        if (method.passes.size > 12) {
                            Text(
                                text = "... + ${method.passes.size - 12} additional pattern passes",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                color = spec.accentBright
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSelect) {
                Text(
                    text = if (isSelected) "SELECTED" else "SELECT METHOD",
                    color = spec.accentBright,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CLOSE", color = spec.textSecondary)
            }
        }
    )
}
