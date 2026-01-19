package com.isep.kotlinproject.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

/**
 * Core data model representing a registered user in the video game rating platform.
 * 
 * This class supports two distinct user roles with different capabilities:
 * - **Player**: Can browse games, write reviews, manage wishlists, and interact socially.
 * - **Editor**: Can publish and manage games, access analytics dashboards, and gain followers.
 * 
 * The model includes backwards compatibility with legacy field names to ensure
 * smooth migration of existing Firestore documents.
 * 
 * @property uid Unique Firebase Authentication user identifier.
 * @property displayName User's display name shown throughout the application.
 * @property displayNameLower Lowercase version of displayName for case-insensitive Firestore queries.
 * @property email User's email address from Firebase Auth.
 * @property photoURL URL to the user's profile picture (Firebase Storage or external URL).
 * @property bio Optional biographical text displayed on the user's public profile.
 * @property role User role stored as lowercase string ("player" or "editor") for Firestore compatibility.
 * @property locale User's preferred language code ("en" or "fr") for internationalization.
 * @property createdAt Server-generated timestamp when the user account was created.
 * @property friends List of user IDs representing confirmed friendships.
 * @property likedGames List of game IDs the user has liked.
 * @property playedGames List of game IDs the user has marked as played.
 * @property wishlist List of internal game IDs in the user's wishlist.
 * @property wishlistSteamAppIds List of Steam App IDs for games imported from Steam wishlist.
 * @property followingEditors List of editor user IDs this user follows for updates.
 * @property themePreference UI theme preference: "light", "dark", or "system" (follows device settings).
 * @property name Legacy field mapping to displayName for backwards compatibility.
 * @property profileImageUrl Legacy field mapping to photoURL for backwards compatibility.
 * 
 * @see UserRole for the enumeration of available user roles.
 * @see UserInfo for a lightweight representation used in lists and references.
 */
data class User(
    val uid: String = "",
    val displayName: String = "",
    val displayNameLower: String = "",
    val email: String = "",
    val photoURL: String = "",
    val bio: String = "",
    var role: String = "player",
    val locale: String = "en",
    
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    
    val friends: List<String> = emptyList(),
    val likedGames: List<String> = emptyList(),
    val playedGames: List<String> = emptyList(),
    val wishlist: List<String> = emptyList(),
    val wishlistSteamAppIds: List<String> = emptyList(),
    val followingEditors: List<String> = emptyList(),
    val themePreference: String = "system",
    
    var name: String = "",
    var profileImageUrl: String = ""
) {
    /**
     * Computed property that converts the string-based role to the [UserRole] enum.
     * This provides type-safe role checking throughout the application while
     * maintaining Firestore string compatibility.
     */
    val userRole: UserRole
        get() = UserRole.fromString(role)
    
    /**
     * No-argument constructor required by Firestore for automatic document deserialization.
     * Firestore uses reflection to instantiate objects, requiring a default constructor.
     */
    constructor() : this(uid = "")
    
    /**
     * Retrieves the user's display name with automatic fallback to the legacy 'name' field.
     * This ensures compatibility with user documents created before the field rename migration.
     * 
     * @return The display name, falling back to the legacy name field if displayName is blank.
     */
    fun getDisplayNameOrLegacy(): String {
        return displayName.ifBlank { name }
    }
    
    /**
     * Retrieves the user's photo URL with automatic fallback to the legacy 'profileImageUrl' field.
     * This ensures profile pictures remain visible for users created before the field migration.
     * 
     * @return The photo URL, falling back to the legacy profileImageUrl if photoURL is blank.
     */
    fun getPhotoURLOrLegacy(): String {
        return photoURL.ifBlank { profileImageUrl }
    }
    
    /**
     * Converts this User instance to a Map suitable for Firestore document writes.
     * 
     * The map includes both current field names and legacy field names to ensure
     * compatibility with older client versions that may still read the legacy fields.
     * The displayNameLower field is automatically computed for search indexing.
     * 
     * @return A map representation of the user data for Firestore storage.
     */
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "uid" to uid,
            "displayName" to displayName,
            "displayNameLower" to displayName.lowercase(),
            "email" to email,
            "photoURL" to photoURL,
            "bio" to bio,
            "role" to userRole.name.lowercase(),
            "locale" to locale,
            "friends" to friends,
            "likedGames" to likedGames,
            "playedGames" to playedGames,
            "wishlist" to wishlist,
            "wishlistSteamAppIds" to wishlistSteamAppIds,
            "followingEditors" to followingEditors,
            "themePreference" to themePreference,
            "name" to displayName,
            "profileImageUrl" to photoURL
        )
    }
    
    companion object {
        /**
         * Factory method to create a new User instance with properly initialized defaults.
         * 
         * This method ensures all computed and legacy fields are correctly populated,
         * providing a consistent starting point for new user registration flows.
         * 
         * @param uid The Firebase Authentication user ID.
         * @param displayName The user's chosen display name.
         * @param email The user's email address.
         * @param userRole The selected role (PLAYER or EDITOR).
         * @param photoURL Optional URL to the user's profile picture.
         * @param locale The user's preferred language code (defaults to "en").
         * @param bio Optional biographical text for the user's profile.
         * @return A fully initialized User instance ready for Firestore storage.
         */
        fun createNew(
            uid: String,
            displayName: String,
            email: String,
            userRole: UserRole,
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
                role = userRole.name.lowercase(),
                locale = locale,
                friends = emptyList(),
                likedGames = emptyList(),
                playedGames = emptyList(),
                wishlist = emptyList(),
                wishlistSteamAppIds = emptyList(),
                followingEditors = emptyList(),
                themePreference = "system",
                name = displayName,
                profileImageUrl = photoURL
            )
        }
    }
}

/**
 * Enumeration defining the available user roles in the platform.
 * 
 * Each role grants different capabilities:
 * - **PLAYER**: Standard user who can browse, review, and interact with games.
 * - **EDITOR**: Content creator who can publish games and access analytics.
 * 
 * The role determines which screens and features are accessible to the user.
 */
enum class UserRole {
    /** Standard user role with game browsing and reviewing capabilities. */
    PLAYER,
    
    /** Content creator role with game publishing and analytics access. */
    EDITOR;
    
    companion object {
        /**
         * Parses a string value to the corresponding UserRole enum.
         * 
         * The parsing is case-insensitive to handle various storage formats.
         * Unknown values default to PLAYER as the safest fallback.
         * 
         * @param value The string representation of the role.
         * @return The corresponding UserRole, defaulting to PLAYER for unknown values.
         */
        fun fromString(value: String): UserRole {
            return when (value.lowercase()) {
                "editor" -> EDITOR
                else -> PLAYER
            }
        }
    }
}

/**
 * Lightweight data class for displaying basic user information in lists.
 * 
 * This class contains only the essential fields needed for UI display,
 * reducing memory overhead when displaying large lists of users such as
 * friend lists, chat participants, or search results.
 * 
 * @property uid The user's unique identifier for navigation and data fetching.
 * @property displayName The user's name for display purposes.
 * @property photoURL URL to the user's profile picture for avatar display.
 */
data class UserInfo(
    val uid: String = "",
    val displayName: String = "",
    val photoURL: String = ""
) {
    companion object {
        /**
         * Creates a UserInfo instance from a full User object.
         * 
         * This factory method extracts only the necessary fields for list display,
         * using the legacy-aware getter for the display name.
         * 
         * @param user The full User object to extract information from.
         * @return A lightweight UserInfo instance suitable for list display.
         */
        fun fromUser(user: User): UserInfo {
            return UserInfo(
                uid = user.uid,
                displayName = user.getDisplayNameOrLegacy(),
                photoURL = user.photoURL
            )
        }
    }
}
