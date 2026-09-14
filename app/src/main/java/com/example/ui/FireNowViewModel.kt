package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.FireApiService
import com.example.data.api.HealthCheckReport
import com.example.data.engine.AlertEngineConfig
import com.example.data.engine.ClockSyncEngine
import com.example.data.engine.CurrentDataGate
import com.example.data.engine.DataFreshnessEngine
import com.example.data.engine.DataIntegrityReport
import com.example.data.engine.DataIntegrityTracker
import com.example.data.engine.DataStallDetector
import com.example.data.engine.FireAlertEngine
import com.example.data.engine.FireNowEngine
import com.example.data.engine.GeospatialIntelligenceEngine
import com.example.data.engine.HotspotWithPriority
import com.example.data.engine.LatestObservationEngine
import com.example.data.local.FireDatabase
import com.example.data.location.LocationHelper
import com.example.data.model.DataAuditLog
import com.example.data.model.DataQualityReport
import com.example.data.model.FireAlert
import com.example.data.model.FireNowPriorityResult
import com.example.data.model.FireNowPriorityScore
import com.example.data.model.FreshnessConfig
import com.example.data.model.Hotspot
import com.example.data.model.HotspotAgeStatus
import com.example.data.model.HotspotDensityInfo
import com.example.data.model.HotspotEvent
import com.example.data.model.HotspotGroup
import com.example.data.model.MonitoringSession
import com.example.data.model.RealTimeClaimGuard
import com.example.data.model.SavedWatchArea
import com.example.data.model.SourceComparisonReport
import com.example.data.model.TemporalChangeResult
import com.example.data.model.UserLocation
import com.example.data.repository.DistanceCalculator
import com.example.data.repository.FetchReport
import com.example.data.repository.FireAgeCalculator
import com.example.data.repository.FireRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Feature 123: HOTSPOT TIME WINDOW
 * Choices: 30 MENIT, 1 JAM, 3 JAM, 6 JAM, 12 JAM, 24 JAM, SEMUA
 */
enum class AgeFilter(val label: String, val maxMinutes: Long) {
    THIRTY_MINUTES("30 MENIT", 30),
    ONE_HOUR("1 JAM", 60),
    THREE_HOURS("3 JAM", 180),
    SIX_HOURS("6 JAM", 360),
    TWELVE_HOURS("12 JAM", 720),
    TWENTY_FOUR_HOURS("24 JAM", 1440),
    ALL("SEMUA", Long.MAX_VALUE)
}

enum class MapLayerType(val label: String) {
    SATELLITE("SATELLITE"),
    HYBRID("HYBRID"),
    ROAD("ROAD"),
    TERRAIN("TERRAIN")
}

data class FireNowUiState(
    val isLoading: Boolean = false,
    val isLocating: Boolean = false,
    val userLocation: UserLocation? = null,
    val customSearchLocation: UserLocation? = null,
    val locationError: String? = null,
    val isLocationStale: Boolean = false,
    val isLocationAccuracyLow: Boolean = false,
    val locationAgeMinutes: Long? = null,
    val allHotspots: List<Hotspot> = emptyList(),
    val filteredHotspots: List<Hotspot> = emptyList(),
    val priorityRankedHotspots: List<HotspotWithPriority> = emptyList(),
    val selectedAgeFilter: AgeFilter = AgeFilter.THIRTY_MINUTES,
    val selectedDistanceFilterKm: Double = 25.0, // Default 25 km
    val selectedSource: String = "VIIRS_NOAA21_NRT",
    val autoRefreshMinutes: Int = 0, // 0 = OFF
    val mapLayerType: MapLayerType = MapLayerType.SATELLITE,
    val selectedHotspot: Hotspot? = null,
    val lastCheckTime: Long? = null,
    val lastReceivedTime: Long? = null,
    val latestObservationTime: Long? = null,
    val latestSatelliteName: String? = null,
    val deliveryLatencyMinutes: Long? = null,
    val deliveryLatencyFormatted: String = "Tidak tersedia",
    val newestDataAgeMinutes: Long? = null,
    val newestDataAgeSeconds: Long? = null,
    val newestDataAgeFormatted: String = "Memuat...",
    val newestDataStatus: HotspotAgeStatus = HotspotAgeStatus.UNKNOWN,
    val isFromCache: Boolean = false,
    val cacheAgeMinutes: Long? = null,
    val errorMessage: String? = null,
    val lastFetchReport: FetchReport? = null,
    val nasaMapKey: String = "",
    val auditLogs: List<DataAuditLog> = emptyList(),
    val showAuditDialog: Boolean = false,
    val showSettingsDialog: Boolean = false,
    val showSearchCoordinateDialog: Boolean = false,
    val showSearchPlaceDialog: Boolean = false,
    val showIntegrityDialog: Boolean = false,
    val showHealthCheckDialog: Boolean = false,
    val showNowOnlyMode: Boolean = true, // Feature 174: Default <=30m mode
    val healthCheckReport: HealthCheckReport? = null,
    val isRunningHealthCheck: Boolean = false,
    val serverTimeOffsetMs: Long = 0L,
    val clockSyncWarning: String? = null,
    val dataIntegrityReport: DataIntegrityReport? = null,
    val isDataStalled: Boolean = false,
    val stallWarningMessage: String? = null,
    val isDeliveryLatencyHigh: Boolean = false,
    val deliveryLatencyWarningMessage: String? = null,
    val isDataGapDetected: Boolean = false,
    val dataGapMessage: String? = null,
    val freshnessConfig: FreshnessConfig = FreshnessConfig.DEFAULT,
    val multiSatelliteSummary: Map<String, Long> = emptyMap(), // Satellite -> latest observation time
    // Tahap 3 States
    val detectedEvents: List<HotspotEvent> = emptyList(),
    val newHotspotsCount: Int = 0,
    val updatedHotspotsCount: Int = 0,
    val hotspotGroups: List<HotspotGroup> = emptyList(),
    val densityInfo: HotspotDensityInfo? = null,
    val temporalChange: TemporalChangeResult? = null,
    val dataQualityReport: DataQualityReport? = null,
    val sourceComparison: SourceComparisonReport? = null,
    val monitoringSession: MonitoringSession? = null,
    val savedWatchAreas: List<SavedWatchArea> = listOf(
        SavedWatchArea("AREA_MANTANGAI", "Blok Mantangai (Kapuas)", -2.3500, 114.4800, 25.0),
        SavedWatchArea("AREA_PALANGKA", "Palangka Raya Sekitar", -2.2100, 113.9200, 25.0),
        SavedWatchArea("AREA_TIMPAH", "Kawasan Timpah", -2.0150, 114.6100, 20.0)
    ),
    val activeWatchAreaId: String? = null,
    val alertHistory: List<FireAlert> = emptyList(),
    val alertEngineConfig: AlertEngineConfig = AlertEngineConfig(),
    val showAlertHistoryDialog: Boolean = false,
    val showWatchAreasDialog: Boolean = false,
    val showPlaybackDialog: Boolean = false,
    val showFieldDashboardDialog: Boolean = false
) {
    /**
     * Feature 137: Map Center Hierarchy
     * 1. GPS actual (userLocation)
     * 2. User search location (customSearchLocation)
     * 3. Fallback center (Central Kalimantan: Mantangai coords, clearly labeled)
     */
    val effectiveCenterLatitude: Double
        get() = userLocation?.latitude ?: customSearchLocation?.latitude ?: -2.3500

    val effectiveCenterLongitude: Double
        get() = userLocation?.longitude ?: customSearchLocation?.longitude ?: 114.4800

    val effectiveCenterLabel: String
        get() = when {
            userLocation != null -> "📍 Posisi GPS Saya"
            customSearchLocation != null -> customSearchLocation.provider ?: "📍 Lokasi Pencarian"
            else -> "📍 Mantangai, Kapuas (Pusat Default)"
        }

    /**
     * Feature 141: REAL DATA BADGE
     * 🛰️ VERIFIED SOURCE DATA / ⚠️ CACHE DATA / ⚫ NO DATA
     */
    val realDataBadgeText: String
        get() = when {
            filteredHotspots.isNotEmpty() && !isFromCache -> "🛰️ VERIFIED SOURCE DATA"
            isFromCache -> "⚠️ CACHE DATA"
            else -> "⚫ NO DATA"
        }

    /**
     * Feature 152: LATEST SATELLITE BADGE
     * e.g. 🛰️ DATA TERBARU: VIIRS NOAA-21
     */
    val latestSatelliteBadge: String
        get() = if (latestSatelliteName != null) {
            "🛰️ DATA TERBARU: $latestSatelliteName"
        } else {
            "🛰️ SATELIT: NASA FIRMS"
        }

    /**
     * Feature 171 & 172: Real-time claim guard header
     */
    val honestHeaderStatus: String
        get() = RealTimeClaimGuard.getHonestBadgeTitle(newestDataAgeMinutes)
}

class FireNowViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("hardi_fire_prefs", Context.MODE_PRIVATE)
    private val database = FireDatabase.getInstance(application)
    private val apiService = FireApiService()
    private val repository = FireRepository(apiService, database)
    private val alertEngine = FireAlertEngine(application)
    private val fireNowEngine = FireNowEngine(repository, alertEngine)
    private val locationHelper = LocationHelper(application)

    private val _uiState = MutableStateFlow(
        FireNowUiState(
            nasaMapKey = prefs.getString("nasa_map_key", "").orEmpty()
        )
    )
    val uiState: StateFlow<FireNowUiState> = _uiState.asStateFlow()

    private var autoRefreshJob: Job? = null
    private var secondTickerJob: Job? = null
    private var monitoringJob: Job? = null

    init {
        // Load initial cached hotspots if any
        viewModelScope.launch {
            val cached = repository.getCachedHotspots(null)
            val latestRec = repository.getLatestReceivedTimestamp()
            val cacheAge = if (latestRec != null) (System.currentTimeMillis() - latestRec) / (60 * 1000) else null
            if (cached.isNotEmpty()) {
                _uiState.update {
                    it.copy(
                        allHotspots = cached,
                        isFromCache = true,
                        cacheAgeMinutes = cacheAge,
                        lastReceivedTime = latestRec
                    )
                }
                recalculateState()
            }
        }

        // Observe real-time DB changes
        viewModelScope.launch {
            repository.observeHotspots(_uiState.value.userLocation).collect { list ->
                _uiState.update { it.copy(allHotspots = list) }
                recalculateState()
            }
        }

        // Start 1-second ticker for second-level accuracy (Feature 104)
        startSecondTicker()
    }

    private fun startSecondTicker() {
        secondTickerJob?.cancel()
        secondTickerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000L)
                updateSecondsTicker()
            }
        }
    }

    private fun updateSecondsTicker() {
        val state = _uiState.value
        val latestObs = state.latestObservationTime ?: return
        val now = System.currentTimeMillis()

        val freshness = DataFreshnessEngine.calculateFreshness(
            acquisitionTimestamp = latestObs,
            currentTimestamp = now,
            serverTimeOffsetMs = state.serverTimeOffsetMs,
            config = state.freshnessConfig
        )

        _uiState.update {
            it.copy(
                newestDataAgeSeconds = freshness.dataAgeSeconds,
                newestDataAgeMinutes = freshness.dataAgeMinutes,
                newestDataAgeFormatted = freshness.formattedAgeFull,
                newestDataStatus = freshness.freshnessStatus
            )
        }
    }

    fun requestLocationAndRefresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLocating = true, locationError = null) }
            val location = locationHelper.getCurrentLocation()
            if (location != null) {
                val locAgeMin = (System.currentTimeMillis() - location.timestamp) / (60 * 1000)
                val isStale = locAgeMin > 15L // Feature 134: Location stale if > 15 min
                val isAccLow = location.accuracyMeters > 100f // Feature 135: Accuracy low if > 100m

                _uiState.update {
                    it.copy(
                        userLocation = location,
                        isLocating = false,
                        locationError = null,
                        isLocationStale = isStale,
                        isLocationAccuracyLow = isAccLow,
                        locationAgeMinutes = locAgeMin
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLocating = false,
                        locationError = "LOKASI PENGGUNA BELUM TERSEDIA (Pastikan izin lokasi dan GPS aktif)"
                    )
                }
            }
            refreshData()
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            val state = _uiState.value
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    lastCheckTime = System.currentTimeMillis()
                )
            }

            val engineResult = fireNowEngine.execute(
                userLocation = state.userLocation ?: state.customSearchLocation,
                mapKey = state.nasaMapKey.takeIf { it.isNotBlank() },
                source = state.selectedSource,
                maxAgeMinutes = state.selectedAgeFilter.maxMinutes,
                maxRadiusKm = state.selectedDistanceFilterKm,
                isLocalFireNowMode = state.showNowOnlyMode,
                freshnessConfig = state.freshnessConfig,
                serverTimeOffsetMs = state.serverTimeOffsetMs,
                alertEngineConfig = state.alertEngineConfig
            )

            // Compute multi-satellite comparison (Feature 151)
            val satMap = mutableMapOf<String, Long>()
            for (hs in engineResult.allValidatedHotspots) {
                val curr = satMap[hs.satellite]
                if (curr == null || hs.acquisitionTimestamp > curr) {
                    satMap[hs.satellite] = hs.acquisitionTimestamp
                }
            }

            // Update monitoring session health if active (Feature 211-213)
            val nowMs = System.currentTimeMillis()
            val updatedSession = state.monitoringSession?.let { s ->
                if (s.isActive) {
                    val isSuccess = engineResult.fetchReport.isSuccess
                    s.copy(
                        checksExecuted = s.checksExecuted + 1,
                        lastCheckTime = nowMs,
                        lastSuccessfulUpdateTime = if (isSuccess) nowMs else s.lastSuccessfulUpdateTime,
                        missedChecksCount = if (!isSuccess) s.missedChecksCount + 1 else s.missedChecksCount,
                        lastMissedReason = if (!isSuccess) engineResult.fetchReport.errorMessage else null
                    )
                } else s
            }

            _uiState.update { current ->
                current.copy(
                    isLoading = false,
                    allHotspots = engineResult.allValidatedHotspots,
                    filteredHotspots = engineResult.filteredHotspots,
                    priorityRankedHotspots = engineResult.priorityRankedHotspots,
                    latestObservationTime = engineResult.latestObservationTimestamp,
                    latestSatelliteName = engineResult.latestHotspot?.satellite ?: state.selectedSource,
                    newestDataAgeSeconds = engineResult.newestDataAgeSeconds,
                    newestDataAgeMinutes = engineResult.newestDataAgeMinutes,
                    newestDataStatus = engineResult.newestDataStatus,
                    deliveryLatencyMinutes = engineResult.deliveryLatencyMinutes,
                    deliveryLatencyFormatted = engineResult.deliveryLatencyFormatted,
                    lastReceivedTime = engineResult.dataReceivedTimestamp ?: current.lastReceivedTime,
                    lastFetchReport = engineResult.fetchReport,
                    isFromCache = engineResult.fetchReport.isFromCache,
                    cacheAgeMinutes = engineResult.fetchReport.cacheAgeMinutes,
                    errorMessage = if (!engineResult.fetchReport.isSuccess) engineResult.fetchReport.errorMessage else null,
                    serverTimeOffsetMs = engineResult.serverTimeOffsetMs,
                    clockSyncWarning = engineResult.clockSyncWarning,
                    dataIntegrityReport = engineResult.dataIntegrityReport,
                    isDataStalled = engineResult.isDataStalled,
                    stallWarningMessage = engineResult.stallWarningMessage,
                    isDeliveryLatencyHigh = engineResult.isDeliveryLatencyHigh,
                    deliveryLatencyWarningMessage = engineResult.deliveryLatencyWarningMessage,
                    isDataGapDetected = engineResult.isDataGapDetected,
                    dataGapMessage = engineResult.dataGapMessage,
                    multiSatelliteSummary = satMap,
                    // Tahap 3 result propagation
                    detectedEvents = engineResult.detectedEvents,
                    newHotspotsCount = engineResult.newHotspotsCount,
                    updatedHotspotsCount = engineResult.updatedHotspotsCount,
                    hotspotGroups = engineResult.hotspotGroups,
                    densityInfo = engineResult.densityInfo,
                    temporalChange = engineResult.temporalChange,
                    dataQualityReport = engineResult.dataQualityReport,
                    sourceComparison = engineResult.sourceComparison,
                    monitoringSession = updatedSession,
                    alertHistory = alertEngine.getAlertHistory()
                )
            }

            updateSecondsTicker()
        }
    }

    /**
     * Feature 138: Search Coordinate
     */
    fun searchCoordinate(lat: Double, lon: Double, label: String = "Pencarian Koordinat") {
        if (lat < -90.0 || lat > 90.0 || lon < -180.0 || lon > 180.0) return
        val customLoc = UserLocation(
            latitude = lat,
            longitude = lon,
            accuracyMeters = 10f,
            timestamp = System.currentTimeMillis(),
            provider = label
        )
        _uiState.update { it.copy(customSearchLocation = customLoc) }
        recalculateState()
    }

    /**
     * Feature 139: Preset Places (Central Kalimantan / Indonesia)
     */
    fun selectPresetPlace(lat: Double, lon: Double, placeName: String) {
        searchCoordinate(lat, lon, placeName)
    }

    fun resetToGpsLocation() {
        _uiState.update { it.copy(customSearchLocation = null) }
        requestLocationAndRefresh()
    }

    /**
     * Feature 143: Active API Health Check
     */
    fun runHealthCheck() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRunningHealthCheck = true) }
            val report = repository.testDataSource(
                mapKey = _uiState.value.nasaMapKey.takeIf { it.isNotBlank() },
                source = _uiState.value.selectedSource
            )
            _uiState.update {
                it.copy(
                    isRunningHealthCheck = false,
                    healthCheckReport = report,
                    showHealthCheckDialog = true
                )
            }
        }
    }

    fun setAgeFilter(filter: AgeFilter) {
        _uiState.update { it.copy(selectedAgeFilter = filter) }
        recalculateState()
    }

    fun setDistanceFilter(distanceKm: Double) {
        _uiState.update { it.copy(selectedDistanceFilterKm = distanceKm) }
        recalculateState()
    }

    fun setSource(source: String) {
        _uiState.update { it.copy(selectedSource = source) }
        refreshData()
    }

    fun setMapLayerType(layerType: MapLayerType) {
        _uiState.update { it.copy(mapLayerType = layerType) }
    }

    fun selectHotspot(hotspot: Hotspot?) {
        _uiState.update { it.copy(selectedHotspot = hotspot) }
    }

    fun toggleNowOnlyMode(enable: Boolean) {
        _uiState.update {
            it.copy(
                showNowOnlyMode = enable,
                selectedAgeFilter = if (enable) AgeFilter.THIRTY_MINUTES else it.selectedAgeFilter
            )
        }
        recalculateState()
    }

    fun setAutoRefresh(minutes: Int) {
        _uiState.update { it.copy(autoRefreshMinutes = minutes) }
        autoRefreshJob?.cancel()
        if (minutes > 0) {
            autoRefreshJob = viewModelScope.launch {
                while (isActive) {
                    delay(minutes * 60 * 1000L)
                    refreshData()
                }
            }
        }
    }

    fun saveNasaMapKey(key: String) {
        prefs.edit().putString("nasa_map_key", key.trim()).apply()
        _uiState.update { it.copy(nasaMapKey = key.trim()) }
    }

    fun setShowAuditDialog(show: Boolean) {
        if (show) {
            viewModelScope.launch {
                val logs = repository.getRecentAuditLogs()
                _uiState.update { it.copy(auditLogs = logs, showAuditDialog = true) }
            }
        } else {
            _uiState.update { it.copy(showAuditDialog = false) }
        }
    }

    fun setShowSettingsDialog(show: Boolean) {
        _uiState.update { it.copy(showSettingsDialog = show) }
    }

    fun setShowSearchCoordinateDialog(show: Boolean) {
        _uiState.update { it.copy(showSearchCoordinateDialog = show) }
    }

    fun setShowSearchPlaceDialog(show: Boolean) {
        _uiState.update { it.copy(showSearchPlaceDialog = show) }
    }

    fun setShowIntegrityDialog(show: Boolean) {
        _uiState.update { it.copy(showIntegrityDialog = show) }
    }

    fun setShowHealthCheckDialog(show: Boolean) {
        _uiState.update { it.copy(showHealthCheckDialog = show) }
    }

    fun clearAuditLogs() {
        viewModelScope.launch {
            repository.clearAllAuditLogs()
            _uiState.update { it.copy(auditLogs = emptyList()) }
        }
    }

    /**
     * Feature 206–210: CONTINUOUS AREA MONITORING SESSIONS
     */
    fun startMonitoring(
        radiusKm: Double = 25.0,
        freshnessThreshold: Long = 30L,
        intervalMinutes: Int = 5
    ) {
        val state = _uiState.value
        val center = state.userLocation ?: state.customSearchLocation
        val session = MonitoringSession(
            sessionId = "SES_${System.currentTimeMillis()}",
            sessionStartedAt = System.currentTimeMillis(),
            isActive = true,
            locationName = state.effectiveCenterLabel,
            latitude = center?.latitude ?: -2.3500,
            longitude = center?.longitude ?: 114.4800,
            radiusKm = radiusKm,
            freshnessThresholdMinutes = freshnessThreshold,
            source = state.selectedSource,
            checkIntervalMinutes = intervalMinutes,
            lastCheckTime = System.currentTimeMillis()
        )

        _uiState.update { it.copy(monitoringSession = session) }

        // Start periodic monitoring loop
        monitoringJob?.cancel()
        monitoringJob = viewModelScope.launch {
            while (isActive) {
                delay(intervalMinutes * 60 * 1000L)
                refreshData()
            }
        }
        refreshData()
    }

    /**
     * Feature 207 & 208: STOP MONITORING
     */
    fun stopMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = null
        _uiState.update { current ->
            val ended = current.monitoringSession?.copy(
                isActive = false,
                sessionEndedAt = System.currentTimeMillis()
            )
            current.copy(monitoringSession = ended)
        }
    }

    /**
     * Feature 214 & 215: SAVED WATCH AREAS
     */
    fun saveWatchArea(name: String, lat: Double, lon: Double, radiusKm: Double) {
        val newArea = SavedWatchArea(
            id = "AREA_${System.currentTimeMillis()}",
            name = name,
            latitude = lat,
            longitude = lon,
            radiusKm = radiusKm
        )
        _uiState.update { it.copy(savedWatchAreas = it.savedWatchAreas + newArea) }
    }

    /**
     * Feature 218: AREA SWITCHING (recalculates dataset)
     */
    fun selectWatchArea(area: SavedWatchArea?) {
        if (area == null) {
            // Kembali ke GPS
            _uiState.update { it.copy(activeWatchAreaId = null, customSearchLocation = null) }
            resetToGpsLocation()
        } else {
            val customLoc = UserLocation(
                latitude = area.latitude,
                longitude = area.longitude,
                accuracyMeters = 10f,
                timestamp = System.currentTimeMillis(),
                provider = area.name
            )
            _uiState.update {
                it.copy(
                    activeWatchAreaId = area.id,
                    customSearchLocation = customLoc,
                    selectedDistanceFilterKm = area.radiusKm
                )
            }
            recalculateState()
        }
    }

    fun deleteWatchArea(id: String) {
        _uiState.update { current ->
            current.copy(
                savedWatchAreas = current.savedWatchAreas.filterNot { it.id == id },
                activeWatchAreaId = if (current.activeWatchAreaId == id) null else current.activeWatchAreaId
            )
        }
    }

    /**
     * Feature 233: CLEAR ALERT HISTORY
     */
    fun clearAlertHistory() {
        alertEngine.clearAlertHistory()
        _uiState.update { it.copy(alertHistory = emptyList()) }
    }

    // Dialog Toggles for Tahap 3
    fun setShowAlertHistoryDialog(show: Boolean) {
        _uiState.update { it.copy(showAlertHistoryDialog = show) }
    }

    fun setShowWatchAreasDialog(show: Boolean) {
        _uiState.update { it.copy(showWatchAreasDialog = show) }
    }

    fun setShowPlaybackDialog(show: Boolean) {
        _uiState.update { it.copy(showPlaybackDialog = show) }
    }

    fun setShowFieldDashboardDialog(show: Boolean) {
        _uiState.update { it.copy(showFieldDashboardDialog = show) }
    }

    private fun recalculateState() {
        val state = _uiState.value
        val now = System.currentTimeMillis()
        val center = state.userLocation ?: state.customSearchLocation

        // Recalculate distance & age for all hotspots
        val updatedHotspots = state.allHotspots.map { hs ->
            val dist = if (center != null) {
                DistanceCalculator.calculateHaversineDistanceKm(
                    center.latitude,
                    center.longitude,
                    hs.latitude,
                    hs.longitude
                )
            } else null

            val freshness = DataFreshnessEngine.calculateFreshness(
                acquisitionTimestamp = hs.acquisitionTimestamp,
                currentTimestamp = now,
                serverTimeOffsetMs = state.serverTimeOffsetMs,
                config = state.freshnessConfig
            )

            val priority = FireNowPriorityScore.calculate(
                ageMinutes = freshness.dataAgeMinutes,
                distanceKm = dist,
                confidence = hs.confidence,
                brightness = hs.brightness
            )

            hs.copy(
                distanceFromUser = dist,
                satelliteAgeMinutes = freshness.dataAgeMinutes,
                status = freshness.freshnessStatus,
                priorityResult = priority
            )
        }

        val filtered = updatedHotspots.filter { hs ->
            val matchesAge = hs.satelliteAgeMinutes <= state.selectedAgeFilter.maxMinutes
            val matchesRadius = if (state.showNowOnlyMode && center != null && hs.distanceFromUser != null) {
                hs.distanceFromUser <= state.selectedDistanceFilterKm
            } else {
                true
            }
            matchesAge && matchesRadius
        }

        val ranked = filtered.map { hs ->
            val priority = hs.priorityResult ?: FireNowPriorityScore.calculate(
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

        val newestHotspot = LatestObservationEngine.findLatestHotspot(updatedHotspots)

        val integrity = DataIntegrityTracker.generateReport(
            received = state.lastFetchReport?.totalReceived ?: updatedHotspots.size,
            valid = updatedHotspots.size,
            rejected = state.lastFetchReport?.rejectedCount ?: 0,
            displayed = filtered.size,
            latestTimestamp = newestHotspot?.acquisitionTimestamp,
            oldestTimestamp = LatestObservationEngine.findOldestHotspot(updatedHotspots)?.acquisitionTimestamp
        )

        _uiState.update {
            it.copy(
                allHotspots = updatedHotspots,
                filteredHotspots = filtered,
                priorityRankedHotspots = ranked,
                latestObservationTime = newestHotspot?.acquisitionTimestamp ?: it.latestObservationTime,
                latestSatelliteName = newestHotspot?.satellite ?: it.latestSatelliteName,
                dataIntegrityReport = integrity
            )
        }

        updateSecondsTicker()
    }
}
