package com.isep.kotlinproject.ui.users

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.isep.kotlinproject.R
import com.isep.kotlinproject.api.SteamAppDetails
import com.isep.kotlinproject.model.Game
import com.isep.kotlinproject.model.ReviewWithGame
import com.isep.kotlinproject.model.User
import com.isep.kotlinproject.model.UserRole
import com.isep.kotlinproject.viewmodel.UsersViewModel
import kotlinx.coroutines.launch

/**
 * Public user profile screen showing user info, reviews, wishlist, etc.
 * This is a VIEW ONLY screen for viewing other users' profiles.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicUserProfileScreen(
    userId: String,
    viewModel: UsersViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToGame: (String) -> Unit,
    onNavigateToChat: (String, String) -> Unit
) {
    val profileState by viewModel.profileState.collectAsState()
    val profileUser by viewModel.profileUser.collectAsState()
    val userReviews by viewModel.userReviews.collectAsState()
    val steamWishlist by viewModel.steamWishlist.collectAsState()
    val likedGames by viewModel.likedGames.collectAsState()
    val playedGames by viewModel.playedGames.collectAsState()
    val isLoadingReviews by viewModel.isLoadingReviews.collectAsState()
    val isLoadingSteamWishlist by viewModel.isLoadingSteamWishlist.collectAsState()
    
    // Load profile on first composition
    LaunchedEffect(userId) {
        viewModel.loadPublicProfile(userId)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.public_profile)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        when (profileState) {
            is UsersViewModel.ProfileState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            is UsersViewModel.ProfileState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = (profileState as UsersViewModel.ProfileState.Error).message,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadPublicProfile(userId) }) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                }
            }
            
            is UsersViewModel.ProfileState.Success, UsersViewModel.ProfileState.Initial -> {
                profileUser?.let { user ->
                    PublicProfileContent(
                        user = user,
                        reviews = userReviews,
                        steamWishlist = steamWishlist,
                        likedGames = likedGames,
                        playedGames = playedGames,
                        isLoadingReviews = isLoadingReviews,
                        isLoadingSteamWishlist = isLoadingSteamWishlist,
                        onLoadMoreReviews = { viewModel.loadMoreReviews(userId) },
                        hasMoreReviews = viewModel.hasMoreReviews(),
                        onNavigateToGame = onNavigateToGame,
                        onSendMessage = { onNavigateToChat(user.uid, user.getDisplayNameOrLegacy()) },
                        modifier = Modifier.padding(padding)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PublicProfileContent(
    user: User,
    reviews: List<ReviewWithGame>,
    steamWishlist: List<SteamAppDetails>,
    likedGames: List<Game>,
    playedGames: List<Game>,
    isLoadingReviews: Boolean,
    isLoadingSteamWishlist: Boolean,
    onLoadMoreReviews: () -> Unit,
    hasMoreReviews: Boolean,
    onNavigateToGame: (String) -> Unit,
    onSendMessage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = listOf(
        stringResource(R.string.reviews),
        stringResource(R.string.steam_wishlist),
        stringResource(R.string.liked_games),
        stringResource(R.string.played_games)
    )
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()
    
    Column(modifier = modifier.fillMaxSize()) {
        // Profile Header
        ProfileHeader(
            user = user,
            reviewCount = reviews.size,
            onSendMessage = onSendMessage
        )
        
        // Tab Row
        TabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    text = {
                        Text(
                            text = title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                )
            }
        }
        
        // Tab Content
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> ReviewsTab(
                    reviews = reviews,
                    isLoading = isLoadingReviews,
                    onLoadMore = onLoadMoreReviews,
                    hasMore = hasMoreReviews,
                    onReviewClick = { review ->
                        review.game?.id?.let { onNavigateToGame(it) }
                    }
                )
                1 -> SteamWishlistTab(
                    wishlist = steamWishlist,
                    isLoading = isLoadingSteamWishlist
                )
                2 -> GamesTab(
                    games = likedGames,
                    emptyMessage = stringResource(R.string.empty_liked),
                    onGameClick = { onNavigateToGame(it.id) }
                )
                3 -> GamesTab(
                    games = playedGames,
                    emptyMessage = stringResource(R.string.empty_played),
                    onGameClick = { onNavigateToGame(it.id) }
                )
            }
        }
    }
}

@Composable
private fun ProfileHeader(
    user: User,
    reviewCount: Int,
    onSendMessage: () -> Unit
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
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Avatar
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(
                    user.photoURL.ifBlank {
                        "https://www.gravatar.com/avatar/00000000000000000000000000000000?d=mp&f=y"
                    }
                )
                .crossfade(true)
                .build(),
            contentDescription = user.getDisplayNameOrLegacy(),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Name and role
        Text(
            text = user.getDisplayNameOrLegacy().ifBlank { "Anonymous" },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Role badge
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (user.role == UserRole.EDITOR) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.secondaryContainer
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (user.role == UserRole.EDITOR) 
                        Icons.Default.Edit 
                    else 
                        Icons.Default.SportsEsports,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (user.role == UserRole.EDITOR) 
                        stringResource(R.string.role_editor) 
                    else 
                        stringResource(R.string.role_player),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
        
        // Bio
        if (user.bio.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = user.bio,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Stats row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                value = reviewCount.toString(),
                label = stringResource(R.string.reviews)
            )
            StatItem(
                value = user.likedGames.size.toString(),
                label = stringResource(R.string.liked_games)
            )
            StatItem(
                value = user.playedGames.size.toString(),
                label = stringResource(R.string.played_games)
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Message button
        OutlinedButton(
            onClick = onSendMessage,
            modifier = Modifier.fillMaxWidth(0.6f)
        ) {
            Icon(
                Icons.Default.Message,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.send_message))
        }
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
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
    onLoadMore: () -> Unit,
    hasMore: Boolean,
    onReviewClick: (ReviewWithGame) -> Unit
) {
    val listState = rememberLazyListState()
    
    // Pagination trigger
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = listState.layoutInfo.totalItemsCount
            lastVisibleItem >= totalItems - 3 && totalItems > 0 && hasMore && !isLoading
        }
    }
    
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            onLoadMore()
        }
    }
    
    if (reviews.isEmpty() && !isLoading) {
        EmptyTabContent(
            icon = Icons.Default.RateReview,
            message = stringResource(R.string.empty_reviews)
        )
    } else {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                items = reviews,
                key = { it.review.id }
            ) { reviewWithGame ->
                ReviewItem(
                    reviewWithGame = reviewWithGame,
                    onClick = { onReviewClick(reviewWithGame) }
                )
            }
            
            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewItem(
    reviewWithGame: ReviewWithGame,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Game image
            AsyncImage(
                model = reviewWithGame.gameImageUrl,
                contentDescription = reviewWithGame.gameTitle,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reviewWithGame.gameTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Rating stars
                Row(verticalAlignment = Alignment.CenterVertically) {
                    repeat(5) { index ->
                        Icon(
                            imageVector = if (index < reviewWithGame.review.rating) 
                                Icons.Default.Star 
                            else 
                                Icons.Default.StarBorder,
                            contentDescription = null,
                            tint = Color(0xFFFFB800),
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun SteamWishlistTab(
    wishlist: List<SteamAppDetails>,
    isLoading: Boolean
) {
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else if (wishlist.isEmpty()) {
        EmptyTabContent(
            icon = Icons.Default.ShoppingCart,
            message = stringResource(R.string.empty_steam_wishlist)
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                items = wishlist,
                key = { it.steamAppId }
            ) { game ->
                SteamWishlistItem(game = game)
            }
        }
    }
}

@Composable
private fun SteamWishlistItem(game: SteamAppDetails) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Game image
            AsyncImage(
                model = game.getBestImageUrl(),
                contentDescription = game.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(100.dp)
                    .height(47.dp) // Steam header aspect ratio
                    .clip(RoundedCornerShape(6.dp))
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = game.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = game.getDeveloperString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Genre
                    Text(
                        text = game.getGenreString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    // Price
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = game.getPriceString(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GamesTab(
    games: List<Game>,
    emptyMessage: String,
    onGameClick: (Game) -> Unit
) {
    if (games.isEmpty()) {
        EmptyTabContent(
            icon = Icons.Default.SportsEsports,
            message = emptyMessage
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                items = games,
                key = { it.id }
            ) { game ->
                GameListItem(
                    game = game,
                    onClick = { onGameClick(game) }
                )
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
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
        ) {
            AsyncImage(
                model = game.posterUrl,
                contentDescription = game.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(80.dp)
                    .fillMaxHeight()
            )
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = game.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = game.genre,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (game.ratingCount > 0) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFB800),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = String.format("%.1f", game.averageRating),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyTabContent(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    message: String
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
