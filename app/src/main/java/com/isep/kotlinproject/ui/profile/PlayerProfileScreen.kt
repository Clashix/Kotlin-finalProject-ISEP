package com.isep.kotlinproject.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.isep.kotlinproject.R
import com.isep.kotlinproject.model.*
import com.isep.kotlinproject.viewmodel.UserViewModel

/**
 * Complete player profile screen with tabs for different sections.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerProfileScreen(
    userId: String? = null, // null = current user
    viewModel: UserViewModel,
    onNavigateToGame: (String) -> Unit,
    onNavigateToUser: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val viewedUser by viewModel.viewedUser.collectAsState()
    val likedGames by viewModel.likedGames.collectAsState()
    val playedGames by viewModel.playedGames.collectAsState()
    val wishlistGames by viewModel.wishlistGames.collectAsState()
    val userReviews by viewModel.userReviews.collectAsState()
    val friends by viewModel.friends.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    val isOwnProfile = userId == null || userId == currentUser?.uid
    val displayUser = if (isOwnProfile) currentUser else viewedUser
    
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        stringResource(R.string.overview),
        stringResource(R.string.my_reviews),
        stringResource(R.string.liked_games),
        stringResource(R.string.played_games)
    )
    
    LaunchedEffect(userId) {
        if (!isOwnProfile && userId != null) {
            viewModel.loadUserProfile(userId)
        }
        viewModel.loadUserReviews(userId, refresh = true)
        viewModel.loadFriends(userId)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.profile)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    if (isOwnProfile) {
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings))
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading && displayUser == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (displayUser == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.error_load_data))
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Profile Header
                item {
                    ProfileHeader(
                        user = displayUser,
                        isOwnProfile = isOwnProfile,
                        reviewCount = userReviews.size,
                        friendsCount = displayUser.friends.size,
                        onEditProfile = { /* Navigate to edit */ },
                        onLogout = onLogout
                    )
                }
                
                // Quick Stats
                item {
                    ProfileQuickStats(
                        likedCount = displayUser.likedGames.size,
                        playedCount = displayUser.playedGames.size,
                        wishlistCount = displayUser.wishlist.size,
                        reviewCount = userReviews.size
                    )
                }
                
                // Tab Row
                item {
                    ScrollableTabRow(
                        selectedTabIndex = selectedTabIndex,
                        modifier = Modifier.fillMaxWidth(),
                        edgePadding = 16.dp
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTabIndex == index,
                                onClick = { selectedTabIndex = index },
                                text = { Text(title) }
                            )
                        }
                    }
                }
                
                // Tab Content
                when (selectedTabIndex) {
                    0 -> {
                        // Overview - Recent activity
                        item {
                            SectionHeader(
                                title = stringResource(R.string.liked_games),
                                onSeeAll = { selectedTabIndex = 2 }
                            )
                        }
                        item {
                            if (likedGames.isEmpty()) {
                                EmptySection(stringResource(R.string.empty_liked))
                            } else {
                                HorizontalGameList(
                                    games = likedGames.take(5),
                                    onGameClick = onNavigateToGame
                                )
                            }
                        }
                        
                        item {
                            SectionHeader(
                                title = stringResource(R.string.played_games),
                                onSeeAll = { selectedTabIndex = 3 }
                            )
                        }
                        item {
                            if (playedGames.isEmpty()) {
                                EmptySection(stringResource(R.string.empty_played))
                            } else {
                                HorizontalGameList(
                                    games = playedGames.take(5),
                                    onGameClick = onNavigateToGame
                                )
                            }
                        }
                        
                        item {
                            SectionHeader(
                                title = stringResource(R.string.friends),
                                onSeeAll = null
                            )
                        }
                        item {
                            if (friends.isEmpty()) {
                                EmptySection(stringResource(R.string.no_friends))
                            } else {
                                HorizontalFriendsList(
                                    friends = friends.take(10),
                                    onUserClick = onNavigateToUser
                                )
                            }
                        }
                    }
                    
                    1 -> {
                        // Reviews
                        if (userReviews.isEmpty()) {
                            item {
                                EmptySection(
                                    message = stringResource(R.string.empty_reviews),
                                    modifier = Modifier.padding(32.dp)
                                )
                            }
                        } else {
                            items(userReviews) { reviewWithGame ->
                                ReviewHistoryItem(
                                    reviewWithGame = reviewWithGame,
                                    onClick = { onNavigateToGame(reviewWithGame.review.gameId) }
                                )
                            }
                        }
                    }
                    
                    2 -> {
                        // Liked Games
                        if (likedGames.isEmpty()) {
                            item {
                                EmptySection(
                                    message = stringResource(R.string.empty_liked),
                                    modifier = Modifier.padding(32.dp)
                                )
                            }
                        } else {
                            items(likedGames) { game ->
                                GameListItem(
                                    game = game,
                                    onClick = { onNavigateToGame(game.id) }
                                )
                            }
                        }
                    }
                    
                    3 -> {
                        // Played Games
                        if (playedGames.isEmpty()) {
                            item {
                                EmptySection(
                                    message = stringResource(R.string.empty_played),
                                    modifier = Modifier.padding(32.dp)
                                )
                            }
                        } else {
                            items(playedGames) { game ->
                                GameListItem(
                                    game = game,
                                    onClick = { onNavigateToGame(game.id) }
                                )
                            }
                        }
                    }
                }
                
                // Bottom spacing
                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }
}

@Composable
private fun ProfileHeader(
    user: User,
    isOwnProfile: Boolean,
    reviewCount: Int,
    friendsCount: Int,
    onEditProfile: () -> Unit,
    onLogout: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
    ) {
        // Gradient Background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary
                        )
                    )
                )
        )
        
        // Profile Content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar
            Box {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(user.photoURL.ifBlank { 
                            "https://www.gravatar.com/avatar/00000000000000000000000000000000?d=mp&f=y" 
                        })
                        .crossfade(true)
                        .build(),
                    contentDescription = "Profile Picture",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                )
                
                if (isOwnProfile) {
                    IconButton(
                        onClick = onEditProfile,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .background(MaterialTheme.colorScheme.secondary, CircleShape)
                            .size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = stringResource(R.string.change_photo),
                            tint = MaterialTheme.colorScheme.onSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Name and Role
            Text(
                text = user.getDisplayNameOrLegacy(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = user.role.name,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun ProfileQuickStats(
    likedCount: Int,
    playedCount: Int,
    wishlistCount: Int,
    reviewCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatItem(value = reviewCount, label = stringResource(R.string.reviews))
        StatItem(value = likedCount, label = stringResource(R.string.liked_games))
        StatItem(value = playedCount, label = stringResource(R.string.played_games))
        StatItem(value = wishlistCount, label = stringResource(R.string.wishlist))
    }
}

@Composable
private fun StatItem(value: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    onSeeAll: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        if (onSeeAll != null) {
            TextButton(onClick = onSeeAll) {
                Text(stringResource(R.string.see_all))
            }
        }
    }
}

@Composable
private fun EmptySection(
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HorizontalGameList(
    games: List<Game>,
    onGameClick: (String) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(games) { game ->
            SmallGameCard(
                game = game,
                onClick = { onGameClick(game.id) }
            )
        }
    }
}

@Composable
private fun SmallGameCard(
    game: Game,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .height(200.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = game.posterUrl,
                contentDescription = game.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                            startY = 100f
                        )
                    )
            )
            
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            ) {
                Text(
                    text = game.title,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                if (game.hasReviews) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFB800),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = String.format("%.1f", game.averageRating),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HorizontalFriendsList(
    friends: List<User>,
    onUserClick: (String) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(friends) { friend ->
            FriendAvatar(
                user = friend,
                onClick = { onUserClick(friend.uid) }
            )
        }
    }
}

@Composable
private fun FriendAvatar(
    user: User,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(user.photoURL.ifBlank { 
                    "https://www.gravatar.com/avatar/00000000000000000000000000000000?d=mp&f=y" 
                })
                .crossfade(true)
                .build(),
            contentDescription = user.getDisplayNameOrLegacy(),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = user.getDisplayNameOrLegacy().take(10),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ReviewHistoryItem(
    reviewWithGame: ReviewWithGame,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            AsyncImage(
                model = reviewWithGame.gameImageUrl,
                contentDescription = reviewWithGame.gameTitle,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reviewWithGame.gameTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    repeat(5) { index ->
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = if (index < reviewWithGame.review.rating) 
                                Color(0xFFFFB800) else Color.Gray.copy(alpha = 0.3f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = reviewWithGame.review.getRelativeTime(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (reviewWithGame.review.comment.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = reviewWithGame.review.comment,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun GameListItem(
    game: Game,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            AsyncImage(
                model = game.posterUrl,
                contentDescription = game.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = game.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = game.genre,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = game.developer,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (game.hasReviews) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFB800),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = String.format("%.1f", game.averageRating),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
