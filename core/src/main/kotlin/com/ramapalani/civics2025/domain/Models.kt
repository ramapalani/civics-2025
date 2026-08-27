package com.ramapalani.civics2025.domain

import kotlinx.serialization.Serializable

enum class AnswerKind {
    ANY_ONE,
    ALL_N,
}

@Serializable
data class CivicsQuestion(
    val id: Int,
    val section: String,
    val topic: String,
    val question: String,
    val acceptedAnswers: List<String>,
    val officialAnswers: List<String> = emptyList(),
    val extraInfo: String,
    val starred6520: Boolean = false,
    val answerKind: String = "any_one",
    val minRequired: Int = 1,
    val currentOfficial: Boolean = false,
    val stateSpecific: Boolean = false,
    val stateField: String? = null,
    val studyGuideChapter: Int = 1,
    val studyGuideTitle: String = "The U.S. Constitution",
    val studyGuidePage: Int = 8,
) {
    val kind: AnswerKind
        get() = if (answerKind == "all_n") AnswerKind.ALL_N else AnswerKind.ANY_ONE
}

data class LocalOfficials(
    val stateName: String = "",
    val senator: String = "",
    val representative: String = "",
    val governor: String = "",
    val stateCapital: String = "",
    val president: String = DEFAULT_PRESIDENT,
    val vicePresident: String = DEFAULT_VICE_PRESIDENT,
    val speaker: String = DEFAULT_SPEAKER,
    val chiefJustice: String = DEFAULT_CHIEF_JUSTICE,
) {
    companion object {
        const val DEFAULT_PRESIDENT = "Donald J. Trump"
        const val DEFAULT_VICE_PRESIDENT = "JD Vance"
        const val DEFAULT_SPEAKER = "Mike Johnson"
        const val DEFAULT_CHIEF_JUSTICE = "John G. Roberts, Jr."
    }

    fun federalName(field: String?): String = when (field) {
        "president" -> president
        "vicePresident" -> vicePresident
        "speaker" -> speaker
        "chiefJustice" -> chiefJustice
        else -> ""
    }
}

data class QuestionStat(
    val questionId: Int,
    val seenCount: Int = 0,
    val correctCount: Int = 0,
    val wrongCount: Int = 0,
    val needsReview: Boolean = false,
    val lastShownAtMillis: Long? = null,
)

enum class SessionMode { LEARN, TEST, DAILY }

enum class AnswerPath { TYPED, MCQ }

data class MatchResult(
    val correct: Boolean,
    val matchedAnswers: List<String>,
    val similarity: Double,
)

data class GradedAnswer(
    val questionId: Int,
    val path: AnswerPath,
    val rawInput: String,
    val correct: Boolean,
    val practicePoints: Double,
    val matchedAnswers: List<String>,
) {
    companion object {
        const val TYPED_POINTS = 1.0
        const val MCQ_POINTS = 0.6
    }
}

data class ScoreSummary(
    val asked: Int,
    val officialCorrect: Int,
    val officialWrong: Int,
    val practicePoints: Double,
    val maxPracticePoints: Double,
    val passed: Boolean,
    val stoppedEarly: Boolean,
) {
    val practicePercent: Int
        get() = if (maxPracticePoints <= 0) 0 else ((practicePoints / maxPracticePoints) * 100).toInt()
}
