package com.isep.kotlinproject.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

/**
 * Data model representing an audit log entry for game modifications.
 * 
 * The game history system provides accountability and traceability for
 * editor actions on the game catalog. Each entry captures who made what
 * change, when, and what values were affected.
 * 
 * **Use Cases:**
 * - Audit trail for content moderation.
 * - Rollback reference for accidental changes.
 * - Editor activity dashboard for analytics.
 * - Dispute resolution for ownership claims.
 * 
 * **Storage Location:** `game_history/{historyId}`
 * 
 * History entries are immutable once created. The repository layer prevents
 * updates or deletions to maintain audit integrity.
 * 
 * @property id Firestore document ID for this history entry.
 * @property gameId Reference to the affected game document.
 * @property gameTitle Game title at time of action (for display after deletion).
 * @property editorId User ID of the editor who performed the action.
 * @property editorName Editor's display name at time of action.
 * @property action The type of modification: CREATE, UPDATE, or DELETE.
 * @property changedFields For UPDATE actions, a map of field changes.
 * @property timestamp Server timestamp when the action was recorded.
 * 
 * @see GameAction for the types of trackable modifications.
 * @see FieldChange for the structure of change records.
 * @see GameHistoryRepository for recording and querying history.
 */
data class GameHistory(
    val id: String = "",
    val gameId: String = "",
    val gameTitle: String = "",
    val editorId: String = "",
    val editorName: String = "",
    val action: GameAction = GameAction.CREATE,
    val changedFields: Map<String, FieldChange> = emptyMap(),
    
    @ServerTimestamp
    val timestamp: Timestamp? = null
) {
    /**
     * Converts this GameHistory to a Map for Firestore document writes.
     * 
     * @return Map representation with timestamp set to current server time.
     */
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "gameId" to gameId,
            "gameTitle" to gameTitle,
            "editorId" to editorId,
            "editorName" to editorName,
            "action" to action.name,
            "changedFields" to changedFields.mapValues { (_, change) -> change.toMap() },
            "timestamp" to Timestamp.now()
        )
    }
    
    /**
     * Generates a human-readable summary of the action for display.
     * 
     * @return A concise description like "Created game 'Portal 2'" or "Updated description, genre".
     */
    fun getSummary(): String {
        return when (action) {
            GameAction.CREATE -> "Created game \"$gameTitle\""
            GameAction.UPDATE -> {
                val fieldNames = changedFields.keys.joinToString(", ")
                "Updated $fieldNames"
            }
            GameAction.DELETE -> "Deleted game \"$gameTitle\""
        }
    }
}

/**
 * Data model representing a single field modification in a game update.
 * 
 * Captures both the previous and new values for comparison and potential
 * rollback reference. Values are stored as strings for consistency, with
 * the repository layer handling type conversion as needed.
 * 
 * @property oldValue The field's value before the update.
 * @property newValue The field's value after the update.
 */
data class FieldChange(
    val oldValue: String = "",
    val newValue: String = ""
) {
    /**
     * Converts this FieldChange to a Map for Firestore storage.
     */
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "oldValue" to oldValue,
            "newValue" to newValue
        )
    }
    
    companion object {
        /**
         * Reconstructs a FieldChange from Firestore document data.
         * 
         * @param map The raw Firestore data map.
         * @return A FieldChange instance with the stored values.
         */
        fun fromMap(map: Map<String, Any?>?): FieldChange {
            return FieldChange(
                oldValue = map?.get("oldValue") as? String ?: "",
                newValue = map?.get("newValue") as? String ?: ""
            )
        }
    }
}

/**
 * Enumeration of game modification actions tracked in the history.
 * 
 * @property displayName User-friendly label for UI display.
 */
enum class GameAction(val displayName: String) {
    /** Initial game publication by an editor. */
    CREATE("Created"),
    
    /** Modification of existing game data. */
    UPDATE("Updated"),
    
    /** Permanent removal of a game from the catalog. */
    DELETE("Deleted")
}
