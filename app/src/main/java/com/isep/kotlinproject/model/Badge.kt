package com.isep.kotlinproject.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

/**
 * Data model representing a user achievement badge.
 * 
 * Badges are gamification elements awarded to users based on their activity
 * on the platform, primarily tracking review milestones. They appear on user
 * profiles and serve as visual indicators of engagement and credibility.
 * 
 * **Storage Location:** `users/{uid}/badges/{badgeId}`
 * 
 * **Implementation Note (Client-Side Computation):**
 * Badge eligibility is evaluated client-side when users interact with the app.
 * This approach has trade-offs:
 * - **Advantage:** No Cloud Functions required, simpler deployment.
 * - **Limitation:** Potential race conditions if multiple devices sync simultaneously.
 * - **Limitation:** Theoretically susceptible to client manipulation.
 * 
 * For production environments with strict integrity requirements, consider
 * migrating badge computation to Firebase Cloud Functions with Firestore triggers.
 * 
 * @property id Firestore document ID for this badge instance.
 * @property type The [BadgeType] defining the badge's category and requirements.
 * @property name Human-readable badge name for display.
 * @property description Explanation of how the badge was earned.
 * @property iconName Material Icons identifier for visual representation.
 * @property earnedAt Server timestamp when the badge was awarded.
 * 
 * @see BadgeType for the complete list of available badges.
 * @see BadgeRepository for badge evaluation and storage logic.
 */
data class Badge(
    val id: String = "",
    val type: BadgeType = BadgeType.FIRST_REVIEW,
    val name: String = "",
    val description: String = "",
    val iconName: String = "",
    
    @ServerTimestamp
    val earnedAt: Timestamp? = null
) {
    /**
     * Converts this Badge to a Map for Firestore document writes.
     * 
     * @return Map representation with the earnedAt timestamp set to the current time.
     */
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "type" to type.name,
            "name" to name,
            "description" to description,
            "iconName" to iconName,
            "earnedAt" to Timestamp.now()
        )
    }
    
    companion object {
        /**
         * Factory method to create a Badge instance from a BadgeType.
         * 
         * This populates the badge's display properties from the type's
         * predefined values, ensuring consistency across the application.
         * 
         * @param type The badge type to instantiate.
         * @return A Badge instance with properties derived from the type.
         */
        fun fromType(type: BadgeType): Badge {
            return Badge(
                type = type,
                name = type.displayName,
                description = type.description,
                iconName = type.iconName
            )
        }
    }
}

/**
 * Enumeration of all achievement badges available on the platform.
 * 
 * Each badge type defines:
 * - **displayName:** User-facing title shown in the UI.
 * - **description:** Explanation of the achievement criteria.
 * - **iconName:** Material Icons identifier for visual display.
 * - **requirement:** Numeric threshold to earn the badge.
 * 
 * Badge tiers are designed to encourage continued engagement:
 * - Entry-level badges (1 review) provide immediate gratification.
 * - Higher tiers (25, 50 reviews) represent significant commitment.
 * 
 * @property displayName The badge title shown to users.
 * @property description Explanation of how to earn the badge.
 * @property iconName Material Icons name for the badge icon.
 * @property requirement Number of reviews required to unlock.
 */
enum class BadgeType(
    val displayName: String,
    val description: String,
    val iconName: String,
    val requirement: Int
) {
    /** Awarded for posting the first review. Entry-level achievement. */
    FIRST_REVIEW(
        displayName = "First Review",
        description = "Posted your first review",
        iconName = "star",
        requirement = 1
    ),
    
    /** Awarded for posting 5 reviews. Shows initial engagement. */
    FIVE_REVIEWS(
        displayName = "Reviewer",
        description = "Posted 5 reviews",
        iconName = "stars",
        requirement = 5
    ),
    
    /** Awarded for posting 10 reviews. Indicates active participation. */
    TEN_REVIEWS(
        displayName = "Critic",
        description = "Posted 10 reviews",
        iconName = "verified",
        requirement = 10
    ),
    
    /** Awarded for posting 25 reviews. Represents dedicated contribution. */
    TWENTY_FIVE_REVIEWS(
        displayName = "Expert Critic",
        description = "Posted 25 reviews",
        iconName = "workspace_premium",
        requirement = 25
    ),
    
    /** Awarded for posting 50 reviews. Highest tier of reviewer dedication. */
    FIFTY_REVIEWS(
        displayName = "Master Critic",
        description = "Posted 50 reviews",
        iconName = "military_tech",
        requirement = 50
    );
    
    companion object {
        /**
         * Determines which badges a user has earned based on their review count.
         * 
         * Returns all badges where the requirement is met or exceeded,
         * allowing batch processing of badge awards.
         * 
         * @param count The user's total number of posted reviews.
         * @return List of BadgeTypes the user has qualified for.
         */
        fun getBadgesForReviewCount(count: Int): List<BadgeType> {
            return values().filter { count >= it.requirement }
        }
    }
}
