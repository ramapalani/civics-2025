package com.ramapalani.civics2025.data

import com.ramapalani.civics2025.data.db.AttemptEntity
import com.ramapalani.civics2025.data.db.CivicsDao
import com.ramapalani.civics2025.data.db.QuestionStatEntity
import com.ramapalani.civics2025.data.db.SessionEntity
import com.ramapalani.civics2025.domain.GradedAnswer
import com.ramapalani.civics2025.domain.QuestionStat
import com.ramapalani.civics2025.domain.RetentionPolicy
import com.ramapalani.civics2025.domain.ScoreSummary
import com.ramapalani.civics2025.domain.SessionMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ResultsRepository(private val dao: CivicsDao) {
    val sessions: Flow<List<SessionEntity>> = dao.observeSessions()
    val stats: Flow<Map<Int, QuestionStat>> = dao.observeStats().map { list ->
        list.associate { it.questionId to it.toDomain() }
    }

    suspend fun statsMap(): Map<Int, QuestionStat> {
        return dao.allStats().associate { it.questionId to it.toDomain() }
    }

    suspend fun saveSession(
        mode: SessionMode,
        grades: List<GradedAnswer>,
        summary: ScoreSummary,
        starred6520: Boolean,
        interview: Boolean,
        now: Long = System.currentTimeMillis(),
    ): Long {
        val sessionId = dao.insertSession(
            SessionEntity(
                mode = mode.name,
                createdAt = now,
                officialCorrect = summary.officialCorrect,
                officialWrong = summary.officialWrong,
                asked = summary.asked,
                practicePoints = summary.practicePoints,
                passed = summary.passed,
                interviewSimulation = interview,
                starred6520 = starred6520,
            ),
        )
        dao.insertAttempts(
            grades.map { grade ->
                AttemptEntity(
                    sessionId = sessionId,
                    questionId = grade.questionId,
                    mode = mode.name,
                    path = grade.path.name,
                    rawInput = grade.rawInput,
                    correct = grade.correct,
                    practicePoints = grade.practicePoints,
                    createdAt = now,
                )
            },
        )
        grades.forEach { applyGradeToStat(it, now) }
        return sessionId
    }

    suspend fun markReview(questionId: Int, needsReview: Boolean) {
        val current = dao.stat(questionId)?.toDomain() ?: QuestionStat(questionId)
        dao.upsertStat(
            current.copy(needsReview = needsReview).toEntity(),
        )
    }

    suspend fun touchShown(questionId: Int, now: Long = System.currentTimeMillis()) {
        val current = dao.stat(questionId)?.toDomain() ?: QuestionStat(questionId)
        dao.upsertStat(current.copy(lastShownAtMillis = now).toEntity())
    }

    suspend fun enforceRetention(maxAgeDays: Int, maxAttempts: Int, estimatedBytes: Long = 0L) {
        val sessions = dao.sessionsOldestFirst()
        val counts = dao.attemptCounts().associate { it.sessionId to it.cnt }
        val plan = RetentionPolicy.plan(
            sessionsOldestFirst = sessions.map { it.id to it.createdAt },
            attemptCountsBySession = counts,
            nowMillis = System.currentTimeMillis(),
            maxAgeDays = maxAgeDays,
            maxAttempts = maxAttempts,
            estimatedBytes = estimatedBytes,
        )
        if (plan.deleteSessionIds.isEmpty()) return
        dao.deleteSessions(plan.deleteSessionIds)
        if (plan.recomputeStats) recomputeStats()
    }

    private suspend fun recomputeStats() {
        val attempts = dao.allAttempts()
        val rebuilt = attempts.groupBy { it.questionId }.map { (id, rows) ->
            QuestionStatEntity(
                questionId = id,
                seenCount = rows.size,
                correctCount = rows.count { it.correct },
                wrongCount = rows.count { !it.correct },
                needsReview = rows.count { !it.correct } > rows.count { it.correct },
                lastShownAtMillis = rows.maxOf { it.createdAt },
            )
        }
        dao.replaceStats(rebuilt)
    }

    private suspend fun applyGradeToStat(grade: GradedAnswer, now: Long) {
        val current = dao.stat(grade.questionId)?.toDomain() ?: QuestionStat(grade.questionId)
        val next = current.copy(
            seenCount = current.seenCount + 1,
            correctCount = current.correctCount + if (grade.correct) 1 else 0,
            wrongCount = current.wrongCount + if (grade.correct) 0 else 1,
            needsReview = if (grade.correct) current.needsReview else true,
            lastShownAtMillis = now,
        )
        dao.upsertStat(next.toEntity())
    }
}

private fun QuestionStatEntity.toDomain() = QuestionStat(
    questionId = questionId,
    seenCount = seenCount,
    correctCount = correctCount,
    wrongCount = wrongCount,
    needsReview = needsReview,
    lastShownAtMillis = lastShownAtMillis,
)

private fun QuestionStat.toEntity() = QuestionStatEntity(
    questionId = questionId,
    seenCount = seenCount,
    correctCount = correctCount,
    wrongCount = wrongCount,
    needsReview = needsReview,
    lastShownAtMillis = lastShownAtMillis,
)
