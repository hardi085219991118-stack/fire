package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "data_audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val requestTime: Long,
    val responseTime: Long,
    val source: String,
    val endpointUrl: String,
    val httpStatusCode: Int?,
    val isSuccess: Boolean,
    val errorMessage: String? = null,
    val numberReceived: Int,
    val numberValid: Int,
    val numberRejected: Int,
    val rejectReason: String? = null,
    val latestAcquisitionTimestamp: Long? = null,
    val oldestAcquisitionTimestamp: Long? = null,
    val latestSatellite: String? = null,
    val latestDataAgeMinutes: Long? = null,
    val deliveryLatencyMinutes: Long? = null
)
