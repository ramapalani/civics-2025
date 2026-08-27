package com.ramapalani.civics2025.work

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ramapalani.civics2025.CivicsApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val reschedule = action == Intent.ACTION_BOOT_COMPLETED ||
            action == AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
        if (!reschedule) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as CivicsApplication
                val prefs = app.preferences.prefs.first()
                DailyQuestionWorker.schedule(context, prefs.dailyHour, prefs.notificationsOn)
            } finally {
                pending.finish()
            }
        }
    }
}
