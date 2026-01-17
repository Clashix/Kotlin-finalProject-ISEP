package com.isep.kotlinproject.model

data class Game(
    val id: String = "",
    val editorId: String = "", // Editor who created the game
    val editorName: String = "", // Name of the editor
    val title: String = "",
    val description: String = "",
    val genre: String = "",
    val releaseDate: String = "",
    val imageUrl: String = "",
    val steamAppId: String? = null,
    val developer: String = "",
    val averageRating: Double = 0.0, // Average rating (1-5 stars)
    val ratingCount: Int = 0, // Total number of ratings
    val totalRatingSum: Int = 0 // Sum of all ratings (for calculating average)
) {
    val posterUrl: String
        get() = if (imageUrl.isNotBlank()) {
            imageUrl
        } else if (!steamAppId.isNullOrBlank()) {
            "https://cdn.akamai.steamstatic.com/steam/apps/$steamAppId/header.jpg"
        } else {
            ""
        }
}
