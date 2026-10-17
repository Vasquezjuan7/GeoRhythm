package com.example.georhythm.core

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.compose.runtime.snapshotFlow
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.georhythm.GeoRhythmApp
import com.example.georhythm.MainActivity
import com.example.georhythm.R
import com.example.georhythm.ui.components.formatDistanceText
import com.example.georhythm.ui.components.formatWalkingTimeText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * Servicio en primer plano (ubicación + reproducción) que mantiene vivos el GPS, la navegación,
 * la audio-guía y la música con la pantalla apagada. Se detiene solo tras 60 s sin ruta ni música.
 */
class WalkService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var controller: GeoRhythmController

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        controller = (application as GeoRhythmApp).controller
        createChannel()
        if (!goForeground()) {
            stopSelf()
            return
        }
        controller.serviceRunning = true

        // Mantener la notificación al día (ruta restante / canción actual)
        scope.launch {
            snapshotFlow { notificationKey() }
                .distinctUntilChanged()
                .collect { getSystemService(NotificationManager::class.java).notify(NOTIF_ID, buildNotification()) }
        }
        // Apagarse si no hay ruta ni música durante 60 s
        scope.launch {
            snapshotFlow { controller.isWalkActive }
                .distinctUntilChanged()
                .collectLatest { active ->
                    if (!active) {
                        delay(60_000)
                        stopSelf()
                    }
                }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_TOGGLE_PLAY -> controller.player.togglePlayPause()
            ACTION_NEXT -> controller.player.next()
            ACTION_END_ROUTE -> controller.finishNavigation(arrived = false)
            ACTION_STOP -> {
                controller.stopEverything()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        if (::controller.isInitialized) controller.serviceRunning = false
        super.onDestroy()
    }

    private fun goForeground(): Boolean = runCatching {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            var type = ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            if (controller.hasLocationPermission()) type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            startForeground(NOTIF_ID, notification, type)
        } else {
            startForeground(NOTIF_ID, notification)
        }
    }.isSuccess

    /** Texto que, al cambiar, obliga a refrescar la notificación (distancia redondeada a 50 m). */
    private fun notificationKey(): String {
        val r = controller.tracker.route
        val p = controller.player
        return "${r?.destinationName}|${(controller.tracker.remainingMeters / 50).toInt()}|" +
            "${controller.tracker.nextInstruction?.instruction}|${p.title}|${p.isPlaying}|${controller.tracker.cadenceSpm / 5}"
    }

    private fun buildNotification(): Notification {
        val route = controller.tracker.route
        val player = controller.player
        val title: String
        val text: String
        if (route != null) {
            val km = (controller.tracker.remainingMeters / 1000).toFloat()
            title = "Hacia ${route.destinationName} · ${formatDistanceText(km)} (${formatWalkingTimeText(km)})"
            text = controller.tracker.nextInstruction?.instruction ?: "Sigue la ruta"
        } else {
            title = player.title
            text = player.artist
        }
        val cadence = controller.tracker.cadenceSpm
        val sub = buildString {
            if (cadence > 0) append("$cadence pasos/min")
            if (route != null && player.isPlaying) {
                if (isNotEmpty()) append(" · ")
                append("♪ ${player.title}")
            }
        }

        val openApp = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val b = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_walk)
            .setContentTitle(title)
            .setContentText(text)
            .setSubText(sub.ifBlank { null })
            .setContentIntent(openApp)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(if (route != null) NotificationCompat.CATEGORY_NAVIGATION else NotificationCompat.CATEGORY_TRANSPORT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)

        if (player.hasMedia) {
            b.addAction(
                if (player.isPlaying) R.drawable.ic_stat_pause else R.drawable.ic_stat_play,
                if (player.isPlaying) "Pausar" else "Reproducir", action(ACTION_TOGGLE_PLAY)
            )
            b.addAction(R.drawable.ic_stat_next, "Siguiente", action(ACTION_NEXT))
        }
        if (route != null) b.addAction(R.drawable.ic_stat_stop, "Terminar ruta", action(ACTION_END_ROUTE))
        else b.addAction(R.drawable.ic_stat_stop, "Detener", action(ACTION_STOP))
        return b.build()
    }

    private fun action(name: String): PendingIntent = PendingIntent.getService(
        this, name.hashCode(), Intent(this, WalkService::class.java).setAction(name),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Caminata en curso", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Ruta, ritmo y música mientras caminas con la pantalla apagada"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "walk_session"
        private const val NOTIF_ID = 42
        private const val ACTION_TOGGLE_PLAY = "georhythm.TOGGLE_PLAY"
        private const val ACTION_NEXT = "georhythm.NEXT"
        private const val ACTION_END_ROUTE = "georhythm.END_ROUTE"
        private const val ACTION_STOP = "georhythm.STOP"

        fun start(context: Context) {
            runCatching {
                ContextCompat.startForegroundService(context, Intent(context, WalkService::class.java))
            }
        }
    }
}
