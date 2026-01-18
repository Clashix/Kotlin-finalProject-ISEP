package com.isep.kotlinproject.util

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import java.util.Locale

/**
 * Utility class for managing app locale/language settings.
 * Supports FR and EN with fallback mechanism.
 */
object LocaleManager {
    
    private const val PREFS_NAME = "locale_prefs"
    private const val KEY_LOCALE = "app_locale"
    
    // Supported locales
    val SUPPORTED_LOCALES = listOf("en", "fr")
    const val DEFAULT_LOCALE = "en"
    
    /**
     * Get the current saved locale code
     */
    fun getSavedLocale(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LOCALE, null) ?: getSystemLocale(context)
    }
    
    /**
     * Get the system locale, falling back to default if not supported
     */
    private fun getSystemLocale(context: Context): String {
        val systemLocale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales[0].language
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale.language
        }
        
        return if (systemLocale in SUPPORTED_LOCALES) systemLocale else DEFAULT_LOCALE
    }
    
    /**
     * Save locale preference
     */
    fun saveLocale(context: Context, localeCode: String) {
        val validLocale = if (localeCode in SUPPORTED_LOCALES) localeCode else DEFAULT_LOCALE
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LOCALE, validLocale)
            .apply()
    }
    
    /**
     * Apply the saved locale to a context
     * Call this in attachBaseContext of activities
     */
    fun applyLocale(context: Context): Context {
        val localeCode = getSavedLocale(context)
        return updateContextLocale(context, localeCode)
    }
    
    /**
     * Update context with a specific locale
     */
    fun updateContextLocale(context: Context, localeCode: String): Context {
        val locale = Locale(localeCode)
        Locale.setDefault(locale)
        
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale)
        
        return context.createConfigurationContext(configuration)
    }
    
    /**
     * Get display name for a locale code
     */
    fun getLocaleDisplayName(localeCode: String, inLocale: String = localeCode): String {
        val locale = Locale(localeCode)
        val displayLocale = Locale(inLocale)
        return locale.getDisplayLanguage(displayLocale).replaceFirstChar { 
            it.uppercase(displayLocale) 
        }
    }
    
    /**
     * Get all supported locales with their display names
     */
    fun getSupportedLocalesWithNames(currentLocale: String = DEFAULT_LOCALE): List<Pair<String, String>> {
        return SUPPORTED_LOCALES.map { code ->
            code to getLocaleDisplayName(code, currentLocale)
        }
    }
    
    /**
     * Check if locale is supported
     */
    fun isSupported(localeCode: String): Boolean = localeCode in SUPPORTED_LOCALES
    
    /**
     * Get fallback locale if given locale is not supported
     */
    fun getWithFallback(localeCode: String): String {
        return if (isSupported(localeCode)) localeCode else DEFAULT_LOCALE
    }
}

/**
 * Extension function to easily apply locale to Context
 */
fun Context.withLocale(): Context = LocaleManager.applyLocale(this)
