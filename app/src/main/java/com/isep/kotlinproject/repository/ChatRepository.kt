package com.isep.kotlinproject.repository

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.isep.kotlinproject.model.Chat
import com.isep.kotlinproject.model.Message
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repository for real-time chat functionality between users.
 */
class ChatRepository {
    
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private val chatsCollection = firestore.collection("chats")
    
    companion object {
        private const val TAG = "ChatRepository"
        const val MESSAGES_PAGE_SIZE = 50
    }
    
    // =====================================================
    // CHAT OPERATIONS
    // =====================================================
    
    /**
     * Get or create a chat between current user and another user
     */
    suspend fun getOrCreateChat(
        otherUserId: String,
        otherUserName: String
    ): Chat? {
        val currentUser = auth.currentUser ?: return null
        val currentUserId = currentUser.uid
        val currentUserName = currentUser.displayName ?: "User"
        
        val chatId = Chat.createChatId(currentUserId, otherUserId)
        
        return try {
            val existingChat = chatsCollection.document(chatId).get().await()
            
            if (existingChat.exists()) {
                existingChat.toObject(Chat::class.java)?.copy(id = existingChat.id)
            } else {
                // Create new chat
                val newChat = Chat.create(
                    user1Id = currentUserId,
                    user1Name = currentUserName,
                    user2Id = otherUserId,
                    user2Name = otherUserName
                )
                
                val chatData = mapOf(
                    "participants" to newChat.participants,
                    "participantNames" to newChat.participantNames,
                    "lastMessage" to "",
                    "lastMessageTimestamp" to null,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                
                chatsCollection.document(chatId).set(chatData).await()
                newChat
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting or creating chat", e)
            null
        }
    }
    
    /**
     * Get all chats for current user as Flow
     */
    fun getChatsFlow(): Flow<List<Chat>> = callbackFlow {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        
        val subscription = chatsCollection
            .whereArrayContains("participants", userId)
            .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to chats", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val chats = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Chat::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                
                trySend(chats)
            }
        
        awaitClose { subscription.remove() }
    }
    
    /**
     * Get a specific chat as Flow
     */
    fun getChatFlow(chatId: String): Flow<Chat?> = callbackFlow {
        val subscription = chatsCollection.document(chatId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to chat $chatId", error)
                    trySend(null)
                    return@addSnapshotListener
                }
                
                val chat = snapshot?.toObject(Chat::class.java)?.copy(id = snapshot.id)
                trySend(chat)
            }
        
        awaitClose { subscription.remove() }
    }
    
    /**
     * Delete a chat
     */
    suspend fun deleteChat(chatId: String): Boolean {
        return try {
            // First delete all messages
            val messagesSnapshot = chatsCollection.document(chatId)
                .collection("messages")
                .get()
                .await()
            
            val batch = firestore.batch()
            messagesSnapshot.documents.forEach { doc ->
                batch.delete(doc.reference)
            }
            batch.delete(chatsCollection.document(chatId))
            batch.commit().await()
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting chat", e)
            false
        }
    }
    
    // =====================================================
    // MESSAGE OPERATIONS
    // =====================================================
    
    /**
     * Get messages for a chat as Flow (real-time)
     */
    fun getMessagesFlow(chatId: String): Flow<List<Message>> = callbackFlow {
        val subscription = chatsCollection.document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(MESSAGES_PAGE_SIZE.toLong())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to messages", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val messages = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Message::class.java)?.copy(id = doc.id)
                }?.reversed() ?: emptyList() // Reverse to show oldest first
                
                trySend(messages)
            }
        
        awaitClose { subscription.remove() }
    }
    
    /**
     * Send a message in a chat
     */
    suspend fun sendMessage(chatId: String, content: String): Boolean {
        val currentUser = auth.currentUser ?: return false
        
        if (content.isBlank()) return false
        
        return try {
            val message = Message.create(
                senderId = currentUser.uid,
                senderName = currentUser.displayName ?: "User",
                content = content.trim()
            )
            
            val batch = firestore.batch()
            
            // Add message
            val messageRef = chatsCollection.document(chatId)
                .collection("messages")
                .document()
            batch.set(messageRef, message.toMap())
            
            // Update chat's last message
            batch.update(chatsCollection.document(chatId), mapOf(
                "lastMessage" to content.take(100), // Truncate for preview
                "lastMessageTimestamp" to FieldValue.serverTimestamp()
            ))
            
            batch.commit().await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error sending message", e)
            false
        }
    }
    
    /**
     * Mark messages as read
     */
    suspend fun markMessagesAsRead(chatId: String): Boolean {
        val userId = auth.currentUser?.uid ?: return false
        
        return try {
            // Get unread messages from other user
            val unreadMessages = chatsCollection.document(chatId)
                .collection("messages")
                .whereEqualTo("read", false)
                .whereNotEqualTo("senderId", userId)
                .get()
                .await()
            
            if (unreadMessages.isEmpty) return true
            
            val batch = firestore.batch()
            unreadMessages.documents.forEach { doc ->
                batch.update(doc.reference, "read", true)
            }
            batch.commit().await()
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error marking messages as read", e)
            false
        }
    }
    
    /**
     * Get unread message count for a chat
     */
    suspend fun getUnreadCount(chatId: String): Int {
        val userId = auth.currentUser?.uid ?: return 0
        
        return try {
            val snapshot = chatsCollection.document(chatId)
                .collection("messages")
                .whereEqualTo("read", false)
                .get()
                .await()
            
            // Filter out messages sent by current user
            snapshot.documents.count { doc ->
                doc.getString("senderId") != userId
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting unread count", e)
            0
        }
    }
    
    /**
     * Get total unread message count across all chats
     */
    suspend fun getTotalUnreadCount(): Int {
        val userId = auth.currentUser?.uid ?: return 0
        
        return try {
            val chats = chatsCollection
                .whereArrayContains("participants", userId)
                .get()
                .await()
            
            var totalUnread = 0
            chats.documents.forEach { chatDoc ->
                totalUnread += getUnreadCount(chatDoc.id)
            }
            
            totalUnread
        } catch (e: Exception) {
            Log.e(TAG, "Error getting total unread count", e)
            0
        }
    }
    
    /**
     * Delete a message (sender only)
     */
    suspend fun deleteMessage(chatId: String, messageId: String): Boolean {
        val userId = auth.currentUser?.uid ?: return false
        
        return try {
            val messageRef = chatsCollection.document(chatId)
                .collection("messages")
                .document(messageId)
            
            val message = messageRef.get().await()
            
            // Verify sender
            if (message.getString("senderId") != userId) {
                Log.w(TAG, "Cannot delete message: not the sender")
                return false
            }
            
            messageRef.delete().await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting message", e)
            false
        }
    }
    
    /**
     * Load more messages (pagination)
     */
    suspend fun loadMoreMessages(
        chatId: String,
        beforeTimestamp: Timestamp
    ): List<Message> {
        return try {
            val snapshot = chatsCollection.document(chatId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .startAfter(beforeTimestamp)
                .limit(MESSAGES_PAGE_SIZE.toLong())
                .get()
                .await()
            
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(Message::class.java)?.copy(id = doc.id)
            }.reversed()
        } catch (e: Exception) {
            Log.e(TAG, "Error loading more messages", e)
            emptyList()
        }
    }
    
    // =====================================================
    // TYPING INDICATORS (Optional Enhancement)
    // =====================================================
    
    /**
     * Set typing status for current user in a chat
     */
    suspend fun setTyping(chatId: String, isTyping: Boolean): Boolean {
        val userId = auth.currentUser?.uid ?: return false
        
        return try {
            chatsCollection.document(chatId).update(
                "typing_$userId", if (isTyping) FieldValue.serverTimestamp() else FieldValue.delete()
            ).await()
            true
        } catch (e: Exception) {
            // Ignore typing indicator errors
            false
        }
    }
    
    /**
     * Check if other user is typing
     */
    fun getTypingStatusFlow(chatId: String): Flow<Boolean> = callbackFlow {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            trySend(false)
            close()
            return@callbackFlow
        }
        
        val subscription = chatsCollection.document(chatId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(false)
                    return@addSnapshotListener
                }
                
                val data = snapshot.data ?: run {
                    trySend(false)
                    return@addSnapshotListener
                }
                
                // Find typing indicator for other user
                val otherTyping = data.keys
                    .filter { it.startsWith("typing_") && !it.endsWith(userId) }
                    .any { key ->
                        val timestamp = data[key] as? Timestamp
                        if (timestamp != null) {
                            // Consider typing if updated within last 5 seconds
                            val fiveSecondsAgo = System.currentTimeMillis() - 5000
                            timestamp.toDate().time > fiveSecondsAgo
                        } else {
                            false
                        }
                    }
                
                trySend(otherTyping)
            }
        
        awaitClose { subscription.remove() }
    }
}
