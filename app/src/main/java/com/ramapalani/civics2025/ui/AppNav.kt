package com.ramapalani.civics2025.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ramapalani.civics2025.CivicsApplication
import com.ramapalani.civics2025.domain.SessionMode
import com.ramapalani.civics2025.ui.guide.StudyGuideScreen
import com.ramapalani.civics2025.ui.history.HistoryScreen
import com.ramapalani.civics2025.ui.home.HomeScreen
import com.ramapalani.civics2025.ui.quiz.QuestionScreen
import com.ramapalani.civics2025.ui.quiz.QuizViewModel
import com.ramapalani.civics2025.ui.about.AboutLegalScreen
import com.ramapalani.civics2025.ui.settings.SettingsScreen
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Composable
fun AppNav(app: CivicsApplication, startDaily: Boolean, dailyQuestionId: Int?) {
    val nav = rememberNavController()
    val start = if (startDaily) {
        val id = dailyQuestionId ?: -1
        "daily/$id"
    } else {
        "home"
    }
    Scaffold(modifier = Modifier.fillMaxSize()) { inner ->
    NavHost(navController = nav, startDestination = start, modifier = Modifier.padding(inner)) {
        composable("home") {
            HomeScreen(
                app = app,
                onLearn = { section ->
                    val encoded = URLEncoder.encode(section ?: "ALL", StandardCharsets.UTF_8.name())
                    nav.navigate("learn/$encoded")
                },
                onTest = { nav.navigate("test") },
                onDaily = { nav.navigate("daily/-1") },
                onHistory = { nav.navigate("history") },
                onSettings = { nav.navigate("settings") },
                onAbout = { nav.navigate("about") },
                onTextbook = { nav.navigate("guide/7/${URLEncoder.encode("Table of Contents", StandardCharsets.UTF_8.name())}") },
            )
        }
        composable(
            "learn/{section}",
            arguments = listOf(navArgument("section") { type = NavType.StringType }),
        ) { entry ->
            val raw = entry.arguments?.getString("section") ?: "ALL"
            val section = URLDecoder.decode(raw, StandardCharsets.UTF_8.name()).let { if (it == "ALL") null else it }
            val vm: QuizViewModel = viewModel(
                factory = QuizViewModel.factory(app, SessionMode.LEARN, section),
            )
            QuestionScreen(
                viewModel = vm,
                onExit = { nav.popBackStack() },
                onOpenGuide = { page, title ->
                    nav.navigate("guide/$page/${URLEncoder.encode(title, StandardCharsets.UTF_8.name())}")
                },
            )
        }
        composable("test") {
            val vm: QuizViewModel = viewModel(factory = QuizViewModel.factory(app, SessionMode.TEST))
            QuestionScreen(
                viewModel = vm,
                onExit = { nav.popBackStack() },
                onOpenGuide = { page, title ->
                    nav.navigate("guide/$page/${URLEncoder.encode(title, StandardCharsets.UTF_8.name())}")
                },
            )
        }
        composable(
            "daily/{id}",
            arguments = listOf(navArgument("id") { type = NavType.IntType }),
        ) { entry ->
            val id = entry.arguments?.getInt("id")?.takeIf { it > 0 }
            val vm: QuizViewModel = viewModel(
                factory = QuizViewModel.factory(app, SessionMode.DAILY, dailyQuestionId = id),
            )
            QuestionScreen(
                viewModel = vm,
                onExit = {
                    if (!nav.popBackStack()) nav.navigate("home") { popUpTo("daily/${id ?: -1}") { inclusive = true } }
                },
                onOpenGuide = { page, title ->
                    nav.navigate("guide/$page/${URLEncoder.encode(title, StandardCharsets.UTF_8.name())}")
                },
            )
        }
        composable("history") { HistoryScreen(app) { nav.popBackStack() } }
        composable("settings") {
            SettingsScreen(
                app = app,
                onBack = { nav.popBackStack() },
                onAbout = { nav.navigate("about") },
            )
        }
        composable("about") { AboutLegalScreen { nav.popBackStack() } }
        composable(
            "guide/{page}/{title}",
            arguments = listOf(
                navArgument("page") { type = NavType.IntType },
                navArgument("title") { type = NavType.StringType },
            ),
        ) { entry ->
            val page = entry.arguments?.getInt("page") ?: 7
            val title = URLDecoder.decode(entry.arguments?.getString("title") ?: "Textbook", StandardCharsets.UTF_8.name())
            val fromQuestion = nav.previousBackStackEntry?.destination?.route.orEmpty().let { route ->
                route.startsWith("learn") || route == "test" || route.startsWith("daily")
            }
            StudyGuideScreen(
                startPage = page,
                title = title,
                showQuestionButton = fromQuestion,
                onBackToQuestion = { nav.popBackStack() },
                onHome = {
                    nav.navigate("home") {
                        popUpTo("home") { inclusive = false }
                        launchSingleTop = true
                    }
                },
            )
        }
    }
    }
}
