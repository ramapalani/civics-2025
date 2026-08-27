package com.ramapalani.civics2025.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ramapalani.civics2025.CivicsApplication
import com.ramapalani.civics2025.data.UserPrefs
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class SectionMastery(
    val section: String,
    val seen: Int,
    val total: Int,
    val accuracy: Int,
)

data class HomeUi(
    val prefs: UserPrefs = UserPrefs(),
    val mastery: List<SectionMastery> = emptyList(),
    val weakCount: Int = 0,
)

class HomeViewModel(app: CivicsApplication) : ViewModel() {
    val ui = combine(app.preferences.prefs, app.results.stats) { prefs, stats ->
        HomeUi(
            prefs = prefs,
            mastery = app.content.sections().map { section ->
                val ids = app.content.questions.filter { it.section == section }.map { it.id }
                val seenStats = ids.mapNotNull { stats[it] }
                val seen = seenStats.count { it.seenCount > 0 }
                val correct = seenStats.sumOf { it.correctCount }
                val attempts = seenStats.sumOf { it.seenCount }
                SectionMastery(
                    section = section,
                    seen = seen,
                    total = ids.size,
                    accuracy = if (attempts == 0) 0 else (correct * 100 / attempts),
                )
            },
            weakCount = stats.values.count { it.wrongCount > 0 },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUi())

    companion object {
        fun factory(app: CivicsApplication) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = HomeViewModel(app) as T
        }
    }
}

