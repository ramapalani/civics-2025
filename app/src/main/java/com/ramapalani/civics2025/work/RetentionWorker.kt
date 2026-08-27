package com.ramapalani.civics2025.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ramapalani.civics2025.CivicsApplication
import kotlinx.coroutines.flow.first

class RetentionWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as CivicsApplication
        val prefs = app.preferences.prefs.first()
        app.results.enforceRetention(prefs.retentionDays, prefs.maxAttempts)
        return Result.success()
    }
}
