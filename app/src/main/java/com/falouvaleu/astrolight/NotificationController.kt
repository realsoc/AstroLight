package com.falouvaleu.astrolight

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.support.annotation.RequiresApi
import android.support.v4.app.NotificationCompat
import com.falouvaleu.astrolight.LightService.Companion.DISABLE_LIGHT

interface NotificationController {

    enum class Type {
        LIGHT_ACTIVE
    }

    fun bindServiceToNotification(service: Service, notificationType: Type)
    fun showNotification(notificationType: Type, notificationId: Int)
    fun cancelNotification(notificationId: Int)

    companion object {

        const val FOREGROUND_NOTIFICATION_ID = 1
        const val CHANNEL_ID = "ASTRO_LIGHT"

        private const val CHANNEL_NAME = "AstroLight"
        private const val CHANNEL_DESCRIPTION = "Une lumière dans la nuit"

        fun initialize(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !mainNotificationChannelExists(context)) {
                createMainNotificationChannel(context)
            }
        }

        @RequiresApi(Build.VERSION_CODES.O)
        private fun createMainNotificationChannel(context: Context) {
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            channel.description = CHANNEL_DESCRIPTION
            channel.enableLights(true)
            channel.lightColor = Color.RED
            channel.enableVibration(true)
            notificationManager.createNotificationChannel(channel)
        }

        @RequiresApi(Build.VERSION_CODES.O)
        private fun mainNotificationChannelExists(context: Context): Boolean {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            return notificationManager.getNotificationChannel(CHANNEL_ID) != null
        }
    }
}

class NotificationControllerImpl(
    private val context: Context
): NotificationController {

    private val notificationManager: NotificationManager by lazy { context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }

    override fun bindServiceToNotification(service: Service, notificationType: NotificationController.Type) {
        val notification = buildForegroundNotification(service, notificationType)
        service.startForeground(NotificationController.FOREGROUND_NOTIFICATION_ID, notification)
    }

    override fun showNotification(notificationType: NotificationController.Type, notificationId: Int) {
        val notification = buildForegroundNotification(context, notificationType)
        notificationManager.notify(notificationId, notification)
    }

    override fun cancelNotification(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }

    private fun getDefaultNotificationBuilder(context: Context): NotificationCompat.Builder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationCompat.Builder(context, NotificationController.CHANNEL_ID)
                .setSmallIcon(R.drawable.icon_star)
                .setColor(Color.RED)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setWhen(System.currentTimeMillis())
                .setVibrate(longArrayOf(0, 100))
                .setOnlyAlertOnce(true)
        } else {
            NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.icon_star)
                .setColor(Color.RED)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setWhen(System.currentTimeMillis())
                .setOnlyAlertOnce(true)
        }
    }

    private fun buildForegroundNotification(context: Context, type: NotificationController.Type): Notification {
        val builder = getDefaultNotificationBuilder(context)
        when (type) {
            NotificationController.Type.LIGHT_ACTIVE -> {
                val contentIntent = Intent(context, LightActivity::class.java)
                val actionIntent = Intent(context, LightActivity::class.java).apply { action = DISABLE_LIGHT }
                contentIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                builder.setContentTitle(context.getString(R.string.app_name))
                builder.setContentIntent(PendingIntent.getActivity(context, 0, contentIntent, PendingIntent.FLAG_UPDATE_CURRENT))
                builder.addAction(
                    android.R.drawable.ic_lock_power_off,
                    "Arrêter l'observation",
                    PendingIntent.getActivity(context, 0, actionIntent, PendingIntent.FLAG_UPDATE_CURRENT))
            }
        }

        return builder.build()
    }


}