package com.isep.kotlinproject.model

import com.google.firebase.Timestamp

/**
 * Trending games data computed by Cloud Functions.
 */
data class Trending(
    val period: TrendingPeriod = TrendingPeriod.DAILY,
    val games: List<TrendingGame> = emptyList(),
    val lastUpdated: Timestamp? = null
) {
    /**
     * Get top N games
     */
    fun getTopGames(n: Int): List<TrendingGame> {
        return games.take(n)
    }
    
    /**
     * Check if data is stale (more than 2 hours old)
     */
    fun isStale(): Boolean {
        val lastUpdate = lastUpdated ?: return true
        val twoHoursAgo = System.currentTimeMillis() - (2 * 60 * 60 * 1000)
        return lastUpdate.toDate().time < twoHoursAgo
    }
}

/**
 * Single game in the trending list
 */
data class TrendingGame(
    val gameId: String = "",
    val title: String = "",
    val imageUrl: String = "",
    val averageRating: Double = 0.0,
    val recentReviewCount: Int = 0,
    val score: Double = 0.0,
    val rank: Int = 0
) {
    /**
     * Get formatted rank string
     */
    fun getFormattedRank(): String {
        return when (rank) {
            1 -> "1st"
            2 -> "2nd"
            3 -> "3rd"
            else -> "${rank}th"
        }
    }
    
    /**
     * Check if this is a top 3 game
     */
    val isTopThree: Boolean
        get() = rank in 1..3
}

/**
 * Trending period options
 */
enum class TrendingPeriod(val value: String, val displayName: String) {
    DAILY("daily", "Today"),
    WEEKLY("weekly", "This Week"),
    MONTHLY("monthly", "This Month");
    
    companion object {
        fun fromString(value: String): TrendingPeriod {
            return entries.firstOrNull { it.value == value } ?: DAILY
        }
    }
}
