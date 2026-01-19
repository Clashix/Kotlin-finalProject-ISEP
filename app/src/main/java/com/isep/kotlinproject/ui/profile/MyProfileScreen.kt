package com.isep.kotlinproject.ui.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.isep.kotlinproject.R
import com.isep.kotlinproject.model.Game
import com.isep.kotlinproject.model.ReviewWithGame
import com.isep.kotlinproject.model.UserRole
import com.isep.kotlinproject.viewmodel.AuthViewModel
import com.isep.kotlinproject.viewmodel.UserViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Personal profile screen showing all user data:
 * - Profile header with avatar, name, role
 * - Tabs for Reviews, Wishlist, Liked Games, Played Games
 * - Settings and logout
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyProfileScreen(
    authViewModel: AuthViewModel,
    userViewModel: UserViewModel,
    onNavigateToGame: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToFriends: () -> Unit,
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit
) {
    val currentUser by authViewModel.user.collectAsState()
    val userReviews by userViewModel.userReviews.collectAsState()
    val likedGames by userViewModel.likedGames.collectAsState()
    val playedGames by userViewModel.playedGames.collectAsState()
    val wishlistGames by userViewModel.wishlistGames.collectAsState()
    val friends by userViewModel.friends.collectAsState()
    val isLoading by userViewModel.isLoading.collectAsState()
    val isLoadingMore by userViewModel.isLoadingMore.collectAsState()
    val hasMoreReviews by userViewModel.hasMoreReviews.collectAsState()
    
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Image picker for profile photo
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { userViewModel.uploadProfileImage(it) }
    }
    
    // Load data on first composition
    LaunchedEffect(currentUser?.uid) {
        currentUser?.uid?.let { userId ->
            userViewModel.loadUserReviews(refresh = true)
            userViewModel.loadFriends()
        }
    }
    
    val pagerState = rememberPagerState(pageCount = { 4 })
    val tabs = listOf(
        stringResource(R.string.reviews),
        stringResource(R.string.wishlist),
        stringResource(R.string.liked_games),
        stringResource(R.string.played_games)
    )
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.my_profile)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings))
                    }
                    IconButton(onClick = {
                        authViewModel.logout()
                        onLogout()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = stringResource(R.string.logout))
                    }
                }
            )
        }
    ) { padding ->
        if (currentUser == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        
        val user = currentUser!!
        
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Profile Header
            item {
                ProfileHeader(
                    photoUrl = user.photoURL.ifEmpty { user.getPhotoURLOrLegacy() },
                    displayName = user.getDisplayNameOrLegacy(),
                    email = user.email,
                    role = user.userRole,
                    bio = user.bio,
                    friendsCount = friends.size,
                    reviewsCount = userReviews.size,
                    onChangePhoto = { imagePickerLauncher.launch("image/*") },
                    onFriendsClick = onNavigateToFriends
                )
            }
            
            // Stats Cards
            item {
                StatsSection(
                    likedCount = likedGames.size,
                    playedCount = playedGames.size,
                    wishlistCount = wishlistGames.size,
                    reviewsCount = userReviews.size
                )
            }
            
            // Tab Row
            item {
                TabRow(
                    selectedTabIndex = pagerState.currentPage,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                            text = { Text(title) }
                        )
                    }
                }
            }
            
            // Tab Content
            item {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 400.dp)
                ) { page ->
                    when (page) {
                        0 -> ReviewsTab(
                            reviews = userReviews,
                            isLoading = isLoading,
                            isLoadingMore = isLoadingMore,
                            hasMore = hasMoreReviews,
                            onLoadMore = { userViewModel.loadUserReviews() },
                            onReviewClick = { onNavigateToGame(it) }
                        )
                        1 -> GamesGridTab(
                            games = wishlistGames,
                            emptyMessage = stringResource(R.string.empty_wishlist_msg),
                            onGameClick = onNavigateToGame
                        )
                        2 -> GamesGridTab(
                            games = likedGames,
                            emptyMessage = stringResource(R.string.empty_liked_msg),
                            onGameClick = onNavigateToGame
                        )
                        3 -> GamesGridTab(
                            games = playedGames,
                            emptyMessage = stringResource(R.string.empty_played_msg),
                            onGameClick = onNavigateToGame
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileHeader(
    photoUrl: String,
    displayName: String,
    email: String,
    role: UserRole,
    bio: String,
    friendsCount: Int,
    reviewsCount: Int,
    onChangePhoto: () -> Unit,
    onFriendsClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Avatar with edit overlay
        Box(
            modifier = Modifier
                .size(120.dp)
                .clickable(onClick = onChangePhoto)
        ) {
            if (photoUrl.isNotEmpty()) {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = stringResource(R.string.cd_profile_photo),
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = displayName.firstOrNull()?.uppercase() ?: "?",
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
            
            // Camera icon overlay
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = stringResource(R.string.cd_change_photo),
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Name
        Text(
            text = displayName,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        // Email
        Text(
            text = email,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Role badge
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = when (role) {
                UserRole.EDITOR -> Color(0xFF6200EA).copy(alpha = 0.15f)
                UserRole.PLAYER -> MaterialTheme.colorScheme.primaryContainer
            }
        ) {
            Text(
                text = role.name.uppercase(),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = when (role) {
                    UserRole.EDITOR -> Color(0xFF6200EA)
                    UserRole.PLAYER -> MaterialTheme.colorScheme.primary
                }
            )
        }
        
        // Bio
        if (bio.isNotBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = bio,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Friends button
        OutlinedButton(onClick = onFriendsClick) {
            Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.friends_count, friendsCount))
        }
    }
}

@Composable
private fun StatsSection(
    likedCount: Int,
    playedCount: Int,
    wishlistCount: Int,
    reviewsCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatCard(
            icon = Icons.Default.Favorite,
            count = likedCount,
            label = stringResource(R.string.liked_games),
            color = Color(0xFFE91E63)
        )
        StatCard(
            icon = Icons.Default.CheckCircle,
            count = playedCount,
            label = stringResource(R.string.played_games),
            color = Color(0xFF4CAF50)
        )
        StatCard(
            icon = Icons.Default.BookmarkAdded,
            count = wishlistCount,
            label = stringResource(R.string.wishlist),
            color = Color(0xFF2196F3)
        )
        StatCard(
            icon = Icons.Default.RateReview,
            count = reviewsCount,
            label = stringResource(R.string.reviews),
            color = Color(0xFFFF9800)
        )
    }
}

@Composable
private fun StatCard(
    icon: ImageVector,
    count: Int,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = CircleShape,
            color = color.copy(alpha = 0.15f),
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ReviewsTab(
    reviews: List<ReviewWithGame>,
    isLoading: Boolean,
    isLoadingMore: Boolean,
    hasMore: Boolean,
    onLoadMore: () -> Unit,
    onReviewClick: (String) -> Unit
) {
    if (isLoading && reviews.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxWidth().height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }
    
    if (reviews.isEmpty()) {
        EmptyStateMessage(
            icon = Icons.Default.RateReview,
            message = stringResource(R.string.empty_reviews_msg)
        )
        return
    }
    
    Column(modifier = Modifier.padding(16.dp)) {
        reviews.forEach { reviewWithGame ->
            ReviewCard(
                reviewWithGame = reviewWithGame,
                onClick = { reviewWithGame.game?.id?.let { onReviewClick(it) } }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
        
        if (hasMore) {
            if (isLoadingMore) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            } else {
                TextButton(
                    onClick = onLoadMore,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.load_more))
                }
            }
        }
    }
}

@Composable
private fun ReviewCard(
    reviewWithGame: ReviewWithGame,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Game image
            AsyncImage(
                model = reviewWithGame.game?.posterUrl ?: "",
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reviewWithGame.gameTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Rating stars
                Row(verticalAlignment = Alignment.CenterVertically) {
                    repeat(5) { index ->
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = if (index < reviewWithGame.review.rating) 
                                Color(0xFFFFB800) else Color.Gray.copy(alpha = 0.3f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${reviewWithGame.review.rating}/5",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                
                // Comment preview
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
                
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = reviewWithGame.review.getRelativeTime(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun GamesGridTab(
    games: List<Game>,
    emptyMessage: String,
    onGameClick: (String) -> Unit
) {
    if (games.isEmpty()) {
        EmptyStateMessage(
            icon = Icons.Default.SportsEsports,
            message = emptyMessage
        )
        return
    }
    
    Column(modifier = Modifier.padding(16.dp)) {
        games.chunked(2).forEach { rowGames ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowGames.forEach { game ->
                    GameCard(
                        game = game,
                        onClick = { onGameClick(game.id) },
                        modifier = Modifier.weight(1f)
                    )
                }
                // Fill empty space if odd number of games
                if (rowGames.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun GameCard(
    game: Game,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .aspectRatio(0.75f)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box {
            AsyncImage(
                model = game.posterUrl,
                contentDescription = game.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            // Gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f)
                            ),
                            startY = 100f
                        )
                    )
            )
            
            // Title and rating
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            ) {
                Text(
                    text = game.title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                if (game.averageRating > 0) {
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
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyStateMessage(
    icon: ImageVector,
    message: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
