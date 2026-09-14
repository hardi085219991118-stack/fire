package com.example.data.engine

import com.example.data.model.Hotspot
import com.example.data.repository.FireAgeCalculator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class HeatmapPoint(
    val latitude: Double,
    val longitude: Double,
    val intensity: Float, // 0.0f..1.0f
    val satelliteAgeMinutes: Long,
    val satellite: String
)

data class HeatmapResult(
    val points: List<HeatmapPoint>,
    val emptyStateMessage: String? = null,
    val disclaimer: String = "Visualisasi kepadatan anomali termal. Bukan representasi luas kebakaran riil."
)

data class DensityGridCell(
    val gridKey: String, // e.g. "-2.35_114.45"
    val centerLat: Double,
    val centerLon: Double,
    val count: Int,
    val latestObservationTimestamp: Long?,
    val newestAgeMinutes: Long?
)

data class DensityComparison(
    val count30Min: Int,
    val count1Hour: Int,
    val count3Hours: Int,
    val count6Hours: Int,
    val concentrationAlert: String? = null
)

data class AreaRankingItem(
    val areaName: String,
    val hotspotCount: Int,
    val newestAgeMinutes: Long?,
    val description: String
)

data class TemporalTrendBucket(
    val timeLabelUtc: String,
    val count: Int,
    val isDataGap: Boolean = false
)

data class AgeDistribution(
    val countUnder30Min: Int,
    val count31To60Min: Int,
    val count1To3Hours: Int,
    val count3To6Hours: Int,
    val countAbove6Hours: Int
)

data class SourceAnalyticsItem(
    val sourceName: String,
    val latestObservationTimestamp: Long?,
    val dataAgeMinutes: Long?,
    val latencyMinutes: Long?,
    val recordCount: Int
)

data class SourceIncident(
    val timestamp: Long = System.currentTimeMillis(),
    val sourceName: String,
    val httpStatus: Int?,
    val errorMessage: String,
    val consecutiveCount: Int
)

/**
 * Feature 275–290: ADVANCED MAP INTELLIGENCE, TEMPORAL TRENDS & SOURCE OBSERVABILITY
 */
object AdvancedMapIntelligence {

    /**
     * Feature 275–278: HOTSPOT HEATMAP COMPUTATION
     * Dihitung hanya dari hotspot nyata tanpa titik interpolasi buatan.
     */
    fun computeHeatmap(
        hotspots: List<Hotspot>,
        maxAgeMinutes: Long = 30L
    ): HeatmapResult {
        val filtered = hotspots.filter { it.satelliteAgeMinutes <= maxAgeMinutes }
        if (filtered.isEmpty()) {
            return HeatmapResult(
                points = emptyList(),
                emptyStateMessage = "TIDAK ADA DATA HOTSPOT UNTUK HEATMAP."
            )
        }

        val maxBright = filtered.maxOfOrNull { it.brightness ?: 300.0 } ?: 300.0
        val minBright = filtered.minOfOrNull { it.brightness ?: 300.0 } ?: 300.0
        val brightRange = (maxBright - minBright).coerceAtLeast(1.0)

        val points = filtered.map { hs ->
            val b = hs.brightness ?: 310.0
            val normalizedWeight = ((b - minBright) / brightRange).toFloat().coerceIn(0.2f, 1.0f)
            HeatmapPoint(
                latitude = hs.latitude,
                longitude = hs.longitude,
                intensity = normalizedWeight,
                satelliteAgeMinutes = hs.satelliteAgeMinutes,
                satellite = hs.satellite
            )
        }

        return HeatmapResult(points = points)
    }

    /**
     * Feature 279: DENSITY GRID
     * Grid spasial ~0.05 derajat (~5.5 km) dari data nyata.
     */
    fun computeDensityGrid(hotspots: List<Hotspot>): List<DensityGridCell> {
        val cellMap = mutableMapOf<String, MutableList<Hotspot>>()

        for (hs in hotspots) {
            val gridLat = String.format(Locale.US, "%.2f", hs.latitude)
            val gridLon = String.format(Locale.US, "%.2f", hs.longitude)
            val key = "${gridLat}_${gridLon}"
            cellMap.getOrPut(key) { mutableListOf() }.add(hs)
        }

        return cellMap.map { (key, list) ->
            val lat = list.map { it.latitude }.average()
            val lon = list.map { it.longitude }.average()
            val newest = list.minByOrNull { it.satelliteAgeMinutes }
            DensityGridCell(
                gridKey = key,
                centerLat = lat,
                centerLon = lon,
                count = list.size,
                latestObservationTimestamp = newest?.acquisitionTimestamp,
                newestAgeMinutes = newest?.satelliteAgeMinutes
            )
        }.sortedByDescending { it.count }
    }

    /**
     * Feature 280 & 282: DENSITY TIME COMPARISON & CONCENTRATION ALERT
     */
    fun compareDensityTimeWindows(
        allHotspots: List<Hotspot>,
        previousCount30Min: Int? = null
    ): DensityComparison {
        val c30 = allHotspots.count { it.satelliteAgeMinutes <= 30 }
        val c60 = allHotspots.count { it.satelliteAgeMinutes <= 60 }
        val c180 = allHotspots.count { it.satelliteAgeMinutes <= 180 }
        val c360 = allHotspots.count { it.satelliteAgeMinutes <= 360 }

        val alert = if (previousCount30Min != null && c30 > previousCount30Min) {
            "⚠️ KONSENTRASI HOTSPOT MENINGKAT (sebelumnya: $previousCount30Min, sekarang: $c30 titik dalam ≤30m)"
        } else null

        return DensityComparison(
            count30Min = c30,
            count1Hour = c60,
            count3Hours = c180,
            count6Hours = c360,
            concentrationAlert = alert
        )
    }

    /**
     * Feature 281: AREA HOTSPOT RANKING
     * Labeled: "area dengan konsentrasi hotspot tertinggi", never "area paling terbakar".
     */
    fun rankAreasByHotspotConcentration(
        gridCells: List<DensityGridCell>
    ): List<AreaRankingItem> {
        return gridCells.take(5).mapIndexed { idx, cell ->
            AreaRankingItem(
                areaName = "Grid Sektor #${idx + 1} [${String.format(Locale.US, "%.3f", cell.centerLat)}, ${String.format(Locale.US, "%.3f", cell.centerLon)}]",
                hotspotCount = cell.count,
                newestAgeMinutes = cell.newestAgeMinutes,
                description = "Area dengan konsentrasi ${cell.count} hotspot (observasi terbaru ${cell.newestAgeMinutes ?: "?"}m lalu)"
            )
        }
    }

    /**
     * Feature 283–285: TEMPORAL TREND & GAP DETECTION
     * Menggunakan STRICT observation timestamp (bukan request time).
     */
    fun computeTemporalTrend(
        hotspots: List<Hotspot>,
        windowHours: Int = 12
    ): List<TemporalTrendBucket> {
        if (hotspots.isEmpty()) return emptyList()

        val timeFormat = SimpleDateFormat("HH:00", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        // Group observations by hourly bucket based on acquisitionTimestamp
        val grouped = hotspots.groupBy {
            val roundedHour = (it.acquisitionTimestamp / (3600 * 1000L)) * (3600 * 1000L)
            roundedHour
        }

        val sortedTimes = grouped.keys.sorted()
        if (sortedTimes.isEmpty()) return emptyList()

        val earliest = sortedTimes.first()
        val latest = sortedTimes.last()
        val hourMillis = 3600 * 1000L
        val result = mutableListOf<TemporalTrendBucket>()

        var current = earliest
        while (current <= latest) {
            val count = grouped[current]?.size ?: 0
            val label = timeFormat.format(Date(current))
            // Feature 285: Trend Gap indicator if 0 observations during period
            val isGap = (count == 0)
            result.add(
                TemporalTrendBucket(
                    timeLabelUtc = label,
                    count = count,
                    isDataGap = isGap
                )
            )
            current += hourMillis
        }

        return result
    }

    /**
     * Feature 286: HOTSPOT AGE DISTRIBUTION
     */
    fun computeAgeDistribution(hotspots: List<Hotspot>): AgeDistribution {
        return AgeDistribution(
            countUnder30Min = hotspots.count { it.satelliteAgeMinutes <= 30 },
            count31To60Min = hotspots.count { it.satelliteAgeMinutes in 31..60 },
            count1To3Hours = hotspots.count { it.satelliteAgeMinutes in 61..180 },
            count3To6Hours = hotspots.count { it.satelliteAgeMinutes in 181..360 },
            countAbove6Hours = hotspots.count { it.satelliteAgeMinutes > 360 }
        )
    }

    /**
     * Feature 287 & 288: SOURCE FRESHNESS & LATENCY COMPARISON
     */
    fun computeSourceAnalytics(
        hotspots: List<Hotspot>,
        dataReceivedTimestamp: Long?
    ): List<SourceAnalyticsItem> {
        val grouped = hotspots.groupBy { "${it.satellite} (${it.instrument})" }
        return grouped.map { (src, list) ->
            val newestObs = list.maxOfOrNull { it.acquisitionTimestamp }
            val dataAge = if (newestObs != null) (System.currentTimeMillis() - newestObs) / (60 * 1000) else null
            val latency = if (dataReceivedTimestamp != null && newestObs != null) {
                ((dataReceivedTimestamp - newestObs) / (60 * 1000)).coerceAtLeast(0)
            } else null

            SourceAnalyticsItem(
                sourceName = src,
                latestObservationTimestamp = newestObs,
                dataAgeMinutes = dataAge,
                latencyMinutes = latency,
                recordCount = list.size
            )
        }.sortedBy { it.dataAgeMinutes ?: Long.MAX_VALUE }
    }
}
