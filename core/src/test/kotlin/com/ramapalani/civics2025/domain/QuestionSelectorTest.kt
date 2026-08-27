package com.ramapalani.civics2025.domain

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QuestionSelectorTest {
    private val questions = (1..40).map { id ->
        CivicsQuestion(
            id = id,
            section = "S",
            topic = "T",
            question = "Q$id",
            acceptedAnswers = listOf("A"),
            extraInfo = "",
            starred6520 = id <= 20,
        )
    }

    @Test
    fun picksTwentyUnique() {
        val picked = QuestionSelector.pickMany(questions, emptyMap(), 20, Random(1))
        assertEquals(20, picked.size)
        assertEquals(20, picked.map { it.id }.toSet().size)
    }

    @Test
    fun prefersHighWrongCounts() {
        val stats = mapOf(
            7 to QuestionStat(7, seenCount = 10, wrongCount = 9),
            8 to QuestionStat(8, seenCount = 10, wrongCount = 8),
        )
        val counts = mutableMapOf<Int, Int>()
        repeat(400) {
            val pick = QuestionSelector.pickMany(questions, stats, 5, Random(it))
            pick.forEach { q -> counts[q.id] = (counts[q.id] ?: 0) + 1 }
        }
        assertTrue((counts[7] ?: 0) > (counts[30] ?: 0))
        assertTrue((counts[8] ?: 0) > (counts[31] ?: 0))
    }

    @Test
    fun dailySkipsRecent() {
        val now = 1_000_000L
        val stats = mapOf(
            1 to QuestionStat(1, lastShownAtMillis = now - 1_000),
        )
        val pick = QuestionSelector.pickDaily(questions.take(3), stats, now, lastDailyId = 2, random = Random(0))
        assertEquals(3, pick.id)
    }
}
