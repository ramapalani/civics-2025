package com.ramapalani.civics2025.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CivicsDao {
    @Insert
    suspend fun insertSession(session: SessionEntity): Long

    @Insert
    suspend fun insertAttempts(attempts: List<AttemptEntity>)

    @Upsert
    suspend fun upsertStat(stat: QuestionStatEntity)

    @Query("SELECT * FROM question_stats")
    suspend fun allStats(): List<QuestionStatEntity>

    @Query("SELECT * FROM question_stats")
    fun observeStats(): Flow<List<QuestionStatEntity>>

    @Query("SELECT * FROM question_stats WHERE questionId = :id")
    suspend fun stat(id: Int): QuestionStatEntity?

    @Query("SELECT * FROM sessions ORDER BY createdAt DESC")
    fun observeSessions(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions ORDER BY createdAt ASC")
    suspend fun sessionsOldestFirst(): List<SessionEntity>

    @Query("SELECT sessionId AS sessionId, COUNT(*) AS cnt FROM attempts GROUP BY sessionId")
    suspend fun attemptCounts(): List<SessionAttemptCount>

    @Query("SELECT * FROM attempts")
    suspend fun allAttempts(): List<AttemptEntity>

    @Query("DELETE FROM sessions WHERE id IN (:ids)")
    suspend fun deleteSessions(ids: List<Long>)

    @Query("DELETE FROM question_stats")
    suspend fun clearStats()

    @Transaction
    suspend fun replaceStats(stats: List<QuestionStatEntity>) {
        clearStats()
        stats.forEach { upsertStat(it) }
    }
}

data class SessionAttemptCount(
    val sessionId: Long,
    val cnt: Int,
)
