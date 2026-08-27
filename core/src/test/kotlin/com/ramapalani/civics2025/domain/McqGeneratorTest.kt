package com.ramapalani.civics2025.domain

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertTrue

class McqGeneratorTest {
    @Test
    fun choicesComeFromOfficialPdfAnswers() {
        val raw = requireNotNull(javaClass.classLoader.getResource("questions.json")).readText()
        val bank = QuestionBank.parse(raw)
        val official = OfficialQaParser.parse(
            requireNotNull(javaClass.classLoader.getResource("official-128.txt")).readText(),
        )
        bank.forEach { question ->
            val pdf = official[question.id]?.answers.orEmpty().filter { McqGenerator.usable(it) }
            if (pdf.isEmpty()) return@forEach
            val choices = McqGenerator.choices(question, bank, random = Random(question.id))
            val correctShown = choices.filter { choice ->
                pdf.any { officialAnswer ->
                    AnswerMatcher.normalize(officialAnswer) == AnswerMatcher.normalize(choice)
                }
            }
            assertTrue(correctShown.isNotEmpty(), "Q${question.id} MCQ has no official PDF answer")
            choices.forEach { choice ->
                val inThisPdf = pdf.any { AnswerMatcher.normalize(it) == AnswerMatcher.normalize(choice) }
                val inAnyPdf = official.values.any { extracted ->
                    extracted.answers.any { AnswerMatcher.normalize(it) == AnswerMatcher.normalize(choice) }
                }
                assertTrue(inThisPdf || inAnyPdf, "Q${question.id} MCQ choice is not from the PDF: $choice")
            }
        }
    }
}
