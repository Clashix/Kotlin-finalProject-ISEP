package com.isep.kotlinproject.model

data class Review(
    val id: String = "",
    val gameId: String = "",
    val userId: String = "",
    val userName: String = "",
    val rating: Int = 5, // Rating from 1 to 5 stars
    val comment: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
