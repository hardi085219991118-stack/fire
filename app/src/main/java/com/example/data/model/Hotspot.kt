package com.example.data.model

data class Hotspot(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val brightness: Double?,
    val confidence: String?,
    val satellite: String,
    val instrument: String,
    val acquisitionDate: String,
    val acquisitionTime: String,
    val acquisitionTimestamp: Long,
    val source: String,
    val dataReceivedTimestamp: Long,
    val satelliteAgeMinutes: Long,
    val deliveryLatencyMinutes: Long?,
    val distanceFromUser: Double?,
    val status: HotspotAgeStatus,
    val bright_ti5: Double? = null,
    val frp: Double? = null,
    val daynight: String? = null,
    val priorityResult: FireNowPriorityResult? = null,
    val firstSeenByApp: Long? = null,       // Feature 182: Waktu pertama aplikasi menerima record ini
    val isNewData: Boolean = false,          // Feature 187: Badge 🆕 DATA BARU MASUK
    val fingerprint: String? = null,         // Feature 189: Identitas fingerprint konsisten
    val observationCount: Int = 1,           // Feature 199: Persistence count
    val groupId: String? = null,             // Feature 193: Hotspot Group ID
    val isMultiSatelliteObs: Boolean = false,// Feature 191 & 192: Diamati oleh multi satelit
    val locationShiftMeters: Double? = null  // Feature 237 & 238: Pergeseran anomali termal
)
