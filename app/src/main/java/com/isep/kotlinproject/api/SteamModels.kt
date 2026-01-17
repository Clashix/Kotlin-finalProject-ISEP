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
