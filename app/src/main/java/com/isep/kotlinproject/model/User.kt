package com.isep.kotlinproject.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

/**
 * User data model representing a registered user in the platform.
 * Supports both Player and Editor roles with social features.
 */
data class User(
    val uid: String = "",
    val displayName: String = "",
    val displayNameLower: String = "", // Lowercase for case-insensitive search
    val email: String = "",
    val photoURL: String = "",
    val bio: String = "", // Optional user bio
    
    // Role stored as string for Firestore compatibility
    @get:PropertyName("role")
    @set:PropertyName("role")
    private var _role: String = "player",
    
    val locale: String = "en", // "fr" or "en"
    
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    
    // Social features
    val friends: List<String> = emptyList(),
    
    // Game lists
    val likedGames: List<String> = emptyList(),
    val playedGames: List<String> = emptyList(),
    val wishlist: List<String> = emptyList(), // Game IDs from Firestore
    val wishlistSteamAppIds: List<String> = emptyList(), // Steam App IDs for Steam wishlist
    
    // Legacy field for backwards compatibility (maps to displayName)
    @get:PropertyName("name")
    @set:PropertyName("name")
    var name: String = ""
) {
    // Role as enum (computed from _role string)
    val role: UserRole
        get() = UserRole.fromString(_role)
    
    // No-argument constructor required for Firestore
    constructor() : this(uid = "")
    
    /**
     * Get display name with fallback to legacy 'name' field
     */
    fun getDisplayNameOrLegacy(): String {
        return displayName.ifBlank { name }
    }
    
    /**
     * Get photo URL with fallback to legacy 'profileImageUrl' field
     * Note: Legacy field name was 'profileImageUrl', new field is 'photoURL'
     */
    fun getPhotoURLOrLegacy(): String {
        return photoURL
    }
    
    /**
     * Convert to map for Firestore writes
     */
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "uid" to uid,
            "displayName" to displayName,
            "displayNameLower" to displayName.lowercase(),
            "email" to email,
            "photoURL" to photoURL,
            "bio" to bio,
            "role" to role.name.lowercase(),
            "locale" to locale,
            "friends" to friends,
            "likedGames" to likedGames,
            "playedGames" to playedGames,
            "wishlist" to wishlist,
            "wishlistSteamAppIds" to wishlistSteamAppIds,
            "name" to displayName // Keep legacy field in sync
        )
    }
    
    companion object {
        /**
         * Create a new user with default values
         */
        fun createNew(
            uid: String,
            displayName: String,
            email: String,
            role: UserRole,
            photoURL: String = "",
            locale: String = "en",
            bio: String = ""
        ): User {
            return User(
                uid = uid,
                displayName = displayName,
                displayNameLower = displayName.lowercase(),
                email = email,
                photoURL = photoURL,
                bio = bio,
                _role = role.name.lowercase(),
                locale = locale,
                friends = emptyList(),
                likedGames = emptyList(),
                playedGames = emptyList(),
                wishlist = emptyList(),
                wishlistSteamAppIds = emptyList(),
                name = displayName
            )
        }
    }
}

/**
 * User role enumeration
 */
enum class UserRole {
    PLAYER,
    EDITOR;
    
    companion object {
        fun fromString(value: String): UserRole {
            return when (value.lowercase()) {
                "editor" -> EDITOR
                else -> PLAYER
            }
        }
    }
}

/**
 * Simplified user info for display in lists (friends, chat participants, etc.)
 */
data class UserInfo(
    val uid: String = "",
    val displayName: String = "",
    val photoURL: String = ""
) {
    companion object {
        fun fromUser(user: User): UserInfo {
            return UserInfo(
                uid = user.uid,
                displayName = user.getDisplayNameOrLegacy(),
                photoURL = user.photoURL
            )
        }
    }
}
