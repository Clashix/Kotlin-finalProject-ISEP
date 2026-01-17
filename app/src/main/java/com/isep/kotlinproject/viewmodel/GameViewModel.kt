package com.isep.kotlinproject.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.isep.kotlinproject.api.SteamStoreItem
import com.isep.kotlinproject.model.Game
import com.isep.kotlinproject.model.Review
import com.isep.kotlinproject.model.UserRole
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
    private var reviewsJob: Job? = null

    // Current user info
    private val _currentUserRole = MutableStateFlow<UserRole?>(null)
    val currentUserRole: StateFlow<UserRole?> = _currentUserRole.asStateFlow()
    
    private val _currentUserName = MutableStateFlow("")
    val currentUserName: StateFlow<String> = _currentUserName.asStateFlow()

    // Games
    private val _games = MutableStateFlow<List<Game>>(emptyList())
    val games: StateFlow<List<Game>> = _games.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Game>>(emptyList())
    val searchResults: StateFlow<List<Game>> = _searchResults.asStateFlow()
    
    private val _steamSearchResults = MutableStateFlow<List<SteamStoreItem>>(emptyList())
    val steamSearchResults: StateFlow<List<SteamStoreItem>> = _steamSearchResults.asStateFlow()
    
    private val _selectedGame = MutableStateFlow<Game?>(null)
    val selectedGame: StateFlow<Game?> = _selectedGame.asStateFlow()

    // Reviews
    private val _reviews = MutableStateFlow<List<Review>>(emptyList())
    val reviews: StateFlow<List<Review>> = _reviews.asStateFlow()
    
    private val _userReview = MutableStateFlow<Review?>(null)
    val userReview: StateFlow<Review?> = _userReview.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /**
     * Set the current user's role and name (called from MainActivity after auth)
     */
    fun setUserInfo(role: UserRole, name: String) {
        _currentUserRole.value = role
        _currentUserName.value = name
    }

    /**
     * Start listening to games - all games for players, editor's games for editors
     */
    fun startListening(isEditor: Boolean = false, editorId: String? = null) {
        listeningJob?.cancel()
        
        listeningJob = viewModelScope.launch {
            _isLoading.value = true
            try {
                val flow = if (isEditor && editorId != null) {
                    repository.getGamesByEditorFlow(editorId)
                } else {
                    repository.getAllGamesFlow()
                }
                
                flow.collect { games ->
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
        reviewsJob?.cancel()
        reviewsJob = null
        _games.value = emptyList()
        _searchResults.value = emptyList()
    }
    
    fun refreshGames() {
        // Re-collect from flow will happen automatically
    }

    fun searchGames(query: String) {
        if (query.isBlank()) {
            _searchResults.value = _games.value
        } else {
            _searchResults.value = _games.value.filter {
                it.title.contains(query, ignoreCase = true) || 
                it.genre.contains(query, ignoreCase = true) ||
                it.developer.contains(query, ignoreCase = true)
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

    /**
     * Select a game and load its reviews
     */
    fun selectGame(gameId: String) {
        reviewsJob?.cancel()
        
        viewModelScope.launch {
            _isLoading.value = true
            val game = repository.getGame(gameId)
            _selectedGame.value = game
            
            // Load user's existing review
            val userId = auth.currentUser?.uid
            if (userId != null && game != null) {
                _userReview.value = repository.getUserReviewForGame(gameId, userId)
            }
            
            _isLoading.value = false
        }
        
        // Listen to reviews in real-time
        reviewsJob = viewModelScope.launch {
            repository.getReviewsForGameFlow(gameId).collect { reviews ->
                _reviews.value = reviews
            }
        }
    }
    
    fun clearSelectedGame() {
        reviewsJob?.cancel()
        _selectedGame.value = null
        _reviews.value = emptyList()
        _userReview.value = null
    }

    // ============== EDITOR FUNCTIONS ==============
    
    /**
     * Add a new game (Editor only)
     */
    fun addGame(game: Game, imageUri: Uri?, onComplete: () -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        val userName = _currentUserName.value
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                var imageUrl = game.imageUrl
                if (imageUri != null) {
                    imageUrl = repository.uploadGameImage(imageUri)
                }
                
                val gameToAdd = game.copy(
                    editorId = userId,
                    editorName = userName,
                    imageUrl = imageUrl
                )
                
                repository.addGame(gameToAdd)
                onComplete()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Update an existing game (Editor only)
     */
    fun updateGame(game: Game, imageUri: Uri?, onComplete: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                var imageUrl = game.imageUrl
                if (imageUri != null) {
                    imageUrl = repository.uploadGameImage(imageUri)
                }
                
                val gameToUpdate = game.copy(imageUrl = imageUrl)
                repository.updateGame(gameToUpdate)
                onComplete()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Delete a game (Editor only)
     */
    fun deleteGame(gameId: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                repository.deleteGame(gameId)
                onComplete()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // ============== PLAYER FUNCTIONS ==============
    
    /**
     * Submit a star rating (1-5) with optional comment (Player only)
     */
    fun submitReview(gameId: String, rating: Int, comment: String) {
        val userId = auth.currentUser?.uid ?: return
        val userName = _currentUserName.value.ifEmpty { auth.currentUser?.displayName ?: "Anonymous" }
        
        // Ensure rating is between 1 and 5
        val validRating = rating.coerceIn(1, 5)
        
        viewModelScope.launch {
            try {
                val review = Review(
                    gameId = gameId,
                    userId = userId,
                    userName = userName,
                    rating = validRating,
                    comment = comment
                )
                repository.addOrUpdateReview(review)
                
                // Refresh user's review
                _userReview.value = repository.getUserReviewForGame(gameId, userId)
                
                // Refresh game to update stats
                _selectedGame.value = repository.getGame(gameId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Delete user's own review (Player only)
     */
    fun deleteMyReview(gameId: String) {
        val currentReview = _userReview.value ?: return
        
        viewModelScope.launch {
            try {
                repository.deleteReview(gameId, currentReview.id, currentReview.rating)
                _userReview.value = null
                
                // Refresh game to update stats
                _selectedGame.value = repository.getGame(gameId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
