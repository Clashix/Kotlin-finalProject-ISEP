package com.isep.kotlinproject.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.isep.kotlinproject.api.SteamStoreItem
import com.isep.kotlinproject.model.Badge
import com.isep.kotlinproject.model.Game
import com.isep.kotlinproject.model.Review
import com.isep.kotlinproject.model.UserRole
import com.isep.kotlinproject.repository.BadgeRepository
import com.isep.kotlinproject.repository.GameHistoryRepository
import com.isep.kotlinproject.repository.GameRepository
import com.isep.kotlinproject.repository.GameSortOption
import com.isep.kotlinproject.repository.SteamRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel responsible for managing game-related data and operations.
 * 
 * This ViewModel serves as the central hub for game catalog management,
 * supporting both Player and Editor user roles with appropriate functionality:
 * 
 * **Player Capabilities:**
 * - Browse and search the game catalog
 * - View game details and reviews
 * - Submit, edit, and delete personal reviews
 * - Receive badge notifications for achievements
 * 
 * **Editor Capabilities:**
 * - All player capabilities
 * - Create, update, and delete games
 * - Upload game cover images
 * - View edit history for audit purposes
 * 
 * The ViewModel maintains real-time listeners for both games and reviews,
 * automatically updating the UI when data changes in Firestore.
 * 
 * @see GameRepository for data access layer implementation.
 * @see GameHistoryRepository for edit history tracking.
 * @see BadgeRepository for achievement system integration.
 */
class GameViewModel : ViewModel() {
    
    // Repository instances for data access
    private val repository = GameRepository()
    private val steamRepository = SteamRepository()
    private val historyRepository = GameHistoryRepository()
    private val badgeRepository = BadgeRepository()
    private val auth = FirebaseAuth.getInstance()

    // Coroutine jobs for cancellable real-time listeners
    private var listeningJob: Job? = null
    private var reviewsJob: Job? = null

    // ======================== USER STATE ========================
    
    private val _currentUserRole = MutableStateFlow<UserRole?>(null)
    /** The current user's role, determining available features. */
    val currentUserRole: StateFlow<UserRole?> = _currentUserRole.asStateFlow()
    
    private val _currentUserName = MutableStateFlow("")
    /** The current user's display name for review attribution. */
    val currentUserName: StateFlow<String> = _currentUserName.asStateFlow()

    // ======================== GAMES STATE ========================
    
    private val _games = MutableStateFlow<List<Game>>(emptyList())
    /** Complete list of games from the active Firestore listener. */
    val games: StateFlow<List<Game>> = _games.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Game>>(emptyList())
    /** Filtered/sorted subset of games based on search and sort criteria. */
    val searchResults: StateFlow<List<Game>> = _searchResults.asStateFlow()
    
    private val _currentSortOption = MutableStateFlow(GameSortOption.TITLE_ASC)
    /** Current sorting algorithm applied to the games list. */
    val currentSortOption: StateFlow<GameSortOption> = _currentSortOption.asStateFlow()
    
    private val _steamSearchResults = MutableStateFlow<List<SteamStoreItem>>(emptyList())
    /** Results from Steam API search for game import functionality. */
    val steamSearchResults: StateFlow<List<SteamStoreItem>> = _steamSearchResults.asStateFlow()
    
    private val _selectedGame = MutableStateFlow<Game?>(null)
    /** Currently selected game for detail view. */
    val selectedGame: StateFlow<Game?> = _selectedGame.asStateFlow()

    // ======================== REVIEWS STATE ========================
    
    private val _reviews = MutableStateFlow<List<Review>>(emptyList())
    /** All reviews for the currently selected game. */
    val reviews: StateFlow<List<Review>> = _reviews.asStateFlow()
    
    private val _userReview = MutableStateFlow<Review?>(null)
    /** The current user's review for the selected game, if one exists. */
    val userReview: StateFlow<Review?> = _userReview.asStateFlow()

    // ======================== UI STATE ========================
    
    private val _isLoading = MutableStateFlow(false)
    /** Indicates whether a data operation is in progress. */
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _newBadgeAwarded = MutableStateFlow<Badge?>(null)
    /** Newly awarded badge to display in a celebration notification. */
    val newBadgeAwarded: StateFlow<Badge?> = _newBadgeAwarded.asStateFlow()

    // ======================== USER CONFIGURATION ========================

    /**
     * Configures the current user's identity for the ViewModel.
     * 
     * This must be called after authentication to enable role-based
     * functionality and proper attribution of reviews.
     * 
     * @param role The user's role (PLAYER or EDITOR).
     * @param name The user's display name for review attribution.
     */
    fun setUserInfo(role: UserRole, name: String) {
        _currentUserRole.value = role
        _currentUserName.value = name
    }

    // ======================== REAL-TIME LISTENERS ========================

    /**
     * Initiates a real-time listener for games in Firestore.
     * 
     * The listener behavior varies based on user role:
     * - **Players**: Receives updates for all published games.
     * - **Editors**: Receives updates only for games they created.
     * 
     * Previous listeners are automatically cancelled to prevent memory leaks.
     * 
     * @param isEditor Whether to filter games by the current editor.
     * @param editorId The editor's user ID for filtering (required if isEditor is true).
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
    
    /**
     * Cancels all active Firestore listeners and clears cached data.
     * 
     * Call this when navigating away from screens that display game data
     * to prevent unnecessary network activity and memory usage.
     */
    fun stopListening() {
        listeningJob?.cancel()
        listeningJob = null
        reviewsJob?.cancel()
        reviewsJob = null
        _games.value = emptyList()
        _searchResults.value = emptyList()
    }
    
    /**
     * Triggers a refresh of the games data.
     * 
     * With Flow-based listeners, data refresh happens automatically.
     * This method exists for compatibility with pull-to-refresh gestures.
     */
    fun refreshGames() {
        // Re-collect from flow will happen automatically
    }

    // ======================== SEARCH FUNCTIONALITY ========================

    /**
     * Filters the games list based on a search query.
     * 
     * The search is performed client-side on already-loaded data for
     * instant results. Matches are found in title, genre, or developer name.
     * 
     * @param query The search string (case-insensitive). Empty resets to full list.
     */
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
    
    /**
     * Searches the Steam Store API for games to import.
     * 
     * This enables editors to import game metadata from Steam by searching
     * for games and selecting from the results.
     * 
     * @param query The search term to send to Steam's API.
     */
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
    
    /**
     * Clears Steam search results when the search dialog is dismissed.
     */
    fun clearSteamSearch() {
        _steamSearchResults.value = emptyList()
    }

    // ======================== GAME SELECTION ========================

    /**
     * Loads a game's full details and initiates a real-time review listener.
     * 
     * This method fetches the complete game document and the current user's
     * review (if one exists), then establishes a listener for all reviews.
     * 
     * @param gameId The Firestore document ID of the game to load.
     */
    fun selectGame(gameId: String) {
        reviewsJob?.cancel()
        
        viewModelScope.launch {
            _isLoading.value = true
            val game = repository.getGame(gameId)
            _selectedGame.value = game
            
            val userId = auth.currentUser?.uid
            if (userId != null && game != null) {
                _userReview.value = repository.getUserReviewForGame(gameId, userId)
            }
            
            _isLoading.value = false
        }
        
        reviewsJob = viewModelScope.launch {
            repository.getReviewsForGameFlow(gameId).collect { reviews ->
                _reviews.value = reviews
            }
        }
    }
    
    /**
     * Clears the currently selected game and cancels review listeners.
     * 
     * Call this when navigating away from the game detail screen.
     */
    fun clearSelectedGame() {
        reviewsJob?.cancel()
        _selectedGame.value = null
        _reviews.value = emptyList()
        _userReview.value = null
    }

    // ======================== EDITOR: GAME MANAGEMENT ========================

    /**
     * Creates a new game in the catalog.
     * 
     * **Editor Role Required**
     * 
     * This method handles the complete game creation flow including:
     * - Uploading cover image to Firebase Storage (if provided)
     * - Creating the Firestore document with editor attribution
     * - Recording the action in the edit history for auditing
     * 
     * @param game The game data (editorId will be set automatically).
     * @param imageUri Optional local URI of the cover image to upload.
     * @param onComplete Callback invoked when the operation completes.
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
                
                val gameId = repository.addGame(gameToAdd)
                historyRepository.recordCreate(gameToAdd.copy(id = gameId))
                
                onComplete()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Updates an existing game's information.
     * 
     * **Editor Role Required** (ownership verified server-side)
     * 
     * This method handles image upload if a new image is provided,
     * and records the changes in the edit history with before/after comparison.
     * 
     * @param game The updated game data.
     * @param imageUri Optional new cover image to upload.
     * @param onComplete Callback invoked when the operation completes.
     */
    fun updateGame(game: Game, imageUri: Uri?, onComplete: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val oldGame = repository.getGame(game.id)
                
                var imageUrl = game.imageUrl
                if (imageUri != null) {
                    imageUrl = repository.uploadGameImage(imageUri)
                }
                
                val gameToUpdate = game.copy(imageUrl = imageUrl)
                repository.updateGame(gameToUpdate)
                
                if (oldGame != null) {
                    historyRepository.recordUpdate(oldGame, gameToUpdate)
                }
                
                onComplete()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Permanently deletes a game from the catalog.
     * 
     * **Editor Role Required** (ownership verified server-side)
     * 
     * The deletion is recorded in edit history before the document is removed.
     * Associated reviews are also deleted (handled by repository).
     * 
     * @param gameId The Firestore document ID of the game to delete.
     * @param onComplete Callback invoked when the operation completes.
     */
    fun deleteGame(gameId: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                val game = repository.getGame(gameId)
                if (game != null) {
                    historyRepository.recordDelete(game)
                }
                
                repository.deleteGame(gameId)
                onComplete()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    // ======================== SORTING ========================

    /**
     * Changes the sorting algorithm for the games list.
     * 
     * Available options include alphabetical, rating-based, review count,
     * and a trending algorithm that combines multiple factors.
     * 
     * @param option The [GameSortOption] to apply.
     */
    fun setSortOption(option: GameSortOption) {
        _currentSortOption.value = option
        applySorting()
    }
    
    /**
     * Applies the current sort option to the games list.
     * 
     * The trending algorithm uses a weighted formula:
     * - Review count × 2 (activity factor)
     * - Average rating × 10 (quality factor)
     * - Bonus for games with 5+ or 10+ reviews (credibility factor)
     */
    private fun applySorting() {
        val sorted = when (_currentSortOption.value) {
            GameSortOption.TITLE_ASC -> _games.value.sortedBy { it.title.lowercase() }
            GameSortOption.TITLE_DESC -> _games.value.sortedByDescending { it.title.lowercase() }
            GameSortOption.HIGHEST_RATED -> _games.value.sortedByDescending { it.averageRating }
            GameSortOption.LOWEST_RATED -> _games.value.sortedBy { it.averageRating }
            GameSortOption.MOST_REVIEWED -> _games.value.sortedByDescending { it.ratingCount }
            GameSortOption.NEWEST -> _games.value.sortedByDescending { it.createdAt?.toDate() }
            GameSortOption.TRENDING -> _games.value.sortedByDescending { 
                (it.ratingCount * 2.0) + (it.averageRating * 10.0) + 
                    if (it.ratingCount >= 10) 20.0 else if (it.ratingCount >= 5) 10.0 else 0.0
            }
        }
        _searchResults.value = sorted
    }

    // ======================== PLAYER: REVIEWS ========================
    
    /**
     * Clears the new badge notification after user acknowledgment.
     */
    fun clearNewBadge() {
        _newBadgeAwarded.value = null
    }
    
    /**
     * Submits or updates the current user's review for a game.
     * 
     * If the user already has a review for this game, it will be updated.
     * The rating is automatically clamped to the valid 1-5 range.
     * 
     * After successful submission, the badge system is checked for newly
     * earned achievements (e.g., "First Review", "5 Reviews Milestone").
     * 
     * @param gameId The game being reviewed.
     * @param rating Star rating from 1 to 5.
     * @param comment Optional text commentary.
     */
    fun submitReview(gameId: String, rating: Int, comment: String) {
        val userId = auth.currentUser?.uid ?: return
        val userName = _currentUserName.value.ifEmpty { auth.currentUser?.displayName ?: "Anonymous" }
        
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
                
                _userReview.value = repository.getUserReviewForGame(gameId, userId)
                _selectedGame.value = repository.getGame(gameId)
                
                val newBadges = badgeRepository.checkAndAwardBadgesForCurrentUser()
                if (newBadges.isNotEmpty()) {
                    _newBadgeAwarded.value = newBadges.first()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Deletes the current user's review for a game.
     * 
     * Users can only delete their own reviews. The game's rating statistics
     * are automatically updated after deletion.
     * 
     * @param gameId The game whose review should be deleted.
     */
    fun deleteMyReview(gameId: String) {
        val currentReview = _userReview.value ?: return
        
        viewModelScope.launch {
            try {
                repository.deleteReview(gameId, currentReview.id, currentReview.rating)
                _userReview.value = null
                _selectedGame.value = repository.getGame(gameId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
