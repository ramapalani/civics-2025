package com.ramapalani.civics2025

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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
        // A typed answer also reports how closely it matched, so the verdict carries a percentage.
        composeRule.onNodeWithTag("gradeResult").assertTextContains("% match", substring = true)

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
     * The first Learn question is "What is the form of government of the United States?", whose
     * accepted answers include "Republic". Typing it with a typo must still grade as correct, and
     * the card must report full confidence — the matcher normalizes before scoring, so a one-letter
     * slip is not a partial match.
     */
    @Test
    fun learnMode_typedAnswer_showsConfidenceForRightAndWrongAnswers() {
        composeRule.onNodeWithTag("learnModeButton").performClick()
        composeRule.onNodeWithTag("questionText").assertTextContains("form of government", substring = true)

        composeRule.onNodeWithTag("answerInput").performTextInput("republik")
        composeRule.onNodeWithTag("checkButton").performClick()
        composeRule.onNodeWithTag("gradeResult").assertTextContains("Correct!", substring = true)
        composeRule.onNodeWithTag("gradeResult").assertTextContains("100% match", substring = true)

        // Question 2 is "What is the supreme law of the land?". An unrelated answer scores zero
        // rather than landing in a middle band that would read as nearly right.
        composeRule.onNodeWithTag("nextButton").performClick()
        composeRule.onNodeWithTag("answerInput").performTextInput("pizza")
        composeRule.onNodeWithTag("checkButton").performClick()
        composeRule.onNodeWithTag("gradeResult").assertTextContains("Not quite.", substring = true)
        composeRule.onNodeWithTag("gradeResult").assertTextContains("0% match", substring = true)
        // Nothing about "pizza" is close, so the card must not soften the verdict with a nudge.
        composeRule.onNodeWithTag("nearMissHint").assertDoesNotExist()
    }

    /**
     * After grading, the result card must keep the USCIS-published answers apart from the extra
     * variants the matcher also accepts. Learn mode starts at Q1 on a fresh install, which has
     * both: "Republic" is official, "Federal republic" is an extra.
     */
    @Test
    fun learnMode_afterGrading_separatesOfficialFromOtherAcceptedAnswers() {
        composeRule.onNodeWithTag("learnModeButton").performClick()
        composeRule.onNodeWithTag("answerInput").performTextInput("test answer")
        composeRule.onNodeWithTag("checkButton").performClick()

        composeRule.onNodeWithText("Official answers (USCIS)").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("• Republic").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Also accepted").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("• Federal republic").performScrollTo().assertIsDisplayed()

        // Q1 has a fixed official list, so the combined single-list heading must not appear.
        composeRule.onNodeWithText("Accepted answers").assertDoesNotExist()
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
