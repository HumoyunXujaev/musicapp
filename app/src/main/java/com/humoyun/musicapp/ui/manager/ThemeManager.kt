package com.humoyun.musicapp.ui.manager

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import com.humoyun.musicapp.R

enum class AppTheme(val themeId: Int, val title: String) {
    BLUE(R.style.Theme_MusicApp_Blue, "Humo Blue"),
    RED(R.style.Theme_MusicApp_Red, "Crimson Red"),
    GREEN(R.style.Theme_MusicApp_Green, "Forest Green"),
    PURPLE(R.style.Theme_MusicApp_Purple, "Deep Purple"),
    AMOLED(R.style.Theme_MusicApp_Amoled, "AMOLED Black");

    companion object {
        fun fromId(id: Int): AppTheme = entries.find { it.themeId == id } ?: BLUE
    }
}

class ThemeManager(private val context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("app_theme_prefs", Context.MODE_PRIVATE)

    fun applyTheme(activity: Activity) {
        val themeId = prefs.getInt("current_theme_id", AppTheme.BLUE.themeId)
        activity.setTheme(themeId)
    }

    fun setTheme(theme: AppTheme, activity: Activity) {
        if (getCurrentTheme() == theme) return

        prefs.edit().putInt("current_theme_id", theme.themeId).apply()

        activity.recreate()
    }

    fun getCurrentTheme(): AppTheme {
        val id = prefs.getInt("current_theme_id", AppTheme.BLUE.themeId)
        return AppTheme.fromId(id)
    }
}