package com.danihg.bitratelab.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Test : Screen("test")
    object Results : Screen("results")
}