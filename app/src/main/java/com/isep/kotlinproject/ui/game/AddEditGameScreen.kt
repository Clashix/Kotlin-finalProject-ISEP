package com.isep.kotlinproject.ui.game

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.isep.kotlinproject.api.SteamStoreItem
import com.isep.kotlinproject.model.Game
import com.isep.kotlinproject.viewmodel.GameViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditGameScreen(
    gameId: String? = null,
    viewModel: GameViewModel,
    onBack: () -> Unit
) {
    val selectedGame by viewModel.selectedGame.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val steamSearchResults by viewModel.steamSearchResults.collectAsState()

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var genre by remember { mutableStateOf("") }
    var releaseDate by remember { mutableStateOf("") }
    var developer by remember { mutableStateOf("") }
    var steamAppId by remember { mutableStateOf<String?>(null) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var existingImageUrl by remember { mutableStateOf("") }
    
    var showSteamSearch by remember { mutableStateOf(false) }

    LaunchedEffect(gameId) {
        if (gameId != null) {
            viewModel.selectGame(gameId)
        }
    }

    LaunchedEffect(selectedGame) {
        if (gameId != null && selectedGame != null && selectedGame!!.id == gameId) {
            title = selectedGame!!.title
            description = selectedGame!!.description
            genre = selectedGame!!.genre
            releaseDate = selectedGame!!.releaseDate
            developer = selectedGame!!.developer
            steamAppId = selectedGame!!.steamAppId
            existingImageUrl = selectedGame!!.imageUrl
        }
    }

    val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
        imageUri = uri
    }
    
    if (showSteamSearch) {
        SteamSearchDialog(
            viewModel = viewModel,
            searchResults = steamSearchResults,
            onDismiss = { 
                showSteamSearch = false 
                viewModel.clearSteamSearch()
            },
            onGameSelected = { steamGame ->
                title = steamGame.name
                // Steam API doesn't give full description/genre/dev in search results easily
                // We'd need another call for details, but for now let's just use what we have.
                // We can construct a high-res image URL from the ID if needed, 
                // or use the tiny_image provided.
                // High res header: https://cdn.akamai.steamstatic.com/steam/apps/{id}/header.jpg
                existingImageUrl = "https://cdn.akamai.steamstatic.com/steam/apps/${steamGame.id}/header.jpg"
                steamAppId = steamGame.id.toString()
                showSteamSearch = false
                viewModel.clearSteamSearch()
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (gameId == null) "Add Game" else "Edit Game") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (gameId == null) {
                         IconButton(onClick = { showSteamSearch = true }) {
                             Icon(Icons.Default.Search, contentDescription = "Search Steam")
                         }
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clickable { launcher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (imageUri != null) {
                        AsyncImage(
                            model = imageUri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else if (existingImageUrl.isNotEmpty()) {
                        AsyncImage(
                            model = existingImageUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Tap to select image")
                            if (existingImageUrl.isEmpty()) {
                                Text("(or Search Steam to auto-fill)", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = genre,
                    onValueChange = { genre = it },
                    label = { Text("Genre") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = developer,
                    onValueChange = { developer = it },
                    label = { Text("Developer") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = releaseDate,
                    onValueChange = { releaseDate = it },
                    label = { Text("Release Date (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                Button(
                    onClick = {
                        val game = Game(
                            id = gameId ?: "",
                            title = title,
                            description = description,
                            genre = genre,
                            releaseDate = releaseDate,
                            developer = developer,
                            imageUrl = existingImageUrl, // Will be overwritten if imageUri is not null
                            steamAppId = steamAppId,
                            // Stats are preserved from existing game if editing
                            averageRating = selectedGame?.averageRating ?: 0.0,
                            ratingCount = selectedGame?.ratingCount ?: 0,
                            totalRatingSum = selectedGame?.totalRatingSum ?: 0
                        )
                        if (gameId == null) {
                            viewModel.addGame(game, imageUri, onBack)
                        } else {
                            viewModel.updateGame(game, imageUri, onBack)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = title.isNotBlank()
                ) {
                    Text(if (gameId == null) "Add Game" else "Update Game")
                }
            }
        }
    }
}

@Composable
fun SteamSearchDialog(
    viewModel: GameViewModel,
    searchResults: List<SteamStoreItem>,
    onDismiss: () -> Unit,
    onGameSelected: (SteamStoreItem) -> Unit
) {
    var query by remember { mutableStateOf("") }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(500.dp)
                .padding(16.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Search Steam", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { 
                        query = it
                        viewModel.searchSteamGames(it)
                    },
                    label = { Text("Game Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(searchResults) { game ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onGameSelected(game) }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                             AsyncImage(
                                model = "https://cdn.akamai.steamstatic.com/steam/apps/${game.id}/capsule_sm_120.jpg",
                                contentDescription = null,
                                modifier = Modifier.size(60.dp, 30.dp),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(game.name, style = MaterialTheme.typography.bodyMedium)
                                Text("ID: ${game.id}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}