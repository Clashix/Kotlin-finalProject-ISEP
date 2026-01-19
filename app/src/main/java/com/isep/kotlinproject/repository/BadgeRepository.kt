package com.isep.kotlinproject.repository

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.isep.kotlinproject.model.Badge
import com.isep.kotlinproject.model.BadgeType
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repository for badge/achievement operations.
 * 
 * IMPLEMENTATION NOTES:
 * - Badges are evaluated client-side based on Firestore queries
 * - Badge awarding is idempotent (checks for existing badges before creating)
 * - Without Cloud Functions, there's a small window for race conditions
 *   if the user opens the app on multiple devices simultaneously
 */
class BadgeRepository {
    
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    companion object {
        private const val TAG = "BadgeRepository"
    }
    
    /**
     * Get badges collection for a user
     */
    private fun getBadgesCollection(userId: String) = 
        firestore.collection("users").document(userId).collection("badges")
    
    /**
     * Get all badges for a user as Flow (real-time updates)
     */
    fun getUserBadgesFlow(userId: String): Flow<List<Badge>> = callbackFlow {
        val subscription = getBadgesCollection(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to badges", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val badges = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val data = doc.data
                        Badge(
                            id = doc.id,
                            type = BadgeType.valueOf(data?.get("type") as? String ?: "FIRST_REVIEW"),
                            name = data?.get("name") as? String ?: "",
                            description = data?.get("description") as? String ?: "",
                            iconName = data?.get("iconName") as? String ?: "",
                            earnedAt = data?.get("earnedAt") as? Timestamp
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing badge", e)
                        null
                    }
                } ?: emptyList()
                
                trySend(badges)
            }
        
        awaitClose { subscription.remove() }
    }
    
    /**
     * Get all badges for a user (one-time fetch)
     */
    suspend fun getUserBadges(userId: String): List<Badge> {
        return try {
            val snapshot = getBadgesCollection(userId).get().await()
            snapshot.documents.mapNotNull { doc ->
                try {
                    val data = doc.data
                    Badge(
                        id = doc.id,
                        type = BadgeType.valueOf(data?.get("type") as? String ?: "FIRST_REVIEW"),
                        name = data?.get("name") as? String ?: "",
                        description = data?.get("description") as? String ?: "",
                        iconName = data?.get("iconName") as? String ?: "",
                        earnedAt = data?.get("earnedAt") as? Timestamp
                    )
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting badges", e)
            emptyList()
        }
    }
    
    /**
     * Get user's review count for badge evaluation
     */
    suspend fun getUserReviewCount(userId: String): Int {
        return try {
            val snapshot = firestore.collectionGroup("reviews")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            snapshot.size()
        } catch (e: Exception) {
            Log.e(TAG, "Error counting reviews", e)
            0
        }
    }
    
    /**
     * Evaluate and award badges based on user activity.
     * This method is idempotent - it checks for existing badges before awarding.
     * 
     * @param userId The user to evaluate badges for
     * @return List of newly awarded badges
     */
    suspend fun evaluateAndAwardBadges(userId: String): List<Badge> {
        val newlyAwarded = mutableListOf<Badge>()
        
        try {
            // Get current badges
            val existingBadges = getUserBadges(userId)
            val existingTypes = existingBadges.map { it.type }.toSet()
            
            // Get review count
            val reviewCount = getUserReviewCount(userId)
            Log.d(TAG, "User $userId has $reviewCount reviews")
            
            // Determine badges to award
            val deservedBadges = BadgeType.getBadgesForReviewCount(reviewCount)
            val newBadges = deservedBadges.filter { it !in existingTypes }
            
            // Award new badges
            for (badgeType in newBadges) {
                val badge = Badge.fromType(badgeType)
                try {
                    // Double-check idempotency by querying for existing badge of this type
                    val existing = getBadgesCollection(userId)
                        .whereEqualTo("type", badgeType.name)
                        .limit(1)
                        .get()
                        .await()
                    
                    if (existing.isEmpty) {
                        getBadgesCollection(userId).add(badge.toMap()).await()
                        newlyAwarded.add(badge)
                        Log.d(TAG, "Awarded badge ${badgeType.name} to user $userId")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error awarding badge ${badgeType.name}", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error evaluating badges for user $userId", e)
        }
        
        return newlyAwarded
    }
    
    /**
     * Check and award badges for current user.
     * Call this after posting a review.
     */
    suspend fun checkAndAwardBadgesForCurrentUser(): List<Badge> {
        val userId = auth.currentUser?.uid ?: return emptyList()
        return evaluateAndAwardBadges(userId)
    }
}
