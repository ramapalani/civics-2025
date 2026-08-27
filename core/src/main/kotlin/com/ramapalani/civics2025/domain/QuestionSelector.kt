package com.ramapalani.civics2025.domain

import kotlin.random.Random

object QuestionSelector {
    const val TEST_SIZE = 20
    const val INTERVIEW_6520_SIZE = 10
    const val DAILY_COOLDOWN_MS = 2L * 24 * 60 * 60 * 1000

    fun weight(stat: QuestionStat?): Double {
        val wrong = stat?.wrongCount ?: 0
        val review = if (stat?.needsReview == true) 1 else 0
        val unseenBoost = if ((stat?.seenCount ?: 0) == 0) 2 else 0
        return 1.0 + 3.0 * wrong + review + unseenBoost
    }

    fun pickMany(
        questions: List<CivicsQuestion>,
        stats: Map<Int, QuestionStat>,
        count: Int,
        random: Random = Random.Default,
        starredOnly: Boolean = false,
    ): List<CivicsQuestion> {
        val pool = if (starredOnly) questions.filter { it.starred6520 } else questions
        if (pool.size <= count) return pool.shuffled(random)

        val remaining = pool.toMutableList()
        val picked = mutableListOf<CivicsQuestion>()
        repeat(count) {
            val chosen = weightedPick(remaining, { weight(stats[it.id]) }, random)
            remaining.remove(chosen)
            picked += chosen
        }
        return picked
    }

    private fun <T> weightedPick(items: List<T>, weightOf: (T) -> Double, random: Random): T {
        val weights = items.map { weightOf(it).coerceAtLeast(0.0001) }
        var ticket = random.nextDouble() * weights.sum()
        items.forEachIndexed { index, item ->
            ticket -= weights[index]
            if (ticket <= 0.0) return item
        }
        return items.last()
    }

    fun pickDaily(
        questions: List<CivicsQuestion>,
        stats: Map<Int, QuestionStat>,
        nowMillis: Long,
        lastDailyId: Int?,
        random: Random = Random.Default,
        starredOnly: Boolean = false,
    ): CivicsQuestion {
        val pool = if (starredOnly) questions.filter { it.starred6520 } else questions
        val cooled = pool.filter { q ->
            val last = stats[q.id]?.lastShownAtMillis
            last == null || nowMillis - last >= DAILY_COOLDOWN_MS
        }.filter { it.id != lastDailyId }
        val candidates = cooled.ifEmpty { pool.filter { it.id != lastDailyId }.ifEmpty { pool } }
        return pickMany(candidates, stats, 1, random).first()
    }
}
