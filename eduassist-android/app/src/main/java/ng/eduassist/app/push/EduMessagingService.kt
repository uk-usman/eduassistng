package ng.eduassist.app.push

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ng.eduassist.app.MainActivity
import ng.eduassist.app.R
import ng.eduassist.app.data.Api
import ng.eduassist.app.data.ApiException
import ng.eduassist.app.data.Session
import org.json.JSONObject

const val CHANNEL_ID = "eduassist_updates"

fun ensureChannel(ctx: Context) {
    if (Build.VERSION.SDK_INT >= 26) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Request updates", NotificationManager.IMPORTANCE_HIGH))
    }
}

/** Sends this phone's push token to the server (only works after Firebase is set up with google-services.json). */
object PushRegistrar {
    fun register(ctx: Context) {
        if (FirebaseApp.getApps(ctx).isEmpty() || Session.token == null) return
        ensureChannel(ctx)
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token -> send(token) }
    }

    fun send(token: String) {
        if (Session.token == null) return
        CoroutineScope(Dispatchers.IO).launch {
            try { Api.call("POST", "/devices", JSONObject().put("token", token)) } catch (_: ApiException) {}
        }
    }

    /** Removes this phone from the account (call before logging out). */
    suspend fun unregister() {
        if (Session.token == null) return
        try {
            val token = kotlinx.coroutines.withContext(Dispatchers.IO) { com.google.android.gms.tasks.Tasks.await(FirebaseMessaging.getInstance().token) }
            Api.call("POST", "/devices/remove", JSONObject().put("token", token))
        } catch (_: Exception) {}
    }
}

class EduMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) { PushRegistrar.send(token) }

    override fun onMessageReceived(message: RemoteMessage) {
        val n = message.notification ?: return
        ensureChannel(this)
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher).setContentTitle(n.title).setContentText(n.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(n.body)).setAutoCancel(true).setContentIntent(open).build()
        NotificationManagerCompat.from(this).notify(System.currentTimeMillis().toInt(), notif)
    }
}
