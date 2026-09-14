package com.example.data.repository

import com.example.data.model.HotspotAgeStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object FireAgeCalculator {

    private val utcDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    private val localDisplayFormat = SimpleDateFormat("dd MMM yyyy HH:mm:ss 'WIB'", Locale("id", "ID")).apply {
        timeZone = TimeZone.getTimeZone("Asia/Jakarta")
    }

    private val timeOnlyFormat = SimpleDateFormat("HH:mm:ss 'WIB'", Locale("id", "ID")).apply {
        timeZone = TimeZone.getTimeZone("Asia/Jakarta")
    }

    /**
     * Parse NASA FIRMS acquisition date ("yyyy-MM-dd") and time ("HHmm" or "HH:mm") into UTC epoch millis.
     * FIRMS acq_time is always in UTC.
     */
    fun parseAcquisitionTimestamp(dateStr: String, timeStr: String): Long? {
        return try {
            val cleanDate = dateStr.trim()
            val cleanTime = timeStr.trim().replace(":", "")
            if (cleanDate.isEmpty() || cleanTime.isEmpty()) return null

            // format time to HH:mm
            val formattedTime = when (cleanTime.length) {
                1 -> "00:0$cleanTime"
                2 -> "00:$cleanTime"
                3 -> "0${cleanTime.substring(0, 1)}:${cleanTime.substring(1)}"
                4 -> "${cleanTime.substring(0, 2)}:${cleanTime.substring(2)}"
                else -> "${cleanTime.substring(0, 2)}:${cleanTime.substring(2, 4)}"
            }

            val combined = "$cleanDate $formattedTime"
            val parsedDate = utcDateFormat.parse(combined)
            parsedDate?.time
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Hitung umur satelit dalam menit: currentTimestamp - acquisitionTimestamp
     */
    fun calculateAgeMinutes(acquisitionTimestamp: Long?, currentTimestamp: Long = System.currentTimeMillis()): Long {
        if (acquisitionTimestamp == null || acquisitionTimestamp <= 0) return Long.MAX_VALUE
        val diffMillis = currentTimestamp - acquisitionTimestamp
        // If clock skew slightly in future (e.g. satellite observation is within 5 minutes of local clock), clamp to 0
        val minutes = diffMillis / (60 * 1000)
        return if (minutes < 0) 0 else minutes
    }

    /**
     * Tentukan status umur hotspot:
     * 0–30 menit: SANGAT TERKINI
     * 31–60 menit: TERKINI
     * 61–180 menit: TERBARU TETAPI TERLAMBAT
     * 181–360 menit: TERLAMBAT
     * >360 menit: HISTORIS
     */
    fun determineStatus(ageMinutes: Long): HotspotAgeStatus {
        return when {
            ageMinutes < 0 -> HotspotAgeStatus.UNKNOWN
            ageMinutes <= 30 -> HotspotAgeStatus.SANGAT_TERKINI
            ageMinutes <= 60 -> HotspotAgeStatus.TERKINI
            ageMinutes <= 180 -> HotspotAgeStatus.TERBARU_TETAPI_TERLAMBAT
            ageMinutes <= 360 -> HotspotAgeStatus.TERLAMBAT
            ageMinutes != Long.MAX_VALUE -> HotspotAgeStatus.HISTORIS
            else -> HotspotAgeStatus.UNKNOWN
        }
    }

    fun formatAgeDescription(ageMinutes: Long): String {
        if (ageMinutes == Long.MAX_VALUE || ageMinutes < 0) return "Waktu tidak diketahui"
        val hours = ageMinutes / 60
        val mins = ageMinutes % 60
        return when {
            hours == 0L -> "$mins menit lalu"
            mins == 0L -> "$hours jam lalu"
            else -> "$hours jam $mins menit lalu"
        }
    }

    fun formatDateTime(epochMillis: Long?): String {
        if (epochMillis == null || epochMillis <= 0) return "Tidak tersedia"
        return try {
            localDisplayFormat.format(Date(epochMillis))
        } catch (_: Exception) {
            "Tidak tersedia"
        }
    }

    fun formatDateTimeIso(epochMillis: Long?): String {
        if (epochMillis == null || epochMillis <= 0) return "Tidak tersedia"
        return try {
            val isoFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'UTC'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            isoFormat.format(Date(epochMillis))
        } catch (_: Exception) {
            "Tidak tersedia"
        }
    }

    fun formatTimeOnly(epochMillis: Long?): String {
        if (epochMillis == null || epochMillis <= 0) return "--:--:--"
        return try {
            timeOnlyFormat.format(Date(epochMillis))
        } catch (_: Exception) {
            "--:--:--"
        }
    }
}
