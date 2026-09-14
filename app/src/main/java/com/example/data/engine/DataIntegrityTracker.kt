package com.example.data.engine

/**
 * Feature 126 & 178: SOURCE RESPONSE COUNTER & DATA INTEGRITY REPORT
 *
 * Memastikan kebenaran audit data:
 * Invariant: recordsDisplayed <= recordsValid <= recordsReceived
 *
 * Laporan Integritas:
 * - recordsReceived
 * - recordsValid
 * - recordsRejected
 * - duplicatesRemoved
 * - missingTimestamps
 * - invalidCoordinates
 * - latestTimestamp
 * - oldestTimestamp
 */
data class DataIntegrityReport(
    val recordsReceived: Int,
    val recordsValid: Int,
    val recordsRejected: Int,
    val recordsDisplayed: Int,
    val duplicatesRemoved: Int,
    val missingTimestamps: Int,
    val invalidCoordinates: Int,
    val latestTimestamp: Long?,
    val oldestTimestamp: Long?,
    val isContractSatisfied: Boolean
)

object DataIntegrityTracker {

    fun generateReport(
        received: Int,
        valid: Int,
        rejected: Int,
        displayed: Int,
        duplicatesRemoved: Int = 0,
        missingTimestamps: Int = 0,
        invalidCoordinates: Int = 0,
        latestTimestamp: Long? = null,
        oldestTimestamp: Long? = null
    ): DataIntegrityReport {
        // Invariant check: displayed <= valid <= received
        val satisfiesInvariant = (displayed <= valid) && (valid <= received)

        return DataIntegrityReport(
            recordsReceived = received,
            recordsValid = valid,
            recordsRejected = rejected,
            recordsDisplayed = displayed,
            duplicatesRemoved = duplicatesRemoved,
            missingTimestamps = missingTimestamps,
            invalidCoordinates = invalidCoordinates,
            latestTimestamp = latestTimestamp,
            oldestTimestamp = oldestTimestamp,
            isContractSatisfied = satisfiesInvariant
        )
    }
}
