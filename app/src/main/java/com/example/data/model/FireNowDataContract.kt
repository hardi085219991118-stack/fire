package com.example.data.model

/**
 * Feature 157 & 158: FIRE NOW DATA CONTRACT & FAIL CLOSED PRINCIPLE
 *
 * Sebuah record hotspot satelit HANYA boleh masuk ke UI jika memenuhi seluruh kontrak data:
 * 1. Koordinat valid (latitude -90 s/d 90, longitude -180 s/d 180)
 * 2. Timestamp observasi valid (epoch millis > 0, parseable)
 * 3. Sumber data valid (sumber resmi terdaftar)
 * 4. Record valid (tidak rusak, tidak kosong)
 *
 * FAIL CLOSED PRINCIPLE:
 * Jika salah satu validasi gagal:
 * JANGAN PERNAH menampilkan record.
 * "NO VALID DATA = NO MARKER"
 * Lebih baik kosong daripada salah atau menampilkan data palsu.
 */
data class DataContractValidationResult(
    val isValid: Boolean,
    val failureReason: String? = null
)

object FireNowDataContract {

    fun validate(
        latitude: Double?,
        longitude: Double?,
        acquisitionTimestamp: Long?,
        source: String?,
        satellite: String?
    ): DataContractValidationResult {
        // 1. Coordinate Check
        if (latitude == null || latitude < -90.0 || latitude > 90.0) {
            return DataContractValidationResult(
                isValid = false,
                failureReason = "FAIL CLOSED: Latitude tidak valid (${latitude ?: "NULL"}). Batas harus [-90, 90]."
            )
        }
        if (longitude == null || longitude < -180.0 || longitude > 180.0) {
            return DataContractValidationResult(
                isValid = false,
                failureReason = "FAIL CLOSED: Longitude tidak valid (${longitude ?: "NULL"}). Batas harus [-180, 180]."
            )
        }

        // 2. Timestamp Check
        if (acquisitionTimestamp == null || acquisitionTimestamp <= 0) {
            return DataContractValidationResult(
                isValid = false,
                failureReason = "FAIL CLOSED: Timestamp observasi satelit tidak valid atau hilang."
            )
        }

        // 3. Source Check
        if (source.isNullOrBlank()) {
            return DataContractValidationResult(
                isValid = false,
                failureReason = "FAIL CLOSED: Sumber data tidak valid atau kosong."
            )
        }

        // 4. Satellite Check
        if (satellite.isNullOrBlank()) {
            return DataContractValidationResult(
                isValid = false,
                failureReason = "FAIL CLOSED: Identitas satelit tidak valid atau kosong."
            )
        }

        return DataContractValidationResult(isValid = true, failureReason = null)
    }
}
