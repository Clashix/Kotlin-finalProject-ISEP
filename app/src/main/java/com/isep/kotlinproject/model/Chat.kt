package com.isep.kotlinproject.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

/**
 * Chat conversation between two users.
 */
data class Chat(
    val id: String = "",
    val participants: List<String> = emptyList(),
    val participantNames: Map<String, String> = emptyMap(),
    val lastMessage: String = "",
    val lastMessageTimestamp: Timestamp? = null,
    
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    
    // Local state (not stored in Firestore)
    @Transient
    var unreadCount: Int = 0
) {
    /**
     * Get the other participant's ID (for current user context)
     */
    fun getOtherParticipantId(currentUserId: String): String? {
        return participants.firstOrNull { it != currentUserId }
    }
    
    /**
     * Get the other participant's name (for current user context)
     */
    fun getOtherParticipantName(currentUserId: String): String {
        val otherId = getOtherParticipantId(currentUserId)
        return participantNames[otherId] ?: "Unknown User"
    }
    
    /**
     * Get formatted last message timestamp
     */
    fun getFormattedTime(): String {
        val timestamp = lastMessageTimestamp ?: return ""
        val date = timestamp.toDate()
        val now = System.currentTimeMillis()
        val diff = now - date.time
        
        return when {
            diff < 60_000 -> "Now"
            diff < 3_600_000 -> "${diff / 60_000}m"
            diff < 86_400_000 -> "${diff / 3_600_000}h"
            diff < 604_800_000 -> "${diff / 86_400_000}d"
            else -> {
                val format = java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault())
                format.format(date)
            }
        }
    }
    
    /**
     * Create a chat ID from two user IDs (deterministic)
     */
    companion object {
        fun createChatId(userId1: String, userId2: String): String {
            return if (userId1 < userId2) {
                "${userId1}_${userId2}"
            } else {
                "${userId2}_${userId1}"
            }
        }
        
        /**
         * Create a new chat between two users
         */
        fun create(
            user1Id: String,
            user1Name: String,
            user2Id: String,
            user2Name: String
        ): Chat {
            val chatId = createChatId(user1Id, user2Id)
            return Chat(
                id = chatId,
                participants = listOf(user1Id, user2Id),
                participantNames = mapOf(
                    user1Id to user1Name,
                    user2Id to user2Name
                ),
                lastMessage = "",
                lastMessageTimestamp = null
            )
        }
    }
}

/**
 * Single message in a chat conversation.
 */
data class Message(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val content: String = "",
    val timestamp: Timestamp? = null,
    val read: Boolean = false
) {
    /**
     * Check if this message was sent by the given user
     */
    fun isSentBy(userId: String): Boolean = senderId == userId
    
    /**
     * Get formatted timestamp
     */
    fun getFormattedTime(): String {
        val ts = timestamp ?: return ""
        val format = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
        return format.format(ts.toDate())
    }
    
    /**
     * Get formatted date for message grouping
     */
    fun getFormattedDate(): String {
        val ts = timestamp ?: return ""
        val date = ts.toDate()
        val now = java.util.Calendar.getInstance()
        val msgDate = java.util.Calendar.getInstance().apply { time = date }
        
        return when {
            now.get(java.util.Calendar.DATE) == msgDate.get(java.util.Calendar.DATE) &&
            now.get(java.util.Calendar.YEAR) == msgDate.get(java.util.Calendar.YEAR) -> "Today"
            
            now.get(java.util.Calendar.DATE) - 1 == msgDate.get(java.util.Calendar.DATE) &&
            now.get(java.util.Calendar.YEAR) == msgDate.get(java.util.Calendar.YEAR) -> "Yesterday"
            
            else -> {
                val format = java.text.SimpleDateFormat("EEEE, MMM dd", java.util.Locale.getDefault())
                format.format(date)
            }
        }
    }
    
    /**
     * Convert to map for Firestore writes
     */
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "senderId" to senderId,
            "senderName" to senderName,
            "content" to content,
            "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
            "read" to read
        )
    }
    
    companion object {
        /**
         * Create a new message
         */
        fun create(
            senderId: String,
            senderName: String,
            content: String
        ): Message {
            return Message(
                senderId = senderId,
                senderName = senderName,
                content = content,
                read = false
            )
        }
    }
}
