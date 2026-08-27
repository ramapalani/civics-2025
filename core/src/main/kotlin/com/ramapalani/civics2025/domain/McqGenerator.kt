package com.ramapalani.civics2025.domain

import kotlin.random.Random

object McqGenerator {
    fun pdfChoices(question: CivicsQuestion): List<String> {
        val source = question.officialAnswers.ifEmpty { question.acceptedAnswers }
        return source.filter { usable(it) }.distinct()
    }

    fun choices(
        question: CivicsQuestion,
        bank: List<CivicsQuestion>,
        officials: LocalOfficials = LocalOfficials(),
        random: Random = Random.Default,
        optionCount: Int = 4,
    ): List<String> {
        val official = pdfChoices(question)
        val extras = AnswerMatcher.acceptedFor(question, officials).filter { usable(it) }
        val correctPool = official.ifEmpty { extras }
        require(correctPool.isNotEmpty()) { "Question ${question.id} has no MCQ answers" }

        val neededCorrect = if (question.kind == AnswerKind.ALL_N) {
            question.minRequired.coerceAtLeast(1).coerceAtMost(correctPool.size)
        } else {
            1
        }
        val correct = correctPool.shuffled(random).take(neededCorrect)

        val officialNorms = official.map { AnswerMatcher.normalize(it) }.toSet()
        val distractors = bank
            .filter { it.id != question.id }
            .flatMap { pdfChoices(it) }
            .filter { candidate ->
                val norm = AnswerMatcher.normalize(candidate)
                usable(candidate) &&
                    norm !in officialNorms &&
                    correct.none { AnswerMatcher.normalize(it) == norm }
            }
            .distinctBy { AnswerMatcher.normalize(it) }
            .shuffled(random)
            .take((optionCount - correct.size).coerceAtLeast(0))

        return (correct + distractors).shuffled(random)
    }

    fun usable(answer: String): Boolean {
        val text = answer.trim()
        if (text.isEmpty()) return false
        if (text.startsWith("Answers will vary", ignoreCase = true)) return false
        if (text.startsWith("Visit uscis.gov", ignoreCase = true)) return false
        return true
    }
}
