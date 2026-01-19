package com.isep.kotlinproject.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.isep.kotlinproject.R
import com.isep.kotlinproject.model.UserRole
import com.isep.kotlinproject.ui.game.GameListScreen
import com.isep.kotlinproject.ui.users.UsersListScreen
import com.isep.kotlinproject.viewmodel.GameViewModel
import com.isep.kotlinproject.viewmodel.UserViewModel
import com.isep.kotlinproject.viewmodel.UsersViewModel

/**
 * Main screen with bottom navigation containing Games, Users, and Notifications tabs.
 */
@Composable
fun MainScreen(
    gameViewModel: GameViewModel,
    usersViewModel: UsersViewModel,
    userViewModel: UserViewModel,
    userRole: UserRole,
    onGameClick: (String) -> Unit,
    onAddGameClick: () -> Unit,
    onProfileClick: () -> Unit,
    onUserClick: (String) -> Unit,
    onNotificationsClick: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    
    // Observe notification count for badge
    val pendingRequests by userViewModel.pendingFriendRequests.collectAsState()
    val notificationCount = pendingRequests.size
    
    Scaffold(
        bottomBar = {
            NavigationBar {
                BottomNavItem.entries.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { 
                            if (item == BottomNavItem.NOTIFICATIONS) {
                                onNotificationsClick()
                            } else {
                                selectedTab = index 
                            }
                        },
                        icon = {
                            if (item == BottomNavItem.NOTIFICATIONS && notificationCount > 0) {
                                BadgedBox(
                                    badge = {
                                        Badge {
                                            Text(
                                                text = if (notificationCount > 9) "9+" else notificationCount.toString(),
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (selectedTab == index) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = stringResource(item.labelResId)
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = if (selectedTab == index) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = stringResource(item.labelResId)
                                )
                            }
                        },
                        label = {
                            Text(stringResource(item.labelResId))
                        }
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (selectedTab) {
                0 -> {
                    // Games Tab - reuse existing GameListScreen
                    GameListScreen(
                        viewModel = gameViewModel,
                        userRole = userRole,
                        onGameClick = onGameClick,
                        onAddGameClick = onAddGameClick,
                        onProfileClick = onProfileClick
                    )
                }
                1 -> {
                    // Users Tab
                    UsersListScreen(
                        viewModel = usersViewModel,
                        onUserClick = onUserClick,
                        onProfileClick = onProfileClick
                    )
                }
            }
        }
    }
}

/**
 * Enum defining the bottom navigation items
 */
enum class BottomNavItem(
    val labelResId: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    GAMES(
        labelResId = R.string.nav_games,
        selectedIcon = Icons.Filled.SportsEsports,
        unselectedIcon = Icons.Outlined.SportsEsports
    ),
    USERS(
        labelResId = R.string.nav_users,
        selectedIcon = Icons.Filled.People,
        unselectedIcon = Icons.Outlined.People
    ),
    NOTIFICATIONS(
        labelResId = R.string.nav_notifications,
        selectedIcon = Icons.Filled.Notifications,
        unselectedIcon = Icons.Outlined.Notifications
    )
}
