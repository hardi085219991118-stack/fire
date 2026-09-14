package com.example.data.engine

import com.example.data.model.DataQualityReport
import com.example.data.model.Hotspot
import com.example.data.model.QualityScoreLevel
import com.example.data.model.SourceComparisonReport

data class GateResult(
    val isPassed: Boolean,
    val reason: String,
    val dataAgeMinutes: Long?
)

/**
 * Feature 249 & 250: CURRENT DATA GATE
 * Sebuah hotspot hanya boleh masuk FIRE NOW jika:
 * - timestamp valid
 * - source valid
 * - coordinate valid
 * - age <= threshold (default 30 menit)
 *
 * TEST INVARIANT (Feature 250):
 * - 29 menit: PASS
 * - 30 menit: PASS
 * - 31 menit: FAIL
 * - 3 jam (180m): FAIL
 * - 8 jam (480m): FAIL
 */
object CurrentDataGate {

    fun evaluateHotspot(
        hotspot: Hotspot,
        maxThresholdMinutes: Long = 30L,
        currentTimestamp: Long = System.currentTimeMillis()
    ): GateResult {
        // 1. Coordinate check
        if (hotspot.latitude < -90.0 || hotspot.latitude > 90.0 ||
            hotspot.longitude < -180.0 || hotspot.longitude > 180.0
        ) {
            return GateResult(
                isPassed = false,
                reason = "FAIL: Koordinat tidak valid (${hotspot.latitude}, ${hotspot.longitude})",
                dataAgeMinutes = hotspot.satelliteAgeMinutes
            )
        }

        // 2. Timestamp check
        if (hotspot.acquisitionTimestamp <= 0L || hotspot.acquisitionTimestamp > (currentTimestamp + 5 * 60 * 1000L)) {
            return GateResult(
                isPassed = false,
                reason = "FAIL: Timestamp observasi tidak valid atau masa depan",
                dataAgeMinutes = hotspot.satelliteAgeMinutes
            )
        }

        // 3. Source check
        if (hotspot.source.isBlank() || hotspot.satellite.isBlank()) {
            return GateResult(
                isPassed = false,
                reason = "FAIL: Metadata sumber atau satelit kosong",
                dataAgeMinutes = hotspot.satelliteAgeMinutes
            )
        }

        // 4. Age gate (<= maxThresholdMinutes)
        val ageMin = hotspot.satelliteAgeMinutes
        if (ageMin > maxThresholdMinutes) {
            return GateResult(
                isPassed = false,
                reason = "FAIL: Umur observasi satelit ($ageMin menit) melebihi batas FIRE NOW ($maxThresholdMinutes menit)",
                dataAgeMinutes = ageMin
            )
        }

        return GateResult(
            isPassed = true,
            reason = "PASS: Memenuhi seluruh kriteria FIRE NOW (umur $ageMin menit <= $maxThresholdMinutes menit)",
            dataAgeMinutes = ageMin
        )
    }

    /**
     * Direct test function for age gate verification
     */
    fun isAgeAllowedInFireNow(ageMinutes: Long, maxThresholdMinutes: Long = 30L): Boolean {
        return ageMinutes in 0..maxThresholdMinutes
    }

    /**
     * Feature 257 & 258: DATA QUALITY SCORE
     * Menilai kualitas dataset secara objektif (BUKAN kepastian kebakaran!)
     */
    fun evaluateDataQuality(
        hotspots: List<Hotspot>,
        latestObsAgeMinutes: Long?,
        isFromCache: Boolean,
        hasNetworkError: Boolean
    ): DataQualityReport {
        val reasons = mutableListOf<String>()
        var scorePoints = 100

        if (hotspots.isEmpty()) {
            reasons.add("Tidak ada data titik api pada kriteria")
            return DataQualityReport(
                level = QualityScoreLevel.LOW,
                scorePercent = 0,
                isTimestampValid = true,
                isCoordinateValid = true,
                isSourceValid = true,
                freshnessMinutes = latestObsAgeMinutes,
                hasConfidence = false,
                explanationReasons = listOf("Dataset kosong pada filter yang dipilih")
            )
        }

        val allCoordsValid = hotspots.all { it.latitude in -90.0..90.0 && it.longitude in -180.0..180.0 }
        if (allCoordsValid) {
            reasons.add("✓ Seluruh koordinat geografis valid")
        } else {
            scorePoints -= 30
            reasons.add("✗ Terdapat anomali koordinat di luar jangkauan")
        }

        val allTimestampsValid = hotspots.all { it.acquisitionTimestamp > 0L }
        if (allTimestampsValid) {
            reasons.add("✓ Seluruh timestamp observasi satelit valid")
        } else {
            scorePoints -= 30
            reasons.add("✗ Terdapat timestamp bernilai nol atau korup")
        }

        val allSourcesValid = hotspots.all { it.satellite.isNotBlank() && it.source.isNotBlank() }
        if (allSourcesValid) {
            reasons.add("✓ Provenance sumber dan satelit lengkap")
        } else {
            scorePoints -= 20
            reasons.add("✗ Beberapa data tidak memiliki informasi satelit/instrumen")
        }

        val hasConfidence = hotspots.any { !it.confidence.isNullOrBlank() }
        if (hasConfidence) {
            reasons.add("✓ Nilai confidence/probabilitas algoritma satelit tersedia")
        } else {
            scorePoints -= 10
            reasons.add("• Nilai confidence satelit tidak disertakan")
        }

        if (latestObsAgeMinutes != null) {
            if (latestObsAgeMinutes <= 30) {
                reasons.add("✓ Umur data sangat terkini ($latestObsAgeMinutes menit)")
            } else if (latestObsAgeMinutes <= 180) {
                scorePoints -= 10
                reasons.add("• Umur data moderat ($latestObsAgeMinutes menit)")
            } else {
                scorePoints -= 25
                reasons.add("• Umur data historis ($latestObsAgeMinutes menit)")
            }
        }

        if (isFromCache) {
            scorePoints -= 15
            reasons.add("⚠️ Menggunakan salinan cache lokal")
        }

        if (hasNetworkError) {
            scorePoints -= 20
            reasons.add("⚠️ Terjadi gangguan konektivitas saat pembaruan")
        }

        val finalScore = scorePoints.coerceIn(0, 100)
        val level = when {
            finalScore >= 80 -> QualityScoreLevel.HIGH
            finalScore >= 50 -> QualityScoreLevel.MEDIUM
            else -> QualityScoreLevel.LOW
        }

        return DataQualityReport(
            level = level,
            scorePercent = finalScore,
            isTimestampValid = allTimestampsValid,
            isCoordinateValid = allCoordsValid,
            isSourceValid = allSourcesValid,
            freshnessMinutes = latestObsAgeMinutes,
            hasConfidence = hasConfidence,
            explanationReasons = reasons
        )
    }

    /**
     * Feature 252–255: SOURCE COMPARISON & FAILOVER EVALUATION
     */
    fun evaluateSourceComparison(
        primarySource: String,
        alternativeSource: String?,
        isUsingAlternative: Boolean,
        primaryLatency: Long?,
        altLatency: Long?
    ): SourceComparisonReport {
        val discrepancy = if (primaryLatency != null && altLatency != null) {
            Math.abs(primaryLatency - altLatency) > 60L // > 1 hour discrepancy
        } else false

        val failoverWarning = if (isUsingAlternative && altLatency != null && primaryLatency != null && altLatency > primaryLatency) {
            "⚠️ SUMBER ALTERNATIF DIGUNAKAN: Latensi sumber alternatif lebih tinggi (+${altLatency - primaryLatency} menit)."
        } else if (isUsingAlternative) {
            "⚠️ MENGGUNAKAN SUMBER ALTERNATIF ($alternativeSource)"
        } else null

        return SourceComparisonReport(
            primarySource = primarySource,
            alternativeSource = alternativeSource,
            isUsingAlternative = isUsingAlternative,
            primaryLatencyMinutes = primaryLatency,
            alternativeLatencyMinutes = altLatency,
            latencyDifferenceMinutes = if (primaryLatency != null && altLatency != null) altLatency - primaryLatency else null,
            discrepancyDetected = discrepancy,
            discrepancyDetails = if (discrepancy) "Terdapat perbedaan latensi signifikan antar sumber data" else null,
            failoverWarningMessage = failoverWarning
        )
    }
}
