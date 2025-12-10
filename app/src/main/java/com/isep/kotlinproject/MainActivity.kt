package com.isep.kotlinproject

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.isep.kotlinproject.ui.auth.LoginScreen
import com.isep.kotlinproject.ui.auth.SignupScreen
import com.isep.kotlinproject.ui.profile.ProfileScreen
import com.isep.kotlinproject.ui.theme.KotlinProjectTheme
import com.isep.kotlinproject.viewmodel.AuthViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KotlinProjectTheme {
                val navController = rememberNavController()
                val authViewModel: AuthViewModel = viewModel()
                val navigateDestination by authViewModel.navigateDestination.collectAsState()

                LaunchedEffect(navigateDestination) {
                    navigateDestination?.let { destination ->
                        navController.navigate(destination) {
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
                        composable("player_home") {
                            // Placeholder for Player Home Screen
                            Column {
                                Text(text = "Player Home Screen Placeholder")
                                Button(onClick = { navController.navigate("profile") }) {
                                    Text("Go to Profile")
                                }
                            }
                        }
                         composable("editor_dashboard") {
                            // Placeholder for Editor Dashboard Screen
                            Column {
                                Text(text = "Editor Dashboard Screen Placeholder")
                                Button(onClick = { navController.navigate("profile") }) {
                                    Text("Go to Profile")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}