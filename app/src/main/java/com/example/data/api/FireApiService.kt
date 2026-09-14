package com.example.data.api

import com.example.data.engine.ClockSyncEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

sealed class ApiResponse<out T> {
    data class Success<out T>(
        val data: T,
        val httpStatusCode: Int,
        val requestTime: Long,
        val responseTime: Long,
        val url: String,
        val serverTimestamp: Long? = null
    ) : ApiResponse<T>() {
        val networkLatencyMs: Long
            get() = responseTime - requestTime
    }

    data class Error(
        val httpStatusCode: Int?,
        val errorMessage: String,
        val requestTime: Long,
        val responseTime: Long,
        val url: String,
        val isRateLimited: Boolean = false,
        val retryAfterSeconds: Long? = null
    ) : ApiResponse<Nothing>() {
        val networkLatencyMs: Long
            get() = responseTime - requestTime
    }
}

data class HealthCheckReport(
    val isHealthy: Boolean,
    val httpStatusCode: Int?,
    val latencyMs: Long,
    val endpointUrl: String,
    val recordsCount: Int,
    val serverTimestamp: Long?,
    val summaryMessage: String
)

class FireApiService {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    /**
     * Feature 143: Active API Health Check
     * Tests endpoint, response, schema, and server clock timestamp.
     */
    suspend fun testDataSource(
        mapKey: String?,
        source: String = "VIIRS_NOAA21_NRT"
    ): HealthCheckReport = withContext(Dispatchers.IO) {
        val cleanKey = mapKey?.trim().orEmpty()
        val response = if (cleanKey.isNotEmpty()) {
            fetchCountryCsv(cleanKey, source, "IDN", 1)
        } else {
            fetchOpenNrtFeed(source)
        }

        when (response) {
            is ApiResponse.Success -> {
                val lines = response.data.lines().filter { it.isNotBlank() }
                val header = lines.firstOrNull().orEmpty().lowercase()
                val hasValidHeader = header.contains("latitude") && header.contains("longitude")

                if (hasValidHeader) {
                    val count = (lines.size - 1).coerceAtLeast(0)
                    HealthCheckReport(
                        isHealthy = true,
                        httpStatusCode = response.httpStatusCode,
                        latencyMs = response.networkLatencyMs,
                        endpointUrl = response.url,
                        recordsCount = count,
                        serverTimestamp = response.serverTimestamp,
                        summaryMessage = "✅ SOURCE HEALTHY: Server NASA FIRMS merespons normal (HTTP ${response.httpStatusCode}, Latensi: ${response.networkLatencyMs} ms, $count titik)."
                    )
                } else {
                    HealthCheckReport(
                        isHealthy = false,
                        httpStatusCode = response.httpStatusCode,
                        latencyMs = response.networkLatencyMs,
                        endpointUrl = response.url,
                        recordsCount = 0,
                        serverTimestamp = response.serverTimestamp,
                        summaryMessage = "❌ SOURCE ERROR: Header CSV tidak sesuai spesifikasi FIRMS."
                    )
                }
            }
            is ApiResponse.Error -> {
                HealthCheckReport(
                    isHealthy = false,
                    httpStatusCode = response.httpStatusCode,
                    latencyMs = response.networkLatencyMs,
                    endpointUrl = response.url,
                    recordsCount = 0,
                    serverTimestamp = null,
                    summaryMessage = "❌ SOURCE ERROR: ${response.errorMessage}"
                )
            }
        }
    }

    /**
     * Fetch from FIRMS Area API using MAP_KEY:
     * https://firms.modaps.eosdis.nasa.gov/api/area/csv/[MAP_KEY]/[SOURCE]/[BBOX]/[DAYS]
     */
    suspend fun fetchAreaCsv(
        mapKey: String,
        source: String,
        west: Double,
        south: Double,
        east: Double,
        north: Double,
        dayRange: Int = 1
    ): ApiResponse<String> {
        val bbox = String.format(java.util.Locale.US, "%.4f,%.4f,%.4f,%.4f", west, south, east, north)
        val url = "https://firms.modaps.eosdis.nasa.gov/api/area/csv/$mapKey/$source/$bbox/$dayRange"
        return executeGet(url)
    }

    /**
     * Fetch from FIRMS Country API using MAP_KEY:
     * https://firms.modaps.eosdis.nasa.gov/api/country/csv/[MAP_KEY]/[SOURCE]/IDN/[DAYS]
     */
    suspend fun fetchCountryCsv(
        mapKey: String,
        source: String,
        countryCode: String = "IDN",
        dayRange: Int = 1
    ): ApiResponse<String> {
        val url = "https://firms.modaps.eosdis.nasa.gov/api/country/csv/$mapKey/$source/$countryCode/$dayRange"
        return executeGet(url)
    }

    /**
     * Fetch from NASA FIRMS Open SouthEast Asia NRT Feed (tidak memerlukan MAP_KEY):
     * e.g. NOAA-21, NOAA-20, Suomi-NPP
     */
    suspend fun fetchOpenNrtFeed(source: String): ApiResponse<String> {
        val filename = when (source) {
            "VIIRS_NOAA21_NRT" -> "J2_VIIRS_C2_SouthEast_Asia_NRT_24h.csv"
            "VIIRS_NOAA20_NRT" -> "J1_VIIRS_C2_SouthEast_Asia_NRT_24h.csv"
            "VIIRS_SNPP_NRT" -> "SUOMI_VIIRS_C2_SouthEast_Asia_NRT_24h.csv"
            "MODIS_NRT" -> "MODIS_C6_1_SouthEast_Asia_24h.csv"
            else -> "J2_VIIRS_C2_SouthEast_Asia_NRT_24h.csv"
        }
        val folder = when (source) {
            "VIIRS_NOAA21_NRT" -> "noaa-21-viirs-nrt"
            "VIIRS_NOAA20_NRT" -> "noaa-20-viirs-nrt"
            "VIIRS_SNPP_NRT" -> "suomi-npp-viirs-nrt"
            "MODIS_NRT" -> "modis-c6.1"
            else -> "noaa-21-viirs-nrt"
        }
        val url = "https://firms.modaps.eosdis.nasa.gov/data/active_fire/$folder/csv/$filename"
        return executeGet(url)
    }

    private suspend fun executeGet(url: String): ApiResponse<String> = withContext(Dispatchers.IO) {
        val requestTime = System.currentTimeMillis()
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "HardiMantangaiFireNow/1.0 (Android; NASA FIRMS Client)")
                .get()
                .build()

            val response = client.newCall(request).execute()
            val responseTime = System.currentTimeMillis()
            val statusCode = response.code

            response.use { res ->
                val bodyString = res.body?.string().orEmpty()
                val serverDateHeader = res.header("Date")
                val serverTimestamp = ClockSyncEngine.parseServerDateHeader(serverDateHeader)

                if (res.isSuccessful) {
                    if (bodyString.contains("Invalid MAP_KEY", ignoreCase = true) ||
                        bodyString.contains("Bad MAP_KEY", ignoreCase = true)
                    ) {
                        return@withContext ApiResponse.Error(
                            httpStatusCode = 403,
                            errorMessage = "HTTP 403 – NASA FIRMS MAP_KEY tidak valid atau belum diaktifkan.",
                            requestTime = requestTime,
                            responseTime = responseTime,
                            url = url
                        )
                    }
                    if (bodyString.contains("Rate limit exceeded", ignoreCase = true)) {
                        return@withContext ApiResponse.Error(
                            httpStatusCode = 429,
                            errorMessage = "HTTP 429 – Rate limit NASA FIRMS terlampaui. Harap tunggu beberapa saat.",
                            requestTime = requestTime,
                            responseTime = responseTime,
                            url = url,
                            isRateLimited = true,
                            retryAfterSeconds = 60
                        )
                    }

                    return@withContext ApiResponse.Success(
                        data = bodyString,
                        httpStatusCode = statusCode,
                        requestTime = requestTime,
                        responseTime = responseTime,
                        url = url,
                        serverTimestamp = serverTimestamp
                    )
                } else {
                    val is429 = statusCode == 429
                    val retryAfter = res.header("Retry-After")?.toLongOrNull() ?: if (is429) 60L else null

                    val errorDetail = when (statusCode) {
                        400 -> "HTTP 400 – Parameter permintaan tidak valid."
                        401, 403 -> "HTTP $statusCode – Akses ditolak. Kunci MAP_KEY tidak valid."
                        404 -> "HTTP 404 – Endpoint NASA FIRMS tidak ditemukan."
                        429 -> "HTTP 429 – Rate limit NASA FIRMS terlampaui. Harap jeda beberapa menit."
                        500, 502, 503, 504 -> "HTTP $statusCode – Server NASA FIRMS sedang mengalami gangguan."
                        else -> "HTTP $statusCode – Gagal mengunduh data satelit."
                    }
                    return@withContext ApiResponse.Error(
                        httpStatusCode = statusCode,
                        errorMessage = errorDetail,
                        requestTime = requestTime,
                        responseTime = responseTime,
                        url = url,
                        isRateLimited = is429,
                        retryAfterSeconds = retryAfter
                    )
                }
            }
        } catch (e: UnknownHostException) {
            val responseTime = System.currentTimeMillis()
            ApiResponse.Error(
                httpStatusCode = null,
                errorMessage = "Koneksi gagal: Tidak dapat terhubung ke server NASA FIRMS (Periksa koneksi internet perangkat).",
                requestTime = requestTime,
                responseTime = responseTime,
                url = url
            )
        } catch (e: SocketTimeoutException) {
            val responseTime = System.currentTimeMillis()
            ApiResponse.Error(
                httpStatusCode = null,
                errorMessage = "Koneksi timeout: Server NASA FIRMS terlalu lama merespons.",
                requestTime = requestTime,
                responseTime = responseTime,
                url = url
            )
        } catch (e: IOException) {
            val responseTime = System.currentTimeMillis()
            ApiResponse.Error(
                httpStatusCode = null,
                errorMessage = "Gagal I/O jaringan: ${e.localizedMessage ?: "Kesalahan komunikasi data"}",
                requestTime = requestTime,
                responseTime = responseTime,
                url = url
            )
        } catch (e: Exception) {
            val responseTime = System.currentTimeMillis()
            ApiResponse.Error(
                httpStatusCode = null,
                errorMessage = "Kesalahan tidak terduga: ${e.localizedMessage ?: "Unknown error"}",
                requestTime = requestTime,
                responseTime = responseTime,
                url = url
            )
        }
    }
}
