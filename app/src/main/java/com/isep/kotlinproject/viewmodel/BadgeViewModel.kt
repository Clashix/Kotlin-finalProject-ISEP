package com.isep.kotlinproject.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.isep.kotlinproject.model.Badge
import com.isep.kotlinproject.repository.BadgeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for badge/achievement operations.
 */
class BadgeViewModel : ViewModel() {
    
    private val badgeRepository = BadgeRepository()
    
    companion object {
        private const val TAG = "BadgeViewModel"
    }
    
    // User badges
    private val _userBadges = MutableStateFlow<List<Badge>>(emptyList())
    val userBadges: StateFlow<List<Badge>> = _userBadges.asStateFlow()
    
    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // Newly awarded badges (for notification)
    private val _newlyAwardedBadges = MutableStateFlow<List<Badge>>(emptyList())
    val newlyAwardedBadges: StateFlow<List<Badge>> = _newlyAwardedBadges.asStateFlow()
    
    /**
     * Load badges for a user (real-time updates)
     */
    fun loadBadges(userId: String) {
        viewModelScope.launch {
            badgeRepository.getUserBadgesFlow(userId).collect { badges ->
                _userBadges.value = badges.sortedByDescending { it.earnedAt?.toDate() }
            }
        }
    }
    
    /**
     * Check and award badges for current user.
     * Call this after posting a review.
     */
    fun checkAndAwardBadges() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val newBadges = badgeRepository.checkAndAwardBadgesForCurrentUser()
                if (newBadges.isNotEmpty()) {
                    _newlyAwardedBadges.value = newBadges
                    Log.d(TAG, "Awarded ${newBadges.size} new badges")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking badges", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Clear newly awarded badges notification
     */
    fun clearNewBadges() {
        _newlyAwardedBadges.value = emptyList()
    }
    
    /**
     * Get badges for a specific user (one-time fetch)
     */
    suspend fun getBadgesForUser(userId: String): List<Badge> {
        return badgeRepository.getUserBadges(userId)
    }
}
