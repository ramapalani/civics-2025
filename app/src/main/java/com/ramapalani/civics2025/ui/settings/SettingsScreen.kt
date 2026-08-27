package com.ramapalani.civics2025.ui.settings

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ramapalani.civics2025.CivicsApplication
import com.ramapalani.civics2025.work.DailyQuestionWorker
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(app: CivicsApplication, onBack: () -> Unit, onAbout: () -> Unit) {
    val context = LocalContext.current
    val prefs by app.preferences.prefs.collectAsState(
        initial = com.ramapalani.civics2025.data.UserPrefs(),
    )
    val scope = rememberCoroutineScope()
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    fun save(update: (com.ramapalani.civics2025.data.UserPrefs) -> com.ramapalani.civics2025.data.UserPrefs) {
        scope.launch {
            app.preferences.update(update)
            val next = update(prefs)
            DailyQuestionWorker.schedule(app, next.dailyHour, next.notificationsOn)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextButton(onClick = onBack) { Text("Back") }
        Text("Settings", style = MaterialTheme.typography.headlineMedium)
        RowSwitch("Daily question notification", prefs.notificationsOn) { on ->
            if (on && Build.VERSION.SDK_INT >= 33) {
                permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            if (on && Build.VERSION.SDK_INT >= 31) {
                val alarms = context.getSystemService(AlarmManager::class.java)
                if (!alarms.canScheduleExactAlarms()) {
                    runCatching {
                        context.startActivity(
                            Intent(AndroidSettings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                data = Uri.parse("package:${context.packageName}")
                            },
                        )
                    }
                }
            }
            save { it.copy(notificationsOn = on) }
        }
        Text("Daily hour: ${prefs.dailyHour}:00")
        Text(
            "The reminder fires at that hour (for example 8:00). Android may ask for Alarms & reminders permission so it stays on time.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Slider(
            value = prefs.dailyHour.toFloat(),
            onValueChange = { hour ->
                scope.launch { app.preferences.update { it.copy(dailyHour = hour.toInt()) } }
            },
            onValueChangeFinished = {
                DailyQuestionWorker.schedule(app, prefs.dailyHour, prefs.notificationsOn)
            },
            valueRange = 6f..21f,
            steps = 14,
        )
        RowSwitch("Interview simulation (stop at 12 correct or 9 wrong)", prefs.interviewSimulation) { on ->
            save { it.copy(interviewSimulation = on) }
        }
        RowSwitch("65/20 special consideration (20 starred questions, 10-question test)", prefs.starred6520) { on ->
            save { it.copy(starred6520 = on) }
        }
        Text("Current U.S. officials (used for President, Vice President, Speaker, Chief Justice)")
        Text(
            "These names can change. Confirm at uscis.gov/citizenship/testupdates, then update them here. President and Vice President: usa.gov. Chief Justice: supremecourt.gov.",
            style = MaterialTheme.typography.bodyMedium,
        )
        TextButton(
            onClick = {
                runCatching {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://www.usa.gov/presidents")),
                    )
                }
            },
        ) {
            Text("President and Vice President (usa.gov)")
        }
        TextButton(
            onClick = {
                runCatching {
                    context.startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://www.supremecourt.gov/about/biographies.aspx"),
                        ),
                    )
                }
            },
        ) {
            Text("Supreme Court justices (supremecourt.gov)")
        }
        OutlinedTextField(prefs.officials.president, { v -> save { it.copy(officials = it.officials.copy(president = v)) } }, label = { Text("President") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(prefs.officials.vicePresident, { v -> save { it.copy(officials = it.officials.copy(vicePresident = v)) } }, label = { Text("Vice President") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(prefs.officials.speaker, { v -> save { it.copy(officials = it.officials.copy(speaker = v)) } }, label = { Text("Speaker of the House") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(prefs.officials.chiefJustice, { v -> save { it.copy(officials = it.officials.copy(chiefJustice = v)) } }, label = { Text("Chief Justice") }, modifier = Modifier.fillMaxWidth())
        Text("Your state officials (used for senator, representative, governor, capital)")
        Text(
            "Look up your U.S. senator and House representative by address on congress.gov, then type the names here.",
            style = MaterialTheme.typography.bodyMedium,
        )
        TextButton(
            onClick = {
                runCatching {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://www.congress.gov/members")),
                    )
                }
            },
        ) {
            Text("Find senator and representative (congress.gov)")
        }
        OutlinedTextField(prefs.officials.stateName, { v -> save { it.copy(officials = it.officials.copy(stateName = v)) } }, label = { Text("State") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(prefs.officials.senator, { v -> save { it.copy(officials = it.officials.copy(senator = v)) } }, label = { Text("U.S. senator") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(prefs.officials.representative, { v -> save { it.copy(officials = it.officials.copy(representative = v)) } }, label = { Text("U.S. representative") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(prefs.officials.governor, { v -> save { it.copy(officials = it.officials.copy(governor = v)) } }, label = { Text("Governor") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(prefs.officials.stateCapital, { v -> save { it.copy(officials = it.officials.copy(stateCapital = v)) } }, label = { Text("State capital") }, modifier = Modifier.fillMaxWidth())
        Text("Keep results ${prefs.retentionDays} days, max ${prefs.maxAttempts} answers.")
        OutlinedButton(onClick = {
            scope.launch { app.results.enforceRetention(prefs.retentionDays, prefs.maxAttempts) }
        }) { Text("Clean old results now") }
        OutlinedButton(onClick = onAbout, modifier = Modifier.fillMaxWidth()) {
            Text("About & privacy")
        }
        Text(
            "This app is unofficial and not affiliated with USCIS. The full notice and privacy details are in the app, not on a website.",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun RowSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    androidx.compose.foundation.layout.Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, modifier = Modifier.weight(1f).padding(end = 12.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
