package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Feature 302: Incident Status
 * OPEN, MONITORING, FIELD_VERIFIED, CLOSED
 */
enum class IncidentStatus(val label: String) {
    OPEN("OPEN"),
    MONITORING("MONITORING"),
    FIELD_VERIFIED("FIELD VERIFIED"),
    CLOSED("CLOSED")
}

/**
 * Feature 318: Field Verification Status
 * BELUM DIVERIFIKASI, DIVERIFIKASI LAPANGAN, TIDAK DITEMUKAN DI LAPANGAN, INFORMASI TIDAK CUKUP
 */
enum class FieldVerificationStatus(val label: String) {
    BELUM_DIVERIFIKASI("BELUM DIVERIFIKASI"),
    DIVERIFIKASI_LAPANGAN("DIVERIFIKASI LAPANGAN"),
    TIDAK_DITEMUKAN_DI_LAPANGAN("TIDAK DITEMUKAN DI LAPANGAN"),
    INFORMASI_TIDAK_CUKUP("INFORMASI TIDAK CUKUP")
}

/**
 * Feature 301–315: Incident Entity for Field Operations and Ground Verification
 */
@Entity(tableName = "incidents")
data class IncidentEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val status: IncidentStatus = IncidentStatus.OPEN,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val closedAt: Long? = null,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float = 0f,
    // Feature 304: Link to hotspot fingerprint
    val linkedHotspotFingerprint: String? = null,
    val satelliteObservationTimestamp: Long? = null,
    val satelliteName: String? = null,
    // Feature 305–311: Ground / Field Observation
    val fieldVerificationStatus: FieldVerificationStatus = FieldVerificationStatus.BELUM_DIVERIFIKASI,
    val fieldNotes: String = "",
    val fieldObserverName: String = "",
    val fieldObservationTimestamp: Long? = null,
    val fieldLatitude: Double? = null,
    val fieldLongitude: Double? = null,
    val fieldPhotoUri: String? = null,
    // Feature 317: Distance between satellite detection and field observation
    val distanceSatelliteToFieldMeters: Double? = null
)
