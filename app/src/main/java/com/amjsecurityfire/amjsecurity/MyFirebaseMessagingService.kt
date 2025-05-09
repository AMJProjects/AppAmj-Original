package com.amjsecurityfire.amjsecurity

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d("FCM", "Notificação recebida")

        val escopoId = remoteMessage.data["escopoId"]
        val title = remoteMessage.data["title"] ?: "Novo Escopo"
        val body = remoteMessage.data["body"] ?: "Clique para ver o escopo"

        Log.d("FCM", "Dados recebidos: escopoId=$escopoId")

        NotificationHelper.createNotificationChannel(this)

        if (escopoId != null) {
            NotificationHelper.showEscopoNotification(this, escopoId, title, body)
            Log.d("FCM", "Chamou NotificationHelper.showEscopoNotification com título: $title")
        } else {
            NotificationHelper.showSimpleNotification(this, title, body)
            Log.d("FCM", "Chamou NotificationHelper.showSimpleNotification")
        }
    }


}