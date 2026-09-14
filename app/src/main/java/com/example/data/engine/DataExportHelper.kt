package com.example.data.engine

import com.example.data.model.DataAuditLog
import com.example.data.model.FireAlert
import com.example.data.model.Hotspot
import com.example.data.model.IncidentEntity
import com.example.data.repository.DistanceCalculator
import com.example.data.repository.FireAgeCalculator
import java.util.Locale

/**
 * Feature 319–328: REPORT GENERATOR & DATA EXPORT ENGINE
 */
object DataExportHelper {

    /**
     * Feature 319–322: LAPORAN PEMANTAUAN RESMI
     */
    fun generateLaporanPemantauan(
        areaLabel: String,
        hotspots: List<Hotspot>,
        latestObservationTimestamp: Long?,
        lastReceivedTimestamp: Long?,
        latencyFormatted: String,
        alerts: List<FireAlert>,
        incidents: List<IncidentEntity> = emptyList()
    ): String {
        val nowFormatted = FireAgeCalculator.formatDateTime(System.currentTimeMillis())
        val latestObsFormatted = latestObservationTimestamp?.let { FireAgeCalculator.formatDateTime(it) } ?: "Tidak ada data"
        val lastRecvFormatted = lastReceivedTimestamp?.let { FireAgeCalculator.formatDateTime(it) } ?: "Tidak ada data"
        val count = hotspots.size
        val countUnder30 = hotspots.count { it.satelliteAgeMinutes <= 30 }
        val countUnder60 = hotspots.count { it.satelliteAgeMinutes in 31..60 }
        val countOlder = hotspots.count { it.satelliteAgeMinutes > 60 }

        val nearest = hotspots.filter { it.distanceFromUser != null }.minByOrNull { it.distanceFromUser ?: Double.MAX_VALUE }
        val nearestDistanceStr = nearest?.distanceFromUser?.let { DistanceCalculator.formatDistanceKm(it) } ?: "Tidak tersedia"

        return buildString {
            append("====================================================\n")
            append("      LAPORAN PEMANTAUAN ANOMALI TERMAL SATELIT    \n")
            append("             HARDI MANTANGAI FIRE NOW               \n")
            append("====================================================\n\n")

            append("1. INFORMASI LAPORAN:\n")
            append("• Waktu Pembuatan: $nowFormatted\n")
            append("• Area / Titik Acuan: $areaLabel\n")
            append("• Sumber Satelit: NASA FIRMS (VIIRS / MODIS)\n\n")

            append("2. STATUS OBSERVASI SATELIT TERBARU:\n")
            append("• Waktu Observasi Satelit: $latestObsFormatted\n")
            append("• Waktu Data Tiba di Sistem: $lastRecvFormatted\n")
            append("• Latensi Transmisi Satelit: $latencyFormatted\n")
            append("• Hotspot Terdekat dari Acuan: $nearestDistanceStr\n\n")

            append("3. REKAPITULASI HOTSPOT AKTIF:\n")
            append("• Total Hotspot Teramati: $count titik\n")
            append("  - Sangat Terkini (≤30 menit): $countUnder30 titik\n")
            append("  - Terkini (31–60 menit): $countUnder60 titik\n")
            append("  - Terlambat / Historis (>60 menit): $countOlder titik\n\n")

            append("4. PERINGATAN SISTEM (ALERTS):\n")
            append("• Total Peringatan Diterbitkan: ${alerts.size}\n")
            alerts.take(5).forEach { a ->
                append("  - [${a.severity.label}] ${a.title} | Jarak: ${a.distanceKm?.let { DistanceCalculator.formatDistanceKm(it) } ?: "--"} | Obs: ${FireAgeCalculator.formatTimeOnly(a.observationTimestamp)}\n")
            }
            append("\n")

            if (incidents.isNotEmpty()) {
                append("5. CATATAN PEMANTAUAN / VERIFIKASI LAPANGAN:\n")
                incidents.forEach { inc ->
                    append("  - [${inc.status.label}] ${inc.title} (${inc.fieldVerificationStatus.label})\n")
                    if (inc.fieldNotes.isNotBlank()) {
                        append("    Catatan Petugas: ${inc.fieldNotes}\n")
                    }
                }
                append("\n")
            }

            append("----------------------------------------------------\n")
            // Feature 321: Time Disclaimer
            append("CATATAN WAKTU:\n")
            append("Data hotspot berdasarkan waktu observasi satelit yang tersedia.\n\n")

            // Feature 322: Safety Disclaimer
            append("DISCLAIMER KESELAMATAN:\n")
            append("Hotspot satelit merupakan indikasi anomali termal dan bukan konfirmasi otomatis kebakaran di lapangan. Diperlukan verifikasi faktual oleh tim darat.\n")
            append("====================================================\n")
        }
    }

    /**
     * Feature 323: EXPORT CSV HOTSPOTS
     */
    fun exportHotspotsToCsv(
        hotspots: List<Hotspot>,
        dataReceivedTimestamp: Long?
    ): String {
        return buildString {
            append("latitude,longitude,acquisitionTimestamp,observationTimeUtc,dataReceivedTimestamp,ageMinutes,satellite,instrument,confidence,brightness,distanceKm,source,status\n")
            for (h in hotspots) {
                val obsTimeIso = FireAgeCalculator.formatDateTimeIso(h.acquisitionTimestamp)
                val distStr = h.distanceFromUser?.let { String.format(Locale.US, "%.2f", it) } ?: ""
                val brightStr = h.brightness?.let { String.format(Locale.US, "%.1f", it) } ?: ""
                append("${h.latitude},${h.longitude},${h.acquisitionTimestamp},\"$obsTimeIso\",${dataReceivedTimestamp ?: ""},${h.satelliteAgeMinutes},\"${h.satellite}\",\"${h.instrument}\",\"${h.confidence ?: "Nominal"}\",$brightStr,$distStr,\"${h.source}\",\"${h.status.label}\"\n")
            }
        }
    }

    /**
     * Feature 324: EXPORT JSON HOTSPOTS (Teknis tanpa API keys)
     */
    fun exportHotspotsToJson(
        hotspots: List<Hotspot>,
        dataReceivedTimestamp: Long?
    ): String {
        val items = hotspots.map { h ->
            """  {
    "latitude": ${h.latitude},
    "longitude": ${h.longitude},
    "acquisitionTimestamp": ${h.acquisitionTimestamp},
    "observationTimeUtc": "${FireAgeCalculator.formatDateTimeIso(h.acquisitionTimestamp)}",
    "dataReceivedTimestamp": ${dataReceivedTimestamp ?: "null"},
    "ageMinutes": ${h.satelliteAgeMinutes},
    "satellite": "${h.satellite}",
    "instrument": "${h.instrument}",
    "confidence": "${h.confidence ?: "Nominal"}",
    "brightness": ${h.brightness ?: "null"},
    "distanceKm": ${h.distanceFromUser ?: "null"},
    "source": "${h.source}",
    "status": "${h.status.name}"
  }"""
        }
        return "[\n" + items.joinToString(",\n") + "\n]"
    }

    /**
     * Feature 325: EXPORT DATA AUDIT LOGS CSV
     */
    fun exportAuditLogsToCsv(logs: List<DataAuditLog>): String {
        return buildString {
            append("timestamp,timeLocal,requestUrlSanitized,responseCode,totalRecords,rejectedRecords,latencyMs,newestObsTimestamp,source,failureReason\n")
            for (l in logs) {
                val cleanUrl = ErrorObservability.sanitizeMessage(l.endpointUrl)
                val timeStr = FireAgeCalculator.formatDateTime(l.requestTime)
                val latency = l.responseTime - l.requestTime
                append("${l.requestTime},\"$timeStr\",\"$cleanUrl\",${l.httpStatusCode ?: 0},${l.numberReceived},${l.numberRejected},$latency,${l.latestAcquisitionTimestamp ?: ""},\"${l.source}\",\"${l.rejectReason ?: l.errorMessage ?: ""}\"\n")
            }
        }
    }

    /**
     * Feature 326: EXPORT INCIDENTS CSV
     */
    fun exportIncidentsToCsv(incidents: List<IncidentEntity>): String {
        return buildString {
            append("id,title,status,createdAt,updatedAt,latitude,longitude,linkedHotspotFingerprint,verificationStatus,fieldNotes,fieldObserver,fieldObservationTime,distanceMeters\n")
            for (inc in incidents) {
                append("\"${inc.id}\",\"${inc.title}\",\"${inc.status.name}\",${inc.createdAt},${inc.updatedAt},${inc.latitude},${inc.longitude},\"${inc.linkedHotspotFingerprint ?: ""}\",\"${inc.fieldVerificationStatus.name}\",\"${inc.fieldNotes.replace("\"", "'")}\",\"${inc.fieldObserverName}\",${inc.fieldObservationTimestamp ?: ""},${inc.distanceSatelliteToFieldMeters ?: ""}\n")
            }
        }
    }
}
