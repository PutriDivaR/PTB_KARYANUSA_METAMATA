package com.example.karyanusa.fcm

import android.util.Log
import com.example.karyanusa.component.auth.LoginTokenManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FirebaseNotifikasi : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM_TOKEN", "Token baru: $token")

        // Simpan token ke SharedPreferences
        val manager = LoginTokenManager(baseContext)
        manager.saveFcmToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        Log.d("FCM_DEBUG", "Message received!")
        Log.d("FCM_DEBUG", "Data: ${message.data}")
        Log.d("FCM_DEBUG", "Notification: ${message.notification}")

        val title = message.data["title"] ?: "Notifikasi"
        val body = message.data["body"] ?: "Pesan baru"

        NotificationHelper.showNotification(
            applicationContext,
            System.currentTimeMillis().toInt(),
            title,
            body
        )
    }

}
