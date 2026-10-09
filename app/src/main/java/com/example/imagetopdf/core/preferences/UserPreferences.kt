package com.example.imagetopdf.core.preferences

import android.content.Context
import android.content.SharedPreferences
import com.example.imagetopdf.MainActivity
import com.example.imagetopdf.core.security.SecurePreferences

enum class ThemeMode(val label: String) {
    SYSTEM("System default"),
    LIGHT("Light"),
    DARK("Dark")
}

enum class DefaultPdfQuality(val label: String) {
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High")
}

object UserPreferences {

    private const val KEY_USER_NAME = "user_name"
    private const val KEY_USER_EMAIL = "user_email"
    private const val KEY_THEME = "theme_mode"
    private const val KEY_DYNAMIC_COLOR = "dynamic_color"
    private const val KEY_NOTIFICATIONS = "notifications_enabled"
    private const val KEY_NOTIFY_DONE = "notify_conversion_done"
    private const val KEY_DEFAULT_QUALITY = "default_pdf_quality"
    private const val KEY_SAVE_DOWNLOADS = "save_to_downloads"
    const val KEY_LOCAL_PASSWORD = "local_password"

    fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)

    fun getDisplayName(context: Context): String =
        prefs(context).getString(KEY_USER_NAME, null)?.takeIf { it.isNotBlank() } ?: "PDF User"

    fun getEmail(context: Context): String =
        prefs(context).getString(KEY_USER_EMAIL, null)?.takeIf { it.isNotBlank() } ?: ""

    fun setProfile(context: Context, name: String, email: String) {
        prefs(context).edit()
            .putString(KEY_USER_NAME, name.trim())
            .putString(KEY_USER_EMAIL, email.trim())
            .apply()
    }

    fun setEmailFromLogin(context: Context, email: String) {
        prefs(context).edit().putString(KEY_USER_EMAIL, email.trim()).apply()
        if (prefs(context).getString(KEY_USER_NAME, null).isNullOrBlank()) {
            val guess = email.substringBefore("@").replace(".", " ").replaceFirstChar { it.uppercase() }
            prefs(context).edit().putString(KEY_USER_NAME, guess).apply()
        }
    }

    fun getThemeMode(context: Context): ThemeMode {
        return when (prefs(context).getString(KEY_THEME, ThemeMode.SYSTEM.name)) {
            ThemeMode.LIGHT.name -> ThemeMode.LIGHT
            ThemeMode.DARK.name -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
    }

    fun setThemeMode(context: Context, mode: ThemeMode) {
        prefs(context).edit().putString(KEY_THEME, mode.name).apply()
    }

    fun getDynamicColor(context: Context): Boolean =
        prefs(context).getBoolean(KEY_DYNAMIC_COLOR, true)

    fun setDynamicColor(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_DYNAMIC_COLOR, enabled).apply()
    }

    fun notificationsEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_NOTIFICATIONS, true)

    fun setNotificationsEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_NOTIFICATIONS, enabled).apply()
    }

    fun notifyOnConversionDone(context: Context): Boolean =
        prefs(context).getBoolean(KEY_NOTIFY_DONE, true)

    fun setNotifyOnConversionDone(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_NOTIFY_DONE, enabled).apply()
    }

    fun getDefaultQuality(context: Context): DefaultPdfQuality {
        return when (prefs(context).getString(KEY_DEFAULT_QUALITY, DefaultPdfQuality.MEDIUM.name)) {
            DefaultPdfQuality.LOW.name -> DefaultPdfQuality.LOW
            DefaultPdfQuality.HIGH.name -> DefaultPdfQuality.HIGH
            else -> DefaultPdfQuality.MEDIUM
        }
    }

    fun setDefaultQuality(context: Context, quality: DefaultPdfQuality) {
        prefs(context).edit().putString(KEY_DEFAULT_QUALITY, quality.name).apply()
    }

    fun saveToDownloads(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SAVE_DOWNLOADS, true)

    fun setSaveToDownloads(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_SAVE_DOWNLOADS, enabled).apply()
    }

    fun qualityLabel(context: Context): String = getDefaultQuality(context).label

    fun getLocalPassword(context: Context): String? {
        val secure = SecurePreferences.prefs(context).getString(KEY_LOCAL_PASSWORD, null)
        if (secure != null) return secure
        val legacy = prefs(context).getString(KEY_LOCAL_PASSWORD, null)
        if (legacy != null) {
            setLocalPassword(context, legacy)
            prefs(context).edit().remove(KEY_LOCAL_PASSWORD).apply()
        }
        return legacy
    }

    fun setLocalPassword(context: Context, password: String) {
        SecurePreferences.prefs(context).edit()
            .putString(KEY_LOCAL_PASSWORD, password)
            .apply()
        prefs(context).edit().remove(KEY_LOCAL_PASSWORD).apply()
    }

    fun clearLocalPassword(context: Context) {
        SecurePreferences.prefs(context).edit().remove(KEY_LOCAL_PASSWORD).apply()
        prefs(context).edit().remove(KEY_LOCAL_PASSWORD).apply()
    }
}
