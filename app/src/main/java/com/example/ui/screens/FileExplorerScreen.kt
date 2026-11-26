package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.AppSettings
import com.example.model.ExplorerEntry
import com.example.model.ExplorerSort
import com.example.model.StorageOverview
import com.example.ui.components.BrandTopBar
import com.example.ui.components.PrimaryShredButton
import com.example.ui.components.WizardStepperBar
import com.example.ui.components.themedScreenBackground
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalAppThemeSpec
import com.example.util.FormatUtils
import com.example.viewmodel.ExplorerBreadcrumb
import com.example.viewmodel.ExplorerNode

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileExplorerScreen(
    storageOverview: StorageOverview,
    settings: AppSettings,
    breadcrumbs: List<ExplorerBreadcrumb>,
    entries: List<ExplorerEntry>,
    selectedIds: Set<String>,
    currentSort: ExplorerSort,
    onBack: () -> Unit,
    onNavigateUp: () -> Boolean,
    onBreadcrumbClick: (Int) -> Unit,
    onEntryClick: (ExplorerEntry) -> Unit,
    onToggleSelect: (String) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onSortChange: (ExplorerSort) -> Unit,
    onMountFolder: (android.net.Uri) -> Unit,
    onUnmountFolder: (String) -> Unit,
    onPickFilesDirect: (List<android.net.Uri>) -> Unit,
    onComputeSha256: suspend (ExplorerEntry) -> String,
    onShredSelected: () -> Unit
) {
    val spec = LocalAppThemeSpec.current
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var inspectedEntry by remember { mutableStateOf<ExplorerEntry?>(null) }
    val isAtRoot = breadcrumbs.lastOrNull()?.node is ExplorerNode.Root

    BackHandler(enabled = breadcrumbs.size > 1) {
        onNavigateUp()
    }

    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) onMountFolder(uri)
    }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) onPickFilesDirect(uris)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .themedScreenBackground()
    ) {
        BrandTopBar(
            showBack = true,
            onBack = {
                if (!onNavigateUp()) onBack()
            }
        )

        WizardStepperBar(activeStep = 1)

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Browse through your device and long press to select data for secure shredding",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Normal,
            color = spec.textPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            if (isAtRoot) {
                Text(
                    text = "Storages",
                    style = MaterialTheme.typography.bodyMedium,
                    color = spec.textSecondary,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(entries, key = { it.id }) { entry ->
                        val isStorageItem = entry.id == "storage:internal"
                        val isSelected = entry.id in selectedIds
                        Surface(
                            shape = spec.cardShape,
                            color = if (isSelected) spec.accentTintBg else spec.surfaceCard,
                            tonalElevation = spec.tonalElevation,
                            shadowElevation = spec.shadowElevation,
                            border = BorderStroke(
                                if (isSelected) 1.5.dp else spec.cardBorderWidth,
                                if (isSelected) spec.accentBright else spec.cardBorderColor
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {
                                        if (selectedIds.isNotEmpty() && !isStorageItem) {
                                            onToggleSelect(entry.id)
                                        } else {
                                            onEntryClick(entry)
                                        }
                                    },
                                    onLongClick = {
                                        if (!isStorageItem) onToggleSelect(entry.id)
                                    }
                                )
                                .testTag("explorer_storage_${entry.name}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isStorageItem) Icons.Filled.Smartphone else Icons.Filled.Folder,
                                    contentDescription = null,
                                    tint = if (isStorageItem) spec.textPrimary else spec.accentBright,
                                    modifier = Modifier.size(30.dp)
                                )
                                Spacer(modifier = Modifier.width(18.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = entry.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Normal,
                                        color = spec.textPrimary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = entry.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = spec.textSecondary
                                    )
                                }
                                if (entry.isMountedRoot && entry.uri != null) {
                                    TextButton(onClick = { onUnmountFolder(entry.uri.toString()) }) {
                                        Text("Unmount", color = spec.danger, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Inside Internal Storage or Folder: breadcrumbs + action tools + list
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        breadcrumbs.forEachIndexed { idx, crumb ->
                            val isLast = idx == breadcrumbs.lastIndex
                            Surface(
                                shape = spec.pillShape,
                                color = if (isLast) spec.accentTintBg else spec.surfaceCard,
                                border = BorderStroke(spec.cardBorderWidth, if (isLast) spec.accentBright.copy(alpha = 0.5f) else spec.cardBorderColor),
                                modifier = Modifier.clickable { onBreadcrumbClick(idx) }
                            ) {
                                Text(
                                    text = crumb.title,
                                    fontSize = 12.sp,
                                    fontWeight = if (isLast) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isLast) spec.accentBright else spec.textSecondary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                            if (!isLast) {
                                Icon(
                                    imageVector = Icons.Filled.ChevronRight,
                                    contentDescription = null,
                                    tint = spec.textSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { folderPicker.launch(null) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("mount_folder_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CreateNewFolder,
                                contentDescription = "Mount Folder",
                                tint = spec.accentBright
                            )
                        }
                        IconButton(
                            onClick = { filePicker.launch(arrayOf("*/*")) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("pick_files_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.NoteAdd,
                                contentDescription = "Pick Files",
                                tint = spec.textPrimary
                            )
                        }
                        Box {
                            IconButton(
                                onClick = { sortMenuExpanded = true },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Sort,
                                    contentDescription = "Sort",
                                    tint = spec.textPrimary
                                )
                            }
                            DropdownMenu(
                                expanded = sortMenuExpanded,
                                onDismissRequest = { sortMenuExpanded = false },
                                containerColor = spec.surfaceElevated
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Name (A–Z)", color = spec.textPrimary) },
                                    onClick = {
                                        onSortChange(ExplorerSort.NAME_ASC)
                                        sortMenuExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Size (Largest)", color = spec.textPrimary) },
                                    onClick = {
                                        onSortChange(ExplorerSort.SIZE_DESC)
                                        sortMenuExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Modified (Newest)", color = spec.textPrimary) },
                                    onClick = {
                                        onSortChange(ExplorerSort.DATE_DESC)
                                        sortMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                if (selectedIds.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${selectedIds.size} selected for shredding",
                            fontSize = 12.sp,
                            color = spec.accentBright
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = onSelectAll) {
                                Text("Select All", color = spec.textPrimary, fontSize = 12.sp)
                            }
                            TextButton(onClick = onClearSelection) {
                                Text("Clear", color = spec.textSecondary, fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                if (entries.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Filled.FolderOff,
                                contentDescription = null,
                                tint = spec.textSecondary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Folder is empty", style = MaterialTheme.typography.titleMedium, color = spec.textPrimary)
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedButton(
                                onClick = { folderPicker.launch(null) },
                                shape = spec.pillShape,
                                border = BorderStroke(1.dp, spec.surfaceDivider)
                            ) {
                                Text("Mount Device Folder", color = spec.accentBright)
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(entries, key = { it.id }) { entry ->
                            val isSelected = entry.id in selectedIds
                            ExplorerEntryRow(
                                entry = entry,
                                isSelected = isSelected,
                                showThumbnail = settings.thumbnailPreview,
                                onRowClick = {
                                    if (selectedIds.isNotEmpty() && !entry.isDirectory) {
                                        onToggleSelect(entry.id)
                                    } else {
                                        onEntryClick(entry)
                                    }
                                },
                                onLongPress = { onToggleSelect(entry.id) },
                                onToggleSelect = { onToggleSelect(entry.id) },
                                onInspect = { inspectedEntry = entry },
                                onUnmount = if (entry.isMountedRoot && entry.uri != null) {
                                    { onUnmountFolder(entry.uri.toString()) }
                                } else null
                            )
                        }
                        item { Spacer(modifier = Modifier.height(12.dp)) }
                    }
                }
            }
        }

        PrimaryShredButton(
            text = if (selectedIds.isEmpty()) "CONTINUE" else "CONTINUE (${selectedIds.size})",
            enabled = selectedIds.isNotEmpty(),
            onClick = onShredSelected,
            testTag = "explorer_continue_button"
        )
    }

    inspectedEntry?.let { entry ->
        FileInspectorDialog(
            entry = entry,
            onComputeSha256 = onComputeSha256,
            onDismiss = { inspectedEntry = null }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ExplorerEntryRow(
    entry: ExplorerEntry,
    isSelected: Boolean,
    showThumbnail: Boolean,
    onRowClick: () -> Unit,
    onLongPress: () -> Unit,
    onToggleSelect: () -> Unit,
    onInspect: () -> Unit,
    onUnmount: (() -> Unit)?
) {
    val spec = LocalAppThemeSpec.current
    Surface(
        shape = spec.cardShape,
        color = if (isSelected) spec.accentTintBg else spec.surfaceCard,
        border = BorderStroke(
            if (isSelected) 1.5.dp else spec.cardBorderWidth,
            if (isSelected) spec.accentBright else spec.cardBorderColor
        ),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onRowClick,
                onLongClick = onLongPress
            )
            .testTag("explorer_item_${entry.name}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onToggleSelect,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(
                    imageVector = if (isSelected) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
                    contentDescription = "Select ${entry.name}",
                    tint = if (isSelected) spec.accentBright else spec.textSecondary
                )
            }

            val isImage = entry.mimeType?.startsWith("image/") == true
            val model: Any? = entry.uri ?: entry.localFile
            if (showThumbnail && !entry.isDirectory && isImage && model != null) {
                AsyncImage(
                    model = model,
                    contentDescription = entry.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(spec.badgeShape)
                )
            } else {
                val icon = when {
                    entry.isDirectory -> Icons.Filled.Folder
                    entry.mimeType?.startsWith("video/") == true -> Icons.Filled.VideoFile
                    entry.mimeType?.startsWith("audio/") == true -> Icons.Filled.AudioFile
                    entry.mimeType == "application/pdf" -> Icons.Filled.PictureAsPdf
                    isImage -> Icons.Filled.Image
                    else -> Icons.AutoMirrored.Filled.InsertDriveFile
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(spec.badgeShape)
                        .background(spec.surfaceElevated),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (entry.isDirectory) spec.accentBright else spec.textPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = spec.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = buildString {
                        if (!entry.isDirectory || entry.sizeBytes > 0L) {
                            append(FormatUtils.formatIecBytes(entry.sizeBytes).ifEmpty { "0 B" })
                            append(" • ")
                        }
                        append(entry.subtitle)
                    },
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 11.sp,
                    color = spec.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (onUnmount != null) {
                TextButton(onClick = onUnmount) {
                    Text("Unmount", color = spec.danger, fontSize = 11.sp)
                }
            } else if (!entry.isDirectory) {
                IconButton(
                    onClick = onInspect,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = "Inspect ${entry.name}",
                        tint = spec.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = "Open folder",
                    tint = spec.textSecondary,
                    modifier = Modifier.padding(end = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun FileInspectorDialog(
    entry: ExplorerEntry,
    onComputeSha256: suspend (ExplorerEntry) -> String,
    onDismiss: () -> Unit
) {
    val spec = LocalAppThemeSpec.current
    var sha256 by remember(entry.id) { mutableStateOf("Calculating SHA-256...") }
    LaunchedEffect(entry.id) {
        sha256 = onComputeSha256(entry)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = spec.dialogShape,
        containerColor = spec.surfaceCard,
        titleContentColor = spec.textPrimary,
        textContentColor = spec.textSecondary,
        title = {
            Text(
                text = "File Forensic Inspector",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InspectorField("Name", entry.name)
                InspectorField("Size", "${FormatUtils.formatIecBytes(entry.sizeBytes).ifEmpty { "0 B" }} (${entry.sizeBytes} bytes)")
                InspectorField("MIME Type", entry.mimeType ?: "Unknown")
                InspectorField("Modified", FormatUtils.formatTimestamp(entry.lastModifiedMillis))
                InspectorField("SHA-256 Checksum", sha256, mono = true)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("CLOSE", color = spec.accentBright, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun InspectorField(label: String, value: String, mono: Boolean = false) {
    val spec = LocalAppThemeSpec.current
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = spec.textSecondary)
        Text(
            text = value,
            fontFamily = if (mono) JetBrainsMonoFontFamily else MaterialTheme.typography.bodyMedium.fontFamily,
            fontSize = if (mono) 11.sp else 14.sp,
            color = spec.textPrimary
        )
    }
}
