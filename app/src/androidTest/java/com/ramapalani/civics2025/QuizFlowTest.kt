package com.ramapalani.civics2025

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QuizFlowTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    /**
     * Learn mode loads the full question bank, so the first question is never the
     * last one — clicking Next must move from question 1 to question 2. We assert on
     * the "Learn · 1 / N" progress label rather than just "questionText still exists",
     * because the latter would also pass if the app never advanced.
     */
    @Test
    fun learnMode_typedAnswer_gradesThenAdvancesToNextQuestion() {
        composeRule.onNodeWithTag("learnModeButton").performClick()
        composeRule.onNodeWithTag("questionText").assertIsDisplayed()
        composeRule.onNodeWithTag("progressLabel").assertTextContains("1 /", substring = true)

        composeRule.onNodeWithTag("answerInput").performTextInput("test answer")
        composeRule.onNodeWithTag("checkButton").performClick()

        // Submitting must produce a graded result: either "Correct!" or "Not quite."
        composeRule.onNodeWithTag("gradeResult").assertIsDisplayed()

        // The Next button is labelled "Next" (not "Finish") because we're not on the
        // last question — clicking it advances to question 2.
        composeRule.onNodeWithTag("nextButton").assertTextContains("Next", substring = true)
        composeRule.onNodeWithTag("nextButton").performClick()

        // Proof of real advancement: the progress label moves to question 2, and the
        // input is cleared (a fresh, ungraded question is shown).
        composeRule.onNodeWithTag("progressLabel").assertTextContains("2 /", substring = true)
        composeRule.onNodeWithTag("questionText").assertIsDisplayed()
        composeRule.onNodeWithTag("gradeResult").assertDoesNotExist()
    }

    /**
     * Daily mode loads exactly one question, so Exit is the only way out. We assert
     * that exiting actually lands back on the home screen rather than merely that the
     * Exit button was clickable.
     */
    @Test
    fun dailyQuestion_exit_returnsToHome() {
        composeRule.onNodeWithTag("dailyQuestionButton").performClick()
        composeRule.onNodeWithTag("questionText").assertIsDisplayed()

        composeRule.onNodeWithTag("exitButton").performClick()

        composeRule.onNodeWithText("Civics 2025").assertIsDisplayed()
        composeRule.onNodeWithTag("dailyQuestionButton").assertIsDisplayed()
    }
}
