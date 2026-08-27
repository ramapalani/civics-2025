package com.ramapalani.civics2025.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramapalani.civics2025.CivicsApplication

@Composable
fun HomeScreen(
    app: CivicsApplication,
    onLearn: (String?) -> Unit,
    onTest: () -> Unit,
    onDaily: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit,
    onAbout: () -> Unit,
    onTextbook: () -> Unit,
) {
    val vm: HomeViewModel = viewModel(factory = HomeViewModel.factory(app))
    val ui by vm.ui.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Civics 2025", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text(
            "Unofficial practice for the 2025 civics test. A real interview is 20 questions — pass with 12. Not affiliated with USCIS.",
            style = MaterialTheme.typography.bodyLarge,
        )
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Daily streak: ${ui.prefs.streak} 🔥", style = MaterialTheme.typography.titleLarge)
                Text("Missed questions ready for review: ${ui.weakCount}")
            }
        }
        Button(onClick = { onLearn(null) }, modifier = Modifier.fillMaxWidth()) {
            Text("Learn mode")
        }
        Button(onClick = onTest, modifier = Modifier.fillMaxWidth()) {
            Text(if (ui.prefs.starred6520) "Test (10 questions, 65/20)" else "Test (20 questions)")
        }
        OutlinedButton(onClick = onDaily, modifier = Modifier.fillMaxWidth()) {
            Text("Today’s question")
        }
        OutlinedButton(onClick = onTextbook, modifier = Modifier.fillMaxWidth()) {
            Text("One Nation, One People textbook")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = onHistory) { Text("History") }
            TextButton(onClick = onSettings) { Text("Settings") }
            TextButton(onClick = onAbout) { Text("About & privacy") }
        }
        Text("Civics map", style = MaterialTheme.typography.titleMedium)
        ui.mastery.forEach { section ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onLearn(section.section) },
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(section.section, fontWeight = FontWeight.SemiBold)
                    Text("${section.seen}/${section.total} seen · ${section.accuracy}% correct")
                    LinearProgressIndicator(
                        progress = { if (section.total == 0) 0f else section.seen / section.total.toFloat() },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        OutlinedButton(onClick = { onLearn("WEAK") }, modifier = Modifier.fillMaxWidth()) {
            Text("Practice missed questions")
        }
    }
}
