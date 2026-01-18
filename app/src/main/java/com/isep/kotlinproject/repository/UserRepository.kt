package com.isep.kotlinproject.repository

import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.isep.kotlinproject.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Repository for user-related operations including profile management,
 * game lists (liked, played, wishlist), friends, and review history.
 */
class UserRepository {
    
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private val usersCollection = firestore.collection("users")
    private val gamesCollection = firestore.collection("games")
    private val friendRequestsCollection = firestore.collection("friend_requests")
    
    companion object {
        private const val TAG = "UserRepository"
        const val PAGE_SIZE = 20
    }
    
    // =====================================================
    // USER PROFILE OPERATIONS
    // =====================================================
    
    /**
     * Get current user's profile as a Flow (real-time updates)
     */
    fun getCurrentUserFlow(): Flow<User?> = callbackFlow {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            trySend(null)
            close()
            return@callbackFlow
        }
        
        val subscription = usersCollection.document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to user", error)
                    close(error)
                    return@addSnapshotListener
                }
                
                val user = snapshot?.toObject(User::class.java)?.copy(uid = snapshot.id)
                trySend(user)
            }
        
        awaitClose { subscription.remove() }
    }
    
    /**
     * Get a user by ID
     */
    suspend fun getUser(userId: String): User? {
        return try {
            val snapshot = usersCollection.document(userId).get().await()
            snapshot.toObject(User::class.java)?.copy(uid = snapshot.id)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user $userId", e)
            null
        }
    }
    
    /**
     * Get a user profile as Flow (for viewing other users)
     */
    fun getUserFlow(userId: String): Flow<User?> = callbackFlow {
        val subscription = usersCollection.document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to user $userId", error)
                    trySend(null)
                    return@addSnapshotListener
                }
                
                val user = snapshot?.toObject(User::class.java)?.copy(uid = snapshot.id)
                trySend(user)
            }
        
        awaitClose { subscription.remove() }
    }
    
    /**
     * Update user profile
     */
    suspend fun updateProfile(
        displayName: String? = null,
        photoURL: String? = null,
        locale: String? = null
    ): Boolean {
        val userId = auth.currentUser?.uid ?: return false
        
        return try {
            val updates = mutableMapOf<String, Any>()
            displayName?.let { 
                updates["displayName"] = it
                updates["name"] = it // Keep legacy field in sync
            }
            photoURL?.let { updates["photoURL"] = it }
            locale?.let { updates["locale"] = it }
            
            if (updates.isNotEmpty()) {
                usersCollection.document(userId).update(updates).await()
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating profile", e)
            false
        }
    }
    
    /**
     * Upload profile image and update user
     */
    suspend fun uploadProfileImage(imageUri: Uri): String? {
        val userId = auth.currentUser?.uid ?: return null
        
        return try {
            val ref = storage.reference.child("profile_images/$userId/${UUID.randomUUID()}")
            ref.putFile(imageUri).await()
            val downloadUrl = ref.downloadUrl.await().toString()
            
            usersCollection.document(userId).update("photoURL", downloadUrl).await()
            downloadUrl
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading profile image", e)
            null
        }
    }
    
    /**
     * Update user locale preference
     */
    suspend fun updateLocale(locale: String): Boolean {
        return updateProfile(locale = locale)
    }
    
    // =====================================================
    // GAME LISTS (LIKED, PLAYED, WISHLIST)
    // =====================================================
    
    /**
     * Add a game to liked games
     */
    suspend fun likeGame(gameId: String): Boolean {
        val userId = auth.currentUser?.uid ?: return false
        
        return try {
            usersCollection.document(userId).update(
                "likedGames", FieldValue.arrayUnion(gameId)
            ).await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error liking game", e)
            false
        }
    }
    
    /**
     * Remove a game from liked games
     */
    suspend fun unlikeGame(gameId: String): Boolean {
        val userId = auth.currentUser?.uid ?: return false
        
        return try {
            usersCollection.document(userId).update(
                "likedGames", FieldValue.arrayRemove(gameId)
            ).await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error unliking game", e)
            false
        }
    }
    
    /**
     * Toggle like status for a game
     */
    suspend fun toggleLike(gameId: String, currentlyLiked: Boolean): Boolean {
        return if (currentlyLiked) unlikeGame(gameId) else likeGame(gameId)
    }
    
    /**
     * Mark a game as played
     */
    suspend fun markAsPlayed(gameId: String): Boolean {
        val userId = auth.currentUser?.uid ?: return false
        
        return try {
            usersCollection.document(userId).update(
                "playedGames", FieldValue.arrayUnion(gameId)
            ).await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error marking game as played", e)
            false
        }
    }
    
    /**
     * Unmark a game as played
     */
    suspend fun unmarkAsPlayed(gameId: String): Boolean {
        val userId = auth.currentUser?.uid ?: return false
        
        return try {
            usersCollection.document(userId).update(
                "playedGames", FieldValue.arrayRemove(gameId)
            ).await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error unmarking game as played", e)
            false
        }
    }
    
    /**
     * Toggle played status for a game
     */
    suspend fun togglePlayed(gameId: String, currentlyPlayed: Boolean): Boolean {
        return if (currentlyPlayed) unmarkAsPlayed(gameId) else markAsPlayed(gameId)
    }
    
    /**
     * Add a game to wishlist
     */
    suspend fun addToWishlist(gameId: String): Boolean {
        val userId = auth.currentUser?.uid ?: return false
        
        return try {
            usersCollection.document(userId).update(
                "wishlist", FieldValue.arrayUnion(gameId)
            ).await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error adding to wishlist", e)
            false
        }
    }
    
    /**
     * Remove a game from wishlist
     */
    suspend fun removeFromWishlist(gameId: String): Boolean {
        val userId = auth.currentUser?.uid ?: return false
        
        return try {
            usersCollection.document(userId).update(
                "wishlist", FieldValue.arrayRemove(gameId)
            ).await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error removing from wishlist", e)
            false
        }
    }
    
    /**
     * Toggle wishlist status for a game
     */
    suspend fun toggleWishlist(gameId: String, currentlyInWishlist: Boolean): Boolean {
        return if (currentlyInWishlist) removeFromWishlist(gameId) else addToWishlist(gameId)
    }
    
    /**
     * Get games from a list of IDs
     */
    suspend fun getGamesByIds(gameIds: List<String>): List<Game> {
        if (gameIds.isEmpty()) return emptyList()
        
        return try {
            // Firestore limits 'in' queries to 30 items, so we need to batch
            val games = mutableListOf<Game>()
            gameIds.chunked(30).forEach { chunk ->
                val snapshot = gamesCollection
                    .whereIn("__name__", chunk.map { firestore.collection("games").document(it) })
                    .get()
                    .await()
                
                // Alternative approach using document IDs directly
                chunk.forEach { id ->
                    val doc = gamesCollection.document(id).get().await()
                    doc.toObject(Game::class.java)?.let { game ->
                        games.add(game.copy(id = doc.id))
                    }
                }
            }
            games
        } catch (e: Exception) {
            Log.e(TAG, "Error getting games by IDs", e)
            emptyList()
        }
    }
    
    /**
     * Get liked games for a user
     */
    suspend fun getLikedGames(userId: String): List<Game> {
        val user = getUser(userId) ?: return emptyList()
        return getGamesByIds(user.likedGames)
    }
    
    /**
     * Get played games for a user
     */
    suspend fun getPlayedGames(userId: String): List<Game> {
        val user = getUser(userId) ?: return emptyList()
        return getGamesByIds(user.playedGames)
    }
    
    /**
     * Get wishlist games for a user
     */
    suspend fun getWishlistGames(userId: String): List<Game> {
        val user = getUser(userId) ?: return emptyList()
        return getGamesByIds(user.wishlist)
    }
    
    // =====================================================
    // REVIEW HISTORY (PAGINATED)
    // =====================================================
    
    /**
     * Paginated result for reviews
     */
    data class PaginatedReviews(
        val reviews: List<Review>,
        val lastDocument: DocumentSnapshot?,
        val hasMore: Boolean
    )
    
    /**
     * Get user's review history with pagination using Collection Group Query
     */
    suspend fun getUserReviews(
        userId: String,
        lastDocument: DocumentSnapshot? = null,
        pageSize: Int = PAGE_SIZE
    ): PaginatedReviews {
        Log.d(TAG, "getUserReviews: fetching reviews for userId=$userId")
        return try {
            var query = firestore.collectionGroup("reviews")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(pageSize.toLong() + 1)
            
            if (lastDocument != null) {
                query = query.startAfter(lastDocument)
            }
            
            val snapshot = query.get().await()
            val documents = snapshot.documents
            
            Log.d(TAG, "getUserReviews: found ${documents.size} reviews for userId=$userId")
            
            val hasMore = documents.size > pageSize
            val reviewDocs = if (hasMore) documents.dropLast(1) else documents
            
            val reviews = reviewDocs.mapNotNull { doc ->
                val review = doc.toObject(Review::class.java)?.copy(id = doc.id)
                Log.d(TAG, "getUserReviews: parsed review id=${doc.id}, gameId=${review?.gameId}")
                review
            }
            
            PaginatedReviews(
                reviews = reviews,
                lastDocument = reviewDocs.lastOrNull(),
                hasMore = hasMore
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user reviews: ${e.message}", e)
            // Check if it's an index error
            if (e.message?.contains("index") == true) {
                Log.e(TAG, "INDEX REQUIRED: Create a composite index for reviews collection group with userId (ASC) and timestamp (DESC)")
            }
            PaginatedReviews(emptyList(), null, false)
        }
    }
    
    /**
     * Get user's reviews with game details
     */
    suspend fun getUserReviewsWithGames(
        userId: String,
        lastDocument: DocumentSnapshot? = null,
        pageSize: Int = PAGE_SIZE
    ): Pair<List<ReviewWithGame>, DocumentSnapshot?> {
        val paginatedReviews = getUserReviews(userId, lastDocument, pageSize)
        
        val reviewsWithGames = paginatedReviews.reviews.map { review ->
            val game = try {
                gamesCollection.document(review.gameId).get().await()
                    .toObject(Game::class.java)?.copy(id = review.gameId)
            } catch (e: Exception) {
                null
            }
            ReviewWithGame(review, game)
        }
        
        return Pair(reviewsWithGames, paginatedReviews.lastDocument)
    }
    
    /**
     * Get review count for a user
     */
    suspend fun getUserReviewCount(userId: String): Int {
        return try {
            val snapshot = firestore.collectionGroup("reviews")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            snapshot.size()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting review count", e)
            0
        }
    }
    
    // =====================================================
    // FRIENDS OPERATIONS
    // =====================================================
    
    /**
     * Get user's friends list
     */
    suspend fun getFriends(userId: String): List<User> {
        val user = getUser(userId) ?: return emptyList()
        if (user.friends.isEmpty()) return emptyList()
        
        return try {
            val friends = mutableListOf<User>()
            user.friends.forEach { friendId ->
                getUser(friendId)?.let { friends.add(it) }
            }
            friends
        } catch (e: Exception) {
            Log.e(TAG, "Error getting friends", e)
            emptyList()
        }
    }
    
    /**
     * Get friends as Flow
     */
    fun getFriendsFlow(userId: String): Flow<List<User>> = callbackFlow {
        val subscription = usersCollection.document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to friends", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val user = snapshot?.toObject(User::class.java)
                if (user == null || user.friends.isEmpty()) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                // Fetch friend details
                CoroutineScope(Dispatchers.IO).launch {
                    val friends = getFriends(userId)
                    trySend(friends)
                }
            }
        
        awaitClose { subscription.remove() }
    }
    
    /**
     * Send a friend request
     */
    suspend fun sendFriendRequest(toUserId: String): Boolean {
        val currentUser = auth.currentUser ?: return false
        val fromUser = getUser(currentUser.uid) ?: return false
        
        // Check if already friends
        if (toUserId in fromUser.friends) {
            Log.w(TAG, "Already friends with $toUserId")
            return false
        }
        
        // Check if request already exists
        val existingRequest = friendRequestsCollection
            .whereEqualTo("fromUserId", currentUser.uid)
            .whereEqualTo("toUserId", toUserId)
            .whereEqualTo("status", "pending")
            .get()
            .await()
        
        if (!existingRequest.isEmpty) {
            Log.w(TAG, "Friend request already sent to $toUserId")
            return false
        }
        
        return try {
            val request = FriendRequest.create(fromUser, toUserId)
            friendRequestsCollection.add(request.toMap()).await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error sending friend request", e)
            false
        }
    }
    
    /**
     * Get pending friend requests for current user
     */
    fun getPendingFriendRequestsFlow(): Flow<List<FriendRequest>> = callbackFlow {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        
        val subscription = friendRequestsCollection
            .whereEqualTo("toUserId", userId)
            .whereEqualTo("status", "pending")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to friend requests", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val requests = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(FriendRequest::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                
                trySend(requests)
            }
        
        awaitClose { subscription.remove() }
    }
    
    /**
     * Accept a friend request
     */
    suspend fun acceptFriendRequest(requestId: String): Boolean {
        return try {
            friendRequestsCollection.document(requestId).update(
                "status", FriendRequestStatus.ACCEPTED.value
            ).await()
            // Cloud Function will handle adding to friends lists
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error accepting friend request", e)
            false
        }
    }
    
    /**
     * Reject a friend request
     */
    suspend fun rejectFriendRequest(requestId: String): Boolean {
        return try {
            friendRequestsCollection.document(requestId).update(
                "status", FriendRequestStatus.REJECTED.value
            ).await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error rejecting friend request", e)
            false
        }
    }
    
    /**
     * Remove a friend
     */
    suspend fun removeFriend(friendId: String): Boolean {
        val userId = auth.currentUser?.uid ?: return false
        
        return try {
            val batch = firestore.batch()
            
            // Remove from current user's friends list
            batch.update(usersCollection.document(userId),
                "friends", FieldValue.arrayRemove(friendId))
            
            // Remove from friend's friends list
            batch.update(usersCollection.document(friendId),
                "friends", FieldValue.arrayRemove(userId))
            
            batch.commit().await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error removing friend", e)
            false
        }
    }
    
    /**
     * Search users by name (case-insensitive using displayNameLower)
     */
    suspend fun searchUsers(query: String, limit: Int = 20): List<User> {
        if (query.isBlank()) return emptyList()
        
        val lowerQuery = query.lowercase()
        
        return try {
            val snapshot = usersCollection
                .orderBy("displayNameLower")
                .startAt(lowerQuery)
                .endAt(lowerQuery + "\uf8ff")
                .limit(limit.toLong())
                .get()
                .await()
            
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(User::class.java)?.copy(uid = doc.id)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error searching users", e)
            emptyList()
        }
    }
    
    // =====================================================
    // USERS DIRECTORY (PAGINATED)
    // =====================================================
    
    /**
     * Paginated result for users list
     */
    data class PaginatedUsers(
        val users: List<User>,
        val lastDocument: DocumentSnapshot?,
        val hasMore: Boolean
    )
    
    /**
     * Get all users with pagination (for public directory)
     * Uses 'name' field which exists in legacy data
     */
    suspend fun getAllUsers(
        lastDocument: DocumentSnapshot? = null,
        pageSize: Int = PAGE_SIZE
    ): PaginatedUsers {
        return try {
            // Fetch without ordering to avoid index requirements
            // Sort client-side for now
            val snapshot = if (lastDocument != null) {
                usersCollection
                    .limit(pageSize.toLong() + 1)
                    .startAfter(lastDocument)
                    .get()
                    .await()
            } else {
                usersCollection
                    .limit(pageSize.toLong() + 1)
                    .get()
                    .await()
            }
            
            val documents = snapshot.documents
            Log.d(TAG, "getAllUsers: fetched ${documents.size} documents")
            
            val hasMore = documents.size > pageSize
            val userDocs = if (hasMore) documents.dropLast(1) else documents
            
            val users = userDocs.mapNotNull { doc ->
                try {
                    val data = doc.data
                    Log.d(TAG, "Parsing user ${doc.id}: $data")
                    
                    // Manual parsing to handle legacy field names
                    User(
                        uid = doc.id,
                        displayName = data?.get("displayName") as? String 
                            ?: data?.get("name") as? String 
                            ?: "",
                        displayNameLower = (data?.get("displayNameLower") as? String)
                            ?: (data?.get("name") as? String)?.lowercase()
                            ?: "",
                        email = data?.get("email") as? String ?: "",
                        photoURL = data?.get("photoURL") as? String 
                            ?: data?.get("profileImageUrl") as? String 
                            ?: "",
                        bio = data?.get("bio") as? String ?: "",
                        _role = (data?.get("role") as? String)?.lowercase() ?: "player",
                        locale = data?.get("locale") as? String ?: "en",
                        friends = (data?.get("friends") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                        likedGames = (data?.get("likedGames") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                        playedGames = (data?.get("playedGames") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                        wishlist = (data?.get("wishlist") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                        wishlistSteamAppIds = (data?.get("wishlistSteamAppIds") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                        name = data?.get("name") as? String ?: ""
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing user ${doc.id}: ${e.message}", e)
                    null
                }
            }.sortedBy { it.getDisplayNameOrLegacy().lowercase() }
            
            Log.d(TAG, "getAllUsers: parsed ${users.size} users")
            
            PaginatedUsers(
                users = users,
                lastDocument = userDocs.lastOrNull(),
                hasMore = hasMore
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting all users: ${e.message}", e)
            PaginatedUsers(emptyList(), null, false)
        }
    }
    
    /**
     * Get users as Flow for real-time updates (limited for directory)
     */
    fun getUsersFlow(limit: Int = 50): Flow<List<User>> = callbackFlow {
        val subscription = usersCollection
            .orderBy("displayNameLower")
            .limit(limit.toLong())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to users", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val users = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(User::class.java)?.copy(uid = doc.id)
                } ?: emptyList()
                
                trySend(users)
            }
        
        awaitClose { subscription.remove() }
    }
    
    /**
     * Search users with pagination
     */
    suspend fun searchUsersPaginated(
        query: String,
        lastDocument: DocumentSnapshot? = null,
        pageSize: Int = PAGE_SIZE
    ): PaginatedUsers {
        if (query.isBlank()) return PaginatedUsers(emptyList(), null, false)
        
        val lowerQuery = query.lowercase()
        
        return try {
            var firestoreQuery = usersCollection
                .orderBy("displayNameLower")
                .startAt(lowerQuery)
                .endAt(lowerQuery + "\uf8ff")
                .limit(pageSize.toLong() + 1)
            
            if (lastDocument != null) {
                firestoreQuery = firestoreQuery.startAfter(lastDocument)
            }
            
            val snapshot = firestoreQuery.get().await()
            val documents = snapshot.documents
            
            val hasMore = documents.size > pageSize
            val userDocs = if (hasMore) documents.dropLast(1) else documents
            
            val users = userDocs.mapNotNull { doc ->
                doc.toObject(User::class.java)?.copy(uid = doc.id)
            }
            
            PaginatedUsers(
                users = users,
                lastDocument = userDocs.lastOrNull(),
                hasMore = hasMore
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error searching users paginated", e)
            PaginatedUsers(emptyList(), null, false)
        }
    }
    
    /**
     * Get public user profile (for viewing other users)
     */
    suspend fun getPublicProfile(userId: String): User? {
        return getUser(userId)
    }
    
    /**
     * Update user's displayNameLower when displayName changes
     */
    suspend fun updateDisplayName(displayName: String): Boolean {
        val userId = auth.currentUser?.uid ?: return false
        
        return try {
            usersCollection.document(userId).update(
                mapOf(
                    "displayName" to displayName,
                    "displayNameLower" to displayName.lowercase(),
                    "name" to displayName // Keep legacy field in sync
                )
            ).await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating display name", e)
            false
        }
    }
    
    /**
     * Update user bio
     */
    suspend fun updateBio(bio: String): Boolean {
        val userId = auth.currentUser?.uid ?: return false
        
        return try {
            usersCollection.document(userId).update("bio", bio).await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating bio", e)
            false
        }
    }
    
    /**
     * Add Steam App ID to wishlist
     */
    suspend fun addSteamToWishlist(steamAppId: String): Boolean {
        val userId = auth.currentUser?.uid ?: return false
        
        return try {
            usersCollection.document(userId).update(
                "wishlistSteamAppIds", FieldValue.arrayUnion(steamAppId)
            ).await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error adding Steam game to wishlist", e)
            false
        }
    }
    
    /**
     * Remove Steam App ID from wishlist
     */
    suspend fun removeSteamFromWishlist(steamAppId: String): Boolean {
        val userId = auth.currentUser?.uid ?: return false
        
        return try {
            usersCollection.document(userId).update(
                "wishlistSteamAppIds", FieldValue.arrayRemove(steamAppId)
            ).await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error removing Steam game from wishlist", e)
            false
        }
    }
    
    // =====================================================
    // NOTIFICATIONS
    // =====================================================
    
    /**
     * Get user notifications as Flow
     */
    fun getNotificationsFlow(): Flow<List<Notification>> = callbackFlow {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        
        val subscription = usersCollection.document(userId)
            .collection("notifications")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to notifications", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val notifications = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Notification::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                
                trySend(notifications)
            }
        
        awaitClose { subscription.remove() }
    }
    
    /**
     * Mark notification as read
     */
    suspend fun markNotificationAsRead(notificationId: String): Boolean {
        val userId = auth.currentUser?.uid ?: return false
        
        return try {
            usersCollection.document(userId)
                .collection("notifications")
                .document(notificationId)
                .update("read", true)
                .await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error marking notification as read", e)
            false
        }
    }
    
    /**
     * Get unread notification count
     */
    suspend fun getUnreadNotificationCount(): Int {
        val userId = auth.currentUser?.uid ?: return 0
        
        return try {
            val snapshot = usersCollection.document(userId)
                .collection("notifications")
                .whereEqualTo("read", false)
                .get()
                .await()
            snapshot.size()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting unread notification count", e)
            0
        }
    }
}
