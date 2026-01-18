package com.isep.kotlinproject.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.isep.kotlinproject.model.*
import com.isep.kotlinproject.repository.UserRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for user profile and social features.
 * Handles liked games, played games, wishlist, friends, and review history.
 */
class UserViewModel : ViewModel() {
    
    private val repository = UserRepository()
    private val auth = FirebaseAuth.getInstance()
    
    // =====================================================
    // STATE
    // =====================================================
    
    // Current user
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()
    
    // Viewed user profile (for public profiles)
    private val _viewedUser = MutableStateFlow<User?>(null)
    val viewedUser: StateFlow<User?> = _viewedUser.asStateFlow()
    
    // Game lists
    private val _likedGames = MutableStateFlow<List<Game>>(emptyList())
    val likedGames: StateFlow<List<Game>> = _likedGames.asStateFlow()
    
    private val _playedGames = MutableStateFlow<List<Game>>(emptyList())
    val playedGames: StateFlow<List<Game>> = _playedGames.asStateFlow()
    
    private val _wishlistGames = MutableStateFlow<List<Game>>(emptyList())
    val wishlistGames: StateFlow<List<Game>> = _wishlistGames.asStateFlow()
    
    // Review history
    private val _userReviews = MutableStateFlow<List<ReviewWithGame>>(emptyList())
    val userReviews: StateFlow<List<ReviewWithGame>> = _userReviews.asStateFlow()
    
    private var reviewsLastDocument: DocumentSnapshot? = null
    
    private val _hasMoreReviews = MutableStateFlow(true)
    val hasMoreReviews: StateFlow<Boolean> = _hasMoreReviews.asStateFlow()
    
    // Friends
    private val _friends = MutableStateFlow<List<User>>(emptyList())
    val friends: StateFlow<List<User>> = _friends.asStateFlow()
    
    private val _pendingFriendRequests = MutableStateFlow<List<FriendRequest>>(emptyList())
    val pendingFriendRequests: StateFlow<List<FriendRequest>> = _pendingFriendRequests.asStateFlow()
    
    // Notifications
    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications.asStateFlow()
    
    private val _unreadNotificationCount = MutableStateFlow(0)
    val unreadNotificationCount: StateFlow<Int> = _unreadNotificationCount.asStateFlow()
    
    // Loading states
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()
    
    // Error state
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    // User search
    private val _searchResults = MutableStateFlow<List<User>>(emptyList())
    val searchResults: StateFlow<List<User>> = _searchResults.asStateFlow()
    
    // =====================================================
    // INITIALIZATION
    // =====================================================
    
    init {
        observeCurrentUser()
        observeFriendRequests()
        observeNotifications()
    }
    
    private fun observeCurrentUser() {
        viewModelScope.launch {
            repository.getCurrentUserFlow().collect { user ->
                _currentUser.value = user
                user?.let { loadUserGameLists(it.uid) }
            }
        }
    }
    
    private fun observeFriendRequests() {
        viewModelScope.launch {
            repository.getPendingFriendRequestsFlow().collect { requests ->
                _pendingFriendRequests.value = requests
            }
        }
    }
    
    private fun observeNotifications() {
        viewModelScope.launch {
            repository.getNotificationsFlow().collect { notifications ->
                _notifications.value = notifications
                _unreadNotificationCount.value = notifications.count { !it.read }
            }
        }
    }
    
    // =====================================================
    // PROFILE OPERATIONS
    // =====================================================
    
    /**
     * Load a user's public profile
     */
    fun loadUserProfile(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _viewedUser.value = repository.getUser(userId)
            loadUserGameLists(userId, forCurrentUser = false)
            _isLoading.value = false
        }
    }
    
    /**
     * Update current user's profile
     */
    fun updateProfile(displayName: String? = null, locale: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            val success = repository.updateProfile(displayName = displayName, locale = locale)
            if (!success) {
                _error.value = "Failed to update profile"
            }
            _isLoading.value = false
        }
    }
    
    /**
     * Upload profile image
     */
    fun uploadProfileImage(imageUri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.uploadProfileImage(imageUri)
            if (result == null) {
                _error.value = "Failed to upload image"
            }
            _isLoading.value = false
        }
    }
    
    /**
     * Update locale preference
     */
    fun updateLocale(locale: String) {
        viewModelScope.launch {
            repository.updateLocale(locale)
        }
    }
    
    // =====================================================
    // GAME LIST OPERATIONS
    // =====================================================
    
    private fun loadUserGameLists(userId: String, forCurrentUser: Boolean = true) {
        viewModelScope.launch {
            try {
                val liked = repository.getLikedGames(userId)
                val played = repository.getPlayedGames(userId)
                val wishlist = repository.getWishlistGames(userId)
                
                if (forCurrentUser) {
                    _likedGames.value = liked
                    _playedGames.value = played
                    _wishlistGames.value = wishlist
                }
            } catch (e: Exception) {
                _error.value = "Failed to load game lists"
            }
        }
    }
    
    // Success message for UI feedback
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()
    
    fun clearSuccessMessage() {
        _successMessage.value = null
    }
    
    /**
     * Toggle like status for a game
     */
    fun toggleLike(gameId: String) {
        val currentlyLiked = _currentUser.value?.likedGames?.contains(gameId) == true
        
        viewModelScope.launch {
            val success = repository.toggleLike(gameId, currentlyLiked)
            if (success) {
                // Update local state immediately for responsive UI
                _currentUser.value = _currentUser.value?.let { user ->
                    val newLikedGames = if (currentlyLiked) {
                        user.likedGames.filter { it != gameId }
                    } else {
                        user.likedGames + gameId
                    }
                    user.copy(likedGames = newLikedGames)
                }
                _successMessage.value = if (currentlyLiked) "Removed from liked games" else "Added to liked games"
                // Reload game lists for profile
                _currentUser.value?.uid?.let { loadUserGameLists(it) }
            } else {
                _error.value = "Failed to update like status"
            }
        }
    }
    
    /**
     * Toggle played status for a game
     */
    fun togglePlayed(gameId: String) {
        val currentlyPlayed = _currentUser.value?.playedGames?.contains(gameId) == true
        
        viewModelScope.launch {
            val success = repository.togglePlayed(gameId, currentlyPlayed)
            if (success) {
                // Update local state immediately
                _currentUser.value = _currentUser.value?.let { user ->
                    val newPlayedGames = if (currentlyPlayed) {
                        user.playedGames.filter { it != gameId }
                    } else {
                        user.playedGames + gameId
                    }
                    user.copy(playedGames = newPlayedGames)
                }
                _successMessage.value = if (currentlyPlayed) "Removed from played games" else "Marked as played"
                // Reload game lists for profile
                _currentUser.value?.uid?.let { loadUserGameLists(it) }
            } else {
                _error.value = "Failed to update played status"
            }
        }
    }
    
    /**
     * Toggle wishlist status for a game
     */
    fun toggleWishlist(gameId: String) {
        val currentlyInWishlist = _currentUser.value?.wishlist?.contains(gameId) == true
        
        viewModelScope.launch {
            val success = repository.toggleWishlist(gameId, currentlyInWishlist)
            if (success) {
                // Update local state immediately
                _currentUser.value = _currentUser.value?.let { user ->
                    val newWishlist = if (currentlyInWishlist) {
                        user.wishlist.filter { it != gameId }
                    } else {
                        user.wishlist + gameId
                    }
                    user.copy(wishlist = newWishlist)
                }
                _successMessage.value = if (currentlyInWishlist) "Removed from wishlist" else "Added to wishlist"
                // Reload game lists for profile
                _currentUser.value?.uid?.let { loadUserGameLists(it) }
            } else {
                _error.value = "Failed to update wishlist"
            }
        }
    }
    
    /**
     * Check if game is liked
     */
    fun isGameLiked(gameId: String): Boolean {
        return _currentUser.value?.likedGames?.contains(gameId) == true
    }
    
    /**
     * Check if game is played
     */
    fun isGamePlayed(gameId: String): Boolean {
        return _currentUser.value?.playedGames?.contains(gameId) == true
    }
    
    /**
     * Check if game is in wishlist
     */
    fun isGameInWishlist(gameId: String): Boolean {
        return _currentUser.value?.wishlist?.contains(gameId) == true
    }
    
    // =====================================================
    // REVIEW HISTORY
    // =====================================================
    
    /**
     * Load user's review history (paginated)
     */
    fun loadUserReviews(userId: String? = null, refresh: Boolean = false) {
        val targetUserId = userId ?: auth.currentUser?.uid ?: return
        
        if (refresh) {
            reviewsLastDocument = null
            _userReviews.value = emptyList()
            _hasMoreReviews.value = true
        }
        
        if (!_hasMoreReviews.value && !refresh) return
        
        viewModelScope.launch {
            _isLoadingMore.value = true
            try {
                val (reviews, lastDoc) = repository.getUserReviewsWithGames(
                    userId = targetUserId,
                    lastDocument = reviewsLastDocument
                )
                
                reviewsLastDocument = lastDoc
                _hasMoreReviews.value = reviews.size >= UserRepository.PAGE_SIZE
                
                if (refresh) {
                    _userReviews.value = reviews
                } else {
                    _userReviews.value = _userReviews.value + reviews
                }
            } catch (e: Exception) {
                _error.value = "Failed to load reviews"
            } finally {
                _isLoadingMore.value = false
            }
        }
    }
    
    /**
     * Get review count for a user
     */
    fun getReviewCount(userId: String? = null, onResult: (Int) -> Unit) {
        val targetUserId = userId ?: auth.currentUser?.uid ?: return
        
        viewModelScope.launch {
            val count = repository.getUserReviewCount(targetUserId)
            onResult(count)
        }
    }
    
    // =====================================================
    // FRIENDS OPERATIONS
    // =====================================================
    
    /**
     * Load friends list for a user
     */
    fun loadFriends(userId: String? = null) {
        val targetUserId = userId ?: auth.currentUser?.uid ?: return
        
        viewModelScope.launch {
            _isLoading.value = true
            _friends.value = repository.getFriends(targetUserId)
            _isLoading.value = false
        }
    }
    
    /**
     * Send a friend request
     */
    fun sendFriendRequest(toUserId: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val success = repository.sendFriendRequest(toUserId)
            if (!success) {
                _error.value = "Failed to send friend request"
            }
            onResult(success)
        }
    }
    
    /**
     * Accept a friend request
     */
    fun acceptFriendRequest(requestId: String) {
        viewModelScope.launch {
            val success = repository.acceptFriendRequest(requestId)
            if (!success) {
                _error.value = "Failed to accept friend request"
            }
        }
    }
    
    /**
     * Reject a friend request
     */
    fun rejectFriendRequest(requestId: String) {
        viewModelScope.launch {
            val success = repository.rejectFriendRequest(requestId)
            if (!success) {
                _error.value = "Failed to reject friend request"
            }
        }
    }
    
    /**
     * Remove a friend
     */
    fun removeFriend(friendId: String) {
        viewModelScope.launch {
            val success = repository.removeFriend(friendId)
            if (success) {
                loadFriends()
            } else {
                _error.value = "Failed to remove friend"
            }
        }
    }
    
    /**
     * Check if a user is a friend
     */
    fun isFriend(userId: String): Boolean {
        return _currentUser.value?.friends?.contains(userId) == true
    }
    
    // =====================================================
    // USER SEARCH
    // =====================================================
    
    /**
     * Search users by name
     */
    fun searchUsers(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        
        viewModelScope.launch {
            _searchResults.value = repository.searchUsers(query)
        }
    }
    
    /**
     * Clear search results
     */
    fun clearSearchResults() {
        _searchResults.value = emptyList()
    }
    
    // =====================================================
    // NOTIFICATIONS
    // =====================================================
    
    /**
     * Mark a notification as read
     */
    fun markNotificationAsRead(notificationId: String) {
        viewModelScope.launch {
            repository.markNotificationAsRead(notificationId)
        }
    }
    
    // =====================================================
    // UTILITY
    // =====================================================
    
    /**
     * Clear error message
     */
    fun clearError() {
        _error.value = null
    }
    
    /**
     * Refresh all data
     */
    fun refresh() {
        val userId = auth.currentUser?.uid ?: return
        loadUserGameLists(userId)
        loadUserReviews(refresh = true)
        loadFriends()
    }
}
