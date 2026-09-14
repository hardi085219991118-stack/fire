package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hotspots")
data class HotspotEntity(
    @PrimaryKey
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
    val bright_ti5: Double? = null,
    val frp: Double? = null,
    val daynight: String? = null
)
