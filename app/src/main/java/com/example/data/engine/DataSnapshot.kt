package com.example.data.engine

import com.example.data.model.Hotspot
import java.security.MessageDigest

data class DataSnapshot(
    val id: String,
    val appTimestamp: Long = System.currentTimeMillis(),
    val latestObservationTimestamp: Long?,
    val recordCount: Int,
    val source: String,
    val freshnessMinutes: Long?,
    val checksum: String,
    val hotspotFingerprints: Set<String>
)

data class SnapshotComparison(
    val previousSnapshot: DataSnapshot,
    val currentSnapshot: DataSnapshot,
    val newCount: Int,
    val updatedCount: Int,
    val removedCount: Int,
    val unchangedCount: Int,
    val explanation: String
)

/**
 * Feature 329–331: DATA SNAPSHOT & SNAPSHOT COMPARISON
 */
object DataSnapshotEngine {

    fun createSnapshot(
        hotspots: List<Hotspot>,
        source: String,
        latestObservationTimestamp: Long?,
        freshnessMinutes: Long?
    ): DataSnapshot {
        val fps = hotspots.map { it.fingerprint ?: it.id }.toSet()
        val rawString = "${hotspots.size}_${latestObservationTimestamp}_${source}_${fps.sorted().joinToString(",")}"
        val checksum = hashSha256(rawString)

        return DataSnapshot(
            id = "SNAP_${System.currentTimeMillis()}",
            appTimestamp = System.currentTimeMillis(),
            latestObservationTimestamp = latestObservationTimestamp,
            recordCount = hotspots.size,
            source = source,
            freshnessMinutes = freshnessMinutes,
            checksum = checksum,
            hotspotFingerprints = fps
        )
    }

    /**
     * Feature 330: SNAPSHOT COMPARISON
     * PENTING: Dilarang menganggap removed = padam!
     * Labeled: "Hotspot tidak muncul pada pembaruan data satelit".
     */
    fun compareSnapshots(
        previous: DataSnapshot,
        current: DataSnapshot
    ): SnapshotComparison {
        val prevFps = previous.hotspotFingerprints
        val currFps = current.hotspotFingerprints

        val newCount = (currFps - prevFps).size
        val removedCount = (prevFps - currFps).size
        val unchangedCount = (currFps intersect prevFps).size

        val explanation = buildString {
            append("Perbandingan Snapshot ${previous.source} vs ${current.source}:\n")
            append("• Data Baru Masuk: $newCount titik\n")
            append("• Tetap Teramati: $unchangedCount titik\n")
            append("• Tidak Muncul pada Pembaruan Ini: $removedCount titik\n")
            append("⚠️ Catatan: Titik yang tidak muncul kembali BUKAN berarti api telah padam, melainkan tidak terdeteksi pada lintasan orbit satelit terkini.")
        }

        return SnapshotComparison(
            previousSnapshot = previous,
            currentSnapshot = current,
            newCount = newCount,
            updatedCount = 0,
            removedCount = removedCount,
            unchangedCount = unchangedCount,
            explanation = explanation
        )
    }

    private fun hashSha256(input: String): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(input.toByteArray(Charsets.UTF_8))
            hash.joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            input.hashCode().toString()
        }
    }
}
