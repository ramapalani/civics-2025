package com.ramapalani.civics2025.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ramapalani.civics2025.CivicsApplication
import java.text.DateFormat
import java.util.Date

@Composable
fun HistoryScreen(app: CivicsApplication, onBack: () -> Unit) {
    val sessions by app.results.sessions.collectAsState(initial = emptyList())
    val stats by app.results.stats.collectAsState(initial = emptyMap())
    val weak = stats.values.sortedByDescending { it.wrongCount }.take(8)

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        TextButton(onClick = onBack) { Text("Back") }
        Text("History", style = MaterialTheme.typography.headlineMedium)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 12.dp)) {
            if (weak.isNotEmpty()) {
                item { Text("Toughest questions", style = MaterialTheme.typography.titleMedium) }
                items(weak, key = { "w${it.questionId}" }) { stat ->
                    val q = app.content.byId(stat.questionId)
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text(q?.question ?: "Question ${stat.questionId}")
                            Text("${stat.wrongCount} missed · ${stat.correctCount} correct")
                        }
                    }
                }
            }
            item { Text("Past sessions", style = MaterialTheme.typography.titleMedium) }
            items(sessions, key = { it.id }) { session ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("${session.mode} · ${DateFormat.getDateTimeInstance().format(Date(session.createdAt))}")
                        Text(
                            "${session.officialCorrect} correct / ${session.asked} asked · practice ${session.practicePoints}" +
                                if (session.passed) " · passed" else " · not yet",
                        )
                    }
                }
            }
            if (sessions.isEmpty()) {
                item { Text("No saved tests yet. Take a 20-question test to see results here.") }
            }
        }
    }
}
