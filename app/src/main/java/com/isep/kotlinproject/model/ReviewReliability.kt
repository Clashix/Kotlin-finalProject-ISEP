package com.isep.kotlinproject.model

import com.google.firebase.Timestamp

/**
 * Data model representing a computed reliability score for a review's author.
 * 
 * Reliability scores help users assess the trustworthiness of reviews by
 * analyzing objective factors about the reviewer's account. Higher scores
 * indicate reviewers with established presence and engagement on the platform.
 * 
 * **Scoring Methodology:**
 * The score is computed as a weighted sum of three factors:
 * 1. **Account Age (40%):** Older accounts are less likely to be fake.
 * 2. **Review History (35%):** Active reviewers have demonstrated commitment.
 * 3. **Profile Completeness (25%):** Invested users complete their profiles.
 * 
 * **Implementation Limitations (Client-Side Computation):**
 * - Cannot verify actual review quality or accuracy.
 * - Relies on heuristics that sophisticated actors could game.
 * - Cannot access cross-device activity or behavioral patterns.
 * - For production systems, consider server-side computation with ML models.
 * 
 * Despite limitations, the score provides useful context for casual users
 * to calibrate their trust in reviews from unknown authors.
 * 
 * @property score Normalized reliability score from 0.0 (least reliable) to 1.0 (most reliable).
 * @property level The [ReliabilityLevel] category for UI badge display.
 * @property factors Breakdown of individual scoring components for transparency.
 * 
 * @see ReliabilityLevel for the tier definitions.
 * @see ReliabilityFactor for individual scoring component details.
 * @see ReliabilityBadge for the UI component displaying this data.
 */
data class ReviewReliability(
    val score: Float,
    val level: ReliabilityLevel,
    val factors: List<ReliabilityFactor>
) {
    companion object {
        /**
         * Computes a reliability score based on available user data.
         * 
         * This factory method analyzes objective account attributes to estimate
         * the likelihood that a reviewer is a genuine, engaged user rather than
         * a fake account or casual one-time visitor.
         * 
         * **Scoring Breakdown:**
         * - Account Age: 0.05 (new) to 0.4 (1+ year)
         * - Review Count: 0.0 (none) to 0.35 (25+ reviews)
         * - Profile Photo: +0.15 if present
         * - Bio: +0.1 if present
         * 
         * @param accountCreatedAt Timestamp when the user account was created.
         * @param reviewCount Total number of reviews posted by the user.
         * @param hasProfilePhoto Whether the user has uploaded a profile photo.
         * @param hasBio Whether the user has written a bio description.
         * @return A [ReviewReliability] instance with the computed score and factors.
         */
        fun calculate(
            accountCreatedAt: Timestamp?,
            reviewCount: Int,
            hasProfilePhoto: Boolean,
            hasBio: Boolean
        ): ReviewReliability {
            val factors = mutableListOf<ReliabilityFactor>()
            var totalScore = 0f
            var maxScore = 0f
            
            // ==================== FACTOR 1: ACCOUNT AGE ====================
            // Rationale: Older accounts require more effort to create and maintain,
            // making them less likely to be throwaway accounts for fake reviews.
            maxScore += 0.4f
            val accountAge = accountCreatedAt?.let {
                val ageMs = System.currentTimeMillis() - it.toDate().time
                val ageDays = ageMs / (1000 * 60 * 60 * 24)
                ageDays
            } ?: 0L
            
            val ageScore = when {
                accountAge >= 365 -> 0.4f  // Established user (1+ year)
                accountAge >= 180 -> 0.3f  // Regular user (6+ months)
                accountAge >= 30 -> 0.2f   // Recent user (1+ month)
                accountAge >= 7 -> 0.1f    // New user (1+ week)
                else -> 0.05f              // Very new account
            }
            totalScore += ageScore
            factors.add(ReliabilityFactor(
                name = "Account Age",
                description = when {
                    accountAge >= 365 -> "Account is over 1 year old"
                    accountAge >= 180 -> "Account is 6+ months old"
                    accountAge >= 30 -> "Account is 1+ month old"
                    else -> "New account"
                },
                score = ageScore,
                maxScore = 0.4f
            ))
            
            // ==================== FACTOR 2: REVIEW HISTORY ====================
            // Rationale: Users who have posted multiple reviews demonstrate ongoing
            // engagement and allow pattern analysis of their rating behavior.
            maxScore += 0.35f
            val reviewScore = when {
                reviewCount >= 25 -> 0.35f // Power reviewer
                reviewCount >= 10 -> 0.25f // Active reviewer
                reviewCount >= 5 -> 0.15f  // Engaged user
                reviewCount >= 1 -> 0.1f   // Has contributed
                else -> 0f                  // No history
            }
            totalScore += reviewScore
            factors.add(ReliabilityFactor(
                name = "Review History",
                description = "$reviewCount reviews posted",
                score = reviewScore,
                maxScore = 0.35f
            ))
            
            // ==================== FACTOR 3: PROFILE COMPLETENESS ====================
            // Rationale: Users who take time to complete their profile are more
            // invested in the platform and less likely to be fake accounts.
            maxScore += 0.25f
            var profileScore = 0f
            if (hasProfilePhoto) profileScore += 0.15f
            if (hasBio) profileScore += 0.1f
            totalScore += profileScore
            factors.add(ReliabilityFactor(
                name = "Profile Completeness",
                description = when {
                    hasProfilePhoto && hasBio -> "Complete profile"
                    hasProfilePhoto -> "Has profile photo"
                    hasBio -> "Has bio"
                    else -> "Incomplete profile"
                },
                score = profileScore,
                maxScore = 0.25f
            ))
            
            // Normalize to 0.0-1.0 range and determine tier
            val normalizedScore = (totalScore / maxScore).coerceIn(0f, 1f)
            
            val level = when {
                normalizedScore >= 0.75f -> ReliabilityLevel.HIGH
                normalizedScore >= 0.5f -> ReliabilityLevel.MEDIUM
                normalizedScore >= 0.25f -> ReliabilityLevel.LOW
                else -> ReliabilityLevel.UNVERIFIED
            }
            
            return ReviewReliability(
                score = normalizedScore,
                level = level,
                factors = factors
            )
        }
    }
}

/**
 * Data model representing a single component of the reliability score.
 * 
 * Factors provide transparency into the scoring algorithm, allowing users
 * to understand exactly why a reviewer received their reliability rating.
 * 
 * @property name Human-readable factor name (e.g., "Account Age").
 * @property description Contextual explanation of the user's status for this factor.
 * @property score Points earned for this factor.
 * @property maxScore Maximum possible points for this factor.
 */
data class ReliabilityFactor(
    val name: String,
    val description: String,
    val score: Float,
    val maxScore: Float
)

/**
 * Enumeration of reliability tier categories for display purposes.
 * 
 * Tiers provide quick visual identification of reviewer credibility:
 * - **HIGH (75%+):** Established users with significant platform history.
 * - **MEDIUM (50-74%):** Users with moderate engagement.
 * - **LOW (25-49%):** New or minimally engaged users.
 * - **UNVERIFIED (<25%):** Users with insufficient data for assessment.
 * 
 * @property displayName User-facing label for the reliability badge.
 * @property color ARGB color value for visual distinction.
 */
enum class ReliabilityLevel(val displayName: String, val color: Long) {
    /** Top tier: Trusted users with established platform presence. */
    HIGH("Trusted Reviewer", 0xFF4CAF50),
    
    /** Mid tier: Users with moderate engagement history. */
    MEDIUM("Established", 0xFF2196F3),
    
    /** Lower tier: New or casual users. */
    LOW("New Reviewer", 0xFFFFC107),
    
    /** Baseline: Insufficient data to assess reliability. */
    UNVERIFIED("Unverified", 0xFF9E9E9E)
}
