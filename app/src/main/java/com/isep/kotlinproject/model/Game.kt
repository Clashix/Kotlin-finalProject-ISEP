package com.isep.kotlinproject.model

data class Game(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val genre: String = "",
    val releaseDate: String = "",
    val imageUrl: String = "",
    val developer: String = "",
    val averageRating: Double = 0.0,
    val ratingCount: Int = 0
)
