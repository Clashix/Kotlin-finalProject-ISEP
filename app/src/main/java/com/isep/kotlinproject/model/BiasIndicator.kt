package com.isep.kotlinproject.model

/**
 * Data model representing a detected bias indicator for reviews or games.
 * 
 * Bias indicators are informational warnings displayed to users to help them
 * interpret ratings with appropriate context. They identify patterns that may
 * suggest the ratings are not fully representative of overall quality.
 * 
 * **Design Philosophy:**
 * - Indicators are purely informational and never block content.
 * - Users can click indicators to see detailed explanations.
 * - The system encourages critical thinking rather than censorship.
 * 
 * **Implementation Limitations (Client-Side Detection):**
 * Detection relies on simple statistical heuristics that can be computed
 * client-side without server infrastructure:
 * - Cannot detect coordinated review campaigns (astroturfing).
 * - Cannot identify purchased or incentivized reviews.
 * - Cannot analyze review text for sentiment manipulation.
 * 
 * For production environments requiring robust fraud detection, consider
 * implementing machine learning models via Cloud Functions.
 * 
 * @property type The [BiasType] category of the detected pattern.
 * @property severity The [BiasSeverity] level for UI color coding.
 * @property message Short label displayed in the bias chip.
 * @property details Expanded explanation shown in the details dialog.
 * 
 * @see BiasType for the categories of detectable patterns.
 * @see BiasSeverity for the visual severity levels.
 * @see BiasDetector for the detection algorithms.
 */
data class BiasIndicator(
    val type: BiasType,
    val severity: BiasSeverity,
    val message: String,
    val details: String
)

/**
 * Enumeration of bias patterns that can be detected on the platform.
 * 
 * Each type represents a specific pattern that may indicate the ratings
 * should be interpreted with additional context.
 * 
 * @property displayName User-friendly category name for the details dialog.
 */
enum class BiasType(val displayName: String) {
    /** User exhibits extreme rating patterns (all 5s or all 1s). */
    RATING_EXTREMISM("Rating Pattern"),
    
    /** Game has too few reviews for statistically reliable ratings. */
    LOW_SAMPLE_SIZE("Limited Reviews"),
    
    /** This is the reviewer's first contribution to the platform. */
    FIRST_REVIEW("New Reviewer"),
    
    /** User's rating history suggests unusually positive tendencies. */
    POSSIBLE_FAN("Possible Fan"),
    
    /** User's rating history suggests unusually negative tendencies. */
    POSSIBLE_CRITIC("Possible Critic")
}

/**
 * Severity levels for bias indicators, determining UI presentation.
 * 
 * Colors follow Material Design guidelines for conveying importance:
 * - Blue indicates neutral information.
 * - Yellow/Amber suggests caution is warranted.
 * - Orange indicates significant concern.
 * 
 * @property color ARGB color value for the indicator chip and icon.
 */
enum class BiasSeverity(val color: Long) {
    /** Informational indicator; no action required. */
    INFO(0xFF2196F3),
    
    /** Caution indicator; user should consider the context. */
    WARNING(0xFFFFC107),
    
    /** Significant concern; pattern strongly suggests bias. */
    HIGH(0xFFFF9800)
}

/**
 * Utility object containing bias detection algorithms.
 * 
 * This object provides static methods for analyzing reviews and identifying
 * patterns that may indicate bias. Detection runs entirely client-side using
 * statistical analysis of available data.
 * 
 * **Algorithm Design Principles:**
 * - Err on the side of caution (avoid false positives).
 * - Require sufficient sample sizes before flagging patterns.
 * - Provide actionable information, not accusations.
 */
object BiasDetector {
    
    /**
     * Analyzes a user's review history to detect potential rating bias.
     * 
     * This method examines patterns in a user's past reviews to identify
     * behaviors that may indicate their ratings are not objective:
     * - **First Review:** New users have no established credibility.
     * - **Rating Extremism:** Users who always give the same rating.
     * - **Consistent High/Low:** Users who rate significantly above/below average.
     * 
     * Pattern detection requires a minimum of 5 reviews to avoid flagging
     * users with limited but legitimate rating variety.
     * 
     * @param userReviews Complete list of reviews authored by this user.
     * @param currentReview The specific review being analyzed (for context).
     * @return List of detected bias indicators, empty if no patterns found.
     */
    fun detectUserBias(
        userReviews: List<Review>,
        currentReview: Review
    ): List<BiasIndicator> {
        val indicators = mutableListOf<BiasIndicator>()
        
        // Flag first-time reviewers for transparency
        if (userReviews.size == 1) {
            indicators.add(BiasIndicator(
                type = BiasType.FIRST_REVIEW,
                severity = BiasSeverity.INFO,
                message = "First review",
                details = "This is the reviewer's first review on the platform"
            ))
        }
        
        // Require minimum sample size for pattern detection
        if (userReviews.size >= 5) {
            val ratings = userReviews.map { it.rating }
            val avgRating = ratings.average()
            
            // Detect consistently high ratings (possible fan behavior)
            if (ratings.all { it == 5 } || avgRating >= 4.9) {
                indicators.add(BiasIndicator(
                    type = BiasType.POSSIBLE_FAN,
                    severity = BiasSeverity.WARNING,
                    message = "High rater",
                    details = "This user rates games very positively on average (${String.format("%.1f", avgRating)}/5)"
                ))
            }
            
            // Detect consistently low ratings (possible critic behavior)
            if (ratings.all { it <= 2 } || avgRating <= 2.0) {
                indicators.add(BiasIndicator(
                    type = BiasType.POSSIBLE_CRITIC,
                    severity = BiasSeverity.WARNING,
                    message = "Low rater",
                    details = "This user rates games negatively on average (${String.format("%.1f", avgRating)}/5)"
                ))
            }
            
            // Detect zero variance (always same rating - highly suspicious)
            if (ratings.distinct().size == 1) {
                indicators.add(BiasIndicator(
                    type = BiasType.RATING_EXTREMISM,
                    severity = BiasSeverity.HIGH,
                    message = "No variation",
                    details = "This user always gives ${ratings.first()} stars"
                ))
            }
        }
        
        return indicators
    }
    
    /**
     * Analyzes a game's review collection for statistical reliability concerns.
     * 
     * This method examines the aggregate review data for a game to identify
     * factors that may affect how representative the ratings are:
     * - **Low Sample Size:** Too few reviews for statistical significance.
     * - **Rating Clustering:** Suspiciously uniform ratings suggesting manipulation.
     * 
     * Rating clustering detection requires at least 5 reviews to avoid
     * false positives from legitimately polarizing games.
     * 
     * @param gameReviews Complete list of reviews for the game.
     * @param averageRating The game's computed average rating.
     * @return List of detected bias indicators, empty if no concerns found.
     */
    fun detectGameBias(
        gameReviews: List<Review>,
        averageRating: Double
    ): List<BiasIndicator> {
        val indicators = mutableListOf<BiasIndicator>()
        
        // Assess sample size adequacy
        when {
            gameReviews.isEmpty() -> {
                indicators.add(BiasIndicator(
                    type = BiasType.LOW_SAMPLE_SIZE,
                    severity = BiasSeverity.INFO,
                    message = "No reviews yet",
                    details = "Be the first to review this game"
                ))
            }
            gameReviews.size < 3 -> {
                indicators.add(BiasIndicator(
                    type = BiasType.LOW_SAMPLE_SIZE,
                    severity = BiasSeverity.WARNING,
                    message = "Limited reviews",
                    details = "Only ${gameReviews.size} review(s). Rating may not be representative."
                ))
            }
            gameReviews.size < 10 -> {
                indicators.add(BiasIndicator(
                    type = BiasType.LOW_SAMPLE_SIZE,
                    severity = BiasSeverity.INFO,
                    message = "Few reviews",
                    details = "Based on ${gameReviews.size} reviews"
                ))
            }
        }
        
        // Detect suspicious rating clustering (potential manipulation)
        if (gameReviews.size >= 5) {
            val ratings = gameReviews.map { it.rating }
            val fiveStarRatio = ratings.count { it == 5 }.toFloat() / ratings.size
            val oneStarRatio = ratings.count { it == 1 }.toFloat() / ratings.size
            
            // Flag games where 90%+ of reviews are 5 stars
            if (fiveStarRatio >= 0.9f && averageRating >= 4.9) {
                indicators.add(BiasIndicator(
                    type = BiasType.RATING_EXTREMISM,
                    severity = BiasSeverity.WARNING,
                    message = "Suspiciously high",
                    details = "90%+ of reviews are 5 stars"
                ))
            }
            
            // Flag games where 90%+ of reviews are 1 star
            if (oneStarRatio >= 0.9f && averageRating <= 1.5) {
                indicators.add(BiasIndicator(
                    type = BiasType.RATING_EXTREMISM,
                    severity = BiasSeverity.WARNING,
                    message = "Suspiciously low",
                    details = "90%+ of reviews are 1 star"
                ))
            }
        }
        
        return indicators
    }
}
