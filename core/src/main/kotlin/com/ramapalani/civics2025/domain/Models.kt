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

/**
 * How close an answer came, independent of whether it passed. [EXACT] and [STRONG] are correct;
 * [CLOSE] is a near miss worth showing the official wording for; [WEAK] and [NONE] are wrong.
 * Only meaningful for [ScoreKind.SIMILARITY]; see [MatchResult.confidence].
 */
enum class AnswerConfidence { EXACT, STRONG, CLOSE, WEAK, NONE }

/** What a [MatchResult.similarity] measures, since the two kinds do not mean the same thing. */
enum class ScoreKind {
    /** How closely one answer matched the official wording, 0..1. */
    SIMILARITY,

    /** What fraction of the required answers were named, for ALL_N. Says nothing about wording. */
    COVERAGE,
}

data class MatchResult(
    val correct: Boolean,
    val matchedAnswers: List<String>,
    val similarity: Double,
    /** For ALL_N, how many distinct answers were required. 1 for ANY_ONE. */
    val required: Int = 1,
    val scoreKind: ScoreKind = ScoreKind.SIMILARITY,
) {
    /**
     * The confidence band, or null when the score is coverage. A multi-answer question that names
     * every answer is complete, not an exact match: the user may have misspelled all of them, and
     * the bands describe wording, so reporting one would overstate what was measured.
     */
    val confidence: AnswerConfidence?
        get() = if (scoreKind != ScoreKind.SIMILARITY) null else when {
            similarity >= 0.99 -> AnswerConfidence.EXACT
            similarity >= ACCEPT_THRESHOLD -> AnswerConfidence.STRONG
            similarity >= CLOSE_THRESHOLD -> AnswerConfidence.CLOSE
            similarity > 0.0 -> AnswerConfidence.WEAK
            else -> AnswerConfidence.NONE
        }

    /**
     * True when the answer missed but came close enough to be worth a nudge rather than a flat no.
     * A multi-answer question qualifies when it named all but one of the answers it needed.
     */
    val nearMiss: Boolean
        get() = when {
            correct -> false
            scoreKind == ScoreKind.COVERAGE -> required > 1 && matchedAnswers.size == required - 1
            else -> confidence == AnswerConfidence.CLOSE
        }

    companion object {
        /** Score at or above which an answer counts as correct. */
        const val ACCEPT_THRESHOLD = 0.78

        /** Score at or above which a miss is close enough to nudge rather than simply reject. */
        const val CLOSE_THRESHOLD = 0.60
    }
}

data class GradedAnswer(
    val questionId: Int,
    val path: AnswerPath,
    val rawInput: String,
    val correct: Boolean,
    val practicePoints: Double,
    val matchedAnswers: List<String>,
    val similarity: Double = 0.0,
    /** Null when [scoreKind] is coverage, which says nothing about wording. */
    val confidence: AnswerConfidence? = null,
    /** For ALL_N, how many distinct answers were required. 1 for ANY_ONE. */
    val required: Int = 1,
    val scoreKind: ScoreKind = ScoreKind.SIMILARITY,
) {
    /** True when the answer missed but came close enough to be worth a nudge rather than a flat no. */
    val nearMiss: Boolean
        get() = when {
            correct -> false
            scoreKind == ScoreKind.COVERAGE -> required > 1 && matchedAnswers.size == required - 1
            else -> confidence == AnswerConfidence.CLOSE
        }

    /** The score as a whole percent, for display. */
    val confidencePercent: Int get() = (similarity * 100).toInt().coerceIn(0, 100)

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
