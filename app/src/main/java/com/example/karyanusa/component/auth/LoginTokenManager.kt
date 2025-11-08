package com.example.karyanusa.component.auth


import android.content.Context
import android.content.SharedPreferences

class LoginTokenManager(context: Context) {

    private val prefsName = "LoginToken"
    private val keyToken = "user_token"
    private val keyUserId = "user_id"
    private val keyUserName = "user_name"

    private val prefs: SharedPreferences =
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    fun saveToken(token: String, userId: String, userName: String) {
        prefs.edit().apply {
            putString(keyToken, token)
            putString(keyUserId, userId)
            putString(keyUserName, userName)
            apply()
        }
    }

    fun getToken(): String? = prefs.getString(keyToken, null)

    fun getUserId(): String? = prefs.getString(keyUserId, null)

    fun getUserName(): String? = prefs.getString(keyUserName, null)

    fun clear() {
        prefs.edit().clear().apply()
    }
}
