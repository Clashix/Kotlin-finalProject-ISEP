package com.isep.kotlinproject.model

/**
 * Theme preference enumeration for Dark/Light mode support.
 * SYSTEM follows the device's current theme setting.
 */
enum class ThemePreference(val value: String) {
    LIGHT("light"),
    DARK("dark"),
    SYSTEM("system");
    
    companion object {
        fun fromString(value: String?): ThemePreference {
            return when (value?.lowercase()) {
                "light" -> LIGHT
                "dark" -> DARK
                else -> SYSTEM
            }
        }
    }
}
