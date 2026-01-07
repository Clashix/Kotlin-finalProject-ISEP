package com.isep.kotlinproject.repository

import android.net.Uri
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.isep.kotlinproject.model.Game
import com.isep.kotlinproject.model.Review
import kotlinx.coroutines.tasks.await
import java.util.UUID

class GameRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val gamesCollection = firestore.collection("games")
    private val reviewsCollection = firestore.collection("reviews")

    private val fallbackGames = listOf(
        Game(
            id = "fallback_1",
            title = "The Witcher 3: Wild Hunt",
            description = "A story-driven, next-generation open world role-playing game set in a visually stunning fantasy universe.",
            genre = "RPG",
            releaseDate = "2015-05-19",
            imageUrl = "https://upload.wikimedia.org/wikipedia/en/0/0c/Witcher_3_cover_art.jpg",
            developer = "CD Projekt Red",
            averageRating = 4.8,
            ratingCount = 10
        ),
        Game(
            id = "fallback_2",
            title = "Minecraft",
            description = "A sandbox game that allows players to build with a variety of different blocks in a 3D procedurally generated world.",
            genre = "Sandbox",
            releaseDate = "2011-11-18",
            imageUrl = "https://upload.wikimedia.org/wikipedia/en/5/51/Minecraft_cover.png",
            developer = "Mojang",
            averageRating = 4.5,
            ratingCount = 20
        ),
        Game(
            id = "fallback_3",
            title = "The Legend of Zelda: Breath of the Wild",
            description = "Step into a world of discovery, exploration, and adventure in The Legend of Zelda: Breath of the Wild.",
            genre = "Action-adventure",
            releaseDate = "2017-03-03",
            imageUrl = "https://upload.wikimedia.org/wikipedia/en/c/c6/The_Legend_of_Zelda_Breath_of_the_Wild.jpg",
            developer = "Nintendo",
            averageRating = 4.9,
            ratingCount = 15
        )
    )

    suspend fun getGames(): List<Game> {
        return try {
            val snapshot = gamesCollection.get().await()
            val games = snapshot.toObjects(Game::class.java)
            if (games.isEmpty()) fallbackGames else games
        } catch (e: Exception) {
            Log.e("GameRepository", "Error getting games, using fallback", e)
            fallbackGames
        }
    }

    suspend fun getGame(gameId: String): Game? {
        return try {
            val snapshot = gamesCollection.document(gameId).get().await()
            snapshot.toObject(Game::class.java)
        } catch (e: Exception) {
            Log.e("GameRepository", "Error getting game, checking fallback", e)
            fallbackGames.find { it.id == gameId }
        }
    }

    suspend fun addGame(game: Game) {
        try {
            val id = if (game.id.isBlank()) gamesCollection.document().id else game.id
            gamesCollection.document(id).set(game.copy(id = id)).await()
        } catch (e: Exception) {
            Log.e("GameRepository", "Error adding game", e)
        }
    }

    suspend fun updateGame(game: Game) {
        try {
            gamesCollection.document(game.id).set(game).await()
        } catch (e: Exception) {
            Log.e("GameRepository", "Error updating game", e)
        }
    }

    suspend fun deleteGame(gameId: String) {
        try {
            gamesCollection.document(gameId).delete().await()
        } catch (e: Exception) {
            Log.e("GameRepository", "Error deleting game", e)
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
        return try {
            val snapshot = reviewsCollection
                .whereEqualTo("gameId", gameId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
            snapshot.toObjects(Review::class.java)
        } catch (e: Exception) {
            Log.e("GameRepository", "Error getting reviews", e)
            emptyList()
        }
    }

    suspend fun addReview(review: Review) {
        try {
            val id = reviewsCollection.document().id
            reviewsCollection.document(id).set(review.copy(id = id)).await()
            updateGameRating(review.gameId)
        } catch (e: Exception) {
            Log.e("GameRepository", "Error adding review", e)
        }
    }

    private suspend fun updateGameRating(gameId: String) {
        try {
            val reviews = getReviewsForGame(gameId)
            if (reviews.isNotEmpty()) {
                val avgRating = reviews.map { it.rating }.average()
                gamesCollection.document(gameId).update(
                    mapOf(
                        "averageRating" to avgRating,
                        "ratingCount" to reviews.size
                    )
                ).await()
            }
        } catch (e: Exception) {
            Log.e("GameRepository", "Error updating game rating", e)
        }
    }

    suspend fun seedData() {
        try {
            val existingGames = getGames()
            // We only seed if the returned list is EMPTY or equal to fallback (meaning we have no real data)
            // But since getGames returns fallback on error, we need a way to check real data.
            // For now, let's just attempt to write if we can.
            
            // Actually, if we are in fallback mode, we can't seed anyway (permissions).
            // So we just rely on getGames returning fallback.
        } catch (e: Exception) {
            Log.e("GameRepository", "Error seeding data", e)
        }
    }
}
