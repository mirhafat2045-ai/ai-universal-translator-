package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.navigation.Screen
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.TranslatorViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: TranslatorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val uiState by viewModel.uiState.collectAsState()
            MyApplicationTheme(darkTheme = uiState.isDarkMode) {
                val navController = rememberNavController()
                val items = listOf(
                    Screen.Translator,
                    Screen.History,
                    Screen.Favorites,
                    Screen.Settings,
                    Screen.About
                )

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar {
                            val navBackStackEntry by navController.currentBackStackEntryAsState()
                            val currentRoute = navBackStackEntry?.destination?.route

                            items.forEach { screen ->
                                val icon = when (screen) {
                                    Screen.Translator -> Icons.Default.Translate
                                    Screen.History -> Icons.Default.History
                                    Screen.Favorites -> Icons.Default.Favorite
                                    Screen.Settings -> Icons.Default.Settings
                                    Screen.About -> Icons.Default.Info
                                    else -> Icons.Default.Home
                                }
                                NavigationBarItem(
                                    icon = { Icon(icon, contentDescription = screen.title) },
                                    label = { Text(screen.title) },
                                    selected = currentRoute == screen.route,
                                    onClick = {
                                        if (currentRoute != screen.route) {
                                            navController.navigate(screen.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Translator.route,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(Screen.Translator.route) { TranslatorScreen(viewModel) }
                        composable(Screen.History.route) { HistoryScreen(viewModel) }
                        composable(Screen.Favorites.route) { FavoritesScreen(viewModel) }
                        composable(Screen.Settings.route) { SettingsScreen(viewModel) }
                        composable(Screen.About.route) { AboutScreen() }
                    }
                }
            }
        }
    }
}
