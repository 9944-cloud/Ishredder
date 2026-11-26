package com.example.engine

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import com.example.data.ShredderRepository
import com.example.model.ErasureCatalog
import com.example.model.SelectiveCategory
import com.example.model.ShredTargetItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

object AutoShredScheduler {
    private const val JOB_ID_AUTO_SHRED = 202601

    fun syncSchedule(
        context: Context,
        enabled: Boolean,
        intervalHours: Int
    ): Boolean {
        val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler
            ?: return false
        scheduler.cancel(JOB_ID_AUTO_SHRED)
        if (!enabled) return true

        val intervalMs = intervalHours.coerceAtLeast(1).toLong() * 60L * 60L * 1000L
        val component = ComponentName(context, AutoShredJobService::class.java)
        val builder = JobInfo.Builder(JOB_ID_AUTO_SHRED, component)
            .setPeriodic(intervalMs)
            .setRequiresBatteryNotLow(true)
        return try {
            scheduler.schedule(builder.build()) == JobScheduler.RESULT_SUCCESS
        } catch (_: Exception) {
            false
        }
    }
}

class AutoShredJobService : JobService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStartJob(params: JobParameters?): Boolean {
        serviceScope.launch {
            try {
                val repo = ShredderRepository(applicationContext)
                val settings = repo.settingsFlow.first()
                if (settings.autoShredEnabled) {
                    val overview = repo.inspectStorage()
                    val targets = mutableListOf(
                        ShredTargetItem(
                            id = "guard:app_cache",
                            title = "Auto-Guard App Cache",
                            subtitle = "${overview.cacheBytes} B cached",
                            sizeBytes = overview.cacheBytes,
                            category = SelectiveCategory.APP_CACHE
                        ),
                        ShredTargetItem(
                            id = "guard:residual",
                            title = "Auto-Guard Residual Files",
                            subtitle = "${overview.residualBytes} B sandbox",
                            sizeBytes = overview.residualBytes,
                            category = SelectiveCategory.RESIDUAL_FILES
                        )
                    )
                    if (settings.autoShredIncludeClipboard) {
                        targets.add(
                            ShredTargetItem(
                                id = "guard:clipboard",
                                title = "Auto-Guard Clipboard",
                                subtitle = "Scheduled RAM buffer purge",
                                sizeBytes = 4096L,
                                category = SelectiveCategory.CLIPBOARD,
                                isClipboardWipe = true
                            )
                        )
                    }
                    val method = ErasureCatalog.findById(settings.autoShredMethodId)
                    repo.executeShredAndSaveReport(
                        targets = targets,
                        method = method,
                        freeSpaceOverwriteMb = settings.freeSpaceOverwriteMb,
                        saveReport = settings.enableErasureReport,
                        onProgress = {}
                    )
                    repo.settingsStore.recordAutoShredRun(System.currentTimeMillis())
                }
            } finally {
                jobFinished(params, false)
            }
        }
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        serviceScope.cancel()
        return true
    }
}
