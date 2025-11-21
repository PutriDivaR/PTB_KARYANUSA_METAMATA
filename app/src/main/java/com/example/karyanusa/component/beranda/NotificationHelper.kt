package com.example.karyanusa.component.beranda

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.karyanusa.R

object NotifHelper {

    private const val CHANNEL_ID = "karya_upload_channel"

    fun showUploadSuccessNotification(context: Context) {

        // Cek izin notifikasi Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Izin belum diberikan → jangan crash
                return
            }
        }

        // Buat channel (Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Karya Upload",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        // Build notification
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Upload Berhasil")
            .setContentText("Karya kamu berhasil diunggah 🎉")
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(
            System.currentTimeMillis().toInt(),
            notification
        )
    }
}
