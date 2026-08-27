package com.ramapalani.civics2025.domain

import kotlinx.serialization.json.Json

object QuestionBank {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(raw: String): List<CivicsQuestion> {
        return json.decodeFromString<List<CivicsQuestion>>(raw).sortedBy { it.id }
    }
}
