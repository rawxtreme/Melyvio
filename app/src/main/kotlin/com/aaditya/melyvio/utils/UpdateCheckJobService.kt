/**
 * Melyvio Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.aaditya.melyvio.utils

import android.Manifest
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.datastore.preferences.core.stringPreferencesKey
import com.aaditya.melyvio.BuildConfig
import com.aaditya.melyvio.R
import com.aaditya.melyvio.constants.CheckForUpdatesKey
import com.aaditya.melyvio.constants.UpdateNotificationsEnabledKey
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import kotlin.concurrent.thread

/**
 * Periodically (every ~6h, survives reboot) asks GitHub for the latest release tag of
 * rawxtreme/Melyvio and posts a notification when a newer version exists.
 */
class UpdateCheckJobService : JobService() {
    @Volatile private var worker: Thread? = null

    override fun onStartJob(params: JobParameters): Boolean {
        worker =
            thread(name = "melyvio-update-check") {
                var reschedule = false
                try {
                    runBlocking { reschedule = !check() }
                } catch (_: Exception) {
                    reschedule = true
                }
                jobFinished(params, reschedule)
            }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        worker?.interrupt()
        return true
    }

    /** @return true when finished (no retry needed) */
    private suspend fun check(): Boolean {
        val prefs = applicationContext.dataStore.data.first()
        if (!(prefs[CheckForUpdatesKey] ?: true)) return true
        if (!(prefs[UpdateNotificationsEnabledKey] ?: true)) return true

        val (release, hasUpdate) = Updater.checkForUpdate(forceRefresh = true).getOrNull() ?: return false
        if (release == null || !hasUpdate) return true
        if (prefs[LastNotifiedTagKey] == release.tagName) return true

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return true
        }

        UpdateNotifier.notifyUpdate(applicationContext, release)
        applicationContext.safeDataStoreEdit { it[LastNotifiedTagKey] = release.tagName }
        return true
    }

    companion object {
        private const val JOB_ID = 7301
        private const val INTERVAL_MS = 6 * 60 * 60 * 1000L
        val LastNotifiedTagKey = stringPreferencesKey("lastNotifiedUpdateTag")

        fun schedule(context: Context) {
            if (!BuildConfig.UPDATER_AVAILABLE) return
            val scheduler = context.getSystemService(JobScheduler::class.java) ?: return
            if (scheduler.allPendingJobs.any { it.id == JOB_ID }) return
            val job =
                JobInfo
                    .Builder(JOB_ID, ComponentName(context, UpdateCheckJobService::class.java))
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setPeriodic(INTERVAL_MS)
                    .setPersisted(true)
                    .build()
            scheduler.schedule(job)
        }
    }
}

object UpdateNotifier {
    private const val NOTIFICATION_ID = 1001

    fun notifyUpdate(
        context: Context,
        release: ReleaseInfo,
    ) {
        val url = Updater.getDownloadUrlForCurrentVariant(release) ?: Updater.RELEASES_PAGE_URL
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
        val pending =
            PendingIntent.getActivity(
                context,
                NOTIFICATION_ID,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        val notification =
            NotificationCompat
                .Builder(context, "updates")
                .setSmallIcon(R.drawable.ic_stat_melyvio)
                .setContentTitle(context.getString(R.string.update_available_title))
                .setContentText(release.versionName)
                .setContentIntent(pending)
                .setAutoCancel(true)
                .build()
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
        }
    }
}
