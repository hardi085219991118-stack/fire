package com.example.data.engine

import com.example.data.local.HotspotEntity
import com.example.data.model.Hotspot

/**
 * Feature 102: LATEST OBSERVATION ENGINE
 *
 * Menentukan record observasi satelit dengan acquisitionTimestamp paling baru.
 *
 * ATURAN MUTLAK:
 * JANGAN menentukan data terbaru berdasarkan:
 * - urutan JSON
 * - urutan array
 * - waktu request aplikasi
 * - waktu cache
 *
 * GUNAKAN HANYA:
 * acquisitionTimestamp asli dari data sumber satelit.
 */
object LatestObservationEngine {

    fun findLatestHotspot(hotspots: List<Hotspot>): Hotspot? {
        if (hotspots.isEmpty()) return null
        return hotspots.maxByOrNull { it.acquisitionTimestamp }
    }

    fun findLatestEntity(entities: List<HotspotEntity>): HotspotEntity? {
        if (entities.isEmpty()) return null
        return entities.maxByOrNull { it.acquisitionTimestamp }
    }

    fun findLatestAcquisitionTimestamp(hotspots: List<Hotspot>): Long? {
        return findLatestHotspot(hotspots)?.acquisitionTimestamp
    }

    fun findOldestHotspot(hotspots: List<Hotspot>): Hotspot? {
        if (hotspots.isEmpty()) return null
        return hotspots.minByOrNull { it.acquisitionTimestamp }
    }
}
