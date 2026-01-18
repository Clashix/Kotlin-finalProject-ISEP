package com.isep.kotlinproject.model

import com.google.firebase.Timestamp

/**
 * Game statistics computed by Cloud Functions.
 * This data is read-only from the client perspective.
 */
data class GameStats(
    val gameId: String = "",
    val averageRating: Double = 0.0,
    val totalReviews: Int = 0,
    val ratingDistribution: RatingDistribution = RatingDistribution(),
    val lastUpdated: Timestamp? = null
) {
    /**
     * Get the most common rating
     */
    fun getMostCommonRating(): Int {
        val distribution = listOf(
            1 to ratingDistribution.star1,
            2 to ratingDistribution.star2,
            3 to ratingDistribution.star3,
            4 to ratingDistribution.star4,
            5 to ratingDistribution.star5
        )
        return distribution.maxByOrNull { it.second }?.first ?: 5
    }
    
    /**
     * Get percentage for a specific rating
     */
    fun getRatingPercentage(stars: Int): Float {
        if (totalReviews == 0) return 0f
        val count = when (stars) {
            1 -> ratingDistribution.star1
            2 -> ratingDistribution.star2
            3 -> ratingDistribution.star3
            4 -> ratingDistribution.star4
            5 -> ratingDistribution.star5
            else -> 0
        }
        return (count.toFloat() / totalReviews) * 100
    }
    
    /**
     * Get distribution as a list of pairs (stars, count)
     */
    fun getDistributionList(): List<Pair<Int, Int>> {
        return listOf(
            5 to ratingDistribution.star5,
            4 to ratingDistribution.star4,
            3 to ratingDistribution.star3,
            2 to ratingDistribution.star2,
            1 to ratingDistribution.star1
        )
    }
}

/**
 * Rating distribution across all 5 star levels.
 * Uses explicit field names to match Firestore document structure.
 */
data class RatingDistribution(
    val star1: Int = 0,
    val star2: Int = 0,
    val star3: Int = 0,
    val star4: Int = 0,
    val star5: Int = 0
) {
    // Alternative constructor for Firestore map structure
    constructor(map: Map<String, Any>?) : this(
        star1 = (map?.get("1") as? Long)?.toInt() ?: 0,
        star2 = (map?.get("2") as? Long)?.toInt() ?: 0,
        star3 = (map?.get("3") as? Long)?.toInt() ?: 0,
        star4 = (map?.get("4") as? Long)?.toInt() ?: 0,
        star5 = (map?.get("5") as? Long)?.toInt() ?: 0
    )
    
    val total: Int
        get() = star1 + star2 + star3 + star4 + star5
    
    /**
     * Convert to map for display
     */
    fun toMap(): Map<Int, Int> {
        return mapOf(
            1 to star1,
            2 to star2,
            3 to star3,
            4 to star4,
            5 to star5
        )
    }
}

/**
 * Daily statistics for a game
 */
data class DailyStats(
    val date: String = "",
    val reviewCount: Int = 0,
    val totalRatingSum: Int = 0,
    val averageRating: Double = 0.0,
    val ratingDistribution: RatingDistribution = RatingDistribution()
) {
    /**
     * Parse date string to components
     */
    fun getDateComponents(): Triple<Int, Int, Int>? {
        return try {
            val parts = date.split("-")
            Triple(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Aggregated statistics for editor dashboard
 */
data class EditorStats(
    val totalGames: Int = 0,
    val totalReviews: Int = 0,
    val overallAverageRating: Double = 0.0,
    val gameStats: List<GameStats> = emptyList(),
    val recentDailyStats: List<DailyStats> = emptyList()
) {
    /**
     * Get top performing game by rating
     */
    fun getTopRatedGame(): GameStats? {
        return gameStats.filter { it.totalReviews >= 5 }
            .maxByOrNull { it.averageRating }
    }
    
    /**
     * Get most reviewed game
     */
    fun getMostReviewedGame(): GameStats? {
        return gameStats.maxByOrNull { it.totalReviews }
    }
}
