package com.example.data.engine

import com.example.data.model.Hotspot
import com.example.data.repository.DistanceCalculator

enum class MatrixPriorityLevel(val label: String, val badgeColorHex: Long) {
    LOW("LOW", 0xFF0284C7),
    MEDIUM("MEDIUM", 0xFFF59E0B),
    HIGH("HIGH", 0xFFEA580C),
    CRITICAL("CRITICAL", 0xFFDC2626)
}

data class MatrixEvaluationResult(
    val level: MatrixPriorityLevel,
    val score: Int,
    val explanation: String,
    val criticalNotice: String? = null
)

/**
 * Feature 270–274: HOTSPOT RISK & MONITORING PRIORITIZATION MATRIX
 */
object HotspotPriorityMatrix {

    /**
     * Feature 270: HOTSPOT TERKINI TERDEKAT
     * Kriteria: age <= configuredFreshnessThreshold (default 30m), kemudian distance minimum.
     */
    fun findNearestCurrentHotspot(
        hotspots: List<Hotspot>,
        freshnessThresholdMinutes: Long = 30L
    ): Hotspot? {
        return hotspots
            .filter { it.satelliteAgeMinutes <= freshnessThresholdMinutes && it.distanceFromUser != null }
            .minByOrNull { it.distanceFromUser ?: Double.MAX_VALUE }
    }

    /**
     * Feature 271: HOTSPOT TERBARU TERDEKAT
     * Pisahkan dengan Hotspot Terkini Terdekat agar pengguna tidak salah paham.
     */
    fun findNearestRecentHotspot(
        hotspots: List<Hotspot>
    ): Hotspot? {
        if (hotspots.isEmpty()) return null
        val minAge = hotspots.minOf { it.satelliteAgeMinutes }
        return hotspots
            .filter { it.satelliteAgeMinutes == minAge && it.distanceFromUser != null }
            .minByOrNull { it.distanceFromUser ?: Double.MAX_VALUE }
            ?: hotspots.minByOrNull { it.distanceFromUser ?: Double.MAX_VALUE }
    }

    /**
     * Feature 272–274: Priority Matrix
     * Freshness + Distance + Confidence + Brightness
     */
    fun evaluate(
        ageMinutes: Long,
        distanceKm: Double?,
        confidence: String?,
        brightness: Double?
    ): MatrixEvaluationResult {
        var score = 0
        val reasons = mutableListOf<String>()

        // 1. Freshness weighting
        when {
            ageMinutes <= 30 -> {
                score += 40
                reasons.add("observasi $ageMinutes menit lalu (≤30m)")
            }
            ageMinutes <= 60 -> {
                score += 25
                reasons.add("observasi $ageMinutes menit lalu (≤1 jam)")
            }
            ageMinutes <= 180 -> {
                score += 15
                reasons.add("observasi ${ageMinutes / 60} jam lalu")
            }
            else -> {
                score += 5
                reasons.add("data lebih dari 3 jam lalu")
            }
        }

        // 2. Distance weighting
        if (distanceKm != null) {
            when {
                distanceKm <= 3.0 -> {
                    score += 35
                    reasons.add("jarak ${DistanceCalculator.formatDistanceKm(distanceKm)} (sangat dekat)")
                }
                distanceKm <= 10.0 -> {
                    score += 25
                    reasons.add("jarak ${DistanceCalculator.formatDistanceKm(distanceKm)} (lingkar pantau)")
                }
                distanceKm <= 25.0 -> {
                    score += 15
                    reasons.add("jarak ${DistanceCalculator.formatDistanceKm(distanceKm)}")
                }
                else -> {
                    score += 5
                    reasons.add("jarak >25 km")
                }
            }
        }

        // 3. Confidence weighting
        val confUpper = confidence?.uppercase() ?: "NOMINAL"
        when {
            confUpper.contains("HIGH") || confUpper.toIntOrNull()?.let { it >= 80 } == true -> {
                score += 15
                reasons.add("confidence tinggi ($confidence)")
            }
            confUpper.contains("NOMINAL") || confUpper.toIntOrNull()?.let { it in 50..79 } == true -> {
                score += 8
                reasons.add("confidence nominal ($confidence)")
            }
            else -> {
                score += 2
                reasons.add("confidence $confidence")
            }
        }

        // 4. Brightness weighting
        if (brightness != null && brightness >= 350.0) {
            score += 10
            reasons.add("brightness termal tinggi (${brightness}K)")
        }

        val level = when {
            score >= 80 -> MatrixPriorityLevel.CRITICAL
            score >= 60 -> MatrixPriorityLevel.HIGH
            score >= 40 -> MatrixPriorityLevel.MEDIUM
            else -> MatrixPriorityLevel.LOW
        }

        // Feature 274: CRITICAL CLAIM GUARD
        val criticalNotice = if (level == MatrixPriorityLevel.CRITICAL) {
            "Prioritas pemantauan tinggi berdasarkan data satelit."
        } else null

        val explanation = "Karena: " + reasons.joinToString(", ")

        return MatrixEvaluationResult(
            level = level,
            score = score,
            explanation = explanation,
            criticalNotice = criticalNotice
        )
    }
}
