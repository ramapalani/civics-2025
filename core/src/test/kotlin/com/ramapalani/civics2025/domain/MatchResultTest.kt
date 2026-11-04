package com.ramapalani.civics2025.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MatchResultTest {
    private fun result(similarity: Double, required: Int = 1) = MatchResult(
        correct = similarity >= MatchResult.ACCEPT_THRESHOLD,
        matchedAnswers = emptyList(),
        similarity = similarity,
        required = required,
    )

    @Test
    fun bandsSplitAtTheirBoundaries() {
        assertEquals(AnswerConfidence.EXACT, result(1.0).confidence)
        assertEquals(AnswerConfidence.EXACT, result(0.99).confidence)
        assertEquals(AnswerConfidence.STRONG, result(0.989).confidence)

        assertEquals(AnswerConfidence.STRONG, result(MatchResult.ACCEPT_THRESHOLD).confidence)
        assertEquals(AnswerConfidence.CLOSE, result(MatchResult.ACCEPT_THRESHOLD - 0.001).confidence)

        assertEquals(AnswerConfidence.CLOSE, result(0.60).confidence)
        assertEquals(AnswerConfidence.WEAK, result(0.599).confidence)

        assertEquals(AnswerConfidence.WEAK, result(0.001).confidence)
        assertEquals(AnswerConfidence.NONE, result(0.0).confidence)
    }

    @Test
    fun acceptThresholdAgreesWithCorrectness() {
        // The band and the verdict must never disagree about whether an answer passed.
        listOf(0.0, 0.3, 0.59, 0.6, 0.77, 0.78, 0.9, 1.0).forEach { score ->
            val match = result(score)
            val passingBand = match.confidence == AnswerConfidence.EXACT ||
                match.confidence == AnswerConfidence.STRONG
            assertEquals(match.correct, passingBand, "disagreed at $score")
        }
    }

    @Test
    fun nearMissOnlyCoversCloseFailures() {
        assertTrue(result(0.70).nearMiss)
        assertTrue(result(0.60).nearMiss)
        // Too far off to be worth a nudge.
        assertFalse(result(0.59).nearMiss)
        assertFalse(result(0.0).nearMiss)
        // Correct answers are never a near miss, however they scored.
        assertFalse(result(0.78).nearMiss)
        assertFalse(result(1.0).nearMiss)
    }

    @Test
    fun anyOneDefaultsToRequiringOne() {
        val match = MatchResult(true, listOf("Republic"), 1.0)
        assertEquals(1, match.required)
        assertEquals(ScoreKind.SIMILARITY, match.scoreKind)
    }

    @Test
    fun coverageReportsNoConfidenceBand() {
        // Naming every answer says nothing about spelling, so a band would overstate the measurement.
        val complete = MatchResult(true, listOf("Liberty", "Equality"), 1.0, 2, ScoreKind.COVERAGE)
        assertNull(complete.confidence)
        assertFalse(complete.nearMiss)

        val partial = MatchResult(false, listOf("Liberty"), 0.5, 2, ScoreKind.COVERAGE)
        assertNull(partial.confidence)
    }

    @Test
    fun coverageNearMissMeansOneAnswerShort() {
        fun coverage(matched: Int, required: Int) = MatchResult(
            correct = matched >= required,
            matchedAnswers = List(matched) { "answer $it" },
            similarity = matched.toDouble() / required,
            required = required,
            scoreKind = ScoreKind.COVERAGE,
        )

        assertTrue(coverage(2, 3).nearMiss)
        assertTrue(coverage(1, 2).nearMiss)
        // Two or more short is not close, and a complete answer is not a miss.
        assertFalse(coverage(1, 3).nearMiss)
        assertFalse(coverage(0, 2).nearMiss)
        assertFalse(coverage(3, 3).nearMiss)
    }
}
