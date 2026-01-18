package com.isep.kotlinproject.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

/**
 * Game data model representing a video game in the platform.
 * Games are created and managed by Editors.
 */
data class Game(
    val id: String = "",
    val editorId: String = "",
    val editorName: String = "",
    val title: String = "",
    val description: String = "",
    val genre: String = "",
    val releaseDate: String = "",
    val imageUrl: String = "",
    val steamAppId: String? = null,
    val developer: String = "",
    
    // Rating statistics (managed by Cloud Functions)
    val averageRating: Double = 0.0,
    val ratingCount: Int = 0,
    val totalRatingSum: Int = 0,
    
    // Timestamps
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    val updatedAt: Timestamp? = null
) {
    /**
     * Get the poster/cover image URL.
     * Falls back to Steam header image if no custom image is set.
     */
    val posterUrl: String
        get() = when {
            imageUrl.isNotBlank() -> imageUrl
            !steamAppId.isNullOrBlank() -> 
                "https://cdn.akamai.steamstatic.com/steam/apps/$steamAppId/header.jpg"
            else -> ""
        }
    
    /**
     * Check if the game has any reviews
     */
    val hasReviews: Boolean
        get() = ratingCount > 0
    
    /**
     * Get formatted rating string (e.g., "4.5/5")
     */
    fun getFormattedRating(): String {
        return if (hasReviews) {
            String.format("%.1f/5", averageRating)
        } else {
            "No ratings"
        }
    }
    
    /**
     * Convert to map for Firestore writes
     */
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "editorId" to editorId,
            "editorName" to editorName,
            "title" to title,
            "description" to description,
            "genre" to genre,
            "releaseDate" to releaseDate,
            "imageUrl" to imageUrl,
            "steamAppId" to steamAppId,
            "developer" to developer,
            "averageRating" to averageRating,
            "ratingCount" to ratingCount,
            "totalRatingSum" to totalRatingSum
        )
    }
    
    companion object {
        /**
         * List of common game genres
         */
        val GENRES = listOf(
            "Action",
            "Adventure",
            "RPG",
            "Strategy",
            "Simulation",
            "Sports",
            "Racing",
            "Puzzle",
            "Horror",
            "Shooter",
            "Fighting",
            "Platformer",
            "Sandbox",
            "MMORPG",
            "Indie",
            "Other"
        )
    }
}

/**
 * Simplified game info for trending lists and quick displays
 */
data class GameSummary(
    val gameId: String = "",
    val title: String = "",
    val imageUrl: String = "",
    val averageRating: Double = 0.0,
    val recentReviewCount: Int = 0,
    val score: Double = 0.0,
    val rank: Int = 0
) {
    companion object {
        fun fromGame(game: Game, rank: Int = 0): GameSummary {
            return GameSummary(
                gameId = game.id,
                title = game.title,
                imageUrl = game.posterUrl,
                averageRating = game.averageRating,
                recentReviewCount = game.ratingCount,
                rank = rank
            )
        }
    }
}
