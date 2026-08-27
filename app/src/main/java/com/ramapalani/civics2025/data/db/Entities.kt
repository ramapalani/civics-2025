package com.ramapalani.civics2025.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mode: String,
    val createdAt: Long,
    val officialCorrect: Int,
    val officialWrong: Int,
    val asked: Int,
    val practicePoints: Double,
    val passed: Boolean,
    val interviewSimulation: Boolean,
    val starred6520: Boolean,
)

@Entity(
    tableName = "attempts",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("sessionId"), Index("questionId")],
)
data class AttemptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val questionId: Int,
    val mode: String,
    val path: String,
    val rawInput: String,
    val correct: Boolean,
    val practicePoints: Double,
    val createdAt: Long,
)

@Entity(tableName = "question_stats")
data class QuestionStatEntity(
    @PrimaryKey val questionId: Int,
    val seenCount: Int,
    val correctCount: Int,
    val wrongCount: Int,
    val needsReview: Boolean,
    val lastShownAtMillis: Long?,
)
