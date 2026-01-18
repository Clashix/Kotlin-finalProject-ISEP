package com.isep.kotlinproject.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.DocumentSnapshot
import com.isep.kotlinproject.api.SteamAppDetails
import com.isep.kotlinproject.model.Game
import com.isep.kotlinproject.model.Review
import com.isep.kotlinproject.model.ReviewWithGame
import com.isep.kotlinproject.model.User
import com.isep.kotlinproject.repository.SteamRepository
import com.isep.kotlinproject.repository.UserRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

/**
 * ViewModel for Users Directory feature.
 * Handles users list, search, pagination, and public profile viewing.
 */
@OptIn(FlowPreview::class)
class UsersViewModel : ViewModel() {
    
    private val userRepository = UserRepository()
    private val steamRepository = SteamRepository()
    
    companion object {
        private const val TAG = "UsersViewModel"
        private const val SEARCH_DEBOUNCE_MS = 400L
    }
    
    // =====================================================
    // USERS LIST STATE
    // =====================================================
    
    sealed class UsersListState {
        object Initial : UsersListState()
        object Loading : UsersListState()
        object LoadingMore : UsersListState()
        data class Success(val users: List<User>, val hasMore: Boolean) : UsersListState()
        data class Error(val message: String) : UsersListState()
    }
    
    private val _usersListState = MutableStateFlow<UsersListState>(UsersListState.Initial)
    val usersListState: StateFlow<UsersListState> = _usersListState.asStateFlow()
    
    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users.asStateFlow()
    
    private var usersLastDocument: DocumentSnapshot? = null
    private var hasMoreUsers = true
    
    // =====================================================
    // SEARCH STATE
    // =====================================================
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _searchResults = MutableStateFlow<List<User>>(emptyList())
    val searchResults: StateFlow<List<User>> = _searchResults.asStateFlow()
    
    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()
    
    private var searchJob: Job? = null
    private var searchLastDocument: DocumentSnapshot? = null
    private var hasMoreSearchResults = true
    
    // =====================================================
    // PUBLIC PROFILE STATE
    // =====================================================
    
    sealed class ProfileState {
        object Initial : ProfileState()
        object Loading : ProfileState()
        data class Success(val user: User) : ProfileState()
        data class Error(val message: String) : ProfileState()
    }
    
    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.Initial)
    val profileState: StateFlow<ProfileState> = _profileState.asStateFlow()
    
    private val _profileUser = MutableStateFlow<User?>(null)
    val profileUser: StateFlow<User?> = _profileUser.asStateFlow()
    
    // User reviews for public profile
    private val _userReviews = MutableStateFlow<List<ReviewWithGame>>(emptyList())
    val userReviews: StateFlow<List<ReviewWithGame>> = _userReviews.asStateFlow()
    
    private val _isLoadingReviews = MutableStateFlow(false)
    val isLoadingReviews: StateFlow<Boolean> = _isLoadingReviews.asStateFlow()
    
    private var reviewsLastDocument: DocumentSnapshot? = null
    private var hasMoreReviews = true
    
    // Steam wishlist for public profile
    private val _steamWishlist = MutableStateFlow<List<SteamAppDetails>>(emptyList())
    val steamWishlist: StateFlow<List<SteamAppDetails>> = _steamWishlist.asStateFlow()
    
    private val _isLoadingSteamWishlist = MutableStateFlow(false)
    val isLoadingSteamWishlist: StateFlow<Boolean> = _isLoadingSteamWishlist.asStateFlow()
    
    // Liked and played games for public profile
    private val _likedGames = MutableStateFlow<List<Game>>(emptyList())
    val likedGames: StateFlow<List<Game>> = _likedGames.asStateFlow()
    
    private val _playedGames = MutableStateFlow<List<Game>>(emptyList())
    val playedGames: StateFlow<List<Game>> = _playedGames.asStateFlow()
    
    // Refreshing state
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    
    init {
        // Setup search debounce
        viewModelScope.launch {
            _searchQuery
                .debounce(SEARCH_DEBOUNCE_MS)
                .collect { query ->
                    if (query.isNotBlank()) {
                        performSearch(query)
                    } else {
                        _searchResults.value = emptyList()
                        _isSearching.value = false
                    }
                }
        }
    }
    
    // =====================================================
    // USERS LIST OPERATIONS
    // =====================================================
    
    /**
     * Load initial users list
     */
    fun loadUsers() {
        if (_usersListState.value is UsersListState.Loading) return
        
        Log.d(TAG, "loadUsers: starting")
        
        viewModelScope.launch {
            _usersListState.value = UsersListState.Loading
            usersLastDocument = null
            
            try {
                val result = userRepository.getAllUsers()
                Log.d(TAG, "loadUsers: got ${result.users.size} users, hasMore=${result.hasMore}")
                
                _users.value = result.users
                usersLastDocument = result.lastDocument
                hasMoreUsers = result.hasMore
                
                _usersListState.value = UsersListState.Success(
                    users = result.users,
                    hasMore = result.hasMore
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error loading users", e)
                _usersListState.value = UsersListState.Error(e.message ?: "Failed to load users")
            }
        }
    }
    
    /**
     * Load more users (pagination)
     */
    fun loadMoreUsers() {
        if (_usersListState.value is UsersListState.LoadingMore || !hasMoreUsers) return
        
        viewModelScope.launch {
            _usersListState.value = UsersListState.LoadingMore
            
            try {
                val result = userRepository.getAllUsers(lastDocument = usersLastDocument)
                val allUsers = _users.value + result.users
                _users.value = allUsers
                usersLastDocument = result.lastDocument
                hasMoreUsers = result.hasMore
                
                _usersListState.value = UsersListState.Success(
                    users = allUsers,
                    hasMore = result.hasMore
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error loading more users", e)
                _usersListState.value = UsersListState.Error(e.message ?: "Failed to load more users")
            }
        }
    }
    
    /**
     * Refresh users list (pull-to-refresh)
     */
    fun refreshUsers() {
        viewModelScope.launch {
            _isRefreshing.value = true
            usersLastDocument = null
            
            try {
                val result = userRepository.getAllUsers()
                _users.value = result.users
                usersLastDocument = result.lastDocument
                hasMoreUsers = result.hasMore
                
                _usersListState.value = UsersListState.Success(
                    users = result.users,
                    hasMore = result.hasMore
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing users", e)
            } finally {
                _isRefreshing.value = false
            }
        }
    }
    
    // =====================================================
    // SEARCH OPERATIONS
    // =====================================================
    
    /**
     * Update search query (debounced)
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.isNotBlank()) {
            _isSearching.value = true
        }
    }
    
    /**
     * Perform search
     */
    private suspend fun performSearch(query: String) {
        searchLastDocument = null
        
        try {
            val result = userRepository.searchUsersPaginated(query)
            _searchResults.value = result.users
            searchLastDocument = result.lastDocument
            hasMoreSearchResults = result.hasMore
        } catch (e: Exception) {
            Log.e(TAG, "Error searching users", e)
            _searchResults.value = emptyList()
        } finally {
            _isSearching.value = false
        }
    }
    
    /**
     * Load more search results
     */
    fun loadMoreSearchResults() {
        if (_isSearching.value || !hasMoreSearchResults || _searchQuery.value.isBlank()) return
        
        viewModelScope.launch {
            _isSearching.value = true
            
            try {
                val result = userRepository.searchUsersPaginated(
                    query = _searchQuery.value,
                    lastDocument = searchLastDocument
                )
                _searchResults.value = _searchResults.value + result.users
                searchLastDocument = result.lastDocument
                hasMoreSearchResults = result.hasMore
            } catch (e: Exception) {
                Log.e(TAG, "Error loading more search results", e)
            } finally {
                _isSearching.value = false
            }
        }
    }
    
    /**
     * Clear search
     */
    fun clearSearch() {
        _searchQuery.value = ""
        _searchResults.value = emptyList()
        searchLastDocument = null
        hasMoreSearchResults = true
        _isSearching.value = false
    }
    
    // =====================================================
    // PUBLIC PROFILE OPERATIONS
    // =====================================================
    
    /**
     * Load public user profile
     */
    fun loadPublicProfile(userId: String) {
        viewModelScope.launch {
            _profileState.value = ProfileState.Loading
            resetProfileData()
            
            try {
                val user = userRepository.getPublicProfile(userId)
                if (user != null) {
                    _profileUser.value = user
                    _profileState.value = ProfileState.Success(user)
                    
                    // Load additional data in parallel
                    launch { loadUserReviews(userId) }
                    launch { loadSteamWishlist(user.wishlistSteamAppIds) }
                    launch { loadLikedGames(userId) }
                    launch { loadPlayedGames(userId) }
                } else {
                    _profileState.value = ProfileState.Error("User not found")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading public profile", e)
                _profileState.value = ProfileState.Error(e.message ?: "Failed to load profile")
            }
        }
    }
    
    /**
     * Reset profile data
     */
    private fun resetProfileData() {
        _profileUser.value = null
        _userReviews.value = emptyList()
        _steamWishlist.value = emptyList()
        _likedGames.value = emptyList()
        _playedGames.value = emptyList()
        reviewsLastDocument = null
        hasMoreReviews = true
    }
    
    /**
     * Load user's reviews
     */
    private suspend fun loadUserReviews(userId: String) {
        _isLoadingReviews.value = true
        
        try {
            val (reviews, lastDoc) = userRepository.getUserReviewsWithGames(userId)
            _userReviews.value = reviews
            reviewsLastDocument = lastDoc
            hasMoreReviews = reviews.size >= UserRepository.PAGE_SIZE
        } catch (e: Exception) {
            Log.e(TAG, "Error loading user reviews", e)
        } finally {
            _isLoadingReviews.value = false
        }
    }
    
    /**
     * Load more user reviews
     */
    fun loadMoreReviews(userId: String) {
        if (_isLoadingReviews.value || !hasMoreReviews) return
        
        viewModelScope.launch {
            _isLoadingReviews.value = true
            
            try {
                val (reviews, lastDoc) = userRepository.getUserReviewsWithGames(
                    userId = userId,
                    lastDocument = reviewsLastDocument
                )
                _userReviews.value = _userReviews.value + reviews
                reviewsLastDocument = lastDoc
                hasMoreReviews = reviews.size >= UserRepository.PAGE_SIZE
            } catch (e: Exception) {
                Log.e(TAG, "Error loading more reviews", e)
            } finally {
                _isLoadingReviews.value = false
            }
        }
    }
    
    /**
     * Load Steam wishlist games
     */
    private suspend fun loadSteamWishlist(steamAppIds: List<String>) {
        if (steamAppIds.isEmpty()) {
            _steamWishlist.value = emptyList()
            return
        }
        
        _isLoadingSteamWishlist.value = true
        
        try {
            val details = steamRepository.getMultipleGameDetails(steamAppIds)
            _steamWishlist.value = details.values.toList()
        } catch (e: Exception) {
            Log.e(TAG, "Error loading Steam wishlist", e)
            _steamWishlist.value = emptyList()
        } finally {
            _isLoadingSteamWishlist.value = false
        }
    }
    
    /**
     * Load liked games
     */
    private suspend fun loadLikedGames(userId: String) {
        try {
            val games = userRepository.getLikedGames(userId)
            _likedGames.value = games
        } catch (e: Exception) {
            Log.e(TAG, "Error loading liked games", e)
        }
    }
    
    /**
     * Load played games
     */
    private suspend fun loadPlayedGames(userId: String) {
        try {
            val games = userRepository.getPlayedGames(userId)
            _playedGames.value = games
        } catch (e: Exception) {
            Log.e(TAG, "Error loading played games", e)
        }
    }
    
    /**
     * Check if there are more reviews to load
     */
    fun hasMoreReviews(): Boolean = hasMoreReviews
}
