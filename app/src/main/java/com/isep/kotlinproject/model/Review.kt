package com.isep.kotlinproject.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Data model representing a user's review of a game on the platform.
 * 
 * Reviews are the primary user-generated content, consisting of a star rating
 * (1-5 stars) and an optional text comment. Each review is associated with
 * both a user and a game, enabling bidirectional queries for user profiles
 * and game detail pages.
 * 
 * The model includes denormalized game information (title, image) to enable
 * efficient rendering of user review histories without requiring additional
 * queries to fetch game data.
 * 
 * Storage: Reviews are stored as subcollections under games (`games/{gameId}/reviews`)
 * and can also be queried via collection group queries for user-specific listings.
 * 
 * @property id Unique Firestore document ID for this review.
 * @property gameId Reference to the parent game document.
 * @property userId Firebase Auth UID of the reviewer.
 * @property userName Display name of the reviewer at time of posting.
 * @property userPhotoURL Profile photo URL of the reviewer at time of posting.
 * @property rating Star rating from 1 (worst) to 5 (best).
 * @property comment Optional text commentary explaining the rating.
 * @property timestamp Unix timestamp (milliseconds) when the review was created.
 * @property updatedAt Server timestamp of the last modification.
 * @property gameTitle Denormalized game title for review history display.
 * @property gameImageUrl Denormalized game cover URL for review history display.
 * 
 * @see ReviewWithGame for a wrapper including full game data.
 * @see ReviewReliability for reviewer trust scoring.
 */
data class Review(
    val id: String = "",
    val gameId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userPhotoURL: String = "",
    val rating: Int = 5,
    val comment: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    
    @ServerTimestamp
    val updatedAt: Timestamp? = null,
    
    val gameTitle: String = "",
    val gameImageUrl: String = ""
) {
    /**
     * Formats the review timestamp as a human-readable date string.
     * 
     * @param locale The locale to use for date formatting (defaults to device locale).
     * @return A formatted date string like "Jan 15, 2024".
     */
    fun getFormattedDate(locale: Locale = Locale.getDefault()): String {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", locale)
        return dateFormat.format(Date(timestamp))
    }
    
    /**
     * Generates a relative time description for the review timestamp.
     * 
     * This provides user-friendly time descriptions that are more intuitive
     * for recent reviews (e.g., "2 hours ago") while falling back to absolute
     * dates for older reviews.
     * 
     * Time ranges:
     * - Under 1 minute: "Just now"
     * - Under 1 hour: "Xm ago"
     * - Under 1 day: "Xh ago"
     * - Under 1 week: "Xd ago"
     * - Under 1 month: "Xw ago"
     * - Older: Full date format
     * 
     * @return A relative time string or formatted date for older reviews.
     */
    fun getRelativeTime(): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        
        return when {
            diff < 60_000 -> "Just now"
            diff < 3_600_000 -> "${diff / 60_000}m ago"
            diff < 86_400_000 -> "${diff / 3_600_000}h ago"
            diff < 604_800_000 -> "${diff / 86_400_000}d ago"
            diff < 2_592_000_000 -> "${diff / 604_800_000}w ago"
            else -> getFormattedDate()
        }
    }
    
    /**
     * Validates that the rating falls within the acceptable 1-5 star range.
     * 
     * @return True if the rating is between 1 and 5 inclusive.
     */
    fun isValidRating(): Boolean = rating in 1..5
    
    /**
     * Converts this Review instance to a Map for Firestore document writes.
     * 
     * Note: The 'id' field is excluded as it's managed by Firestore as the document ID.
     * The 'updatedAt' field is excluded to allow server-side timestamp generation.
     * 
     * @return A map representation suitable for Firestore set/update operations.
     */
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "gameId" to gameId,
            "userId" to userId,
            "userName" to userName,
            "userPhotoURL" to userPhotoURL,
            "rating" to rating,
            "comment" to comment,
            "timestamp" to timestamp,
            "gameTitle" to gameTitle,
            "gameImageUrl" to gameImageUrl
        )
    }
    
    companion object {
        /**
         * Factory method to create a new Review with the current timestamp.
         * 
         * The rating is automatically clamped to the valid 1-5 range to prevent
         * invalid data from being stored.
         * 
         * @param gameId The ID of the game being reviewed.
         * @param userId The reviewer's Firebase Auth UID.
         * @param userName The reviewer's display name.
         * @param userPhotoURL Optional URL to the reviewer's profile photo.
         * @param rating Star rating (will be clamped to 1-5 range).
         * @param comment Optional text commentary.
         * @param gameTitle Optional game title for denormalization.
         * @param gameImageUrl Optional game image URL for denormalization.
         * @return A new Review instance with current timestamp.
         */
        fun create(
            gameId: String,
            userId: String,
            userName: String,
            userPhotoURL: String = "",
            rating: Int,
            comment: String = "",
            gameTitle: String = "",
            gameImageUrl: String = ""
        ): Review {
            return Review(
                gameId = gameId,
                userId = userId,
                userName = userName,
                userPhotoURL = userPhotoURL,
                rating = rating.coerceIn(1, 5),
                comment = comment,
                timestamp = System.currentTimeMillis(),
                gameTitle = gameTitle,
                gameImageUrl = gameImageUrl
            )
        }
    }
}

/**
 * Wrapper class combining a Review with its associated Game data.
 * 
 * This class is used when displaying reviews in contexts where full game
 * information is needed (e.g., user review history), avoiding the need for
 * separate queries while still supporting graceful degradation when game
 * data is unavailable.
 * 
 * @property review The review data.
 * @property game The associated game data, or null if unavailable.
 */
data class ReviewWithGame(
    val review: Review,
    val game: Game?
) {
    /**
     * Resolves the game title with fallback to denormalized data.
     * Prefers live game data but falls back to the cached title in the review.
     */
    val gameTitle: String
        get() = game?.title ?: review.gameTitle
    
    /**
     * Resolves the game image URL with fallback to denormalized data.
     * Prefers live game poster URL but falls back to the cached URL in the review.
     */
    val gameImageUrl: String
        get() = game?.posterUrl ?: review.gameImageUrl
}
