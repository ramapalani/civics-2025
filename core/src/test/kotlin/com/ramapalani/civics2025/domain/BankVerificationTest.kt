package com.ramapalani.civics2025.domain

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

class BankVerificationTest {
    private val bank = QuestionBank.parse(
        requireNotNull(javaClass.classLoader.getResource("questions.json")).readText(),
    )
    private val officials = LocalOfficials(
        senator = "Alex Padilla",
        representative = "Nancy Pelosi",
        governor = "Gavin Newsom",
        stateCapital = "Sacramento",
    )

    @Test
    fun acceptsSpokenVariantsForEveryQuestion() {
        val failures = mutableListOf<String>()
        bank.forEach { question ->
            val accepted = AnswerMatcher.acceptedFor(question, officials)
                .filter { !it.startsWith("Answers will vary", ignoreCase = true) }
            if (accepted.isEmpty()) return@forEach

            accepted.forEach { answer ->
                spokenVariants(answer).forEach { variant ->
                    val probe = question.copy(answerKind = "any_one", minRequired = 1)
                    if (!AnswerMatcher.match(probe, variant, officials).correct) {
                        failures += "Q${question.id} rejected variant \"$variant\" of \"$answer\""
                    }
                }
            }

            if (question.kind == AnswerKind.ALL_N) {
                val picks = distinctAnswers(question, accepted)
                if (picks.size >= question.minRequired) {
                    val chosen = picks.take(question.minRequired)
                    listOf(
                        chosen.joinToString("\n"),
                        chosen.joinToString(", "),
                        chosen.joinToString(" and "),
                        chosen.joinToString(" "),
                    ).forEach { combo ->
                        if (!AnswerMatcher.match(question, combo, officials).correct) {
                            failures += "Q${question.id} rejected multi-answer \"$combo\""
                        }
                    }
                }
            }
        }
        if (failures.isNotEmpty()) {
            fail("${failures.size} verification gaps:\n${failures.joinToString("\n")}")
        }
    }

    @Test
    fun rejectsUnrelatedAnswers() {
        val decoys = listOf(
            "pizza",
            "the moon is cheese",
            "King Henry VIII",
            "Canada",
        )
        val failures = mutableListOf<String>()
        bank.forEach { question ->
            if (question.stateSpecific) return@forEach
            decoys.forEach { decoy ->
                if (AnswerMatcher.match(question, decoy, officials).correct) {
                    failures += "Q${question.id} accepted decoy \"$decoy\""
                }
            }
        }
        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }

    private fun distinctAnswers(question: CivicsQuestion, accepted: List<String>): List<String> {
        val source = question.officialAnswers.ifEmpty { accepted }
        val unique = mutableListOf<String>()
        for (answer in source) {
            val probe = question.copy(answerKind = "any_one", minRequired = 1)
            if (unique.none { existing ->
                    AnswerMatcher.match(probe, existing, officials).correct &&
                        AnswerMatcher.match(probe.copy(acceptedAnswers = listOf(answer)), existing, officials).correct
                }
            ) {
                unique += answer
            }
        }
        return unique.ifEmpty { accepted.take(question.minRequired) }
    }

    private fun spokenVariants(answer: String): List<String> {
        val variants = mutableListOf(answer, answer.lowercase())
        val noParens = answer.replace(Regex("""\([^)]*\)"""), " ").replace(Regex("\\s+"), " ").trim()
        if (noParens.isNotBlank()) variants += noParens
        Regex("""\(([^)]*)\)""").findAll(answer).forEach { variants += it.groupValues[1] }
        val words = noParens.split(Regex("\\s+")).filter { it.isNotBlank() }
        val last = words.lastOrNull()?.replace(Regex("[^A-Za-z]"), "").orEmpty()
        if (looksLikePersonName(noParens, words) && last.length >= 4) {
            variants += last
        }
        return variants.distinct().filter { it.isNotBlank() && it.length > 1 }
    }

    private fun looksLikePersonName(answer: String, words: List<String>): Boolean {
        if (words.size !in 2..4 || answer.length > 40) return false
        if (answer.contains(" of ", ignoreCase = true)) return false
        val blocked = setOf(
            "day", "war", "court", "states", "america", "government", "constitution",
            "president", "acts", "hill", "york", "city", "island", "jersey", "hampshire",
            "carolina", "general", "secretary", "department", "papers", "amendment",
        )
        if (words.any { it.lowercase().replace(Regex("[^a-z]"), "") in blocked }) return false
        return words.all { token ->
            token.first().isUpperCase() || token.endsWith(".") || token.length == 1
        }
    }
}
