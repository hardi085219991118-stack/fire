package com.example.data.engine

import com.example.data.model.Hotspot

/**
 * Feature 148, 149 & 150:
 * - DATA STALL DETECTION (148)
 * - SOURCE DELAY ALERT (149)
 * - DATA GAP DETECTION (150)
 */
data class DataHealthAuditResult(
    val isDataStalled: Boolean,
    val stallConsecutiveCount: Int,
    val stallMessage: String?,
    val isDeliveryLatencyHigh: Boolean,
    val deliveryLatencyAlertMessage: String?,
    val isDataGapDetected: Boolean,
    val dataGapMessage: String?
)

object DataStallDetector {

    /**
     * Feature 148: Deteksi apakah sumber data belum memperbarui observasi (stalled)
     * setelah minimal 3 refresh berturut-turut dengan acquisition timestamp yang identik.
     */
    fun checkDataStall(
        recentLatestTimestamps: List<Long?>
    ): Pair<Boolean, String?> {
        // Ambil 3 timestamp terakhir yang valid
        val validRecent = recentLatestTimestamps.filterNotNull().takeLast(3)
        if (validRecent.size < 3) return Pair(false, null)

        val first = validRecent[0]
        val allSame = validRecent.all { it == first }

        return if (allSame) {
            Pair(
                true,
                "⚠️ DATA SOURCE STALLED: Sumber data belum memberikan observasi yang lebih baru setelah ${validRecent.size} kali pengecekan."
            )
        } else {
            Pair(false, null)
        }
    }

    /**
     * Feature 149: Deteksi apakah latensi pengiriman satelit melonjak drastis (> 30 menit)
     */
    fun checkDeliveryLatencySpike(deliveryLatencyMinutes: Long?): Pair<Boolean, String?> {
        if (deliveryLatencyMinutes == null) return Pair(false, null)
        return if (deliveryLatencyMinutes >= 30) {
            Pair(
                true,
                "⚠️ LATENCY SUMBER MENINGKAT: Latensi pengiriman satelit mencapai $deliveryLatencyMinutes menit."
            )
        } else {
            Pair(false, null)
        }
    }

    /**
     * Feature 150: Deteksi gap besar pada timestamp observasi satelit dalam satu dataset
     * Contoh: observasi 19:25 lalu berikutnya loncat ke 22:40 (selisih > 2 jam).
     * ATURAN: JANGAN mengisi gap dengan data buatan.
     */
    fun detectDataGaps(hotspots: List<Hotspot>, gapThresholdMinutes: Long = 120L): Pair<Boolean, String?> {
        if (hotspots.size < 2) return Pair(false, null)

        val sorted = hotspots.map { it.acquisitionTimestamp }.filter { it > 0 }.distinct().sorted()
        if (sorted.size < 2) return Pair(false, null)

        var maxGapMinutes = 0L
        for (i in 0 until sorted.size - 1) {
            val gapMin = (sorted[i + 1] - sorted[i]) / (60 * 1000)
            if (gapMin > maxGapMinutes) {
                maxGapMinutes = gapMin
            }
        }

        return if (maxGapMinutes >= gapThresholdMinutes) {
            Pair(
                true,
                "⚠️ DATA GAP TERDETEKSI: Ditemukan jeda waktu observasi sebesar ${maxGapMinutes / 60} jam ${maxGapMinutes % 60} menit dalam dataset."
            )
        } else {
            Pair(false, null)
        }
    }
}
