package com.isep.kotlinproject.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.isep.kotlinproject.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Repository for game statistics and trending data.
 * Data in this repository is computed by Cloud Functions and is read-only.
 */
class StatsRepository {
    
    private val firestore = FirebaseFirestore.getInstance()
    
    private val gameStatsCollection = firestore.collection("game_stats")
    private val trendingCollection = firestore.collection("trending")
    private val gamesCollection = firestore.collection("games")
    
    companion object {
        private const val TAG = "StatsRepository"
    }
    
    // =====================================================
    // GAME STATISTICS
    // =====================================================
    
    /**
     * Get statistics for a specific game
     */
    suspend fun getGameStats(gameId: String): GameStats? {
        return try {
            val doc = gameStatsCollection.document(gameId).get().await()
            if (doc.exists()) {
                val data = doc.data ?: return null
                GameStats(
                    gameId = gameId,
                    averageRating = (data["averageRating"] as? Double) ?: 0.0,
                    totalReviews = (data["totalReviews"] as? Long)?.toInt() ?: 0,
                    ratingDistribution = RatingDistribution(
                        data["ratingDistribution"] as? Map<String, Any>
                    ),
                    lastUpdated = doc.getTimestamp("lastUpdated")
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting game stats for $gameId", e)
            null
        }
    }
    
    /**
     * Get statistics for a game as Flow (real-time updates)
     */
    fun getGameStatsFlow(gameId: String): Flow<GameStats?> = callbackFlow {
        val subscription = gameStatsCollection.document(gameId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to game stats", error)
                    trySend(null)
                    return@addSnapshotListener
                }
                
                if (snapshot == null || !snapshot.exists()) {
                    trySend(null)
                    return@addSnapshotListener
                }
                
                val data = snapshot.data ?: run {
                    trySend(null)
                    return@addSnapshotListener
                }
                
                val stats = GameStats(
                    gameId = gameId,
                    averageRating = (data["averageRating"] as? Double) ?: 0.0,
                    totalReviews = (data["totalReviews"] as? Long)?.toInt() ?: 0,
                    ratingDistribution = RatingDistribution(
                        data["ratingDistribution"] as? Map<String, Any>
                    ),
                    lastUpdated = snapshot.getTimestamp("lastUpdated")
                )
                
                trySend(stats)
            }
        
        awaitClose { subscription.remove() }
    }
    
    /**
     * Get daily statistics for a game (for charts)
     */
    suspend fun getDailyStats(
        gameId: String,
        days: Int = 30
    ): List<DailyStats> {
        return try {
            val snapshot = gameStatsCollection.document(gameId)
                .collection("daily_stats")
                .orderBy("date", Query.Direction.DESCENDING)
                .limit(days.toLong())
                .get()
                .await()
            
            snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                DailyStats(
                    date = doc.id,
                    reviewCount = (data["reviewCount"] as? Long)?.toInt() ?: 0,
                    totalRatingSum = (data["totalRatingSum"] as? Long)?.toInt() ?: 0,
                    averageRating = (data["averageRating"] as? Double) ?: 0.0,
                    ratingDistribution = RatingDistribution(
                        data["ratingDistribution"] as? Map<String, Any>
                    )
                )
            }.reversed() // Return in chronological order
        } catch (e: Exception) {
            Log.e(TAG, "Error getting daily stats for $gameId", e)
            emptyList()
        }
    }
    
    /**
     * Get daily statistics as Flow (real-time updates)
     */
    fun getDailyStatsFlow(gameId: String, days: Int = 30): Flow<List<DailyStats>> = callbackFlow {
        val subscription = gameStatsCollection.document(gameId)
            .collection("daily_stats")
            .orderBy("date", Query.Direction.DESCENDING)
            .limit(days.toLong())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to daily stats", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val dailyStats = snapshot?.documents?.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    DailyStats(
                        date = doc.id,
                        reviewCount = (data["reviewCount"] as? Long)?.toInt() ?: 0,
                        totalRatingSum = (data["totalRatingSum"] as? Long)?.toInt() ?: 0,
                        averageRating = (data["averageRating"] as? Double) ?: 0.0,
                        ratingDistribution = RatingDistribution(
                            data["ratingDistribution"] as? Map<String, Any>
                        )
                    )
                }?.reversed() ?: emptyList()
                
                trySend(dailyStats)
            }
        
        awaitClose { subscription.remove() }
    }
    
    // =====================================================
    // EDITOR STATISTICS
    // =====================================================
    
    /**
     * Get aggregated statistics for all games by an editor
     */
    suspend fun getEditorStats(editorId: String): EditorStats {
        return try {
            // Get all games by this editor
            val gamesSnapshot = gamesCollection
                .whereEqualTo("editorId", editorId)
                .get()
                .await()
            
            val gameIds = gamesSnapshot.documents.map { it.id }
            
            if (gameIds.isEmpty()) {
                return EditorStats()
            }
            
            // Get stats for each game
            val gameStatsList = gameIds.mapNotNull { gameId ->
                getGameStats(gameId)
            }
            
            // Calculate aggregate statistics
            val totalReviews = gameStatsList.sumOf { it.totalReviews }
            val weightedRatingSum = gameStatsList.sumOf { it.averageRating * it.totalReviews }
            val overallAverage = if (totalReviews > 0) {
                weightedRatingSum / totalReviews
            } else {
                0.0
            }
            
            // Get recent daily stats across all games (last 30 days)
            val allDailyStats = mutableListOf<DailyStats>()
            gameIds.forEach { gameId ->
                val dailyStats = getDailyStats(gameId, 30)
                allDailyStats.addAll(dailyStats)
            }
            
            // Aggregate daily stats by date
            val aggregatedDailyStats = allDailyStats
                .groupBy { it.date }
                .map { (date, stats) ->
                    DailyStats(
                        date = date,
                        reviewCount = stats.sumOf { it.reviewCount },
                        totalRatingSum = stats.sumOf { it.totalRatingSum },
                        averageRating = if (stats.sumOf { it.reviewCount } > 0) {
                            stats.sumOf { it.totalRatingSum }.toDouble() / stats.sumOf { it.reviewCount }
                        } else 0.0,
                        ratingDistribution = RatingDistribution(
                            star1 = stats.sumOf { it.ratingDistribution.star1 },
                            star2 = stats.sumOf { it.ratingDistribution.star2 },
                            star3 = stats.sumOf { it.ratingDistribution.star3 },
                            star4 = stats.sumOf { it.ratingDistribution.star4 },
                            star5 = stats.sumOf { it.ratingDistribution.star5 }
                        )
                    )
                }
                .sortedBy { it.date }
            
            EditorStats(
                totalGames = gameIds.size,
                totalReviews = totalReviews,
                overallAverageRating = overallAverage,
                gameStats = gameStatsList,
                recentDailyStats = aggregatedDailyStats
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting editor stats", e)
            EditorStats()
        }
    }
    
    /**
     * Get editor statistics as Flow
     */
    fun getEditorStatsFlow(editorId: String): Flow<EditorStats> = callbackFlow {
        // Listen to the editor's games collection for changes
        val subscription = gamesCollection
            .whereEqualTo("editorId", editorId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to editor games", error)
                    return@addSnapshotListener
                }
                
                // Fetch fresh stats when games change
                CoroutineScope(Dispatchers.IO).launch {
                    val stats = getEditorStats(editorId)
                    trySend(stats)
                }
            }
        
        awaitClose { subscription.remove() }
    }
    
    // =====================================================
    // TRENDING GAMES
    // =====================================================
    
    /**
     * Get trending games for a specific period
     */
    suspend fun getTrending(period: TrendingPeriod): Trending? {
        return try {
            val doc = trendingCollection.document(period.value).get().await()
            if (!doc.exists()) return null
            
            val data = doc.data ?: return null
            val gamesData = data["games"] as? List<Map<String, Any>> ?: emptyList()
            
            val games = gamesData.map { gameData ->
                TrendingGame(
                    gameId = gameData["gameId"] as? String ?: "",
                    title = gameData["title"] as? String ?: "",
                    imageUrl = gameData["imageUrl"] as? String ?: "",
                    averageRating = (gameData["averageRating"] as? Double) ?: 0.0,
                    recentReviewCount = (gameData["recentReviewCount"] as? Long)?.toInt() ?: 0,
                    score = (gameData["score"] as? Double) ?: 0.0,
                    rank = (gameData["rank"] as? Long)?.toInt() ?: 0
                )
            }
            
            Trending(
                period = period,
                games = games,
                lastUpdated = doc.getTimestamp("lastUpdated")
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting trending for $period", e)
            null
        }
    }
    
    /**
     * Get trending games as Flow (real-time updates)
     */
    fun getTrendingFlow(period: TrendingPeriod): Flow<Trending?> = callbackFlow {
        val subscription = trendingCollection.document(period.value)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to trending", error)
                    trySend(null)
                    return@addSnapshotListener
                }
                
                if (snapshot == null || !snapshot.exists()) {
                    trySend(null)
                    return@addSnapshotListener
                }
                
                val data = snapshot.data ?: run {
                    trySend(null)
                    return@addSnapshotListener
                }
                
                val gamesData = data["games"] as? List<Map<String, Any>> ?: emptyList()
                
                val games = gamesData.map { gameData ->
                    TrendingGame(
                        gameId = gameData["gameId"] as? String ?: "",
                        title = gameData["title"] as? String ?: "",
                        imageUrl = gameData["imageUrl"] as? String ?: "",
                        averageRating = (gameData["averageRating"] as? Double) ?: 0.0,
                        recentReviewCount = (gameData["recentReviewCount"] as? Long)?.toInt() ?: 0,
                        score = (gameData["score"] as? Double) ?: 0.0,
                        rank = (gameData["rank"] as? Long)?.toInt() ?: 0
                    )
                }
                
                trySend(Trending(
                    period = period,
                    games = games,
                    lastUpdated = snapshot.getTimestamp("lastUpdated")
                ))
            }
        
        awaitClose { subscription.remove() }
    }
    
    /**
     * Get top N trending games for a period
     */
    suspend fun getTopTrending(period: TrendingPeriod, limit: Int = 10): List<TrendingGame> {
        return getTrending(period)?.getTopGames(limit) ?: emptyList()
    }
    
    // =====================================================
    // CHART DATA HELPERS
    // =====================================================
    
    /**
     * Data class for rating evolution chart
     */
    data class RatingDataPoint(
        val date: String,
        val averageRating: Double,
        val reviewCount: Int
    )
    
    /**
     * Get rating evolution data for charts
     */
    suspend fun getRatingEvolution(gameId: String, days: Int = 30): List<RatingDataPoint> {
        val dailyStats = getDailyStats(gameId, days)
        return dailyStats.map { stat ->
            RatingDataPoint(
                date = stat.date,
                averageRating = stat.averageRating,
                reviewCount = stat.reviewCount
            )
        }
    }
    
    /**
     * Get rating distribution data for pie/bar charts
     */
    data class RatingDistributionData(
        val stars: Int,
        val count: Int,
        val percentage: Float
    )
    
    /**
     * Get rating distribution for charts
     */
    suspend fun getRatingDistributionData(gameId: String): List<RatingDistributionData> {
        val stats = getGameStats(gameId) ?: return emptyList()
        val total = stats.totalReviews.toFloat()
        
        return (1..5).map { stars ->
            val count = when (stars) {
                1 -> stats.ratingDistribution.star1
                2 -> stats.ratingDistribution.star2
                3 -> stats.ratingDistribution.star3
                4 -> stats.ratingDistribution.star4
                5 -> stats.ratingDistribution.star5
                else -> 0
            }
            RatingDistributionData(
                stars = stars,
                count = count,
                percentage = if (total > 0) (count / total) * 100 else 0f
            )
        }
    }
    
    /**
     * Get reviews per day data for line chart
     */
    suspend fun getReviewsPerDay(gameId: String, days: Int = 30): List<Pair<String, Int>> {
        val dailyStats = getDailyStats(gameId, days)
        return dailyStats.map { it.date to it.reviewCount }
    }
}
