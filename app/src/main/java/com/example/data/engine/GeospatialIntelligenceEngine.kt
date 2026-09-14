package com.example.data.engine

import com.example.data.model.ChangeClassification
import com.example.data.model.Hotspot
import com.example.data.model.HotspotDensityInfo
import com.example.data.model.HotspotGroup
import com.example.data.model.TemporalChangeResult
import com.example.data.repository.DistanceCalculator
import java.util.Locale

/**
 * Feature 190–205: GEOSPATIAL INTELLIGENCE & TEMPORAL CHANGE ENGINE
 */
object GeospatialIntelligenceEngine {

    /**
     * Feature 193, 194, 195: HOTSPOT GROUPING
     * Mengelompokkan titik anomali termal dalam radius kedekatan (misal <= 1.5 km).
     * Rule 194: latestObservation = timestamp terbaru dari anggota kelompok (BUKAN firstSeen!).
     * Rule 195: groupAge dihitung dari latestObservation.
     */
    fun createHotspotGroups(
        hotspots: List<Hotspot>,
        clusterRadiusKm: Double = 1.5,
        currentTimestamp: Long = System.currentTimeMillis()
    ): List<HotspotGroup> {
        if (hotspots.isEmpty()) return emptyList()

        val unassigned = hotspots.toMutableList()
        val groups = mutableListOf<HotspotGroup>()
        var groupCounter = 1

        while (unassigned.isNotEmpty()) {
            val seed = unassigned.removeAt(0)
            val members = mutableListOf(seed)

            val iterator = unassigned.iterator()
            while (iterator.hasNext()) {
                val candidate = iterator.next()
                val dist = DistanceCalculator.calculateHaversineDistanceKm(
                    seed.latitude,
                    seed.longitude,
                    candidate.latitude,
                    candidate.longitude
                )
                if (dist <= clusterRadiusKm) {
                    members.add(candidate)
                    iterator.remove()
                }
            }

            val avgLat = members.map { it.latitude }.average()
            val avgLon = members.map { it.longitude }.average()

            // Hitung radius kelompok dalam meter dari centroid
            val maxDistMeters = members.maxOfOrNull {
                DistanceCalculator.calculateHaversineDistanceKm(avgLat, avgLon, it.latitude, it.longitude) * 1000.0
            } ?: 0.0

            // Feature 194: Waktu observasi TERBARU dari anggota kelompok
            val latestObsTimestamp = members.maxOf { it.acquisitionTimestamp }

            // Feature 195: Umur kelompok dihitung dari latestObsTimestamp
            val groupAgeMin = maxOf(0L, (currentTimestamp - latestObsTimestamp) / (60 * 1000))

            val satellites = members.map { it.satellite.trim() }.toSet()
            val isMultiSat = satellites.size > 1 // Feature 191 & 192

            val groupId = "GRP_${groupCounter++}"

            groups.add(
                HotspotGroup(
                    groupId = groupId,
                    hotspotCount = members.size,
                    centerLatitude = avgLat,
                    centerLongitude = avgLon,
                    radiusMeters = maxDistMeters,
                    latestObservationTimestamp = latestObsTimestamp,
                    groupAgeMinutes = groupAgeMin,
                    satellitesInvolved = satellites,
                    isMultiSatellite = isMultiSat,
                    memberHotspotIds = members.map { it.id }
                )
            )
        }

        return groups
    }

    /**
     * Feature 191 & 192: SATELLITE CROSS-CHECK & MULTI-SATELLITE DETECTION
     * Tandai hotspot jika ada satelit lain yang mendeteksi anomali pada radius dekat (< 2 km).
     */
    fun annotateMultiSatelliteObservations(hotspots: List<Hotspot>): List<Hotspot> {
        if (hotspots.size <= 1) return hotspots

        return hotspots.map { hs ->
            val hasCrossSatellite = hotspots.any { other ->
                other.id != hs.id &&
                        !other.satellite.equals(hs.satellite, ignoreCase = true) &&
                        DistanceCalculator.calculateHaversineDistanceKm(
                            hs.latitude, hs.longitude, other.latitude, other.longitude
                        ) <= 2.0
            }
            hs.copy(isMultiSatelliteObs = hasCrossSatellite)
        }
    }

    /**
     * Feature 196: HOTSPOT DENSITY (KONSENTRASI HOTSPOT)
     * Hitung hotspot density dalam radius tertentu.
     * Disclaimer ketat: density != luas kebakaran.
     */
    fun calculateDensity(
        hotspots: List<Hotspot>,
        centerLat: Double,
        centerLon: Double,
        radiusKm: Double = 5.0,
        previousCount: Int = 0
    ): HotspotDensityInfo {
        val countWithin = hotspots.count { hs ->
            DistanceCalculator.calculateHaversineDistanceKm(centerLat, centerLon, hs.latitude, hs.longitude) <= radiusKm
        }

        val level = when {
            countWithin >= 10 -> "TINGGI"
            countWithin >= 4 -> "SEDANG"
            countWithin >= 1 -> "RENDAH"
            else -> "NIHIL"
        }

        val delta = countWithin - previousCount

        return HotspotDensityInfo(
            countWithinRadius = countWithin,
            radiusKm = radiusKm,
            concentrationLevel = level,
            previousCount = previousCount,
            delta = delta,
            isIncreased = delta > 0 // Feature 197: Peningkatan jumlah hotspot
        )
    }

    /**
     * Feature 201 & 202: TEMPORAL CHANGE ENGINE
     * Klasifikasi perubahan antar update.
     * Rule 205: DILARANG menggunakan kata "API MEMBESAR" / "API MENJALAR".
     * Gunakan istilah netral: "JUMLAH ANOMALI TERMAL MENINGKAT".
     */
    fun analyzeTemporalChange(
        previousHotspots: List<Hotspot>,
        currentHotspots: List<Hotspot>,
        newHotspotsCount: Int,
        isSourceStalled: Boolean,
        latestObservationTimestamp: Long?
    ): TemporalChangeResult {
        val prevCount = previousHotspots.size
        val currCount = currentHotspots.size
        val delta = currCount - prevCount

        val classification = when {
            isSourceStalled -> ChangeClassification.DATA_SOURCE_STALLED
            newHotspotsCount > 0 && delta > 0 -> ChangeClassification.HOTSPOT_COUNT_INCREASED
            newHotspotsCount > 0 -> ChangeClassification.NEW_HOTSPOTS
            delta > 0 -> ChangeClassification.HOTSPOT_COUNT_INCREASED
            delta < 0 -> ChangeClassification.HOTSPOT_COUNT_DECREASED
            prevCount > 0 && currCount > 0 -> ChangeClassification.DATA_SOURCE_UPDATED
            else -> ChangeClassification.NO_SIGNIFICANT_CHANGE
        }

        val message = when (classification) {
            ChangeClassification.HOTSPOT_COUNT_INCREASED ->
                "⚠️ Peningkatan jumlah anomali termal: sebelumnya $prevCount titik, sekarang $currCount titik (selisih +$delta). Bukan konfirmasi api membesar."
            ChangeClassification.NEW_HOTSPOTS ->
                "🆕 Terdapat $newHotspotsCount record anomali termal baru pertama kali diterima aplikasi."
            ChangeClassification.HOTSPOT_COUNT_DECREASED ->
                "Jumlah anomali termal berkurang dari $prevCount menjadi $currCount titik. Bukan konfirmasi api padam."
            ChangeClassification.DATA_SOURCE_STALLED ->
                "⚠️ Sumber data satelit tidak memperbarui observasi (stalled)."
            ChangeClassification.DATA_SOURCE_UPDATED ->
                "Data satelit telah diperbarui. Jumlah anomali termal stabil ($currCount titik)."
            ChangeClassification.NO_SIGNIFICANT_CHANGE ->
                "Tidak ada perubahan signifikan pada dataset pemantauan."
        }

        return TemporalChangeResult(
            classification = classification,
            previousHotspotCount = prevCount,
            currentHotspotCount = currCount,
            countDifference = delta,
            latestObservationTimestamp = latestObservationTimestamp,
            summaryMessage = message
        )
    }
}
