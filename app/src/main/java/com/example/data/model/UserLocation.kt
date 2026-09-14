package com.example.data.model

data class UserLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val timestamp: Long,
    val provider: String = "GPS/FusedLocation"
)
