package com.ramapalani.civics2025

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ramapalani.civics2025.ui.AppNav
import com.ramapalani.civics2025.ui.theme.CivicsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as CivicsApplication
        val openDaily = intent.getBooleanExtra(EXTRA_OPEN_DAILY, false)
        val questionId = intent.getIntExtra(EXTRA_QUESTION_ID, -1).takeIf { it > 0 }
        setContent {
            CivicsTheme {
                AppNav(app = app, startDaily = openDaily, dailyQuestionId = questionId)
            }
        }
    }

    companion object {
        const val EXTRA_OPEN_DAILY = "open_daily"
        const val EXTRA_QUESTION_ID = "question_id"
    }
}
