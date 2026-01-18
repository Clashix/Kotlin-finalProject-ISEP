package com.isep.kotlinproject.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.isep.kotlinproject.model.*
import com.isep.kotlinproject.repository.StatsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for game statistics and trending features.
 * Used by editors for their dashboard and by players for trending games.
 */
class StatsViewModel : ViewModel() {
    
    private val repository = StatsRepository()
    private val auth = FirebaseAuth.getInstance()
    
    // =====================================================
    // STATE
    // =====================================================
    
    // Game statistics (for detail view)
    private val _gameStats = MutableStateFlow<GameStats?>(null)
    val gameStats: StateFlow<GameStats?> = _gameStats.asStateFlow()
    
    // Daily statistics for charts
    private val _dailyStats = MutableStateFlow<List<DailyStats>>(emptyList())
    val dailyStats: StateFlow<List<DailyStats>> = _dailyStats.asStateFlow()
    
    // Editor aggregate statistics
    private val _editorStats = MutableStateFlow<EditorStats?>(null)
    val editorStats: StateFlow<EditorStats?> = _editorStats.asStateFlow()
    
    // Trending games
    private val _trendingDaily = MutableStateFlow<Trending?>(null)
    val trendingDaily: StateFlow<Trending?> = _trendingDaily.asStateFlow()
    
    private val _trendingWeekly = MutableStateFlow<Trending?>(null)
    val trendingWeekly: StateFlow<Trending?> = _trendingWeekly.asStateFlow()
    
    private val _trendingMonthly = MutableStateFlow<Trending?>(null)
    val trendingMonthly: StateFlow<Trending?> = _trendingMonthly.asStateFlow()
    
    private val _selectedTrendingPeriod = MutableStateFlow(TrendingPeriod.WEEKLY)
    val selectedTrendingPeriod: StateFlow<TrendingPeriod> = _selectedTrendingPeriod.asStateFlow()
    
    // Chart data (preprocessed for display)
    private val _ratingEvolution = MutableStateFlow<List<StatsRepository.RatingDataPoint>>(emptyList())
    val ratingEvolution: StateFlow<List<StatsRepository.RatingDataPoint>> = _ratingEvolution.asStateFlow()
    
    private val _ratingDistribution = MutableStateFlow<List<StatsRepository.RatingDistributionData>>(emptyList())
    val ratingDistribution: StateFlow<List<StatsRepository.RatingDistributionData>> = _ratingDistribution.asStateFlow()
    
    private val _reviewsPerDay = MutableStateFlow<List<Pair<String, Int>>>(emptyList())
    val reviewsPerDay: StateFlow<List<Pair<String, Int>>> = _reviewsPerDay.asStateFlow()
    
    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // Error state
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    // =====================================================
    // GAME STATISTICS
    // =====================================================
    
    /**
     * Load statistics for a specific game
     */
    fun loadGameStats(gameId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _gameStats.value = repository.getGameStats(gameId)
                loadChartData(gameId)
            } catch (e: Exception) {
                _error.value = "Failed to load game statistics"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Observe game statistics in real-time
     */
    fun observeGameStats(gameId: String) {
        viewModelScope.launch {
            repository.getGameStatsFlow(gameId).collect { stats ->
                _gameStats.value = stats
            }
        }
    }
    
    /**
     * Load daily statistics for a game
     */
    fun loadDailyStats(gameId: String, days: Int = 30) {
        viewModelScope.launch {
            _dailyStats.value = repository.getDailyStats(gameId, days)
        }
    }
    
    /**
     * Observe daily statistics in real-time
     */
    fun observeDailyStats(gameId: String, days: Int = 30) {
        viewModelScope.launch {
            repository.getDailyStatsFlow(gameId, days).collect { stats ->
                _dailyStats.value = stats
            }
        }
    }
    
    // =====================================================
    // EDITOR STATISTICS
    // =====================================================
    
    /**
     * Load aggregated statistics for the current editor
     */
    fun loadEditorStats() {
        val editorId = auth.currentUser?.uid ?: return
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _editorStats.value = repository.getEditorStats(editorId)
            } catch (e: Exception) {
                _error.value = "Failed to load editor statistics"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Load statistics for a specific editor
     */
    fun loadEditorStats(editorId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _editorStats.value = repository.getEditorStats(editorId)
            } catch (e: Exception) {
                _error.value = "Failed to load editor statistics"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Observe editor statistics in real-time
     */
    fun observeEditorStats(editorId: String? = null) {
        val targetId = editorId ?: auth.currentUser?.uid ?: return
        
        viewModelScope.launch {
            repository.getEditorStatsFlow(targetId).collect { stats ->
                _editorStats.value = stats
            }
        }
    }
    
    // =====================================================
    // TRENDING GAMES
    // =====================================================
    
    /**
     * Load trending games for all periods
     */
    fun loadAllTrending() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _trendingDaily.value = repository.getTrending(TrendingPeriod.DAILY)
                _trendingWeekly.value = repository.getTrending(TrendingPeriod.WEEKLY)
                _trendingMonthly.value = repository.getTrending(TrendingPeriod.MONTHLY)
            } catch (e: Exception) {
                _error.value = "Failed to load trending games"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Load trending games for a specific period
     */
    fun loadTrending(period: TrendingPeriod) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val trending = repository.getTrending(period)
                when (period) {
                    TrendingPeriod.DAILY -> _trendingDaily.value = trending
                    TrendingPeriod.WEEKLY -> _trendingWeekly.value = trending
                    TrendingPeriod.MONTHLY -> _trendingMonthly.value = trending
                }
            } catch (e: Exception) {
                _error.value = "Failed to load trending games"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Observe trending games in real-time
     */
    fun observeTrending(period: TrendingPeriod) {
        viewModelScope.launch {
            repository.getTrendingFlow(period).collect { trending ->
                when (period) {
                    TrendingPeriod.DAILY -> _trendingDaily.value = trending
                    TrendingPeriod.WEEKLY -> _trendingWeekly.value = trending
                    TrendingPeriod.MONTHLY -> _trendingMonthly.value = trending
                }
            }
        }
    }
    
    /**
     * Change selected trending period
     */
    fun selectTrendingPeriod(period: TrendingPeriod) {
        _selectedTrendingPeriod.value = period
    }
    
    /**
     * Get current trending based on selected period
     */
    val currentTrending: StateFlow<Trending?> = combine(
        selectedTrendingPeriod,
        trendingDaily,
        trendingWeekly,
        trendingMonthly
    ) { period, daily, weekly, monthly ->
        when (period) {
            TrendingPeriod.DAILY -> daily
            TrendingPeriod.WEEKLY -> weekly
            TrendingPeriod.MONTHLY -> monthly
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    
    /**
     * Get top N trending games for current period
     */
    fun getTopTrending(limit: Int = 10): List<TrendingGame> {
        return currentTrending.value?.getTopGames(limit) ?: emptyList()
    }
    
    // =====================================================
    // CHART DATA
    // =====================================================
    
    /**
     * Load all chart data for a game
     */
    private fun loadChartData(gameId: String) {
        viewModelScope.launch {
            try {
                _ratingEvolution.value = repository.getRatingEvolution(gameId)
                _ratingDistribution.value = repository.getRatingDistributionData(gameId)
                _reviewsPerDay.value = repository.getReviewsPerDay(gameId)
            } catch (e: Exception) {
                _error.value = "Failed to load chart data"
            }
        }
    }
    
    /**
     * Get rating evolution for charts
     */
    fun loadRatingEvolution(gameId: String, days: Int = 30) {
        viewModelScope.launch {
            _ratingEvolution.value = repository.getRatingEvolution(gameId, days)
        }
    }
    
    /**
     * Get rating distribution for charts
     */
    fun loadRatingDistribution(gameId: String) {
        viewModelScope.launch {
            _ratingDistribution.value = repository.getRatingDistributionData(gameId)
        }
    }
    
    /**
     * Get reviews per day for charts
     */
    fun loadReviewsPerDay(gameId: String, days: Int = 30) {
        viewModelScope.launch {
            _reviewsPerDay.value = repository.getReviewsPerDay(gameId, days)
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
     * Clear all loaded data
     */
    fun clearData() {
        _gameStats.value = null
        _dailyStats.value = emptyList()
        _ratingEvolution.value = emptyList()
        _ratingDistribution.value = emptyList()
        _reviewsPerDay.value = emptyList()
    }
    
    /**
     * Refresh editor statistics
     */
    fun refreshEditorStats() {
        loadEditorStats()
    }
    
    /**
     * Refresh trending data
     */
    fun refreshTrending() {
        loadAllTrending()
    }
}
