package com.ramapalani.civics2025.domain

object ScoreCalculator {
    const val STANDARD_ASK = 20
    const val STANDARD_PASS = 12
    const val STANDARD_FAIL_WRONG = 9
    const val SPECIAL_ASK = 10
    const val SPECIAL_PASS = 6
    const val SPECIAL_FAIL_WRONG = 5

    fun grade(
        question: CivicsQuestion,
        path: AnswerPath,
        rawInput: String,
        officials: LocalOfficials,
    ): GradedAnswer {
        val match = if (path == AnswerPath.MCQ) {
            val exact = question.acceptedAnswers.any { it.equals(rawInput.trim(), ignoreCase = true) } ||
                AnswerMatcher.match(question, rawInput, officials).correct
            MatchResult(exact, if (exact) listOf(rawInput) else emptyList(), if (exact) 1.0 else 0.0)
        } else {
            AnswerMatcher.match(question, rawInput, officials)
        }
        val points = when {
            !match.correct -> 0.0
            path == AnswerPath.MCQ -> GradedAnswer.MCQ_POINTS
            else -> GradedAnswer.TYPED_POINTS
        }
        return GradedAnswer(
            questionId = question.id,
            path = path,
            rawInput = rawInput,
            correct = match.correct,
            practicePoints = points,
            matchedAnswers = match.matchedAnswers,
        )
    }

    fun summarize(
        grades: List<GradedAnswer>,
        starred6520: Boolean,
        interviewSimulation: Boolean,
    ): ScoreSummary {
        val passAt = if (starred6520) SPECIAL_PASS else STANDARD_PASS
        val failAt = if (starred6520) SPECIAL_FAIL_WRONG else STANDARD_FAIL_WRONG
        var correct = 0
        var wrong = 0
        var points = 0.0
        var asked = 0
        var stopped = false
        for (grade in grades) {
            asked += 1
            points += grade.practicePoints
            if (grade.correct) correct += 1 else wrong += 1
            if (interviewSimulation && (correct >= passAt || wrong >= failAt)) {
                stopped = asked < grades.size || correct >= passAt || wrong >= failAt
                if (correct >= passAt || wrong >= failAt) {
                    // Count only through the stopping question.
                    val viewed = grades.take(asked)
                    return ScoreSummary(
                        asked = viewed.size,
                        officialCorrect = viewed.count { it.correct },
                        officialWrong = viewed.count { !it.correct },
                        practicePoints = viewed.sumOf { it.practicePoints },
                        maxPracticePoints = viewed.size.toDouble(),
                        passed = viewed.count { it.correct } >= passAt,
                        stoppedEarly = viewed.size < (if (starred6520) SPECIAL_ASK else STANDARD_ASK) ||
                            viewed.size < grades.size,
                    )
                }
            }
        }
        return ScoreSummary(
            asked = asked,
            officialCorrect = correct,
            officialWrong = wrong,
            practicePoints = points,
            maxPracticePoints = asked.toDouble(),
            passed = correct >= passAt,
            stoppedEarly = stopped,
        )
    }

    fun shouldStopInterview(correct: Int, wrong: Int, starred6520: Boolean): Boolean {
        val passAt = if (starred6520) SPECIAL_PASS else STANDARD_PASS
        val failAt = if (starred6520) SPECIAL_FAIL_WRONG else STANDARD_FAIL_WRONG
        return correct >= passAt || wrong >= failAt
    }
}
