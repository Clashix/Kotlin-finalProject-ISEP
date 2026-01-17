package com.isep.kotlinproject.ui.game

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.isep.kotlinproject.model.Game
import com.isep.kotlinproject.model.UserRole
import com.isep.kotlinproject.viewmodel.GameViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameListScreen(
    viewModel: GameViewModel,
    userRole: UserRole,
    onGameClick: (String) -> Unit,
    onAddGameClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    val filteredGames by viewModel.searchResults.collectAsState()
    val allGames by viewModel.games.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    
    val isEditor = userRole == UserRole.EDITOR

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(if (isEditor) "My Games (Editor)" else "Discover Games") 
                },
                actions = {
                    // Only editors can add games
                    if (isEditor) {
                        IconButton(onClick = onAddGameClick) {
                            Icon(Icons.Default.Add, contentDescription = "Add Game")
                        }
                    }
                    IconButton(onClick = onProfileClick) {
                        Icon(Icons.Default.Person, contentDescription = "Profile")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading && allGames.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(modifier = Modifier.padding(padding)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        viewModel.searchGames(it)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    placeholder = { Text("Search games...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                )

                if (allGames.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isEditor) "No games yet. Tap + to add your first game!" 
                                   else "No games available yet.",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Top Rated Section (only for players viewing all games)
                        if (!isEditor && allGames.size > 5) {
                            item {
                                Text(
                                    text = "Top Rated",
                                    style = MaterialTheme.typography.titleLarge,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                            item {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(bottom = 16.dp)
                                ) {
                                    // Sort by average rating
                                    val topRatedGames = allGames
                                        .filter { it.ratingCount > 0 }
                                        .sortedByDescending { it.averageRating }
                                        .take(5)
                                    items(topRatedGames.ifEmpty { allGames.take(5) }) { game ->
                                        GameCard(game = game, onClick = { onGameClick(game.id) })
                                    }
                                }
                            }
                            item {
                                Text(
                                    text = "All Games",
                                    style = MaterialTheme.typography.titleLarge,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }

                        items(filteredGames) { game ->
                            GameItem(game = game, onClick = { onGameClick(game.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StarRatingBar(
    rating: Double,
    maxStars: Int = 5,
    starSize: Int = 16
) {
    val filledStars = rating.toInt()
    val hasHalfStar = (rating - filledStars) >= 0.5
    
    Row(verticalAlignment = Alignment.CenterVertically) {
        repeat(maxStars) { index ->
            val starColor = when {
                index < filledStars -> Color(0xFFFFB800) // Gold for filled
                index == filledStars && hasHalfStar -> Color(0xFFFFB800).copy(alpha = 0.5f)
                else -> Color.Gray.copy(alpha = 0.3f)
            }
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = starColor,
                modifier = Modifier.size(starSize.dp)
            )
        }
    }
}

@Composable
fun GameCard(game: Game, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            AsyncImage(
                model = game.posterUrl,
                contentDescription = game.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = game.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    StarRatingBar(rating = game.averageRating, starSize = 14)
                    if (game.ratingCount > 0) {
                        Text(
                            text = "(${game.ratingCount})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GameItem(game: Game, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(modifier = Modifier.padding(8.dp)) {
            AsyncImage(
                model = game.posterUrl,
                contentDescription = game.title,
                modifier = Modifier
                    .size(80.dp)
                    .padding(end = 8.dp),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(text = game.title, style = MaterialTheme.typography.titleMedium)
                Text(text = game.genre, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = "by ${game.developer}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StarRatingBar(rating = game.averageRating, starSize = 16)
                    Text(
                        text = if (game.ratingCount > 0) 
                            String.format("%.1f", game.averageRating) + " (${game.ratingCount})"
                        else 
                            "No ratings",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
