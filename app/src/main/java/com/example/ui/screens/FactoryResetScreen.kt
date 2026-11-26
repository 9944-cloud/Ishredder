package com.example.ui.screens

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppSettings
import com.example.data.MountedFolderEntity
import com.example.model.ErasureCatalog
import com.example.model.ShredExecutionState
import com.example.model.StorageOverview
import com.example.ui.components.ShredderHeroGraphic
import com.example.ui.components.StandardTitleTopBar
import com.example.ui.components.themedScreenBackground
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalAppThemeSpec
import com.example.util.FormatUtils

private enum class FactoryResetMode {
    DEVICE_RESET,
    APP_RESET
}

@Composable
fun FactoryResetScreen(
    settings: AppSettings,
    storageOverview: StorageOverview,
    mountedFolders: List<MountedFolderEntity>,
    contactsPermissionGranted: Boolean,
    executionState: ShredExecutionState,
    reportCount: Int,
    usdtPurchaseCount: Int,
    onContactsPermissionResult: (Boolean) -> Unit,
    onMountFolder: (Uri) -> Unit,
    onStartDevicePreReset: (
        methodId: String,
        includeFreeSpace: Boolean,
        includeMountedFolders: Boolean,
        includeCacheAndResidual: Boolean,
        includeClipboard: Boolean,
        includeContacts: Boolean
    ) -> Unit,
    onCancelShredding: () -> Unit,
    onViewReport: (Long) -> Unit,
    onPerformAppFactoryReset: (clearUsdtReceipts: Boolean) -> Unit,
    onResetExecutionState: () -> Unit,
    onBack: () -> Unit
) {
    val spec = LocalAppThemeSpec.current
    val context = LocalContext.current

    var mode by remember { mutableStateOf(FactoryResetMode.DEVICE_RESET) }
    var selectedMethodId by remember(settings.defaultMethodId) { mutableStateOf(settings.defaultMethodId) }
    var includeFreeSpace by remember { mutableStateOf(true) }
    var includeMountedFolders by remember { mutableStateOf(true) }
    var includeCacheAndResidual by remember { mutableStateOf(true) }
    var includeClipboard by remember { mutableStateOf(true) }
    var includeContacts by remember { mutableStateOf(false) }

    var showMethodDialog by remember { mutableStateOf(false) }
    var showDeviceResetConfirm by remember { mutableStateOf(false) }
    var showAppResetConfirm by remember { mutableStateOf(false) }
    var clearUsdtReceipts by remember { mutableStateOf(false) }
    var systemLaunchNote by remember { mutableStateOf<String?>(null) }

    val selectedMethod = remember(selectedMethodId) { ErasureCatalog.findById(selectedMethodId) }

    val contactsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result[Manifest.permission.READ_CONTACTS] == true &&
            result[Manifest.permission.WRITE_CONTACTS] == true
        onContactsPermissionResult(granted)
        if (granted) includeContacts = true
    }

    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) onMountFolder(uri)
    }

    fun launchSystemResetSettings() {
        val opened = openAndroidFactoryResetSettings(context)
        if (!opened) {
            systemLaunchNote = "System Reset settings unavailable on this device. Open Settings → System → Reset options manually."
        } else {
            systemLaunchNote = null
        }
    }

    BackHandler(enabled = executionState !is ShredExecutionState.Idle) {
        if (executionState !is ShredExecutionState.Running) {
            onResetExecutionState()
            onBack()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .themedScreenBackground()
            .navigationBarsPadding()
    ) {
        StandardTitleTopBar(
            title = "Factory Reset",
            onBack = {
                if (executionState !is ShredExecutionState.Running) {
                    onResetExecutionState()
                    onBack()
                }
            }
        )

        if (executionState is ShredExecutionState.Idle) {
            // Mode Switcher Bar
            Surface(
                shape = spec.stepperShape,
                color = spec.surfaceCard,
                border = BorderStroke(spec.cardBorderWidth, spec.cardBorderColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Row(modifier = Modifier.padding(4.dp)) {
                    FactoryResetModeChip(
                        label = "Device Reset",
                        selected = mode == FactoryResetMode.DEVICE_RESET,
                        onClick = { mode = FactoryResetMode.DEVICE_RESET },
                        testTag = "mode_device_reset",
                        modifier = Modifier.weight(1f)
                    )
                    FactoryResetModeChip(
                        label = "App Reset",
                        selected = mode == FactoryResetMode.APP_RESET,
                        onClick = { mode = FactoryResetMode.APP_RESET },
                        testTag = "mode_app_reset",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        when {
            executionState is ShredExecutionState.Running -> {
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
                    ShredderHeroGraphic(isAnimated = true, sizeDp = 176.dp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "${(fraction * 100).toInt()}%",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 34.sp,
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
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        shape = spec.cardShape,
                        color = spec.surfaceCard,
                        border = BorderStroke(spec.cardBorderWidth, spec.cardBorderColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Pre-Reset Wipe: ${executionState.currentItemName}",
                                style = MaterialTheme.typography.titleSmall,
                                color = spec.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Target ${executionState.currentItemIndex}/${executionState.totalItems} • Pass ${executionState.currentPass}/${executionState.totalPasses}",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 12.sp,
                                color = spec.accentBright
                            )
                            Text(
                                text = "${executionState.passLabel} • ETA ${FormatUtils.formatEta(executionState.etaSeconds)}",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                color = spec.textSecondary
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancelShredding,
                        shape = spec.buttonShape,
                        border = BorderStroke(1.dp, spec.danger),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("factory_reset_abort_button")
                    ) {
                        Icon(Icons.Filled.Cancel, contentDescription = null, tint = spec.danger)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ABORT PRE-RESET WIPE", color = spec.danger, fontWeight = FontWeight.Bold)
                    }
                }
            }

            executionState is ShredExecutionState.Completed -> {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
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
                        text = if (executionState.wasCancelled) "Pre-Reset Wipe Cancelled" else "Device Ready for Factory Reset",
                        style = MaterialTheme.typography.headlineMedium,
                        color = spec.textPrimary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Overwrote ${FormatUtils.formatBytes(executionState.totalBytesShredded)} using ${executionState.methodName}. Proceed to Android Reset Settings to wipe the OS.",
                        style = MaterialTheme.typography.bodySmall,
                        color = spec.textSecondary,
                        textAlign = TextAlign.Center
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
                            ResetStatRow("Certificate ID", executionState.reportCode)
                            ResetStatRow("Targets Shredded", "${executionState.itemsShredded}")
                            ResetStatRow("SHA-256 Digest", executionState.verificationSha256.take(24) + "...")
                        }
                    }

                    if (systemLaunchNote != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(systemLaunchNote!!, style = MaterialTheme.typography.bodySmall, color = spec.warning)
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (executionState.reportId != null) {
                        OutlinedButton(
                            onClick = { onViewReport(executionState.reportId) },
                            shape = spec.buttonShape,
                            border = BorderStroke(1.dp, spec.accentBright),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Icon(Icons.Filled.ReceiptLong, contentDescription = null, tint = spec.accentBright)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("VIEW VERIFICATION REPORT", color = spec.accentBright, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Button(
                        onClick = { launchSystemResetSettings() },
                        shape = spec.buttonShape,
                        colors = ButtonDefaults.buttonColors(containerColor = spec.danger, contentColor = Color.White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("open_android_factory_reset_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("OPEN ANDROID FACTORY RESET", fontWeight = FontWeight.Bold)
                    }
                    TextButton(
                        onClick = {
                            onResetExecutionState()
                            onBack()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("DONE", color = spec.textSecondary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            mode == FactoryResetMode.DEVICE_RESET -> {
                val anySelected = includeFreeSpace ||
                    (includeMountedFolders && mountedFolders.isNotEmpty()) ||
                    includeCacheAndResidual ||
                    includeClipboard ||
                    (includeContacts && contactsPermissionGranted)

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Danger Notice
                    Surface(
                        shape = spec.cardShape,
                        color = spec.dangerTint,
                        border = BorderStroke(1.dp, spec.danger.copy(alpha = 0.65f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.WarningAmber,
                                contentDescription = null,
                                tint = spec.danger,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Military-grade pre-reset sanitization: securely overwrite free space, mounted storage folders, cache, and contacts before launching Android System Factory Reset.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFFDAD6),
                                lineHeight = 18.sp
                            )
                        }
                    }

                    // Targets Card
                    Surface(
                        shape = spec.cardShape,
                        color = spec.surfaceCard,
                        border = BorderStroke(spec.cardBorderWidth, spec.cardBorderColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            val freeLabel = if (settings.freeSpaceOverwriteMb <= 0L) {
                                "Safe Max (${FormatUtils.formatIecBytes(storageOverview.availableBytes)} free)"
                            } else {
                                "${settings.freeSpaceOverwriteMb} MB unallocated block sweep"
                            }
                            ResetToggleRow(
                                title = "Free Space Overwrite",
                                subtitle = freeLabel,
                                checked = includeFreeSpace,
                                onCheckedChange = { includeFreeSpace = it },
                                testTag = "reset_toggle_free_space"
                            )
                            HorizontalDivider(color = spec.surfaceDivider.copy(alpha = 0.45f))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { includeMountedFolders = !includeMountedFolders }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Mounted Folders (SD / Storage)",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = spec.textPrimary
                                    )
                                    Text(
                                        text = if (mountedFolders.isEmpty()) {
                                            "No external/shared folders mounted yet"
                                        } else {
                                            "${mountedFolders.size} folder(s): ${mountedFolders.joinToString { it.displayName }}"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = spec.textSecondary,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                TextButton(onClick = { folderPicker.launch(null) }) {
                                    Icon(Icons.Filled.CreateNewFolder, contentDescription = null, tint = spec.accentBright, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Mount", color = spec.accentBright, fontSize = 12.sp)
                                }
                                Checkbox(
                                    checked = includeMountedFolders,
                                    onCheckedChange = { includeMountedFolders = it },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = spec.accentPrimary,
                                        uncheckedColor = spec.textSecondary,
                                        checkmarkColor = Color.White
                                    )
                                )
                            }
                            HorizontalDivider(color = spec.surfaceDivider.copy(alpha = 0.45f))

                            ResetToggleRow(
                                title = "Cache & Residual Sandbox Files",
                                subtitle = FormatUtils.formatIecBytes(storageOverview.cacheBytes + storageOverview.residualBytes).ifEmpty { "Clean" },
                                checked = includeCacheAndResidual,
                                onCheckedChange = { includeCacheAndResidual = it },
                                testTag = "reset_toggle_cache"
                            )
                            HorizontalDivider(color = spec.surfaceDivider.copy(alpha = 0.45f))

                            ResetToggleRow(
                                title = "System Clipboard",
                                subtitle = "CSPRNG overwrite & clear clipboard buffer",
                                checked = includeClipboard,
                                onCheckedChange = { includeClipboard = it },
                                testTag = "reset_toggle_clipboard"
                            )
                            HorizontalDivider(color = spec.surfaceDivider.copy(alpha = 0.45f))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (!contactsPermissionGranted) {
                                            contactsLauncher.launch(
                                                arrayOf(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS)
                                            )
                                        } else {
                                            includeContacts = !includeContacts
                                        }
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Contacts Address Book", style = MaterialTheme.typography.titleSmall, color = spec.textPrimary)
                                    Text(
                                        text = if (contactsPermissionGranted) "Overwrite & remove device contacts" else "Tap to grant Contacts permission",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (contactsPermissionGranted) spec.textSecondary else spec.warning
                                    )
                                }
                                Checkbox(
                                    checked = includeContacts && contactsPermissionGranted,
                                    onCheckedChange = { want ->
                                        if (!contactsPermissionGranted) {
                                            contactsLauncher.launch(
                                                arrayOf(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS)
                                            )
                                        } else {
                                            includeContacts = want
                                        }
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = spec.accentPrimary,
                                        uncheckedColor = spec.textSecondary,
                                        checkmarkColor = Color.White
                                    )
                                )
                            }
                        }
                    }

                    // Erasure Method Card
                    Surface(
                        shape = spec.cardShape,
                        color = spec.surfaceCard,
                        border = BorderStroke(spec.cardBorderWidth, spec.cardBorderColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showMethodDialog = true }
                            .testTag("reset_method_picker")
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Erasure Method", style = MaterialTheme.typography.labelMedium, color = spec.textSecondary)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(selectedMethod.name, style = MaterialTheme.typography.titleMedium, color = spec.textPrimary)
                                Text(
                                    text = "${selectedMethod.tier.badge} • ${selectedMethod.cycles} passes • ${selectedMethod.origin}",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 11.sp,
                                    color = spec.accentBright
                                )
                            }
                            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = spec.textSecondary)
                        }
                    }

                    if (systemLaunchNote != null) {
                        Text(systemLaunchNote!!, style = MaterialTheme.typography.bodySmall, color = spec.warning)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showDeviceResetConfirm = true },
                        enabled = anySelected,
                        shape = spec.buttonShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = spec.danger,
                            contentColor = Color.White,
                            disabledContainerColor = spec.surfaceElevated,
                            disabledContentColor = spec.textMuted
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("start_device_pre_reset_button")
                    ) {
                        Icon(Icons.Filled.DeleteForever, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SHRED DATA & PREPARE RESET", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { launchSystemResetSettings() },
                        shape = spec.buttonShape,
                        border = BorderStroke(1.dp, spec.surfaceDivider),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("open_system_reset_direct_button")
                    ) {
                        Icon(Icons.Filled.PhoneAndroid, contentDescription = null, tint = spec.textSecondary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open Android System Reset Settings", color = spec.textPrimary, fontSize = 13.sp)
                    }
                }
            }

            else -> {
                // APP FACTORY RESET MODE
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = spec.cardShape,
                        color = spec.surfaceCard,
                        border = BorderStroke(spec.cardBorderWidth, spec.cardBorderColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.RestartAlt,
                                    contentDescription = null,
                                    tint = spec.danger,
                                    modifier = Modifier.size(26.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Reset iShredder to Factory Defaults",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = spec.textPrimary
                                )
                            }
                            Text(
                                text = "This permanently wipes iShredder local state while leaving your device OS intact:",
                                style = MaterialTheme.typography.bodySmall,
                                color = spec.textSecondary
                            )
                            ResetStatRow("Preferences & Passcode", "Reset PIN lock, Theme UI, Language & wallet")
                            ResetStatRow("Mounted SAF Folders", "${mountedFolders.size} folder permission(s) released")
                            ResetStatRow("Erasure Certificates", "$reportCount local audit report(s) deleted")
                            ResetStatRow("Sandbox Cache & Files", "Overwritten & purged")
                        }
                    }

                    Surface(
                        shape = spec.cardShape,
                        color = spec.surfaceCard,
                        border = BorderStroke(spec.cardBorderWidth, spec.cardBorderColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { clearUsdtReceipts = !clearUsdtReceipts }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Clear Saved USDT Receipts",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = spec.textPrimary
                                )
                                Text(
                                    text = "$usdtPurchaseCount verified on-chain receipt(s) stored locally",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = spec.textSecondary
                                )
                            }
                            Switch(
                                checked = clearUsdtReceipts,
                                onCheckedChange = { clearUsdtReceipts = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = spec.danger,
                                    uncheckedThumbColor = spec.textSecondary,
                                    uncheckedTrackColor = spec.surfaceElevated
                                )
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Button(
                        onClick = { showAppResetConfirm = true },
                        shape = spec.buttonShape,
                        colors = ButtonDefaults.buttonColors(containerColor = spec.danger, contentColor = Color.White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("perform_app_factory_reset_button")
                    ) {
                        Icon(Icons.Filled.RestartAlt, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("RESET APP TO FACTORY DEFAULTS", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showMethodDialog) {
        AlertDialog(
            onDismissRequest = { showMethodDialog = false },
            shape = spec.dialogShape,
            containerColor = spec.surfaceCard,
            titleContentColor = spec.textPrimary,
            title = { Text("Pre-Reset Erasure Method") },
            text = {
                LazyColumn(modifier = Modifier.height(340.dp)) {
                    items(ErasureCatalog.allMethods, key = { it.id }) { m ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedMethodId = m.id
                                    showMethodDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = m.id == selectedMethodId,
                                onClick = {
                                    selectedMethodId = m.id
                                    showMethodDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = spec.accentBright)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(m.name, color = spec.textPrimary, fontSize = 14.sp)
                                Text(
                                    "${m.tier.badge} • ${m.cycles} ${if (m.cycles == 1) "pass" else "passes"}",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 11.sp,
                                    color = spec.textSecondary
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMethodDialog = false }) {
                    Text("CLOSE", color = spec.textSecondary)
                }
            }
        )
    }

    if (showDeviceResetConfirm) {
        var acknowledged by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showDeviceResetConfirm = false },
            shape = spec.dialogShape,
            containerColor = spec.surfaceCard,
            titleContentColor = spec.textPrimary,
            title = { Text("Confirm Pre-Reset Sanitization") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Selected storage, free space, and privacy buffers will be permanently overwritten using ${selectedMethod.name} (${selectedMethod.cycles} passes).",
                        style = MaterialTheme.typography.bodySmall,
                        color = spec.textSecondary
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { acknowledged = !acknowledged },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = acknowledged,
                            onCheckedChange = { acknowledged = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = spec.danger,
                                uncheckedColor = spec.textPrimary,
                                checkmarkColor = Color.White
                            ),
                            modifier = Modifier.testTag("device_reset_ack_checkbox")
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "I understand erased data cannot be recovered.",
                            style = MaterialTheme.typography.bodySmall,
                            color = spec.textPrimary
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = acknowledged,
                    onClick = {
                        showDeviceResetConfirm = false
                        onStartDevicePreReset(
                            selectedMethodId,
                            includeFreeSpace,
                            includeMountedFolders,
                            includeCacheAndResidual,
                            includeClipboard,
                            includeContacts
                        )
                    },
                    modifier = Modifier.testTag("confirm_device_pre_reset_button")
                ) {
                    Text(
                        "SHRED NOW",
                        color = if (acknowledged) spec.danger else spec.textMuted,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeviceResetConfirm = false }) {
                    Text("CANCEL", color = spec.textSecondary)
                }
            }
        )
    }

    if (showAppResetConfirm) {
        AlertDialog(
            onDismissRequest = { showAppResetConfirm = false },
            shape = spec.dialogShape,
            containerColor = spec.surfaceCard,
            titleContentColor = spec.textPrimary,
            title = { Text("Confirm App Factory Reset") },
            text = {
                Text(
                    text = "Reset all iShredder settings, passcode lock, mounted folders, and $reportCount erasure report(s)?",
                    style = MaterialTheme.typography.bodySmall,
                    color = spec.textSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showAppResetConfirm = false
                        onPerformAppFactoryReset(clearUsdtReceipts)
                    },
                    modifier = Modifier.testTag("confirm_app_factory_reset_button")
                ) {
                    Text("RESET APP", color = spec.danger, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAppResetConfirm = false }) {
                    Text("CANCEL", color = spec.textSecondary)
                }
            }
        )
    }
}

@Composable
private fun FactoryResetModeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val spec = LocalAppThemeSpec.current
    Surface(
        onClick = onClick,
        shape = spec.stepperShape,
        color = if (selected) spec.accentPrimary else Color.Transparent,
        modifier = modifier
            .height(48.dp)
            .testTag(testTag)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (selected) Color.White else spec.textSecondary
            )
        }
    }
}

@Composable
private fun ResetToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    val spec = LocalAppThemeSpec.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = spec.textPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = spec.textSecondary)
        }
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = spec.accentPrimary,
                uncheckedColor = spec.textSecondary,
                checkmarkColor = Color.White
            )
        )
    }
}

@Composable
private fun ResetStatRow(label: String, value: String) {
    val spec = LocalAppThemeSpec.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = spec.textSecondary)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 11.sp,
            color = spec.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun openAndroidFactoryResetSettings(context: Context): Boolean {
    val candidates = listOf(
        Intent(Settings.ACTION_PRIVACY_SETTINGS),
        Intent(Settings.ACTION_SETTINGS)
    )
    for (intent in candidates) {
        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            return true
        } catch (_: ActivityNotFoundException) {
        }
    }
    return false
}
