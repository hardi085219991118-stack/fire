package com.example

import com.example.data.api.NasaFirmsParser
import com.example.data.model.HotspotAgeStatus
import com.example.data.repository.DistanceCalculator
import com.example.data.repository.FireAgeCalculator
import com.example.data.repository.FireValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class ExampleUnitTest {

    /**
     * TEST 1: Data observasi 10 menit lalu -> SANGAT TERKINI (🟢)
     */
    @Test
    fun test1_observation10MinutesAgo_isSangatTerkini() {
        val now = 1726300000000L
        val acqTime = now - (10 * 60 * 1000L) // 10 menit lalu
        val age = FireAgeCalculator.calculateAgeMinutes(acqTime, now)
        assertEquals(10L, age)

        val status = FireAgeCalculator.determineStatus(age)
        assertEquals(HotspotAgeStatus.SANGAT_TERKINI, status)
        assertTrue("Harus memenuhi syarat Fire Now", status.isEligibleForFireNow)
    }

    /**
     * TEST 2: Data observasi 2 jam lalu (120 menit) -> TERBARU TETAPI TERLAMBAT (🟡)
     * Tidak boleh dianggap sebagai API SEKARANG.
     */
    @Test
    fun test2_observation2HoursAgo_isTerlambat() {
        val now = 1726300000000L
        val acqTime = now - (120 * 60 * 1000L) // 2 jam lalu
        val age = FireAgeCalculator.calculateAgeMinutes(acqTime, now)
        assertEquals(120L, age)

        val status = FireAgeCalculator.determineStatus(age)
        assertEquals(HotspotAgeStatus.TERBARU_TETAPI_TERLAMBAT, status)
        assertFalse("Tidak boleh masuk kategori Fire Now", status.isEligibleForFireNow)
    }

    /**
     * TEST 3: Data observasi 8 jam lalu (480 menit) -> HISTORIS (🔴)
     * Tidak boleh masuk default FIRE NOW.
     */
    @Test
    fun test3_observation8HoursAgo_isHistoris() {
        val now = 1726300000000L
        val acqTime = now - (480 * 60 * 1000L) // 8 jam lalu
        val age = FireAgeCalculator.calculateAgeMinutes(acqTime, now)
        assertEquals(480L, age)

        val status = FireAgeCalculator.determineStatus(age)
        assertEquals(HotspotAgeStatus.HISTORIS, status)
        assertFalse("Data >6 jam tidak boleh masuk default FIRE NOW", status.isEligibleForFireNow)
    }

    /**
     * TEST 4: Tidak ada response / CSV kosong -> 0 record valid, status UNKNOWN
     * Tidak boleh membuat marker.
     */
    @Test
    fun test4_emptyCsv_createsNoMarkers() {
        val parseResult = NasaFirmsParser.parseCsv("", "NASA FIRMS")
        assertEquals(0, parseResult.validCount)
        assertEquals(0, parseResult.validHotspots.size)
        assertNull(parseResult.latestAcquisitionTimestamp)
    }

    /**
     * TEST 5: API memberikan format header salah atau error -> rejected
     */
    @Test
    fun test5_invalidHeader_isRejected() {
        val badCsv = "col1,col2,col3\n1,2,3"
        val parseResult = NasaFirmsParser.parseCsv(badCsv, "NASA FIRMS")
        assertEquals(0, parseResult.validCount)
        assertTrue(parseResult.rejectedCount > 0)
    }

    /**
     * TEST 6: Jarak Haversine antar koordinat pengguna dan hotspot
     */
    @Test
    fun test6_haversineDistance() {
        // Hardi Mantangai coordinates approx -2.44, 114.47
        val userLat = -2.44
        val userLon = 114.47

        // Point 0.01 deg away (~1.1 km)
        val fireLat = -2.445
        val fireLon = 114.475

        val dist = DistanceCalculator.calculateHaversineDistanceKm(userLat, userLon, fireLat, fireLon)
        assertTrue(dist > 0.5 && dist < 2.0)
    }

    /**
     * TEST 7: API memberikan koordinat invalid (lat > 90, lon > 180) -> Record ditolak
     */
    @Test
    fun test7_invalidCoordinates_recordRejected() {
        val result1 = FireValidator.validateRecord(95.0, 114.0, "2026-09-14", "0540", 320.0)
        assertFalse(result1.isValid)
        assertTrue(result1.rejectReason!!.contains("Latitude"))

        val result2 = FireValidator.validateRecord(-2.0, 195.0, "2026-09-14", "0540", 320.0)
        assertFalse(result2.isValid)
        assertTrue(result2.rejectReason!!.contains("Longitude"))
    }

    /**
     * TEST 8: Timestamp kosong atau format invalid -> Record ditolak
     */
    @Test
    fun test8_emptyOrInvalidTimestamp_recordRejected() {
        val result1 = FireValidator.validateRecord(-2.0, 114.0, "", "0540", 320.0)
        assertFalse(result1.isValid)

        val result2 = FireValidator.validateRecord(-2.0, 114.0, "2026-09-14", "", 320.0)
        assertFalse(result2.isValid)

        val parseNull = FireAgeCalculator.parseAcquisitionTimestamp("", "")
        assertNull(parseNull)
    }

    /**
     * PENGUJIAN PALING PENTING:
     * Simulasikan:
     * Waktu perangkat: 21:46 WIB
     * Data satelit: 21:40 WIB
     * HASIL: Umur 6 menit, Status: SANGAT TERKINI (🟢)
     *
     * Kemudian:
     * Data satelit: 18:40 WIB
     * HASIL: Umur 3 jam 06 menit (186 menit), Status: TERLAMBAT (🟠)
     * BUKAN: API SEKARANG!
     */
    @Test
    fun testCrucial_simulationDeviceVsSatelliteTime() {
        val tzWib = TimeZone.getTimeZone("Asia/Jakarta")
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply {
            timeZone = tzWib
        }

        val deviceTime = sdf.parse("2026-09-14 21:46")!!.time
        val satTime1 = sdf.parse("2026-09-14 21:40")!!.time

        // Kasus 1: Selisih 6 menit
        val age1 = FireAgeCalculator.calculateAgeMinutes(satTime1, deviceTime)
        assertEquals(6L, age1)
        val status1 = FireAgeCalculator.determineStatus(age1)
        assertEquals(HotspotAgeStatus.SANGAT_TERKINI, status1)
        assertEquals("6 menit lalu", FireAgeCalculator.formatAgeDescription(age1))

        // Kasus 2: Data satelit 18:40 (3 jam 6 menit = 186 menit)
        val satTime2 = sdf.parse("2026-09-14 18:40")!!.time
        val age2 = FireAgeCalculator.calculateAgeMinutes(satTime2, deviceTime)
        assertEquals(186L, age2)
        val status2 = FireAgeCalculator.determineStatus(age2)
        assertEquals(HotspotAgeStatus.TERLAMBAT, status2)
        assertEquals("3 jam 6 menit lalu", FireAgeCalculator.formatAgeDescription(age2))
        assertFalse("Data 3 jam 6 menit BUKAN API SEKARANG!", status2.isEligibleForFireNow)
    }

    /**
     * Test Real NASA FIRMS CSV parsing with authentic headers
     */
    @Test
    fun testNasaFirmsCsvParsing_realDataFormat() {
        val csv = """
            latitude,longitude,bright_ti4,scan,track,acq_date,acq_time,satellite,instrument,confidence,version,bright_ti5,frp,daynight
            -2.4412,114.4715,345.2,0.4,0.4,2026-09-14,0540,1,VIIRS,nominal,2.0NRT,295.1,12.4,D
            -2.4500,114.4800,360.5,0.4,0.4,2026-09-14,0540,1,VIIRS,high,2.0NRT,298.0,18.2,D
        """.trimIndent()

        val result = NasaFirmsParser.parseCsv(csv, "NASA FIRMS (VIIRS NOAA-21)")
        assertEquals(2, result.validCount)
        assertEquals(0, result.rejectedCount)
        assertEquals("VIIRS NOAA-21", result.validHotspots[0].satellite)
        assertEquals(-2.4412, result.validHotspots[0].latitude, 0.0001)
        assertEquals(114.4715, result.validHotspots[0].longitude, 0.0001)
        assertEquals(345.2, result.validHotspots[0].brightness!!, 0.01)
    }

    /**
     * TAHAP 2 TEST 1: DataFreshnessEngine calculates second-level precision age
     */
    @Test
    fun testTahap2_dataFreshnessEngine_secondLevelPrecision() {
        val now = 1726300000000L
        val acq = now - (14 * 60 * 1000L + 25 * 1000L) // 14m 25s
        val freshness = com.example.data.engine.DataFreshnessEngine.calculateFreshness(acq, now)

        assertEquals(14L, freshness.dataAgeMinutes)
        assertEquals(865L, freshness.dataAgeSeconds)
        assertEquals(HotspotAgeStatus.SANGAT_TERKINI, freshness.freshnessStatus)
        assertTrue(freshness.formattedAgeFull.contains("14 menit 25 detik"))
        assertEquals("14m 25s", freshness.formattedAgeShort)
    }

    /**
     * TAHAP 2 TEST 2: FireDuplicateDetector removes exact coordinate & time overlaps
     */
    @Test
    fun testTahap2_fireDuplicateDetector() {
        val hs1 = com.example.data.model.Hotspot(
            id = "test_1",
            latitude = -2.44123,
            longitude = 114.47152,
            brightness = 340.0,
            confidence = "high",
            satellite = "VIIRS",
            instrument = "VIIRS",
            acquisitionDate = "2026-09-14",
            acquisitionTime = "0540",
            acquisitionTimestamp = 1726300000000L,
            source = "NASA",
            dataReceivedTimestamp = 1726300100000L,
            satelliteAgeMinutes = 10,
            deliveryLatencyMinutes = 1,
            distanceFromUser = 2.0,
            status = HotspotAgeStatus.SANGAT_TERKINI
        )
        // Duplicate with identical lat, lon, time
        val hs2 = hs1.copy(id = "test_2", dataReceivedTimestamp = 1726300200000L)

        val deduplicationResult = com.example.data.engine.FireDuplicateDetector.deduplicateHotspots(listOf(hs1, hs2))
        assertEquals(1, deduplicationResult.uniqueRecords.size)
        assertEquals(1, deduplicationResult.duplicatesRemovedCount)
        assertEquals("test_1", deduplicationResult.uniqueRecords[0].id)
    }

    /**
     * TAHAP 2 TEST 3: FireNowPriorityScore strictly ranks without claiming fire certainty
     */
    @Test
    fun testTahap2_priorityScore_ranking() {
        // High priority: close (1.2 km) and fresh (12 min)
        val p1 = com.example.data.model.FireNowPriorityScore.calculate(
            ageMinutes = 12L,
            distanceKm = 1.2,
            confidence = "high",
            brightness = 350.0
        )
        assertEquals(com.example.data.model.PriorityLevel.SANGAT_TINGGI, p1.priorityLevel)
        assertTrue(p1.explanationLines.any { it.contains("12 menit") })

        // Historical: 8 hours old
        val p2 = com.example.data.model.FireNowPriorityScore.calculate(
            ageMinutes = 480L,
            distanceKm = 1.2,
            confidence = "high",
            brightness = 350.0
        )
        assertEquals(com.example.data.model.PriorityLevel.HISTORIS, p2.priorityLevel)
    }

    /**
     * TAHAP 2 TEST 4: RealTimeClaimGuard sanitizes misleading "LIVE" claims
     */
    @Test
    fun testTahap2_realTimeClaimGuard_sanitization() {
        val sanitizedLive = com.example.data.model.RealTimeClaimGuard.sanitizeClaim("API SEKARANG", 180L)
        assertFalse(sanitizedLive.contains("SEKARANG"))

        val isAllowed = com.example.data.model.RealTimeClaimGuard.isRealTimeClaimAllowed(180L)
        assertFalse(isAllowed)
    }

    /**
     * TAHAP 2 TEST 5: ClockSyncEngine detects server drift
     */
    @Test
    fun testTahap2_clockSyncEngine() {
        val deviceTime = 1726300000000L
        val serverTimeWithDrift = deviceTime - (150 * 1000L) // 150 seconds drift
        val sync = com.example.data.engine.ClockSyncEngine.evaluateClockSync(serverTimeWithDrift, deviceTime)

        assertTrue(sync.isClockUnsynchronized)
        assertNotNull(sync.syncWarningMessage)
        assertTrue(sync.syncWarningMessage!!.contains("150 detik"))
    }

    /**
     * TAHAP 2 TEST 6: DataIntegrityTracker enforces invariant displayed <= valid <= received
     */
    @Test
    fun testTahap2_dataIntegrityInvariant() {
        val report = com.example.data.engine.DataIntegrityTracker.generateReport(
            received = 100,
            valid = 95,
            rejected = 5,
            displayed = 20,
            latestTimestamp = 1726300000000L,
            oldestTimestamp = 1726200000000L
        )
        assertTrue(report.isContractSatisfied)
        assertEquals(100, report.recordsReceived)
        assertEquals(95, report.recordsValid)
        assertEquals(20, report.recordsDisplayed)
    }
}
