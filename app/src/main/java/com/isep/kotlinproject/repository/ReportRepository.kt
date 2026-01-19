package com.isep.kotlinproject.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.isep.kotlinproject.model.ReportReason
import com.isep.kotlinproject.model.ReportStatus
import com.isep.kotlinproject.model.ReviewReport
import kotlinx.coroutines.tasks.await

/**
 * Repository for review reporting operations.
 * 
 * IMPLEMENTATION NOTES:
 * - Reports are stored in a separate collection for moderation
 * - Duplicate reports by the same user are prevented client-side
 * - Without Cloud Functions, moderation is manual via Firebase Console
 */
class ReportRepository {
    
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val reportsCollection = firestore.collection("review_reports")
    
    companion object {
        private const val TAG = "ReportRepository"
    }
    
    /**
     * Check if user has already reported this review
     */
    suspend fun hasUserReportedReview(reviewId: String): Boolean {
        val userId = auth.currentUser?.uid ?: return false
        
        return try {
            val snapshot = reportsCollection
                .whereEqualTo("reviewId", reviewId)
                .whereEqualTo("reporterUserId", userId)
                .limit(1)
                .get()
                .await()
            
            !snapshot.isEmpty
        } catch (e: Exception) {
            Log.e(TAG, "Error checking existing report", e)
            false
        }
    }
    
    /**
     * Submit a review report
     * 
     * @return true if report was submitted successfully, false if already reported or error
     */
    suspend fun reportReview(
        reviewId: String,
        gameId: String,
        reportedUserId: String,
        reason: ReportReason,
        additionalInfo: String = ""
    ): Result<Boolean> {
        val reporterUserId = auth.currentUser?.uid 
            ?: return Result.failure(Exception("User not authenticated"))
        
        // Prevent self-reporting
        if (reporterUserId == reportedUserId) {
            return Result.failure(Exception("Cannot report your own review"))
        }
        
        // Check for duplicate report
        if (hasUserReportedReview(reviewId)) {
            return Result.failure(Exception("You have already reported this review"))
        }
        
        return try {
            val report = ReviewReport(
                reviewId = reviewId,
                gameId = gameId,
                reportedUserId = reportedUserId,
                reporterUserId = reporterUserId,
                reason = reason,
                additionalInfo = additionalInfo,
                status = ReportStatus.PENDING
            )
            
            reportsCollection.add(report.toMap()).await()
            Log.d(TAG, "Report submitted for review $reviewId")
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error submitting report", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get report count for a specific review (for potential auto-hiding)
     */
    suspend fun getReportCountForReview(reviewId: String): Int {
        return try {
            val snapshot = reportsCollection
                .whereEqualTo("reviewId", reviewId)
                .get()
                .await()
            snapshot.size()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting report count", e)
            0
        }
    }
}
