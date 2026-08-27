package com.ramapalani.civics2025.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QuestionBankTest {
    @Test
    fun loadsOfficialBank() {
        val raw = requireNotNull(javaClass.classLoader.getResource("questions.json")).readText()
        val bank = QuestionBank.parse(raw)
        assertEquals(128, bank.size)
        assertEquals(20, bank.count { it.starred6520 })
        assertEquals((1..128).toList(), bank.map { it.id })
        assertTrue(bank.any { it.answerKind == "all_n" })
        assertTrue(bank.any { it.stateSpecific })
        assertTrue(bank.all { it.studyGuidePage in 1..88 })
        assertTrue(bank.all { it.studyGuideTitle.isNotBlank() })
        assertEquals(8, bank.first { it.id == 1 }.studyGuidePage)
        assertEquals(24, bank.first { it.id == 17 }.studyGuidePage)
        assertEquals(18, bank.first { it.id == 16 }.studyGuidePage)
        assertEquals(69, bank.first { it.id == 128 }.studyGuidePage)
        assertEquals("The Judicial Branch", bank.first { it.id == 50 }.studyGuideTitle)
    }
}
