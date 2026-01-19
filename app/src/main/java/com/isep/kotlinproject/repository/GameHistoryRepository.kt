package com.isep.kotlinproject.repository

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.isep.kotlinproject.model.FieldChange
import com.isep.kotlinproject.model.Game
import com.isep.kotlinproject.model.GameAction
import com.isep.kotlinproject.model.GameHistory
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repository for game edit history operations.
 * Tracks create/update/delete actions by editors.
 */
class GameHistoryRepository {
    
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val historyCollection = firestore.collection("game_history")
    
    companion object {
        private const val TAG = "GameHistoryRepository"
        private const val PAGE_SIZE = 20
    }
    
    /**
     * Record a game creation
     */
    suspend fun recordCreate(game: Game): Boolean {
        val user = auth.currentUser ?: return false
        
        return try {
            val history = GameHistory(
                gameId = game.id,
                gameTitle = game.title,
                editorId = user.uid,
                editorName = game.editorName,
                action = GameAction.CREATE
            )
            historyCollection.add(history.toMap()).await()
            Log.d(TAG, "Recorded CREATE for game ${game.id}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error recording create", e)
            false
        }
    }
    
    /**
     * Record a game update with changed fields
     */
    suspend fun recordUpdate(
        oldGame: Game,
        newGame: Game
    ): Boolean {
        val user = auth.currentUser ?: return false
        
        // Calculate changed fields
        val changedFields = mutableMapOf<String, FieldChange>()
        
        if (oldGame.title != newGame.title) {
            changedFields["title"] = FieldChange(oldGame.title, newGame.title)
        }
        if (oldGame.description != newGame.description) {
            changedFields["description"] = FieldChange(
                oldGame.description.take(100), 
                newGame.description.take(100)
            )
        }
        if (oldGame.genre != newGame.genre) {
            changedFields["genre"] = FieldChange(oldGame.genre, newGame.genre)
        }
        if (oldGame.releaseDate != newGame.releaseDate) {
            changedFields["releaseDate"] = FieldChange(oldGame.releaseDate, newGame.releaseDate)
        }
        if (oldGame.developer != newGame.developer) {
            changedFields["developer"] = FieldChange(oldGame.developer, newGame.developer)
        }
        if (oldGame.imageUrl != newGame.imageUrl) {
            changedFields["imageUrl"] = FieldChange("(old image)", "(new image)")
        }
        
        if (changedFields.isEmpty()) {
            Log.d(TAG, "No changes detected, skipping history record")
            return true
        }
        
        return try {
            val history = GameHistory(
                gameId = newGame.id,
                gameTitle = newGame.title,
                editorId = user.uid,
                editorName = newGame.editorName,
                action = GameAction.UPDATE,
                changedFields = changedFields
            )
            historyCollection.add(history.toMap()).await()
            Log.d(TAG, "Recorded UPDATE for game ${newGame.id}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error recording update", e)
            false
        }
    }
    
    /**
     * Record a game deletion
     */
    suspend fun recordDelete(game: Game): Boolean {
        val user = auth.currentUser ?: return false
        
        return try {
            val history = GameHistory(
                gameId = game.id,
                gameTitle = game.title,
                editorId = user.uid,
                editorName = game.editorName,
                action = GameAction.DELETE
            )
            historyCollection.add(history.toMap()).await()
            Log.d(TAG, "Recorded DELETE for game ${game.id}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error recording delete", e)
            false
        }
    }
    
    /**
     * Get history for a specific game as Flow
     */
    fun getGameHistoryFlow(gameId: String): Flow<List<GameHistory>> = callbackFlow {
        val subscription = historyCollection
            .whereEqualTo("gameId", gameId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to game history", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val history = parseHistoryDocuments(snapshot?.documents)
                trySend(history)
            }
        
        awaitClose { subscription.remove() }
    }
    
    /**
     * Get all history for an editor as Flow
     */
    fun getEditorHistoryFlow(editorId: String): Flow<List<GameHistory>> = callbackFlow {
        val subscription = historyCollection
            .whereEqualTo("editorId", editorId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(100)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to editor history", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val history = parseHistoryDocuments(snapshot?.documents)
                trySend(history)
            }
        
        awaitClose { subscription.remove() }
    }
    
    /**
     * Get editor history one-time fetch
     */
    suspend fun getEditorHistory(editorId: String, limit: Int = PAGE_SIZE): List<GameHistory> {
        return try {
            val snapshot = historyCollection
                .whereEqualTo("editorId", editorId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()
            
            parseHistoryDocuments(snapshot.documents)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting editor history", e)
            emptyList()
        }
    }
    
    @Suppress("UNCHECKED_CAST")
    private fun parseHistoryDocuments(documents: List<com.google.firebase.firestore.DocumentSnapshot>?): List<GameHistory> {
        return documents?.mapNotNull { doc ->
            try {
                val data = doc.data ?: return@mapNotNull null
                val changedFieldsRaw = data["changedFields"] as? Map<String, Map<String, Any?>>
                val changedFields = changedFieldsRaw?.mapValues { (_, value) ->
                    FieldChange.fromMap(value)
                } ?: emptyMap()
                
                GameHistory(
                    id = doc.id,
                    gameId = data["gameId"] as? String ?: "",
                    gameTitle = data["gameTitle"] as? String ?: "",
                    editorId = data["editorId"] as? String ?: "",
                    editorName = data["editorName"] as? String ?: "",
                    action = try {
                        GameAction.valueOf(data["action"] as? String ?: "CREATE")
                    } catch (e: Exception) {
                        GameAction.CREATE
                    },
                    changedFields = changedFields,
                    timestamp = data["timestamp"] as? Timestamp
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing history document", e)
                null
            }
        } ?: emptyList()
    }
}
