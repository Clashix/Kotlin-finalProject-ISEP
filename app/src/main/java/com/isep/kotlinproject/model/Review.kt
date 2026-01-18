package com.isep.kotlinproject.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Review data model representing a user's review of a game.
 * Reviews include a star rating (1-5) and an optional comment.
 */
data class Review(
    val id: String = "",
    val gameId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userPhotoURL: String = "",
    val rating: Int = 5, // Rating from 1 to 5 stars
    val comment: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    
    @ServerTimestamp
    val updatedAt: Timestamp? = null,
    
    // Denormalized game info for user review history
    val gameTitle: String = "",
    val gameImageUrl: String = ""
) {
    /**
     * Get formatted date string
     */
    fun getFormattedDate(locale: Locale = Locale.getDefault()): String {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", locale)
        return dateFormat.format(Date(timestamp))
    }
    
    /**
     * Get relative time string (e.g., "2 days ago")
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
     * Validate rating is within acceptable range
     */
    fun isValidRating(): Boolean = rating in 1..5
    
    /**
     * Convert to map for Firestore writes
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
         * Create a new review with current timestamp
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
 * Review with associated game data for display in user's review history
 */
data class ReviewWithGame(
    val review: Review,
    val game: Game?
) {
    val gameTitle: String
        get() = game?.title ?: review.gameTitle
    
    val gameImageUrl: String
        get() = game?.posterUrl ?: review.gameImageUrl
}
