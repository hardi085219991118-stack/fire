package com.example.data.engine

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Feature 267–269: GEODESIC BEARING & DISTANCE FORMATTER
 * Menghitung arah kompas geografis (bearing) userLocation -> hotspotLocation
 * menggunakan rumus geodetik forward azimuth bola bumi.
 */
object GeodesicBearingCalculator {

    /**
     * Hitung initial bearing dalam derajat (0..360)
     */
    fun calculateBearing(
        fromLat: Double,
        fromLon: Double,
        toLat: Double,
        toLon: Double
    ): Double {
        val phi1 = Math.toRadians(fromLat)
        val phi2 = Math.toRadians(toLat)
        val deltaLambda = Math.toRadians(toLon - fromLon)

        val y = sin(deltaLambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda)
        val theta = atan2(y, x)

        val bearingDeg = Math.toDegrees(theta)
        return (bearingDeg + 360.0) % 360.0
    }

    /**
     * Konversi derajat bearing ke arah mata angin (Cardinal Direction)
     */
    fun bearingToCardinal(bearingDeg: Double): String {
        val normalized = (bearingDeg % 360.0 + 360.0) % 360.0
        return when {
            normalized >= 337.5 || normalized < 22.5 -> "U (Utara / N)"
            normalized in 22.5..67.5 -> "TL (Timur Laut / NE)"
            normalized in 67.5..112.5 -> "T (Timur / E)"
            normalized in 112.5..157.5 -> "TG (Tenggara / SE)"
            normalized in 157.5..202.5 -> "S (Selatan / S)"
            normalized in 202.5..247.5 -> "BD (Barat Daya / SW)"
            normalized in 247.5..292.5 -> "B (Barat / W)"
            else -> "BL (Barat Laut / NW)"
        }
    }

    /**
     * Feature 269: Jarak display
     * Jika <1 km: meter
     * Jika >=1 km: kilometer
     */
    fun formatDisplayDistance(distanceMeters: Double?): String {
        if (distanceMeters == null || distanceMeters.isNaN()) return "-- meter"
        return if (distanceMeters < 1000.0) {
            "${distanceMeters.roundToInt()} meter"
        } else {
            val km = distanceMeters / 1000.0
            "${String.format(java.util.Locale.US, "%.1f", km)} km"
        }
    }
}
