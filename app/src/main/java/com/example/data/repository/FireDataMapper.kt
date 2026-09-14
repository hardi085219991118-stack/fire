package com.example.data.repository

import com.example.data.local.HotspotEntity
import com.example.data.model.Hotspot
import com.example.data.model.UserLocation

object FireDataMapper {

    fun entityToDomain(
        entity: HotspotEntity,
        userLocation: UserLocation?,
        currentTimestamp: Long = System.currentTimeMillis()
    ): Hotspot {
        val distance = if (userLocation != null) {
            DistanceCalculator.calculateHaversineDistanceKm(
                userLocation.latitude,
                userLocation.longitude,
                entity.latitude,
                entity.longitude
            )
        } else {
            null
        }

        val ageMinutes = FireAgeCalculator.calculateAgeMinutes(entity.acquisitionTimestamp, currentTimestamp)
        val status = FireAgeCalculator.determineStatus(ageMinutes)

        val deliveryLatency: Long? = if (entity.dataReceivedTimestamp > 0 && entity.acquisitionTimestamp > 0) {
            val latency = (entity.dataReceivedTimestamp - entity.acquisitionTimestamp) / (60 * 1000)
            if (latency >= 0) latency else null
        } else {
            null
        }

        return Hotspot(
            id = entity.id,
            latitude = entity.latitude,
            longitude = entity.longitude,
            brightness = entity.brightness,
            confidence = entity.confidence,
            satellite = entity.satellite,
            instrument = entity.instrument,
            acquisitionDate = entity.acquisitionDate,
            acquisitionTime = entity.acquisitionTime,
            acquisitionTimestamp = entity.acquisitionTimestamp,
            source = entity.source,
            dataReceivedTimestamp = entity.dataReceivedTimestamp,
            satelliteAgeMinutes = ageMinutes,
            deliveryLatencyMinutes = deliveryLatency,
            distanceFromUser = distance,
            status = status,
            bright_ti5 = entity.bright_ti5,
            frp = entity.frp,
            daynight = entity.daynight
        )
    }
}
