package dev.mariinkys.kantan.ui.navigation

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.mariinkys.kantan.R
import dev.mariinkys.kantan.ui.AboutScreen
import dev.mariinkys.kantan.ui.entry.EntryDetailScreen
import dev.mariinkys.kantan.ui.kanji.KanjiDetailScreen
import dev.mariinkys.kantan.ui.search.SearchScreen
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.net.URLEncoder

private fun String.enc() = URLEncoder.encode(this, "UTF-8")
private fun String.dec() = URLDecoder.decode(this, "UTF-8")

sealed class Screen(val route: String) {
    data object Search : Screen("search")

    data object About : Screen("about")

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
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 28.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                        contentDescription = "App Logo",
                        modifier = Modifier.size(56.dp),
                        tint = Color.Unspecified
                    )
                    Text(
                        text = "Kantan",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp))

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("Home") },
                    selected = currentRoute == Screen.Search.route,
                    onClick = {
                        navController.navigate(Screen.Search.route) {
                            popUpTo(Screen.Search.route) { inclusive = true }
                            launchSingleTop = true
                        }

                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Info, contentDescription = null) },
                    label = { Text("About") },
                    selected = currentRoute == Screen.About.route,
                    onClick = {
                        navController.navigate(Screen.About.route) {
                            popUpTo(Screen.Search.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }

                        scope.launch {
                            drawerState.close()
                        }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }) {
        NavHost(
            navController = navController,
            startDestination = Screen.Search.route,
            modifier = modifier
        ) {
            composable(Screen.Search.route) {
                SearchScreen(
                    modifier = Modifier.fillMaxSize(),
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onEntryClick = { expression, reading ->
                        if (it.lifecycle.currentState == Lifecycle.State.RESUMED) {
                            navController.navigate(
                                Screen.EntryDetail.createRoute(
                                    expression,
                                    reading
                                )
                            )
                        }
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
                    onBack = {
                        if (backStack.lifecycle.currentState == Lifecycle.State.RESUMED) {
                            navController.popBackStack()
                        }
                    },
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

                KanjiDetailScreen(onBack = {
                    if (backStack.lifecycle.currentState == Lifecycle.State.RESUMED) {
                        navController.popBackStack()
                    }
                })
            }

            composable(Screen.About.route) {
                AboutScreen(onMenuClick = { scope.launch { drawerState.open() } })
            }
        }
    }
}

