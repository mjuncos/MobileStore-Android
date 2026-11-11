package com.mobilestore.core

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.mobilestore.MainActivity
import com.mobilestore.R

/**
 * Punto 3.5 del enunciado: notificacion del sistema mediante NotificationCompat.
 *
 * Se dispara al confirmar una compra, que es el momento relevante del caso
 * de negocio: la venta queda registrada y el vendedor se lleva el aviso
 * aunque cierre la aplicacion.
 *
 * El permiso POST_NOTIFICATIONS se pide recien en ese momento, no al abrir
 * la aplicacion (punto 3.6: permisos solo cuando son necesarios).
 */
object NotificationHelper {

    private const val CHANNEL_ID = "mobilestore_orders"
    private const val ORDER_NOTIFICATION_ID = 1001

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_orders),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notification_channel_orders_description)
        }
        manager.createNotificationChannel(channel)
    }

    fun hasPermission(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

    fun showOrderConfirmed(context: Context, itemCount: Int, formattedTotal: String) {
        if (!hasPermission(context)) return
        ensureChannel(context)

        // Al tocar la notificacion se vuelve a abrir la aplicacion.
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val text = context.getString(R.string.notification_order_text, itemCount, formattedTotal)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(context.getString(R.string.notification_order_title))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(ORDER_NOTIFICATION_ID, notification)
        }
    }
}
