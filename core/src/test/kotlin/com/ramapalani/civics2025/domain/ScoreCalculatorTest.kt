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
    fun gradeCarriesConfidence() {
        val question = CivicsQuestion(
            id = 2,
            section = "S",
            topic = "T",
            question = "Q",
            acceptedAnswers = listOf("The Constitution"),
            extraInfo = "",
        )
        val exact = ScoreCalculator.grade(question, AnswerPath.TYPED, "the constitution", LocalOfficials())
        assertEquals(AnswerConfidence.EXACT, exact.confidence)
        assertEquals(100, exact.confidencePercent)

        val wrong = ScoreCalculator.grade(question, AnswerPath.TYPED, "bananas", LocalOfficials())
        assertFalse(wrong.correct)
        assertTrue(wrong.confidencePercent < 60, "got ${wrong.confidencePercent}")
    }

    @Test
    fun multiAnswerReportsPartialCoverage() {
        val question = CivicsQuestion(
            id = 10,
            section = "S",
            topic = "T",
            question = "Name two",
            acceptedAnswers = listOf("Equality", "Liberty", "Natural rights"),
            extraInfo = "",
            answerKind = "all_n",
            minRequired = 2,
        )
        // One of two required: wrong, but the score reflects half the work rather than zero.
        val partial = ScoreCalculator.grade(question, AnswerPath.TYPED, "liberty", LocalOfficials())
        assertFalse(partial.correct)
        assertEquals(1, partial.matchedAnswers.size)
        assertEquals(2, partial.required)
        assertEquals(50, partial.confidencePercent)

        val full = ScoreCalculator.grade(question, AnswerPath.TYPED, "liberty and equality", LocalOfficials())
        assertTrue(full.correct)
        assertEquals(100, full.confidencePercent)
    }

    @Test
    fun mcqCarriesNoPartialCredit() {
        val question = CivicsQuestion(
            id = 1,
            section = "S",
            topic = "T",
            question = "Q",
            acceptedAnswers = listOf("Republic"),
            extraInfo = "",
        )
        // A tapped choice is right or wrong, so its score must never land in a middle band.
        val right = ScoreCalculator.grade(question, AnswerPath.MCQ, "Republic", LocalOfficials())
        val wrong = ScoreCalculator.grade(question, AnswerPath.MCQ, "Monarchy", LocalOfficials())
        assertEquals(AnswerConfidence.EXACT, right.confidence)
        assertEquals(100, right.confidencePercent)
        assertEquals(AnswerConfidence.NONE, wrong.confidence)
        assertEquals(0, wrong.confidencePercent)
        assertFalse(wrong.nearMiss)
    }

    @Test
    fun failAtNineWrong() {
        assertTrue(ScoreCalculator.shouldStopInterview(correct = 3, wrong = 9, starred6520 = false))
        assertFalse(ScoreCalculator.shouldStopInterview(correct = 3, wrong = 8, starred6520 = false))
    }
}
