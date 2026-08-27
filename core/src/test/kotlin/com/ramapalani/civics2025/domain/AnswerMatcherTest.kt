package com.ramapalani.civics2025.domain

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AnswerMatcherTest {
    private val constitution = CivicsQuestion(
        id = 2,
        section = "AMERICAN GOVERNMENT",
        topic = "Principles of American Government",
        question = "What is the supreme law of the land?",
        acceptedAnswers = listOf("The Constitution", "U.S. Constitution", "Constitution"),
        extraInfo = "test",
        starred6520 = true,
    )

    private val twoIdeas = CivicsQuestion(
        id = 10,
        section = "AMERICAN GOVERNMENT",
        topic = "Principles of American Government",
        question = "Name two important ideas",
        acceptedAnswers = listOf("Equality", "Liberty", "Social contract", "Natural rights"),
        extraInfo = "test",
        answerKind = "all_n",
        minRequired = 2,
    )

    private val president = CivicsQuestion(
        id = 38,
        section = "AMERICAN GOVERNMENT",
        topic = "System of Government",
        question = "What is the name of the President of the United States now?",
        acceptedAnswers = listOf("Donald J. Trump", "Donald Trump", "Trump"),
        extraInfo = "test",
        currentOfficial = true,
        stateField = "president",
    )

    private val senator = CivicsQuestion(
        id = 23,
        section = "AMERICAN GOVERNMENT",
        topic = "System of Government",
        question = "Who is one of your state’s U.S. senators now?",
        acceptedAnswers = listOf("Answers will vary"),
        extraInfo = "test",
        stateSpecific = true,
        stateField = "senator",
    )

    @Test
    fun exactAndAliasMatch() {
        assertTrue(AnswerMatcher.match(constitution, "the constitution").correct)
        assertTrue(AnswerMatcher.match(constitution, "U.S. Constitution").correct)
        assertTrue(AnswerMatcher.match(constitution, "us constitution").correct)
    }

    @Test
    fun spellingTolerance() {
        assertTrue(AnswerMatcher.match(constitution, "constitusion").correct)
        assertTrue(AnswerMatcher.match(constitution, "the constituttion").correct)
    }

    @Test
    fun rejectsUnrelatedAnswer() {
        assertFalse(AnswerMatcher.match(constitution, "Declaration of Independence").correct)
        assertFalse(AnswerMatcher.match(president, "Joe Biden").correct)
        assertFalse(AnswerMatcher.match(president, "Biden").correct)
    }

    @Test
    fun multiAnswerRequiresTwo() {
        assertFalse(AnswerMatcher.match(twoIdeas, "liberty").correct)
        assertTrue(AnswerMatcher.match(twoIdeas, "liberty and equality").correct)
        assertTrue(AnswerMatcher.match(twoIdeas, "natural rights, liberty").correct)
    }

    @Test
    fun originalStatesSpaceSeparated() {
        val raw = requireNotNull(javaClass.classLoader.getResource("questions.json")).readText()
        val q81 = QuestionBank.parse(raw).first { it.id == 81 }
        val combo = "New Hampshire Massachusetts Rhode Island Connecticut New York"
        val result = AnswerMatcher.match(q81, combo)
        assertTrue(result.correct, "matched=${result.matchedAnswers}")
    }

    @Test
    fun holidaysFromNewlinesAndShortNames() {
        val holidays = CivicsQuestion(
            id = 126,
            section = "SYMBOLS AND HOLIDAYS",
            topic = "Holidays",
            question = "Name three national U.S. holidays.",
            acceptedAnswers = listOf(
                "New Year's Day",
                "Martin Luther King, Jr. Day",
                "Memorial Day",
                "Independence Day",
                "Labor Day",
                "Veterans Day",
                "Thanksgiving Day",
                "Thanksgiving",
                "Christmas Day",
                "Christmas",
            ),
            extraInfo = "test",
            answerKind = "all_n",
            minRequired = 3,
        )
        val typed = """
            independence day
            thanksgiving
            memorial day
        """.trimIndent()
        assertTrue(AnswerMatcher.match(holidays, typed).correct)
        assertTrue(AnswerMatcher.match(holidays, "independence day thanksgiving memorial day").correct)
        assertTrue(AnswerMatcher.match(holidays, "july 4, thanksgiving, memorial day").correct)
        assertFalse(AnswerMatcher.match(holidays, "thanksgiving, thanksgiving day, turkey day").correct)
        assertFalse(AnswerMatcher.match(holidays, "day day day").correct)
    }

    @Test
    fun usesLocalOfficials() {
        val officials = LocalOfficials(senator = "Alex Padilla")
        assertTrue(AnswerMatcher.match(senator, "Alex Padilla", officials).correct)
        assertFalse(AnswerMatcher.match(senator, "Alex Padilla", LocalOfficials()).correct)
    }

    @Test
    fun federalOfficialsDefaultThenOverride() {
        assertTrue(AnswerMatcher.match(president, "Donald Trump").correct)
        assertTrue(AnswerMatcher.match(president, "Trump").correct)
        val updated = LocalOfficials(president = "Jane Doe")
        assertTrue(AnswerMatcher.match(president, "Jane Doe", updated).correct)
        assertTrue(AnswerMatcher.match(president, "Doe", updated).correct)
        assertFalse(AnswerMatcher.match(president, "Donald Trump", updated).correct)
        assertFalse(AnswerMatcher.match(president, "Trump", updated).correct)
    }
}
