package com.isep.kotlinproject.model

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val role: UserRole = UserRole.PLAYER,
    val profileImageUrl: String = "",
    val games: List<Game> = emptyList()
)

enum class UserRole {
    PLAYER,
    EDITOR
}
