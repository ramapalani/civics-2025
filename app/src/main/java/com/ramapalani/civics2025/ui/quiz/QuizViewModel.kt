package com.ramapalani.civics2025.ui.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ramapalani.civics2025.CivicsApplication
import com.ramapalani.civics2025.data.ContentRepository
import com.ramapalani.civics2025.data.ResultsRepository
import com.ramapalani.civics2025.data.UserPreferencesRepository
import com.ramapalani.civics2025.data.UserPrefs
import com.ramapalani.civics2025.domain.AnswerKind
import com.ramapalani.civics2025.domain.AnswerMatcher
import com.ramapalani.civics2025.domain.AnswerPath
import com.ramapalani.civics2025.domain.CivicsQuestion
import com.ramapalani.civics2025.domain.GradedAnswer
import com.ramapalani.civics2025.domain.McqGenerator
import com.ramapalani.civics2025.domain.QuestionSelector
import com.ramapalani.civics2025.domain.ScoreCalculator
import com.ramapalani.civics2025.domain.ScoreSummary
import com.ramapalani.civics2025.domain.SessionMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class QuizUiState(
    val mode: SessionMode = SessionMode.LEARN,
    val loading: Boolean = true,
    val questions: List<CivicsQuestion> = emptyList(),
    val index: Int = 0,
    val input: String = "",
    val useMcq: Boolean = false,
    val mcqChoices: List<String> = emptyList(),
    val selectedMcq: Set<String> = emptySet(),
    val lastGrade: GradedAnswer? = null,
    val grades: List<GradedAnswer> = emptyList(),
    val combo: Int = 0,
    val finished: Boolean = false,
    val summary: ScoreSummary? = null,
    val prefs: UserPrefs = UserPrefs(),
    val browseSection: String? = null,
) {
    val current: CivicsQuestion? get() = questions.getOrNull(index)
}

class QuizViewModel(
    private val mode: SessionMode,
    private val section: String?,
    private val dailyQuestionId: Int?,
    private val content: ContentRepository,
    private val results: ResultsRepository,
    private val preferences: UserPreferencesRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(QuizUiState(mode = mode, browseSection = section))
    val state: StateFlow<QuizUiState> = _state

    init {
        viewModelScope.launch { start() }
    }

    private suspend fun start() {
        val prefs = preferences.prefs.first()
        val stats = results.statsMap()
        val bank = if (prefs.starred6520) content.questions.filter { it.starred6520 } else content.questions
        val questions = when (mode) {
            SessionMode.LEARN -> {
                val pool = if (section != null) bank.filter { it.section == section } else bank
                val weak = pool.sortedByDescending { stats[it.id]?.wrongCount ?: 0 }
                if (section == "WEAK") {
                    content.questions.sortedByDescending { stats[it.id]?.wrongCount ?: 0 }.take(20)
                } else {
                    weak
                }
            }
            SessionMode.TEST -> QuestionSelector.pickMany(
                questions = bank,
                stats = stats,
                count = if (prefs.starred6520) QuestionSelector.INTERVIEW_6520_SIZE else QuestionSelector.TEST_SIZE,
                starredOnly = false,
            )
            SessionMode.DAILY -> {
                val chosen = dailyQuestionId?.let { id -> content.byId(id) }
                    ?: QuestionSelector.pickDaily(bank, stats, System.currentTimeMillis(), prefs.lastDailyId)
                listOf(chosen)
            }
        }
        questions.firstOrNull()?.id?.let { results.touchShown(it) }
        _state.update { it.copy(loading = false, questions = questions, prefs = prefs) }
    }

    fun onInput(value: String) {
        _state.update { it.copy(input = value) }
    }

    fun toggleMcq() {
        val current = _state.value.current ?: return
        val next = !_state.value.useMcq
        val choices = if (next) {
            McqGenerator.choices(current, content.questions, _state.value.prefs.officials)
        } else {
            emptyList()
        }
        _state.update {
            it.copy(useMcq = next, mcqChoices = choices, selectedMcq = emptySet(), input = "", lastGrade = null)
        }
    }

    fun toggleMcqChoice(choice: String) {
        val snapshot = _state.value
        if (!snapshot.useMcq || snapshot.lastGrade != null) return
        val question = snapshot.current ?: return
        if (question.kind != AnswerKind.ALL_N) {
            submit(choice)
            return
        }
        val next = snapshot.selectedMcq.toMutableSet()
        if (!next.add(choice)) next.remove(choice)
        _state.update { it.copy(selectedMcq = next) }
    }

    fun submitMcqSelection() {
        val snapshot = _state.value
        submit(snapshot.selectedMcq.joinToString(", "))
    }

    fun submit(selectedMcq: String? = null) {
        val snapshot = _state.value
        val question = snapshot.current ?: return
        if (snapshot.lastGrade != null) return
        val path = if (snapshot.useMcq) AnswerPath.MCQ else AnswerPath.TYPED
        val raw = selectedMcq ?: snapshot.input
        val grade = ScoreCalculator.grade(question, path, raw, snapshot.prefs.officials)
        val grades = snapshot.grades + grade
        val combo = if (grade.correct && path == AnswerPath.TYPED) snapshot.combo + 1 else 0
        _state.update { it.copy(lastGrade = grade, grades = grades, combo = combo) }
    }

    fun markReview(needsReview: Boolean) {
        val id = _state.value.current?.id ?: return
        viewModelScope.launch { results.markReview(id, needsReview) }
    }

    fun next() {
        val snapshot = _state.value
        val interview = snapshot.mode == SessionMode.TEST && snapshot.prefs.interviewSimulation
        val correct = snapshot.grades.count { it.correct }
        val wrong = snapshot.grades.count { !it.correct }
        val shouldStop = interview && ScoreCalculator.shouldStopInterview(
            correct,
            wrong,
            snapshot.prefs.starred6520,
        )
        val lastQuestion = snapshot.index >= snapshot.questions.lastIndex
        if (shouldStop || lastQuestion) {
            finish()
            return
        }
        val nextIndex = snapshot.index + 1
        viewModelScope.launch {
            snapshot.questions.getOrNull(nextIndex)?.id?.let { results.touchShown(it) }
        }
        _state.update {
            it.copy(
                index = nextIndex,
                input = "",
                useMcq = false,
                mcqChoices = emptyList(),
                selectedMcq = emptySet(),
                lastGrade = null,
            )
        }
    }

    private fun finish() {
        val snapshot = _state.value
        val summary = ScoreCalculator.summarize(
            grades = snapshot.grades,
            starred6520 = snapshot.prefs.starred6520,
            interviewSimulation = snapshot.mode == SessionMode.TEST && snapshot.prefs.interviewSimulation,
        )
        viewModelScope.launch {
            results.saveSession(
                mode = snapshot.mode,
                grades = snapshot.grades,
                summary = summary,
                starred6520 = snapshot.prefs.starred6520,
                interview = snapshot.mode == SessionMode.TEST && snapshot.prefs.interviewSimulation,
            )
            if (snapshot.mode == SessionMode.DAILY) {
                val today = LocalDate.now().toString()
                preferences.update { prefs ->
                    val newStreak = if (prefs.lastDailyDay == LocalDate.now().minusDays(1).toString()) {
                        prefs.streak + 1
                    } else if (prefs.lastDailyDay == today) {
                        prefs.streak
                    } else {
                        1
                    }
                    prefs.copy(
                        streak = newStreak,
                        lastDailyDay = today,
                        lastDailyId = snapshot.current?.id ?: prefs.lastDailyId,
                    )
                }
            }
        }
        _state.update { it.copy(finished = true, summary = summary) }
    }

    fun acceptedAnswers(): List<String> {
        val question = _state.value.current ?: return emptyList()
        return AnswerMatcher.acceptedFor(question, _state.value.prefs.officials)
    }

    companion object {
        fun factory(
            app: CivicsApplication,
            mode: SessionMode,
            section: String? = null,
            dailyQuestionId: Int? = null,
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return QuizViewModel(
                        mode,
                        section,
                        dailyQuestionId,
                        app.content,
                        app.results,
                        app.preferences,
                    ) as T
                }
            }
        }
    }
}
