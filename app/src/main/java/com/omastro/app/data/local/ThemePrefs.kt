package com.omastro.app.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Persists the dark/light theme choice. Default = dark (existing look).
 * The in-memory StateFlow lets MainActivity recompose on toggle.
 */
object ThemePrefs {
    private const val PREFS = "omastro_theme_prefs"
    private const val KEY_DARK = "dark_theme"

    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            _isDarkTheme.value = prefs!!.getBoolean(KEY_DARK, true)
        }
    }

    fun setDarkTheme(dark: Boolean) {
        _isDarkTheme.value = dark
        prefs?.edit()?.putBoolean(KEY_DARK, dark)?.apply()
    }
}
