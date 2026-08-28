package com.ramapalani.civics2025

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeScreenNavigationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun homeScreen_showsCoreEntryPoints() {
        composeRule.onNodeWithText("Civics 2025").assertIsDisplayed()
        composeRule.onNodeWithTag("learnModeButton").assertIsDisplayed()
        composeRule.onNodeWithTag("testModeButton").assertIsDisplayed()
        composeRule.onNodeWithTag("dailyQuestionButton").assertIsDisplayed()
    }

    @Test
    fun tappingLearnMode_opensQuestionScreen() {
        composeRule.onNodeWithTag("learnModeButton").performClick()

        // The question screen is open when a question and its answer input are shown.
        composeRule.onNodeWithTag("questionText").assertIsDisplayed()
        composeRule.onNodeWithTag("answerInput").assertIsDisplayed()
    }

    @Test
    fun exitingLearnMode_returnsHome() {
        composeRule.onNodeWithTag("learnModeButton").performClick()
        composeRule.onNodeWithTag("questionText").assertIsDisplayed()

        composeRule.onNodeWithTag("exitButton").performClick()

        // Back on home: the title and the entry-point buttons are shown again.
        composeRule.onNodeWithText("Civics 2025").assertIsDisplayed()
        composeRule.onNodeWithTag("learnModeButton").assertIsDisplayed()
        composeRule.onNodeWithTag("questionText").assertDoesNotExist()
    }

    @Test
    fun tappingHistory_opensHistoryScreen_andBackReturnsHome() {
        composeRule.onNodeWithTag("historyButton").performClick()
        composeRule.onNodeWithText("Back").assertIsDisplayed()

        composeRule.onNodeWithText("Back").performClick()
        composeRule.onNodeWithTag("historyButton").assertIsDisplayed()
    }

    @Test
    fun tappingSettings_opensSettingsScreen_andBackReturnsHome() {
        composeRule.onNodeWithTag("settingsButton").performClick()
        // "Settings" also labels the home button, so anchor on the Settings screen's
        // own Back control to confirm we navigated.
        composeRule.onNodeWithText("Back").assertIsDisplayed()

        composeRule.onNodeWithText("Back").performClick()
        composeRule.onNodeWithTag("settingsButton").assertIsDisplayed()
    }
}
