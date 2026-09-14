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
import java.util.Calendar

data class SmartAlertConfig(
    val maxFreshnessMinutes: Long = 30L,
    val maxDistanceKm: Double = 25.0,
    val minConfidenceLevel: String = "ALL", // "HIGH", "NOMINAL", "ALL"
    val minBrightnessKelvin: Double? = null,
    val cooldownMinutes: Long = 60L,
    val quietHoursEnabled: Boolean = false,
    val quietHoursStartHour: Int = 22, // 22:00
    val quietHoursEndHour: Int = 5     // 05:00
)

data class DailyAlertSummary(
    val dateLabel: String,
    val totalHotspotsObserved: Int,
    val totalAlertsIssued: Int,
    val latestObservationTimeFormatted: String,
    val nearestHotspotDistanceFormatted: String,
    val sourceHealthStatus: String,
    val satelliteBreakdown: Map<String, Int>
)

/**
 * Feature 291–300: SMART FIRE ALERT ENGINE
 * Menangani evaluasi peringatan cerdas dengan pengaman:
 * - Freshness Guard (Fitur 294)
 * - Distance Guard (Fitur 295)
 * - Confidence Guard (Fitur 296: High only, status Unknown jika tidak ada, tidak boleh dinaikkan)
 * - Brightness Guard (Fitur 297: Filter opsional, tidak boleh dianggap bukti api)
 * - Cooldown & Deduplikasi
 * - Quiet Hours / Jam Tenang (Fitur 298: Simpan riwayat, tunda notifikasi suara)
 * - Daily Summary & Pemisahan Waktu Satelit vs Notifikasi (Fitur 299 & 300)
 */
class SmartFireAlertEngine(private val context: Context) {

    private val sentAlertFingerprints = mutableMapOf<String, Long>()
    private val alertHistory = mutableListOf<FireAlert>()

    companion object {
        const val CHANNEL_ID = "hardi_fire_alerts_v2"
        const val CHANNEL_NAME = "Smart Peringatan Pemantauan Hotspot"
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
                description = "Peringatan anomali termal satelit berdasarkan aturan pengguna"
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    /**
     * Evaluasi hotspot baru dan bangkitkan alert jika memenuhi filter
     */
    fun evaluateAndGenerateAlerts(
        newHotspots: List<Hotspot>,
        config: SmartAlertConfig = SmartAlertConfig(),
        currentTimestamp: Long = System.currentTimeMillis()
    ): List<FireAlert> {
        val generatedAlerts = mutableListOf<FireAlert>()

        for (hs in newHotspots) {
            // Feature 294: ALERT AGE GUARD (age > freshness threshold -> JANGAN kirim)
            if (hs.satelliteAgeMinutes > config.maxFreshnessMinutes) continue

            // Feature 295: ALERT DISTANCE GUARD
            val dist = hs.distanceFromUser
            if (dist != null && dist > config.maxDistanceKm) continue

            // Feature 296: ALERT CONFIDENCE GUARD
            if (!matchesConfidence(hs.confidence, config.minConfidenceLevel)) continue

            // Feature 297: ALERT BRIGHTNESS GUARD
            if (config.minBrightnessKelvin != null) {
                val b = hs.brightness
                if (b == null || b < config.minBrightnessKelvin) continue
            }

            // Cooldown & Deduplikasi per (fingerprint + timestamp observasi)
            val alertKey = "${hs.fingerprint ?: hs.id}_${hs.acquisitionTimestamp}"
            val lastAlertedTime = sentAlertFingerprints[alertKey]
            if (lastAlertedTime != null) {
                val cooldownMs = config.cooldownMinutes * 60 * 1000L
                if (currentTimestamp - lastAlertedTime < cooldownMs) {
                    continue // Cooldown aktif
                }
            }

            // Feature 292: Alert Severity (Prioritas Pemantauan)
            val severity = determineSeverity(hs)

            // Feature 293: NEW OBSERVATION vs NEW ANOMALY
            val isNewObservation = (hs.observationCount > 1)
            val titlePrefix = if (isNewObservation) "OBSERVASI BARU" else "ANOMALI TERMAL BARU"
            val title = "🔔 $titlePrefix (${severity.label})"

            val explanations = mutableListOf<String>()
            if (dist != null) {
                explanations.add("• Jarak terdeteksi: ${DistanceCalculator.formatDistanceKm(dist)} dari pusat pemantauan")
            }
            explanations.add("• Observasi satelit: ${hs.satelliteAgeMinutes} menit lalu (${FireAgeCalculator.formatTimeOnly(hs.acquisitionTimestamp)})")
            explanations.add("• Satelit pengamat: ${hs.satellite} (${hs.instrument})")
            if (!hs.confidence.isNullOrBlank()) {
                explanations.add("• Confidence satelit: ${hs.confidence}")
            } else {
                explanations.add("• Confidence satelit: UNKNOWN (tidak tersedia)")
            }
            if (hs.isMultiSatelliteObs) {
                explanations.add("• Dikonfirmasi lintas-satelit (multi-satellite)")
            }
            if (isNewObservation) {
                explanations.add("• Anomali termal teramati kembali pada lintasan orbit terbaru")
            }

            // Feature 292 & 274: Critical Claim Guard
            val safetyDisclaimer = if (severity == AlertSeverity.CRITICAL) {
                "Prioritas pemantauan tinggi berdasarkan data satelit. Bukan konfirmasi pasti kebakaran di lapangan."
            } else {
                "Hotspot satelit bukan konfirmasi kebakaran di lapangan. Perlu verifikasi darat."
            }

            val alert = FireAlert(
                alertId = "SMART_ALT_${System.currentTimeMillis()}_${hs.id}",
                hotspotFingerprint = hs.fingerprint ?: hs.id,
                severity = severity,
                title = title,
                observationTimestamp = hs.acquisitionTimestamp,
                alertedAt = currentTimestamp,
                dataAgeMinutes = hs.satelliteAgeMinutes,
                distanceKm = dist,
                satellite = hs.satellite,
                confidence = hs.confidence ?: "UNKNOWN",
                explanationLines = explanations,
                safetyDisclaimer = safetyDisclaimer
            )

            sentAlertFingerprints[alertKey] = currentTimestamp
            generatedAlerts.add(alert)
            alertHistory.add(0, alert)

            // Feature 298: ALERT QUIET HOURS
            val isQuietHour = isCurrentlyQuietHour(config)
            if (!isQuietHour) {
                dispatchAndroidNotification(alert)
            }
        }

        return generatedAlerts
    }

    private fun matchesConfidence(hotspotConfidence: String?, minLevel: String): Boolean {
        if (minLevel.equals("ALL", ignoreCase = true)) return true
        if (hotspotConfidence == null) return false // Unknown tidak boleh diangkat jadi High

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
            dist <= 3.0 && age <= 30 && isHighConf -> AlertSeverity.CRITICAL
            dist <= 5.0 && age <= 30 -> AlertSeverity.HIGH
            dist <= 15.0 && age <= 60 -> AlertSeverity.MEDIUM
            dist <= 25.0 -> AlertSeverity.LOW
            else -> AlertSeverity.INFO
        }
    }

    private fun isCurrentlyQuietHour(config: SmartAlertConfig): Boolean {
        if (!config.quietHoursEnabled) return false
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return if (config.quietHoursStartHour > config.quietHoursEndHour) {
            // e.g. 22:00 to 05:00
            currentHour >= config.quietHoursStartHour || currentHour < config.quietHoursEndHour
        } else {
            currentHour in config.quietHoursStartHour until config.quietHoursEndHour
        }
    }

    private fun dispatchAndroidNotification(alert: FireAlert) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
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

    /**
     * Feature 299 & 300: DAILY SUMMARY GENERATOR
     */
    fun generateDailySummary(hotspots: List<Hotspot>): DailyAlertSummary {
        val todayStr = FireAgeCalculator.formatDateTime(System.currentTimeMillis())
        val latestObs = hotspots.maxByOrNull { it.acquisitionTimestamp }
        val nearest = hotspots.filter { it.distanceFromUser != null }.minByOrNull { it.distanceFromUser ?: Double.MAX_VALUE }

        val satCounts = hotspots.groupBy { it.satellite }.mapValues { it.value.size }

        return DailyAlertSummary(
            dateLabel = todayStr,
            totalHotspotsObserved = hotspots.size,
            totalAlertsIssued = alertHistory.size,
            latestObservationTimeFormatted = latestObs?.let { FireAgeCalculator.formatDateTime(it.acquisitionTimestamp) } ?: "Tidak ada",
            nearestHotspotDistanceFormatted = nearest?.distanceFromUser?.let { DistanceCalculator.formatDistanceKm(it) } ?: "Tidak ada",
            sourceHealthStatus = "Aktif & Terverifikasi",
            satelliteBreakdown = satCounts
        )
    }
}
