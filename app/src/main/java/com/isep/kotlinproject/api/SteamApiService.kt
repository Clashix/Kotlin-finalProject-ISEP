package com.isep.kotlinproject.api

import retrofit2.http.GET
import retrofit2.http.Query

interface SteamApiService {
    @GET("api/storesearch")
    suspend fun searchGames(
        @Query("term") term: String,
        @Query("l") language: String = "english",
        @Query("cc") countryCode: String = "US"
    ): SteamStoreSearchResponse
    
    /**
     * Get app details by Steam App ID
     * Returns a map with appId as key
     */
    @GET("api/appdetails")
    suspend fun getAppDetails(
        @Query("appids") appId: String,
        @Query("l") language: String = "english",
        @Query("cc") countryCode: String = "US"
    ): Map<String, SteamAppDetailsResponse>
}
