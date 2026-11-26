package com.example.ui.screens

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppSettings
import com.example.model.SelectiveParentGroup
import com.example.model.SelectiveSubCategory
import com.example.model.ShredTargetItem
import com.example.model.StorageOverview
import com.example.ui.components.BrandTopBar
import com.example.ui.components.PrimaryShredButton
import com.example.ui.components.WizardStepperBar
import com.example.ui.components.themedScreenBackground
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalAppThemeSpec
import com.example.util.FormatUtils

@Composable
fun SelectiveEraseScreen(
    storageOverview: StorageOverview,
    settings: AppSettings,
    selectiveBuckets: Map<SelectiveSubCategory, List<ShredTargetItem>>,
    queuedTargets: List<ShredTargetItem>,
    contactsPermissionGranted: Boolean,
    onContactsPermissionResult: (Boolean) -> Unit,
    onToggleParentGroup: (SelectiveParentGroup) -> Unit,
    onToggleSubBucketAll: (SelectiveSubCategory) -> Unit,
    onToggleSingleItem: (ShredTargetItem) -> Unit,
    onPickedUrisForSubCategory: (List<Uri>, SelectiveSubCategory) -> Unit,
    onMountFolder: (Uri) -> Unit,
    onSetFreeSpaceMb: (Long) -> Unit,
    onBack: () -> Unit,
    onContinueToMethod: () -> Unit
) {
    val spec = LocalAppThemeSpec.current
    var expandFreeSpace by remember { mutableStateOf(true) }
    var expandFilesFolders by remember { mutableStateOf(true) }
    var expandPrivacy by remember { mutableStateOf(true) }
    var expandMedia by remember { mutableStateOf(true) }

    var activeInspectSub by remember { mutableStateOf<SelectiveSubCategory?>(null) }
    var showFreeSpaceConfigDialog by remember { mutableStateOf(false) }
    var pendingPickSub by remember { mutableStateOf<SelectiveSubCategory?>(null) }

    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result[Manifest.permission.READ_CONTACTS] == true &&
            result[Manifest.permission.WRITE_CONTACTS] == true
        onContactsPermissionResult(granted)
    }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        val targetSub = pendingPickSub
        if (uris.isNotEmpty() && targetSub != null) {
            onPickedUrisForSubCategory(uris, targetSub)
        }
        pendingPickSub = null
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            onMountFolder(uri)
        }
    }

    val queuedIds = remember(queuedTargets) { queuedTargets.map { it.id }.toSet() }

    fun launchPickerForSub(sub: SelectiveSubCategory) {
        pendingPickSub = sub
        val mime = when (sub) {
            SelectiveSubCategory.PHOTOS, SelectiveSubCategory.THUMBNAILS -> arrayOf("image/*")
            SelectiveSubCategory.VIDEO -> arrayOf("video/*")
            SelectiveSubCategory.SONGS_VOICE -> arrayOf("audio/*")
            else -> arrayOf("*/*")
        }
        documentPickerLauncher.launch(mime)
    }

    fun isGroupChecked(group: SelectiveParentGroup): Boolean {
        val subs = SelectiveSubCategory.entries.filter { it.parentGroup == group }
        val allItems = subs.flatMap { selectiveBuckets[it].orEmpty() }
        return allItems.isNotEmpty() && allItems.any { it.id in queuedIds }
    }

    val hasConnectedDevices = storageOverview.connectedRemovableVolumes.isNotEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .themedScreenBackground()
    ) {
        BrandTopBar(showBack = true, onBack = onBack)

        WizardStepperBar(activeStep = 1)

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "What do you want to shred securely?",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Normal,
            color = spec.textPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Free Space
            item {
                ParentCategoryCard(
                    title = "Free Space",
                    expanded = expandFreeSpace,
                    checked = isGroupChecked(SelectiveParentGroup.FREE_SPACE),
                    hasItems = selectiveBuckets[SelectiveSubCategory.INTERNAL_STORAGE].orEmpty().isNotEmpty(),
                    testTag = "group_free_space",
                    iconContent = { PiePartitionIcon(spec.textPrimary, Modifier.size(24.dp)) },
                    onToggleExpand = { expandFreeSpace = !expandFreeSpace },
                    onToggleCheck = { onToggleParentGroup(SelectiveParentGroup.FREE_SPACE) }
                ) {
                    val items = selectiveBuckets[SelectiveSubCategory.INTERNAL_STORAGE].orEmpty()
                    val selectedCount = items.count { it.id in queuedIds }
                    val sizeBytes = items.sumOf { it.sizeBytes }
                    SubCategoryRow(
                        sub = SelectiveSubCategory.INTERNAL_STORAGE,
                        icon = Icons.Filled.Smartphone,
                        subtitle = "$selectedCount of ${items.size} selected",
                        sizeLabel = FormatUtils.formatIecBytes(sizeBytes),
                        enabled = true,
                        showCheckbox = true,
                        checked = items.isNotEmpty() && selectedCount == items.size,
                        onRowClick = { showFreeSpaceConfigDialog = true },
                        onCheckboxToggle = { onToggleSubBucketAll(SelectiveSubCategory.INTERNAL_STORAGE) }
                    )
                }
            }

            // 2. Files & Folders
            item {
                val filesSubs = listOf(
                    SelectiveSubCategory.APKS_AABS,
                    SelectiveSubCategory.DOWNLOADS,
                    SelectiveSubCategory.EMPTY_FOLDERS,
                    SelectiveSubCategory.TEMPORARY_FILES,
                    SelectiveSubCategory.TRASH_BIN
                )
                val filesHasItems = filesSubs.any { selectiveBuckets[it].orEmpty().isNotEmpty() }
                ParentCategoryCard(
                    title = "Files & Folders",
                    expanded = expandFilesFolders,
                    checked = isGroupChecked(SelectiveParentGroup.FILES_FOLDERS),
                    hasItems = filesHasItems,
                    testTag = "group_files_folders",
                    iconContent = {
                        Icon(
                            imageVector = Icons.Filled.Folder,
                            contentDescription = null,
                            tint = spec.textPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    onToggleExpand = { expandFilesFolders = !expandFilesFolders },
                    onToggleCheck = {
                        if (filesHasItems) {
                            onToggleParentGroup(SelectiveParentGroup.FILES_FOLDERS)
                        } else {
                            folderPickerLauncher.launch(null)
                        }
                    }
                ) {
                    filesSubs.forEachIndexed { idx, sub ->
                        val items = selectiveBuckets[sub].orEmpty()
                        val selectedCount = items.count { it.id in queuedIds }
                        val totalSize = items.sumOf { it.sizeBytes }
                        val hasFound = items.isNotEmpty()
                        val rowIcon = when (sub) {
                            SelectiveSubCategory.APKS_AABS -> Icons.Filled.Android
                            SelectiveSubCategory.DOWNLOADS -> Icons.Filled.Download
                            SelectiveSubCategory.EMPTY_FOLDERS -> Icons.Filled.FolderOpen
                            SelectiveSubCategory.TEMPORARY_FILES -> Icons.Filled.Schedule
                            SelectiveSubCategory.TRASH_BIN -> Icons.Filled.Delete
                            else -> Icons.Filled.Folder
                        }
                        SubCategoryRow(
                            sub = sub,
                            icon = rowIcon,
                            subtitle = if (hasFound) "$selectedCount of ${items.size} selected" else "Nothing found",
                            sizeLabel = if (hasFound && totalSize > 0L) FormatUtils.formatIecBytes(totalSize) else "",
                            enabled = hasFound || sub != SelectiveSubCategory.TEMPORARY_FILES,
                            showCheckbox = hasFound,
                            checked = hasFound && selectedCount == items.size,
                            onRowClick = {
                                if (hasFound) {
                                    activeInspectSub = sub
                                } else if (sub != SelectiveSubCategory.TEMPORARY_FILES) {
                                    activeInspectSub = sub
                                }
                            },
                            onCheckboxToggle = { onToggleSubBucketAll(sub) }
                        )
                        if (idx < filesSubs.lastIndex) {
                            HorizontalDivider(color = spec.surfaceDivider.copy(alpha = 0.45f), thickness = 0.8.dp)
                        }
                    }
                }
            }

            // 3. Privacy
            item {
                val privacySubs = listOf(
                    SelectiveSubCategory.CLIPBOARD,
                    SelectiveSubCategory.CONTACTS,
                    SelectiveSubCategory.WHATSAPP,
                    SelectiveSubCategory.TELEGRAM
                )
                val privacyHasItems = privacySubs.any { selectiveBuckets[it].orEmpty().isNotEmpty() }
                ParentCategoryCard(
                    title = "Privacy",
                    expanded = expandPrivacy,
                    checked = isGroupChecked(SelectiveParentGroup.PRIVACY),
                    hasItems = privacyHasItems,
                    testTag = "group_privacy",
                    iconContent = {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            tint = spec.textPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    onToggleExpand = { expandPrivacy = !expandPrivacy },
                    onToggleCheck = { onToggleParentGroup(SelectiveParentGroup.PRIVACY) }
                ) {
                    privacySubs.forEachIndexed { idx, sub ->
                        val items = selectiveBuckets[sub].orEmpty()
                        val selectedCount = items.count { it.id in queuedIds }
                        val totalSize = items.sumOf { it.sizeBytes }
                        val isContactsLocked = sub == SelectiveSubCategory.CONTACTS && !contactsPermissionGranted
                        val hasFound = items.isNotEmpty()

                        val rowIcon = when (sub) {
                            SelectiveSubCategory.CLIPBOARD -> Icons.Filled.Assignment
                            SelectiveSubCategory.CONTACTS -> Icons.Filled.Contacts
                            SelectiveSubCategory.WHATSAPP -> Icons.Filled.ChatBubbleOutline
                            SelectiveSubCategory.TELEGRAM -> Icons.AutoMirrored.Filled.Send
                            else -> Icons.Filled.Lock
                        }

                        val subtitleText = when {
                            isContactsLocked -> "Press to permit missing permission"
                            hasFound -> "$selectedCount of ${items.size} selected"
                            else -> "Nothing found"
                        }

                        SubCategoryRow(
                            sub = sub,
                            icon = rowIcon,
                            subtitle = subtitleText,
                            sizeLabel = if (hasFound && totalSize > 0L && sub != SelectiveSubCategory.CONTACTS) {
                                FormatUtils.formatIecBytes(totalSize)
                            } else "",
                            enabled = !isContactsLocked && (hasFound || sub == SelectiveSubCategory.WHATSAPP || sub == SelectiveSubCategory.TELEGRAM),
                            dimmedLook = isContactsLocked || (!hasFound && sub != SelectiveSubCategory.CLIPBOARD),
                            showCheckbox = hasFound && !isContactsLocked,
                            checked = hasFound && selectedCount == items.size,
                            onRowClick = {
                                when {
                                    isContactsLocked -> {
                                        contactsPermissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.READ_CONTACTS,
                                                Manifest.permission.WRITE_CONTACTS
                                            )
                                        )
                                    }
                                    sub == SelectiveSubCategory.CLIPBOARD -> {
                                        onToggleSubBucketAll(SelectiveSubCategory.CLIPBOARD)
                                    }
                                    else -> {
                                        activeInspectSub = sub
                                    }
                                }
                            },
                            onCheckboxToggle = { onToggleSubBucketAll(sub) }
                        )
                        if (idx < privacySubs.lastIndex) {
                            HorizontalDivider(color = spec.surfaceDivider.copy(alpha = 0.45f), thickness = 0.8.dp)
                        }
                    }
                }
            }

            // 4. Media
            item {
                val mediaSubs = listOf(
                    SelectiveSubCategory.THUMBNAILS,
                    SelectiveSubCategory.SONGS_VOICE,
                    SelectiveSubCategory.VIDEO,
                    SelectiveSubCategory.PHOTOS
                )
                val mediaHasItems = mediaSubs.any { selectiveBuckets[it].orEmpty().isNotEmpty() }
                ParentCategoryCard(
                    title = "Media",
                    expanded = expandMedia,
                    checked = isGroupChecked(SelectiveParentGroup.MEDIA),
                    hasItems = mediaHasItems,
                    testTag = "group_media",
                    iconContent = {
                        Icon(
                            imageVector = Icons.Filled.PlayCircleFilled,
                            contentDescription = null,
                            tint = spec.textPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    onToggleExpand = { expandMedia = !expandMedia },
                    onToggleCheck = {
                        if (mediaHasItems) {
                            onToggleParentGroup(SelectiveParentGroup.MEDIA)
                        } else {
                            launchPickerForSub(SelectiveSubCategory.PHOTOS)
                        }
                    }
                ) {
                    mediaSubs.forEachIndexed { idx, sub ->
                        val items = selectiveBuckets[sub].orEmpty()
                        val selectedCount = items.count { it.id in queuedIds }
                        val totalSize = items.sumOf { it.sizeBytes }
                        val hasFound = items.isNotEmpty()
                        val rowIcon = when (sub) {
                            SelectiveSubCategory.THUMBNAILS -> Icons.Filled.PhotoLibrary
                            SelectiveSubCategory.SONGS_VOICE -> Icons.Filled.AudioFile
                            SelectiveSubCategory.VIDEO -> Icons.Filled.VideoLibrary
                            SelectiveSubCategory.PHOTOS -> Icons.Filled.Image
                            else -> Icons.Filled.Image
                        }

                        SubCategoryRow(
                            sub = sub,
                            icon = rowIcon,
                            subtitle = if (hasFound) "$selectedCount of ${items.size} selected" else "Nothing found",
                            sizeLabel = if (hasFound && totalSize > 0L) FormatUtils.formatIecBytes(totalSize) else "",
                            enabled = true,
                            dimmedLook = !hasFound,
                            showCheckbox = hasFound,
                            checked = hasFound && selectedCount == items.size,
                            onRowClick = { activeInspectSub = sub },
                            onCheckboxToggle = { onToggleSubBucketAll(sub) }
                        )
                        if (idx < mediaSubs.lastIndex) {
                            HorizontalDivider(color = spec.surfaceDivider.copy(alpha = 0.45f), thickness = 0.8.dp)
                        }
                    }
                }
            }

            // 5. Connected Devices
            item {
                Surface(
                    shape = spec.cardShape,
                    color = if (hasConnectedDevices) spec.surfaceCard else spec.surfaceCard.copy(alpha = 0.65f),
                    border = BorderStroke(spec.cardBorderWidth, spec.cardBorderColor),
                    tonalElevation = if (hasConnectedDevices) spec.tonalElevation else 0.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = hasConnectedDevices) { folderPickerLauncher.launch(null) }
                        .testTag("group_connected_devices")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Usb,
                            contentDescription = null,
                            tint = if (hasConnectedDevices) spec.textPrimary else spec.textMuted,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(18.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Connected Devices",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Normal,
                                color = if (hasConnectedDevices) spec.textPrimary else spec.textMuted
                            )
                            Text(
                                text = if (hasConnectedDevices) {
                                    storageOverview.connectedRemovableVolumes.joinToString(", ")
                                } else {
                                    "Nothing found"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = spec.textMuted
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }

        PrimaryShredButton(
            text = "CONTINUE",
            enabled = queuedTargets.isNotEmpty(),
            onClick = onContinueToMethod,
            testTag = "selective_continue_button"
        )
    }

    // Free Space Block Size Config Dialog
    if (showFreeSpaceConfigDialog) {
        val presets = listOf(
            64L to "64 MiB (Quick Free Space Sweep)",
            128L to "128 MiB (Standard Wipe)",
            256L to "256 MiB (Extended Wipe)",
            512L to "512 MiB (Deep Wipe)",
            0L to "Safe Max (${FormatUtils.formatIecBytes(storageOverview.availableBytes)} available)"
        )
        AlertDialog(
            onDismissRequest = { showFreeSpaceConfigDialog = false },
            shape = spec.dialogShape,
            containerColor = spec.surfaceCard,
            titleContentColor = spec.textPrimary,
            title = { Text("Internal Storage Free Space") },
            text = {
                Column {
                    Text(
                        text = "Configure how much unallocated space to overwrite per pass:",
                        style = MaterialTheme.typography.bodySmall,
                        color = spec.textSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    presets.forEach { (mb, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSetFreeSpaceMb(mb)
                                    showFreeSpaceConfigDialog = false
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = settings.freeSpaceOverwriteMb == mb,
                                onClick = {
                                    onSetFreeSpaceMb(mb)
                                    showFreeSpaceConfigDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = spec.accentBright)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(label, color = spec.textPrimary, fontSize = 13.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFreeSpaceConfigDialog = false }) {
                    Text("DONE", color = spec.accentBright, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Sub-category Item Picker & Scanner Dialog
    activeInspectSub?.let { sub ->
        val items = selectiveBuckets[sub].orEmpty()
        SubCategoryInspectorDialog(
            subCategory = sub,
            items = items,
            queuedIds = queuedIds,
            onToggleItem = onToggleSingleItem,
            onToggleAll = { onToggleSubBucketAll(sub) },
            onPickFiles = { launchPickerForSub(sub) },
            onMountFolder = { folderPickerLauncher.launch(null) },
            onDismiss = { activeInspectSub = null }
        )
    }
}

@Composable
private fun ParentCategoryCard(
    title: String,
    expanded: Boolean,
    checked: Boolean,
    hasItems: Boolean,
    testTag: String,
    iconContent: @Composable () -> Unit,
    onToggleExpand: () -> Unit,
    onToggleCheck: () -> Unit,
    content: @Composable () -> Unit
) {
    val spec = LocalAppThemeSpec.current
    Surface(
        shape = spec.cardShape,
        color = spec.surfaceCard,
        border = BorderStroke(spec.cardBorderWidth, spec.cardBorderColor),
        tonalElevation = spec.tonalElevation,
        shadowElevation = spec.shadowElevation,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpand)
                    .padding(start = 18.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                iconContent()
                Spacer(modifier = Modifier.width(18.dp))
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Normal,
                        color = spec.textPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (expanded) "Collapse $title" else "Expand $title",
                        tint = spec.textPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Checkbox(
                    checked = checked,
                    onCheckedChange = { onToggleCheck() },
                    enabled = hasItems || title == "Files & Folders" || title == "Media",
                    colors = CheckboxDefaults.colors(
                        checkedColor = spec.accentPrimary,
                        uncheckedColor = spec.textPrimary,
                        checkmarkColor = Color.White
                    ),
                    modifier = Modifier.testTag("${testTag}_checkbox")
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    HorizontalDivider(color = spec.surfaceDivider.copy(alpha = 0.5f), thickness = 0.8.dp)
                    content()
                }
            }
        }
    }
}

@Composable
private fun SubCategoryRow(
    sub: SelectiveSubCategory,
    icon: ImageVector,
    subtitle: String,
    sizeLabel: String,
    enabled: Boolean,
    dimmedLook: Boolean = !enabled,
    showCheckbox: Boolean,
    checked: Boolean,
    onRowClick: () -> Unit,
    onCheckboxToggle: () -> Unit
) {
    val spec = LocalAppThemeSpec.current
    val contentColor = if (dimmedLook) spec.textMuted else spec.textPrimary
    val subColor = if (dimmedLook) spec.textMuted else spec.textSecondary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onRowClick)
            .padding(start = 18.dp, end = 8.dp, top = 12.dp, bottom = 12.dp)
            .testTag("subcat_${sub.name.lowercase()}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(28.dp), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(22.dp)
            )
            if (sub.hasSearchBadge) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 2.dp, y = 2.dp)
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(spec.surfaceCard)
                        .border(1.dp, contentColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = sub.title,
                style = MaterialTheme.typography.bodyLarge,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (checked && !dimmedLook) spec.accentBright else subColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (sizeLabel.isNotEmpty()) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = sizeLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor
            )
        }

        if (showCheckbox) {
            Spacer(modifier = Modifier.width(4.dp))
            Checkbox(
                checked = checked,
                onCheckedChange = { onCheckboxToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = spec.accentPrimary,
                    uncheckedColor = spec.textPrimary,
                    checkmarkColor = Color.White
                ),
                modifier = Modifier.testTag("subcat_check_${sub.name.lowercase()}")
            )
        } else {
            Spacer(modifier = Modifier.width(12.dp))
        }
    }
}

@Composable
private fun SubCategoryInspectorDialog(
    subCategory: SelectiveSubCategory,
    items: List<ShredTargetItem>,
    queuedIds: Set<String>,
    onToggleItem: (ShredTargetItem) -> Unit,
    onToggleAll: () -> Unit,
    onPickFiles: () -> Unit,
    onMountFolder: () -> Unit,
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(subCategory.title, style = MaterialTheme.typography.titleLarge)
                if (items.isNotEmpty()) {
                    TextButton(onClick = onToggleAll) {
                        val allSelected = items.all { it.id in queuedIds }
                        Text(
                            text = if (allSelected) "Deselect All" else "Select All",
                            color = spec.accentBright,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (items.isEmpty()) {
                    Text(
                        text = "No ${subCategory.title} items discovered yet in mounted storage. Pick files directly or mount a folder to scan automatically.",
                        style = MaterialTheme.typography.bodySmall,
                        color = spec.textSecondary
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(items, key = { it.id }) { item ->
                            val isChecked = item.id in queuedIds
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(spec.badgeShape)
                                    .background(spec.surfaceElevated)
                                    .clickable { onToggleItem(item) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = spec.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (item.sizeBytes > 0L) {
                                        Text(
                                            text = FormatUtils.formatIecBytes(item.sizeBytes),
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontSize = 10.sp,
                                            color = spec.textSecondary
                                        )
                                    }
                                }
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { onToggleItem(item) },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = spec.accentPrimary,
                                        uncheckedColor = spec.textPrimary,
                                        checkmarkColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }

                if (subCategory != SelectiveSubCategory.CONTACTS && subCategory != SelectiveSubCategory.TEMPORARY_FILES) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onPickFiles,
                            shape = spec.pillShape,
                            border = BorderStroke(1.dp, spec.surfaceDivider),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, tint = spec.accentBright, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Pick Files", color = spec.textPrimary, fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = onMountFolder,
                            shape = spec.pillShape,
                            border = BorderStroke(1.dp, spec.surfaceDivider),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.CreateNewFolder, contentDescription = null, tint = spec.accentBright, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Scan Folder", color = spec.textPrimary, fontSize = 12.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("DONE", color = spec.accentBright, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun PiePartitionIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val r = size.minDimension * 0.42f
        val cx = size.width / 2f
        val cy = size.height / 2f
        val gap = size.minDimension * 0.05f

        drawArc(
            color = tint,
            startAngle = 90f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = Offset(cx - r - gap / 2f, cy - r),
            size = Size(r * 2f, r * 2f)
        )
        drawArc(
            color = tint,
            startAngle = 270f,
            sweepAngle = 90f,
            useCenter = true,
            topLeft = Offset(cx - r + gap / 2f, cy - r - gap / 2f),
            size = Size(r * 2f, r * 2f)
        )
        drawArc(
            color = tint,
            startAngle = 0f,
            sweepAngle = 90f,
            useCenter = true,
            topLeft = Offset(cx - r + gap / 2f, cy - r + gap / 2f),
            size = Size(r * 2f, r * 2f)
        )
    }
}

@Composable
fun StorageSummaryBanner(overview: StorageOverview) {
    val spec = LocalAppThemeSpec.current
    val usedFraction = if (overview.totalBytes > 0L) {
        (overview.usedBytes.toFloat() / overview.totalBytes.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Surface(
        shape = spec.cardShape,
        color = spec.surfaceCard,
        border = BorderStroke(spec.cardBorderWidth, spec.cardBorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Internal Storage",
                    style = MaterialTheme.typography.titleSmall,
                    color = spec.textPrimary
                )
                Text(
                    text = "${FormatUtils.formatIecBytes(overview.availableBytes)} Free",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 12.sp,
                    color = spec.accentBright
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            androidx.compose.material3.LinearProgressIndicator(
                progress = { usedFraction },
                color = spec.accentBright,
                trackColor = spec.surfaceElevated,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(spec.pillShape)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Used ${FormatUtils.formatIecBytes(overview.usedBytes)} of ${FormatUtils.formatIecBytes(overview.totalBytes)}",
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 11.sp,
                color = spec.textSecondary
            )
        }
    }
}
