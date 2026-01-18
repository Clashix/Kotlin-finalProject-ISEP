package com.isep.kotlinproject.api

import com.google.gson.annotations.SerializedName

data class SteamStoreSearchResponse(
    @SerializedName("total") val total: Int,
    @SerializedName("items") val items: List<SteamStoreItem>
)

data class SteamStoreItem(
    @SerializedName("id") val id: Int,
    @SerializedName("type") val type: String, // "app", "mod", "bundle"
    @SerializedName("name") val name: String,
    @SerializedName("tiny_image") val tinyImage: String, // URL
    @SerializedName("metascore") val metascore: String? // sometimes null or empty
)

/**
 * Response wrapper for app details API
 */
data class SteamAppDetailsResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: SteamAppDetails?
)

/**
 * Detailed Steam app information
 */
data class SteamAppDetails(
    @SerializedName("steam_appid") val steamAppId: Int,
    @SerializedName("name") val name: String,
    @SerializedName("type") val type: String,
    @SerializedName("is_free") val isFree: Boolean = false,
    @SerializedName("detailed_description") val detailedDescription: String? = null,
    @SerializedName("about_the_game") val aboutTheGame: String? = null,
    @SerializedName("short_description") val shortDescription: String? = null,
    @SerializedName("header_image") val headerImage: String? = null,
    @SerializedName("capsule_image") val capsuleImage: String? = null,
    @SerializedName("capsule_imagev5") val capsuleImageV5: String? = null,
    @SerializedName("website") val website: String? = null,
    @SerializedName("developers") val developers: List<String>? = null,
    @SerializedName("publishers") val publishers: List<String>? = null,
    @SerializedName("price_overview") val priceOverview: SteamPriceOverview? = null,
    @SerializedName("platforms") val platforms: SteamPlatforms? = null,
    @SerializedName("metacritic") val metacritic: SteamMetacritic? = null,
    @SerializedName("categories") val categories: List<SteamCategory>? = null,
    @SerializedName("genres") val genres: List<SteamGenre>? = null,
    @SerializedName("screenshots") val screenshots: List<SteamScreenshot>? = null,
    @SerializedName("release_date") val releaseDate: SteamReleaseDate? = null
) {
    /**
     * Get the best available image URL
     */
    fun getBestImageUrl(): String {
        return headerImage 
            ?: capsuleImage 
            ?: capsuleImageV5 
            ?: "https://cdn.akamai.steamstatic.com/steam/apps/$steamAppId/header.jpg"
    }
    
    /**
     * Get developer name(s) as a single string
     */
    fun getDeveloperString(): String {
        return developers?.joinToString(", ") ?: "Unknown"
    }
    
    /**
     * Get genres as a single string
     */
    fun getGenreString(): String {
        return genres?.joinToString(", ") { it.description } ?: "Unknown"
    }
    
    /**
     * Get price as formatted string
     */
    fun getPriceString(): String {
        return when {
            isFree -> "Free"
            priceOverview != null -> priceOverview.finalFormatted
            else -> "N/A"
        }
    }
}

data class SteamPriceOverview(
    @SerializedName("currency") val currency: String,
    @SerializedName("initial") val initial: Int,
    @SerializedName("final") val final: Int,
    @SerializedName("discount_percent") val discountPercent: Int,
    @SerializedName("initial_formatted") val initialFormatted: String,
    @SerializedName("final_formatted") val finalFormatted: String
)

data class SteamPlatforms(
    @SerializedName("windows") val windows: Boolean = false,
    @SerializedName("mac") val mac: Boolean = false,
    @SerializedName("linux") val linux: Boolean = false
)

data class SteamMetacritic(
    @SerializedName("score") val score: Int,
    @SerializedName("url") val url: String?
)

data class SteamCategory(
    @SerializedName("id") val id: Int,
    @SerializedName("description") val description: String
)

data class SteamGenre(
    @SerializedName("id") val id: String,
    @SerializedName("description") val description: String
)

data class SteamScreenshot(
    @SerializedName("id") val id: Int,
    @SerializedName("path_thumbnail") val pathThumbnail: String,
    @SerializedName("path_full") val pathFull: String
)

data class SteamReleaseDate(
    @SerializedName("coming_soon") val comingSoon: Boolean,
    @SerializedName("date") val date: String?
)