package com.isep.kotlinproject.ui.game

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.isep.kotlinproject.R
import com.isep.kotlinproject.api.SteamStoreItem
import com.isep.kotlinproject.model.Game
import com.isep.kotlinproject.ui.components.AppDatePickerField
import com.isep.kotlinproject.ui.components.AppTextField
import com.isep.kotlinproject.viewmodel.GameViewModel

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AddEditGameScreen(
    gameId: String? = null,
    viewModel: GameViewModel,
    onBack: () -> Unit
) {
    val selectedGame by viewModel.selectedGame.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val steamSearchResults by viewModel.steamSearchResults.collectAsState()

    // Form State
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var genre by remember { mutableStateOf("") }
    var releaseDate by remember { mutableStateOf("") }
    var developer by remember { mutableStateOf("") }
    var steamAppId by remember { mutableStateOf<String?>(null) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var existingImageUrl by remember { mutableStateOf("") }
    
    // Validation State
    var isSubmitted by remember { mutableStateOf(false) }
    
    // Logic
    val isTitleValid = title.isNotBlank()
    // Genre is valid if the list is not empty
    var selectedGenres by remember { mutableStateOf<List<String>>(emptyList()) }
    val isGenreValid = selectedGenres.isNotEmpty()
    val isDateValid = releaseDate.isNotBlank()
    
    var showSteamSearch by remember { mutableStateOf(false) }
    var showGenreDialog by remember { mutableStateOf(false) }

    val predefinedGenres = listOf(
        "Action", "Adventure", "RPG", "Strategy", "Shooter", 
        "Puzzle", "Simulation", "Sports", "Racing", "Horror", 
        "Indie", "Fighting", "Platformer", "Open World", "Survival", 
        "Sandbox", "Battle Royale", "Visual Novel", "MMO"
    )

    LaunchedEffect(gameId) {
        if (gameId != null) {
            viewModel.selectGame(gameId)
        }
    }

    LaunchedEffect(selectedGame) {
        if (gameId != null && selectedGame != null && selectedGame!!.id == gameId) {
            title = selectedGame!!.title
            description = selectedGame!!.description
            // Split the string "Action - RPG" back into a list
            selectedGenres = selectedGame!!.genre.split(" - ").filter { it.isNotBlank() }
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
                existingImageUrl = "https://cdn.akamai.steamstatic.com/steam/apps/${steamGame.id}/header.jpg"
                steamAppId = steamGame.id.toString()
                showSteamSearch = false
                viewModel.clearSteamSearch()
            }
        )
    }

    // Genre Selection Dialog
    if (showGenreDialog) {
        AlertDialog(
            onDismissRequest = { showGenreDialog = false },
            title = { Text(stringResource(R.string.select_categories)) },
            text = {
                var newCustomGenre by remember { mutableStateOf("") }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(stringResource(R.string.popular_genres), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        predefinedGenres.forEach { genre ->
                            val isSelected = selectedGenres.contains(genre)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedGenres = if (isSelected) {
                                        selectedGenres - genre
                                    } else {
                                        selectedGenres + genre
                                    }
                                },
                                label = { Text(genre) }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(stringResource(R.string.custom_category), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        OutlinedTextField(
                            value = newCustomGenre,
                            onValueChange = { newCustomGenre = it },
                            label = { Text(stringResource(R.string.add_other)) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        IconButton(
                            onClick = { 
                                if (newCustomGenre.isNotBlank() && !selectedGenres.contains(newCustomGenre.trim())) {
                                    selectedGenres = selectedGenres + newCustomGenre.trim()
                                    newCustomGenre = ""
                                }
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_game))
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showGenreDialog = false }) {
                    Text(stringResource(R.string.done))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (gameId == null) stringResource(R.string.add_game) else stringResource(R.string.edit_game)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    if (gameId == null) {
                        TextButton(onClick = { showSteamSearch = true }) {
                            Icon(Icons.Default.Search, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.import_from_steam))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
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
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Image Picker Section
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clickable { launcher.launch("image/*") }
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (imageUri != null) {
                            AsyncImage(
                                model = imageUri,
                                contentDescription = stringResource(R.string.cd_selected_image),
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else if (existingImageUrl.isNotEmpty()) {
                            AsyncImage(
                                model = existingImageUrl,
                                contentDescription = stringResource(R.string.cd_existing_image),
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.AddPhotoAlternate, 
                                    contentDescription = null, 
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(stringResource(R.string.tap_to_add_cover), style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }

                Text(
                    stringResource(R.string.game_details), 
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                AppTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = stringResource(R.string.game_title),
                    leadingIcon = Icons.Default.Title,
                    helpText = stringResource(R.string.title_help),
                    errorMessage = if (isSubmitted && !isTitleValid) stringResource(R.string.error) else null
                )

                // Category / Genre Section
                Column {
                    Text(
                        text = stringResource(R.string.categories), 
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { showGenreDialog = true },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, if (isSubmitted && !isGenreValid) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline),
                        shape = RoundedCornerShape(4.dp) // Match TextField shape roughly
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                             if (selectedGenres.isEmpty()) {
                                Text(
                                    stringResource(R.string.select_categories),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                @OptIn(ExperimentalLayoutApi::class)
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    selectedGenres.forEach { genre ->
                                        InputChip(
                                            selected = true,
                                            onClick = { /* Clicking chip inside could open dialog or do nothing */ showGenreDialog = true },
                                            label = { Text(genre) },
                                            trailingIcon = {
                                                Icon(
                                                    Icons.Default.Close,
                                                    contentDescription = stringResource(R.string.delete),
                                                    modifier = Modifier.size(16.dp).clickable { selectedGenres = selectedGenres - genre }
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (isSubmitted && !isGenreValid) {
                        Text(
                            text = stringResource(R.string.error_category_required),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                        )
                    }
                }

                AppTextField(
                    value = developer,
                    onValueChange = { developer = it },
                    label = stringResource(R.string.developer),
                    leadingIcon = Icons.Default.Person,
                    helpText = stringResource(R.string.developer_help)
                )

                AppDatePickerField(
                    value = releaseDate,
                    onDateSelected = { releaseDate = it },
                    label = stringResource(R.string.release_date),
                    helpText = stringResource(R.string.release_date_help),
                    errorMessage = if (isSubmitted && !isDateValid) stringResource(R.string.error) else null
                )

                AppTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = stringResource(R.string.game_description),
                    minLines = 3,
                    singleLine = false,
                    helpText = stringResource(R.string.description_help)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        isSubmitted = true
                        if (isTitleValid && isGenreValid && isDateValid) {
                            // Join genres with " - "
                            val finalGenreString = selectedGenres.joinToString(separator = " - ")
                            
                            val game = Game(
                                id = gameId ?: "",
                                title = title,
                                description = description,
                                genre = finalGenreString,
                                releaseDate = releaseDate,
                                developer = developer,
                                imageUrl = existingImageUrl, 
                                steamAppId = steamAppId,
                                averageRating = selectedGame?.averageRating ?: 0.0,
                                ratingCount = selectedGame?.ratingCount ?: 0,
                                totalRatingSum = selectedGame?.totalRatingSum ?: 0
                            )
                            if (gameId == null) {
                                viewModel.addGame(game, imageUri, onBack)
                            } else {
                                viewModel.updateGame(game, imageUri, onBack)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(if (gameId == null) stringResource(R.string.add_game) else stringResource(R.string.save))
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
                .height(600.dp)
                .padding(16.dp),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    stringResource(R.string.import_from_steam), 
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = query,
                    onValueChange = { 
                        query = it
                        viewModel.searchSteamGames(it)
                    },
                    label = { Text(stringResource(R.string.search_by_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    shape = CircleShape
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(searchResults) { game ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.medium)
                                .clickable { onGameSelected(game) }
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                             AsyncImage(
                                model = "https://cdn.akamai.steamstatic.com/steam/apps/${game.id}/capsule_sm_120.jpg",
                                contentDescription = null,
                                modifier = Modifier
                                    .width(80.dp)
                                    .height(40.dp)
                                    .clip(MaterialTheme.shapes.small),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    game.name, 
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "ID: ${game.id}", 
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
                
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    }
}