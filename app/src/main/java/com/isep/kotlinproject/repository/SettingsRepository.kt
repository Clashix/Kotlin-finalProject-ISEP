package com.isep.kotlinproject.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.isep.kotlinproject.model.ThemePreference
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repository for user settings including theme preferences.
 * Settings are synced to Firestore for cross-device consistency.
 */
class SettingsRepository(private val context: Context) {
    
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "app_settings", 
        Context.MODE_PRIVATE
    )
    
    companion object {
        private const val TAG = "SettingsRepository"
        private const val KEY_THEME = "theme_preference"
    }
    
    /**
     * Get theme preference from local storage (immediate, no network)
     */
    fun getLocalThemePreference(): ThemePreference {
        val value = prefs.getString(KEY_THEME, null)
        return ThemePreference.fromString(value)
    }
    
    /**
     * Save theme preference locally
     */
    private fun saveLocalThemePreference(theme: ThemePreference) {
        prefs.edit().putString(KEY_THEME, theme.value).apply()
    }
    
    /**
     * Get theme preference as Flow (synced with Firestore)
     */
    fun getThemePreferenceFlow(): Flow<ThemePreference> = callbackFlow {
        // Emit local value first for immediate UI
        trySend(getLocalThemePreference())
        
        val userId = auth.currentUser?.uid
        if (userId == null) {
            close()
            return@callbackFlow
        }
        
        // Listen to Firestore for cross-device sync
        val subscription = firestore.collection("users")
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to theme preference", error)
                    return@addSnapshotListener
                }
                
                val themeValue = snapshot?.getString("themePreference")
                val theme = ThemePreference.fromString(themeValue)
                
                // Update local cache
                saveLocalThemePreference(theme)
                
                trySend(theme)
            }
        
        awaitClose { subscription.remove() }
    }
    
    /**
     * Update theme preference (saves locally and syncs to Firestore)
     */
    suspend fun setThemePreference(theme: ThemePreference): Boolean {
        // Save locally first for immediate effect
        saveLocalThemePreference(theme)
        
        val userId = auth.currentUser?.uid ?: return true // Local-only if not logged in
        
        return try {
            firestore.collection("users")
                .document(userId)
                .update("themePreference", theme.value)
                .await()
            Log.d(TAG, "Theme preference synced to Firestore: ${theme.value}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing theme preference to Firestore", e)
            // Local save succeeded, so return true
            true
        }
    }
    
    /**
     * Sync theme from Firestore (call on app start or login)
     */
    suspend fun syncThemeFromFirestore() {
        val userId = auth.currentUser?.uid ?: return
        
        try {
            val snapshot = firestore.collection("users")
                .document(userId)
                .get()
                .await()
            
            val themeValue = snapshot.getString("themePreference")
            if (themeValue != null) {
                val theme = ThemePreference.fromString(themeValue)
                saveLocalThemePreference(theme)
                Log.d(TAG, "Theme synced from Firestore: ${theme.value}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing theme from Firestore", e)
        }
    }
}
