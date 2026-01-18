package com.isep.kotlinproject.repository

import android.util.Log
import com.isep.kotlinproject.api.SteamApiService
import com.isep.kotlinproject.api.SteamAppDetails
import com.isep.kotlinproject.api.SteamStoreItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Repository for Steam API operations with in-memory caching.
 */
class SteamRepository {
    
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://store.steampowered.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val api = retrofit.create(SteamApiService::class.java)
    
    // In-memory cache for game details
    private val gameDetailsCache = mutableMapOf<String, SteamAppDetails>()
    private val cacheMutex = Mutex()
    
    // Cache for search results (short-lived)
    private val searchCache = mutableMapOf<String, List<SteamStoreItem>>()
    
    companion object {
        private const val TAG = "SteamRepository"
        private const val CACHE_MAX_SIZE = 100
    }

    /**
     * Search Steam games by query
     */
    suspend fun searchGames(query: String) = api.searchGames(query)
    
    /**
     * Get game details by Steam App ID with caching
     */
    suspend fun getGameDetails(appId: String): SteamAppDetails? = withContext(Dispatchers.IO) {
        // Check cache first
        cacheMutex.withLock {
            gameDetailsCache[appId]?.let { return@withContext it }
        }
        
        return@withContext try {
            val response = api.getAppDetails(appId)
            val appData = response[appId]
            
            if (appData?.success == true && appData.data != null) {
                val details = appData.data
                
                // Add to cache (with size limit)
                cacheMutex.withLock {
                    if (gameDetailsCache.size >= CACHE_MAX_SIZE) {
                        // Remove oldest entries (simple FIFO)
                        val keysToRemove = gameDetailsCache.keys.take(10)
                        keysToRemove.forEach { gameDetailsCache.remove(it) }
                    }
                    gameDetailsCache[appId] = details
                }
                
                details
            } else {
                Log.w(TAG, "Failed to get details for app $appId")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching game details for $appId", e)
            null
        }
    }
    
    /**
     * Get multiple game details by Steam App IDs (batch with caching)
     */
    suspend fun getMultipleGameDetails(appIds: List<String>): Map<String, SteamAppDetails> = withContext(Dispatchers.IO) {
        val results = mutableMapOf<String, SteamAppDetails>()
        val uncachedIds = mutableListOf<String>()
        
        // Check cache first
        cacheMutex.withLock {
            appIds.forEach { appId ->
                gameDetailsCache[appId]?.let { 
                    results[appId] = it 
                } ?: uncachedIds.add(appId)
            }
        }
        
        // Fetch uncached items (with rate limiting to avoid Steam API issues)
        uncachedIds.forEach { appId ->
            try {
                getGameDetails(appId)?.let { details ->
                    results[appId] = details
                }
                // Small delay to avoid rate limiting
                kotlinx.coroutines.delay(100)
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching details for $appId", e)
            }
        }
        
        results
    }
    
    /**
     * Clear the cache
     */
    suspend fun clearCache() {
        cacheMutex.withLock {
            gameDetailsCache.clear()
            searchCache.clear()
        }
    }
    
    /**
     * Get cached game details if available (without network call)
     */
    suspend fun getCachedGameDetails(appId: String): SteamAppDetails? {
        return cacheMutex.withLock {
            gameDetailsCache[appId]
        }
    }
}
