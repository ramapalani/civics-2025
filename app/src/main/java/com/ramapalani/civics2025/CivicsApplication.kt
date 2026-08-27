package com.ramapalani.civics2025

import android.app.Application
import com.ramapalani.civics2025.data.ContentRepository
import com.ramapalani.civics2025.data.ResultsRepository
import com.ramapalani.civics2025.data.UserPreferencesRepository
import com.ramapalani.civics2025.data.db.CivicsDatabase
import com.ramapalani.civics2025.work.DailyQuestionWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class CivicsApplication : Application() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    lateinit var content: ContentRepository
        private set
    lateinit var results: ResultsRepository
        private set
    lateinit var preferences: UserPreferencesRepository
        private set

    override fun onCreate() {
        super.onCreate()
        content = ContentRepository(this)
        val db = CivicsDatabase.create(this)
        results = ResultsRepository(db.dao())
        preferences = UserPreferencesRepository(this)
        scope.launch {
            val prefs = preferences.prefs.first()
            DailyQuestionWorker.schedule(this@CivicsApplication, prefs.dailyHour, prefs.notificationsOn)
            results.enforceRetention(prefs.retentionDays, prefs.maxAttempts)
        }
    }
}
