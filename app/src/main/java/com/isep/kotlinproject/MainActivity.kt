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
import com.isep.kotlinproject.model.UserRole
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
                val currentUser by authViewModel.user.collectAsState()

                // Update GameViewModel with user info and start listening
                LaunchedEffect(currentUser) {
                    if (currentUser != null) {
                        val user = currentUser!!
                        gameViewModel.setUserInfo(user.role, user.name)
                        
                        // Editors see only their games, Players see all games
                        if (user.role == UserRole.EDITOR) {
                            gameViewModel.startListening(isEditor = true, editorId = user.id)
                        } else {
                            gameViewModel.startListening(isEditor = false)
                        }
                    } else {
                        gameViewModel.stopListening()
                    }
                }

                // Handle navigation after login/signup
                LaunchedEffect(navigateDestination) {
                    navigateDestination?.let { destination ->
                        // Both roles go to game_list, but with different views
                        val finalDest = when (destination) {
                            "player_home", "editor_dashboard" -> "game_list"
                            else -> destination
                        }
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
                        
                        // Game List - works for both Player and Editor
                        composable("game_list") {
                            val userRole = currentUser?.role ?: UserRole.PLAYER
                            GameListScreen(
                                viewModel = gameViewModel,
                                userRole = userRole,
                                onGameClick = { gameId -> navController.navigate("game_detail/$gameId") },
                                onAddGameClick = { navController.navigate("add_edit_game") },
                                onProfileClick = { navController.navigate("profile") }
                            )
                        }
                        
                        // Game Detail - works for both Player and Editor (with different actions)
                        composable(
                            route = "game_detail/{gameId}",
                            arguments = listOf(navArgument("gameId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val gameId = backStackEntry.arguments?.getString("gameId") ?: return@composable
                            val userRole = currentUser?.role ?: UserRole.PLAYER
                            GameDetailScreen(
                                gameId = gameId,
                                viewModel = gameViewModel,
                                userRole = userRole,
                                onEditClick = { id -> navController.navigate("add_edit_game?gameId=$id") },
                                onBack = { navController.popBackStack() }
                            )
                        }
                        
                        // Add/Edit Game - Editor only (but we check role in the screen)
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
