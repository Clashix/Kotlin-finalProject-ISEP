package com.isep.kotlinproject.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

/**
 * Friend request between two users.
 */
data class FriendRequest(
    val id: String = "",
    val fromUserId: String = "",
    val fromUserName: String = "",
    val fromUserPhotoURL: String = "",
    val toUserId: String = "",
    val status: FriendRequestStatus = FriendRequestStatus.PENDING,
    
    @ServerTimestamp
    val timestamp: Timestamp? = null
) {
    /**
     * Check if the request is pending
     */
    val isPending: Boolean
        get() = status == FriendRequestStatus.PENDING
    
    /**
     * Get formatted time since request was sent
     */
    fun getRelativeTime(): String {
        val ts = timestamp ?: return ""
        val now = System.currentTimeMillis()
        val diff = now - ts.toDate().time
        
        return when {
            diff < 60_000 -> "Just now"
            diff < 3_600_000 -> "${diff / 60_000}m ago"
            diff < 86_400_000 -> "${diff / 3_600_000}h ago"
            diff < 604_800_000 -> "${diff / 86_400_000}d ago"
            else -> {
                val format = java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault())
                format.format(ts.toDate())
            }
        }
    }
    
    /**
     * Convert to map for Firestore writes
     */
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "fromUserId" to fromUserId,
            "fromUserName" to fromUserName,
            "fromUserPhotoURL" to fromUserPhotoURL,
            "toUserId" to toUserId,
            "status" to status.value,
            "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )
    }
    
    companion object {
        /**
         * Create a new friend request
         */
        fun create(
            fromUser: User,
            toUserId: String
        ): FriendRequest {
            return FriendRequest(
                fromUserId = fromUser.uid,
                fromUserName = fromUser.getDisplayNameOrLegacy(),
                fromUserPhotoURL = fromUser.photoURL,
                toUserId = toUserId,
                status = FriendRequestStatus.PENDING
            )
        }
    }
}

/**
 * Status of a friend request
 */
enum class FriendRequestStatus(val value: String) {
    PENDING("pending"),
    ACCEPTED("accepted"),
    REJECTED("rejected");
    
    companion object {
        fun fromString(value: String): FriendRequestStatus {
            return entries.firstOrNull { it.value == value } ?: PENDING
        }
    }
}

/**
 * Notification for user activities
 */
data class Notification(
    val id: String = "",
    val type: NotificationType = NotificationType.FRIEND_REQUEST,
    val fromUserId: String = "",
    val fromUserName: String = "",
    val message: String = "",
    val read: Boolean = false,
    
    @ServerTimestamp
    val timestamp: Timestamp? = null
) {
    /**
     * Get formatted time
     */
    fun getRelativeTime(): String {
        val ts = timestamp ?: return ""
        val now = System.currentTimeMillis()
        val diff = now - ts.toDate().time
        
        return when {
            diff < 60_000 -> "Just now"
            diff < 3_600_000 -> "${diff / 60_000}m ago"
            diff < 86_400_000 -> "${diff / 3_600_000}h ago"
            else -> "${diff / 86_400_000}d ago"
        }
    }
}

/**
 * Types of notifications
 */
enum class NotificationType(val value: String) {
    FRIEND_REQUEST("friend_request"),
    FRIEND_ACCEPTED("friend_accepted"),
    NEW_MESSAGE("new_message"),
    GAME_UPDATE("game_update");
    
    companion object {
        fun fromString(value: String): NotificationType {
            return entries.firstOrNull { it.value == value } ?: FRIEND_REQUEST
        }
    }
}
