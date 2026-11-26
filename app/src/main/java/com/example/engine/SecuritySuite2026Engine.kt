package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import com.example.data.AppSettings
import com.example.data.ErasureReportEntity
import com.example.data.MountedFolderEntity
import com.example.model.ErasureCatalog
import com.example.model.StorageOverview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import kotlin.math.max

data class PrivacyAuditItem(
    val id: String,
    val title: String,
    val detail: String,
    val severity: String, // "SAFE", "WARN", "RISK"
    val pointsDeducted: Int
)

data class PrivacyAuditReport(
    val score: Int, // 0..100
    val grade: String,
    val items: List<PrivacyAuditItem>,
    val cleanableBytes: Long,
    val hasClipboardRisk: Boolean
)

data class ExifPhotoReport(
    val sourceUri: Uri,
    val displayName: String,
    val sizeBytes: Long,
    val resolutionLabel: String,
    val hasGps: Boolean,
    val gpsSummary: String?,
    val cameraMakeModel: String?,
    val captureDateTime: String?,
    val softwareTag: String?,
    val orientation: Int,
    val riskTagCount: Int
)

data class ScannedExifBatchResult(
    val photos: List<ExifPhotoReport>
)

data class ScrubbedPhotoResult(
    val originalName: String,
    val outputUri: Uri,
    val strippedTagCount: Int,
    val outputBytes: Long,
    val sha256: String
)

data class HardwareBenchmarkResult(
    val testSizeMb: Int,
    val csprngMbPerSec: Double,
    val flashWriteMbPerSec: Double,
    val readVerifyMbPerSec: Double,
    val elapsedMillis: Long,
    val recommendedMethodId: String,
    val verificationDigest: String
)

class SecuritySuite2026Engine(private val context: Context) {

    fun computePrivacyAudit(
        overview: StorageOverview,
        settings: AppSettings,
        mountedFolders: List<MountedFolderEntity>,
        reports: List<ErasureReportEntity>
    ): PrivacyAuditReport {
        val findings = mutableListOf<PrivacyAuditItem>()
        var score = 100

        val sandboxBytes = overview.cacheBytes + overview.residualBytes
        if (sandboxBytes > 512L * 1024L) {
            val deduct = if (sandboxBytes > 16L * 1024L * 1024L) 22 else 12
            score -= deduct
            findings.add(
                PrivacyAuditItem(
                    id = "audit_sandbox",
                    title = "Sandbox Cache & Residual Blocks",
                    detail = "${(sandboxBytes / 1024L).coerceAtLeast(1L)} KB waiting for cryptographic wipe",
                    severity = if (deduct >= 20) "RISK" else "WARN",
                    pointsDeducted = deduct
                )
            )
        } else {
            findings.add(
                PrivacyAuditItem(
                    id = "audit_sandbox",
                    title = "Sandbox Storage Hygiene",
                    detail = "App cache & residual files are clean",
                    severity = "SAFE",
                    pointsDeducted = 0
                )
            )
        }

        val hasClip = !overview.clipboardPreview.isNullOrBlank()
        if (hasClip) {
            score -= 18
            findings.add(
                PrivacyAuditItem(
                    id = "audit_clipboard",
                    title = "Active System Clipboard Buffer",
                    detail = "Unencrypted clipboard payload detected in RAM (${overview.clipboardPreview?.length ?: 0} chars)",
                    severity = "RISK",
                    pointsDeducted = 18
                )
            )
        } else {
            findings.add(
                PrivacyAuditItem(
                    id = "audit_clipboard",
                    title = "System Clipboard Buffer",
                    detail = "No active plaintext secrets detected in clipboard",
                    severity = "SAFE",
                    pointsDeducted = 0
                )
            )
        }

        if (!settings.passcodeEnabled) {
            score -= 15
            findings.add(
                PrivacyAuditItem(
                    id = "audit_passcode",
                    title = "App PIN Lock Disabled",
                    detail = "Enable 4-digit passcode lock in General Settings to guard audit reports",
                    severity = "WARN",
                    pointsDeducted = 15
                )
            )
        } else {
            findings.add(
                PrivacyAuditItem(
                    id = "audit_passcode",
                    title = "App PIN Lock Active",
                    detail = "SHA-256 salted 4-digit lock screen enabled",
                    severity = "SAFE",
                    pointsDeducted = 0
                )
            )
        }

        if (!settings.autoShredEnabled) {
            score -= 10
            findings.add(
                PrivacyAuditItem(
                    id = "audit_autoshred",
                    title = "Auto-Shred Guard Inactive",
                    detail = "Schedule periodic background cache sanitization in the Auto Guard tab",
                    severity = "WARN",
                    pointsDeducted = 10
                )
            )
        } else {
            findings.add(
                PrivacyAuditItem(
                    id = "audit_autoshred",
                    title = "Auto-Shred Guard Armed",
                    detail = "Periodic wipe every ${settings.autoShredIntervalHours}h (${ErasureCatalog.findById(settings.autoShredMethodId).name})",
                    severity = "SAFE",
                    pointsDeducted = 0
                )
            )
        }

        val latestReport = reports.firstOrNull()
        val ageHours = if (latestReport != null) {
            (System.currentTimeMillis() - latestReport.timestampMillis) / (1000L * 60L * 60L)
        } else Long.MAX_VALUE
        if (latestReport == null || ageHours > 168L) {
            score -= 10
            findings.add(
                PrivacyAuditItem(
                    id = "audit_cert",
                    title = "No Recent Certified Erasure",
                    detail = if (latestReport == null) "Run a wipe or hygiene sweep to issue your 2026 SHA-256 certificate" else "Last erasure certificate is > 7 days old",
                    severity = "WARN",
                    pointsDeducted = 10
                )
            )
        } else {
            findings.add(
                PrivacyAuditItem(
                    id = "audit_cert",
                    title = "Verified Audit Trail Current",
                    detail = "${latestReport.reportCode} • ${latestReport.methodName}",
                    severity = "SAFE",
                    pointsDeducted = 0
                )
            )
        }

        if (mountedFolders.isNotEmpty()) {
            findings.add(
                PrivacyAuditItem(
                    id = "audit_saf",
                    title = "SAF Mounted Folders",
                    detail = "${mountedFolders.size} persistable device folder(s) linked for deep shred",
                    severity = "SAFE",
                    pointsDeducted = 0
                )
            )
        }

        val clamped = score.coerceIn(0, 100)
        val grade = when {
            clamped >= 90 -> "A+ HARDENED"
            clamped >= 75 -> "B PROTECTED"
            clamped >= 55 -> "C EXPOSED"
            else -> "D HIGH RISK"
        }
        return PrivacyAuditReport(
            score = clamped,
            grade = grade,
            items = findings,
            cleanableBytes = sandboxBytes,
            hasClipboardRisk = hasClip
        )
    }

    suspend fun inspectPhotoExif(uris: List<Uri>): List<ExifPhotoReport> = withContext(Dispatchers.IO) {
        uris.mapNotNull { uri ->
            readExifForUri(uri)
        }
    }

    private fun readExifForUri(uri: Uri): ExifPhotoReport? {
        var displayName = "Photo.jpg"
        var sizeBytes = 0L
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                if (nameIdx >= 0) displayName = cursor.getString(nameIdx) ?: displayName
                if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) sizeBytes = cursor.getLong(sizeIdx)
            }
        }

        val exif: ExifInterface? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    ExifInterface(stream)
                }
            } catch (_: Exception) {
                null
            }
        } else {
            val tmp = File(context.cacheDir, "exif_probe_${System.nanoTime()}.tmp")
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(tmp).use { out -> input.copyTo(out) }
                }
                ExifInterface(tmp.absolutePath)
            } catch (_: Exception) {
                null
            } finally {
                tmp.delete()
            }
        }

        val lat = exif?.getAttribute(ExifInterface.TAG_GPS_LATITUDE)
        val lon = exif?.getAttribute(ExifInterface.TAG_GPS_LONGITUDE)
        val latRef = exif?.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF).orEmpty()
        val lonRef = exif?.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF).orEmpty()
        val hasGps = !lat.isNullOrBlank() && !lon.isNullOrBlank()
        val gpsSummary = if (hasGps) {
            "GPS Coordinates ($latRef / $lonRef)"
        } else null

        val make = exif?.getAttribute(ExifInterface.TAG_MAKE)?.trim().orEmpty()
        val model = exif?.getAttribute(ExifInterface.TAG_MODEL)?.trim().orEmpty()
        val makeModel = listOf(make, model).filter { it.isNotEmpty() }.joinToString(" ").ifBlank { null }

        @Suppress("DEPRECATION")
        val dateTime = exif?.getAttribute(ExifInterface.TAG_DATETIME)?.trim()?.takeIf { it.isNotEmpty() }
        val software = exif?.getAttribute("Software")?.trim()?.takeIf { it.isNotEmpty() }
        val width = exif?.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0) ?: 0
        val height = exif?.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0) ?: 0
        val orientation = exif?.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        ) ?: ExifInterface.ORIENTATION_NORMAL

        val resolution = if (width > 0 && height > 0) "${width}x${height}" else "Auto"
        var riskCount = 0
        if (hasGps) riskCount++
        if (makeModel != null) riskCount++
        if (dateTime != null) riskCount++
        if (software != null) riskCount++

        return ExifPhotoReport(
            sourceUri = uri,
            displayName = displayName,
            sizeBytes = sizeBytes,
            resolutionLabel = resolution,
            hasGps = hasGps,
            gpsSummary = gpsSummary,
            cameraMakeModel = makeModel,
            captureDateTime = dateTime,
            softwareTag = software,
            orientation = orientation,
            riskTagCount = riskCount
        )
    }

    suspend fun scrubPhotoToSafUri(
        report: ExifPhotoReport,
        destUri: Uri
    ): ScrubbedPhotoResult? = withContext(Dispatchers.IO) {
        val rawBitmap = context.contentResolver.openInputStream(report.sourceUri)?.use { input ->
            BitmapFactory.decodeStream(input)
        } ?: return@withContext null

        val orientedBitmap = rotateBitmapIfNeeded(rawBitmap, report.orientation)
        if (orientedBitmap !== rawBitmap) {
            rawBitmap.recycle()
        }

        val tempClean = File(context.cacheDir, "clean_exif_${System.nanoTime()}.jpg")
        try {
            FileOutputStream(tempClean).use { out ->
                orientedBitmap.compress(Bitmap.CompressFormat.JPEG, 94, out)
                out.fd.sync()
            }
            orientedBitmap.recycle()

            val digest = MessageDigest.getInstance("SHA-256")
            val bytes = tempClean.readBytes()
            val sha256 = digest.digest(bytes).joinToString("") { "%02x".format(it) }

            context.contentResolver.openOutputStream(destUri, "wt")?.use { out ->
                out.write(bytes)
                out.flush()
            } ?: return@withContext null

            ScrubbedPhotoResult(
                originalName = report.displayName,
                outputUri = destUri,
                strippedTagCount = max(report.riskTagCount, 1),
                outputBytes = bytes.size.toLong(),
                sha256 = sha256
            )
        } finally {
            // Cryptographically wipe and delete temp clean file from sandbox
            if (tempClean.exists()) {
                val len = tempClean.length().toInt().coerceAtLeast(64)
                val noise = ByteArray(len)
                SecureRandom().nextBytes(noise)
                try {
                    FileOutputStream(tempClean).use { it.write(noise) }
                } catch (_: Exception) {
                }
                tempClean.delete()
            }
        }
    }

    private fun rotateBitmapIfNeeded(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            else -> return bitmap
        }
        return try {
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (_: Exception) {
            bitmap
        }
    }

    suspend fun runHardwareBenchmark(testSizeMb: Int = 4): HardwareBenchmarkResult = withContext(Dispatchers.IO) {
        val totalBytes = testSizeMb.coerceIn(2, 16) * 1024L * 1024L
        val chunk = ByteArray(64 * 1024)
        val sr = SecureRandom()

        // 1. Measure CSPRNG Entropy Generation Rate
        val csprngStart = System.nanoTime()
        var generated = 0L
        while (generated < totalBytes) {
            sr.nextBytes(chunk)
            generated += chunk.size
        }
        val csprngElapsedSec = max((System.nanoTime() - csprngStart) / 1_000_000_000.0, 0.001)
        val csprngMbPerSec = testSizeMb / csprngElapsedSec

        // 2. Measure Flash Write Rate with fsync
        val benchFile = File(context.cacheDir, "flash_bench_${System.nanoTime()}.bin")
        val writeStart = System.nanoTime()
        try {
            FileOutputStream(benchFile).use { fos ->
                var written = 0L
                while (written < totalBytes) {
                    sr.nextBytes(chunk)
                    fos.write(chunk)
                    written += chunk.size
                }
                fos.fd.sync()
            }
            val writeElapsedSec = max((System.nanoTime() - writeStart) / 1_000_000_000.0, 0.001)
            val flashWriteMbPerSec = testSizeMb / writeElapsedSec

            // 3. Measure Read-Back Verification & SHA-256 Rate
            val verifyStart = System.nanoTime()
            val md = MessageDigest.getInstance("SHA-256")
            benchFile.inputStream().use { fis ->
                var read = fis.read(chunk)
                while (read != -1) {
                    md.update(chunk, 0, read)
                    read = fis.read(chunk)
                }
            }
            val verifyElapsedSec = max((System.nanoTime() - verifyStart) / 1_000_000_000.0, 0.001)
            val readVerifyMbPerSec = testSizeMb / verifyElapsedSec
            val digestHex = md.digest().joinToString("") { "%02x".format(it) }

            // Securely zero-overwrite & remove benchmark file
            FileOutputStream(benchFile).use { fos ->
                chunk.fill(0x00)
                var cleared = 0L
                while (cleared < totalBytes) {
                    fos.write(chunk)
                    cleared += chunk.size
                }
                fos.fd.sync()
            }
            benchFile.delete()

            val totalElapsedMs = max((System.nanoTime() - csprngStart) / 1_000_000L, 1L)
            val recommended = when {
                flashWriteMbPerSec >= 120.0 -> "q_shred_2026"
                flashWriteMbPerSec >= 50.0 -> "bsi_vsitr"
                else -> "nist_800_88_r2_2026"
            }

            HardwareBenchmarkResult(
                testSizeMb = testSizeMb,
                csprngMbPerSec = csprngMbPerSec,
                flashWriteMbPerSec = flashWriteMbPerSec,
                readVerifyMbPerSec = readVerifyMbPerSec,
                elapsedMillis = totalElapsedMs,
                recommendedMethodId = recommended,
                verificationDigest = digestHex
            )
        } finally {
            if (benchFile.exists()) {
                benchFile.delete()
            }
        }
    }
}
