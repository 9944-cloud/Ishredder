package com.example.engine

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.ContactsContract
import android.provider.DocumentsContract
import com.example.model.ErasureMethod
import com.example.model.PassPattern
import com.example.model.SelectiveCategory
import com.example.model.SelectiveSubCategory
import com.example.model.ShredExecutionState
import com.example.model.ShredTargetItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.security.MessageDigest
import java.security.SecureRandom
import kotlin.math.max
import kotlin.math.min

data class ShredRunResult(
    val itemsShredded: Int,
    val itemsFailed: Int,
    val totalBytesOverwritten: Long,
    val durationMillis: Long,
    val detailLog: String,
    val verificationSha256: String,
    val wasCancelled: Boolean
)

class SecureShredderEngine(
    private val context: Context,
    private val inspector: StorageInspector
) {
    private val secureRandom = SecureRandom()
    private val chunkSize = 64 * 1024 // 64 KB aligned I/O buffer

    suspend fun executeShred(
        targets: List<ShredTargetItem>,
        method: ErasureMethod,
        freeSpaceOverwriteMb: Long,
        onProgress: (ShredExecutionState.Running) -> Unit
    ): ShredRunResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val logBuilder = StringBuilder()
        var itemsShredded = 0
        var itemsFailed = 0
        var totalBytesProcessed = 0L
        var cancelled = false

        val plannedPerPassBytes = targets.sumOf { item ->
            when {
                item.isFreeSpaceWipe -> resolveFreeSpaceTargetBytes(freeSpaceOverwriteMb)
                item.isClipboardWipe -> 4096L
                else -> max(item.sizeBytes, 4096L)
            }
        }
        val totalPlannedAllPasses = max(plannedPerPassBytes * method.cycles, 1L)

        fun emitProgress(
            itemName: String,
            itemIndex: Int,
            passIndex: Int,
            passLabel: String
        ) {
            val elapsedMs = max(System.currentTimeMillis() - startTime, 1L)
            val speedBps = (totalBytesProcessed * 1000L) / elapsedMs
            val remainingBytes = (totalPlannedAllPasses - totalBytesProcessed).coerceAtLeast(0L)
            val eta = if (speedBps > 0L) remainingBytes / speedBps else 0L
            onProgress(
                ShredExecutionState.Running(
                    currentItemName = itemName,
                    currentItemIndex = itemIndex,
                    totalItems = targets.size,
                    currentPass = passIndex,
                    totalPasses = method.cycles,
                    passLabel = passLabel,
                    bytesProcessedInRun = totalBytesProcessed,
                    totalBytesPlanned = totalPlannedAllPasses,
                    speedBytesPerSec = speedBps,
                    etaSeconds = eta
                )
            )
        }

        try {
            targets.forEachIndexed { idx, target ->
                currentCoroutineContext().ensureActive()
                when {
                    target.isFreeSpaceWipe -> {
                        val wipeBytes = resolveFreeSpaceTargetBytes(freeSpaceOverwriteMb)
                        val ok = shredFreeSpace(
                            bytesToWipe = wipeBytes,
                            method = method,
                            onPassChunk = { passNum, passLabel, delta ->
                                totalBytesProcessed += delta
                                emitProgress(target.title, idx + 1, passNum, passLabel)
                            }
                        )
                        if (ok) {
                            itemsShredded++
                            logBuilder.appendLine("[OK] Free Space Wiped: ${wipeBytes / (1024 * 1024)} MB x ${method.cycles} passes")
                        } else {
                            itemsFailed++
                            logBuilder.appendLine("[WARN] Free Space Wipe partial/failed")
                        }
                    }

                    target.isClipboardWipe -> {
                        emitProgress(target.title, idx + 1, 1, "CSPRNG Clipboard Overwrite")
                        shredClipboard(method.cycles.coerceAtMost(7))
                        totalBytesProcessed += 4096L * method.cycles
                        itemsShredded++
                        logBuilder.appendLine("[OK] Clipboard Purged (${method.cycles.coerceAtMost(7)} overwrites)")
                    }

                    target.category == SelectiveCategory.APP_CACHE && target.localFile == null && target.uri == null -> {
                        val files = inspector.getCacheDirectories().flatMap { collectAllFiles(it) }
                        if (files.isEmpty()) {
                            itemsShredded++
                            logBuilder.appendLine("[OK] App Cache already clean")
                        } else {
                            var subSuccess = 0
                            files.forEach { file ->
                                currentCoroutineContext().ensureActive()
                                val ok = shredLocalFile(file, method) { passNum, passLabel, delta ->
                                    totalBytesProcessed += delta
                                    emitProgress("Cache: ${file.name}", idx + 1, passNum, passLabel)
                                }
                                if (ok) subSuccess++
                            }
                            inspector.getCacheDirectories().forEach { cleanEmptyDirs(it, preserveRoot = true) }
                            itemsShredded++
                            logBuilder.appendLine("[OK] App Cache Shredded: $subSuccess/${files.size} entries")
                        }
                    }

                    target.category == SelectiveCategory.RESIDUAL_FILES && target.localFile == null && target.uri == null -> {
                        val files = inspector.getResidualFiles()
                        if (files.isEmpty()) {
                            itemsShredded++
                            logBuilder.appendLine("[OK] Residual Files already clean")
                        } else {
                            var subSuccess = 0
                            files.forEach { file ->
                                currentCoroutineContext().ensureActive()
                                val ok = shredLocalFile(file, method) { passNum, passLabel, delta ->
                                    totalBytesProcessed += delta
                                    emitProgress(file.name, idx + 1, passNum, passLabel)
                                }
                                if (ok) subSuccess++
                            }
                            itemsShredded++
                            logBuilder.appendLine("[OK] Residual Files Shredded: $subSuccess/${files.size} files")
                        }
                    }

                    target.localFile != null -> {
                        val f = target.localFile
                        if (f.isDirectory) {
                            val children = collectAllFiles(f)
                            var subSuccess = 0
                            children.forEach { child ->
                                currentCoroutineContext().ensureActive()
                                val ok = shredLocalFile(child, method) { passNum, passLabel, delta ->
                                    totalBytesProcessed += delta
                                    emitProgress(child.name, idx + 1, passNum, passLabel)
                                }
                                if (ok) subSuccess++
                            }
                            cleanEmptyDirs(f, preserveRoot = false)
                            itemsShredded++
                            logBuilder.appendLine("[OK] Directory '${target.title}': $subSuccess/${children.size} files shredded")
                        } else {
                            val ok = shredLocalFile(f, method) { passNum, passLabel, delta ->
                                totalBytesProcessed += delta
                                emitProgress(target.title, idx + 1, passNum, passLabel)
                            }
                            if (ok) {
                                itemsShredded++
                                logBuilder.appendLine("[OK] File '${target.title}' (${target.sizeBytes} B) shredded")
                            } else {
                                itemsFailed++
                                logBuilder.appendLine("[FAIL] Could not shred '${target.title}'")
                            }
                        }
                    }

                    target.contactRawId != null -> {
                        emitProgress(target.title, idx + 1, 1, "Overwriting contact record")
                        val ok = shredContact(target.contactRawId)
                        totalBytesProcessed += 512L * method.cycles
                        if (ok) {
                            itemsShredded++
                            logBuilder.appendLine("[OK] Contact '${target.title}' overwritten & deleted")
                        } else {
                            itemsFailed++
                            logBuilder.appendLine("[FAIL] Could not delete contact '${target.title}'")
                        }
                    }

                    target.uri != null -> {
                        val ok = when {
                            target.isDirectory && target.subCategory == SelectiveSubCategory.EMPTY_FOLDERS -> {
                                emitProgress(target.title, idx + 1, 1, "Removing empty folder")
                                totalBytesProcessed += 4096L
                                try {
                                    DocumentsContract.deleteDocument(context.contentResolver, target.uri)
                                } catch (_: Exception) {
                                    false
                                }
                            }
                            target.isDirectory -> {
                                shredSafTree(target.uri, method) { name, passNum, passLabel, delta ->
                                    totalBytesProcessed += delta
                                    emitProgress(name, idx + 1, passNum, passLabel)
                                }
                            }
                            else -> {
                                shredSafUri(target.uri, max(target.sizeBytes, 4096L), method) { passNum, passLabel, delta ->
                                    totalBytesProcessed += delta
                                    emitProgress(target.title, idx + 1, passNum, passLabel)
                                }
                            }
                        }
                        if (ok) {
                            itemsShredded++
                            logBuilder.appendLine("[OK] Document '${target.title}' shredded & unlinked")
                        } else {
                            itemsFailed++
                            logBuilder.appendLine("[FAIL] Read-only or inaccessible URI '${target.title}'")
                        }
                    }
                }
            }
        } catch (_: CancellationException) {
            cancelled = true
            logBuilder.appendLine("[CANCELLED] Operation halted by user.")
        }

        val duration = max(System.currentTimeMillis() - startTime, 1L)
        val digestInput = "${method.id}|$startTime|$itemsShredded|$totalBytesProcessed|${logBuilder}"
        val sha256 = MessageDigest.getInstance("SHA-256")
            .digest(digestInput.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

        ShredRunResult(
            itemsShredded = itemsShredded,
            itemsFailed = itemsFailed,
            totalBytesOverwritten = totalBytesProcessed,
            durationMillis = duration,
            detailLog = logBuilder.toString().trim(),
            verificationSha256 = sha256,
            wasCancelled = cancelled
        )
    }

    private fun resolveFreeSpaceTargetBytes(configuredMb: Long): Long {
        val stat = StatFs(Environment.getDataDirectory().absolutePath)
        val safetyFloor = 128L * 1024L * 1024L
        val maxSafe = (stat.availableBytes - safetyFloor).coerceAtLeast(16L * 1024L * 1024L)
        return if (configuredMb <= 0L) {
            min(maxSafe, 1024L * 1024L * 1024L) // Cap auto full-safe run at 1 GB per pass to remain responsive
        } else {
            min(configuredMb * 1024L * 1024L, maxSafe)
        }
    }

    private suspend fun shredFreeSpace(
        bytesToWipe: Long,
        method: ErasureMethod,
        onPassChunk: (Int, String, Long) -> Unit
    ): Boolean {
        val baseDir = context.getExternalFilesDir(null) ?: context.filesDir
        val tempFile = File(baseDir, ".ishredder_freespace_${System.currentTimeMillis()}.tmp")
        val buffer = ByteArray(chunkSize)
        var lastBytePattern: Byte = 0x00

        return try {
            method.passes.forEachIndexed { passIdx, pattern ->
                currentCoroutineContext().ensureActive()
                lastBytePattern = prepareBuffer(buffer, pattern, lastBytePattern)
                RandomAccessFile(tempFile, "rwd").use { raf ->
                    raf.seek(0L)
                    var remaining = bytesToWipe
                    while (remaining > 0L) {
                        currentCoroutineContext().ensureActive()
                        if (pattern is PassPattern.Random) {
                            secureRandom.nextBytes(buffer)
                        }
                        val toWrite = min(remaining, buffer.size.toLong()).toInt()
                        raf.write(buffer, 0, toWrite)
                        remaining -= toWrite
                        onPassChunk(passIdx + 1, pattern.label, toWrite.toLong())
                    }
                    raf.fd.sync()
                }
            }
            true
        } catch (ce: CancellationException) {
            throw ce
        } catch (_: Exception) {
            false
        } finally {
            if (tempFile.exists()) {
                tempFile.delete()
            }
        }
    }

    private suspend fun shredLocalFile(
        file: File,
        method: ErasureMethod,
        onPassChunk: (Int, String, Long) -> Unit
    ): Boolean {
        if (!file.exists() || !file.canWrite()) return false
        val length = max(file.length(), 4096L)
        val buffer = ByteArray(chunkSize)
        var lastBytePattern: Byte = 0x00

        return try {
            method.passes.forEachIndexed { passIdx, pattern ->
                currentCoroutineContext().ensureActive()
                lastBytePattern = prepareBuffer(buffer, pattern, lastBytePattern)
                RandomAccessFile(file, "rwd").use { raf ->
                    raf.seek(0L)
                    var remaining = length
                    while (remaining > 0L) {
                        currentCoroutineContext().ensureActive()
                        if (pattern is PassPattern.Random) {
                            secureRandom.nextBytes(buffer)
                        }
                        val toWrite = min(remaining, buffer.size.toLong()).toInt()
                        raf.write(buffer, 0, toWrite)
                        remaining -= toWrite
                        onPassChunk(passIdx + 1, pattern.label, toWrite.toLong())
                    }
                    raf.fd.sync()
                }
            }

            // Truncate to 0 and obfuscate filename before unlinking
            RandomAccessFile(file, "rwd").use { raf ->
                raf.setLength(0L)
                raf.fd.sync()
            }
            val parent = file.parentFile
            var currentFile = file
            if (parent != null) {
                val obfuscated = File(parent, "00000000_${System.nanoTime()}.tmp")
                if (currentFile.renameTo(obfuscated)) {
                    currentFile = obfuscated
                }
            }
            currentFile.delete()
        } catch (ce: CancellationException) {
            throw ce
        } catch (_: Exception) {
            false
        }
    }

    private suspend fun shredSafUri(
        uri: Uri,
        knownSize: Long,
        method: ErasureMethod,
        onPassChunk: (Int, String, Long) -> Unit
    ): Boolean {
        val targetBytes = max(knownSize, 4096L)
        val buffer = ByteArray(chunkSize)
        var lastBytePattern: Byte = 0x00

        return try {
            val canOpenRw = try {
                context.contentResolver.openFileDescriptor(uri, "rw")?.use { pfd ->
                    val fos = FileOutputStream(pfd.fileDescriptor)
                    val channel = fos.channel
                    method.passes.forEachIndexed { passIdx, pattern ->
                        currentCoroutineContext().ensureActive()
                        lastBytePattern = prepareBuffer(buffer, pattern, lastBytePattern)
                        channel.position(0L)
                        var remaining = targetBytes
                        while (remaining > 0L) {
                            currentCoroutineContext().ensureActive()
                            if (pattern is PassPattern.Random) {
                                secureRandom.nextBytes(buffer)
                            }
                            val toWrite = min(remaining, buffer.size.toLong()).toInt()
                            fos.write(buffer, 0, toWrite)
                            remaining -= toWrite
                            onPassChunk(passIdx + 1, pattern.label, toWrite.toLong())
                        }
                        fos.flush()
                        channel.force(true)
                    }
                    channel.truncate(0L)
                    true
                } ?: false
            } catch (_: Exception) {
                false
            }

            if (!canOpenRw) {
                // Fallback "wt" mode for providers that disallow "rw"
                method.passes.forEachIndexed { passIdx, pattern ->
                    currentCoroutineContext().ensureActive()
                    lastBytePattern = prepareBuffer(buffer, pattern, lastBytePattern)
                    context.contentResolver.openOutputStream(uri, "wt")?.use { out ->
                        var remaining = targetBytes
                        while (remaining > 0L) {
                            currentCoroutineContext().ensureActive()
                            if (pattern is PassPattern.Random) {
                                secureRandom.nextBytes(buffer)
                            }
                            val toWrite = min(remaining, buffer.size.toLong()).toInt()
                            out.write(buffer, 0, toWrite)
                            remaining -= toWrite
                            onPassChunk(passIdx + 1, pattern.label, toWrite.toLong())
                        }
                        out.flush()
                    } ?: return false
                }
            }

            try {
                DocumentsContract.deleteDocument(context.contentResolver, uri)
            } catch (_: Exception) {
                // Some providers delete via contentResolver.delete
                context.contentResolver.delete(uri, null, null) > 0
            }
        } catch (ce: CancellationException) {
            throw ce
        } catch (_: Exception) {
            false
        }
    }

    private suspend fun shredSafTree(
        treeUri: Uri,
        method: ErasureMethod,
        onItemChunk: (String, Int, String, Long) -> Unit
    ): Boolean {
        val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
        return shredSafDirectoryRecursive(treeUri, rootDocId, method, deleteSelf = false, onItemChunk)
    }

    private suspend fun shredSafDirectoryRecursive(
        treeUri: Uri,
        docId: String,
        method: ErasureMethod,
        deleteSelf: Boolean,
        onItemChunk: (String, Int, String, Long) -> Unit
    ): Boolean {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE
        )
        val children = mutableListOf<Triple<String, String, Pair<String, Long>>>()
        context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
            val idIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
            val sizeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
            while (cursor.moveToNext()) {
                val cId = cursor.getString(idIdx) ?: continue
                val name = cursor.getString(nameIdx) ?: "Item"
                val mime = cursor.getString(mimeIdx) ?: ""
                val size = if (!cursor.isNull(sizeIdx)) cursor.getLong(sizeIdx) else 4096L
                children.add(Triple(cId, name, mime to size))
            }
        }

        var allOk = true
        for ((cId, name, meta) in children) {
            currentCoroutineContext().ensureActive()
            val (mime, size) = meta
            if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                val ok = shredSafDirectoryRecursive(treeUri, cId, method, deleteSelf = true, onItemChunk)
                if (!ok) allOk = false
            } else {
                val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, cId)
                val ok = shredSafUri(docUri, size, method) { passNum, passLabel, delta ->
                    onItemChunk(name, passNum, passLabel, delta)
                }
                if (!ok) allOk = false
            }
        }

        if (deleteSelf) {
            try {
                val dirUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                DocumentsContract.deleteDocument(context.contentResolver, dirUri)
            } catch (_: Exception) {
            }
        }
        return allOk
    }

    private fun shredContact(contactId: Long): Boolean {
        return try {
            val values = ContentValues().apply {
                put(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, "00000000")
            }
            context.contentResolver.update(
                ContactsContract.Data.CONTENT_URI,
                values,
                "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                arrayOf(contactId.toString(), ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
            )
            val deleted = context.contentResolver.delete(
                ContactsContract.RawContacts.CONTENT_URI,
                "${ContactsContract.RawContacts.CONTACT_ID} = ?",
                arrayOf(contactId.toString())
            )
            deleted > 0
        } catch (_: Exception) {
            false
        }
    }

    private fun shredClipboard(passes: Int) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        val randomBytes = ByteArray(64)
        repeat(passes) {
            secureRandom.nextBytes(randomBytes)
            val hex = randomBytes.joinToString("") { "%02x".format(it) }
            cm.setPrimaryClip(ClipData.newPlainText("shred_pass", hex))
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            cm.clearPrimaryClip()
        } else {
            cm.setPrimaryClip(ClipData.newPlainText("", ""))
        }
    }

    private fun prepareBuffer(buffer: ByteArray, pattern: PassPattern, lastByte: Byte): Byte {
        return when (pattern) {
            is PassPattern.Zero -> {
                buffer.fill(0x00)
                0x00
            }
            is PassPattern.One -> {
                buffer.fill(0xFF.toByte())
                0xFF.toByte()
            }
            is PassPattern.FixedByte -> {
                buffer.fill(pattern.value)
                pattern.value
            }
            is PassPattern.ThreeBytePattern -> {
                for (i in buffer.indices) {
                    buffer[i] = when (i % 3) {
                        0 -> pattern.b1
                        1 -> pattern.b2
                        else -> pattern.b3
                    }
                }
                pattern.b1
            }
            is PassPattern.ComplementLast -> {
                val comp = (lastByte.toInt() xor 0xFF).toByte()
                buffer.fill(comp)
                comp
            }
            is PassPattern.Random, is PassPattern.VerifyLast -> {
                secureRandom.nextBytes(buffer)
                buffer[0]
            }
        }
    }

    private fun collectAllFiles(dir: File): List<File> {
        val list = mutableListOf<File>()
        dir.listFiles()?.forEach { child ->
            if (child.isDirectory) list.addAll(collectAllFiles(child))
            else list.add(child)
        }
        return list
    }

    private fun cleanEmptyDirs(dir: File, preserveRoot: Boolean) {
        dir.listFiles()?.forEach { child ->
            if (child.isDirectory) cleanEmptyDirs(child, preserveRoot = false)
        }
        if (!preserveRoot && dir.listFiles()?.isEmpty() == true) {
            dir.delete()
        }
    }
}
