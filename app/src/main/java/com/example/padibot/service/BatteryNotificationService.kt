package com.example.padibot.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.padibot.MainActivity
import com.example.padibot.R
import com.example.padibot.model.Telemetry
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

enum class BatteryAlertSeverity {
    WARNING,  // <= 20%
    CRITICAL  // <= 10%
}

data class BatteryAlertEvent(
    val batteryPct: Float,
    val severity: BatteryAlertSeverity,
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

class BatteryNotificationService(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "padibot_battery_alerts"
        const val CHANNEL_NAME = "Peringatan Baterai Robot PadiBot"
        const val CHANNEL_DESC = "Notifikasi status baterai rendah dan kritis untuk robot penanam padi otonom"
        const val NOTIFICATION_ID_LOW_BATTERY = 1001
        const val NOTIFICATION_ID_CRITICAL_BATTERY = 1002
        private const val TAG = "BatteryNotification"
    }

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val _batteryAlertEvents = MutableSharedFlow<BatteryAlertEvent>(extraBufferCapacity = 10)
    val batteryAlertEvents: SharedFlow<BatteryAlertEvent> = _batteryAlertEvents.asSharedFlow()

    // Tracking state to avoid noisy spamming while ensuring reliable alerting
    private var hasAlertedLow = false
    private var hasAlertedCritical = false
    private var lastAlertedPct = -1f
    private var lastAlertedTime = 0L

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 400, 200, 400)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Evaluates current telemetry and triggers system notification and in-app event if battery <= 20%
     */
    fun processBatteryTelemetry(telemetry: Telemetry) {
        val batteryPct = telemetry.batteryPct
        val now = System.currentTimeMillis()

        // Reset alert triggers if battery is recharged above 25%
        if (batteryPct > 25f) {
            hasAlertedLow = false
            hasAlertedCritical = false
            lastAlertedPct = batteryPct
            return
        }

        // Trigger Critical Alert (<= 10%)
        if (batteryPct <= 10f) {
            val shouldTrigger = !hasAlertedCritical || (now - lastAlertedTime > 60_000L && Math.abs(batteryPct - lastAlertedPct) >= 2f)
            if (shouldTrigger) {
                hasAlertedCritical = true
                hasAlertedLow = true
                lastAlertedPct = batteryPct
                lastAlertedTime = now
                val event = BatteryAlertEvent(
                    batteryPct = batteryPct,
                    severity = BatteryAlertSeverity.CRITICAL,
                    title = "🚨 BATERAI KRITIS: PadiBot (${batteryPct.toInt()}%)",
                    message = "Kapasitas daya tersisa ${batteryPct.toInt()}%! Mesin tanam berisiko berhenti otomatis. Segera amankan robot atau swap baterai."
                )
                _batteryAlertEvents.tryEmit(event)
                showSystemNotification(event, NOTIFICATION_ID_CRITICAL_BATTERY)
                vibrateDevice(isCritical = true)
            }
            return
        }

        // Trigger Warning Alert (<= 20%)
        if (batteryPct <= 20f) {
            val shouldTrigger = !hasAlertedLow || (now - lastAlertedTime > 120_000L && Math.abs(batteryPct - lastAlertedPct) >= 3f)
            if (shouldTrigger) {
                hasAlertedLow = true
                lastAlertedPct = batteryPct
                lastAlertedTime = now
                val event = BatteryAlertEvent(
                    batteryPct = batteryPct,
                    severity = BatteryAlertSeverity.WARNING,
                    title = "⚠️ Peringatan Baterai Lemah: PadiBot (${batteryPct.toInt()}%)",
                    message = "Daya baterai tersisa ${batteryPct.toInt()}% (${telemetry.estimatedRemainingTimeString}). Disarankan menyelesaikan baris tanam atau bawa ke dock pengisian."
                )
                _batteryAlertEvents.tryEmit(event)
                showSystemNotification(event, NOTIFICATION_ID_LOW_BATTERY)
                vibrateDevice(isCritical = false)
            }
        }
    }

    /**
     * Force-trigger a test notification (useful for verification & user testing)
     */
    fun triggerTestAlert(percentage: Float = 18f) {
        val severity = if (percentage <= 10f) BatteryAlertSeverity.CRITICAL else BatteryAlertSeverity.WARNING
        val event = BatteryAlertEvent(
            batteryPct = percentage,
            severity = severity,
            title = if (severity == BatteryAlertSeverity.CRITICAL) "🚨 BATERAI KRITIS (Uji Notifikasi: ${percentage.toInt()}%)" else "⚠️ Peringatan Baterai Lemah: PadiBot (${percentage.toInt()}%)",
            message = "Pengujian Sistem Notifikasi Lokal: Baterai robot berada di level rendah (${percentage.toInt()}%)."
        )
        _batteryAlertEvents.tryEmit(event)
        showSystemNotification(event, if (severity == BatteryAlertSeverity.CRITICAL) NOTIFICATION_ID_CRITICAL_BATTERY else NOTIFICATION_ID_LOW_BATTERY)
        vibrateDevice(isCritical = (severity == BatteryAlertSeverity.CRITICAL))
    }

    private fun showSystemNotification(event: BatteryAlertEvent, notificationId: Int) {
        // Check POST_NOTIFICATIONS permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "Cannot post notification: POST_NOTIFICATIONS permission not granted")
                return
            }
        }

        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                .setContentTitle(event.title)
                .setContentText(event.message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(event.message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setSound(soundUri)
                .setVibrate(
                    if (event.severity == BatteryAlertSeverity.CRITICAL)
                        longArrayOf(0, 500, 200, 500, 200, 500)
                    else
                        longArrayOf(0, 350, 150, 350)
                )

            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException while showing notification", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error posting battery notification", e)
        }
    }

    private fun vibrateDevice(isCritical: Boolean) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val vibrator = vibratorManager?.defaultVibrator
                val timings = if (isCritical) longArrayOf(0, 400, 200, 400, 200, 400) else longArrayOf(0, 300, 150, 300)
                val amplitudes = if (isCritical) intArrayOf(0, 255, 0, 255, 0, 255) else intArrayOf(0, 180, 0, 180)
                vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                val pattern = if (isCritical) longArrayOf(0, 400, 200, 400) else longArrayOf(0, 300, 150, 300)
                vibrator?.vibrate(pattern, -1)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not vibrate device", e)
        }
    }
}
