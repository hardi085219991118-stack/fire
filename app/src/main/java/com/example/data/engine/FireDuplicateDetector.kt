package com.example.data.engine

import com.example.data.local.HotspotEntity
import com.example.data.model.Hotspot

/**
 * Feature 127: FIRE DUPLICATE DETECTOR
 *
 * Mencegah penampilan record duplikat jika sumber mengirim record identik.
 *
 * Menggunakan kombinasi kunci identitas unik:
 * - latitude (presisi penuh)
 * - longitude (presisi penuh)
 * - acquisitionTimestamp
 * - satellite
 * - instrument
 *
 * ATURAN MUTLAK:
 * JANGAN menghapus dua hotspot berbeda hanya karena lokasinya berdekatan.
 */
data class DeduplicationResult<T>(
    val uniqueRecords: List<T>,
    val duplicatesRemovedCount: Int
)

object FireDuplicateDetector {

    fun deduplicateEntities(entities: List<HotspotEntity>): DeduplicationResult<HotspotEntity> {
        val seenKeys = mutableSetOf<String>()
        val uniqueList = mutableListOf<HotspotEntity>()
        var duplicatesCount = 0

        for (item in entities) {
            val key = "${item.latitude}_${item.longitude}_${item.acquisitionTimestamp}_${item.satellite}_${item.instrument}"
            if (seenKeys.add(key)) {
                uniqueList.add(item)
            } else {
                duplicatesCount++
            }
        }

        return DeduplicationResult(
            uniqueRecords = uniqueList,
            duplicatesRemovedCount = duplicatesCount
        )
    }

    fun deduplicateHotspots(hotspots: List<Hotspot>): DeduplicationResult<Hotspot> {
        val seenKeys = mutableSetOf<String>()
        val uniqueList = mutableListOf<Hotspot>()
        var duplicatesCount = 0

        for (item in hotspots) {
            val key = "${item.latitude}_${item.longitude}_${item.acquisitionTimestamp}_${item.satellite}_${item.instrument}"
            if (seenKeys.add(key)) {
                uniqueList.add(item)
            } else {
                duplicatesCount++
            }
        }

        return DeduplicationResult(
            uniqueRecords = uniqueList,
            duplicatesRemovedCount = duplicatesCount
        )
    }
}
