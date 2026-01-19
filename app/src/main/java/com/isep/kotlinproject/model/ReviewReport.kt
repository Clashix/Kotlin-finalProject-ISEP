package com.isep.kotlinproject.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

/**
 * Data model representing a user-submitted report for an inappropriate review.
 * 
 * The review reporting system enables community moderation by allowing users
 * to flag reviews that violate platform guidelines. Reports are stored in a
 * dedicated collection for administrative review.
 * 
 * **Storage Location:** `review_reports/{reportId}`
 * 
 * **Moderation Workflow:**
 * Without Cloud Functions, moderation is a manual process:
 * 1. Users submit reports through the app UI.
 * 2. Administrators review reports in Firebase Console.
 * 3. Status is updated and appropriate action is taken.
 * 
 * **Duplicate Prevention:**
 * The repository layer prevents users from submitting multiple reports
 * for the same review, avoiding spam and ensuring fair representation.
 * 
 * @property id Firestore document ID for this report.
 * @property reviewId Reference to the reported review document.
 * @property gameId Reference to the game containing the review.
 * @property reportedUserId User ID of the review author being reported.
 * @property reporterUserId User ID of the person filing the report.
 * @property reason The [ReportReason] categorizing the complaint.
 * @property additionalInfo Optional free-text explanation from the reporter.
 * @property status Current [ReportStatus] in the moderation workflow.
 * @property createdAt Server timestamp when the report was submitted.
 * 
 * @see ReportReason for available report categories.
 * @see ReportStatus for moderation workflow states.
 * @see ReportRepository for report submission and query logic.
 */
data class ReviewReport(
    val id: String = "",
    val reviewId: String = "",
    val gameId: String = "",
    val reportedUserId: String = "",
    val reporterUserId: String = "",
    val reason: ReportReason = ReportReason.SPAM,
    val additionalInfo: String = "",
    val status: ReportStatus = ReportStatus.PENDING,
    
    @ServerTimestamp
    val createdAt: Timestamp? = null
) {
    /**
     * Converts this ReviewReport to a Map for Firestore document writes.
     * 
     * @return Map representation with the createdAt timestamp set to current time.
     */
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "reviewId" to reviewId,
            "gameId" to gameId,
            "reportedUserId" to reportedUserId,
            "reporterUserId" to reporterUserId,
            "reason" to reason.name,
            "additionalInfo" to additionalInfo,
            "status" to status.name,
            "createdAt" to Timestamp.now()
        )
    }
}

/**
 * Enumeration of valid reasons for reporting a review.
 * 
 * These categories help moderators quickly triage reports and apply
 * consistent moderation policies. Each reason maps to specific guideline
 * violations that warrant review.
 * 
 * @property displayName User-friendly label shown in the report dialog.
 */
enum class ReportReason(val displayName: String) {
    /** Review contains unsolicited promotional content or advertisements. */
    SPAM("Spam"),
    
    /** Review contains hate speech, harassment, or explicit content. */
    OFFENSIVE("Offensive Content"),
    
    /** Review appears to be inauthentic or manipulated (e.g., paid review). */
    FAKE_REVIEW("Fake Review"),
    
    /** Review reveals major plot points without appropriate warnings. */
    SPOILERS("Contains Spoilers"),
    
    /** Report reason doesn't fit other categories; see additionalInfo. */
    OTHER("Other")
}

/**
 * Enumeration of report moderation workflow states.
 * 
 * Reports progress through these states as moderators review them:
 * 1. **PENDING:** Newly submitted, awaiting moderator review.
 * 2. **REVIEWED:** Moderator has examined the report.
 * 3. **DISMISSED:** Report determined to be invalid or unfounded.
 * 4. **ACTION_TAKEN:** Moderation action applied (e.g., review removed).
 */
enum class ReportStatus {
    /** Report is awaiting moderator review. */
    PENDING,
    
    /** Report has been reviewed by a moderator. */
    REVIEWED,
    
    /** Report was determined to be invalid or unfounded. */
    DISMISSED,
    
    /** Moderation action was taken against the reported review. */
    ACTION_TAKEN
}
