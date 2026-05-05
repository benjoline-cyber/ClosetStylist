package com.Ben.closetstylist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.Ben.closetstylist.ui.closet.AddItemScreen
import com.Ben.closetstylist.ui.closet.AddItemViewModel
import com.Ben.closetstylist.ui.closet.ClosetScreen
import com.Ben.closetstylist.ui.closet.ClosetViewModel
import com.Ben.closetstylist.ui.inspiration.InspirationScreen
import com.Ben.closetstylist.ui.inspiration.InspirationViewModel
import com.Ben.closetstylist.ui.settings.SettingsScreen
import com.Ben.closetstylist.ui.settings.SettingsViewModel
import com.Ben.closetstylist.ui.suggest.SuggestScreen
import com.Ben.closetstylist.ui.suggest.SuggestViewModel
import com.Ben.closetstylist.ui.theme.ClosetStylistTheme

private const val ROUTE_ADD_ITEM = "closet/add"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as ClosetStylistApplication
        enableEdgeToEdge()
        setContent {
            ClosetStylistTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                val showBottomBar = currentDestination?.route != ROUTE_ADD_ITEM

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (showBottomBar) {
                            NavigationBar {
                                Tab.entries.forEach { tab ->
                                    NavigationBarItem(
                                        selected = currentDestination
                                            ?.hierarchy
                                            ?.any { it.route == tab.route } == true,
                                        onClick = {
                                            navController.navigate(tab.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                        icon = { Icon(tab.icon, contentDescription = null) },
                                        label = { Text(stringResource(tab.labelRes)) },
                                    )
                                }
                            }
                        }
                    },
                ) { innerPadding ->
                    val tabFadeIn = fadeIn(animationSpec = tween(220))
                    val tabFadeOut = fadeOut(animationSpec = tween(180))
                    NavHost(
                        navController = navController,
                        startDestination = Tab.Closet.route,
                        modifier = Modifier.padding(innerPadding),
                        enterTransition = { tabFadeIn },
                        exitTransition = { tabFadeOut },
                        popEnterTransition = { tabFadeIn },
                        popExitTransition = { tabFadeOut },
                    ) {
                        composable(Tab.Closet.route) {
                            val vm: ClosetViewModel = viewModel(
                                factory = ClosetViewModel.Factory(app.appContainer.clothingRepository),
                            )
                            ClosetScreen(
                                viewModel = vm,
                                onAddItem = { navController.navigate(ROUTE_ADD_ITEM) },
                            )
                        }
                        composable(ROUTE_ADD_ITEM) {
                            val vm: AddItemViewModel = viewModel(
                                factory = AddItemViewModel.Factory(
                                    app,
                                    app.appContainer.clothingRepository,
                                    app.appContainer.claudeRepository,
                                ),
                            )
                            AddItemScreen(
                                viewModel = vm,
                                onNavigateBack = { navController.popBackStack() },
                            )
                        }
                        composable(Tab.Inspiration.route) {
                            val vm: InspirationViewModel = viewModel(
                                factory = InspirationViewModel.Factory(
                                    app,
                                    app.appContainer.inspirationRepository,
                                ),
                            )
                            InspirationScreen(viewModel = vm)
                        }
                        composable(Tab.Suggest.route) {
                            val vm: SuggestViewModel = viewModel(
                                factory = SuggestViewModel.Factory(
                                    app,
                                    app.appContainer.clothingRepository,
                                    app.appContainer.inspirationRepository,
                                    app.appContainer.weatherRepository,
                                    app.appContainer.claudeRepository,
                                    app.appContainer.settingsRepository,
                                    app.appContainer.outfitFeedbackRepository,
                                ),
                            )
                            SuggestScreen(
                                viewModel = vm,
                                onNavigateToCloset = {
                                    navController.navigate(Tab.Closet.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                            )
                        }
                        composable(Tab.Settings.route) {
                            val vm: SettingsViewModel = viewModel(
                                factory = SettingsViewModel.Factory(
                                    app.appContainer.settingsRepository,
                                    app.appContainer.claudeRepository,
                                ),
                            )
                            SettingsScreen(viewModel = vm)
                        }
                    }
                }
            }
        }
    }
}

private enum class Tab(
    val route: String,
    val icon: ImageVector,
    val labelRes: Int,
) {
    Closet("closet", Icons.Default.Home, R.string.tab_closet),
    Inspiration("inspiration", Icons.Default.Favorite, R.string.tab_inspiration),
    Suggest("suggest", Icons.Default.Star, R.string.tab_suggest),
    Settings("settings", Icons.Default.Settings, R.string.tab_settings),
}
