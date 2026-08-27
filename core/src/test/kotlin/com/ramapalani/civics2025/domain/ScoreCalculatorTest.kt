package com.ramapalani.civics2025.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScoreCalculatorTest {
    private fun grade(correct: Boolean, mcq: Boolean) = GradedAnswer(
        questionId = 1,
        path = if (mcq) AnswerPath.MCQ else AnswerPath.TYPED,
        rawInput = "x",
        correct = correct,
        practicePoints = if (!correct) 0.0 else if (mcq) 0.6 else 1.0,
        matchedAnswers = emptyList(),
    )

    @Test
    fun passAtTwelve() {
        val grades = List(12) { grade(true, false) } + List(8) { grade(false, false) }
        val summary = ScoreCalculator.summarize(grades, starred6520 = false, interviewSimulation = false)
        assertTrue(summary.passed)
        assertEquals(12, summary.officialCorrect)
    }

    @Test
    fun interviewStopsAtTwelve() {
        val grades = List(12) { grade(true, false) } + List(8) { grade(true, false) }
        val summary = ScoreCalculator.summarize(grades, starred6520 = false, interviewSimulation = true)
        assertTrue(summary.passed)
        assertEquals(12, summary.asked)
        assertTrue(summary.stoppedEarly)
    }

    @Test
    fun mcqPenalty() {
        val question = CivicsQuestion(
            id = 12,
            section = "S",
            topic = "T",
            question = "Q",
            acceptedAnswers = listOf("Capitalism"),
            extraInfo = "",
        )
        val typed = ScoreCalculator.grade(question, AnswerPath.TYPED, "capitalism", LocalOfficials())
        val mcq = ScoreCalculator.grade(question, AnswerPath.MCQ, "Capitalism", LocalOfficials())
        assertEquals(1.0, typed.practicePoints)
        assertEquals(0.6, mcq.practicePoints)
        assertTrue(typed.correct && mcq.correct)
    }

    @Test
    fun failAtNineWrong() {
        assertTrue(ScoreCalculator.shouldStopInterview(correct = 3, wrong = 9, starred6520 = false))
        assertFalse(ScoreCalculator.shouldStopInterview(correct = 3, wrong = 8, starred6520 = false))
    }
}
