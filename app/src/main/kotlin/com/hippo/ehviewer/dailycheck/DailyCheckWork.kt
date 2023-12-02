package com.hippo.ehviewer.dailycheck

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.style.URLSpan
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.PendingIntentCompat
import androidx.core.text.getSpans
import androidx.core.text.parseAsHtml
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhCookieStore
import com.hippo.ehviewer.client.EhEngine
import eu.kanade.tachiyomi.util.lang.withIOContext
import java.time.Duration
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.days
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import splitties.init.appCtx

private const val CHANNEL_ID = "DailyCheckNotification"

class DailyCheckWork(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result = withIOContext {
        checkDawn().onFailure {
            return@withIOContext Result.retry()
        }
        Result.success()
    }
}

val schedHour
    get() = Settings.requestNewsTimerHour.takeUnless { it == -1 } ?: 0

val schedMinute
    get() = Settings.requestNewsTimerMinute.takeUnless { it == -1 } ?: 0

private fun getDailyCheckWorkRequest(): PeriodicWorkRequest {
    val now = Clock.System.now()
    val timeZone = TimeZone.currentSystemDefault()
    val whenToWork = LocalDateTime(
        now.toLocalDateTime(timeZone).date,
        LocalTime(schedHour, schedMinute),
    ).toInstant(timeZone)
    val initialDelay = (whenToWork - now).run { if (isNegative()) plus(1.days) else this }
    val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()
    return PeriodicWorkRequestBuilder<DailyCheckWork>(Duration.ofDays(1))
        .setConstraints(constraints)
        .setInitialDelay(initialDelay.inWholeSeconds, TimeUnit.SECONDS)
        .build()
}

private const val WORK_NAME = "DailyCheckWork"

fun updateDailyCheckWork(context: Context) {
    if (Settings.requestNews) {
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            getDailyCheckWorkRequest(),
        )
    } else {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}

suspend fun checkDawn() = runCatching {
    val now = Clock.System.now()
    val last = Instant.fromEpochSeconds(Settings.lastDawnTime)
    if (EhCookieStore.hasSignedIn() && now > last + 1.days) {
        EhEngine.getNews(true)?.let {
            Settings.lastDawnTime = now.epochSeconds
            showEventNotification(it)
        }
    }
}.onFailure {
    it.printStackTrace()
}

@SuppressLint("MissingPermission")
fun showEventNotification(html: String) {
    if (Settings.hideHvEvents && html.contains("You have encountered a monster!")) {
        return
    }
    val notificationManager = NotificationManagerCompat.from(appCtx)
    val chan = NotificationChannelCompat
        .Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_LOW)
        .setName(CHANNEL_ID)
        .build()
    notificationManager.createNotificationChannel(chan)
    val text = html.parseAsHtml()
    val msg = NotificationCompat.Builder(appCtx, CHANNEL_ID)
        .setAutoCancel(true)
        .setSmallIcon(R.drawable.ic_launcher_monochrome)
        .setContentText(text)
        .setStyle(NotificationCompat.BigTextStyle())
    val urls = text.getSpans<URLSpan>(0, text.length)
    if (urls.isNotEmpty()) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(urls.first().url))
        val pi = PendingIntentCompat.getActivity(appCtx, 0, intent, 0, false)
        msg.setContentIntent(pi)
    }
    runCatching {
        notificationManager.notify(1, msg.build())
    }.onFailure {
        it.printStackTrace()
    }
}
