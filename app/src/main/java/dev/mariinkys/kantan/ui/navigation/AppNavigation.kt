package dev.mariinkys.kantan.ui.navigation

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
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
import dev.mariinkys.kantan.ui.favorites.FavoritesScreen
import dev.mariinkys.kantan.ui.kanji.KanjiDetailScreen
import dev.mariinkys.kantan.ui.search.SearchScreen
import dev.mariinkys.kantan.ui.search.SearchViewModel
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.net.URLEncoder

private fun String.enc() = URLEncoder.encode(this, "UTF-8")
private fun String.dec() = URLDecoder.decode(this, "UTF-8")

sealed class Screen(val route: String) {
    data object Search : Screen("search")

    data object About : Screen("about")

    data object Favorites : Screen("favorites")

    data object EntryDetail : Screen("entry/{sequence}") {
        fun createRoute(sequence: Int) = "entry/$sequence"
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

    // One snackbar for the entire app, I guess it should be better (similar to web)
    val snackbarHostState = remember { SnackbarHostState() }

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
                    icon = { Icon(Icons.Default.Favorite, contentDescription = null) },
                    label = { Text("Favorites") },
                    selected = currentRoute == Screen.Favorites.route,
                    onClick = {
                        navController.navigate(Screen.Favorites.route) {
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

        Scaffold(
            modifier = modifier,
            snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->

            NavHost(
                navController = navController,
                startDestination = Screen.Search.route,
                modifier = modifier
                    .fillMaxSize()
                    .padding()
            ) {
                composable(Screen.Search.route) {
                    SearchScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        onMenuClick = { scope.launch { drawerState.open() } },
                        onEntryClick = { sequence ->
                            if (it.lifecycle.currentState == Lifecycle.State.RESUMED) {
                                navController.navigate(
                                    Screen.EntryDetail.createRoute(
                                        sequence
                                    )
                                )
                            }
                        }
                    )
                }

                composable(Screen.Favorites.route) {
                    FavoritesScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        snackbarHostState = snackbarHostState,
                        onMenuClick = { scope.launch { drawerState.open() } },
                        onEntryClick = { sequence ->
                            navController.navigate(
                                Screen.EntryDetail.createRoute(
                                    sequence
                                )
                            )
                        }
                    )
                }

                composable(
                    route = Screen.EntryDetail.route,
                    arguments = listOf(
                        navArgument("sequence") { type = NavType.IntType }
                    )
                ) { backStack ->
                    val sequence = backStack.arguments?.getString("sequence")?.dec() ?: ""
                    backStack.arguments?.putString("sequence", sequence)

                    // Retrieve the same SearchViewModel instance that SearchScreen holds, so onQueryChange lands in the right state when we pop back.
                    val searchEntry = remember(backStack) {
                        navController.getBackStackEntry(Screen.Search.route)
                    }
                    val searchViewModel: SearchViewModel = hiltViewModel(searchEntry)

                    EntryDetailScreen(
                        modifier = Modifier.fillMaxSize(),
                        onBack = {
                            if (backStack.lifecycle.currentState == Lifecycle.State.RESUMED) {
                                navController.popBackStack()
                            }
                        },
                        onKanjiClick = { character ->
                            navController.navigate(Screen.KanjiDetail.createRoute(character))
                        },
                        onTermClick = { term ->
                            searchViewModel.onQueryChange(term)
                            navController.popBackStack(Screen.Search.route, inclusive = false)
                        }
                    )
                }

                composable(
                    route = Screen.KanjiDetail.route,
                    arguments = listOf(navArgument("character") { type = NavType.StringType })
                ) { backStack ->
                    val character = backStack.arguments?.getString("character")?.dec() ?: ""
                    backStack.arguments?.putString("character", character)

                    KanjiDetailScreen(modifier = Modifier.fillMaxSize(), onBack = {
                        if (backStack.lifecycle.currentState == Lifecycle.State.RESUMED) {
                            navController.popBackStack()
                        }
                    })
                }

                composable(Screen.About.route) {
                    AboutScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        onMenuClick = { scope.launch { drawerState.open() } })
                }
            }
        }

    }
}

