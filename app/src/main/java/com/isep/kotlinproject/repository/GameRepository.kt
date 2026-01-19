package com.isep.kotlinproject.repository

import android.net.Uri
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.isep.kotlinproject.model.Game
import com.isep.kotlinproject.model.Review
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Enumeration of available sorting strategies for the game catalog.
 * 
 * Each option provides a different perspective on the game collection:
 * - Alphabetical options for browsing.
 * - Rating-based options for quality discovery.
 * - Activity-based options for popular/trending content.
 * 
 * @property displayName User-facing label for the sort option dropdown.
 */
enum class GameSortOption(val displayName: String) {
    TITLE_ASC("Title (A-Z)"),
    TITLE_DESC("Title (Z-A)"),
    HIGHEST_RATED("Highest Rated"),
    LOWEST_RATED("Lowest Rated"),
    MOST_REVIEWED("Most Reviewed"),
    NEWEST("Newest"),
    TRENDING("Trending")
}

/**
 * Repository class providing data access operations for games and reviews.
 * 
 * This repository serves as the single source of truth for game-related data,
 * abstracting Firestore and Firebase Storage operations from the ViewModel layer.
 * It provides both real-time Flow-based listeners and one-shot query methods.
 * 
 * **Responsibilities:**
 * - CRUD operations for games (create, read, update, delete).
 * - Review management (add, update, delete, query).
 * - Image upload to Firebase Storage.
 * - Real-time listeners for game and review collections.
 * - Rating statistics computation.
 * 
 * **Firestore Structure:**
 * ```
 * games/{gameId}
 *   - Game document fields
 *   - reviews/{reviewId}  (subcollection)
 * ```
 * 
 * **Thread Safety:**
 * All operations are suspending functions or Flow-based, designed to be called
 * from coroutine scopes. Firebase SDK handles internal threading.
 * 
 * @see Game for the game data model.
 * @see Review for the review data model.
 * @see GameViewModel for the consumer of this repository.
 */
class GameRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    
    /** Reference to the global games collection in Firestore. */
    private val gamesCollection = firestore.collection("games")
    
    /**
     * Returns a reference to the reviews subcollection for a specific game.
     * 
     * @param gameId The Firestore document ID of the parent game.
     * @return CollectionReference for the game's reviews.
     */
    private fun getReviewsCollection(gameId: String) = 
        gamesCollection.document(gameId).collection("reviews")

    // ============== GAMES ==============
    
    /**
     * Get all games as a Flow (real-time updates)
     */
    fun getAllGamesFlow(): Flow<List<Game>> = callbackFlow {
        val subscription = gamesCollection
            .orderBy("title", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("GameRepository", "Listen failed", error)
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val games = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Game::class.java)?.copy(id = doc.id)
                    }
                    trySend(games)
                } else {
                    trySend(emptyList())
                }
            }
        awaitClose { subscription.remove() }
    }
    
    /**
     * Get games created by a specific editor
     */
    fun getGamesByEditorFlow(editorId: String): Flow<List<Game>> = callbackFlow {
        val subscription = gamesCollection
            .whereEqualTo("editorId", editorId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("GameRepository", "Listen failed", error)
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val games = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Game::class.java)?.copy(id = doc.id)
                    }
                    trySend(games)
                } else {
                    trySend(emptyList())
                }
            }
        awaitClose { subscription.remove() }
    }

    /**
     * Get a single game by ID
     */
    suspend fun getGame(gameId: String): Game? {
        return try {
            val snapshot = gamesCollection.document(gameId).get().await()
            snapshot.toObject(Game::class.java)?.copy(id = snapshot.id)
        } catch (e: Exception) {
            Log.e("GameRepository", "Error getting game", e)
            null
        }
    }

    /**
     * Add a new game (Editor only)
     */
    suspend fun addGame(game: Game): String {
        return try {
            val gameData = hashMapOf(
                "editorId" to game.editorId,
                "editorName" to game.editorName,
                "title" to game.title,
                "description" to game.description,
                "genre" to game.genre,
                "releaseDate" to game.releaseDate,
                "imageUrl" to game.imageUrl,
                "steamAppId" to game.steamAppId,
                "developer" to game.developer,
                "averageRating" to 0.0,
                "ratingCount" to 0,
                "totalRatingSum" to 0
            )
            val docRef = gamesCollection.add(gameData).await()
            docRef.id
        } catch (e: Exception) {
            Log.e("GameRepository", "Error adding game", e)
            throw e
        }
    }

    /**
     * Update an existing game (Editor only)
     */
    suspend fun updateGame(game: Game) {
        try {
            val gameData = hashMapOf(
                "title" to game.title,
                "description" to game.description,
                "genre" to game.genre,
                "releaseDate" to game.releaseDate,
                "imageUrl" to game.imageUrl,
                "steamAppId" to game.steamAppId,
                "developer" to game.developer
            )
            gamesCollection.document(game.id).update(gameData as Map<String, Any>).await()
        } catch (e: Exception) {
            Log.e("GameRepository", "Error updating game", e)
            throw e
        }
    }

    /**
     * Delete a game (Editor only)
     */
    suspend fun deleteGame(gameId: String) {
        try {
            // First delete all reviews
            val reviews = getReviewsCollection(gameId).get().await()
            for (doc in reviews.documents) {
                doc.reference.delete().await()
            }
            // Then delete the game
            gamesCollection.document(gameId).delete().await()
        } catch (e: Exception) {
            Log.e("GameRepository", "Error deleting game", e)
            throw e
        }
    }

    /**
     * Upload a game image
     */
    suspend fun uploadGameImage(imageUri: Uri): String {
        return try {
            val ref = storage.reference.child("game_images/${UUID.randomUUID()}")
            ref.putFile(imageUri).await()
            ref.downloadUrl.await().toString()
        } catch (e: Exception) {
            Log.e("GameRepository", "Error uploading image", e)
            ""
        }
    }

    // ============== REVIEWS ==============
    
    /**
     * Get all reviews for a game as a Flow
     */
    fun getReviewsForGameFlow(gameId: String): Flow<List<Review>> = callbackFlow {
        val subscription = getReviewsCollection(gameId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("GameRepository", "Listen failed for reviews", error)
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val reviews = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Review::class.java)?.copy(id = doc.id)
                    }
                    trySend(reviews)
                } else {
                    trySend(emptyList())
                }
            }
        awaitClose { subscription.remove() }
    }
    
    /**
     * Get a user's review for a specific game
     */
    suspend fun getUserReviewForGame(gameId: String, userId: String): Review? {
        return try {
            val snapshot = getReviewsCollection(gameId)
                .whereEqualTo("userId", userId)
                .limit(1)
                .get()
                .await()
            snapshot.documents.firstOrNull()?.let { doc ->
                doc.toObject(Review::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            Log.e("GameRepository", "Error getting user review", e)
            null
        }
    }

    /**
     * Add or update a review with star rating (Player only)
     * If user already has a review, it updates it. Otherwise creates new one.
     */
    suspend fun addOrUpdateReview(review: Review) {
        try {
            val existingReview = getUserReviewForGame(review.gameId, review.userId)
            
            firestore.runTransaction { transaction ->
                val gameRef = gamesCollection.document(review.gameId)
                val gameSnapshot = transaction.get(gameRef)
                val game = gameSnapshot.toObject(Game::class.java) ?: return@runTransaction
                
                var totalRatingSum = game.totalRatingSum
                var ratingCount = game.ratingCount
                
                if (existingReview != null) {
                    // Update existing review - adjust the sum
                    totalRatingSum = totalRatingSum - existingReview.rating + review.rating
                    
                    // Update review document
                    val reviewRef = getReviewsCollection(review.gameId).document(existingReview.id)
                    transaction.update(reviewRef, mapOf(
                        "rating" to review.rating,
                        "comment" to review.comment,
                        "timestamp" to System.currentTimeMillis()
                    ))
                } else {
                    // New review
                    totalRatingSum += review.rating
                    ratingCount++
                    
                    // Create new review document
                    val reviewData = hashMapOf(
                        "gameId" to review.gameId,
                        "userId" to review.userId,
                        "userName" to review.userName,
                        "rating" to review.rating,
                        "comment" to review.comment,
                        "timestamp" to System.currentTimeMillis()
                    )
                    val newReviewRef = getReviewsCollection(review.gameId).document()
                    transaction.set(newReviewRef, reviewData)
                }
                
                // Calculate new average
                val averageRating = if (ratingCount > 0) {
                    totalRatingSum.toDouble() / ratingCount
                } else {
                    0.0
                }
                
                // Update game stats
                transaction.update(gameRef, mapOf(
                    "totalRatingSum" to totalRatingSum,
                    "ratingCount" to ratingCount,
                    "averageRating" to averageRating
                ))
            }.await()
        } catch (e: Exception) {
            Log.e("GameRepository", "Error adding/updating review", e)
            throw e
        }
    }

    /**
     * Delete a review (Player can delete their own review)
     */
    suspend fun deleteReview(gameId: String, reviewId: String, reviewRating: Int) {
        try {
            firestore.runTransaction { transaction ->
                val gameRef = gamesCollection.document(gameId)
                val gameSnapshot = transaction.get(gameRef)
                val game = gameSnapshot.toObject(Game::class.java) ?: return@runTransaction
                
                // Update counts
                val totalRatingSum = maxOf(0, game.totalRatingSum - reviewRating)
                val ratingCount = maxOf(0, game.ratingCount - 1)
                val averageRating = if (ratingCount > 0) {
                    totalRatingSum.toDouble() / ratingCount
                } else {
                    0.0
                }
                
                // Delete review
                val reviewRef = getReviewsCollection(gameId).document(reviewId)
                transaction.delete(reviewRef)
                
                // Update game stats
                transaction.update(gameRef, mapOf(
                    "totalRatingSum" to totalRatingSum,
                    "ratingCount" to ratingCount,
                    "averageRating" to averageRating
                ))
            }.await()
        } catch (e: Exception) {
            Log.e("GameRepository", "Error deleting review", e)
            throw e
        }
    }
    
    // ============== ADVANCED SORTING ==============
    
    /**
     * Get all games with a specific sort order.
     * Note: Some sorting options require client-side processing due to Firestore limitations.
     * 
     * IMPLEMENTATION NOTES:
     * - TITLE_ASC/DESC: Uses Firestore ordering
     * - HIGHEST/LOWEST_RATED: Uses Firestore ordering on averageRating
     * - MOST_REVIEWED: Uses Firestore ordering on ratingCount
     * - NEWEST: Uses Firestore ordering on createdAt
     * - TRENDING: Client-side heuristic (recent activity + rating)
     */
    fun getGamesSortedFlow(sortOption: GameSortOption): Flow<List<Game>> = callbackFlow {
        val query = when (sortOption) {
            GameSortOption.TITLE_ASC -> 
                gamesCollection.orderBy("title", Query.Direction.ASCENDING)
            GameSortOption.TITLE_DESC -> 
                gamesCollection.orderBy("title", Query.Direction.DESCENDING)
            GameSortOption.HIGHEST_RATED -> 
                gamesCollection.orderBy("averageRating", Query.Direction.DESCENDING)
            GameSortOption.LOWEST_RATED -> 
                gamesCollection.orderBy("averageRating", Query.Direction.ASCENDING)
            GameSortOption.MOST_REVIEWED -> 
                gamesCollection.orderBy("ratingCount", Query.Direction.DESCENDING)
            GameSortOption.NEWEST -> 
                gamesCollection.orderBy("createdAt", Query.Direction.DESCENDING)
            GameSortOption.TRENDING -> 
                // Fetch all and sort client-side
                gamesCollection.orderBy("title", Query.Direction.ASCENDING)
        }
        
        val subscription = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("GameRepository", "Listen failed", error)
                close(error)
                return@addSnapshotListener
            }
            
            if (snapshot != null) {
                var games = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Game::class.java)?.copy(id = doc.id)
                }
                
                // Apply client-side trending sort if needed
                if (sortOption == GameSortOption.TRENDING) {
                    games = games.sortedByDescending { game ->
                        calculateTrendingScore(game)
                    }
                }
                
                trySend(games)
            } else {
                trySend(emptyList())
            }
        }
        
        awaitClose { subscription.remove() }
    }
    
    /**
     * Calculate trending score for a game.
     * 
     * HEURISTIC:
     * - Weight recent reviews more heavily
     * - Consider average rating
     * - Formula: (ratingCount * 2) + (averageRating * 10)
     * 
     * LIMITATION: This is a simplified heuristic. A production system would
     * track review timestamps and weight recent reviews more heavily.
     */
    private fun calculateTrendingScore(game: Game): Double {
        val reviewWeight = game.ratingCount * 2.0
        val ratingWeight = game.averageRating * 10.0
        
        // Bonus for games with more reviews (social proof)
        val popularityBonus = if (game.ratingCount >= 10) 20.0 
                              else if (game.ratingCount >= 5) 10.0 
                              else 0.0
        
        return reviewWeight + ratingWeight + popularityBonus
    }
    
    /**
     * Get recent reviews count for a game (last 7 days)
     * Used for more accurate trending calculation
     */
    suspend fun getRecentReviewCount(gameId: String, daysAgo: Int = 7): Int {
        return try {
            val cutoffTime = System.currentTimeMillis() - (daysAgo * 24 * 60 * 60 * 1000L)
            val snapshot = getReviewsCollection(gameId)
                .whereGreaterThan("timestamp", cutoffTime)
                .get()
                .await()
            snapshot.size()
        } catch (e: Exception) {
            Log.e("GameRepository", "Error getting recent review count", e)
            0
        }
    }
}
