package com.example.karyanusa.fcm

import android.content.Intent
import android.util.Log
import com.example.karyanusa.MainActivity
import com.example.karyanusa.component.auth.LoginTokenManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FirebaseNotifikasi : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM_TOKEN", "Token baru: $token")

        val manager = LoginTokenManager(baseContext)
        manager.saveFcmToken(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {

        val data = remoteMessage.data

        val type = data["type"]
        val relatedId = data["related_id"]
        val notifId = data["notif_id"]
        val title = data["title"] ?: "Notifikasi"
        val body = data["body"] ?: "Pesan Baru"

        val notificationId = notifId?.toIntOrNull() ?: System.currentTimeMillis().toInt()

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("type", type)
            putExtra("related_id", relatedId)
            putExtra("notif_id", notifId)
        }

        NotificationHelper.showNotification(
            context = this,
            title = title,
            body = body,
            intent = intent,
            id = notificationId
        )
    }
}

