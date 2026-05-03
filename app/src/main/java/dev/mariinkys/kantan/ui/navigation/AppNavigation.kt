package dev.mariinkys.kantan.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.mariinkys.kantan.ui.entry.EntryDetailScreen
import dev.mariinkys.kantan.ui.kanji.KanjiDetailScreen
import dev.mariinkys.kantan.ui.search.SearchScreen
import java.net.URLDecoder
import java.net.URLEncoder

private fun String.enc() = URLEncoder.encode(this, "UTF-8")
private fun String.dec() = URLDecoder.decode(this, "UTF-8")

sealed class Screen(val route: String) {
    data object Search : Screen("search")

    data object EntryDetail : Screen("entry/{expression}/{reading}") {
        fun createRoute(expression: String, reading: String) =
            "entry/${expression.enc()}/${reading.enc()}"
    }

    data object KanjiDetail : Screen("kanji/{character}") {
        fun createRoute(character: String) = "kanji/${character.enc()}"
    }
}

@Composable
fun AppNavigation(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Search.route,
        modifier = modifier
    ) {
        composable(Screen.Search.route) {
            SearchScreen(
                onEntryClick = { expression, reading ->
                    navController.navigate(Screen.EntryDetail.createRoute(expression, reading))
                }
            )
        }

        composable(
            route = Screen.EntryDetail.route,
            arguments = listOf(
                navArgument("expression") { type = NavType.StringType },
                navArgument("reading") { type = NavType.StringType }
            )
        ) { backStack ->
            val expression = backStack.arguments?.getString("expression")?.dec() ?: ""
            val reading = backStack.arguments?.getString("reading")?.dec() ?: ""
            backStack.arguments?.putString("expression", expression)
            backStack.arguments?.putString("reading", reading)

            EntryDetailScreen(
                onBack = { navController.popBackStack() },
                onKanjiClick = { character ->
                    navController.navigate(Screen.KanjiDetail.createRoute(character))
                }
            )
        }

        composable(
            route = Screen.KanjiDetail.route,
            arguments = listOf(navArgument("character") { type = NavType.StringType })
        ) { backStack ->
            val character = backStack.arguments?.getString("character")?.dec() ?: ""
            backStack.arguments?.putString("character", character)

            KanjiDetailScreen(onBack = { navController.popBackStack() })
        }
    }
}