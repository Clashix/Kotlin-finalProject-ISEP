package com.isep.kotlinproject.model

data class Review(
    val id: String = "",
    val gameId: String = "",
    val userId: String = "",
    val userName: String = "",
    val rating: Double = 0.0,
    val comment: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
