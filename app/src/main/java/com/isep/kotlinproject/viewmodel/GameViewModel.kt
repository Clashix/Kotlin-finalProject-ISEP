package com.isep.kotlinproject.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.isep.kotlinproject.api.SteamStoreItem
import com.isep.kotlinproject.model.Game
import com.isep.kotlinproject.model.Review
import com.isep.kotlinproject.repository.GameRepository
import com.isep.kotlinproject.repository.SteamRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GameViewModel : ViewModel() {
    private val repository = GameRepository()
    private val steamRepository = SteamRepository()
    private val auth = FirebaseAuth.getInstance()

    private var listeningJob: Job? = null

    private val _games = MutableStateFlow<List<Game>>(emptyList())
    val games: StateFlow<List<Game>> = _games.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Game>>(emptyList())
    val searchResults: StateFlow<List<Game>> = _searchResults.asStateFlow()
    
    private val _steamSearchResults = MutableStateFlow<List<SteamStoreItem>>(emptyList())
    val steamSearchResults: StateFlow<List<SteamStoreItem>> = _steamSearchResults.asStateFlow()
    
    private val _selectedGame = MutableStateFlow<Game?>(null)
    val selectedGame: StateFlow<Game?> = _selectedGame.asStateFlow()

    private val _reviews = MutableStateFlow<List<Review>>(emptyList())
    val reviews: StateFlow<List<Review>> = _reviews.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        // We do NOT start listening automatically anymore.
        // It must be triggered externally when the user is logged in.
    }

    fun startListening() {
        // Cancel any existing job to prevent duplicates
        listeningJob?.cancel()
        
        val userId = auth.currentUser?.uid
        if (userId == null) {
            return
        }
        
        listeningJob = viewModelScope.launch {
            _isLoading.value = true
            try {
                // Now passing userId to getGamesFlow
                repository.getGamesFlow(userId).collect { games ->
                    _games.value = games
                    _searchResults.value = games
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _isLoading.value = false
            }
        }
    }
    
    fun stopListening() {
        listeningJob?.cancel()
        listeningJob = null
        _games.value = emptyList()
        _searchResults.value = emptyList()
    }
    
    fun refreshGames() {
        startListening()
    }

    fun searchGames(query: String) {
        if (query.isBlank()) {
            _searchResults.value = _games.value
        } else {
            _searchResults.value = _games.value.filter {
                it.title.contains(query, ignoreCase = true) || it.genre.contains(query, ignoreCase = true)
            }
        }
    }
    
    fun searchSteamGames(query: String) {
        viewModelScope.launch {
            if (query.isBlank()) {
                _steamSearchResults.value = emptyList()
            } else {
                try {
                    val response = steamRepository.searchGames(query)
                    _steamSearchResults.value = response.items
                } catch (e: Exception) {
                    _steamSearchResults.value = emptyList()
                    e.printStackTrace()
                }
            }
        }
    }
    
    fun clearSteamSearch() {
        _steamSearchResults.value = emptyList()
    }

    fun selectGame(gameId: String) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            val game = repository.getGame(userId, gameId)
            _selectedGame.value = game
            _reviews.value = game?.reviews ?: emptyList()
        }
    }

    private fun loadReviews(gameId: String) {
        // Reviews are now embedded in the Game object, so no need to fetch separately.
    }

    fun addGame(game: Game, imageUri: Uri?, onComplete: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            // Inject userId here
            var finalGame = game.copy(userId = auth.currentUser?.uid ?: "")
            if (imageUri != null) {
                val imageUrl = repository.uploadGameImage(imageUri)
                finalGame = finalGame.copy(imageUrl = imageUrl)
            }
            repository.addGame(finalGame)
            _isLoading.value = false
            onComplete()
        }
    }

    fun updateGame(game: Game, imageUri: Uri?, onComplete: () -> Unit) {
        viewModelScope.launch {
             _isLoading.value = true
             // Ensure userId is preserved or set if missing (though usually it's already there)
            var finalGame = game
            if (finalGame.userId.isBlank()) {
                finalGame = finalGame.copy(userId = auth.currentUser?.uid ?: "")
            }
            
            if (imageUri != null) {
                val imageUrl = repository.uploadGameImage(imageUri)
                finalGame = finalGame.copy(imageUrl = imageUrl)
            }
            repository.updateGame(finalGame)
             _isLoading.value = false
            onComplete()
        }
    }

    fun deleteGame(gameId: String, onComplete: () -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            repository.deleteGame(userId, gameId)
            onComplete()
        }
    }

    fun addReview(gameId: String, rating: Double, comment: String) {
        val userId = auth.currentUser?.uid ?: return
        val userName = auth.currentUser?.displayName ?: "You"
        
        viewModelScope.launch {
            val review = Review(
                gameId = gameId,
                userId = userId,
                userName = userName,
                rating = rating,
                comment = comment
            )
            repository.addReview(review)
            
            // Refresh game details to update rating and reviews list
            val updatedGame = repository.getGame(userId, gameId)
            _selectedGame.value = updatedGame
            _reviews.value = updatedGame?.reviews ?: emptyList()
        }
    }
}