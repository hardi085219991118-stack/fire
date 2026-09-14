package com.example.data.repository

data class ValidationResult(
    val isValid: Boolean,
    val rejectReason: String? = null
)

object FireValidator {

    fun validateRecord(
        lat: Double?,
        lon: Double?,
        acqDate: String?,
        acqTime: String?,
        brightness: Double?
    ): ValidationResult {
        if (lat == null || lat.isNaN() || lat < -90.0 || lat > 90.0) {
            return ValidationResult(false, "Latitude invalid atau di luar rentang [-90, 90]: $lat")
        }
        if (lon == null || lon.isNaN() || lon < -180.0 || lon > 180.0) {
            return ValidationResult(false, "Longitude invalid atau di luar rentang [-180, 180]: $lon")
        }
        if (acqDate.isNullOrBlank()) {
            return ValidationResult(false, "Acquisition date kosong")
        }
        val dateParts = acqDate.trim().split("-")
        if (dateParts.size != 3 || dateParts[0].length != 4) {
            return ValidationResult(false, "Format acquisition date invalid (harus yyyy-MM-dd): $acqDate")
        }
        if (acqTime.isNullOrBlank()) {
            return ValidationResult(false, "Acquisition time kosong")
        }
        val timeClean = acqTime.trim().replace(":", "")
        if (timeClean.length !in 3..4 || timeClean.toIntOrNull() == null) {
            return ValidationResult(false, "Format acquisition time invalid (harus HHmm): $acqTime")
        }
        if (brightness != null && (brightness.isNaN() || brightness <= 0.0 || brightness > 2000.0)) {
            return ValidationResult(false, "Nilai kecerahan (brightness) di luar batas fisik: $brightness K")
        }
        return ValidationResult(true, null)
    }
}
