package com.ramapalani.civics2025.ui.quiz

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ramapalani.civics2025.domain.AnswerKind
import com.ramapalani.civics2025.domain.SessionMode
import com.ramapalani.civics2025.ui.theme.DeepGreen
import com.ramapalani.civics2025.ui.theme.FlagRed

@Composable
fun QuestionScreen(
    viewModel: QuizViewModel,
    onExit: () -> Unit,
    onOpenGuide: (page: Int, title: String) -> Unit = { _, _ -> },
) {
    val state by viewModel.state.collectAsState()
    if (state.loading) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            CircularProgressIndicator()
        }
        return
    }
    if (state.finished) {
        ResultScreen(state, onExit)
        return
    }
    val question = state.current
    if (question == null) {
        Column(Modifier.padding(24.dp)) {
            Text("No questions in this set.")
            Button(onClick = onExit) { Text("Back") }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            "${state.mode.name.lowercase().replaceFirstChar { it.titlecase() }}  ·  ${state.index + 1} / ${state.questions.size}",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.testTag("progressLabel"),
        )
        if (state.combo >= 2) {
            Text("Combo x${state.combo} — keep the streak going!", color = DeepGreen, fontWeight = FontWeight.Bold)
        }
        Text(question.question, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.testTag("questionText"))
        Text("${question.section} · ${question.topic}", style = MaterialTheme.typography.bodyMedium)
        if (question.minRequired > 1) {
            Text("Name ${question.minRequired}. Separate answers with a comma or “and”.")
        }

        if (state.useMcq) {
            Text("Choices are the official USCIS answers. Multiple choice scores 0.6 if correct.")
            if (question.kind == AnswerKind.ALL_N) {
                Text("Select ${question.minRequired} official answers, then check.")
            }
            state.mcqChoices.forEach { choice ->
                val selected = choice in state.selectedMcq
                OutlinedButton(
                    onClick = { viewModel.toggleMcqChoice(choice) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.lastGrade == null,
                ) {
                    Text(if (selected) "✓ $choice" else choice)
                }
            }
            if (question.kind == AnswerKind.ALL_N) {
                Button(
                    onClick = viewModel::submitMcqSelection,
                    enabled = state.lastGrade == null && state.selectedMcq.size >= question.minRequired,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Check") }
            }
        } else {
            OutlinedTextField(
                value = state.input,
                onValueChange = viewModel::onInput,
                modifier = Modifier.fillMaxWidth().testTag("answerInput"),
                minLines = 2,
                label = { Text("Type your answer") },
                enabled = state.lastGrade == null,
            )
            Button(
                onClick = { viewModel.submit() },
                enabled = state.lastGrade == null && state.input.isNotBlank(),
                modifier = Modifier.fillMaxWidth().testTag("checkButton"),
            ) { Text("Check") }
        }

        TextButton(onClick = viewModel::toggleMcq, enabled = state.lastGrade == null) {
            Text(if (state.useMcq) "Switch to typed answer" else "I want multiple choice (−0.4 points)")
        }

        AnimatedVisibility(visible = state.lastGrade != null) {
            val grade = state.lastGrade
            if (grade != null) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            if (grade.correct) "Correct! +${grade.practicePoints}" else "Not quite.",
                            color = if (grade.correct) DeepGreen else FlagRed,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.testTag("gradeResult"),
                        )
                        Text("Accepted answers", fontWeight = FontWeight.SemiBold)
                        viewModel.acceptedAnswers().forEach { Text("• $it") }
                        Text("Why this matters", fontWeight = FontWeight.SemiBold)
                        Text(question.extraInfo)
                        OutlinedButton(
                            onClick = {
                                onOpenGuide(
                                    question.studyGuidePage,
                                    "Chapter ${question.studyGuideChapter}: ${question.studyGuideTitle}",
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Read textbook p. ${question.studyGuidePage}")
                        }
                        if (state.mode == SessionMode.LEARN) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { viewModel.markReview(false) }) { Text("Known") }
                                OutlinedButton(onClick = { viewModel.markReview(true) }) { Text("Needs review") }
                            }
                        }
                        Button(onClick = viewModel::next, modifier = Modifier.fillMaxWidth().testTag("nextButton")) {
                            Text(if (state.index >= state.questions.lastIndex) "Finish" else "Next")
                        }
                    }
                }
            }
        }

        TextButton(onClick = onExit, modifier = Modifier.testTag("exitButton")) { Text("Exit") }
    }
}

@Composable
private fun ResultScreen(state: QuizUiState, onExit: () -> Unit) {
    val summary = state.summary
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            if (summary?.passed == true) "You passed!" else "Keep practicing",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = if (summary?.passed == true) DeepGreen else FlagRed,
        )
        if (summary != null) {
            Text("Official score: ${summary.officialCorrect} correct, ${summary.officialWrong} wrong")
            Text("Practice score: ${summary.practicePoints} / ${summary.maxPracticePoints} (${summary.practicePercent}%)")
            if (summary.stoppedEarly) {
                Text("Interview simulation stopped early — 12 correct or 9 wrong.")
            }
        }
        Text("Typed answers earn 1.0. Multiple choice earns 0.6 when correct.")
        Button(onClick = onExit, modifier = Modifier.fillMaxWidth().testTag("homeButton")) { Text("Home") }
    }
}
