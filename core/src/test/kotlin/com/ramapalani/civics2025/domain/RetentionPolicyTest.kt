package com.ramapalani.civics2025.domain

import kotlin.test.Test
import kotlin.test.assertTrue

class RetentionPolicyTest {
    @Test
    fun dropsSessionsOlderThanNinetyDays() {
        val now = 200L * 24 * 60 * 60 * 1000
        val old = now - 120L * 24 * 60 * 60 * 1000
        val recent = now - 10L * 24 * 60 * 60 * 1000
        val plan = RetentionPolicy.plan(
            sessionsOldestFirst = listOf(1L to old, 2L to recent),
            attemptCountsBySession = mapOf(1L to 10, 2L to 10),
            nowMillis = now,
        )
        assertTrue(1L in plan.deleteSessionIds)
        assertTrue(2L !in plan.deleteSessionIds)
        assertTrue(plan.recomputeStats)
    }

    @Test
    fun dropsOldestWhenOverAttemptCap() {
        val now = 1_000_000L
        val plan = RetentionPolicy.plan(
            sessionsOldestFirst = listOf(1L to now, 2L to now, 3L to now),
            attemptCountsBySession = mapOf(1L to 900, 2L to 900, 3L to 900),
            nowMillis = now,
            maxAttempts = 1000,
        )
        assertTrue(plan.deleteSessionIds.isNotEmpty())
        assertTrue(1L in plan.deleteSessionIds)
    }
}
