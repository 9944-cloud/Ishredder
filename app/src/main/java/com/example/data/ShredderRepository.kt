package com.example.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import com.example.engine.AutoShredScheduler
import com.example.engine.ExifPhotoReport
import com.example.engine.HardwareBenchmarkResult
import com.example.engine.PrivacyAuditReport
import com.example.engine.ScrubbedPhotoResult
import com.example.engine.SecureShredderEngine
import com.example.engine.SecuritySuite2026Engine
import com.example.engine.ShredRunResult
import com.example.engine.StorageInspector
import com.example.engine.UsdtBlockchainVerifier
import com.example.engine.UsdtVerificationResult
import com.example.model.ErasureCatalog
import com.example.model.ErasureMethod
import com.example.model.ExplorerEntry
import com.example.model.SelectiveCategory
import com.example.model.SelectiveSubCategory
import com.example.model.ShredExecutionState
import com.example.model.ShredTargetItem
import com.example.model.StorageOverview
import com.example.model.UsdtIapItem
import com.example.model.UsdtNetwork
import com.example.util.FormatUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.io.File

class ShredderRepository(private val context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val reportDao = db.erasureReportDao()
    private val mountedFolderDao = db.mountedFolderDao()
    private val usdtPurchaseDao = db.usdtPurchaseDao()
    val settingsStore = SettingsDataStore(context)
    val storageInspector = StorageInspector(context)
    private val shredderEngine = SecureShredderEngine(context, storageInspector)
    private val usdtVerifier = UsdtBlockchainVerifier()
    private val securitySuite2026 = SecuritySuite2026Engine(context)

    val settingsFlow: Flow<AppSettings> = settingsStore.settingsFlow
    val allReportsFlow: Flow<List<ErasureReportEntity>> = reportDao.observeAllReports()
    val mountedFoldersFlow: Flow<List<MountedFolderEntity>> = mountedFolderDao.observeMountedFolders()
    val usdtPurchasesFlow: Flow<List<UsdtPurchaseEntity>> = usdtPurchaseDao.observePurchases()

    suspend fun inspectStorage(): StorageOverview = storageInspector.inspectStorageOverview()

    fun hasContactsPermission(): Boolean = storageInspector.hasContactsPermission()

    suspend fun scanSelectiveBuckets(
        mountedFolders: List<MountedFolderEntity>,
        availableBytes: Long,
        showHidden: Boolean
    ): Map<SelectiveSubCategory, List<ShredTargetItem>> =
        storageInspector.scanSelectiveSubBuckets(mountedFolders, availableBytes, showHidden)

    suspend fun buildTargetFromUri(uri: Uri, category: SelectiveCategory): ShredTargetItem? {
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } catch (_: Exception) {
            // Single document pickers may grant temporary URI permissions without persistable flags
        }
        return storageInspector.buildTargetFromUri(uri, category)
    }

    suspend fun persistMountedFolder(treeUri: Uri): MountedFolderEntity {
        try {
            context.contentResolver.takePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } catch (_: Exception) {
        }
        val docId = DocumentsContract.getTreeDocumentId(treeUri)
        val displayName = docId.substringAfterLast(':', docId).substringAfterLast('/').ifEmpty { "Mounted Folder" }
        val entity = MountedFolderEntity(
            uriString = treeUri.toString(),
            displayName = displayName,
            addedAtMillis = System.currentTimeMillis()
        )
        mountedFolderDao.insertFolder(entity)
        return entity
    }

    suspend fun removeMountedFolder(uriString: String) {
        try {
            context.contentResolver.releasePersistableUriPermission(
                Uri.parse(uriString),
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } catch (_: Exception) {
        }
        mountedFolderDao.deleteFolder(uriString)
    }

    suspend fun buildTargetFromMountedFolder(folder: MountedFolderEntity): ShredTargetItem =
        storageInspector.buildTargetFromMountedFolder(folder)

    suspend fun listCacheEntries(showHidden: Boolean): List<ExplorerEntry> =
        storageInspector.listCacheExplorerEntries(showHidden)

    suspend fun listResidualEntries(showHidden: Boolean): List<ExplorerEntry> =
        storageInspector.listResidualExplorerEntries(showHidden)

    suspend fun listLocalDirectoryEntries(dir: File, showHidden: Boolean): List<ExplorerEntry> =
        storageInspector.listLocalDirectoryEntries(dir, showHidden)

    suspend fun listSafChildren(treeUri: Uri, parentDocId: String?, showHidden: Boolean): List<ExplorerEntry> =
        storageInspector.listSafChildren(treeUri, parentDocId, showHidden)

    suspend fun computeSha256(entry: ExplorerEntry): String =
        storageInspector.computeSha256(entry)

    suspend fun executeShredAndSaveReport(
        targets: List<ShredTargetItem>,
        method: ErasureMethod,
        freeSpaceOverwriteMb: Long,
        saveReport: Boolean,
        onProgress: (ShredExecutionState.Running) -> Unit
    ): Pair<ShredRunResult, ErasureReportEntity?> {
        val result = shredderEngine.executeShred(targets, method, freeSpaceOverwriteMb, onProgress)
        val reportCode = "ISR-${(System.currentTimeMillis() % 1_000_000_000L).toString().padStart(9, '0')}"
        val targetSummary = targets.joinToString(", ") { it.title }.take(180)
        val status = when {
            result.wasCancelled -> "CANCELLED"
            result.itemsFailed > 0 && result.itemsShredded == 0 -> "FAILED"
            result.itemsFailed > 0 -> "PARTIAL"
            else -> "VERIFIED"
        }

        val entity = if (saveReport) {
            val draft = ErasureReportEntity(
                reportCode = reportCode,
                timestampMillis = System.currentTimeMillis(),
                methodId = method.id,
                methodName = method.name,
                methodTier = method.tier.badge,
                cycles = method.cycles,
                itemsShreddedCount = result.itemsShredded,
                itemsFailedCount = result.itemsFailed,
                totalBytesShredded = result.totalBytesOverwritten,
                durationMillis = result.durationMillis,
                status = status,
                targetSummary = targetSummary.ifEmpty { "Selected Data" },
                itemsDetailLog = result.detailLog.ifEmpty { "No entries logged." },
                verificationSha256 = result.verificationSha256
            )
            val id = reportDao.insertReport(draft)
            draft.copy(id = id)
        } else {
            null
        }
        return result to entity
    }

    suspend fun getReportById(id: Long): ErasureReportEntity? = reportDao.getReportById(id)

    suspend fun deleteReport(id: Long) = reportDao.deleteReportById(id)

    suspend fun clearAllReports() = reportDao.clearAllReports()

    suspend fun verifyAndSaveUsdtPurchase(
        item: UsdtIapItem,
        network: UsdtNetwork,
        txHashInput: String,
        merchantAddressInput: String
    ): UsdtVerificationResult {
        val txHash = txHashInput.trim()
        val merchantAddress = merchantAddressInput.trim()
        val existing = usdtPurchaseDao.findByTxHash(txHash)
        if (existing != null) {
            return UsdtVerificationResult.Failed(
                "This transaction hash has already been redeemed for '${existing.itemTitle}'."
            )
        }
        val verification = usdtVerifier.verifyTransaction(
            txHashInput = txHash,
            network = network,
            merchantAddressInput = merchantAddress
        )
        if (verification is UsdtVerificationResult.Verified) {
            usdtPurchaseDao.insertPurchase(
                UsdtPurchaseEntity(
                    itemId = item.id,
                    itemTitle = item.title,
                    usdtAmount = item.usdtAmount,
                    networkCode = network.shortCode,
                    txHash = txHash,
                    merchantAddress = merchantAddress,
                    status = "VERIFIED",
                    blockNumber = verification.blockNumber,
                    purchasedAtMillis = System.currentTimeMillis()
                )
            )
        }
        return verification
    }

    suspend fun clearAllMountedFolders() {
        val folders = mountedFolderDao.observeMountedFolders().first()
        folders.forEach { folder ->
            try {
                context.contentResolver.releasePersistableUriPermission(
                    Uri.parse(folder.uriString),
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (_: Exception) {
            }
        }
        mountedFolderDao.clearAllFolders()
    }

    suspend fun buildDevicePreResetTargets(
        includeFreeSpace: Boolean,
        includeMountedFolders: Boolean,
        includeCacheAndResidual: Boolean,
        includeClipboard: Boolean,
        includeContacts: Boolean,
        freeSpaceOverwriteMb: Long
    ): List<ShredTargetItem> {
        val overview = inspectStorage()
        val folders = mountedFolderDao.observeMountedFolders().first()
        val targets = mutableListOf<ShredTargetItem>()

        if (includeFreeSpace) {
            val wipeBytes = if (freeSpaceOverwriteMb <= 0L) {
                (overview.availableBytes - 128L * 1024L * 1024L).coerceIn(16L * 1024L * 1024L, 1024L * 1024L * 1024L)
            } else {
                freeSpaceOverwriteMb * 1024L * 1024L
            }
            targets.add(
                ShredTargetItem(
                    id = "reset:free_space",
                    title = "Internal Free Space (${FormatUtils.formatBytes(wipeBytes)})",
                    subtitle = "Overwrite unallocated blocks prior to reset",
                    sizeBytes = wipeBytes,
                    category = SelectiveCategory.FREE_SPACE,
                    isFreeSpaceWipe = true
                )
            )
        }

        if (includeMountedFolders) {
            folders.forEach { folder ->
                targets.add(buildTargetFromMountedFolder(folder))
            }
        }

        if (includeCacheAndResidual) {
            targets.add(
                ShredTargetItem(
                    id = "reset:app_cache",
                    title = "App Cache & Temp Data",
                    subtitle = "${FormatUtils.formatBytes(overview.cacheBytes)} cached blocks",
                    sizeBytes = overview.cacheBytes,
                    category = SelectiveCategory.APP_CACHE
                )
            )
            targets.add(
                ShredTargetItem(
                    id = "reset:residual_files",
                    title = "App Residual Sandbox Files",
                    subtitle = "${FormatUtils.formatBytes(overview.residualBytes)} sandbox files",
                    sizeBytes = overview.residualBytes,
                    category = SelectiveCategory.RESIDUAL_FILES
                )
            )
        }

        if (includeClipboard) {
            targets.add(
                ShredTargetItem(
                    id = "reset:clipboard",
                    title = "System Clipboard Buffer",
                    subtitle = "CSPRNG overwrite & purge",
                    sizeBytes = 4096L,
                    category = SelectiveCategory.CLIPBOARD,
                    isClipboardWipe = true
                )
            )
        }

        if (includeContacts && hasContactsPermission()) {
            val buckets = scanSelectiveBuckets(
                mountedFolders = emptyList(),
                availableBytes = overview.availableBytes,
                showHidden = false
            )
            targets.addAll(buckets[SelectiveSubCategory.CONTACTS].orEmpty())
        }

        return targets
    }

    suspend fun performAppFactoryReset(clearUsdtReceipts: Boolean) {
        val quickMethod = ErasureCatalog.allMethods.first()
        val sandboxTargets = listOf(
            ShredTargetItem(
                id = "factory:cache",
                title = "App Cache & Temp",
                subtitle = "Sandbox cache wipe",
                sizeBytes = 4096L,
                category = SelectiveCategory.APP_CACHE
            ),
            ShredTargetItem(
                id = "factory:residual",
                title = "App Residual Files",
                subtitle = "Sandbox residual wipe",
                sizeBytes = 4096L,
                category = SelectiveCategory.RESIDUAL_FILES
            ),
            ShredTargetItem(
                id = "factory:clipboard",
                title = "System Clipboard",
                subtitle = "Clipboard purge",
                sizeBytes = 4096L,
                category = SelectiveCategory.CLIPBOARD,
                isClipboardWipe = true
            )
        )
        shredderEngine.executeShred(
            targets = sandboxTargets,
            method = quickMethod,
            freeSpaceOverwriteMb = 64L,
            onProgress = {}
        )
        clearAllMountedFolders()
        reportDao.clearAllReports()
        if (clearUsdtReceipts) {
            usdtPurchaseDao.clearAllPurchases()
        }
        AutoShredScheduler.syncSchedule(context, enabled = false, intervalHours = 24)
        settingsStore.resetToDefaults()
    }

    // --- 2026 Excellent Suite ---
    fun computePrivacyAudit(
        overview: StorageOverview,
        settings: AppSettings,
        mountedFolders: List<MountedFolderEntity>,
        reports: List<ErasureReportEntity>
    ): PrivacyAuditReport = securitySuite2026.computePrivacyAudit(
        overview = overview,
        settings = settings,
        mountedFolders = mountedFolders,
        reports = reports
    )

    suspend fun inspectExifPhotos(uris: List<Uri>): List<ExifPhotoReport> =
        securitySuite2026.inspectPhotoExif(uris)

    suspend fun scrubExifPhoto(report: ExifPhotoReport, destUri: Uri): ScrubbedPhotoResult? =
        securitySuite2026.scrubPhotoToSafUri(report, destUri)

    suspend fun runHardwareBenchmark(testSizeMb: Int): HardwareBenchmarkResult =
        securitySuite2026.runHardwareBenchmark(testSizeMb)

    suspend fun runQuickHygieneSweep(
        includeClipboard: Boolean,
        method: ErasureMethod,
        freeSpaceOverwriteMb: Long,
        saveReport: Boolean
    ): Pair<ShredRunResult, ErasureReportEntity?> {
        val overview = inspectStorage()
        val targets = mutableListOf(
            ShredTargetItem(
                id = "hygiene:cache",
                title = "2026 Deep Cache Sanitization",
                subtitle = "${FormatUtils.formatBytes(overview.cacheBytes)} cached blocks",
                sizeBytes = overview.cacheBytes,
                category = SelectiveCategory.APP_CACHE
            ),
            ShredTargetItem(
                id = "hygiene:residual",
                title = "2026 Residual Sandbox Purge",
                subtitle = "${FormatUtils.formatBytes(overview.residualBytes)} sandbox blocks",
                sizeBytes = overview.residualBytes,
                category = SelectiveCategory.RESIDUAL_FILES
            )
        )
        if (includeClipboard) {
            targets.add(
                ShredTargetItem(
                    id = "hygiene:clipboard",
                    title = "RAM Clipboard Scrub",
                    subtitle = "CSPRNG overwrite",
                    sizeBytes = 4096L,
                    category = SelectiveCategory.CLIPBOARD,
                    isClipboardWipe = true
                )
            )
        }
        val res = executeShredAndSaveReport(
            targets = targets,
            method = method,
            freeSpaceOverwriteMb = freeSpaceOverwriteMb,
            saveReport = saveReport,
            onProgress = {}
        )
        settingsStore.recordAutoShredRun(System.currentTimeMillis())
        return res
    }

    fun syncAutoShredJob(enabled: Boolean, intervalHours: Int): Boolean =
        AutoShredScheduler.syncSchedule(context, enabled, intervalHours)
}
