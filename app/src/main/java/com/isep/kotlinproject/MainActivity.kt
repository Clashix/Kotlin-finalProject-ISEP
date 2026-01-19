package com.isep.kotlinproject

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
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
import com.isep.kotlinproject.model.ThemePreference
import com.isep.kotlinproject.model.UserRole
import com.isep.kotlinproject.ui.auth.LoginScreen
import com.isep.kotlinproject.ui.auth.SignupScreen
import com.isep.kotlinproject.ui.editor.EditorDashboardScreen
import com.isep.kotlinproject.ui.editor.GameHistoryScreen
import com.isep.kotlinproject.ui.game.AddEditGameScreen
import com.isep.kotlinproject.ui.game.GameDetailScreen
import com.isep.kotlinproject.ui.main.MainScreen
import com.isep.kotlinproject.ui.onboarding.OnboardingScreen
import com.isep.kotlinproject.ui.profile.MyProfileScreen
import com.isep.kotlinproject.ui.profile.PlayerProfileScreen
import com.isep.kotlinproject.ui.profile.ProfileScreen
import com.isep.kotlinproject.ui.profile.WishlistScreen
import com.isep.kotlinproject.ui.settings.SettingsScreen
import com.isep.kotlinproject.ui.social.ChatScreen
import com.isep.kotlinproject.ui.social.ChatsListScreen
import com.isep.kotlinproject.ui.social.FriendsScreen
import com.isep.kotlinproject.ui.social.NotificationsScreen
import com.isep.kotlinproject.ui.theme.KotlinProjectTheme
import com.isep.kotlinproject.ui.trending.TrendingScreen
import com.isep.kotlinproject.ui.users.PublicUserProfileScreen
import com.isep.kotlinproject.util.withLocale
import com.isep.kotlinproject.viewmodel.AuthViewModel
import com.isep.kotlinproject.viewmodel.BadgeViewModel
import com.isep.kotlinproject.viewmodel.ChatViewModel
import com.isep.kotlinproject.viewmodel.GameViewModel
import com.isep.kotlinproject.viewmodel.SettingsViewModel
import com.isep.kotlinproject.viewmodel.StatsViewModel
import com.isep.kotlinproject.viewmodel.UserViewModel
import com.isep.kotlinproject.viewmodel.UsersViewModel

/**
 * Main activity containing the navigation graph for the entire application.
 * Supports both Player and Editor user roles with role-specific screens.
 */
class MainActivity : ComponentActivity() {
    
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase.withLocale())
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // ViewModels that need to be available before theme
            val settingsViewModel: SettingsViewModel = viewModel()
            
            // Theme preference - must be computed before KotlinProjectTheme
            val themePreference by settingsViewModel.themePreference.collectAsState()
            val systemInDarkTheme = isSystemInDarkTheme()
            val useDarkTheme = when (themePreference) {
                ThemePreference.LIGHT -> false
                ThemePreference.DARK -> true
                ThemePreference.SYSTEM -> systemInDarkTheme
            }
            
            KotlinProjectTheme(darkTheme = useDarkTheme) {
                val navController = rememberNavController()
                
                // ViewModels
                val authViewModel: AuthViewModel = viewModel()
                val gameViewModel: GameViewModel = viewModel()
                val userViewModel: UserViewModel = viewModel()
                val usersViewModel: UsersViewModel = viewModel()
                val statsViewModel: StatsViewModel = viewModel()
                val chatViewModel: ChatViewModel = viewModel()
                val badgeViewModel: BadgeViewModel = viewModel()
                
                // State
                val navigateDestination by authViewModel.navigateDestination.collectAsState()
                val currentUser by authViewModel.user.collectAsState()

                // Check Onboarding State
                val sharedPref = getPreferences(Context.MODE_PRIVATE)
                val onboardingCompleted = sharedPref.getBoolean("onboarding_completed", false)
                val startDestination = if (onboardingCompleted) "login" else "onboarding"

                // Update GameViewModel with user info and start listening
                LaunchedEffect(currentUser) {
                    if (currentUser != null) {
                        val user = currentUser!!
                        gameViewModel.setUserInfo(user.userRole, user.getDisplayNameOrLegacy())
                        
                        // Editors see only their games, Players see all games
                        if (user.userRole == UserRole.EDITOR) {
                            gameViewModel.startListening(isEditor = true, editorId = user.uid)
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
                        startDestination = startDestination,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        // =====================================================
                        // ONBOARDING & AUTH
                        // =====================================================
                        
                        composable("onboarding") {
                            OnboardingScreen(
                                onFinished = {
                                    with(sharedPref.edit()) {
                                        putBoolean("onboarding_completed", true)
                                        apply()
                                    }
                                    navController.navigate("login") {
                                        popUpTo("onboarding") { inclusive = true }
                                    }
                                }
                            )
                        }
                        
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
                        
                        // =====================================================
                        // MAIN SCREENS
                        // =====================================================
                        
                        // Main screen with bottom navigation (Games + Users + Notifications tabs)
                        composable("game_list") {
                            val userRole = currentUser?.userRole ?: UserRole.PLAYER
                            MainScreen(
                                gameViewModel = gameViewModel,
                                usersViewModel = usersViewModel,
                                userViewModel = userViewModel,
                                userRole = userRole,
                                onGameClick = { gameId -> 
                                    navController.navigate("game_detail/$gameId") 
                                },
                                onAddGameClick = { navController.navigate("add_edit_game") },
                                onProfileClick = { navController.navigate("profile") },
                                onUserClick = { userId ->
                                    navController.navigate("public_profile/$userId")
                                },
                                onNotificationsClick = { 
                                    navController.navigate("notifications") 
                                }
                            )
                        }
                        
                        // Public User Profile (view only)
                        composable(
                            route = "public_profile/{userId}",
                            arguments = listOf(navArgument("userId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
                            PublicUserProfileScreen(
                                userId = userId,
                                viewModel = usersViewModel,
                                userViewModel = userViewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToGame = { gameId ->
                                    navController.navigate("game_detail/$gameId")
                                },
                                onNavigateToChat = { otherUserId, otherUserName ->
                                    navController.navigate("chat/$otherUserId/$otherUserName")
                                }
                            )
                        }
                        
                        // Game Detail - works for both Player and Editor
                        composable(
                            route = "game_detail/{gameId}",
                            arguments = listOf(navArgument("gameId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val gameId = backStackEntry.arguments?.getString("gameId") ?: return@composable
                            val userRole = currentUser?.userRole ?: UserRole.PLAYER
                            GameDetailScreen(
                                gameId = gameId,
                                viewModel = gameViewModel,
                                userViewModel = userViewModel,
                                userRole = userRole,
                                onEditClick = { id -> navController.navigate("add_edit_game?gameId=$id") },
                                onBack = { navController.popBackStack() }
                            )
                        }
                        
                        // Add/Edit Game - Editor only
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
                        
                        // =====================================================
                        // PROFILE SCREENS
                        // =====================================================
                        
                        // My Profile - full version with all user data
                        composable("profile") {
                            MyProfileScreen(
                                authViewModel = authViewModel,
                                userViewModel = userViewModel,
                                onNavigateToGame = { gameId ->
                                    navController.navigate("game_detail/$gameId")
                                },
                                onNavigateToSettings = { navController.navigate("settings") },
                                onNavigateToFriends = { navController.navigate("friends") },
                                onNavigateBack = { navController.popBackStack() },
                                onLogout = {
                                    navController.navigate("login") {
                                        popUpTo("game_list") { inclusive = true }
                                    }
                                }
                            )
                        }
                        
                        // Player Profile (full version with tabs)
                        composable(
                            route = "player_profile?userId={userId}",
                            arguments = listOf(navArgument("userId") {
                                type = NavType.StringType
                                nullable = true
                            })
                        ) { backStackEntry ->
                            val userId = backStackEntry.arguments?.getString("userId")
                            PlayerProfileScreen(
                                userId = userId,
                                viewModel = userViewModel,
                                onNavigateToGame = { gameId -> 
                                    navController.navigate("game_detail/$gameId") 
                                },
                                onNavigateToUser = { uid -> 
                                    navController.navigate("player_profile?userId=$uid") 
                                },
                                onNavigateToSettings = { navController.navigate("settings") },
                                onNavigateBack = { navController.popBackStack() },
                                onLogout = {
                                    authViewModel.logout()
                                    navController.navigate("login") {
                                        popUpTo("game_list") { inclusive = true }
                                    }
                                }
                            )
                        }
                        
                        // Wishlist
                        composable("wishlist") {
                            WishlistScreen(
                                viewModel = userViewModel,
                                onNavigateToGame = { gameId -> 
                                    navController.navigate("game_detail/$gameId") 
                                },
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        
                        // =====================================================
                        // SOCIAL SCREENS
                        // =====================================================
                        
                        // Friends
                        composable("friends") {
                            FriendsScreen(
                                viewModel = userViewModel,
                                onNavigateToProfile = { userId -> 
                                    navController.navigate("player_profile?userId=$userId") 
                                },
                                onNavigateToChat = { userId, userName ->
                                    navController.navigate("chat/$userId/$userName")
                                },
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        
                        // Notifications (Friend requests and other notifications)
                        composable("notifications") {
                            NotificationsScreen(
                                viewModel = userViewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToUser = { userId ->
                                    navController.navigate("public_profile/$userId")
                                }
                            )
                        }
                        
                        // Chats List
                        composable("chats") {
                            ChatsListScreen(
                                viewModel = chatViewModel,
                                onNavigateToChat = { chatId ->
                                    navController.navigate("chat_by_id/$chatId")
                                },
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        
                        // Chat by User ID and Name
                        composable(
                            route = "chat/{userId}/{userName}",
                            arguments = listOf(
                                navArgument("userId") { type = NavType.StringType },
                                navArgument("userName") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
                            val userName = backStackEntry.arguments?.getString("userName") ?: "User"
                            ChatScreen(
                                viewModel = chatViewModel,
                                otherUserId = userId,
                                otherUserName = userName,
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToProfile = { uid ->
                                    navController.navigate("player_profile?userId=$uid")
                                }
                            )
                        }
                        
                        // Chat by Chat ID
                        composable(
                            route = "chat_by_id/{chatId}",
                            arguments = listOf(navArgument("chatId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val chatId = backStackEntry.arguments?.getString("chatId") ?: return@composable
                            // For chat by ID, we need to extract user info from the chat
                            val otherUserId = chatViewModel.getOtherParticipantId() ?: ""
                            val otherUserName = chatViewModel.getOtherParticipantName()
                            
                            LaunchedEffect(chatId) {
                                chatViewModel.openChatById(chatId)
                            }
                            
                            ChatScreen(
                                viewModel = chatViewModel,
                                otherUserId = otherUserId,
                                otherUserName = otherUserName,
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToProfile = { uid ->
                                    navController.navigate("player_profile?userId=$uid")
                                }
                            )
                        }
                        
                        // =====================================================
                        // EDITOR SCREENS
                        // =====================================================
                        
                        // Editor Statistics Dashboard
                        composable("editor_stats") {
                            val games by gameViewModel.games.collectAsState()
                            EditorDashboardScreen(
                                viewModel = statsViewModel,
                                editorGames = games,
                                onNavigateToGame = { gameId -> 
                                    navController.navigate("game_detail/$gameId") 
                                },
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        
                        // =====================================================
                        // TRENDING & DISCOVERY
                        // =====================================================
                        
                        // Trending Games
                        composable("trending") {
                            TrendingScreen(
                                viewModel = statsViewModel,
                                onNavigateToGame = { gameId -> 
                                    navController.navigate("game_detail/$gameId") 
                                },
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        
                        // =====================================================
                        // SETTINGS
                        // =====================================================
                        
                        composable("settings") {
                            SettingsScreen(
                                viewModel = settingsViewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        
                        // Game Edit History (Editor only)
                        composable("game_history") {
                            val editorId = currentUser?.uid ?: return@composable
                            GameHistoryScreen(
                                editorId = editorId,
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToGame = { gameId ->
                                    navController.navigate("game_detail/$gameId")
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
