package com.example.ui.screens

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.R
import com.example.data.AppLanguage
import com.example.data.AppSettings
import com.example.data.UsdtPurchaseEntity
import com.example.model.ErasureCatalog
import com.example.model.UsdtIapCatalog
import com.example.model.UsdtIapItem
import com.example.model.UsdtNetwork
import com.example.ui.components.StandardTitleTopBar
import com.example.ui.components.themedScreenBackground
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalAppThemeSpec
import com.example.util.FormatUtils
import com.example.viewmodel.UsdtVerificationUiState

@Composable
fun SettingsRootScreen(
    activeThemeTitle: String,
    onBack: () -> Unit,
    onOpenThemeUi: () -> Unit,
    onOpenSecuritySuite2026: () -> Unit,
    onOpenGeneral: () -> Unit,
    onOpenErasure: () -> Unit,
    onOpenReportHistory: () -> Unit,
    onOpenSubscription: () -> Unit,
    onOpenFactoryReset: () -> Unit
) {
    val spec = LocalAppThemeSpec.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .themedScreenBackground()
            .navigationBarsPadding()
    ) {
        StandardTitleTopBar(
            title = stringResource(R.string.settings_title),
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            // Theme UI Featured Card (iOS 27, Android 17, HyperOS 4, ColorOS 17, M3 Expressive)
            Surface(
                shape = spec.cardShape,
                color = spec.surfaceCard,
                border = BorderStroke(spec.cardBorderWidth, spec.accentBright.copy(alpha = 0.45f)),
                tonalElevation = spec.tonalElevation,
                shadowElevation = spec.shadowElevation,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenThemeUi)
                        .padding(horizontal = 18.dp, vertical = 18.dp)
                        .testTag("settings_theme_row"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = spec.badgeShape,
                        color = spec.accentTintBg,
                        border = BorderStroke(1.dp, spec.accentBright.copy(alpha = 0.5f)),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Palette,
                            contentDescription = null,
                            tint = spec.accentBright,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.settings_theme_ui),
                                style = MaterialTheme.typography.titleMedium,
                                color = spec.textPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = spec.pillShape,
                                color = spec.accentPrimary
                            ) {
                                Text(
                                    text = activeThemeTitle,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "iOS 27 • Android 17 • HyperOS 4 • ColorOS 17 • M3 Expressive • OLED",
                            style = MaterialTheme.typography.bodySmall,
                            color = spec.textSecondary,
                            lineHeight = 18.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(
                        imageVector = Icons.Filled.ChevronRight,
                        contentDescription = null,
                        tint = spec.textPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Card 1: General + Erasure
            Surface(
                shape = spec.cardShape,
                color = spec.surfaceCard,
                border = BorderStroke(spec.cardBorderWidth, spec.cardBorderColor),
                tonalElevation = spec.tonalElevation,
                shadowElevation = spec.shadowElevation,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    SettingsMenuItem(
                        title = stringResource(R.string.settings_general),
                        subtitle = stringResource(R.string.settings_general_sub),
                        testTag = "settings_general_row",
                        onClick = onOpenGeneral
                    )
                    HorizontalDivider(color = spec.surfaceDivider.copy(alpha = 0.5f), thickness = 0.8.dp)
                    SettingsMenuItem(
                        title = stringResource(R.string.settings_erasure),
                        subtitle = stringResource(R.string.settings_erasure_sub),
                        testTag = "settings_erasure_row",
                        onClick = onOpenErasure
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Card 2: Erasure Report History + Manage Subscription
            Surface(
                shape = spec.cardShape,
                color = spec.surfaceCard,
                border = BorderStroke(spec.cardBorderWidth, spec.cardBorderColor),
                tonalElevation = spec.tonalElevation,
                shadowElevation = spec.shadowElevation,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    SettingsMenuItem(
                        title = stringResource(R.string.settings_report_history),
                        subtitle = null,
                        testTag = "settings_reports_row",
                        onClick = onOpenReportHistory
                    )
                    HorizontalDivider(color = spec.surfaceDivider.copy(alpha = 0.5f), thickness = 0.8.dp)
                    SettingsMenuItem(
                        title = stringResource(R.string.settings_manage_subscription),
                        subtitle = "In-app purchases (\$1.49 – \$39.99) • USDT only",
                        testTag = "settings_subscription_row",
                        onClick = onOpenSubscription
                    )
                    HorizontalDivider(color = spec.surfaceDivider.copy(alpha = 0.5f), thickness = 0.8.dp)
                    SettingsMenuItem(
                        title = stringResource(R.string.settings_security_2026),
                        subtitle = "Privacy Radar • EXIF GPS Scrubber • Auto-Guard • Speed Lab",
                        testTag = "settings_2026_suite_row",
                        onClick = onOpenSecuritySuite2026
                    )
                    HorizontalDivider(color = spec.surfaceDivider.copy(alpha = 0.5f), thickness = 0.8.dp)
                    SettingsMenuItem(
                        title = stringResource(R.string.settings_factory_reset),
                        subtitle = "Pre-reset device shred & reset app defaults",
                        testTag = "settings_factory_reset_row",
                        onClick = onOpenFactoryReset
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stable Release Build Card
            Surface(
                shape = spec.cardShape,
                color = spec.surfaceCard,
                border = BorderStroke(spec.cardBorderWidth, spec.cardBorderColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_release_info_card")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.VerifiedUser,
                        contentDescription = null,
                        tint = spec.accentBright,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "iShredder Excellent • Full Version Stable",
                            style = MaterialTheme.typography.titleSmall,
                            color = spec.textPrimary
                        )
                        Text(
                            text = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) • ${ErasureCatalog.allMethods.size} Algorithms • Android 5.0–17",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.5.sp,
                            color = spec.textSecondary
                        )
                        Text(
                            text = BuildConfig.APPLICATION_ID,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            color = spec.textMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SettingsMenuItem(
    title: String,
    subtitle: String?,
    testTag: String,
    onClick: () -> Unit
) {
    val spec = LocalAppThemeSpec.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 18.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = spec.textPrimary
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = spec.textSecondary,
                    lineHeight = 18.sp
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = spec.textPrimary,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
fun GeneralSettingsScreen(
    settings: AppSettings,
    onBack: () -> Unit,
    onEnablePasscode: (String) -> Unit,
    onDisablePasscode: () -> Unit,
    onSelectLanguage: (String) -> Unit
) {
    val spec = LocalAppThemeSpec.current
    var showPinDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .themedScreenBackground()
            .navigationBarsPadding()
    ) {
        StandardTitleTopBar(title = "General", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = spec.cardShape,
                color = spec.surfaceCard,
                border = BorderStroke(spec.cardBorderWidth, spec.cardBorderColor),
                tonalElevation = spec.tonalElevation,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (settings.passcodeEnabled) {
                                    onDisablePasscode()
                                } else {
                                    showPinDialog = true
                                }
                            }
                            .padding(horizontal = 18.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Passcode", style = MaterialTheme.typography.titleMedium, color = spec.textPrimary)
                            Text(
                                text = if (settings.passcodeEnabled) "4-digit PIN lock active" else "Protect app access with a 4-digit PIN",
                                style = MaterialTheme.typography.bodySmall,
                                color = spec.textSecondary
                            )
                        }
                        Switch(
                            checked = settings.passcodeEnabled,
                            onCheckedChange = { checked ->
                                if (checked) showPinDialog = true else onDisablePasscode()
                            },
                            colors = shredSwitchColors()
                        )
                    }

                    if (settings.passcodeEnabled) {
                        HorizontalDivider(color = spec.surfaceDivider.copy(alpha = 0.5f))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showPinDialog = true }
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Change Passcode",
                                style = MaterialTheme.typography.bodyMedium,
                                color = spec.accentBright,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = spec.textSecondary)
                        }
                    }

                    HorizontalDivider(color = spec.surfaceDivider.copy(alpha = 0.5f))

                    val currentLang = AppLanguage.entries.firstOrNull { it.code == settings.languageCode } ?: AppLanguage.ENGLISH
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showLanguageDialog = true }
                            .padding(horizontal = 18.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Language", style = MaterialTheme.typography.titleMedium, color = spec.textPrimary)
                            Text(
                                text = currentLang.label,
                                style = MaterialTheme.typography.bodySmall,
                                color = spec.accentBright
                            )
                        }
                        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = spec.textSecondary)
                    }
                }
            }
        }
    }

    if (showPinDialog) {
        var pinInput by remember { mutableStateOf("") }
        var pinError by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            shape = spec.dialogShape,
            containerColor = spec.surfaceCard,
            titleContentColor = spec.textPrimary,
            title = { Text("Set 4-Digit Passcode") },
            text = {
                Column {
                    Text(
                        "Enter a 4-digit PIN required when opening iShredder Excellent.",
                        style = MaterialTheme.typography.bodySmall,
                        color = spec.textSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) pinInput = it },
                        label = { Text("4-Digit PIN") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = spec.accentBright,
                            unfocusedBorderColor = spec.surfaceDivider,
                            focusedLabelColor = spec.accentBright,
                            unfocusedLabelColor = spec.textSecondary,
                            focusedTextColor = spec.textPrimary,
                            unfocusedTextColor = spec.textPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (pinError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(pinError!!, color = spec.danger, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (pinInput.length == 4) {
                            onEnablePasscode(pinInput)
                            showPinDialog = false
                        } else {
                            pinError = "Please enter exactly 4 digits."
                        }
                    }
                ) {
                    Text("SAVE PIN", color = spec.accentBright, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) {
                    Text("CANCEL", color = spec.textSecondary)
                }
            }
        )
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            shape = spec.dialogShape,
            containerColor = spec.surfaceCard,
            titleContentColor = spec.textPrimary,
            title = { Text("Select Language") },
            text = {
                Column {
                    AppLanguage.entries.forEach { lang ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectLanguage(lang.code)
                                    showLanguageDialog = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = settings.languageCode == lang.code,
                                onClick = {
                                    onSelectLanguage(lang.code)
                                    showLanguageDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = spec.accentBright)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(lang.label, color = spec.textPrimary)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text("CLOSE", color = spec.textSecondary)
                }
            }
        )
    }
}

@Composable
fun ErasureSettingsScreen(
    settings: AppSettings,
    onBack: () -> Unit,
    onSetSkipScan: (Boolean) -> Unit,
    onSetDefaultMethod: (String) -> Unit,
    onSetFreeSpaceMb: (Long) -> Unit,
    onSetEnableReport: (Boolean) -> Unit,
    onSetThumbnailPreview: (Boolean) -> Unit,
    onSetShowHidden: (Boolean) -> Unit
) {
    val spec = LocalAppThemeSpec.current
    var showMethodDialog by remember { mutableStateOf(false) }
    var showFreeSpaceDialog by remember { mutableStateOf(false) }
    val defaultMethod = remember(settings.defaultMethodId) { ErasureCatalog.findById(settings.defaultMethodId) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .themedScreenBackground()
            .navigationBarsPadding()
    ) {
        StandardTitleTopBar(title = "Erasure", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = spec.cardShape,
                color = spec.surfaceCard,
                border = BorderStroke(spec.cardBorderWidth, spec.cardBorderColor),
                tonalElevation = spec.tonalElevation,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    ErasureSwitchRow(
                        title = "Skip scan",
                        subtitle = "Skip pre-scan prompt and enter target selection immediately",
                        checked = settings.skipScan,
                        onCheckedChange = onSetSkipScan
                    )
                    HorizontalDivider(color = spec.surfaceDivider.copy(alpha = 0.5f))

                    // Default method
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showMethodDialog = true }
                            .padding(horizontal = 18.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Default method", style = MaterialTheme.typography.titleMedium, color = spec.textPrimary)
                            Text(
                                text = "${defaultMethod.name} (${defaultMethod.cycles} cycles)",
                                style = MaterialTheme.typography.bodySmall,
                                color = spec.accentBright
                            )
                        }
                        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = spec.textSecondary)
                    }
                    HorizontalDivider(color = spec.surfaceDivider.copy(alpha = 0.5f))

                    // Free Space Overwrite
                    val freeLabel = if (settings.freeSpaceOverwriteMb <= 0L) "Auto Safe Max" else "${settings.freeSpaceOverwriteMb} MB"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showFreeSpaceDialog = true }
                            .padding(horizontal = 18.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Free Space Overwrite", style = MaterialTheme.typography.titleMedium, color = spec.textPrimary)
                            Text(
                                text = "Target block size: $freeLabel",
                                style = MaterialTheme.typography.bodySmall,
                                color = spec.accentBright
                            )
                        }
                        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = spec.textSecondary)
                    }
                    HorizontalDivider(color = spec.surfaceDivider.copy(alpha = 0.5f))

                    ErasureSwitchRow(
                        title = "Enable Erasure Report",
                        subtitle = "Save signed local SHA-256 deletion certificates",
                        checked = settings.enableErasureReport,
                        onCheckedChange = onSetEnableReport
                    )
                    HorizontalDivider(color = spec.surfaceDivider.copy(alpha = 0.5f))

                    ErasureSwitchRow(
                        title = "Thumbnail preview",
                        subtitle = "Render media thumbnails in File Explorer and Queue",
                        checked = settings.thumbnailPreview,
                        onCheckedChange = onSetThumbnailPreview
                    )
                    HorizontalDivider(color = spec.surfaceDivider.copy(alpha = 0.5f))

                    ErasureSwitchRow(
                        title = "Hidden files & folders",
                        subtitle = "Display dotfiles and hidden system entries in Explorer",
                        checked = settings.showHiddenFiles,
                        onCheckedChange = onSetShowHidden
                    )
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
            title = { Text("Default Erasure Method") },
            text = {
                LazyColumn(modifier = Modifier.height(360.dp)) {
                    items(ErasureCatalog.allMethods, key = { it.id }) { m ->
                        val selected = m.id == settings.defaultMethodId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSetDefaultMethod(m.id)
                                    showMethodDialog = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selected,
                                onClick = {
                                    onSetDefaultMethod(m.id)
                                    showMethodDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = spec.accentBright)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(m.name, color = spec.textPrimary, fontSize = 14.sp)
                                Text(
                                    "${m.tier.badge} • ${m.cycles} ${if (m.cycles == 1) "cycle" else "cycles"}",
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

    if (showFreeSpaceDialog) {
        val options = listOf(
            64L to "64 MB (Fast Sweep)",
            128L to "128 MB (Balanced)",
            256L to "256 MB (Standard)",
            512L to "512 MB (Deep)",
            1024L to "1 GB (Extended)",
            0L to "Auto Safe Max (Up to 1 GB/pass)"
        )
        AlertDialog(
            onDismissRequest = { showFreeSpaceDialog = false },
            shape = spec.dialogShape,
            containerColor = spec.surfaceCard,
            titleContentColor = spec.textPrimary,
            title = { Text("Free Space Overwrite Size") },
            text = {
                Column {
                    options.forEach { (mb, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSetFreeSpaceMb(mb)
                                    showFreeSpaceDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = settings.freeSpaceOverwriteMb == mb,
                                onClick = {
                                    onSetFreeSpaceMb(mb)
                                    showFreeSpaceDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = spec.accentBright)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(label, color = spec.textPrimary, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFreeSpaceDialog = false }) {
                    Text("CLOSE", color = spec.textSecondary)
                }
            }
        )
    }
}

@Composable
private fun ErasureSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val spec = LocalAppThemeSpec.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = spec.textPrimary)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = spec.textSecondary)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = shredSwitchColors()
        )
    }
}

@Composable
private fun shredSwitchColors() = LocalAppThemeSpec.current.let { spec ->
    SwitchDefaults.colors(
        checkedThumbColor = Color.White,
        checkedTrackColor = spec.accentPrimary,
        uncheckedThumbColor = spec.textSecondary,
        uncheckedTrackColor = spec.surfaceElevated
    )
}

@Composable
fun SubscriptionScreen(
    settings: AppSettings,
    purchases: List<UsdtPurchaseEntity>,
    verificationState: UsdtVerificationUiState,
    onSaveMerchantAddress: (String) -> Unit,
    onVerifyUsdtPurchase: (UsdtIapItem, UsdtNetwork, String, String) -> Unit,
    onResetVerificationState: () -> Unit,
    onBack: () -> Unit
) {
    val spec = LocalAppThemeSpec.current
    val context = LocalContext.current
    var selectedNetwork by remember { mutableStateOf(UsdtNetwork.TRC20) }
    var merchantWalletInput by remember(settings.usdtMerchantAddress) {
        mutableStateOf(settings.usdtMerchantAddress)
    }
    var checkoutItem by remember { mutableStateOf<UsdtIapItem?>(null) }
    var txHashInput by remember { mutableStateOf("") }
    var walletActionNote by remember { mutableStateOf<String?>(null) }

    val ownedByItemId = remember(purchases) {
        purchases.groupBy { it.itemId }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .themedScreenBackground()
            .navigationBarsPadding()
    ) {
        StandardTitleTopBar(title = "Manage Subscription", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Hero License + Compatibility + Payment Banner
            Surface(
                shape = spec.cardShape,
                color = spec.accentTintBg,
                border = BorderStroke(1.dp, spec.accentBright),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.VerifiedUser,
                            contentDescription = null,
                            tint = spec.accentBright,
                            modifier = Modifier.size(30.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "iShredder Excellent",
                                style = MaterialTheme.typography.titleLarge,
                                color = spec.textPrimary
                            )
                            Text(
                                text = "ANDROID 5.0 – ANDROID 17 • API ${Build.VERSION.SDK_INT}",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = spec.accentBright
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        shape = spec.badgeShape,
                        color = spec.bgDeep.copy(alpha = 0.72f),
                        border = BorderStroke(1.dp, spec.accentPrimary.copy(alpha = 0.7f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AccountBalanceWallet,
                                contentDescription = null,
                                tint = spec.accentBright,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "In-App Purchases: \$1.49 – \$39.99 • Support USDT Only",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = spec.textPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Fiat cards and Google Play Billing are disabled. All license packs and items settle directly on-chain in Tether (USDT) across TRC-20, ERC-20, BEP-20, and Polygon.",
                        style = MaterialTheme.typography.bodySmall,
                        color = spec.textSecondary,
                        lineHeight = 18.sp
                    )
                }
            }

            // Receiving Treasury Wallet Config Card
            Surface(
                shape = spec.cardShape,
                color = spec.surfaceCard,
                border = BorderStroke(spec.cardBorderWidth, spec.cardBorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "USDT Receiving Wallet & Network",
                        style = MaterialTheme.typography.titleMedium,
                        color = spec.textPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        UsdtNetwork.entries.forEach { net ->
                            FilterChip(
                                selected = selectedNetwork == net,
                                onClick = { selectedNetwork = net },
                                shape = spec.pillShape,
                                label = {
                                    Text(
                                        text = net.shortCode,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 12.sp
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = spec.accentPrimary,
                                    selectedLabelColor = Color.White,
                                    containerColor = spec.surfaceElevated,
                                    labelColor = spec.textSecondary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Official ${selectedNetwork.displayName} Contract:",
                        style = MaterialTheme.typography.bodySmall,
                        color = spec.textSecondary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedNetwork.contractAddress,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            color = spec.accentBright,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                copyToClipboard(context, "USDT Contract", selectedNetwork.contractAddress)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ContentCopy,
                                contentDescription = "Copy contract address",
                                tint = spec.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = merchantWalletInput,
                        onValueChange = { merchantWalletInput = it },
                        label = {
                            Text(
                                if (selectedNetwork.isTron) "Merchant TRC-20 Address (T...)"
                                else "Merchant ${selectedNetwork.shortCode} Address (0x...)"
                            )
                        },
                        placeholder = {
                            Text(
                                if (selectedNetwork.isTron) "Enter TRON receiving wallet"
                                else "Enter 0x EVM receiving wallet",
                                color = spec.textMuted
                            )
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = spec.accentBright,
                            unfocusedBorderColor = spec.surfaceDivider,
                            focusedLabelColor = spec.accentBright,
                            unfocusedLabelColor = spec.textSecondary,
                            focusedTextColor = spec.textPrimary,
                            unfocusedTextColor = spec.textPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("usdt_merchant_address_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = { onSaveMerchantAddress(merchantWalletInput) },
                            shape = spec.pillShape,
                            border = BorderStroke(1.dp, spec.accentBright)
                        ) {
                            Text("SAVE RECEIVING ADDRESS", color = spec.accentBright, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Catalog Section Header
            Text(
                text = "IN-APP PURCHASES (\$1.49 – \$39.99 • USDT ONLY)",
                style = MaterialTheme.typography.labelLarge,
                color = spec.textSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )

            UsdtIapCatalog.items.forEach { item ->
                val verifiedReceipts = ownedByItemId[item.id].orEmpty()
                val isOwned = verifiedReceipts.isNotEmpty()
                Surface(
                    shape = spec.cardShape,
                    color = spec.surfaceCard,
                    border = BorderStroke(
                        if (isOwned) 1.5.dp else spec.cardBorderWidth,
                        if (isOwned) spec.accentBright else spec.cardBorderColor
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("iap_card_${item.id}")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = spec.badgeShape,
                                color = if (isOwned) spec.accentPrimary else spec.surfaceElevated
                            ) {
                                Text(
                                    text = item.badge,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isOwned) Color.White else spec.accentBright,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = spec.textPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            Surface(
                                shape = spec.pillShape,
                                color = spec.accentTintBg,
                                border = BorderStroke(1.dp, spec.accentPrimary)
                            ) {
                                Text(
                                    text = "$${item.usdtAmount} USDT",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = spec.accentBright,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = item.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = spec.textSecondary
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        item.highlights.forEach { perk ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = spec.accentBright,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = perk,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = spec.textPrimary
                                )
                            }
                        }

                        if (isOwned) {
                            Spacer(modifier = Modifier.height(8.dp))
                            val latest = verifiedReceipts.first()
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = spec.accentBright,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "UNLOCKED • ${latest.networkCode} ${latest.blockNumber}",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = spec.accentBright
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                txHashInput = ""
                                walletActionNote = null
                                onResetVerificationState()
                                checkoutItem = item
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isOwned) spec.surfaceElevated else spec.accentPrimary,
                                contentColor = Color.White
                            ),
                            shape = spec.buttonShape,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("pay_usdt_${item.id}")
                        ) {
                            Text(
                                text = if (isOwned) "PURCHASE ANOTHER (${item.usdtAmount} USDT)" else "PAY ${item.usdtAmount} USDT",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            if (purchases.isNotEmpty()) {
                Text(
                    text = "VERIFIED ON-CHAIN RECEIPTS (${purchases.size})",
                    style = MaterialTheme.typography.labelLarge,
                    color = spec.textSecondary,
                    modifier = Modifier.padding(top = 6.dp)
                )
                Surface(
                    shape = spec.cardShape,
                    color = spec.surfaceCard,
                    border = BorderStroke(spec.cardBorderWidth, spec.cardBorderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        purchases.forEachIndexed { idx, receipt ->
                            if (idx > 0) {
                                HorizontalDivider(color = spec.surfaceDivider.copy(alpha = 0.5f))
                            }
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = receipt.itemTitle,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = spec.textPrimary
                                    )
                                    Text(
                                        text = "${receipt.usdtAmount} USDT",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = spec.accentBright
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${receipt.networkCode} • Block ${receipt.blockNumber} • ${FormatUtils.formatTimestamp(receipt.purchasedAtMillis)}",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 11.sp,
                                    color = spec.textSecondary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Tx: ${receipt.txHash}",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 11.sp,
                                    color = spec.textMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    val activeCheckout = checkoutItem
    if (activeCheckout != null) {
        AlertDialog(
            onDismissRequest = {
                if (verificationState !is UsdtVerificationUiState.Verifying) {
                    onResetVerificationState()
                    checkoutItem = null
                }
            },
            shape = spec.dialogShape,
            containerColor = spec.surfaceCard,
            titleContentColor = spec.textPrimary,
            title = {
                Column {
                    Text(activeCheckout.title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = "Amount Due: ${activeCheckout.usdtAmount} USDT (USDT Only)",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 12.sp,
                        color = spec.accentBright
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "1. Select USDT Network",
                        style = MaterialTheme.typography.labelMedium,
                        color = spec.textSecondary
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        UsdtNetwork.entries.forEach { net ->
                            FilterChip(
                                selected = selectedNetwork == net,
                                onClick = { selectedNetwork = net },
                                shape = spec.pillShape,
                                label = { Text(net.shortCode, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = spec.accentPrimary,
                                    selectedLabelColor = Color.White,
                                    containerColor = spec.surfaceElevated,
                                    labelColor = spec.textSecondary
                                )
                            )
                        }
                    }

                    Text(
                        text = "2. Send exactly ${activeCheckout.usdtAmount} USDT (${selectedNetwork.shortCode})",
                        style = MaterialTheme.typography.labelMedium,
                        color = spec.textSecondary
                    )
                    Text(
                        text = "Contract: ${selectedNetwork.contractAddress}",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        color = spec.accentBright
                    )

                    OutlinedTextField(
                        value = merchantWalletInput,
                        onValueChange = { merchantWalletInput = it },
                        label = { Text("Receiving Wallet Address") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = spec.accentBright,
                            unfocusedBorderColor = spec.surfaceDivider,
                            focusedLabelColor = spec.accentBright,
                            unfocusedLabelColor = spec.textSecondary,
                            focusedTextColor = spec.textPrimary,
                            unfocusedTextColor = spec.textPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedButton(
                        onClick = {
                            val addr = merchantWalletInput.trim()
                            if (addr.isEmpty()) {
                                walletActionNote = "Enter the receiving USDT wallet address first."
                            } else {
                                val deepLinkUri = if (selectedNetwork.isTron) {
                                    Uri.parse("tron:$addr?amount=${activeCheckout.usdtAmount}")
                                } else {
                                    Uri.parse("ethereum:$addr")
                                }
                                try {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, deepLinkUri))
                                    walletActionNote = null
                                } catch (_: ActivityNotFoundException) {
                                    copyToClipboard(context, "USDT Address", addr)
                                    walletActionNote = "No crypto wallet app detected; receiving address copied to clipboard."
                                }
                            }
                        },
                        shape = spec.pillShape,
                        border = BorderStroke(1.dp, spec.accentBright),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            tint = spec.accentBright,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("OPEN CRYPTO WALLET", color = spec.accentBright, fontSize = 12.sp)
                    }

                    if (walletActionNote != null) {
                        Text(
                            text = walletActionNote!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = spec.textSecondary
                        )
                    }

                    Text(
                        text = "3. Paste On-Chain TxID / TxHash to Verify",
                        style = MaterialTheme.typography.labelMedium,
                        color = spec.textSecondary
                    )
                    OutlinedTextField(
                        value = txHashInput,
                        onValueChange = { txHashInput = it },
                        label = {
                            Text(if (selectedNetwork.isTron) "TRON TxID (64 hex)" else "${selectedNetwork.shortCode} TxHash (0x...)")
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = spec.accentBright,
                            unfocusedBorderColor = spec.surfaceDivider,
                            focusedLabelColor = spec.accentBright,
                            unfocusedLabelColor = spec.textSecondary,
                            focusedTextColor = spec.textPrimary,
                            unfocusedTextColor = spec.textPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("usdt_tx_hash_input")
                    )

                    when (verificationState) {
                        is UsdtVerificationUiState.Verifying -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    color = spec.accentBright,
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Verifying USDT contract log on ${selectedNetwork.shortCode}...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = spec.accentBright
                                )
                            }
                        }
                        is UsdtVerificationUiState.Error -> {
                            Text(
                                text = verificationState.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = spec.danger
                            )
                        }
                        is UsdtVerificationUiState.Success -> {
                            Text(
                                text = "Verified on-chain in block ${verificationState.blockNumber}!",
                                style = MaterialTheme.typography.bodySmall,
                                color = spec.accentBright,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        UsdtVerificationUiState.Idle -> Unit
                    }
                }
            },
            confirmButton = {
                if (verificationState is UsdtVerificationUiState.Success) {
                    TextButton(
                        onClick = {
                            onResetVerificationState()
                            checkoutItem = null
                        }
                    ) {
                        Text("DONE", color = spec.accentBright, fontWeight = FontWeight.Bold)
                    }
                } else {
                    TextButton(
                        enabled = verificationState !is UsdtVerificationUiState.Verifying,
                        onClick = {
                            onSaveMerchantAddress(merchantWalletInput)
                            onVerifyUsdtPurchase(
                                activeCheckout,
                                selectedNetwork,
                                txHashInput,
                                merchantWalletInput
                            )
                        },
                        modifier = Modifier.testTag("verify_usdt_tx_button")
                    ) {
                        Text("VERIFY ON-CHAIN & UNLOCK", color = spec.accentBright, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    enabled = verificationState !is UsdtVerificationUiState.Verifying,
                    onClick = {
                        onResetVerificationState()
                        checkoutItem = null
                    }
                ) {
                    Text("CLOSE", color = spec.textSecondary)
                }
            }
        )
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    cm?.setPrimaryClip(ClipData.newPlainText(label, text))
}
