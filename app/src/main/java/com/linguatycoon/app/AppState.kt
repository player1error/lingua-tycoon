package com.linguatycoon.app

import android.content.Context
import java.security.MessageDigest

class AppState(context: Context) {
    private val prefs = context.getSharedPreferences("lingua_tycoon", Context.MODE_PRIVATE)

    var username: String
        get() = prefs.getString("username", "") ?: ""
        set(v) = prefs.edit().putString("username", v).apply()
    var passwordHash: String
        get() = prefs.getString("password", "") ?: ""
        set(v) = prefs.edit().putString("password", v).apply()
    var loggedIn: Boolean
        get() = prefs.getBoolean("logged_in", false)
        set(v) = prefs.edit().putBoolean("logged_in", v).apply()
    var nativeLanguage: String
        get() = prefs.getString("native_language", "English") ?: "English"
        set(v) = prefs.edit().putString("native_language", v).apply()
    var targetLanguage: String
        get() = prefs.getString("target_language", "Spanish") ?: "Spanish"
        set(v) = prefs.edit().putString("target_language", v).apply()
    var xp: Int
        get() = prefs.getInt("xp", 0)
        set(v) = prefs.edit().putInt("xp", v).apply()
    var lessons: Int
        get() = prefs.getInt("lessons", 0)
        set(v) = prefs.edit().putInt("lessons", v).apply()
    var businessLevel: Int
        get() = prefs.getInt("business_level", 0)
        set(v) = prefs.edit().putInt("business_level", v).apply()
    var apiUrl: String
        get() = prefs.getString("api_url", BuildConfig.DEFAULT_API_URL) ?: BuildConfig.DEFAULT_API_URL
        set(v) = prefs.edit().putString("api_url", v.trimEnd('/')).apply()
    var githubRepository: String
        get() = prefs.getString("github_repo", BuildConfig.GITHUB_REPOSITORY) ?: BuildConfig.GITHUB_REPOSITORY
        set(v) = prefs.edit().putString("github_repo", v.trim()).apply()

    fun register(name: String, password: String) {
        username = name.trim()
        passwordHash = hash(password)
        loggedIn = true
    }

    fun login(name: String, password: String) =
        name.trim() == username && hash(password) == passwordHash

    companion object {
        fun hash(value: String): String = MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}
