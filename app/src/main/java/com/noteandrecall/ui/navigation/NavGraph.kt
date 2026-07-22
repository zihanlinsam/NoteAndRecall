package com.noteandrecall.ui.navigation

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.noteandrecall.data.KnowledgeDao
import com.noteandrecall.data.PreferencesManager
import com.noteandrecall.ui.screens.*

@Composable
fun MainNavGraph(
    dao: KnowledgeDao,
    prefsManager: PreferencesManager,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen(navController = navController, prefsManager = prefsManager, dao = dao)
        }
        composable(
            "note",
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) }
        ) {
            NoteScreen(navController = navController, dao = dao, prefsManager = prefsManager, mode = NoteMode.MANUAL)
        }
        composable(
            "note_auto",
            enterTransition = { slideInHorizontally(initialOffsetX = { -it }) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { -it }) }
        ) {
            NoteScreen(navController = navController, dao = dao, prefsManager = prefsManager, mode = NoteMode.AUTO)
        }
        composable(
            route = "note_edit?title={title}&tags={tags}&content={content}",
            arguments = listOf(
                navArgument("title") { type = NavType.StringType; defaultValue = "" },
                navArgument("tags") { type = NavType.StringType; defaultValue = "" },
                navArgument("content") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val t = backStackEntry.arguments?.getString("title")?.let { java.net.URLDecoder.decode(it, "UTF-8") } ?: ""
            val tg = backStackEntry.arguments?.getString("tags")?.let { java.net.URLDecoder.decode(it, "UTF-8") } ?: ""
            val c = backStackEntry.arguments?.getString("content")?.let { java.net.URLDecoder.decode(it, "UTF-8") } ?: ""
            NoteScreen(navController = navController, dao = dao, prefsManager = prefsManager, mode = NoteMode.AUTO_EDIT, initialTitle = t, initialTags = tg, initialContent = c)
        }
        composable("recall") {
            RecallScreen(navController = navController, dao = dao, prefsManager = prefsManager)
        }
        composable("knowledge_list") {
            KnowledgeListScreen(navController = navController, dao = dao)
        }
        composable(
            route = "detail/{itemId}",
            arguments = listOf(navArgument("itemId") { type = NavType.LongType })
        ) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getLong("itemId") ?: 0L
            KnowledgeDetailScreen(navController = navController, dao = dao, prefsManager = prefsManager, itemId = itemId)
        }
        composable("ai_config") {
            AiConfigScreen(navController = navController, prefsManager = prefsManager)
        }
        composable("import_export") {
            ImportExportScreen(navController = navController, dao = dao)
        }
        composable("score_settings") {
            ScoreSettingsScreen(navController = navController, prefsManager = prefsManager)
        }
        composable("log") {
            LogScreen(navController = navController)
        }
        composable("help") {
            HelpScreen(navController = navController)
        }
        composable("about") {
            AboutScreen(navController = navController)
        }
        composable("graph") {
            GraphScreen(navController = navController, dao = dao)
        }
    }
}
