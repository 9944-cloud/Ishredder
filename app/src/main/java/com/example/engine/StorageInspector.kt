package com.example.engine

import android.Manifest
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager
import android.provider.ContactsContract
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import androidx.core.content.ContextCompat
import com.example.data.MountedFolderEntity
import com.example.model.ExplorerEntry
import com.example.model.SelectiveCategory
import com.example.model.SelectiveSubCategory
import com.example.model.ShredTargetItem
import com.example.model.StorageOverview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

class StorageInspector(private val context: Context) {

    suspend fun inspectStorageOverview(): StorageOverview = withContext(Dispatchers.IO) {
        val statFs = StatFs(Environment.getDataDirectory().absolutePath)
        val total = statFs.totalBytes
        val available = statFs.availableBytes
        val used = (total - available).coerceAtLeast(0L)

        val cacheBytes = getCacheDirectories().sumOf { dirSize(it) }
        val residualBytes = getResidualFiles().sumOf { it.length() }

        val clipboardPreview = try {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = cm?.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val text = clip.getItemAt(0).coerceToText(context)?.toString()?.trim()
                if (!text.isNullOrEmpty()) {
                    if (text.length > 42) text.take(42) + "..." else text
                } else null
            } else null
        } catch (_: Exception) {
            null
        }

        val removableVolumes = try {
            val sm = context.getSystemService(Context.STORAGE_SERVICE) as? StorageManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && sm != null) {
                sm.storageVolumes
                    .filter { it.isRemovable && it.state == Environment.MEDIA_MOUNTED }
                    .mapNotNull { it.getDescription(context)?.takeIf { d -> d.isNotBlank() } }
            } else {
                emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }

        StorageOverview(
            totalBytes = total,
            availableBytes = available,
            usedBytes = used,
            cacheBytes = cacheBytes,
            residualBytes = residualBytes,
            clipboardPreview = clipboardPreview,
            connectedRemovableVolumes = removableVolumes
        )
    }

    fun getCacheDirectories(): List<File> {
        return listOfNotNull(
            context.cacheDir,
            context.codeCacheDir,
            context.externalCacheDir
        ).distinctBy { it.absolutePath }.filter { it.exists() }
    }

    fun getResidualFiles(): List<File> {
        val protectedNames = setOf("datastore", "ishredder_excellent.db", "ishredder_excellent.db-wal", "ishredder_excellent.db-shm")
        val dirs = listOfNotNull(context.filesDir, context.getExternalFilesDir(null))
        val results = mutableListOf<File>()
        dirs.forEach { root ->
            root.listFiles()?.forEach { child ->
                if (!protectedNames.contains(child.name) && !child.name.startsWith(".ishredder_freespace")) {
                    if (child.isFile) {
                        results.add(child)
                    } else if (child.isDirectory && child.name != "datastore") {
                        collectFilesRecursive(child, results)
                    }
                }
            }
        }
        return results
    }

    private fun collectFilesRecursive(dir: File, out: MutableList<File>) {
        dir.listFiles()?.forEach { file ->
            if (file.isDirectory) collectFilesRecursive(file, out) else out.add(file)
        }
    }

    suspend fun listCacheExplorerEntries(showHidden: Boolean): List<ExplorerEntry> = withContext(Dispatchers.IO) {
        val entries = mutableListOf<ExplorerEntry>()
        getCacheDirectories().forEach { cacheRoot ->
            cacheRoot.listFiles()?.forEach { child ->
                val hidden = child.name.startsWith(".")
                if (showHidden || !hidden) {
                    entries.add(
                        ExplorerEntry(
                            id = "file:${child.absolutePath}",
                            name = child.name,
                            subtitle = if (child.isDirectory) "Cache folder" else "Cache file",
                            isDirectory = child.isDirectory,
                            sizeBytes = if (child.isDirectory) dirSize(child) else child.length(),
                            lastModifiedMillis = child.lastModified(),
                            localFile = child,
                            mimeType = guessMime(child.name),
                            isHidden = hidden
                        )
                    )
                }
            }
        }
        entries
    }

    suspend fun listResidualExplorerEntries(showHidden: Boolean): List<ExplorerEntry> = withContext(Dispatchers.IO) {
        getResidualFiles().mapNotNull { file ->
            val hidden = file.name.startsWith(".")
            if (!showHidden && hidden) return@mapNotNull null
            ExplorerEntry(
                id = "file:${file.absolutePath}",
                name = file.name,
                subtitle = file.parentFile?.name ?: "App Files",
                isDirectory = false,
                sizeBytes = file.length(),
                lastModifiedMillis = file.lastModified(),
                localFile = file,
                mimeType = guessMime(file.name),
                isHidden = hidden
            )
        }
    }

    suspend fun listLocalDirectoryEntries(dir: File, showHidden: Boolean): List<ExplorerEntry> = withContext(Dispatchers.IO) {
        dir.listFiles()?.mapNotNull { file ->
            val hidden = file.name.startsWith(".")
            if (!showHidden && hidden) return@mapNotNull null
            ExplorerEntry(
                id = "file:${file.absolutePath}",
                name = file.name,
                subtitle = if (file.isDirectory) "${file.listFiles()?.size ?: 0} items" else "Local file",
                isDirectory = file.isDirectory,
                sizeBytes = if (file.isDirectory) dirSize(file) else file.length(),
                lastModifiedMillis = file.lastModified(),
                localFile = file,
                mimeType = if (file.isDirectory) null else guessMime(file.name),
                isHidden = hidden
            )
        } ?: emptyList()
    }

    suspend fun listSafChildren(
        treeUri: Uri,
        parentDocId: String?,
        showHidden: Boolean
    ): List<ExplorerEntry> = withContext(Dispatchers.IO) {
        val docId = parentDocId ?: DocumentsContract.getTreeDocumentId(treeUri)
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED
        )
        val results = mutableListOf<ExplorerEntry>()
        try {
            context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                val idIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val sizeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
                val modIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)

                while (cursor.moveToNext()) {
                    val childDocId = cursor.getString(idIdx) ?: continue
                    val name = cursor.getString(nameIdx) ?: "Unnamed"
                    val hidden = name.startsWith(".")
                    if (!showHidden && hidden) continue
                    val mime = cursor.getString(mimeIdx) ?: ""
                    val isDir = mime == DocumentsContract.Document.MIME_TYPE_DIR
                    val size = if (!cursor.isNull(sizeIdx)) cursor.getLong(sizeIdx) else 0L
                    val modified = if (!cursor.isNull(modIdx)) cursor.getLong(modIdx) else 0L
                    val childUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, childDocId)
                    results.add(
                        ExplorerEntry(
                            id = "saf:$childUri",
                            name = name,
                            subtitle = if (isDir) "Folder" else (mime.ifEmpty { "Document" }),
                            isDirectory = isDir,
                            sizeBytes = size,
                            lastModifiedMillis = modified,
                            uri = childUri,
                            mimeType = if (isDir) null else mime,
                            isHidden = hidden
                        )
                    )
                }
            }
        } catch (_: Exception) {
            // Permission revoked or folder removed
        }
        results
    }

    suspend fun buildTargetFromUri(uri: Uri, category: SelectiveCategory): ShredTargetItem? =
        withContext(Dispatchers.IO) {
            try {
                var displayName = uri.lastPathSegment ?: "Selected Item"
                var size = 0L
                val mime = context.contentResolver.getType(uri)
                context.contentResolver.query(
                    uri,
                    arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameCol = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        val sizeCol = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (nameCol >= 0 && !cursor.isNull(nameCol)) {
                            displayName = cursor.getString(nameCol)
                        }
                        if (sizeCol >= 0 && !cursor.isNull(sizeCol)) {
                            size = cursor.getLong(sizeCol)
                        }
                    }
                }
                if (size <= 0L) {
                    context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { afd ->
                        if (afd.length > 0L) size = afd.length
                    }
                }
                ShredTargetItem(
                    id = "uri:$uri",
                    title = displayName,
                    subtitle = mime ?: category.title,
                    sizeBytes = size,
                    category = category,
                    uri = uri,
                    mimeType = mime
                )
            } catch (_: Exception) {
                null
            }
        }

    suspend fun buildTargetFromMountedFolder(folder: MountedFolderEntity): ShredTargetItem =
        withContext(Dispatchers.IO) {
            val treeUri = Uri.parse(folder.uriString)
            val total = computeSafTreeSize(treeUri, DocumentsContract.getTreeDocumentId(treeUri))
            ShredTargetItem(
                id = "tree:${folder.uriString}",
                title = folder.displayName,
                subtitle = "Mounted Folder (Recursive)",
                sizeBytes = total,
                category = SelectiveCategory.MOUNTED_FOLDERS,
                uri = treeUri,
                isDirectory = true
            )
        }

    private fun computeSafTreeSize(treeUri: Uri, docId: String): Long {
        var total = 0L
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE
        )
        try {
            context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                val idIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val mimeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val sizeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
                while (cursor.moveToNext()) {
                    val childId = cursor.getString(idIdx) ?: continue
                    val mime = cursor.getString(mimeIdx) ?: ""
                    if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                        total += computeSafTreeSize(treeUri, childId)
                    } else if (!cursor.isNull(sizeIdx)) {
                        total += cursor.getLong(sizeIdx)
                    }
                }
            }
        } catch (_: Exception) {
        }
        return total
    }

    suspend fun computeSha256(entry: ExplorerEntry): String = withContext(Dispatchers.IO) {
        if (entry.isDirectory) return@withContext "Directory (N/A)"
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(16 * 1024)
        try {
            val stream = when {
                entry.localFile != null -> entry.localFile.inputStream()
                entry.uri != null -> context.contentResolver.openInputStream(entry.uri)
                else -> null
            } ?: return@withContext "Unavailable"
            stream.use { input ->
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    digest.update(buffer, 0, read)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            "Unreadable"
        }
    }

    fun dirSize(dir: File): Long {
        if (!dir.exists()) return 0L
        if (dir.isFile) return dir.length()
        var total = 0L
        dir.listFiles()?.forEach { child ->
            total += if (child.isDirectory) dirSize(child) else child.length()
        }
        return total
    }

    fun hasContactsPermission(): Boolean {
        val read = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        val write = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CONTACTS) == PackageManager.PERMISSION_GRANTED
        return read && write
    }

    suspend fun scanSelectiveSubBuckets(
        mountedFolders: List<MountedFolderEntity>,
        availableBytes: Long,
        showHidden: Boolean
    ): Map<SelectiveSubCategory, List<ShredTargetItem>> = withContext(Dispatchers.IO) {
        val buckets = SelectiveSubCategory.entries.associateWith { mutableListOf<ShredTargetItem>() }

        // 1. Free Space -> Internal Storage
        buckets[SelectiveSubCategory.INTERNAL_STORAGE]?.add(
            ShredTargetItem(
                id = "cat:free_space",
                title = "Internal Storage",
                subtitle = "Unallocated storage blocks",
                sizeBytes = availableBytes,
                category = SelectiveCategory.FREE_SPACE,
                subCategory = SelectiveSubCategory.INTERNAL_STORAGE,
                isFreeSpaceWipe = true
            )
        )

        // 2. Temporary Files (app caches + residual .tmp)
        getCacheDirectories().forEach { root ->
            collectTempFiles(root, buckets[SelectiveSubCategory.TEMPORARY_FILES]!!)
        }

        // 3. Clipboard
        buckets[SelectiveSubCategory.CLIPBOARD]?.add(
            ShredTargetItem(
                id = "cat:clipboard",
                title = "System Clipboard",
                subtitle = "Current clipboard buffer",
                sizeBytes = 0L,
                category = SelectiveCategory.CLIPBOARD,
                subCategory = SelectiveSubCategory.CLIPBOARD,
                isClipboardWipe = true
            )
        )

        // 4. Contacts (when runtime permission granted)
        if (hasContactsPermission()) {
            scanContacts(buckets[SelectiveSubCategory.CONTACTS]!!)
        }

        // 5. Mounted SAF Folders classified across real categories
        mountedFolders.forEach { folder ->
            val treeUri = Uri.parse(folder.uriString)
            val rootDocId = try {
                DocumentsContract.getTreeDocumentId(treeUri)
            } catch (_: Exception) {
                null
            }
            if (rootDocId != null) {
                scanSafFolderRecursive(
                    treeUri = treeUri,
                    docId = rootDocId,
                    pathHint = folder.displayName.lowercase(),
                    showHidden = showHidden,
                    buckets = buckets,
                    depth = 0
                )
            }
        }

        // 6. Local residual files in app storage
        getResidualFiles().forEach { file ->
            val sub = classifyNameAndMime(file.name, guessMime(file.name), file.parentFile?.name?.lowercase() ?: "")
            buckets[sub]?.add(
                ShredTargetItem(
                    id = "file:${file.absolutePath}",
                    title = file.name,
                    subtitle = file.parentFile?.name ?: "App Files",
                    sizeBytes = file.length(),
                    category = SelectiveCategory.RESIDUAL_FILES,
                    subCategory = sub,
                    localFile = file,
                    mimeType = guessMime(file.name)
                )
            )
        }

        buckets
    }

    private fun collectTempFiles(dir: File, out: MutableList<ShredTargetItem>) {
        dir.listFiles()?.forEach { child ->
            if (child.isDirectory) {
                if (child.listFiles()?.isEmpty() == true) {
                    // skip empty cache root subdirs here
                } else {
                    collectTempFiles(child, out)
                }
            } else {
                out.add(
                    ShredTargetItem(
                        id = "file:${child.absolutePath}",
                        title = child.name,
                        subtitle = "Temporary cache file",
                        sizeBytes = child.length(),
                        category = SelectiveCategory.APP_CACHE,
                        subCategory = SelectiveSubCategory.TEMPORARY_FILES,
                        localFile = child,
                        mimeType = guessMime(child.name)
                    )
                )
            }
        }
    }

    private fun scanContacts(out: MutableList<ShredTargetItem>) {
        try {
            context.contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                arrayOf(
                    ContactsContract.Contacts._ID,
                    ContactsContract.Contacts.DISPLAY_NAME_PRIMARY
                ),
                null,
                null,
                ContactsContract.Contacts.DISPLAY_NAME_PRIMARY + " ASC"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                val nameIdx = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
                while (cursor.moveToNext()) {
                    val cId = cursor.getLong(idIdx)
                    val name = cursor.getString(nameIdx)?.takeIf { it.isNotBlank() } ?: "Unnamed Contact"
                    out.add(
                        ShredTargetItem(
                            id = "contact:$cId",
                            title = name,
                            subtitle = "Device contact record",
                            sizeBytes = 512L,
                            category = SelectiveCategory.CONTACTS,
                            subCategory = SelectiveSubCategory.CONTACTS,
                            contactRawId = cId
                        )
                    )
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun scanSafFolderRecursive(
        treeUri: Uri,
        docId: String,
        pathHint: String,
        showHidden: Boolean,
        buckets: Map<SelectiveSubCategory, MutableList<ShredTargetItem>>,
        depth: Int
    ) {
        if (depth > 5) return
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE
        )
        var childCount = 0
        try {
            context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                val idIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val sizeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
                while (cursor.moveToNext()) {
                    childCount++
                    val childId = cursor.getString(idIdx) ?: continue
                    val name = cursor.getString(nameIdx) ?: "Unnamed"
                    val hidden = name.startsWith(".")
                    if (!showHidden && hidden && !name.lowercase().contains("trash") && !name.lowercase().contains("thumb")) continue
                    val mime = cursor.getString(mimeIdx) ?: ""
                    val size = if (!cursor.isNull(sizeIdx)) cursor.getLong(sizeIdx) else 0L
                    val childUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, childId)
                    val childPath = "$pathHint/${name.lowercase()}"

                    if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                        val beforeEmpty = buckets[SelectiveSubCategory.EMPTY_FOLDERS]?.size ?: 0
                        val subChildCount = countAndScanSafDir(treeUri, childId, childPath, showHidden, buckets, depth + 1)
                        if (subChildCount == 0 && (buckets[SelectiveSubCategory.EMPTY_FOLDERS]?.size ?: 0) == beforeEmpty) {
                            buckets[SelectiveSubCategory.EMPTY_FOLDERS]?.add(
                                ShredTargetItem(
                                    id = "saf_empty:$childUri",
                                    title = name,
                                    subtitle = pathHint,
                                    sizeBytes = 0L,
                                    category = SelectiveCategory.MOUNTED_FOLDERS,
                                    subCategory = SelectiveSubCategory.EMPTY_FOLDERS,
                                    uri = childUri,
                                    isDirectory = true
                                )
                            )
                        }
                    } else {
                        val sub = classifyNameAndMime(name, mime, pathHint)
                        val category = when (sub.parentGroup) {
                            com.example.model.SelectiveParentGroup.MEDIA -> SelectiveCategory.PHOTOS_VIDEOS
                            else -> SelectiveCategory.DOCUMENTS_FILES
                        }
                        buckets[sub]?.add(
                            ShredTargetItem(
                                id = "uri:$childUri",
                                title = name,
                                subtitle = pathHint,
                                sizeBytes = size,
                                category = category,
                                subCategory = sub,
                                uri = childUri,
                                mimeType = mime
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun countAndScanSafDir(
        treeUri: Uri,
        docId: String,
        pathHint: String,
        showHidden: Boolean,
        buckets: Map<SelectiveSubCategory, MutableList<ShredTargetItem>>,
        depth: Int
    ): Int {
        var count = 0
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE
        )
        try {
            context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                val idIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val sizeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
                while (cursor.moveToNext()) {
                    count++
                    val childId = cursor.getString(idIdx) ?: continue
                    val name = cursor.getString(nameIdx) ?: "Unnamed"
                    val mime = cursor.getString(mimeIdx) ?: ""
                    val size = if (!cursor.isNull(sizeIdx)) cursor.getLong(sizeIdx) else 0L
                    val childUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, childId)
                    if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                        if (depth < 5) {
                            val subCount = countAndScanSafDir(treeUri, childId, "$pathHint/${name.lowercase()}", showHidden, buckets, depth + 1)
                            if (subCount == 0) {
                                buckets[SelectiveSubCategory.EMPTY_FOLDERS]?.add(
                                    ShredTargetItem(
                                        id = "saf_empty:$childUri",
                                        title = name,
                                        subtitle = pathHint,
                                        sizeBytes = 0L,
                                        category = SelectiveCategory.MOUNTED_FOLDERS,
                                        subCategory = SelectiveSubCategory.EMPTY_FOLDERS,
                                        uri = childUri,
                                        isDirectory = true
                                    )
                                )
                            }
                        }
                    } else {
                        val sub = classifyNameAndMime(name, mime, pathHint)
                        val category = when (sub.parentGroup) {
                            com.example.model.SelectiveParentGroup.MEDIA -> SelectiveCategory.PHOTOS_VIDEOS
                            else -> SelectiveCategory.DOCUMENTS_FILES
                        }
                        buckets[sub]?.add(
                            ShredTargetItem(
                                id = "uri:$childUri",
                                title = name,
                                subtitle = pathHint,
                                sizeBytes = size,
                                category = category,
                                subCategory = sub,
                                uri = childUri,
                                mimeType = mime
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {
        }
        return count
    }

    private fun classifyNameAndMime(name: String, mime: String, pathHint: String): SelectiveSubCategory {
        val lower = name.lowercase()
        val lowerPath = pathHint.lowercase()
        return when {
            lowerPath.contains("whatsapp") -> SelectiveSubCategory.WHATSAPP
            lowerPath.contains("telegram") -> SelectiveSubCategory.TELEGRAM
            lowerPath.contains("trash") || lower.startsWith(".trashed") -> SelectiveSubCategory.TRASH_BIN
            lowerPath.contains("thumb") -> SelectiveSubCategory.THUMBNAILS
            lower.endsWith(".apk") || lower.endsWith(".aab") || lower.endsWith(".xapk") -> SelectiveSubCategory.APKS_AABS
            lower.endsWith(".tmp") || lower.endsWith(".temp") || lower.endsWith(".bak") -> SelectiveSubCategory.TEMPORARY_FILES
            mime.startsWith("image/") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
                lower.endsWith(".png") || lower.endsWith(".webp") || lower.endsWith(".heic") -> SelectiveSubCategory.PHOTOS
            mime.startsWith("video/") || lower.endsWith(".mp4") || lower.endsWith(".mkv") ||
                lower.endsWith(".mov") || lower.endsWith(".avi") -> SelectiveSubCategory.VIDEO
            mime.startsWith("audio/") || lower.endsWith(".mp3") || lower.endsWith(".m4a") ||
                lower.endsWith(".ogg") || lower.endsWith(".opus") || lower.endsWith(".wav") -> SelectiveSubCategory.SONGS_VOICE
            else -> SelectiveSubCategory.DOWNLOADS
        }
    }

    private fun guessMime(name: String): String {
        val ext = name.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "jpg", "jpeg", "png", "webp", "gif", "heic" -> "image/$ext"
            "mp4", "mkv", "mov", "avi" -> "video/$ext"
            "mp3", "wav", "ogg", "flac", "m4a" -> "audio/$ext"
            "pdf" -> "application/pdf"
            "apk", "aab" -> "application/vnd.android.package-archive"
            "txt", "log", "md", "csv", "json", "xml" -> "text/plain"
            "zip", "7z", "tar", "gz" -> "application/zip"
            else -> "application/octet-stream"
        }
    }
}
