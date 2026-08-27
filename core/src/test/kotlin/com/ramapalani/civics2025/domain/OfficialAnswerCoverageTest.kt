package com.ramapalani.civics2025.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class OfficialAnswerCoverageTest {
    @Test
    fun everyOfficialAnswerIsAccepted() {
        val bank = QuestionBank.parse(
            requireNotNull(javaClass.classLoader.getResource("questions.json")).readText(),
        ).associateBy { it.id }
        val official = OfficialQaParser.parse(
            requireNotNull(javaClass.classLoader.getResource("official-128.txt")).readText(),
        )
        assertEquals(128, official.size, "PDF parser should find 128 official questions")

        val failures = mutableListOf<String>()
        for ((id, extracted) in official) {
            val question = bank[id]
            if (question == null) {
                failures += "Q$id missing from questions.json"
                continue
            }
            if (extracted.answers.isEmpty()) {
                failures += "Q$id has no official answers parsed"
                continue
            }
            extracted.answers.forEach { officialAnswer ->
                when {
                    officialAnswer.startsWith("Visit uscis.gov", ignoreCase = true) -> {
                        if (!question.currentOfficial) {
                            failures += "Q$id official answer points to test updates but currentOfficial is false"
                        }
                        if (question.acceptedAnswers.isEmpty()) {
                            failures += "Q$id needs at least one concrete accepted official name"
                        }
                    }
                    officialAnswer.startsWith("Answers will vary", ignoreCase = true) -> {
                        if (!question.stateSpecific) {
                            failures += "Q$id official answer varies by state but stateSpecific is false"
                        }
                    }
                    else -> {
                        val probe = question.copy(answerKind = "any_one", minRequired = 1)
                        val result = AnswerMatcher.match(probe, officialAnswer)
                        if (!result.correct) {
                            failures += "Q$id rejected official answer: \"$officialAnswer\" (best=${"%.2f".format(result.similarity)})"
                        }
                    }
                }
            }
        }
        if (failures.isNotEmpty()) {
            fail("${failures.size} official-answer gaps:\n${failures.joinToString("\n")}")
        }
    }

    @Test
    fun matcherAcceptsEachBankAnswer() {
        val bank = QuestionBank.parse(
            requireNotNull(javaClass.classLoader.getResource("questions.json")).readText(),
        )
        val officials = LocalOfficials(
            senator = "Alex Padilla",
            representative = "Nancy Pelosi",
            governor = "Gavin Newsom",
            stateCapital = "Sacramento",
        )
        val failures = mutableListOf<String>()
        bank.forEach { question ->
            val answers = AnswerMatcher.acceptedFor(question, officials)
            answers.forEach { answer ->
                if (answer.startsWith("Answers will vary", ignoreCase = true)) return@forEach
                val probe = question.copy(answerKind = "any_one", minRequired = 1)
                if (!AnswerMatcher.match(probe, answer, officials).correct) {
                    failures += "Q${question.id} does not accept its own answer: \"$answer\""
                }
            }
        }
        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }
}

data class OfficialQuestion(
    val id: Int,
    val question: String,
    val answers: List<String>,
)

object OfficialQaParser {
    private val questionStart = Regex("""^(\d{1,3})\.\s+(.+)$""")
    private val bullet = Regex("""^[•·]\s*(.+)$""")
    private val skip = Regex(
        """^(M-1778|AMERICAN |SYMBOLS |A: |B: |C: |\d+ of 19|uscis\.gov|For a complete list|Listed below|These questions|test is an oral|questions\. You must|of the civics test|On the civics|uscis\.gov/citizenship|test\. You must|naturalization interview|Although USCIS|applicants are encouraged|65/20|If you are 65|resident of the|have been marked|the civics test in|the 20 civics|60%\) correctly|$)""",
        RegexOption.IGNORE_CASE,
    )

    fun parse(raw: String): Map<Int, OfficialQuestion> {
        val found = linkedMapOf<Int, Pair<StringBuilder, MutableList<String>>>()
        var currentId: Int? = null
        var lastWasAnswer = false
        raw.replace("\u000c", "\n").lines().forEach { original ->
            val line = original.trim().trimStart('*', ' ').replace(Regex("""\s+\*$"""), "").trim()
            if (line.isEmpty() || skip.containsMatchIn(line) || line == "*") return@forEach
            val qMatch = questionStart.find(line)
            if (qMatch != null) {
                currentId = qMatch.groupValues[1].toInt()
                found[currentId!!] = StringBuilder(qMatch.groupValues[2].trim()) to mutableListOf()
                lastWasAnswer = false
                return@forEach
            }
            val id = currentId ?: return@forEach
            val bMatch = bullet.find(line)
            if (bMatch != null) {
                found[id]!!.second += bMatch.groupValues[1].trim()
                lastWasAnswer = true
                return@forEach
            }
            val entry = found[id] ?: return@forEach
            if (lastWasAnswer && entry.second.isNotEmpty()) {
                val last = entry.second.removeLast()
                entry.second += "$last ${line.trim()}"
            } else if (!lastWasAnswer) {
                entry.first.append(' ').append(line)
            }
        }
        return found.mapValues { (id, pair) ->
            OfficialQuestion(
                id = id,
                question = pair.first.toString(),
                answers = pair.second.map { it.replace(Regex("\\s+"), " ").trim() }.filter { it.isNotBlank() },
            )
        }
    }
}
