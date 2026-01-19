package com.isep.kotlinproject.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.isep.kotlinproject.model.ThemePreference
import com.isep.kotlinproject.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for app settings including theme preferences.
 * Uses AndroidViewModel to access application context for SharedPreferences.
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    
    private val settingsRepository = SettingsRepository(application.applicationContext)
    
    companion object {
        private const val TAG = "SettingsViewModel"
    }
    
    // Theme preference state
    private val _themePreference = MutableStateFlow(ThemePreference.SYSTEM)
    val themePreference: StateFlow<ThemePreference> = _themePreference.asStateFlow()
    
    // Dark mode computed from theme preference
    private val _isDarkMode = MutableStateFlow<Boolean?>(null)
    val isDarkMode: StateFlow<Boolean?> = _isDarkMode.asStateFlow()
    
    init {
        // Load local theme immediately
        _themePreference.value = settingsRepository.getLocalThemePreference()
        
        // Listen for Firestore updates
        viewModelScope.launch {
            settingsRepository.getThemePreferenceFlow().collect { theme ->
                _themePreference.value = theme
            }
        }
    }
    
    /**
     * Update theme preference
     */
    fun setThemePreference(theme: ThemePreference) {
        viewModelScope.launch {
            val success = settingsRepository.setThemePreference(theme)
            if (success) {
                _themePreference.value = theme
                Log.d(TAG, "Theme updated to ${theme.value}")
            }
        }
    }
    
    /**
     * Toggle between light and dark mode
     * If currently SYSTEM, switches to opposite of current system theme
     */
    fun toggleDarkMode(isCurrentlyDark: Boolean) {
        val newTheme = if (isCurrentlyDark) ThemePreference.LIGHT else ThemePreference.DARK
        setThemePreference(newTheme)
    }
    
    /**
     * Sync theme from Firestore (call on login)
     */
    fun syncThemeFromCloud() {
        viewModelScope.launch {
            settingsRepository.syncThemeFromFirestore()
        }
    }
    
    /**
     * Get whether dark mode should be used based on preference
     * Returns null for SYSTEM preference (let system decide)
     */
    fun shouldUseDarkMode(): Boolean? {
        return when (_themePreference.value) {
            ThemePreference.LIGHT -> false
            ThemePreference.DARK -> true
            ThemePreference.SYSTEM -> null
        }
    }
}
