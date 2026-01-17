package com.isep.kotlinproject.model

data class Game(
    val id: String = "",
    val userId: String = "", // Owner of the game entry
    val title: String = "",
    val description: String = "",
    val genre: String = "",
    val releaseDate: String = "",
    val imageUrl: String = "",
    val steamAppId: String? = null,
    val developer: String = "",
    val averageRating: Double = 0.0,
    val ratingCount: Int = 0,
    val reviews: List<Review> = emptyList()
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
