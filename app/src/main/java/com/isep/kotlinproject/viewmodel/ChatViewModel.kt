package com.isep.kotlinproject.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.isep.kotlinproject.model.Chat
import com.isep.kotlinproject.model.Message
import com.isep.kotlinproject.repository.ChatRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for real-time chat functionality.
 */
class ChatViewModel : ViewModel() {
    
    private val repository = ChatRepository()
    private val auth = FirebaseAuth.getInstance()
    
    // =====================================================
    // STATE
    // =====================================================
    
    // All chats for current user
    private val _chats = MutableStateFlow<List<Chat>>(emptyList())
    val chats: StateFlow<List<Chat>> = _chats.asStateFlow()
    
    // Current chat being viewed
    private val _currentChat = MutableStateFlow<Chat?>(null)
    val currentChat: StateFlow<Chat?> = _currentChat.asStateFlow()
    
    // Messages in current chat
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()
    
    // Typing indicator
    private val _isOtherTyping = MutableStateFlow(false)
    val isOtherTyping: StateFlow<Boolean> = _isOtherTyping.asStateFlow()
    
    // Total unread count
    private val _totalUnreadCount = MutableStateFlow(0)
    val totalUnreadCount: StateFlow<Int> = _totalUnreadCount.asStateFlow()
    
    // Loading states
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()
    
    // Error state
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    // Message input
    private val _messageInput = MutableStateFlow("")
    val messageInput: StateFlow<String> = _messageInput.asStateFlow()
    
    // Jobs for managing subscriptions
    private var chatsJob: Job? = null
    private var messagesJob: Job? = null
    private var typingJob: Job? = null
    
    // =====================================================
    // INITIALIZATION
    // =====================================================
    
    init {
        observeChats()
    }
    
    private fun observeChats() {
        chatsJob?.cancel()
        chatsJob = viewModelScope.launch {
            repository.getChatsFlow().collect { chatList ->
                _chats.value = chatList
                updateTotalUnreadCount()
            }
        }
    }
    
    private fun updateTotalUnreadCount() {
        viewModelScope.launch {
            _totalUnreadCount.value = repository.getTotalUnreadCount()
        }
    }
    
    // =====================================================
    // CHAT OPERATIONS
    // =====================================================
    
    /**
     * Open or create a chat with another user
     */
    fun openChat(otherUserId: String, otherUserName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val chat = repository.getOrCreateChat(otherUserId, otherUserName)
                _currentChat.value = chat
                chat?.let {
                    observeMessages(it.id)
                    observeTypingStatus(it.id)
                    markAsRead(it.id)
                }
            } catch (e: Exception) {
                _error.value = "Failed to open chat"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Open an existing chat by ID
     */
    fun openChatById(chatId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                observeChat(chatId)
                observeMessages(chatId)
                observeTypingStatus(chatId)
                markAsRead(chatId)
            } catch (e: Exception) {
                _error.value = "Failed to open chat"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private fun observeChat(chatId: String) {
        viewModelScope.launch {
            repository.getChatFlow(chatId).collect { chat ->
                _currentChat.value = chat
            }
        }
    }
    
    private fun observeMessages(chatId: String) {
        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            repository.getMessagesFlow(chatId).collect { messageList ->
                _messages.value = messageList
            }
        }
    }
    
    private fun observeTypingStatus(chatId: String) {
        typingJob?.cancel()
        typingJob = viewModelScope.launch {
            repository.getTypingStatusFlow(chatId).collect { isTyping ->
                _isOtherTyping.value = isTyping
            }
        }
    }
    
    /**
     * Close current chat (stop listening)
     */
    fun closeChat() {
        messagesJob?.cancel()
        typingJob?.cancel()
        _currentChat.value = null
        _messages.value = emptyList()
        _isOtherTyping.value = false
        _messageInput.value = ""
    }
    
    /**
     * Delete a chat
     */
    fun deleteChat(chatId: String) {
        viewModelScope.launch {
            val success = repository.deleteChat(chatId)
            if (!success) {
                _error.value = "Failed to delete chat"
            } else if (_currentChat.value?.id == chatId) {
                closeChat()
            }
        }
    }
    
    // =====================================================
    // MESSAGE OPERATIONS
    // =====================================================
    
    /**
     * Update message input
     */
    fun updateMessageInput(text: String) {
        _messageInput.value = text
        
        // Update typing indicator
        _currentChat.value?.let { chat ->
            viewModelScope.launch {
                repository.setTyping(chat.id, text.isNotBlank())
            }
        }
    }
    
    /**
     * Send a message
     */
    fun sendMessage() {
        val chatId = _currentChat.value?.id ?: return
        val content = _messageInput.value.trim()
        
        if (content.isBlank()) return
        
        viewModelScope.launch {
            _isSending.value = true
            try {
                val success = repository.sendMessage(chatId, content)
                if (success) {
                    _messageInput.value = ""
                    repository.setTyping(chatId, false)
                } else {
                    _error.value = "Failed to send message"
                }
            } catch (e: Exception) {
                _error.value = "Failed to send message"
            } finally {
                _isSending.value = false
            }
        }
    }
    
    /**
     * Mark messages as read
     */
    private fun markAsRead(chatId: String) {
        viewModelScope.launch {
            repository.markMessagesAsRead(chatId)
            updateTotalUnreadCount()
        }
    }
    
    /**
     * Delete a message
     */
    fun deleteMessage(messageId: String) {
        val chatId = _currentChat.value?.id ?: return
        
        viewModelScope.launch {
            val success = repository.deleteMessage(chatId, messageId)
            if (!success) {
                _error.value = "Failed to delete message"
            }
        }
    }
    
    // =====================================================
    // UTILITY
    // =====================================================
    
    /**
     * Get current user ID
     */
    fun getCurrentUserId(): String? = auth.currentUser?.uid
    
    /**
     * Check if a message was sent by current user
     */
    fun isMyMessage(message: Message): Boolean {
        return message.senderId == auth.currentUser?.uid
    }
    
    /**
     * Get other participant's name in current chat
     */
    fun getOtherParticipantName(): String {
        val userId = auth.currentUser?.uid ?: return "Unknown"
        return _currentChat.value?.getOtherParticipantName(userId) ?: "Unknown"
    }
    
    /**
     * Get other participant's ID in current chat
     */
    fun getOtherParticipantId(): String? {
        val userId = auth.currentUser?.uid ?: return null
        return _currentChat.value?.getOtherParticipantId(userId)
    }
    
    /**
     * Clear error
     */
    fun clearError() {
        _error.value = null
    }
    
    /**
     * Refresh chats
     */
    fun refresh() {
        observeChats()
    }
    
    override fun onCleared() {
        super.onCleared()
        chatsJob?.cancel()
        messagesJob?.cancel()
        typingJob?.cancel()
    }
}
