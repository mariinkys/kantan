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

sealed class Screen(val route: String) {
    data object Search : Screen("search")
    data object EntryDetail : Screen("entry/{entryId}") {
        fun createRoute(entryId: Long) = "entry/$entryId"
    }

    data object KanjiDetail : Screen("kanji/{character}") {
        // Kanji characters can be multibyte, URL-encode to survive nav route parsing
        fun createRoute(character: String) =
            "kanji/${URLEncoder.encode(character, "UTF-8")}"
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
                onEntryClick = { entryId ->
                    navController.navigate(Screen.EntryDetail.createRoute(entryId))
                }
            )
        }

        composable(
            route = Screen.EntryDetail.route,
            arguments = listOf(navArgument("entryId") { type = NavType.LongType })
        ) {
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
        ) { backStackEntry ->
            // Decode the URL-encoded character before passing to the VM via SavedStateHandle
            val raw = backStackEntry.arguments?.getString("character") ?: ""
            val character = URLDecoder.decode(raw, "UTF-8")

            // Re-inject decoded value so the VM's SavedStateHandle sees the plain character
            backStackEntry.arguments?.putString("character", character)

            KanjiDetailScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}