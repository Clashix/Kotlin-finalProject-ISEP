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

class GameRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    
    // Global games collection
    private val gamesCollection = firestore.collection("games")
    
    // Get reviews subcollection for a game
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
}
