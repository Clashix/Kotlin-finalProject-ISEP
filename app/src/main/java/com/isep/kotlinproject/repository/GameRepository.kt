package com.isep.kotlinproject.repository

import android.net.Uri
import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.isep.kotlinproject.model.Game
import com.isep.kotlinproject.model.Review
import com.isep.kotlinproject.model.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class GameRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    
    // Helper to get the collection for a specific user
    private fun getUserDocument(userId: String) = 
        firestore.collection("users").document(userId)
        
    // We no longer use global reviews collection
    // private val reviewsCollection = firestore.collection("reviews")

    fun getGamesFlow(userId: String): Flow<List<Game>> = callbackFlow {
        if (userId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        
        val subscription = getUserDocument(userId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("GameRepository", "Listen failed", error)
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val user = snapshot.toObject(User::class.java)
                trySend(user?.games ?: emptyList())
            } else {
                trySend(emptyList())
            }
        }
        awaitClose { subscription.remove() }
    }

    suspend fun getGames(userId: String): List<Game> {
        return try {
            val snapshot = getUserDocument(userId).get().await()
            val user = snapshot.toObject(User::class.java)
            user?.games ?: emptyList()
        } catch (e: Exception) {
            Log.e("GameRepository", "Error getting games", e)
            emptyList()
        }
    }

    suspend fun getGame(userId: String, gameId: String): Game? {
        val games = getGames(userId)
        return games.find { it.id == gameId }
    }

    suspend fun addGame(game: Game) {
        try {
            if (game.userId.isBlank()) return
             val id = if (game.id.isBlank()) UUID.randomUUID().toString() else game.id
             val gameToAdd = game.copy(id = id)
             
            getUserDocument(game.userId).update("games", FieldValue.arrayUnion(gameToAdd)).await()
        } catch (e: Exception) {
            Log.e("GameRepository", "Error adding game", e)
            throw e
        }
    }

    suspend fun updateGame(game: Game) {
        try {
            if (game.userId.isBlank()) return
            
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(getUserDocument(game.userId))
                val user = snapshot.toObject(User::class.java) ?: return@runTransaction
                
                val updatedGames = user.games.map { 
                    if (it.id == game.id) game else it 
                }
                
                transaction.update(getUserDocument(game.userId), "games", updatedGames)
            }.await()
            
        } catch (e: Exception) {
            Log.e("GameRepository", "Error updating game", e)
            throw e
        }
    }

    suspend fun deleteGame(userId: String, gameId: String) {
        try {
             firestore.runTransaction { transaction ->
                val snapshot = transaction.get(getUserDocument(userId))
                val user = snapshot.toObject(User::class.java) ?: return@runTransaction
                
                val updatedGames = user.games.filter { it.id != gameId }
                
                transaction.update(getUserDocument(userId), "games", updatedGames)
            }.await()
        } catch (e: Exception) {
            Log.e("GameRepository", "Error deleting game", e)
             throw e
        }
    }

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

    suspend fun getReviewsForGame(gameId: String): List<Review> {
         // This is tricky because we need the userId to find the game.
         // But this function signature only has gameId.
         // Since reviews are now PRIVATE, we rely on the ViewModel to have passed the userId
         // implicitly via `getGame`.
         // Ideally, we shouldn't fetch reviews separately anymore. They are part of the Game object.
         // I will return empty list here to avoid breaking callers, but the logic should move to using game.reviews directly.
         return emptyList()
    }

    suspend fun addReview(review: Review) {
        try {
            if (review.userId.isBlank()) return
            
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(getUserDocument(review.userId))
                val user = snapshot.toObject(User::class.java) ?: return@runTransaction
                
                // Find the game
                val gameIndex = user.games.indexOfFirst { it.id == review.gameId }
                if (gameIndex == -1) return@runTransaction
                
                val game = user.games[gameIndex]
                val id = if (review.id.isBlank()) UUID.randomUUID().toString() else review.id
                val newReview = review.copy(id = id)
                
                val updatedReviews = game.reviews + newReview
                val updatedGame = game.copy(
                    reviews = updatedReviews,
                    ratingCount = updatedReviews.size,
                    averageRating = updatedReviews.map { it.rating }.average()
                )
                
                val updatedGames = user.games.toMutableList()
                updatedGames[gameIndex] = updatedGame
                
                transaction.update(getUserDocument(review.userId), "games", updatedGames)
            }.await()
        } catch (e: Exception) {
            Log.e("GameRepository", "Error adding review", e)
            throw e
        }
    }

    suspend fun seedData() {
        // No-op
    }
}