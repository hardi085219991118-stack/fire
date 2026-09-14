package com.example.data.engine

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.data.model.AlertSeverity
import com.example.data.model.FireAlert
import com.example.data.model.Hotspot
import com.example.data.repository.DistanceCalculator
import com.example.data.repository.FireAgeCalculator

data class AlertEngineConfig(
    val maxFreshnessMinutes: Long = 30L,
    val maxDistanceKm: Double = 25.0,
    val minConfidenceLevel: String = "ALL", // "HIGH", "NOMINAL", "ALL"
    val cooldownMinutes: Long = 60L
)

/**
 * Feature 221–235: FIRE ALERT ENGINE
 */
class FireAlertEngine(private val context: Context) {

    private val sentAlertFingerprints = mutableMapOf<String, Long>() // Fingerprint -> alertedAt
    private val alertHistory = mutableListOf<FireAlert>()

    companion object {
        const val CHANNEL_ID = "hardi_fire_alerts"
        const val CHANNEL_NAME = "Peringatan Pemantauan Hotspot Satelit"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Peringatan pemantauan anomali termal satelit (Bukan konfirmasi kebakaran)"
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    /**
     * Feature 221 & 222: Evaluasi hotspot baru dan bangkitkan alert jika memenuhi filter
     */
    fun evaluateAndGenerateAlerts(
        newHotspots: List<Hotspot>,
        config: AlertEngineConfig = AlertEngineConfig(),
        currentTimestamp: Long = System.currentTimeMillis()
    ): List<FireAlert> {
        val generatedAlerts = mutableListOf<FireAlert>()

        for (hs in newHotspots) {
            // Feature 223: Alert Freshness Gate
            if (hs.satelliteAgeMinutes > config.maxFreshnessMinutes) continue

            // Feature 224: Alert Distance Gate
            val dist = hs.distanceFromUser
            if (dist != null && dist > config.maxDistanceKm) continue

            // Feature 225: Alert Confidence Gate
            if (!matchesConfidence(hs.confidence, config.minConfidenceLevel)) continue

            // Feature 226 & 227: Alert Cooldown & Deduplication
            val alertKey = "${hs.fingerprint ?: hs.id}_${hs.acquisitionTimestamp}"
            val lastAlertedTime = sentAlertFingerprints[alertKey]
            if (lastAlertedTime != null) {
                val cooldownMs = config.cooldownMinutes * 60 * 1000L
                if (currentTimestamp - lastAlertedTime < cooldownMs) {
                    continue // Sedang dalam masa cooldown, lewati
                }
            }

            // Feature 228: Alert Severity (Prioritas Pemantauan, bukan bahaya api)
            val severity = determineSeverity(hs)

            // Feature 229: Alert Explanation
            val explanations = mutableListOf<String>()
            if (dist != null) {
                explanations.add("• Jarak terdeteksi: ${DistanceCalculator.formatDistanceKm(dist)} dari pusat pemantauan")
            }
            explanations.add("• Observasi satelit: ${hs.satelliteAgeMinutes} menit lalu (${FireAgeCalculator.formatTimeOnly(hs.acquisitionTimestamp)})")
            explanations.add("• Satelit pengamat: ${hs.satellite} (${hs.instrument})")
            if (!hs.confidence.isNullOrBlank()) {
                explanations.add("• Tingkat keyakinan algoritma: ${hs.confidence}")
            }
            if (hs.isMultiSatelliteObs) {
                explanations.add("• Area ini juga diamati oleh satelit lain (multi-satellite)")
            }

            val alert = FireAlert(
                alertId = "ALT_${System.currentTimeMillis()}_${hs.id}",
                hotspotFingerprint = hs.fingerprint ?: hs.id,
                severity = severity,
                title = "🔔 ANOMALI TERMAL BARU (${severity.label})",
                observationTimestamp = hs.acquisitionTimestamp,
                alertedAt = currentTimestamp,
                dataAgeMinutes = hs.satelliteAgeMinutes,
                distanceKm = dist,
                satellite = hs.satellite,
                confidence = hs.confidence,
                explanationLines = explanations,
                safetyDisclaimer = "Hotspot satelit bukan konfirmasi kebakaran di lapangan. Perlu verifikasi darat."
            )

            sentAlertFingerprints[alertKey] = currentTimestamp
            generatedAlerts.add(alert)
            alertHistory.add(0, alert) // Newest first

            // Feature 231 & 232: Kirim notifikasi Android jika izin ada
            dispatchAndroidNotification(alert)
        }

        return generatedAlerts
    }

    private fun matchesConfidence(hotspotConfidence: String?, minLevel: String): Boolean {
        if (minLevel.equals("ALL", ignoreCase = true)) return true
        if (hotspotConfidence == null) return false

        val conf = hotspotConfidence.trim().lowercase()
        return when (minLevel.uppercase()) {
            "HIGH" -> conf == "high" || conf == "h" || (conf.toIntOrNull() ?: 0) >= 80
            "NOMINAL" -> conf == "high" || conf == "nominal" || conf == "h" || conf == "n" || (conf.toIntOrNull() ?: 0) >= 50
            else -> true
        }
    }

    private fun determineSeverity(hs: Hotspot): AlertSeverity {
        val dist = hs.distanceFromUser ?: Double.MAX_VALUE
        val age = hs.satelliteAgeMinutes
        val isHighConf = hs.confidence?.lowercase() in listOf("high", "h") || (hs.confidence?.toIntOrNull() ?: 0) >= 80

        return when {
            dist <= 5.0 && age <= 30 && isHighConf -> AlertSeverity.HIGH
            dist <= 15.0 && age <= 60 -> AlertSeverity.MEDIUM
            dist <= 25.0 -> AlertSeverity.LOW
            else -> AlertSeverity.INFO
        }
    }

    /**
     * Feature 231 & 232: Dispatch Android Notification with permission checks
     */
    private fun dispatchAndroidNotification(alert: FireAlert) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionGranted = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!permissionGranted) {
                // Jangan crash jika izin ditolak (Feature 231)
                return
            }
        }

        try {
            val distStr = alert.distanceKm?.let { "Jarak: ${DistanceCalculator.formatDistanceKm(it)}" } ?: ""
            val body = "$distStr • Satelit: ${alert.satellite} (${alert.dataAgeMinutes}m lalu)\n${alert.safetyDisclaimer}"

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(alert.title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText("${alert.explanationLines.joinToString("\n")}\n\n⚠️ ${alert.safetyDisclaimer}"))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.notify(alert.alertId.hashCode(), builder.build())
        } catch (_: Exception) {
            // Graceful fallback
        }
    }

    fun getAlertHistory(): List<FireAlert> = alertHistory.toList()

    fun clearAlertHistory() {
        alertHistory.clear()
    }
}
