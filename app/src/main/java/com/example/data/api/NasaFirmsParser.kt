package com.example.data.api

import com.example.data.local.HotspotEntity
import com.example.data.repository.FireAgeCalculator
import com.example.data.repository.FireValidator

data class ParseResult(
    val validHotspots: List<HotspotEntity>,
    val totalReceived: Int,
    val validCount: Int,
    val rejectedCount: Int,
    val rejectReasons: List<String>,
    val latestAcquisitionTimestamp: Long?,
    val oldestAcquisitionTimestamp: Long?,
    val latestSatellite: String?
)

object NasaFirmsParser {

    fun parseCsv(
        csvContent: String,
        sourceName: String,
        dataReceivedTimestamp: Long = System.currentTimeMillis()
    ): ParseResult {
        val lines = csvContent.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) {
            return ParseResult(
                validHotspots = emptyList(),
                totalReceived = 0,
                validCount = 0,
                rejectedCount = 0,
                rejectReasons = emptyList(),
                latestAcquisitionTimestamp = null,
                oldestAcquisitionTimestamp = null,
                latestSatellite = null
            )
        }

        // Check if header exists
        val headerLine = lines.first()
        val headers = headerLine.split(",").map { it.trim().lowercase() }

        val latIdx = headers.indexOfFirst { it == "latitude" || it == "lat" }
        val lonIdx = headers.indexOfFirst { it == "longitude" || it == "lon" }
        val dateIdx = headers.indexOfFirst { it == "acq_date" || it == "date" }
        val timeIdx = headers.indexOfFirst { it == "acq_time" || it == "time" }
        val brightIdx = headers.indexOfFirst { it == "bright_ti4" || it == "brightness" || it == "bright" }
        val bright5Idx = headers.indexOfFirst { it == "bright_ti5" || it == "bright_t31" }
        val confIdx = headers.indexOfFirst { it == "confidence" }
        val satIdx = headers.indexOfFirst { it == "satellite" }
        val instIdx = headers.indexOfFirst { it == "instrument" }
        val frpIdx = headers.indexOfFirst { it == "frp" }
        val dnIdx = headers.indexOfFirst { it == "daynight" }

        if (latIdx == -1 || lonIdx == -1) {
            // Not a valid FIRMS CSV
            return ParseResult(
                validHotspots = emptyList(),
                totalReceived = lines.size - 1,
                validCount = 0,
                rejectedCount = lines.size - 1,
                rejectReasons = listOf("Header CSV tidak memiliki kolom latitude / longitude"),
                latestAcquisitionTimestamp = null,
                oldestAcquisitionTimestamp = null,
                latestSatellite = null
            )
        }

        val validList = mutableListOf<HotspotEntity>()
        val rejectReasons = mutableListOf<String>()
        var rejectedCount = 0
        var totalDataRows = 0

        var latestAcq: Long? = null
        var oldestAcq: Long? = null
        var latestSat: String? = null

        // Process data rows
        for (i in 1 until lines.size) {
            val line = lines[i]
            if (line.isBlank() || line.startsWith("#")) continue
            totalDataRows++

            val tokens = line.split(",")
            if (tokens.size <= maxOf(latIdx, lonIdx)) {
                rejectedCount++
                if (rejectReasons.size < 5) {
                    rejectReasons.add("Baris #$i: Kolom tidak lengkap (${tokens.size})")
                }
                continue
            }

            val lat = tokens.getOrNull(latIdx)?.toDoubleOrNull()
            val lon = tokens.getOrNull(lonIdx)?.toDoubleOrNull()
            val dateStr = if (dateIdx != -1) tokens.getOrNull(dateIdx)?.trim() else null
            val timeStr = if (timeIdx != -1) tokens.getOrNull(timeIdx)?.trim() else null
            val brightness = if (brightIdx != -1) tokens.getOrNull(brightIdx)?.toDoubleOrNull() else null
            val brightTi5 = if (bright5Idx != -1) tokens.getOrNull(bright5Idx)?.toDoubleOrNull() else null
            val confidence = if (confIdx != -1) tokens.getOrNull(confIdx)?.trim() else null
            val rawSatellite = if (satIdx != -1) tokens.getOrNull(satIdx)?.trim() else null
            val instrument = if (instIdx != -1) tokens.getOrNull(instIdx)?.trim() ?: "VIIRS" else "VIIRS"
            val frp = if (frpIdx != -1) tokens.getOrNull(frpIdx)?.toDoubleOrNull() else null
            val daynight = if (dnIdx != -1) tokens.getOrNull(dnIdx)?.trim() else null

            // Validate record
            val validation = FireValidator.validateRecord(lat, lon, dateStr, timeStr, brightness)
            if (!validation.isValid) {
                rejectedCount++
                if (rejectReasons.size < 5) {
                    rejectReasons.add("Baris #$i: ${validation.rejectReason}")
                }
                continue
            }

            val acqTimestamp = FireAgeCalculator.parseAcquisitionTimestamp(dateStr!!, timeStr!!)
            if (acqTimestamp == null || acqTimestamp <= 0) {
                rejectedCount++
                if (rejectReasons.size < 5) {
                    rejectReasons.add("Baris #$i: Gagal mem-parse timestamp dari $dateStr $timeStr")
                }
                continue
            }

            // Standardize satellite display name
            val satelliteDisplay = when (rawSatellite?.uppercase()) {
                "N", "NOAA-20", "J1" -> "VIIRS NOAA-20"
                "1", "NOAA-21", "J2" -> "VIIRS NOAA-21"
                "NPP", "SNPP" -> "VIIRS Suomi-NPP"
                "T", "TERRA" -> "MODIS Terra"
                "A", "AQUA" -> "MODIS Aqua"
                else -> rawSatellite ?: "VIIRS Satelit"
            }

            val id = "firms_${lat}_${lon}_${dateStr}_${timeStr}_${rawSatellite ?: "sat"}"

            val entity = HotspotEntity(
                id = id,
                latitude = lat!!,
                longitude = lon!!,
                brightness = brightness,
                confidence = confidence,
                satellite = satelliteDisplay,
                instrument = instrument,
                acquisitionDate = dateStr,
                acquisitionTime = timeStr,
                acquisitionTimestamp = acqTimestamp,
                source = sourceName,
                dataReceivedTimestamp = dataReceivedTimestamp,
                bright_ti5 = brightTi5,
                frp = frp,
                daynight = daynight
            )

            validList.add(entity)

            // Track timestamps
            if (latestAcq == null || acqTimestamp > latestAcq!!) {
                latestAcq = acqTimestamp
                latestSat = satelliteDisplay
            }
            if (oldestAcq == null || acqTimestamp < oldestAcq!!) {
                oldestAcq = acqTimestamp
            }
        }

        return ParseResult(
            validHotspots = validList,
            totalReceived = totalDataRows,
            validCount = validList.size,
            rejectedCount = rejectedCount,
            rejectReasons = rejectReasons,
            latestAcquisitionTimestamp = latestAcq,
            oldestAcquisitionTimestamp = oldestAcq,
            latestSatellite = latestSat
        )
    }
}
