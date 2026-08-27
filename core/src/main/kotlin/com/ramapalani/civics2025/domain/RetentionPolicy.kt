package com.ramapalani.civics2025.domain

object RetentionPolicy {
    const val DEFAULT_MAX_AGE_DAYS = 90
    const val DEFAULT_MAX_ATTEMPTS = 2_000
    const val DEFAULT_MAX_BYTES = 2L * 1024 * 1024

    data class CleanupPlan(
        val deleteSessionIds: List<Long>,
        val recomputeStats: Boolean,
    )

    fun plan(
        sessionsOldestFirst: List<Pair<Long, Long>>,
        attemptCountsBySession: Map<Long, Int>,
        nowMillis: Long,
        maxAgeDays: Int = DEFAULT_MAX_AGE_DAYS,
        maxAttempts: Int = DEFAULT_MAX_ATTEMPTS,
        estimatedBytes: Long = 0L,
        maxBytes: Long = DEFAULT_MAX_BYTES,
    ): CleanupPlan {
        val cutoff = nowMillis - maxAgeDays * 24L * 60 * 60 * 1000
        val doomed = linkedSetOf<Long>()
        sessionsOldestFirst.forEach { (id, createdAt) ->
            if (createdAt < cutoff) doomed += id
        }

        var attempts = attemptCountsBySession.values.sum() -
            doomed.sumOf { attemptCountsBySession[it] ?: 0 }
        var bytes = estimatedBytes
        for ((id, _) in sessionsOldestFirst) {
            if (id in doomed) continue
            val overCount = attempts > maxAttempts
            val overSize = bytes > maxBytes && maxBytes > 0
            if (!overCount && !overSize) break
            doomed += id
            attempts -= attemptCountsBySession[id] ?: 0
            bytes = (bytes * 0.9).toLong()
        }
        return CleanupPlan(doomed.toList(), doomed.isNotEmpty())
    }
}
