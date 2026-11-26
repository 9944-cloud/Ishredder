package com.example.viewmodel

import android.app.Application
import android.net.Uri
import android.provider.DocumentsContract
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppSettings
import com.example.data.ErasureReportEntity
import com.example.data.MountedFolderEntity
import com.example.data.SettingsDataStore
import com.example.data.ShredderRepository
import com.example.data.UsdtPurchaseEntity
import com.example.engine.ExifPhotoReport
import com.example.engine.HardwareBenchmarkResult
import com.example.engine.PrivacyAuditReport
import com.example.engine.ScrubbedPhotoResult
import com.example.engine.UsdtVerificationResult
import com.example.model.ErasureCatalog
import com.example.model.ErasureMethod
import com.example.model.ExplorerEntry
import com.example.model.ExplorerSort
import com.example.model.SelectiveCategory
import com.example.model.SelectiveParentGroup
import com.example.model.SelectiveSubCategory
import com.example.model.ShredExecutionState
import com.example.model.ShredTargetItem
import com.example.model.StorageOverview
import com.example.model.UsdtIapItem
import com.example.model.UsdtNetwork
import com.example.util.FormatUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

sealed class ExplorerNode {
    data object Root : ExplorerNode()
    data object InternalStorage : ExplorerNode()
    data object AppCache : ExplorerNode()
    data object ResidualFiles : ExplorerNode()
    data class LocalDir(val dir: File, val title: String) : ExplorerNode()
    data class SafDir(val treeUri: Uri, val docId: String?, val title: String) : ExplorerNode()
}

data class ExplorerBreadcrumb(
    val title: String,
    val node: ExplorerNode
)

sealed class UsdtVerificationUiState {
    data object Idle : UsdtVerificationUiState()
    data object Verifying : UsdtVerificationUiState()
    data class Success(val itemTitle: String, val blockNumber: String) : UsdtVerificationUiState()
    data class Error(val message: String) : UsdtVerificationUiState()
}

class ShredderViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = ShredderRepository(application)

    val settings: StateFlow<AppSettings> = repo.settingsFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        AppSettings()
    )

    val reports: StateFlow<List<ErasureReportEntity>> = repo.allReportsFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val mountedFolders: StateFlow<List<MountedFolderEntity>> = repo.mountedFoldersFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val usdtPurchases: StateFlow<List<UsdtPurchaseEntity>> = repo.usdtPurchasesFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    private val _usdtVerificationState = MutableStateFlow<UsdtVerificationUiState>(UsdtVerificationUiState.Idle)
    val usdtVerificationState: StateFlow<UsdtVerificationUiState> = _usdtVerificationState.asStateFlow()

    private val _storageOverview = MutableStateFlow(StorageOverview())
    val storageOverview: StateFlow<StorageOverview> = _storageOverview.asStateFlow()

    private val _selectiveBuckets = MutableStateFlow<Map<SelectiveSubCategory, List<ShredTargetItem>>>(
        SelectiveSubCategory.entries.associateWith { emptyList() }
    )
    val selectiveBuckets: StateFlow<Map<SelectiveSubCategory, List<ShredTargetItem>>> = _selectiveBuckets.asStateFlow()

    private val _contactsPermissionGranted = MutableStateFlow(false)
    val contactsPermissionGranted: StateFlow<Boolean> = _contactsPermissionGranted.asStateFlow()

    private val _queuedTargets = MutableStateFlow<List<ShredTargetItem>>(emptyList())
    val queuedTargets: StateFlow<List<ShredTargetItem>> = _queuedTargets.asStateFlow()

    private val _selectedMethodId = MutableStateFlow("bsi_vsitr")
    val selectedMethodId: StateFlow<String> = _selectedMethodId.asStateFlow()

    private val _executionState = MutableStateFlow<ShredExecutionState>(ShredExecutionState.Idle)
    val executionState: StateFlow<ShredExecutionState> = _executionState.asStateFlow()

    private val _explorerBreadcrumbs = MutableStateFlow(listOf(ExplorerBreadcrumb("Storage", ExplorerNode.Root)))
    val explorerBreadcrumbs: StateFlow<List<ExplorerBreadcrumb>> = _explorerBreadcrumbs.asStateFlow()

    private val _explorerRawEntries = MutableStateFlow<List<ExplorerEntry>>(emptyList())
    private val _explorerSort = MutableStateFlow(ExplorerSort.NAME_ASC)
    val explorerSort: StateFlow<ExplorerSort> = _explorerSort.asStateFlow()

    val explorerEntries: StateFlow<List<ExplorerEntry>> =
        combine(_explorerRawEntries, _explorerSort) { list, sort ->
            val dirs = list.filter { it.isDirectory }
            val files = list.filter { !it.isDirectory }
            val comparator = when (sort) {
                ExplorerSort.NAME_ASC -> compareBy<ExplorerEntry> { it.name.lowercase() }
                ExplorerSort.SIZE_DESC -> compareByDescending<ExplorerEntry> { it.sizeBytes }
                ExplorerSort.DATE_DESC -> compareByDescending<ExplorerEntry> { it.lastModifiedMillis }
            }
            dirs.sortedWith(comparator) + files.sortedWith(comparator)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _explorerSelectedIds = MutableStateFlow<Set<String>>(emptySet())
    val explorerSelectedIds: StateFlow<Set<String>> = _explorerSelectedIds.asStateFlow()

    private val _isAppLocked = MutableStateFlow(false)
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _exifScanResults = MutableStateFlow<List<ExifPhotoReport>>(emptyList())
    val exifScanResults: StateFlow<List<ExifPhotoReport>> = _exifScanResults.asStateFlow()

    private val _lastScrubbedResult = MutableStateFlow<ScrubbedPhotoResult?>(null)
    val lastScrubbedResult: StateFlow<ScrubbedPhotoResult?> = _lastScrubbedResult.asStateFlow()

    private val _benchmarkResult = MutableStateFlow<HardwareBenchmarkResult?>(null)
    val benchmarkResult: StateFlow<HardwareBenchmarkResult?> = _benchmarkResult.asStateFlow()

    private val _isRunning2026Task = MutableStateFlow(false)
    val isRunning2026Task: StateFlow<Boolean> = _isRunning2026Task.asStateFlow()

    val privacyAudit: StateFlow<PrivacyAuditReport> =
        combine(_storageOverview, settings, mountedFolders, reports) { overview, curSettings, folders, reportList ->
            repo.computePrivacyAudit(overview, curSettings, folders, reportList)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            PrivacyAuditReport(
                score = 100,
                grade = "A+ HARDENED",
                items = emptyList(),
                cleanableBytes = 0L,
                hasClipboardRisk = false
            )
        )

    private var shredJob: Job? = null

    init {
        viewModelScope.launch {
            val initialSettings = repo.settingsFlow.first()
            _selectedMethodId.value = initialSettings.defaultMethodId
            if (initialSettings.passcodeEnabled && initialSettings.passcodeHash.isNotEmpty()) {
                _isAppLocked.value = true
            }
            refreshStorageOverview()
            openExplorerRoot()
        }
        viewModelScope.launch {
            mountedFolders.collect { folders ->
                if (_explorerBreadcrumbs.value.lastOrNull()?.node is ExplorerNode.Root) {
                    loadExplorerNode(ExplorerNode.Root)
                }
                refreshSelectiveBuckets(folders)
            }
        }
    }

    fun clearStatusBanner() {
        _statusBanner.value = null
    }

    fun refreshStorageOverview() {
        viewModelScope.launch {
            val overview = repo.inspectStorage()
            _storageOverview.value = overview
            _contactsPermissionGranted.value = repo.hasContactsPermission()
            refreshSelectiveBuckets(mountedFolders.value, overview)
        }
    }

    fun onContactsPermissionResult(granted: Boolean) {
        _contactsPermissionGranted.value = granted || repo.hasContactsPermission()
        refreshStorageOverview()
    }

    private fun refreshSelectiveBuckets(
        folders: List<MountedFolderEntity>,
        knownOverview: StorageOverview? = null
    ) {
        viewModelScope.launch {
            val overview = knownOverview ?: repo.inspectStorage()
            _storageOverview.value = overview
            _contactsPermissionGranted.value = repo.hasContactsPermission()
            val scanned = repo.scanSelectiveBuckets(
                mountedFolders = folders,
                availableBytes = overview.availableBytes,
                showHidden = settings.value.showHiddenFiles
            )
            // Preserve manually picked items that aren't in the mounted folder scan
            val currentMap = _selectiveBuckets.value
            val merged = SelectiveSubCategory.entries.associateWith { sub ->
                val baseList = scanned[sub].orEmpty()
                val baseIds = baseList.map { it.id }.toSet()
                val manualExtras = currentMap[sub].orEmpty().filter { it.id !in baseIds && it.uri != null && it.id.startsWith("uri:") }
                baseList + manualExtras
            }
            _selectiveBuckets.value = merged
        }
    }

    fun toggleSubBucketAll(sub: SelectiveSubCategory) {
        val items = _selectiveBuckets.value[sub].orEmpty()
        if (items.isEmpty()) return
        val current = _queuedTargets.value.toMutableList()
        val itemIds = items.map { it.id }.toSet()
        val allCurrentlySelected = items.all { item -> current.any { it.id == item.id } }
        if (allCurrentlySelected) {
            current.removeAll { it.id in itemIds }
        } else {
            val existing = current.map { it.id }.toMutableSet()
            items.forEach { item ->
                if (existing.add(item.id)) current.add(item)
            }
        }
        _queuedTargets.value = current
    }

    fun toggleParentGroupAll(group: SelectiveParentGroup) {
        val subs = SelectiveSubCategory.entries.filter { it.parentGroup == group }
        val allItems = subs.flatMap { _selectiveBuckets.value[it].orEmpty() }
        if (allItems.isEmpty()) return
        val current = _queuedTargets.value.toMutableList()
        val allIds = allItems.map { it.id }.toSet()
        val anySelected = current.any { it.id in allIds }
        if (anySelected) {
            current.removeAll { it.id in allIds }
        } else {
            val existing = current.map { it.id }.toMutableSet()
            allItems.forEach { item ->
                if (existing.add(item.id)) current.add(item)
            }
        }
        _queuedTargets.value = current
    }

    fun toggleSingleQueuedItem(item: ShredTargetItem) {
        val current = _queuedTargets.value.toMutableList()
        val idx = current.indexOfFirst { it.id == item.id }
        if (idx >= 0) current.removeAt(idx) else current.add(item)
        _queuedTargets.value = current
    }

    fun addPickedUrisToSubCategory(uris: List<Uri>, sub: SelectiveSubCategory) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val category = when (sub.parentGroup) {
                SelectiveParentGroup.MEDIA -> SelectiveCategory.PHOTOS_VIDEOS
                else -> SelectiveCategory.DOCUMENTS_FILES
            }
            val currentBuckets = _selectiveBuckets.value.toMutableMap()
            val bucketItems = currentBuckets[sub].orEmpty().toMutableList()
            val queued = _queuedTargets.value.toMutableList()
            val queuedIds = queued.map { it.id }.toMutableSet()
            val bucketIds = bucketItems.map { it.id }.toMutableSet()
            var added = 0
            for (uri in uris) {
                val built = repo.buildTargetFromUri(uri, category)?.copy(subCategory = sub)
                if (built != null) {
                    if (bucketIds.add(built.id)) bucketItems.add(built)
                    if (queuedIds.add(built.id)) {
                        queued.add(built)
                        added++
                    }
                }
            }
            currentBuckets[sub] = bucketItems
            _selectiveBuckets.value = currentBuckets
            _queuedTargets.value = queued
            if (added > 0) {
                _statusBanner.value = "Queued $added item(s) in ${sub.title}"
            }
        }
    }

    fun syncMethodWithDefault() {
        _selectedMethodId.value = settings.value.defaultMethodId
    }

    fun getRecommendedMethod(): ErasureMethod {
        val hasFreeSpaceOrMedia = _queuedTargets.value.any {
            it.isFreeSpaceWipe || it.category == SelectiveCategory.PHOTOS_VIDEOS || it.category == SelectiveCategory.MOUNTED_FOLDERS
        }
        return if (hasFreeSpaceOrMedia || _queuedTargets.value.isEmpty()) {
            ErasureCatalog.findById("bsi_vsitr")
        } else {
            ErasureCatalog.findById("bsi_vsitr")
        }
    }

    fun toggleCategoryTarget(category: SelectiveCategory) {
        viewModelScope.launch {
            val current = _queuedTargets.value.toMutableList()
            val overview = repo.inspectStorage()
            _storageOverview.value = overview

            when (category) {
                SelectiveCategory.FREE_SPACE -> {
                    val existing = current.indexOfFirst { it.isFreeSpaceWipe }
                    if (existing >= 0) {
                        current.removeAt(existing)
                    } else {
                        val mb = settings.value.freeSpaceOverwriteMb
                        val wipeBytes = if (mb <= 0L) {
                            (overview.availableBytes - 128L * 1024L * 1024L).coerceIn(16L * 1024L * 1024L, 1024L * 1024L * 1024L)
                        } else {
                            mb * 1024L * 1024L
                        }
                        current.add(
                            ShredTargetItem(
                                id = "cat:free_space",
                                title = "Free Space (${FormatUtils.formatBytes(wipeBytes)})",
                                subtitle = "Unallocated blocks • ${FormatUtils.formatBytes(overview.availableBytes)} free",
                                sizeBytes = wipeBytes,
                                category = SelectiveCategory.FREE_SPACE,
                                isFreeSpaceWipe = true
                            )
                        )
                    }
                }

                SelectiveCategory.APP_CACHE -> {
                    val existing = current.indexOfFirst { it.id == "cat:app_cache" }
                    if (existing >= 0) {
                        current.removeAt(existing)
                    } else {
                        current.add(
                            ShredTargetItem(
                                id = "cat:app_cache",
                                title = "App Cache & Temp Data",
                                subtitle = "${FormatUtils.formatBytes(overview.cacheBytes)} cached blocks",
                                sizeBytes = overview.cacheBytes,
                                category = SelectiveCategory.APP_CACHE
                            )
                        )
                    }
                }

                SelectiveCategory.CLIPBOARD -> {
                    val existing = current.indexOfFirst { it.isClipboardWipe }
                    if (existing >= 0) {
                        current.removeAt(existing)
                    } else {
                        current.add(
                            ShredTargetItem(
                                id = "cat:clipboard",
                                title = "System Clipboard",
                                subtitle = if (overview.clipboardPreview != null) "Contains clip data" else "Overwrite & clear buffer",
                                sizeBytes = 4096L,
                                category = SelectiveCategory.CLIPBOARD,
                                isClipboardWipe = true
                            )
                        )
                    }
                }

                SelectiveCategory.RESIDUAL_FILES -> {
                    val existing = current.indexOfFirst { it.id == "cat:residual_files" }
                    if (existing >= 0) {
                        current.removeAt(existing)
                    } else {
                        current.add(
                            ShredTargetItem(
                                id = "cat:residual_files",
                                title = "Residual App Files",
                                subtitle = "${FormatUtils.formatBytes(overview.residualBytes)} in app sandbox",
                                sizeBytes = overview.residualBytes,
                                category = SelectiveCategory.RESIDUAL_FILES
                            )
                        )
                    }
                }

                else -> Unit
            }
            _queuedTargets.value = current
        }
    }

    fun togglePrivacyGroup() {
        viewModelScope.launch {
            val current = _queuedTargets.value.toMutableList()
            val overview = repo.inspectStorage()
            _storageOverview.value = overview
            val hasPrivacy = current.any {
                it.id == "cat:app_cache" || it.isClipboardWipe
            }
            if (hasPrivacy) {
                current.removeAll { it.id == "cat:app_cache" || it.isClipboardWipe }
            } else {
                current.add(
                    ShredTargetItem(
                        id = "cat:app_cache",
                        title = "App Cache & Temp Data",
                        subtitle = "${FormatUtils.formatBytes(overview.cacheBytes)} cached blocks",
                        sizeBytes = overview.cacheBytes,
                        category = SelectiveCategory.APP_CACHE
                    )
                )
                current.add(
                    ShredTargetItem(
                        id = "cat:clipboard",
                        title = "System Clipboard",
                        subtitle = if (overview.clipboardPreview != null) "Contains clip data" else "Overwrite & clear buffer",
                        sizeBytes = 4096L,
                        category = SelectiveCategory.CLIPBOARD,
                        isClipboardWipe = true
                    )
                )
            }
            _queuedTargets.value = current
        }
    }

    fun clearCategories(vararg categories: SelectiveCategory) {
        val catSet = categories.toSet()
        _queuedTargets.value = _queuedTargets.value.filterNot { it.category in catSet }
    }

    fun addPickedDocumentUris(uris: List<Uri>, category: SelectiveCategory) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val current = _queuedTargets.value.toMutableList()
            val existingIds = current.map { it.id }.toMutableSet()
            var added = 0
            for (uri in uris) {
                val item = repo.buildTargetFromUri(uri, category)
                if (item != null && existingIds.add(item.id)) {
                    current.add(item)
                    added++
                }
            }
            _queuedTargets.value = current
            if (added > 0) {
                _statusBanner.value = "Queued $added item(s) for secure erasure"
            }
        }
    }

    fun mountSafFolder(treeUri: Uri, alsoQueueForShred: Boolean = false) {
        viewModelScope.launch {
            val entity = repo.persistMountedFolder(treeUri)
            if (alsoQueueForShred) {
                val item = repo.buildTargetFromMountedFolder(entity)
                val current = _queuedTargets.value.toMutableList()
                if (current.none { it.id == item.id }) {
                    current.add(item)
                    _queuedTargets.value = current
                }
            }
            if (_explorerBreadcrumbs.value.lastOrNull()?.node is ExplorerNode.Root) {
                loadExplorerNode(ExplorerNode.Root)
            }
            _statusBanner.value = "Mounted '${entity.displayName}'"
        }
    }

    fun unmountSafFolder(uriString: String) {
        viewModelScope.launch {
            repo.removeMountedFolder(uriString)
            _queuedTargets.value = _queuedTargets.value.filterNot { it.id == "tree:$uriString" }
            if (_explorerBreadcrumbs.value.lastOrNull()?.node is ExplorerNode.Root) {
                loadExplorerNode(ExplorerNode.Root)
            }
        }
    }

    fun removeQueuedTarget(id: String) {
        _queuedTargets.value = _queuedTargets.value.filterNot { it.id == id }
    }

    fun clearQueue() {
        _queuedTargets.value = emptyList()
    }

    fun selectMethod(methodId: String) {
        _selectedMethodId.value = methodId
    }

    fun startShredding() {
        if (_executionState.value is ShredExecutionState.Running) return
        val targets = _queuedTargets.value
        if (targets.isEmpty()) return
        val method = ErasureCatalog.findById(_selectedMethodId.value)
        val currentSettings = settings.value

        shredJob = viewModelScope.launch {
            _executionState.value = ShredExecutionState.Running(
                currentItemName = targets.first().title,
                currentItemIndex = 1,
                totalItems = targets.size,
                currentPass = 1,
                totalPasses = method.cycles,
                passLabel = method.passes.first().label,
                bytesProcessedInRun = 0L,
                totalBytesPlanned = 1L,
                speedBytesPerSec = 0L,
                etaSeconds = 0L
            )

            val (result, savedReport) = repo.executeShredAndSaveReport(
                targets = targets,
                method = method,
                freeSpaceOverwriteMb = currentSettings.freeSpaceOverwriteMb,
                saveReport = currentSettings.enableErasureReport,
                onProgress = { progress ->
                    _executionState.value = progress
                }
            )

            _executionState.value = ShredExecutionState.Completed(
                reportId = savedReport?.id,
                reportCode = savedReport?.reportCode ?: "REPORT-DISABLED",
                methodName = method.name,
                cycles = method.cycles,
                itemsShredded = result.itemsShredded,
                itemsFailed = result.itemsFailed,
                totalBytesShredded = result.totalBytesOverwritten,
                durationMillis = result.durationMillis,
                verificationSha256 = result.verificationSha256,
                wasCancelled = result.wasCancelled
            )

            if (!result.wasCancelled) {
                _queuedTargets.value = emptyList()
            }
            refreshStorageOverview()
            val currentNode = _explorerBreadcrumbs.value.lastOrNull()?.node ?: ExplorerNode.Root
            loadExplorerNode(currentNode)
        }
    }

    fun cancelShredding() {
        shredJob?.cancel()
    }

    fun resetExecutionState() {
        _executionState.value = ShredExecutionState.Idle
    }

    // --- File Explorer ---
    fun openExplorerRoot() {
        _explorerBreadcrumbs.value = listOf(ExplorerBreadcrumb("Storages", ExplorerNode.Root))
        _explorerSelectedIds.value = emptySet()
        loadExplorerNode(ExplorerNode.Root)
    }

    fun openExplorerEntry(entry: ExplorerEntry) {
        if (!entry.isDirectory) {
            toggleExplorerSelection(entry.id)
            return
        }
        val nextNode = when {
            entry.id == "storage:internal" -> ExplorerNode.InternalStorage
            entry.id == "root:cache" -> ExplorerNode.AppCache
            entry.id == "root:residual" -> ExplorerNode.ResidualFiles
            entry.localFile != null -> ExplorerNode.LocalDir(entry.localFile, entry.name)
            entry.uri != null && entry.isMountedRoot -> ExplorerNode.SafDir(entry.uri, null, entry.name)
            entry.uri != null -> {
                val docId = DocumentsContract.getDocumentId(entry.uri)
                val rootTreeUri = (_explorerBreadcrumbs.value.lastOrNull()?.node as? ExplorerNode.SafDir)?.treeUri
                    ?: entry.uri
                ExplorerNode.SafDir(rootTreeUri, docId, entry.name)
            }
            else -> return
        }
        _explorerBreadcrumbs.value = _explorerBreadcrumbs.value + ExplorerBreadcrumb(entry.name, nextNode)
        _explorerSelectedIds.value = emptySet()
        loadExplorerNode(nextNode)
    }

    fun navigateExplorerBreadcrumb(index: Int) {
        val crumbs = _explorerBreadcrumbs.value
        if (index !in crumbs.indices) return
        val sliced = crumbs.take(index + 1)
        _explorerBreadcrumbs.value = sliced
        _explorerSelectedIds.value = emptySet()
        loadExplorerNode(sliced.last().node)
    }

    fun navigateExplorerUp(): Boolean {
        val crumbs = _explorerBreadcrumbs.value
        return if (crumbs.size > 1) {
            navigateExplorerBreadcrumb(crumbs.lastIndex - 1)
            true
        } else {
            false
        }
    }

    private fun loadExplorerNode(node: ExplorerNode) {
        viewModelScope.launch {
            val showHidden = settings.value.showHiddenFiles
            val overview = repo.inspectStorage()
            _storageOverview.value = overview

            val entries = when (node) {
                ExplorerNode.Root -> {
                    val base = mutableListOf(
                        ExplorerEntry(
                            id = "storage:internal",
                            name = "Internal Storage",
                            subtitle = "${FormatUtils.formatIecBytes(overview.usedBytes)} / ${FormatUtils.formatIecBytes(overview.totalBytes)}",
                            isDirectory = true,
                            sizeBytes = overview.usedBytes,
                            lastModifiedMillis = System.currentTimeMillis()
                        )
                    )
                    mountedFolders.value.forEach { folder ->
                        base.add(
                            ExplorerEntry(
                                id = "mount:${folder.uriString}",
                                name = folder.displayName,
                                subtitle = "Mounted folder (SAF)",
                                isDirectory = true,
                                sizeBytes = 0L,
                                lastModifiedMillis = folder.addedAtMillis,
                                uri = Uri.parse(folder.uriString),
                                isMountedRoot = true
                            )
                        )
                    }
                    base
                }
                ExplorerNode.InternalStorage -> {
                    val list = mutableListOf(
                        ExplorerEntry(
                            id = "root:cache",
                            name = "App Cache & Temp",
                            subtitle = "${FormatUtils.formatIecBytes(overview.cacheBytes).ifEmpty { "0 B" }} • Cached temporary files",
                            isDirectory = true,
                            sizeBytes = overview.cacheBytes,
                            lastModifiedMillis = System.currentTimeMillis()
                        ),
                        ExplorerEntry(
                            id = "root:residual",
                            name = "App Residual Files",
                            subtitle = "${FormatUtils.formatIecBytes(overview.residualBytes).ifEmpty { "0 B" }} • Sandbox files",
                            isDirectory = true,
                            sizeBytes = overview.residualBytes,
                            lastModifiedMillis = System.currentTimeMillis()
                        )
                    )
                    mountedFolders.value.forEach { folder ->
                        list.add(
                            ExplorerEntry(
                                id = "mount:${folder.uriString}",
                                name = folder.displayName,
                                subtitle = "Mounted device folder (SAF)",
                                isDirectory = true,
                                sizeBytes = 0L,
                                lastModifiedMillis = folder.addedAtMillis,
                                uri = Uri.parse(folder.uriString),
                                isMountedRoot = true
                            )
                        )
                    }
                    list
                }
                ExplorerNode.AppCache -> repo.listCacheEntries(showHidden)
                ExplorerNode.ResidualFiles -> repo.listResidualEntries(showHidden)
                is ExplorerNode.LocalDir -> repo.listLocalDirectoryEntries(node.dir, showHidden)
                is ExplorerNode.SafDir -> repo.listSafChildren(node.treeUri, node.docId, showHidden)
            }
            _explorerRawEntries.value = entries
        }
    }

    fun toggleExplorerSelection(id: String) {
        val current = _explorerSelectedIds.value.toMutableSet()
        if (!current.add(id)) {
            current.remove(id)
        }
        _explorerSelectedIds.value = current
    }

    fun selectAllExplorer() {
        _explorerSelectedIds.value = explorerEntries.value.map { it.id }.toSet()
    }

    fun clearExplorerSelection() {
        _explorerSelectedIds.value = emptySet()
    }

    fun setExplorerSort(sort: ExplorerSort) {
        _explorerSort.value = sort
    }

    fun queueExplorerSelectionForShred(): Boolean {
        val selectedIds = _explorerSelectedIds.value
        val selectedEntries = explorerEntries.value.filter { it.id in selectedIds }
        if (selectedEntries.isEmpty()) return false

        val targets = selectedEntries.map { entry ->
            when (entry.id) {
                "root:cache" -> ShredTargetItem(
                    id = "cat:app_cache",
                    title = "App Cache & Temp Data",
                    subtitle = "${FormatUtils.formatBytes(entry.sizeBytes)} cached blocks",
                    sizeBytes = entry.sizeBytes,
                    category = SelectiveCategory.APP_CACHE
                )
                "root:residual" -> ShredTargetItem(
                    id = "cat:residual_files",
                    title = "Residual App Files",
                    subtitle = "${FormatUtils.formatBytes(entry.sizeBytes)} in app sandbox",
                    sizeBytes = entry.sizeBytes,
                    category = SelectiveCategory.RESIDUAL_FILES
                )
                else -> ShredTargetItem(
                    id = entry.id,
                    title = entry.name,
                    subtitle = entry.subtitle,
                    sizeBytes = entry.sizeBytes,
                    category = if (entry.isDirectory) SelectiveCategory.MOUNTED_FOLDERS else SelectiveCategory.DOCUMENTS_FILES,
                    uri = entry.uri,
                    localFile = entry.localFile,
                    isDirectory = entry.isDirectory,
                    mimeType = entry.mimeType
                )
            }
        }
        _queuedTargets.value = targets
        _explorerSelectedIds.value = emptySet()
        syncMethodWithDefault()
        return true
    }

    suspend fun computeFileSha256(entry: ExplorerEntry): String = repo.computeSha256(entry)

    // --- Passcode & Settings ---
    fun verifyPasscode(pin: String): Boolean {
        val ok = SettingsDataStore.hashPin(pin) == settings.value.passcodeHash
        if (ok) {
            _isAppLocked.value = false
        }
        return ok
    }

    fun enablePasscode(pin: String) {
        viewModelScope.launch {
            repo.settingsStore.setPasscode(enabled = true, pin = pin)
        }
    }

    fun disablePasscode() {
        viewModelScope.launch {
            repo.settingsStore.setPasscode(enabled = false, pin = null)
        }
    }

    fun setLanguage(code: String) {
        viewModelScope.launch { repo.settingsStore.setLanguage(code) }
    }

    fun setSkipScan(enabled: Boolean) {
        viewModelScope.launch { repo.settingsStore.setSkipScan(enabled) }
    }

    fun setDefaultMethod(methodId: String) {
        viewModelScope.launch {
            repo.settingsStore.setDefaultMethod(methodId)
            _selectedMethodId.value = methodId
        }
    }

    fun setFreeSpaceOverwriteMb(mb: Long) {
        viewModelScope.launch { repo.settingsStore.setFreeSpaceOverwriteMb(mb) }
    }

    fun setEnableErasureReport(enabled: Boolean) {
        viewModelScope.launch { repo.settingsStore.setEnableErasureReport(enabled) }
    }

    fun setThumbnailPreview(enabled: Boolean) {
        viewModelScope.launch { repo.settingsStore.setThumbnailPreview(enabled) }
    }

    fun setShowHiddenFiles(enabled: Boolean) {
        viewModelScope.launch {
            repo.settingsStore.setShowHiddenFiles(enabled)
            val currentNode = _explorerBreadcrumbs.value.lastOrNull()?.node ?: ExplorerNode.Root
            loadExplorerNode(currentNode)
        }
    }

    // --- Reports ---
    suspend fun getReportById(id: Long): ErasureReportEntity? = repo.getReportById(id)

    fun deleteReport(id: Long) {
        viewModelScope.launch { repo.deleteReport(id) }
    }

    fun clearAllReports() {
        viewModelScope.launch { repo.clearAllReports() }
    }

    fun buildShareableReportText(report: ErasureReportEntity): String {
        return buildString {
            appendLine("=== iSHREDDER EXCELLENT ERASURE CERTIFICATE ===")
            appendLine("Certificate ID : ${report.reportCode}")
            appendLine("Timestamp      : ${FormatUtils.formatTimestamp(report.timestampMillis)}")
            appendLine("Status         : ${report.status}")
            appendLine("Algorithm      : ${report.methodName} [${report.methodTier}]")
            appendLine("Overwrite Passes: ${report.cycles}")
            appendLine("Items Shredded : ${report.itemsShreddedCount} (Failed: ${report.itemsFailedCount})")
            appendLine("Bytes Written  : ${FormatUtils.formatBytes(report.totalBytesShredded)}")
            appendLine("Duration       : ${FormatUtils.formatDuration(report.durationMillis)}")
            appendLine("Targets        : ${report.targetSummary}")
            appendLine("SHA-256 Digest : ${report.verificationSha256}")
            appendLine()
            appendLine("--- Execution Log ---")
            appendLine(report.itemsDetailLog)
        }
    }

    // --- USDT-Only In-App Purchases ---
    fun setUsdtMerchantAddress(address: String) {
        viewModelScope.launch {
            repo.settingsStore.setUsdtMerchantAddress(address)
            _statusBanner.value = if (address.isBlank()) {
                "Cleared merchant receiving wallet"
            } else {
                "Saved USDT receiving address"
            }
        }
    }

    fun verifyUsdtPurchase(
        item: UsdtIapItem,
        network: UsdtNetwork,
        txHash: String,
        merchantAddress: String
    ) {
        if (_usdtVerificationState.value is UsdtVerificationUiState.Verifying) return
        viewModelScope.launch {
            _usdtVerificationState.value = UsdtVerificationUiState.Verifying
            when (val res = repo.verifyAndSaveUsdtPurchase(item, network, txHash, merchantAddress)) {
                is UsdtVerificationResult.Verified -> {
                    _usdtVerificationState.value = UsdtVerificationUiState.Success(
                        itemTitle = item.title,
                        blockNumber = res.blockNumber
                    )
                    _statusBanner.value = "Verified ${item.title} (${item.usdtAmount} USDT) in block ${res.blockNumber}"
                }
                is UsdtVerificationResult.Failed -> {
                    _usdtVerificationState.value = UsdtVerificationUiState.Error(res.reason)
                }
            }
        }
    }

    fun resetUsdtVerificationState() {
        _usdtVerificationState.value = UsdtVerificationUiState.Idle
    }

    // --- Theme UI ---
    fun setUiThemeStyle(styleId: String) {
        viewModelScope.launch {
            repo.settingsStore.setUiThemeStyle(styleId)
        }
    }

    fun setDynamicColorEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repo.settingsStore.setDynamicColorEnabled(enabled)
        }
    }

    fun setOledModeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repo.settingsStore.setOledModeEnabled(enabled)
        }
    }

    fun setUiAccentId(accentId: String) {
        viewModelScope.launch {
            repo.settingsStore.setUiAccentId(accentId)
        }
    }

    // --- 2026 Excellent Suite ---
    fun inspectExifPhotos(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _isRunning2026Task.value = true
            _lastScrubbedResult.value = null
            try {
                _exifScanResults.value = repo.inspectExifPhotos(uris)
            } finally {
                _isRunning2026Task.value = false
            }
        }
    }

    fun scrubExifPhoto(report: ExifPhotoReport, destUri: Uri) {
        viewModelScope.launch {
            _isRunning2026Task.value = true
            try {
                val res = repo.scrubExifPhoto(report, destUri)
                _lastScrubbedResult.value = res
                _statusBanner.value = if (res != null) {
                    "Sanitized '${res.originalName}' (${res.strippedTagCount} EXIF risk tags stripped)"
                } else {
                    "Unable to export sanitized photo"
                }
            } finally {
                _isRunning2026Task.value = false
            }
        }
    }

    fun clearExifScanResults() {
        _exifScanResults.value = emptyList()
        _lastScrubbedResult.value = null
    }

    fun updateAutoShredConfig(
        enabled: Boolean,
        intervalHours: Int,
        includeClipboard: Boolean,
        methodId: String
    ) {
        viewModelScope.launch {
            repo.settingsStore.setAutoShredConfig(
                enabled = enabled,
                intervalHours = intervalHours,
                includeClipboard = includeClipboard,
                methodId = methodId
            )
            val scheduled = repo.syncAutoShredJob(enabled, intervalHours)
            _statusBanner.value = if (enabled) {
                if (scheduled) "Auto-Shred Guard armed (every ${intervalHours}h)"
                else "Auto-Shred config saved"
            } else {
                "Auto-Shred Guard disarmed"
            }
        }
    }

    fun runImmediate2026HygieneSweep(includeClipboard: Boolean) {
        if (_isRunning2026Task.value) return
        viewModelScope.launch {
            _isRunning2026Task.value = true
            try {
                val curSettings = settings.value
                val method = ErasureCatalog.findById(curSettings.autoShredMethodId)
                val (result, report) = repo.runQuickHygieneSweep(
                    includeClipboard = includeClipboard,
                    method = method,
                    freeSpaceOverwriteMb = curSettings.freeSpaceOverwriteMb,
                    saveReport = curSettings.enableErasureReport
                )
                refreshStorageOverview()
                val codeSuffix = report?.let { " • ${it.reportCode}" }.orEmpty()
                _statusBanner.value = "2026 Hygiene Sweep complete (${FormatUtils.formatBytes(result.totalBytesOverwritten)}$codeSuffix)"
            } finally {
                _isRunning2026Task.value = false
            }
        }
    }

    fun runHardwareBenchmark(testSizeMb: Int = 4) {
        if (_isRunning2026Task.value) return
        viewModelScope.launch {
            _isRunning2026Task.value = true
            try {
                val res = repo.runHardwareBenchmark(testSizeMb)
                _benchmarkResult.value = res
                _statusBanner.value = "Benchmark complete: ${String.format("%.1f", res.flashWriteMbPerSec)} MB/s flash write"
            } finally {
                _isRunning2026Task.value = false
            }
        }
    }

    // --- Factory Reset ---
    fun startPreResetDeviceWipe(
        methodId: String,
        includeFreeSpace: Boolean,
        includeMountedFolders: Boolean,
        includeCacheAndResidual: Boolean,
        includeClipboard: Boolean,
        includeContacts: Boolean
    ) {
        if (_executionState.value is ShredExecutionState.Running) return
        viewModelScope.launch {
            val targets = repo.buildDevicePreResetTargets(
                includeFreeSpace = includeFreeSpace,
                includeMountedFolders = includeMountedFolders,
                includeCacheAndResidual = includeCacheAndResidual,
                includeClipboard = includeClipboard,
                includeContacts = includeContacts,
                freeSpaceOverwriteMb = settings.value.freeSpaceOverwriteMb
            )
            if (targets.isEmpty()) {
                _statusBanner.value = "Select at least one wipe category or mount a folder first."
                return@launch
            }
            _selectedMethodId.value = methodId
            _queuedTargets.value = targets
            startShredding()
        }
    }

    fun performAppFactoryReset(
        clearUsdtReceipts: Boolean,
        onCompleted: () -> Unit
    ) {
        if (_executionState.value is ShredExecutionState.Running) return
        viewModelScope.launch {
            _queuedTargets.value = emptyList()
            _explorerSelectedIds.value = emptySet()
            _selectedMethodId.value = "bsi_vsitr"
            _isAppLocked.value = false
            repo.performAppFactoryReset(clearUsdtReceipts = clearUsdtReceipts)
            refreshStorageOverview()
            openExplorerRoot()
            _statusBanner.value = "iShredder restored to factory defaults"
            onCompleted()
        }
    }
}
