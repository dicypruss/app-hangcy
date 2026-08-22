package com.dicypruss.hangcy.notify

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
import com.dicypruss.hangcy.MainActivity
import com.dicypruss.hangcy.R

class BlockedCallNotifier(private val context: Context) {
    fun notifyRejected(number: String?) {
        if (!canNotify()) {
            return
        }
        ensureChannel()
        val text = number?.takeIf { it.isNotBlank() }
            ?: context.getString(R.string.unknown_number)
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setContentTitle(context.getString(R.string.call_rejected_title))
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()
        val id = (System.currentTimeMillis() and 0x7fffffff).toInt()
        NotificationManagerCompat.from(context).notify(id, notification)
    }

    private fun canNotify(): Boolean {
        if (Build.VERSION.SDK_INT < 33) {
            return true
        }
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun ensureChannel() {
        val manager = context.getSystemService(NotificationManager::class.java)
        val existing = manager.getNotificationChannel(CHANNEL_ID)
        if (existing != null) {
            return
        }
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.blocked_calls_channel),
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
    }

    private companion object {
        const val CHANNEL_ID = "blocked_calls"
    }
}
