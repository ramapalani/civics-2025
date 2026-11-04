package com.ramapalani.civics2025.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
    fun matchedCountTracksMatchedAnswers() {
        val none = MatchResult(false, emptyList(), 0.0)
        val two = MatchResult(true, listOf("Liberty", "Equality"), 1.0, required = 2)
        assertEquals(0, none.matchedCount)
        assertEquals(2, two.matchedCount)
        assertEquals(2, two.required)
    }

    @Test
    fun anyOneDefaultsToRequiringOne() {
        assertEquals(1, MatchResult(true, listOf("Republic"), 1.0).required)
    }
}
