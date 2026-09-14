package com.example.data.repository

import com.example.data.api.ApiResponse
import com.example.data.api.FireApiService
import com.example.data.api.HealthCheckReport
import com.example.data.api.NasaFirmsParser
import com.example.data.engine.ClockSyncEngine
import com.example.data.engine.FireDuplicateDetector
import com.example.data.local.AuditLogEntity
import com.example.data.local.FireDatabase
import com.example.data.local.HotspotEntity
import com.example.data.model.DataAuditLog
import com.example.data.model.Hotspot
import com.example.data.model.UserLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

data class FetchReport(
    val isSuccess: Boolean,
    val isFromCache: Boolean,
    val cacheAgeMinutes: Long?,
    val totalReceived: Int,
    val validCount: Int,
    val rejectedCount: Int,
    val rejectReason: String?,
    val latestObservationTime: Long?,
    val oldestObservationTime: Long?,
    val latestDataAgeMinutes: Long?,
    val deliveryLatencyMinutes: Long?,
    val errorMessage: String? = null,
    val sourceUsed: String,
    val serverTimeOffsetMs: Long = 0L,
    val clockSyncWarning: String? = null,
    val duplicatesRemoved: Int = 0
)

class FireRepository(
    private val apiService: FireApiService,
    private val database: FireDatabase
) {
    private val hotspotDao = database.hotspotDao()
    private val auditDao = database.auditLogDao()

    suspend fun testDataSource(
        mapKey: String?,
        source: String = "VIIRS_NOAA21_NRT"
    ): HealthCheckReport = withContext(Dispatchers.IO) {
        apiService.testDataSource(mapKey, source)
    }

    fun observeHotspots(userLocation: UserLocation?): Flow<List<Hotspot>> {
        return hotspotDao.getAllHotspotsFlow().map { entities ->
            val now = System.currentTimeMillis()
            entities.map { FireDataMapper.entityToDomain(it, userLocation, now) }
                .sortedWith(
                    compareBy<Hotspot> { it.satelliteAgeMinutes }
                        .thenBy { it.distanceFromUser ?: Double.MAX_VALUE }
                )
        }
    }

    suspend fun getCachedHotspots(userLocation: UserLocation?): List<Hotspot> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        hotspotDao.getAllHotspots().map { FireDataMapper.entityToDomain(it, userLocation, now) }
            .sortedWith(
                compareBy<Hotspot> { it.satelliteAgeMinutes }
                    .thenBy { it.distanceFromUser ?: Double.MAX_VALUE }
            )
    }

    suspend fun getLatestReceivedTimestamp(): Long? = withContext(Dispatchers.IO) {
        hotspotDao.getLatestReceivedTimestamp()
    }

    suspend fun fetchHotspots(
        userLocation: UserLocation?,
        mapKey: String?,
        source: String = "VIIRS_NOAA21_NRT",
        preferAreaQuery: Boolean = true
    ): FetchReport = withContext(Dispatchers.IO) {
        val cleanKey = mapKey?.trim().orEmpty()
        val requestTime = System.currentTimeMillis()
        val hasKey = cleanKey.isNotEmpty()

        val apiResponse: ApiResponse<String> = if (hasKey && preferAreaQuery && userLocation != null) {
            // Area Bounding box around user: ~2.5 degrees (~275 km radius)
            val delta = 2.5
            val west = (userLocation.longitude - delta).coerceIn(-180.0, 180.0)
            val east = (userLocation.longitude + delta).coerceIn(-180.0, 180.0)
            val south = (userLocation.latitude - delta).coerceIn(-90.0, 90.0)
            val north = (userLocation.latitude + delta).coerceIn(-90.0, 90.0)
            apiService.fetchAreaCsv(
                mapKey = cleanKey,
                source = source,
                west = west,
                south = south,
                east = east,
                north = north,
                dayRange = 1
            )
        } else if (hasKey && preferAreaQuery && userLocation == null) {
            // Indonesia regional bounding box (lat -11 to 6, lon 95 to 141)
            apiService.fetchCountryCsv(
                mapKey = cleanKey,
                source = source,
                countryCode = "IDN",
                dayRange = 1
            )
        } else {
            // Direct Open NRT Feed from NASA FIRMS
            apiService.fetchOpenNrtFeed(source)
        }

        when (apiResponse) {
            is ApiResponse.Success -> {
                val dataReceivedTimestamp = apiResponse.responseTime
                val parseResult = NasaFirmsParser.parseCsv(
                    csvContent = apiResponse.data,
                    sourceName = "NASA FIRMS ($source)",
                    dataReceivedTimestamp = dataReceivedTimestamp
                )

                // Feature 127: Deduplicate identical records
                val deduplication = FireDuplicateDetector.deduplicateEntities(parseResult.validHotspots)
                val finalEntities = deduplication.uniqueRecords
                val duplicatesRemoved = deduplication.duplicatesRemovedCount

                // Feature 105 & 106: Clock Sync evaluation
                val clockSync = ClockSyncEngine.evaluateClockSync(
                    serverTimestamp = apiResponse.serverTimestamp,
                    deviceResponseTime = apiResponse.responseTime
                )

                val deliveryLatency = if (parseResult.latestAcquisitionTimestamp != null) {
                    val lat = (dataReceivedTimestamp - parseResult.latestAcquisitionTimestamp) / (60 * 1000)
                    if (lat >= 0) lat else null
                } else {
                    null
                }

                val now = System.currentTimeMillis()
                val latestAge = if (parseResult.latestAcquisitionTimestamp != null) {
                    FireAgeCalculator.calculateAgeMinutes(parseResult.latestAcquisitionTimestamp, now)
                } else {
                    null
                }

                // Simpan ke database jika valid
                if (finalEntities.isNotEmpty()) {
                    hotspotDao.deleteAll()
                    hotspotDao.insertAll(finalEntities)
                } else if (parseResult.validCount == 0 && parseResult.totalReceived == 0) {
                    // Berhasil terhubung ke satelit tetapi tidak ada thermal hotspot aktif di area
                    hotspotDao.deleteAll()
                }

                // Log audit ke database
                val auditLog = AuditLogEntity(
                    requestTime = apiResponse.requestTime,
                    responseTime = apiResponse.responseTime,
                    source = "NASA FIRMS ($source)",
                    endpointUrl = apiResponse.url,
                    httpStatusCode = apiResponse.httpStatusCode,
                    isSuccess = true,
                    errorMessage = null,
                    numberReceived = parseResult.totalReceived,
                    numberValid = parseResult.validCount,
                    numberRejected = parseResult.rejectedCount,
                    rejectReason = parseResult.rejectReasons.joinToString("; ").takeIf { it.isNotBlank() },
                    latestAcquisitionTimestamp = parseResult.latestAcquisitionTimestamp,
                    oldestAcquisitionTimestamp = parseResult.oldestAcquisitionTimestamp,
                    latestSatellite = parseResult.latestSatellite,
                    latestDataAgeMinutes = latestAge,
                    deliveryLatencyMinutes = deliveryLatency
                )
                auditDao.insert(auditLog)

                FetchReport(
                    isSuccess = true,
                    isFromCache = false,
                    cacheAgeMinutes = null,
                    totalReceived = parseResult.totalReceived,
                    validCount = finalEntities.size,
                    rejectedCount = parseResult.rejectedCount,
                    rejectReason = parseResult.rejectReasons.firstOrNull(),
                    latestObservationTime = parseResult.latestAcquisitionTimestamp,
                    oldestObservationTime = parseResult.oldestAcquisitionTimestamp,
                    latestDataAgeMinutes = latestAge,
                    deliveryLatencyMinutes = deliveryLatency,
                    errorMessage = null,
                    sourceUsed = "NASA FIRMS ($source)",
                    serverTimeOffsetMs = clockSync.serverTimeOffsetMs,
                    clockSyncWarning = clockSync.syncWarningMessage,
                    duplicatesRemoved = duplicatesRemoved
                )
            }

            is ApiResponse.Error -> {
                // Log kegagalan ke database audit
                val auditLog = AuditLogEntity(
                    requestTime = apiResponse.requestTime,
                    responseTime = apiResponse.responseTime,
                    source = "NASA FIRMS ($source)",
                    endpointUrl = apiResponse.url,
                    httpStatusCode = apiResponse.httpStatusCode,
                    isSuccess = false,
                    errorMessage = apiResponse.errorMessage,
                    numberReceived = 0,
                    numberValid = 0,
                    numberRejected = 0,
                    rejectReason = apiResponse.errorMessage,
                    latestAcquisitionTimestamp = null,
                    oldestAcquisitionTimestamp = null,
                    latestSatellite = null,
                    latestDataAgeMinutes = null,
                    deliveryLatencyMinutes = null
                )
                auditDao.insert(auditLog)

                // Cek apakah ada cache di database
                val cached = hotspotDao.getAllHotspots()
                val latestCachedTime = hotspotDao.getLatestReceivedTimestamp()
                val cacheAgeMinutes = if (latestCachedTime != null) {
                    (System.currentTimeMillis() - latestCachedTime) / (60 * 1000)
                } else null

                FetchReport(
                    isSuccess = false,
                    isFromCache = cached.isNotEmpty(),
                    cacheAgeMinutes = cacheAgeMinutes,
                    totalReceived = 0,
                    validCount = cached.size,
                    rejectedCount = 0,
                    rejectReason = null,
                    latestObservationTime = hotspotDao.getLatestAcquisitionTimestamp(),
                    oldestObservationTime = null,
                    latestDataAgeMinutes = if (hotspotDao.getLatestAcquisitionTimestamp() != null) {
                        FireAgeCalculator.calculateAgeMinutes(hotspotDao.getLatestAcquisitionTimestamp()!!)
                    } else null,
                    deliveryLatencyMinutes = null,
                    errorMessage = apiResponse.errorMessage,
                    sourceUsed = "NASA FIRMS ($source)"
                )
            }
        }
    }

    suspend fun getRecentAuditLogs(): List<DataAuditLog> = withContext(Dispatchers.IO) {
        auditDao.getRecentLogs().map { entity ->
            DataAuditLog(
                id = entity.id,
                requestTime = entity.requestTime,
                responseTime = entity.responseTime,
                source = entity.source,
                endpointUrl = entity.endpointUrl,
                httpStatusCode = entity.httpStatusCode,
                isSuccess = entity.isSuccess,
                errorMessage = entity.errorMessage,
                numberReceived = entity.numberReceived,
                numberValid = entity.numberValid,
                numberRejected = entity.numberRejected,
                rejectReason = entity.rejectReason,
                latestAcquisitionTimestamp = entity.latestAcquisitionTimestamp,
                oldestAcquisitionTimestamp = entity.oldestAcquisitionTimestamp,
                latestSatellite = entity.latestSatellite,
                latestDataAgeMinutes = entity.latestDataAgeMinutes,
                deliveryLatencyMinutes = entity.deliveryLatencyMinutes
            )
        }
    }

    suspend fun clearAllAuditLogs() = withContext(Dispatchers.IO) {
        auditDao.deleteAll()
    }
}
