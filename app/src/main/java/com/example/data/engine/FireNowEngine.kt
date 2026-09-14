package com.example.data.engine

import com.example.data.model.DataQualityReport
import com.example.data.model.FireAlert
import com.example.data.model.FireNowPriorityResult
import com.example.data.model.FireNowPriorityScore
import com.example.data.model.FreshnessConfig
import com.example.data.model.Hotspot
import com.example.data.model.HotspotAgeStatus
import com.example.data.model.HotspotChangeHistory
import com.example.data.model.HotspotDensityInfo
import com.example.data.model.HotspotEvent
import com.example.data.model.HotspotGroup
import com.example.data.model.SourceComparisonReport
import com.example.data.model.TemporalChangeResult
import com.example.data.model.UserLocation
import com.example.data.repository.DistanceCalculator
import com.example.data.repository.FetchReport
import com.example.data.repository.FireRepository

/**
 * Feature 101 & Tahap 3: FIRE NOW DATA ENGINE & GEOSPATIAL INTELLIGENCE
 */
data class FireNowEngineResult(
    val allValidatedHotspots: List<Hotspot>,
    val filteredHotspots: List<Hotspot>,
    val priorityRankedHotspots: List<HotspotWithPriority>,
    val latestHotspot: Hotspot?,
    val latestObservationTimestamp: Long?,
    val newestDataAgeSeconds: Long?,
    val newestDataAgeMinutes: Long?,
    val newestDataStatus: HotspotAgeStatus,
    val isFreshEnoughForFireNow: Boolean, // <= 30 menit
    val deliveryLatencyMinutes: Long?,
    val deliveryLatencyFormatted: String,
    val dataReceivedTimestamp: Long?,
    val requestDurationMs: Long?,
    val serverTimeOffsetMs: Long,
    val clockSyncWarning: String?,
    val dataIntegrityReport: DataIntegrityReport,
    val isDataStalled: Boolean,
    val stallWarningMessage: String?,
    val isDeliveryLatencyHigh: Boolean,
    val deliveryLatencyWarningMessage: String?,
    val isDataGapDetected: Boolean,
    val dataGapMessage: String?,
    val fetchReport: FetchReport,
    // Tahap 3 additions
    val detectedEvents: List<HotspotEvent> = emptyList(),
    val newHotspotsCount: Int = 0,
    val updatedHotspotsCount: Int = 0,
    val hotspotGroups: List<HotspotGroup> = emptyList(),
    val densityInfo: HotspotDensityInfo? = null,
    val temporalChange: TemporalChangeResult? = null,
    val dataQualityReport: DataQualityReport? = null,
    val sourceComparison: SourceComparisonReport? = null,
    val generatedAlerts: List<FireAlert> = emptyList()
)

data class HotspotWithPriority(
    val hotspot: Hotspot,
    val priorityResult: FireNowPriorityResult
)

class FireNowEngine(
    private val repository: FireRepository,
    private val alertEngine: FireAlertEngine? = null
) {
    private val recentObservationHistory = mutableListOf<Long?>()
    private val changeHistoryMap = mutableMapOf<String, HotspotChangeHistory>()
    private var previousHotspotsList = listOf<Hotspot>()
    private var previousDensityCount = 0

    suspend fun execute(
        userLocation: UserLocation?,
        mapKey: String?,
        source: String = "VIIRS_NOAA21_NRT",
        maxAgeMinutes: Long = 30L,
        maxRadiusKm: Double = 25.0,
        isLocalFireNowMode: Boolean = true,
        freshnessConfig: FreshnessConfig = FreshnessConfig.DEFAULT,
        serverTimeOffsetMs: Long = 0L,
        alertEngineConfig: AlertEngineConfig = AlertEngineConfig()
    ): FireNowEngineResult {
        val requestStartTime = System.currentTimeMillis()

        // 1. Fetch from repository
        val fetchReport = repository.fetchHotspots(
            userLocation = userLocation,
            mapKey = mapKey,
            source = source,
            preferAreaQuery = mapKey?.isNotBlank() == true
        )

        val requestEndTime = System.currentTimeMillis()
        val requestDuration = requestEndTime - requestStartTime

        // 2 & 3. Get cached/validated hotspots
        val rawHotspots = repository.getCachedHotspots(userLocation)
        val now = System.currentTimeMillis()

        // Deduplicate
        val deduplicated = FireDuplicateDetector.deduplicateHotspots(rawHotspots).uniqueRecords

        // Tahap 3: Process New Hotspots, First-Seen, Persistence & Change Detection (Fitur 181-189)
        val detectionResult = NewHotspotDetectionEngine.processHotspots(
            incomingHotspots = deduplicated,
            existingHistory = changeHistoryMap,
            currentReceivedTime = repository.getLatestReceivedTimestamp() ?: now
        )

        // Tahap 3: Annotate Multi-Satellite Observations (Fitur 191 & 192)
        val multiSatAnnotated = GeospatialIntelligenceEngine.annotateMultiSatelliteObservations(detectionResult.processedHotspots)

        // Update timestamps & distance
        val processedHotspots = multiSatAnnotated.map { hs ->
            val dist = if (userLocation != null) {
                DistanceCalculator.calculateHaversineDistanceKm(
                    userLocation.latitude,
                    userLocation.longitude,
                    hs.latitude,
                    hs.longitude
                )
            } else null

            val freshness = DataFreshnessEngine.calculateFreshness(
                acquisitionTimestamp = hs.acquisitionTimestamp,
                currentTimestamp = now,
                serverTimeOffsetMs = serverTimeOffsetMs,
                config = freshnessConfig
            )

            hs.copy(
                distanceFromUser = dist,
                satelliteAgeMinutes = freshness.dataAgeMinutes,
                status = freshness.freshnessStatus
            )
        }

        // Tahap 3: Hotspot Grouping (Fitur 193-195)
        val hotspotGroups = GeospatialIntelligenceEngine.createHotspotGroups(
            hotspots = processedHotspots,
            clusterRadiusKm = 1.5,
            currentTimestamp = now
        )

        // 4. Find latest observation
        val latestHotspot = LatestObservationEngine.findLatestHotspot(processedHotspots)
        val latestAcqTimestamp = latestHotspot?.acquisitionTimestamp ?: fetchReport.latestObservationTime

        // Track history for stall detection
        recentObservationHistory.add(latestAcqTimestamp)
        if (recentObservationHistory.size > 10) {
            recentObservationHistory.removeAt(0)
        }

        val (isStalled, stallMsg) = DataStallDetector.checkDataStall(recentObservationHistory)
        val (isLatencyHigh, latencyHighMsg) = DataStallDetector.checkDeliveryLatencySpike(fetchReport.deliveryLatencyMinutes)
        val (isGapDetected, gapMsg) = DataStallDetector.detectDataGaps(processedHotspots)

        // 5. Measure freshness of the newest observation
        val latestFreshness = DataFreshnessEngine.calculateFreshness(
            acquisitionTimestamp = latestAcqTimestamp,
            currentTimestamp = now,
            serverTimeOffsetMs = serverTimeOffsetMs,
            config = freshnessConfig
        )

        val isFreshEnough = latestFreshness.dataAgeMinutes <= freshnessConfig.veryFreshMinutes

        // 6. Filter by age and radius using CurrentDataGate (Fitur 249-250)
        val filtered = processedHotspots.filter { hs ->
            val gateResult = if (isLocalFireNowMode) {
                CurrentDataGate.evaluateHotspot(hs, maxThresholdMinutes = maxAgeMinutes, currentTimestamp = now)
            } else {
                GateResult(isPassed = hs.satelliteAgeMinutes <= maxAgeMinutes, reason = "", dataAgeMinutes = hs.satelliteAgeMinutes)
            }

            val matchesRadius = if (isLocalFireNowMode && userLocation != null && hs.distanceFromUser != null) {
                hs.distanceFromUser <= maxRadiusKm
            } else {
                true
            }
            gateResult.isPassed && matchesRadius
        }

        // Tahap 3: Hotspot Density & Concentration (Fitur 196 & 197)
        val densityInfo = if (userLocation != null) {
            GeospatialIntelligenceEngine.calculateDensity(
                hotspots = processedHotspots,
                centerLat = userLocation.latitude,
                centerLon = userLocation.longitude,
                radiusKm = maxRadiusKm,
                previousCount = previousDensityCount
            ).also { previousDensityCount = it.countWithinRadius }
        } else null

        // Tahap 3: Temporal Change Analysis (Fitur 201 & 202)
        val temporalChange = GeospatialIntelligenceEngine.analyzeTemporalChange(
            previousHotspots = previousHotspotsList,
            currentHotspots = processedHotspots,
            newHotspotsCount = detectionResult.newHotspotsCount,
            isSourceStalled = isStalled,
            latestObservationTimestamp = latestAcqTimestamp
        )
        previousHotspotsList = processedHotspots

        // Tahap 3: Data Quality Score (Fitur 257 & 258)
        val qualityReport = CurrentDataGate.evaluateDataQuality(
            hotspots = processedHotspots,
            latestObsAgeMinutes = if (latestAcqTimestamp != null) latestFreshness.dataAgeMinutes else null,
            isFromCache = fetchReport.isFromCache,
            hasNetworkError = !fetchReport.isSuccess
        )

        // Tahap 3: Source Failover & Consistency Comparison (Fitur 252-255)
        val sourceComparison = CurrentDataGate.evaluateSourceComparison(
            primarySource = source,
            alternativeSource = if (source != "MODIS_NRT") "MODIS_NRT" else "VIIRS_SNPP_NRT",
            isUsingAlternative = false,
            primaryLatency = fetchReport.deliveryLatencyMinutes,
            altLatency = null
        )

        // Tahap 3: Fire Alert Engine evaluation (Fitur 221-235)
        val alerts = if (alertEngine != null) {
            val newlyArrived = processedHotspots.filter { it.isNewData }
            alertEngine.evaluateAndGenerateAlerts(
                newHotspots = newlyArrived,
                config = alertEngineConfig,
                currentTimestamp = now
            )
        } else emptyList()

        // 7 & 8. Calculate Priority Score & Rank
        val ranked = filtered.map { hs ->
            val priority = FireNowPriorityScore.calculate(
                ageMinutes = hs.satelliteAgeMinutes,
                distanceKm = hs.distanceFromUser,
                confidence = hs.confidence,
                brightness = hs.brightness
            )
            HotspotWithPriority(hs, priority)
        }.sortedWith(
            compareByDescending<HotspotWithPriority> { it.priorityResult.score }
                .thenBy { it.hotspot.satelliteAgeMinutes }
                .thenBy { it.hotspot.distanceFromUser ?: Double.MAX_VALUE }
        )

        // 9. Delivery latency formatting
        val deliveryLatencySec = if (latestAcqTimestamp != null && fetchReport.isSuccess) {
            val recTime = repository.getLatestReceivedTimestamp() ?: now
            val diffSec = (recTime - latestAcqTimestamp) / 1000
            if (diffSec >= 0) diffSec else null
        } else null

        val formattedDeliveryLatency = if (deliveryLatencySec != null) {
            val m = deliveryLatencySec / 60
            val s = deliveryLatencySec % 60
            if (m > 0) "$m menit $s detik" else "$s detik"
        } else {
            "Tidak tersedia"
        }

        // 10. Data Integrity Report
        val integrity = DataIntegrityTracker.generateReport(
            received = fetchReport.totalReceived,
            valid = fetchReport.validCount,
            rejected = fetchReport.rejectedCount,
            displayed = filtered.size,
            duplicatesRemoved = rawHotspots.size - deduplicated.size,
            latestTimestamp = latestAcqTimestamp,
            oldestTimestamp = fetchReport.oldestObservationTime
        )

        return FireNowEngineResult(
            allValidatedHotspots = processedHotspots,
            filteredHotspots = filtered,
            priorityRankedHotspots = ranked,
            latestHotspot = latestHotspot,
            latestObservationTimestamp = latestAcqTimestamp,
            newestDataAgeSeconds = if (latestAcqTimestamp != null) latestFreshness.dataAgeSeconds else null,
            newestDataAgeMinutes = if (latestAcqTimestamp != null) latestFreshness.dataAgeMinutes else null,
            newestDataStatus = latestFreshness.freshnessStatus,
            isFreshEnoughForFireNow = isFreshEnough,
            deliveryLatencyMinutes = fetchReport.deliveryLatencyMinutes,
            deliveryLatencyFormatted = formattedDeliveryLatency,
            dataReceivedTimestamp = repository.getLatestReceivedTimestamp(),
            requestDurationMs = requestDuration,
            serverTimeOffsetMs = serverTimeOffsetMs,
            clockSyncWarning = null,
            dataIntegrityReport = integrity,
            isDataStalled = isStalled,
            stallWarningMessage = stallMsg,
            isDeliveryLatencyHigh = isLatencyHigh,
            deliveryLatencyWarningMessage = latencyHighMsg,
            isDataGapDetected = isGapDetected,
            dataGapMessage = gapMsg,
            fetchReport = fetchReport,
            detectedEvents = detectionResult.newEvents,
            newHotspotsCount = detectionResult.newHotspotsCount,
            updatedHotspotsCount = detectionResult.updatedHotspotsCount,
            hotspotGroups = hotspotGroups,
            densityInfo = densityInfo,
            temporalChange = temporalChange,
            dataQualityReport = qualityReport,
            sourceComparison = sourceComparison,
            generatedAlerts = alerts
        )
    }
}
