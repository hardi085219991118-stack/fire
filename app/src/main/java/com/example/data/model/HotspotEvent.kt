package com.example.data.model

/**
 * Feature 184, 185, 186: Hotspot Lifecycle Events
 */
enum class HotspotEventType {
    NEW_HOTSPOT_DATA,       // Record baru pertama kali diterima aplikasi
    HOTSPOT_DATA_UPDATED,   // Record yang sama mengalami pembaruan metadata
    HOTSPOT_REMOVED         // Hotspot tidak muncul lagi pada response berikutnya
}

data class HotspotEvent(
    val eventId: String,
    val type: HotspotEventType,
    val hotspotFingerprint: String,
    val acquisitionTimestamp: Long,
    val receivedAt: Long,
    val latitude: Double,
    val longitude: Double,
    val satellite: String,
    val confidence: String?,
    val brightness: Double?,
    val explanation: String
)

/**
 * Feature 188, 199, 200, 236: Hotspot Change History & Observation Timeline
 */
data class HotspotObservationRecord(
    val observationIndex: Int,
    val acquisitionTimestamp: Long,
    val receivedAt: Long,
    val latitude: Double,
    val longitude: Double,
    val satellite: String,
    val instrument: String,
    val confidence: String?,
    val brightness: Double?,
    val distanceShiftMeters: Double? = null
)

data class HotspotChangeHistory(
    val fingerprint: String,
    val firstSeenByApp: Long,          // Feature 182: Waktu pertama aplikasi menerima record ini (BUKAN waktu kebakaran!)
    val lastSeenByApp: Long,           // Waktu terakhir aplikasi menerima record ini
    val latestObservationTime: Long,   // Feature 183: Waktu observasi satelit asli
    val observationCount: Int,         // Feature 199: Jumlah observasi berulang
    val observations: List<HotspotObservationRecord>, // Feature 200: Timeline observasi
    val isPersistent: Boolean,         // Terdeteksi berulang (>= 2 observasi)
    val totalLocationShiftMeters: Double, // Feature 237 & 238: Pergeseran lokasi anomali termal
    val lastConfidence: String?,
    val lastBrightness: Double?
)

/**
 * Feature 193, 194, 195: Hotspot Grouping
 */
data class HotspotGroup(
    val groupId: String,
    val hotspotCount: Int,
    val centerLatitude: Double,
    val centerLongitude: Double,
    val radiusMeters: Double,
    val latestObservationTimestamp: Long, // Feature 194: Waktu observasi terbaru dari anggota kelompok (BUKAN firstSeen)
    val groupAgeMinutes: Long,            // Feature 195: Dihitung dari latestObservationTimestamp
    val satellitesInvolved: Set<String>,
    val isMultiSatellite: Boolean,        // Feature 191 & 192: Diamati oleh beberapa satelit
    val memberHotspotIds: List<String>
)

/**
 * Feature 196 & 197: Hotspot Density & Concentration
 */
data class HotspotDensityInfo(
    val countWithinRadius: Int,
    val radiusKm: Double,
    val concentrationLevel: String, // RENDAH, SEDANG, TINGGI
    val previousCount: Int,
    val delta: Int,
    val isIncreased: Boolean
)

/**
 * Feature 201 & 202: Temporal Change Engine Results
 */
enum class ChangeClassification(val label: String) {
    NO_SIGNIFICANT_CHANGE("Tidak Ada Perubahan Signifikan"),
    NEW_HOTSPOTS("Hotspot Baru Terdeteksi"),
    HOTSPOT_COUNT_INCREASED("Peningkatan Jumlah Anomali Termal"),
    HOTSPOT_COUNT_DECREASED("Penurunan Jumlah Hotspot"),
    DATA_SOURCE_UPDATED("Sumber Data Diperbarui"),
    DATA_SOURCE_STALLED("Sumber Data Stalled")
}

data class TemporalChangeResult(
    val classification: ChangeClassification,
    val previousHotspotCount: Int,
    val currentHotspotCount: Int,
    val countDifference: Int,
    val latestObservationTimestamp: Long?,
    val summaryMessage: String,
    val detectedAt: Long = System.currentTimeMillis()
)

/**
 * Feature 206, 207, 208, 211, 212, 213: Continuous Monitoring Session
 */
data class MonitoringSession(
    val sessionId: String,
    val sessionStartedAt: Long,
    val sessionEndedAt: Long? = null,
    val isActive: Boolean = false,
    val locationName: String = "Lokasi Saya",
    val latitude: Double,
    val longitude: Double,
    val radiusKm: Double = 25.0,
    val freshnessThresholdMinutes: Long = 30L,
    val source: String = "VIIRS_NOAA21_NRT",
    val checkIntervalMinutes: Int = 5,
    val checksExecuted: Int = 0,
    val missedChecksCount: Int = 0,
    val lastCheckTime: Long? = null,
    val lastSuccessfulUpdateTime: Long? = null,
    val lastMissedReason: String? = null
)

/**
 * Feature 214 & 215: Saved Watch Area
 */
data class SavedWatchArea(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusKm: Double,
    val createdAt: Long = System.currentTimeMillis(),
    val isCurrentGps: Boolean = false
)

/**
 * Feature 228: Alert Severity (Prioritas Pemantauan, BUKAN bahaya kebakaran)
 */
enum class AlertSeverity(val label: String) {
    INFO("INFORMASI"),
    LOW("PRIORITAS RENDAH"),
    MEDIUM("PRIORITAS SEDANG"),
    HIGH("PRIORITAS TINGGI"),
    CRITICAL("PRIORITAS KRITIS")
}

/**
 * Feature 221, 222, 229, 230, 233, 234, 235: Fire Alert
 */
data class FireAlert(
    val alertId: String,
    val hotspotFingerprint: String,
    val severity: AlertSeverity,
    val title: String,
    val observationTimestamp: Long, // Feature 235: Waktu observasi satelit asli
    val alertedAt: Long,             // Feature 234: Waktu aplikasi membuat alert (BUKAN waktu kebakaran)
    val dataAgeMinutes: Long,
    val distanceKm: Double?,
    val satellite: String,
    val confidence: String?,
    val explanationLines: List<String>,
    val safetyDisclaimer: String = "Hotspot satelit bukan konfirmasi kebakaran di lapangan.",
    val isRead: Boolean = false
)

/**
 * Feature 257 & 258: Data Quality Score
 */
enum class QualityScoreLevel(val label: String) {
    HIGH("TINGGI"),
    MEDIUM("SEDANG"),
    LOW("RENDAH")
}

data class DataQualityReport(
    val level: QualityScoreLevel,
    val scorePercent: Int,
    val isTimestampValid: Boolean,
    val isCoordinateValid: Boolean,
    val isSourceValid: Boolean,
    val freshnessMinutes: Long?,
    val hasConfidence: Boolean,
    val explanationReasons: List<String>
)

/**
 * Feature 252, 253, 254, 255: Source Failover & Consistency Comparison
 */
data class SourceComparisonReport(
    val primarySource: String,
    val alternativeSource: String? = null,
    val isUsingAlternative: Boolean = false,
    val primaryLatencyMinutes: Long? = null,
    val alternativeLatencyMinutes: Long? = null,
    val latencyDifferenceMinutes: Long? = null,
    val discrepancyDetected: Boolean = false,
    val discrepancyDetails: String? = null,
    val failoverWarningMessage: String? = null
)
