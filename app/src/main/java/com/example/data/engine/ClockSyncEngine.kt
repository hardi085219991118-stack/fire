package com.example.data.engine

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Feature 105 & 106: CLOCK SYNCHRONIZATION & SERVER TIME OFFSET
 *
 * Mengukur sinkronisasi waktu antara jam perangkat lokal dengan jam server NASA (dari header HTTP Date).
 *
 * Contoh:
 * Device: 21:50:00
 * Server: 21:49:43
 * Offset: -17 detik
 *
 * Jika waktu perangkat berbeda signifikan (> 2 menit) dari server:
 * Tampilkan peringatan: ⚠️ WAKTU PERANGKAT TIDAK SINKRON
 *
 * ATURAN:
 * Gunakan offset untuk membantu perhitungan freshness jika jam perangkat melenceng,
 * tetapi JANGAN PERNAH mengubah acquisitionTimestamp asli dari satelit.
 */
data class ClockSyncResult(
    val serverTimestamp: Long?,
    val deviceResponseTime: Long,
    val serverTimeOffsetMs: Long,
    val isClockUnsynchronized: Boolean,
    val syncWarningMessage: String?
)

object ClockSyncEngine {

    // HTTP Date header format RFC 1123: "EEE, dd MMM yyyy HH:mm:ss z"
    private val httpDateFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("GMT")
    }

    /**
     * Parse header 'Date' dari HTTP response
     */
    fun parseServerDateHeader(headerValue: String?): Long? {
        if (headerValue.isNullOrBlank()) return null
        return try {
            httpDateFormat.parse(headerValue.trim())?.time
        } catch (_: Exception) {
            null
        }
    }

    fun evaluateClockSync(
        serverTimestamp: Long?,
        deviceResponseTime: Long,
        thresholdUnsyncMs: Long = 120_000L // 2 menit
    ): ClockSyncResult {
        if (serverTimestamp == null) {
            return ClockSyncResult(
                serverTimestamp = null,
                deviceResponseTime = deviceResponseTime,
                serverTimeOffsetMs = 0L,
                isClockUnsynchronized = false,
                syncWarningMessage = null
            )
        }

        val offsetMs = serverTimestamp - deviceResponseTime
        val absOffset = kotlin.math.abs(offsetMs)
        val isUnsync = absOffset > thresholdUnsyncMs

        val warningMsg = if (isUnsync) {
            val offsetSec = offsetMs / 1000
            "⚠️ WAKTU PERANGKAT TIDAK SINKRON (Selisih $offsetSec detik dari server waktu NASA)"
        } else {
            null
        }

        return ClockSyncResult(
            serverTimestamp = serverTimestamp,
            deviceResponseTime = deviceResponseTime,
            serverTimeOffsetMs = offsetMs,
            isClockUnsynchronized = isUnsync,
            syncWarningMessage = warningMsg
        )
    }
}
