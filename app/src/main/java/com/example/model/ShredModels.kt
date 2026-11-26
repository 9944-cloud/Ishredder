package com.example.model

import android.net.Uri
import java.io.File

enum class SelectiveCategory(val title: String, val subtitle: String) {
    FREE_SPACE("Free Space", "Overwrite unallocated storage blocks safely"),
    PHOTOS_VIDEOS("Photos & Videos", "Select media items via system document picker"),
    DOCUMENTS_FILES("Documents & Files", "Select PDFs, archives, and sensitive files"),
    MOUNTED_FOLDERS("Folders", "Mount a device folder to shred files or subtrees"),
    APP_CACHE("App Cache & Temp Data", "Wipe cached thumbnails, shaders & temp files"),
    CLIPBOARD("Clipboard", "Overwrite and purge sensitive clipboard history"),
    RESIDUAL_FILES("Residual App Files", "Scrub leftover exported or staging files"),
    CONTACTS("Contacts", "Overwrite and remove selected device contacts")
}

enum class SelectiveSubCategory(
    val title: String,
    val parentGroup: SelectiveParentGroup,
    val hasSearchBadge: Boolean = false
) {
    INTERNAL_STORAGE("Internal Storage", SelectiveParentGroup.FREE_SPACE),
    APKS_AABS("APKs & AABs", SelectiveParentGroup.FILES_FOLDERS, hasSearchBadge = true),
    DOWNLOADS("Downloads", SelectiveParentGroup.FILES_FOLDERS, hasSearchBadge = true),
    EMPTY_FOLDERS("Empty Folders", SelectiveParentGroup.FILES_FOLDERS, hasSearchBadge = true),
    TEMPORARY_FILES("Temporary Files", SelectiveParentGroup.FILES_FOLDERS),
    TRASH_BIN("Trash Bin", SelectiveParentGroup.FILES_FOLDERS, hasSearchBadge = true),
    CLIPBOARD("Clipboard", SelectiveParentGroup.PRIVACY),
    CONTACTS("Contacts", SelectiveParentGroup.PRIVACY),
    WHATSAPP("WhatsApp", SelectiveParentGroup.PRIVACY, hasSearchBadge = true),
    TELEGRAM("Telegram", SelectiveParentGroup.PRIVACY, hasSearchBadge = true),
    THUMBNAILS("Thumbnails", SelectiveParentGroup.MEDIA, hasSearchBadge = true),
    SONGS_VOICE("Songs & Voice Messages", SelectiveParentGroup.MEDIA, hasSearchBadge = true),
    VIDEO("Video", SelectiveParentGroup.MEDIA, hasSearchBadge = true),
    PHOTOS("Photos", SelectiveParentGroup.MEDIA, hasSearchBadge = true)
}

enum class SelectiveParentGroup(val title: String) {
    FREE_SPACE("Free Space"),
    FILES_FOLDERS("Files & Folders"),
    PRIVACY("Privacy"),
    MEDIA("Media")
}

data class ShredTargetItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val sizeBytes: Long,
    val category: SelectiveCategory,
    val subCategory: SelectiveSubCategory? = null,
    val uri: Uri? = null,
    val localFile: File? = null,
    val contactRawId: Long? = null,
    val isDirectory: Boolean = false,
    val isFreeSpaceWipe: Boolean = false,
    val isClipboardWipe: Boolean = false,
    val mimeType: String? = null
)

data class StorageOverview(
    val totalBytes: Long = 0L,
    val availableBytes: Long = 0L,
    val usedBytes: Long = 0L,
    val cacheBytes: Long = 0L,
    val residualBytes: Long = 0L,
    val clipboardPreview: String? = null,
    val connectedRemovableVolumes: List<String> = emptyList()
)

data class ExplorerEntry(
    val id: String,
    val name: String,
    val subtitle: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val lastModifiedMillis: Long,
    val uri: Uri? = null,
    val localFile: File? = null,
    val mimeType: String? = null,
    val isHidden: Boolean = false,
    val isMountedRoot: Boolean = false
)

enum class ExplorerSort {
    NAME_ASC,
    SIZE_DESC,
    DATE_DESC
}

sealed class ShredExecutionState {
    data object Idle : ShredExecutionState()
    data class Running(
        val currentItemName: String,
        val currentItemIndex: Int,
        val totalItems: Int,
        val currentPass: Int,
        val totalPasses: Int,
        val passLabel: String,
        val bytesProcessedInRun: Long,
        val totalBytesPlanned: Long,
        val speedBytesPerSec: Long,
        val etaSeconds: Long
    ) : ShredExecutionState()

    data class Completed(
        val reportId: Long?,
        val reportCode: String,
        val methodName: String,
        val cycles: Int,
        val itemsShredded: Int,
        val itemsFailed: Int,
        val totalBytesShredded: Long,
        val durationMillis: Long,
        val verificationSha256: String,
        val wasCancelled: Boolean
    ) : ShredExecutionState()
}
