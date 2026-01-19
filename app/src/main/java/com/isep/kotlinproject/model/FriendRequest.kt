package com.isep.kotlinproject.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ServerTimestamp

/**
 * Data model representing a pending or resolved friend connection request.
 * 
 * The friend request system enables users to build social connections on
 * the platform. Requests flow through a simple state machine:
 * PENDING → ACCEPTED (friendship established) or REJECTED (request declined).
 * 
 * **Storage Location:** `friend_requests/{requestId}`
 * 
 * **Security Considerations:**
 * - Users can only send requests to other authenticated users.
 * - Users cannot send requests to themselves.
 * - Duplicate pending requests are prevented at the repository level.
 * 
 * @property id Firestore document ID for this request.
 * @property fromUserId User ID of the person initiating the request.
 * @property fromUserName Display name of the requester for notification display.
 * @property fromUserPhotoURL Photo URL of the requester for avatar display.
 * @property toUserId User ID of the person receiving the request.
 * @property status Current [FriendRequestStatus] in the request lifecycle.
 * @property timestamp Server timestamp when the request was created.
 * 
 * @see FriendRequestStatus for the possible request states.
 * @see UserRepository for friend request operations.
 * @see NotificationsScreen for the UI handling incoming requests.
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
     * Convenience property indicating if this request awaits a response.
     */
    val isPending: Boolean
        get() = status == FriendRequestStatus.PENDING
    
    /**
     * Generates a human-readable relative time description.
     * 
     * Returns descriptions like "5m ago" for recent requests or
     * formatted dates like "Jan 15" for older ones.
     * 
     * @return Relative time string for UI display.
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
     * Converts this FriendRequest to a Map for Firestore document writes.
     * 
     * Uses server timestamp for consistent ordering across time zones.
     * 
     * @return Map representation suitable for Firestore set/update operations.
     */
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "fromUserId" to fromUserId,
            "fromUserName" to fromUserName,
            "fromUserPhotoURL" to fromUserPhotoURL,
            "toUserId" to toUserId,
            "status" to status.value,
            "timestamp" to FieldValue.serverTimestamp()
        )
    }
    
    companion object {
        /**
         * Factory method to create a new friend request from user objects.
         * 
         * Extracts necessary display information from the sender for
         * notification rendering without additional queries.
         * 
         * @param fromUser The user initiating the friend request.
         * @param toUserId The target user's ID.
         * @return A new FriendRequest in PENDING status.
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
 * Enumeration of friend request lifecycle states.
 * 
 * @property value String value stored in Firestore for this status.
 */
enum class FriendRequestStatus(val value: String) {
    /** Request sent but not yet responded to by the recipient. */
    PENDING("pending"),
    
    /** Recipient accepted; users are now friends. */
    ACCEPTED("accepted"),
    
    /** Recipient declined the request. */
    REJECTED("rejected");
    
    companion object {
        /**
         * Parses a string value to the corresponding status enum.
         * 
         * @param value The string representation from Firestore.
         * @return The matching status, defaulting to PENDING if unknown.
         */
        fun fromString(value: String): FriendRequestStatus {
            return entries.firstOrNull { it.value == value } ?: PENDING
        }
    }
}

/**
 * Data model representing a user notification for in-app events.
 * 
 * Notifications inform users of social activity and platform events.
 * They appear in the notification center and can optionally trigger
 * push notifications when the app supports FCM integration.
 * 
 * **Storage Location:** `users/{uid}/notifications/{notificationId}`
 * 
 * @property id Firestore document ID for this notification.
 * @property type The [NotificationType] categorizing this event.
 * @property fromUserId User ID of the person who triggered the notification.
 * @property fromUserName Display name for notification text.
 * @property message Primary notification text content.
 * @property read Whether the user has viewed this notification.
 * @property timestamp Server timestamp when the event occurred.
 * 
 * @see NotificationType for the categories of trackable events.
 * @see NotificationsScreen for the UI displaying notifications.
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
     * Generates a human-readable relative time description.
     * 
     * @return Relative time string for UI display.
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
 * Enumeration of notification event types.
 * 
 * Each type corresponds to a specific platform event that generates
 * a notification for the affected user.
 * 
 * @property value String value stored in Firestore for this type.
 */
enum class NotificationType(val value: String) {
    /** Someone sent a friend request to this user. */
    FRIEND_REQUEST("friend_request"),
    
    /** Someone accepted this user's friend request. */
    FRIEND_ACCEPTED("friend_accepted"),
    
    /** New chat message received (for future real-time chat). */
    NEW_MESSAGE("new_message"),
    
    /** A followed editor updated one of their games. */
    GAME_UPDATE("game_update");
    
    companion object {
        /**
         * Parses a string value to the corresponding type enum.
         * 
         * @param value The string representation from Firestore.
         * @return The matching type, defaulting to FRIEND_REQUEST if unknown.
         */
        fun fromString(value: String): NotificationType {
            return entries.firstOrNull { it.value == value } ?: FRIEND_REQUEST
        }
    }
}
