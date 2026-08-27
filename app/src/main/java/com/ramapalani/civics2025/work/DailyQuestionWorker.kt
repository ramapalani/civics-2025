package com.ramapalani.civics2025.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.ramapalani.civics2025.CivicsApplication
import com.ramapalani.civics2025.MainActivity
import com.ramapalani.civics2025.R
import com.ramapalani.civics2025.domain.QuestionSelector
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class DailyQuestionWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as CivicsApplication
        val prefs = app.preferences.prefs.first()
        if (!prefs.notificationsOn) return Result.success()

        val stats = app.results.statsMap()
        val question = QuestionSelector.pickDaily(
            questions = app.content.questions,
            stats = stats,
            nowMillis = System.currentTimeMillis(),
            lastDailyId = prefs.lastDailyId,
            starredOnly = prefs.starred6520,
        )
        notify(question.id, question.question)
        return Result.success()
    }

    private fun notify(questionId: Int, text: String) {
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                applicationContext.getString(R.string.daily_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = applicationContext.getString(R.string.daily_channel_desc) },
        )
        if (ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN_DAILY, true)
            putExtra(MainActivity.EXTRA_QUESTION_ID, questionId)
        }
        val pending = PendingIntent.getActivity(
            applicationContext,
            questionId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_civics)
            .setContentTitle("Today’s civics question")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val UNIQUE_WORK = "daily_civics_question"
        const val RETENTION_WORK = "civics_retention"
        private const val CHANNEL_ID = "daily_civics"
        private const val NOTIFICATION_ID = 2025

        fun schedule(context: Context, hour: Int, enabled: Boolean) {
            val wm = WorkManager.getInstance(context)
            wm.cancelUniqueWork(UNIQUE_WORK)
            DailyAlarmScheduler.schedule(context, hour, enabled)
            val cleanup = PeriodicWorkRequestBuilder<RetentionWorker>(7, TimeUnit.DAYS).build()
            wm.enqueueUniquePeriodicWork(RETENTION_WORK, ExistingPeriodicWorkPolicy.KEEP, cleanup)
        }
    }
}
