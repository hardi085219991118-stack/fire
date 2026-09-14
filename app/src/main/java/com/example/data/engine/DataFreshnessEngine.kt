package com.example.data.engine

import com.example.data.model.FreshnessConfig
import com.example.data.model.HotspotAgeStatus

/**
 * Feature 103 & 104: DATA FRESHNESS ENGINE & SECOND-LEVEL AGE
 *
 * Menghitung umur observasi data satelit dengan presisi tingkat detik.
 * Input:
 * - currentTimestamp (waktu sekarang)
 * - acquisitionTimestamp (waktu observasi satelit)
 * - serverTimeOffsetMs (selisih jam server vs perangkat)
 *
 * Output:
 * - dataAgeSeconds
 * - dataAgeMinutes
 * - freshnessStatus
 * - formattedAgeFull ("25 menit 17 detik", "42 detik", "2 jam 14 menit 31 detik")
 * - formattedAgeShort ("25m 17s", "42s", "2j 14m")
 */
data class DataFreshnessResult(
    val dataAgeSeconds: Long,
    val dataAgeMinutes: Long,
    val freshnessStatus: HotspotAgeStatus,
    val formattedAgeFull: String,
    val formattedAgeShort: String
)

object DataFreshnessEngine {

    fun calculateFreshness(
        acquisitionTimestamp: Long?,
        currentTimestamp: Long = System.currentTimeMillis(),
        serverTimeOffsetMs: Long = 0L,
        config: FreshnessConfig = FreshnessConfig.DEFAULT
    ): DataFreshnessResult {
        if (acquisitionTimestamp == null || acquisitionTimestamp <= 0) {
            return DataFreshnessResult(
                dataAgeSeconds = Long.MAX_VALUE,
                dataAgeMinutes = Long.MAX_VALUE,
                freshnessStatus = HotspotAgeStatus.UNKNOWN,
                formattedAgeFull = "Waktu tidak diketahui",
                formattedAgeShort = "Tidak tersedia"
            )
        }

        // Adjust current timestamp with server offset if calibrated
        val adjustedCurrentTime = currentTimestamp + serverTimeOffsetMs
        val diffMillis = adjustedCurrentTime - acquisitionTimestamp
        // Clamp skew slightly (if satellite data is a few seconds ahead due to slight time sync)
        val diffSec = if (diffMillis < 0) 0L else diffMillis / 1000
        val diffMin = diffSec / 60

        val status = determineStatus(diffMin, config)
        val formattedFull = formatSecondLevelAgeFull(diffSec)
        val formattedShort = formatSecondLevelAgeShort(diffSec)

        return DataFreshnessResult(
            dataAgeSeconds = diffSec,
            dataAgeMinutes = diffMin,
            freshnessStatus = status,
            formattedAgeFull = formattedFull,
            formattedAgeShort = formattedShort
        )
    }

    fun determineStatus(ageMinutes: Long, config: FreshnessConfig = FreshnessConfig.DEFAULT): HotspotAgeStatus {
        return when {
            ageMinutes < 0 -> HotspotAgeStatus.UNKNOWN
            ageMinutes <= config.veryFreshMinutes -> HotspotAgeStatus.SANGAT_TERKINI
            ageMinutes <= config.freshMinutes -> HotspotAgeStatus.TERKINI
            ageMinutes <= config.delayedMinutes -> HotspotAgeStatus.TERBARU_TETAPI_TERLAMBAT
            ageMinutes <= config.oldMinutes -> HotspotAgeStatus.TERLAMBAT
            ageMinutes != Long.MAX_VALUE -> HotspotAgeStatus.HISTORIS
            else -> HotspotAgeStatus.UNKNOWN
        }
    }

    /**
     * Format presisi detik lengkap sesuai spesifikasi Fitur 104:
     * Contoh:
     * < 1 menit: "42 detik"
     * 1 - 59 menit: "25 menit 17 detik"
     * > 1 jam: "2 jam 14 menit 31 detik"
     */
    fun formatSecondLevelAgeFull(ageSeconds: Long): String {
        if (ageSeconds == Long.MAX_VALUE || ageSeconds < 0) return "Waktu tidak diketahui"
        val hours = ageSeconds / 3600
        val remainingSec = ageSeconds % 3600
        val minutes = remainingSec / 60
        val seconds = remainingSec % 60

        return when {
            hours > 0 -> {
                if (seconds > 0) {
                    "$hours jam $minutes menit $seconds detik lalu"
                } else {
                    "$hours jam $minutes menit lalu"
                }
            }
            minutes > 0 -> {
                if (seconds > 0) {
                    "$minutes menit $seconds detik lalu"
                } else {
                    "$minutes menit lalu"
                }
            }
            else -> "$seconds detik lalu"
        }
    }

    /**
     * Format ringkas untuk badge / baris tabel
     */
    fun formatSecondLevelAgeShort(ageSeconds: Long): String {
        if (ageSeconds == Long.MAX_VALUE || ageSeconds < 0) return "--"
        val hours = ageSeconds / 3600
        val remainingSec = ageSeconds % 3600
        val minutes = remainingSec / 60
        val seconds = remainingSec % 60

        return when {
            hours > 0 -> "${hours}j ${minutes}m"
            minutes > 0 -> "${minutes}m ${seconds}s"
            else -> "${seconds}s"
        }
    }
}
