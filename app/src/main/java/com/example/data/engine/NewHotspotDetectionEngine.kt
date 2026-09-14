package com.example.data.engine

import com.example.data.model.Hotspot
import com.example.data.model.HotspotChangeHistory
import com.example.data.model.HotspotEvent
import com.example.data.model.HotspotEventType
import com.example.data.model.HotspotObservationRecord
import com.example.data.repository.DistanceCalculator
import java.util.Locale

data class HotspotDetectionResult(
    val processedHotspots: List<Hotspot>,
    val newEvents: List<HotspotEvent>,
    val newHotspotsCount: Int,
    val updatedHotspotsCount: Int,
    val removedHotspotsCount: Int
)

/**
 * Feature 181: NEW HOTSPOT DETECTION ENGINE
 * Feature 182: HOTSPOT FIRST-SEEN (firstSeenByApp BUKAN waktu kebakaran!)
 * Feature 183: SATELLITE OBSERVATION TIME (tetap gunakan acquisitionTimestamp asli)
 * Feature 184: NEW DATA EVENT (NEW_HOTSPOT_DATA)
 * Feature 185: CHANGED HOTSPOT EVENT (HOTSPOT_DATA_UPDATED)
 * Feature 186: REMOVED HOTSPOT EVENT (JANGAN simpulkan api padam!)
 * Feature 187: NEW HOTSPOT BADGE ("🆕 DATA BARU MASUK", bukan "🔥 API BARU TERJADI")
 * Feature 188: HOTSPOT CHANGE HISTORY
 * Feature 189: HOTSPOT IDENTITY FINGERPRINT
 * Feature 199 & 200: HOTSPOT PERSISTENCE & TIMELINE
 * Feature 237 & 238: THERMAL DETECTION SHIFT ("pergeseran lokasi anomali termal", bukan "api bergerak")
 */
object NewHotspotDetectionEngine {

    /**
     * Feature 189: Konsisten Fingerprint
     * Menggunakan lat (4 desimal ~11m), lon (4 desimal), acqTimestamp, satellite, instrument.
     */
    fun generateFingerprint(
        latitude: Double,
        longitude: Double,
        acquisitionTimestamp: Long,
        satellite: String,
        instrument: String
    ): String {
        return String.format(
            Locale.US,
            "FP_%.4f_%.4f_%d_%s_%s",
            latitude,
            longitude,
            acquisitionTimestamp,
            satellite.trim().uppercase(),
            instrument.trim().uppercase()
        )
    }

    /**
     * Feature 189: Location-based persistent key for tracking across multiple observation passes
     */
    fun generateLocationClusterKey(latitude: Double, longitude: Double): String {
        return String.format(Locale.US, "LOC_%.3f_%.3f", latitude, longitude)
    }

    /**
     * Proses perbandingan dataset baru dengan riwayat
     */
    fun processHotspots(
        incomingHotspots: List<Hotspot>,
        existingHistory: MutableMap<String, HotspotChangeHistory>,
        currentReceivedTime: Long = System.currentTimeMillis()
    ): HotspotDetectionResult {
        val events = mutableListOf<HotspotEvent>()
        val processedList = mutableListOf<Hotspot>()
        val seenFingerprintsInThisBatch = mutableSetOf<String>()

        var newCount = 0
        var updatedCount = 0

        for (hs in incomingHotspots) {
            val fingerprint = generateFingerprint(
                latitude = hs.latitude,
                longitude = hs.longitude,
                acquisitionTimestamp = hs.acquisitionTimestamp,
                satellite = hs.satellite,
                instrument = hs.instrument
            )
            seenFingerprintsInThisBatch.add(fingerprint)

            val existing = existingHistory[fingerprint]

            if (existing == null) {
                // Feature 181 & 182: Record baru belum pernah dilihat aplikasi
                newCount++
                val firstSeen = currentReceivedTime

                val obsRecord = HotspotObservationRecord(
                    observationIndex = 1,
                    acquisitionTimestamp = hs.acquisitionTimestamp,
                    receivedAt = currentReceivedTime,
                    latitude = hs.latitude,
                    longitude = hs.longitude,
                    satellite = hs.satellite,
                    instrument = hs.instrument,
                    confidence = hs.confidence,
                    brightness = hs.brightness,
                    distanceShiftMeters = 0.0
                )

                val newHistory = HotspotChangeHistory(
                    fingerprint = fingerprint,
                    firstSeenByApp = firstSeen,
                    lastSeenByApp = currentReceivedTime,
                    latestObservationTime = hs.acquisitionTimestamp,
                    observationCount = 1,
                    observations = listOf(obsRecord),
                    isPersistent = false,
                    totalLocationShiftMeters = 0.0,
                    lastConfidence = hs.confidence,
                    lastBrightness = hs.brightness
                )
                existingHistory[fingerprint] = newHistory

                // Feature 184: NEW_HOTSPOT_DATA EVENT
                events.add(
                    HotspotEvent(
                        eventId = "EVT_NEW_${System.nanoTime()}",
                        type = HotspotEventType.NEW_HOTSPOT_DATA,
                        hotspotFingerprint = fingerprint,
                        acquisitionTimestamp = hs.acquisitionTimestamp,
                        receivedAt = currentReceivedTime,
                        latitude = hs.latitude,
                        longitude = hs.longitude,
                        satellite = hs.satellite,
                        confidence = hs.confidence,
                        brightness = hs.brightness,
                        explanation = "Data anomali termal baru pertama kali diterima aplikasi dari satelit ${hs.satellite}."
                    )
                )

                processedList.add(
                    hs.copy(
                        fingerprint = fingerprint,
                        firstSeenByApp = firstSeen,
                        isNewData = true, // Feature 187: Badge 🆕 DATA BARU MASUK
                        observationCount = 1,
                        locationShiftMeters = 0.0
                    )
                )
            } else {
                // Hotspot sudah ada di history
                val hasMetadataChange = (existing.lastConfidence != hs.confidence) ||
                        (existing.lastBrightness != hs.brightness)

                val isNewObservation = hs.acquisitionTimestamp > existing.latestObservationTime

                var shiftMeters = 0.0
                val lastObs = existing.observations.lastOrNull()
                if (lastObs != null) {
                    val distKm = DistanceCalculator.calculateHaversineDistanceKm(
                        lastObs.latitude,
                        lastObs.longitude,
                        hs.latitude,
                        hs.longitude
                    )
                    shiftMeters = distKm * 1000.0 // meters
                }

                val updatedObservations = existing.observations.toMutableList()
                if (isNewObservation) {
                    updatedObservations.add(
                        HotspotObservationRecord(
                            observationIndex = existing.observationCount + 1,
                            acquisitionTimestamp = hs.acquisitionTimestamp,
                            receivedAt = currentReceivedTime,
                            latitude = hs.latitude,
                            longitude = hs.longitude,
                            satellite = hs.satellite,
                            instrument = hs.instrument,
                            confidence = hs.confidence,
                            brightness = hs.brightness,
                            distanceShiftMeters = shiftMeters
                        )
                    )
                }

                val updatedHistory = existing.copy(
                    lastSeenByApp = currentReceivedTime,
                    latestObservationTime = maxOf(existing.latestObservationTime, hs.acquisitionTimestamp),
                    observationCount = if (isNewObservation) existing.observationCount + 1 else existing.observationCount,
                    observations = updatedObservations,
                    isPersistent = (existing.observationCount + (if (isNewObservation) 1 else 0)) > 1,
                    totalLocationShiftMeters = existing.totalLocationShiftMeters + shiftMeters,
                    lastConfidence = hs.confidence,
                    lastBrightness = hs.brightness
                )
                existingHistory[fingerprint] = updatedHistory

                if (hasMetadataChange || isNewObservation) {
                    updatedCount++
                    // Feature 185: HOTSPOT_DATA_UPDATED
                    events.add(
                        HotspotEvent(
                            eventId = "EVT_UPD_${System.nanoTime()}",
                            type = HotspotEventType.HOTSPOT_DATA_UPDATED,
                            hotspotFingerprint = fingerprint,
                            acquisitionTimestamp = hs.acquisitionTimestamp,
                            receivedAt = currentReceivedTime,
                            latitude = hs.latitude,
                            longitude = hs.longitude,
                            satellite = hs.satellite,
                            confidence = hs.confidence,
                            brightness = hs.brightness,
                            explanation = if (isNewObservation) {
                                "Pembaruan observasi satelit terbaru (${hs.satellite})."
                            } else {
                                "Pembaruan metadata confidence/brightness dari sumber."
                            }
                        )
                    )
                }

                processedList.add(
                    hs.copy(
                        fingerprint = fingerprint,
                        firstSeenByApp = existing.firstSeenByApp,
                        isNewData = false, // Bukan data baru masuk
                        observationCount = updatedHistory.observationCount,
                        locationShiftMeters = shiftMeters
                    )
                )
            }
        }

        // Feature 186: Deteksi record yang tidak muncul lagi pada update ini
        // (JANGAN simpulkan api padam!)
        var removedCount = 0
        for ((fp, hist) in existingHistory) {
            if (!seenFingerprintsInThisBatch.contains(fp)) {
                // Periksa apakah baru saja menghilang
                if (currentReceivedTime - hist.lastSeenByApp <= 30 * 60 * 1000L) {
                    removedCount++
                    events.add(
                        HotspotEvent(
                            eventId = "EVT_REM_${System.nanoTime()}",
                            type = HotspotEventType.HOTSPOT_REMOVED,
                            hotspotFingerprint = fp,
                            acquisitionTimestamp = hist.latestObservationTime,
                            receivedAt = currentReceivedTime,
                            latitude = hist.observations.lastOrNull()?.latitude ?: 0.0,
                            longitude = hist.observations.lastOrNull()?.longitude ?: 0.0,
                            satellite = hist.observations.lastOrNull()?.satellite ?: "SAT",
                            confidence = hist.lastConfidence,
                            brightness = hist.lastBrightness,
                            explanation = "Hotspot tidak muncul pada pembaruan data terbaru (disebabkan jangkauan lintasan satelit atau pemrosesan sumber, bukan konfirmasi api padam)."
                        )
                    )
                }
            }
        }

        return HotspotDetectionResult(
            processedHotspots = processedList,
            newEvents = events,
            newHotspotsCount = newCount,
            updatedHotspotsCount = updatedCount,
            removedHotspotsCount = removedCount
        )
    }
}
