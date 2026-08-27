package com.ramapalani.civics2025.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ramapalani.civics2025.domain.LocalOfficials
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("civics_prefs")

data class UserPrefs(
    val dailyHour: Int = 8,
    val notificationsOn: Boolean = true,
    val starred6520: Boolean = false,
    val interviewSimulation: Boolean = false,
    val officials: LocalOfficials = LocalOfficials(),
    val streak: Int = 0,
    val lastDailyDay: String = "",
    val lastDailyId: Int? = null,
    val retentionDays: Int = 90,
    val maxAttempts: Int = 2000,
)

class UserPreferencesRepository(private val context: Context) {
    private val dailyHour = intPreferencesKey("daily_hour")
    private val notificationsOn = booleanPreferencesKey("notifications_on")
    private val starred = booleanPreferencesKey("starred_6520")
    private val interview = booleanPreferencesKey("interview")
    private val state = stringPreferencesKey("state")
    private val senator = stringPreferencesKey("senator")
    private val representative = stringPreferencesKey("representative")
    private val governor = stringPreferencesKey("governor")
    private val capital = stringPreferencesKey("capital")
    private val president = stringPreferencesKey("president")
    private val vicePresident = stringPreferencesKey("vice_president")
    private val speaker = stringPreferencesKey("speaker")
    private val chiefJustice = stringPreferencesKey("chief_justice")
    private val streak = intPreferencesKey("streak")
    private val lastDailyDay = stringPreferencesKey("last_daily_day")
    private val lastDailyId = intPreferencesKey("last_daily_id")
    private val retentionDays = intPreferencesKey("retention_days")
    private val maxAttempts = intPreferencesKey("max_attempts")

    val prefs: Flow<UserPrefs> = context.dataStore.data.map { it.toPrefs() }

    suspend fun update(transform: (UserPrefs) -> UserPrefs) {
        context.dataStore.edit { store ->
            val next = transform(store.toPrefs())
            store[dailyHour] = next.dailyHour
            store[notificationsOn] = next.notificationsOn
            store[starred] = next.starred6520
            store[interview] = next.interviewSimulation
            store[state] = next.officials.stateName
            store[senator] = next.officials.senator
            store[representative] = next.officials.representative
            store[governor] = next.officials.governor
            store[capital] = next.officials.stateCapital
            store[president] = next.officials.president
            store[vicePresident] = next.officials.vicePresident
            store[speaker] = next.officials.speaker
            store[chiefJustice] = next.officials.chiefJustice
            store[streak] = next.streak
            store[lastDailyDay] = next.lastDailyDay
            next.lastDailyId?.let { store[lastDailyId] = it }
            store[retentionDays] = next.retentionDays
            store[maxAttempts] = next.maxAttempts
        }
    }

    private fun Preferences.toPrefs(): UserPrefs {
        return UserPrefs(
            dailyHour = this[dailyHour] ?: 8,
            notificationsOn = this[notificationsOn] ?: true,
            starred6520 = this[starred] ?: false,
            interviewSimulation = this[interview] ?: false,
            officials = LocalOfficials(
                stateName = this[state].orEmpty(),
                senator = this[senator].orEmpty(),
                representative = this[representative].orEmpty(),
                governor = this[governor].orEmpty(),
                stateCapital = this[capital].orEmpty(),
                president = this[president]?.ifBlank { null } ?: LocalOfficials.DEFAULT_PRESIDENT,
                vicePresident = this[vicePresident]?.ifBlank { null } ?: LocalOfficials.DEFAULT_VICE_PRESIDENT,
                speaker = this[speaker]?.ifBlank { null } ?: LocalOfficials.DEFAULT_SPEAKER,
                chiefJustice = this[chiefJustice]?.ifBlank { null } ?: LocalOfficials.DEFAULT_CHIEF_JUSTICE,
            ),
            streak = this[streak] ?: 0,
            lastDailyDay = this[lastDailyDay].orEmpty(),
            lastDailyId = this[lastDailyId],
            retentionDays = this[retentionDays] ?: 90,
            maxAttempts = this[maxAttempts] ?: 2000,
        )
    }
}
