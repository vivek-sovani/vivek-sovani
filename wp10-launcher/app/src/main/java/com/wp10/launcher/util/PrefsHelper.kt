package com.wp10.launcher.util

import android.content.Context
import android.graphics.Color

object PrefsHelper {

    private const val PREFS_NAME = "wp10_prefs"
    private const val KEY_ACCENT = "accent_color"
    private const val KEY_DARK_THEME = "dark_theme"
    private const val KEY_WALLPAPER_URI = "wallpaper_uri"

    // Default Windows Phone accent blue
    private const val DEFAULT_ACCENT = 0xFF0078D7.toInt()

    fun getAccentColor(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_ACCENT, DEFAULT_ACCENT)
    }

    fun setAccentColor(context: Context, color: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putInt(KEY_ACCENT, color).apply()
    }

    fun isDarkTheme(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_DARK_THEME, true)
    }

    fun setDarkTheme(context: Context, dark: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_DARK_THEME, dark).apply()
    }

    fun getWallpaperUri(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_WALLPAPER_URI, null)
    }

    fun setWallpaperUri(context: Context, uri: String?) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_WALLPAPER_URI, uri).apply()
    }

    val ACCENT_COLORS = listOf(
        0xFF0078D7.toInt(),  // Cobalt (default)
        0xFF00ABA9.toInt(),  // Teal
        0xFF6A00FF.toInt(),  // Violet
        0xFFAA00FF.toInt(),  // Purple
        0xFF76608A.toInt(),  // Mauve
        0xFF647687.toInt(),  // Steel
        0xFF0050EF.toInt(),  // Cobalt
        0xFF1BA1E2.toInt(),  // Cyan
        0xFF60A917.toInt(),  // Lime green
        0xFF008A00.toInt(),  // Green
        0xFFD80073.toInt(),  // Magenta
        0xFFA20025.toInt(),  // Crimson
        0xFFE51400.toInt(),  // Red
        0xFFFA6800.toInt(),  // Orange
        0xFFF0A30A.toInt(),  // Amber
        0xFFE3C800.toInt(),  // Yellow
        0xFF825A2C.toInt(),  // Brown
        0xFF6D8764.toInt(),  // Olive
        0xFF647687.toInt(),  // Steel
        0xFF76608A.toInt()   // Mauve
    )
}
