package com.example.karyanusa.component.auth

import android.content.Context
import android.content.SharedPreferences

class LoginTokenManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("LoginToken", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_TOKEN = "user_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
    }

    fun saveToken(token: String?, userId: String?, userName: String?) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putInt(KEY_USER_ID, userId?.toIntOrNull() ?: -1)
            .putString(KEY_USER_NAME, userName)
            .apply()
    }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun getBearerToken(): String? {
        val token = getToken()
        return if (token != null) "Bearer $token" else null
    }

    fun getUserId(): Int? {
        val id = prefs.getInt(KEY_USER_ID, -1)
        return if (id == -1) null else id
    }

    fun getUserIdString(): String? {
        val id = prefs.getInt(KEY_USER_ID, -1)
        return if (id == -1) null else id.toString()
    }

    fun getUserName(): String? = prefs.getString(KEY_USER_NAME, null)

    fun clear() {
        prefs.edit().clear().apply()
    }
}
