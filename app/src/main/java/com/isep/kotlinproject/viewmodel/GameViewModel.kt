package com.isep.kotlinproject.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.isep.kotlinproject.model.Game
import com.isep.kotlinproject.model.Review
import com.isep.kotlinproject.repository.GameRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GameViewModel : ViewModel() {
    private val repository = GameRepository()

    private val _games = MutableStateFlow<List<Game>>(emptyList())
    val games: StateFlow<List<Game>> = _games.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Game>>(emptyList())
    val searchResults: StateFlow<List<Game>> = _searchResults.asStateFlow()
    
    private val _selectedGame = MutableStateFlow<Game?>(null)
    val selectedGame: StateFlow<Game?> = _selectedGame.asStateFlow()

    private val _reviews = MutableStateFlow<List<Review>>(emptyList())
    val reviews: StateFlow<List<Review>> = _reviews.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadGames()
        // Try to seed data if empty (only for demo purposes, can be removed)
        viewModelScope.launch {
            repository.seedData()
            loadGames()
        }
    }

    fun loadGames() {
        viewModelScope.launch {
            _isLoading.value = true
            val loadedGames = repository.getGames()
            _games.value = loadedGames
            _searchResults.value = loadedGames
            _isLoading.value = false
        }
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

    fun selectGame(gameId: String) {
        viewModelScope.launch {
            _selectedGame.value = repository.getGame(gameId)
            loadReviews(gameId)
        }
    }

    private fun loadReviews(gameId: String) {
        viewModelScope.launch {
            _reviews.value = repository.getReviewsForGame(gameId)
        }
    }

    fun addGame(game: Game, imageUri: Uri?, onComplete: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            var finalGame = game
            if (imageUri != null) {
                val imageUrl = repository.uploadGameImage(imageUri)
                finalGame = game.copy(imageUrl = imageUrl)
            }
            repository.addGame(finalGame)
            loadGames()
            _isLoading.value = false
            onComplete()
        }
    }

    fun updateGame(game: Game, imageUri: Uri?, onComplete: () -> Unit) {
        viewModelScope.launch {
             _isLoading.value = true
            var finalGame = game
            if (imageUri != null) {
                val imageUrl = repository.uploadGameImage(imageUri)
                finalGame = game.copy(imageUrl = imageUrl)
            }
            repository.updateGame(finalGame)
            loadGames()
             _isLoading.value = false
            onComplete()
        }
    }

    fun deleteGame(gameId: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.deleteGame(gameId)
            loadGames()
            onComplete()
        }
    }

    fun addReview(gameId: String, userId: String, userName: String, rating: Double, comment: String) {
        viewModelScope.launch {
            val review = Review(
                gameId = gameId,
                userId = userId,
                userName = userName,
                rating = rating,
                comment = comment
            )
            repository.addReview(review)
            loadReviews(gameId)
            // Refresh game details to update rating
            _selectedGame.value = repository.getGame(gameId)
        }
    }
}
