package com.ramapalani.civics2025.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.ramapalani.civics2025.CivicsApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DailyAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                WorkManager.getInstance(context).enqueue(
                    OneTimeWorkRequestBuilder<DailyQuestionWorker>().build(),
                )
                val app = context.applicationContext as CivicsApplication
                val prefs = app.preferences.prefs.first()
                DailyAlarmScheduler.schedule(context, prefs.dailyHour, prefs.notificationsOn)
            } finally {
                pending.finish()
            }
        }
    }
}
