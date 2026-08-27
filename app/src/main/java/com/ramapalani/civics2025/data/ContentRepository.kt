package com.ramapalani.civics2025.data

import android.content.Context
import com.ramapalani.civics2025.domain.CivicsQuestion
import com.ramapalani.civics2025.domain.QuestionBank

class ContentRepository(context: Context) {
    val questions: List<CivicsQuestion> = context.assets.open("questions.json")
        .bufferedReader()
        .use { QuestionBank.parse(it.readText()) }

    fun byId(id: Int): CivicsQuestion? = questions.firstOrNull { it.id == id }

    fun sections(): List<String> = questions.map { it.section }.distinct()
}
