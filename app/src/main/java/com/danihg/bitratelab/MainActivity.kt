package com.danihg.bitratelab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.danihg.bitratelab.navigation.Screen
import com.danihg.bitratelab.ui.results.ResultsScreen
import com.danihg.bitratelab.ui.splash.SplashScreen
import com.danihg.bitratelab.ui.test.TestScreen
import com.danihg.bitratelab.ui.test.TestViewModel
import com.danihg.bitratelab.ui.theme.BitrateLabTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BitrateLabTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    BitrateLabApp()
                }
            }
        }
    }
}

@Composable
fun BitrateLabApp() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToTest = {
                    navController.navigate(Screen.Test.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Test.route) {
            val viewModel: TestViewModel = viewModel()
            TestScreen(
                viewModel = viewModel,
                onNavigateToResults = {
                    navController.navigate(Screen.Results.route)
                }
            )
        }

        composable(Screen.Results.route) {
            val parentEntry = navController.getBackStackEntry(Screen.Test.route)
            val viewModel: TestViewModel = viewModel(parentEntry)
            ResultsScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}