package com.example.ui.navigation

sealed class Screen(val route: String, val title: String, val icon: String) {
    object Translator : Screen("translator", "Translate", "translate")
    object History : Screen("history", "History", "history")
    object Favorites : Screen("favorites", "Favorites", "favorite")
    object Settings : Screen("settings", "Settings", "settings")
    object About : Screen("about", "About", "info")
}
