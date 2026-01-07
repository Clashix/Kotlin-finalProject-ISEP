package com.isep.kotlinproject.ui.game

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
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

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var genre by remember { mutableStateOf("") }
    var releaseDate by remember { mutableStateOf("") }
    var developer by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var existingImageUrl by remember { mutableStateOf("") }

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
            existingImageUrl = selectedGame!!.imageUrl
        }
    }

    val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
        imageUri = uri
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (gameId == null) "Add Game" else "Edit Game") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
                        Text("Tap to select image")
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
                            averageRating = selectedGame?.averageRating ?: 0.0,
                            ratingCount = selectedGame?.ratingCount ?: 0
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
