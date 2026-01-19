package com.isep.kotlinproject.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

/**
 * Data model representing a video game in the platform's catalog.
 * 
 * Games are the core content of the platform, created and managed by users with
 * the EDITOR role. Each game maintains its own rating statistics which are
 * automatically updated when reviews are added, modified, or deleted.
 * 
 * The model supports integration with the Steam API for automatic cover image
 * fetching when a Steam App ID is provided.
 * 
 * @property id Unique Firestore document ID for this game.
 * @property editorId User ID of the editor who created this game.
 * @property editorName Display name of the editor for UI convenience.
 * @property title The game's title displayed throughout the application.
 * @property description Detailed description of the game, supporting multi-line text.
 * @property genre Primary genre classification (see [GENRES] for available options).
 * @property releaseDate Release date as a formatted string (e.g., "2024-01-15").
 * @property imageUrl Custom cover image URL uploaded to Firebase Storage.
 * @property steamAppId Optional Steam Application ID for Steam API integration.
 * @property developer The game's development studio or team name.
 * @property averageRating Computed average of all review ratings (0.0 to 5.0).
 * @property ratingCount Total number of reviews submitted for this game.
 * @property totalRatingSum Sum of all ratings for average calculation.
 * @property createdAt Server timestamp when the game was first published.
 * @property updatedAt Server timestamp of the most recent modification.
 * 
 * @see Review for the rating/review data structure.
 * @see GameSummary for a lightweight representation used in lists.
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
    val averageRating: Double = 0.0,
    val ratingCount: Int = 0,
    val totalRatingSum: Int = 0,
    
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    
    @ServerTimestamp
    val updatedAt: Timestamp? = null
) {
    /**
     * Computed property that resolves the best available cover image URL.
     * 
     * Resolution priority:
     * 1. Custom uploaded image (imageUrl) if available
     * 2. Steam CDN header image if steamAppId is provided
     * 3. Empty string if no image source is available
     * 
     * The Steam CDN URL format provides high-quality header images suitable
     * for game cards and detail views.
     */
    val posterUrl: String
        get() = when {
            imageUrl.isNotBlank() -> imageUrl
            !steamAppId.isNullOrBlank() -> 
                "https://cdn.akamai.steamstatic.com/steam/apps/$steamAppId/header.jpg"
            else -> ""
        }
    
    /**
     * Indicates whether this game has received any reviews.
     * Useful for conditional UI rendering (e.g., showing "No reviews yet" messages).
     */
    val hasReviews: Boolean
        get() = ratingCount > 0
    
    /**
     * Formats the average rating for display in the UI.
     * 
     * @return A formatted string like "4.5/5" or "No ratings" if no reviews exist.
     */
    fun getFormattedRating(): String {
        return if (hasReviews) {
            String.format("%.1f/5", averageRating)
        } else {
            "No ratings"
        }
    }
    
    /**
     * Converts this Game instance to a Map for Firestore document writes.
     * 
     * Note: The 'id' field is excluded as it's managed by Firestore as the document ID.
     * Timestamp fields are also excluded to allow server-side timestamp generation.
     * 
     * @return A map representation suitable for Firestore set/update operations.
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
         * Predefined list of game genres available for selection.
         * 
         * This list provides a consistent set of genre options across the platform,
         * ensuring data consistency for filtering and search functionality.
         * The "Other" option serves as a catch-all for games that don't fit
         * standard categories.
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
 * Lightweight data class for displaying games in ranked lists and carousels.
 * 
 * This model is optimized for trending lists and quick display scenarios where
 * full game details are not required. It includes a rank field for position
 * display and a score field for sorting algorithms.
 * 
 * @property gameId The game's unique identifier for navigation.
 * @property title The game's title for display.
 * @property imageUrl URL to the game's cover image.
 * @property averageRating The game's current average rating.
 * @property recentReviewCount Number of recent reviews (for trending calculation).
 * @property score Computed trending/ranking score for sorting.
 * @property rank Display position in the ranked list (1-based).
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
        /**
         * Creates a GameSummary from a full Game object.
         * 
         * @param game The source Game object to extract summary data from.
         * @param rank Optional rank position for display in lists.
         * @return A lightweight GameSummary suitable for list rendering.
         */
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
