package com.isep.kotlinproject

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.isep.kotlinproject.ui.auth.LoginScreen
import com.isep.kotlinproject.ui.auth.SignupScreen
import com.isep.kotlinproject.ui.game.AddEditGameScreen
import com.isep.kotlinproject.ui.game.GameDetailScreen
import com.isep.kotlinproject.ui.game.GameListScreen
import com.isep.kotlinproject.ui.profile.ProfileScreen
import com.isep.kotlinproject.ui.theme.KotlinProjectTheme
import com.isep.kotlinproject.viewmodel.AuthViewModel
import com.isep.kotlinproject.viewmodel.GameViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KotlinProjectTheme {
                val navController = rememberNavController()
                val authViewModel: AuthViewModel = viewModel()
                val gameViewModel: GameViewModel = viewModel()
                val navigateDestination by authViewModel.navigateDestination.collectAsState()

                LaunchedEffect(navigateDestination) {
                    navigateDestination?.let { destination ->
                        // Redirect player_home and editor_dashboard to game_list for now
                        val finalDest = if (destination == "player_home" || destination == "editor_dashboard") "game_list" else destination
                        navController.navigate(finalDest) {
                            popUpTo("login") { inclusive = true }
                        }
                        authViewModel.clearNavigation()
                    }
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "login",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("login") {
                            LoginScreen(
                                viewModel = authViewModel,
                                onNavigateToSignup = { navController.navigate("signup") }
                            )
                        }
                        composable("signup") {
                            SignupScreen(
                                viewModel = authViewModel,
                                onNavigateToLogin = { navController.popBackStack() }
                            )
                        }
                        composable("profile") {
                            ProfileScreen(
                                viewModel = authViewModel,
                                onNavigateToLogin = {
                                    navController.navigate("login") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                            )
                        }
                        
                        // Game Routes
                        composable("game_list") {
                            GameListScreen(
                                viewModel = gameViewModel,
                                onGameClick = { gameId -> navController.navigate("game_detail/$gameId") },
                                onAddGameClick = { navController.navigate("add_edit_game") },
                                onProfileClick = { navController.navigate("profile") }
                            )
                        }
                        
                        composable(
                            route = "game_detail/{gameId}",
                            arguments = listOf(navArgument("gameId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val gameId = backStackEntry.arguments?.getString("gameId") ?: return@composable
                            GameDetailScreen(
                                gameId = gameId,
                                viewModel = gameViewModel,
                                onEditClick = { id -> navController.navigate("add_edit_game?gameId=$id") },
                                onBack = { navController.popBackStack() }
                            )
                        }
                        
                        composable(
                            route = "add_edit_game?gameId={gameId}",
                            arguments = listOf(navArgument("gameId") { 
                                type = NavType.StringType 
                                nullable = true
                            })
                        ) { backStackEntry ->
                            val gameId = backStackEntry.arguments?.getString("gameId")
                            AddEditGameScreen(
                                gameId = gameId,
                                viewModel = gameViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
