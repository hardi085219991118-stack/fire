package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.engine.HotspotWithPriority
import com.example.data.model.Hotspot
import com.example.data.model.HotspotAgeStatus
import com.example.data.repository.DistanceCalculator
import com.example.data.repository.FireAgeCalculator
import com.example.ui.AgeFilter
import com.example.ui.FireNowUiState
import com.example.ui.FireNowViewModel
import com.example.ui.components.AdvancedMapIntelligenceDialog
import com.example.ui.components.AlertHistoryDialog
import com.example.ui.components.DataAuditDialog
import com.example.ui.components.DataIntegrityDialog
import com.example.ui.components.FieldDashboardDialog
import com.example.ui.components.FieldOperationDialog
import com.example.ui.components.HealthCheckDialog
import com.example.ui.components.HotspotDetailDialog
import com.example.ui.components.IncidentManagementDialog
import com.example.ui.components.InteractiveTileMap
import com.example.ui.components.LatestDataCard
import com.example.ui.components.MonitoringStatusCard
import com.example.ui.components.ObservationPlaybackDialog
import com.example.ui.components.ReportExportDialog
import com.example.ui.components.SearchLocationDialog
import com.example.ui.components.SettingsDialog
import com.example.ui.components.TimeComparisonCard
import com.example.ui.components.WatchAreasDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FireNowScreen(viewModel: FireNowViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Location Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            viewModel.requestLocationAndRefresh()
        } else {
            viewModel.refreshData()
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "🔥 HARDI MANTANGAI FIRE NOW",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                        // Feature 167: DATA STATUS HEADER
                        Text(
                            text = "STATUS DATA: ${uiState.honestHeaderStatus} • DICEK: ${FireAgeCalculator.formatTimeOnly(uiState.lastCheckTime ?: System.currentTimeMillis())}",
                            fontSize = 10.sp,
                            color = Color(0xFF38BDF8),
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                actions = {
                    // Feature 138 & 139: Search Coordinate / Place Button
                    IconButton(
                        onClick = { viewModel.setShowSearchCoordinateDialog(true) },
                        modifier = Modifier.testTag("open_search_button")
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Cari Koordinat / Wilayah",
                            tint = Color(0xFF38BDF8)
                        )
                    }

                    // Feature 143: Health Check Button
                    IconButton(
                        onClick = { viewModel.runHealthCheck() },
                        modifier = Modifier.testTag("open_health_check_button")
                    ) {
                        Icon(
                            Icons.Default.Speed,
                            contentDescription = "Test Data Source",
                            tint = Color(0xFFFFB74D)
                        )
                    }

                    // Feature 126 & 178: Data Integrity & Invariant Audit Button
                    IconButton(
                        onClick = { viewModel.setShowIntegrityDialog(true) },
                        modifier = Modifier.testTag("open_integrity_button")
                    ) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = "Audit Integritas Data",
                            tint = Color(0xFF00E676)
                        )
                    }

                    // Settings Button
                    IconButton(
                        onClick = { viewModel.setShowSettingsDialog(true) },
                        modifier = Modifier.testTag("open_settings_button")
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Pengaturan",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        containerColor = Color(0xFF0A0F1D)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // 1. Primary Action: CEK DATA TERBARU with REFRESH LOCK (Feature 164)
            item {
                Button(
                    onClick = { viewModel.refreshData() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722)),
                    enabled = !uiState.isLoading,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("primary_refresh_button")
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "🔥 MEMERIKSA DATA SATELIT TERBARU...",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    } else {
                        Icon(Icons.Default.LocalFireDepartment, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "🔥 CEK DATA TERBARU SEKARANG",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                    }
                }
                if (uiState.autoRefreshMinutes > 0) {
                    Text(
                        text = "Auto-Refresh aktif: setiap ${uiState.autoRefreshMinutes} menit",
                        fontSize = 11.sp,
                        color = Color(0xFF38BDF8),
                        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                    )
                }
            }

            // 2. Verified Badges (Feature 141 & 152)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color(0xFF0F172A),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
                    ) {
                        Text(
                            text = uiState.latestSatelliteBadge,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Surface(
                        color = if (uiState.realDataBadgeText.contains("VERIFIED")) Color(0x3300E676) else Color(0x33FFB74D),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (uiState.realDataBadgeText.contains("VERIFIED")) Color(0xFF00E676) else Color(0xFFFFB74D)
                        )
                    ) {
                        Text(
                            text = uiState.realDataBadgeText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (uiState.realDataBadgeText.contains("VERIFIED")) Color(0xFF00E676) else Color(0xFFFFB74D),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // 3. Status Warnings & Latency Banners
            item {
                StatusBannersSection(
                    uiState = uiState,
                    onRetry = { viewModel.refreshData() }
                )
            }

            // 4. Feature 111: LATEST DATA CARD
            item {
                LatestDataCard(
                    latestObservationTime = uiState.latestObservationTime,
                    latestSatelliteName = uiState.latestSatelliteName,
                    dataReceivedTime = uiState.lastReceivedTime,
                    deliveryLatencyFormatted = uiState.deliveryLatencyFormatted,
                    newestDataAgeFormatted = uiState.newestDataAgeFormatted,
                    status = uiState.newestDataStatus,
                    realDataBadge = uiState.realDataBadgeText,
                    sourceName = "NASA FIRMS (${uiState.selectedSource})"
                )
            }

            // 4b. Feature 206–213: CONTINUOUS MONITORING SESSION CARD
            item {
                MonitoringStatusCard(
                    session = uiState.monitoringSession,
                    latestObsTimestamp = uiState.latestObservationTime,
                    latestDataAgeFormatted = uiState.newestDataAgeFormatted,
                    onStartMonitoring = {
                        viewModel.startMonitoring(
                            radiusKm = uiState.selectedDistanceFilterKm,
                            freshnessThreshold = uiState.selectedAgeFilter.maxMinutes,
                            intervalMinutes = 5
                        )
                    },
                    onStopMonitoring = { viewModel.stopMonitoring() }
                )
            }

            // 4c. Tahap 3 Action Bar (11 Indikator, Riwayat Alert, Area Pantauan, Playback)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Feature 259: 11 Indikator Lapangan
                    Button(
                        onClick = { viewModel.setShowFieldDashboardDialog(true) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("open_field_dashboard_btn")
                    ) {
                        Text("📋 11 INDIKATOR LAPANGAN", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Feature 233: Riwayat Peringatan
                    OutlinedButton(
                        onClick = { viewModel.setShowAlertHistoryDialog(true) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFB74D)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("open_alert_history_btn")
                    ) {
                        Text("🔔 ALERT (${uiState.alertHistory.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Feature 214: Area Pantauan
                    OutlinedButton(
                        onClick = { viewModel.setShowWatchAreasDialog(true) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("open_watch_areas_btn")
                    ) {
                        Text("📍 AREA PANTAUAN", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Feature 236: Riwayat & Playback
                    OutlinedButton(
                        onClick = { viewModel.setShowPlaybackDialog(true) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00E676)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("open_playback_btn")
                    ) {
                        Text("⏱ PLAYBACK OBSERVASI", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 4d. Tahap 4 Action Bar (Mode Lapangan, Kecerdasan Peta, Incident, Laporan)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Feature 261: Mode Operasi Lapangan (High Outdoor Contrast)
                    Button(
                        onClick = { viewModel.setShowFieldOperationDialog(true) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("open_field_mode_btn")
                    ) {
                        Text("🔥 MODE OPERASI LAPANGAN", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Feature 275: Kecerdasan Peta & Heatmap
                    OutlinedButton(
                        onClick = { viewModel.setShowMapIntelligenceDialog(true) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("open_map_intelligence_btn")
                    ) {
                        Text("🌐 HEATMAP & TREN TEMPORAL", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Feature 301: Manajemen Catatan Incident & Verifikasi
                    OutlinedButton(
                        onClick = { viewModel.setShowIncidentDialog(true) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFB74D)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("open_incident_dialog_btn")
                    ) {
                        Text("📋 INCIDENT & VERIFIKASI (${uiState.incidents.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Feature 319: Laporan Pemantauan & Export Data
                    OutlinedButton(
                        onClick = { viewModel.setShowReportExportDialog(true) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00E676)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("open_report_export_btn")
                    ) {
                        Text("📄 LAPORAN & AUDIT DATA", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 5. Proximity Alert Banner if fire within 5km
            item {
                ProximityAlertSection(
                    hotspots = uiState.allHotspots,
                    userLocation = uiState.userLocation ?: uiState.customSearchLocation,
                    onSelectHotspot = { viewModel.selectHotspot(it) }
                )
            }

            // 6. Metrics Overview Dashboard
            item {
                FireNowMetricsDashboard(uiState = uiState)
            }

            // 7. Location Center Card (GPS vs Custom Search)
            item {
                UserLocationCard(
                    userLocation = uiState.userLocation,
                    customSearchLocation = uiState.customSearchLocation,
                    locationError = uiState.locationError,
                    isLocating = uiState.isLocating,
                    isStale = uiState.isLocationStale,
                    isAccuracyLow = uiState.isLocationAccuracyLow,
                    onRequestLocation = {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    },
                    onOpenSearch = { viewModel.setShowSearchCoordinateDialog(true) },
                    onResetToGps = { viewModel.resetToGpsLocation() }
                )
            }

            // 8. Interactive Satellite Map
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PETA SATELIT TERMAL",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Layer: ${uiState.mapLayerType.label}",
                            fontSize = 11.sp,
                            color = Color(0xFF38BDF8)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    InteractiveTileMap(
                        userLocation = uiState.userLocation ?: uiState.customSearchLocation,
                        hotspots = uiState.filteredHotspots,
                        layerType = uiState.mapLayerType,
                        onLayerChanged = { viewModel.setMapLayerType(it) },
                        selectedHotspot = uiState.selectedHotspot,
                        onHotspotSelected = { viewModel.selectHotspot(it) },
                        onCenterOnUser = { viewModel.requestLocationAndRefresh() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                    )
                }
            }

            // 9. Time Comparison Card (Device vs Obs vs Received)
            item {
                TimeComparisonCard(
                    currentDeviceTime = System.currentTimeMillis(),
                    latestObservationTime = uiState.latestObservationTime,
                    lastReceivedTime = uiState.lastReceivedTime,
                    lastCheckTime = uiState.lastCheckTime,
                    deliveryLatencyMinutes = uiState.deliveryLatencyMinutes,
                    satelliteAgeMinutes = uiState.newestDataAgeMinutes
                )
            }

            // 10. Filters Section: Time Window & Radius
            item {
                FiltersSection(
                    selectedAgeFilter = uiState.selectedAgeFilter,
                    selectedDistanceKm = uiState.selectedDistanceFilterKm,
                    isNowOnlyMode = uiState.showNowOnlyMode,
                    onAgeFilterSelected = { viewModel.setAgeFilter(it) },
                    onDistanceFilterSelected = { viewModel.setDistanceFilter(it) },
                    onToggleNowOnly = { viewModel.toggleNowOnlyMode(it) }
                )
            }

            // 11. Feature 121: Empty State when no hotspot matches criteria
            if (uiState.filteredHotspots.isEmpty()) {
                item {
                    NoDataInFilterCard(
                        selectedAgeFilter = uiState.selectedAgeFilter,
                        allHotspotsCount = uiState.allHotspots.size,
                        newestDataAgeMinutes = uiState.newestDataAgeMinutes,
                        onSelectOneHour = { viewModel.setAgeFilter(AgeFilter.ONE_HOUR) },
                        onSelectThreeHours = { viewModel.setAgeFilter(AgeFilter.THREE_HOURS) },
                        onSelectAll = { viewModel.setAgeFilter(AgeFilter.ALL) }
                    )
                }
            }

            // 12. Feature 117 & 118: HOTSPOT PRIORITY LIST (Urutan Prioritas Pemantauan)
            if (uiState.priorityRankedHotspots.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🔥 PRIORITAS PEMANTAUAN (${uiState.priorityRankedHotspots.size} Titik)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Bukan probabilitas api",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }
                }

                itemsIndexed(uiState.priorityRankedHotspots) { index, itemWithPriority ->
                    HotspotPriorityItemCard(
                        rank = index + 1,
                        item = itemWithPriority,
                        onClick = { viewModel.selectHotspot(itemWithPriority.hotspot) }
                    )
                }
            }

            // 13. Feature 168 & 170: Safety & Thermal Anomaly Disclaimer
            item {
                Surface(
                    color = Color(0xFF0F172A),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "⚠️ CATATAN KEAMANAN & PRINSIP KEJUJURAN DATA",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFB74D)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Hotspot satelit adalah indikasi anomali termal dan bukan konfirmasi kebakaran di lapangan. Tim lapangan perlu melakukan verifikasi langsung. Aplikasi ini tidak pernah membuat data dummy, memanipulasi timestamp, atau mengklaim data historis sebagai 'LIVE'.",
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8),
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }
    }

    // Dialogs binding
    uiState.selectedHotspot?.let { hotspot ->
        HotspotDetailDialog(
            hotspot = hotspot,
            onDismiss = { viewModel.selectHotspot(null) }
        )
    }

    if (uiState.showSearchCoordinateDialog) {
        SearchLocationDialog(
            currentLat = uiState.effectiveCenterLatitude,
            currentLon = uiState.effectiveCenterLongitude,
            isCustomCenter = uiState.customSearchLocation != null,
            onSearchCoordinate = { lat, lon, label -> viewModel.searchCoordinate(lat, lon, label) },
            onResetToGps = { viewModel.resetToGpsLocation() },
            onDismiss = { viewModel.setShowSearchCoordinateDialog(false) }
        )
    }

    if (uiState.showIntegrityDialog) {
        DataIntegrityDialog(
            integrityReport = uiState.dataIntegrityReport,
            clockSyncWarning = uiState.clockSyncWarning,
            serverTimeOffsetMs = uiState.serverTimeOffsetMs,
            isDataStalled = uiState.isDataStalled,
            stallMessage = uiState.stallWarningMessage,
            isLatencyHigh = uiState.isDeliveryLatencyHigh,
            latencyMessage = uiState.deliveryLatencyWarningMessage,
            isDataGap = uiState.isDataGapDetected,
            dataGapMessage = uiState.dataGapMessage,
            multiSatelliteSummary = uiState.multiSatelliteSummary,
            isRunningHealthCheck = uiState.isRunningHealthCheck,
            onRunHealthCheck = { viewModel.runHealthCheck() },
            onDismiss = { viewModel.setShowIntegrityDialog(false) }
        )
    }

    if (uiState.showHealthCheckDialog) {
        HealthCheckDialog(
            report = uiState.healthCheckReport,
            onDismiss = { viewModel.setShowHealthCheckDialog(false) }
        )
    }

    if (uiState.showAuditDialog) {
        DataAuditDialog(
            logs = uiState.auditLogs,
            onDismiss = { viewModel.setShowAuditDialog(false) },
            onClearLogs = { viewModel.clearAuditLogs() }
        )
    }

    if (uiState.showSettingsDialog) {
        SettingsDialog(
            currentMapKey = uiState.nasaMapKey,
            currentSource = uiState.selectedSource,
            currentAutoRefresh = uiState.autoRefreshMinutes,
            onSaveSettings = { key, source, refresh ->
                viewModel.saveNasaMapKey(key)
                viewModel.setAutoRefresh(refresh)
                viewModel.setSource(source)
            },
            onDismiss = { viewModel.setShowSettingsDialog(false) }
        )
    }

    // Tahap 3: Field Dashboard Dialog (11 Indikator)
    if (uiState.showFieldDashboardDialog) {
        val latestHs = uiState.allHotspots.maxByOrNull { it.acquisitionTimestamp }
        val nearestHs = uiState.priorityRankedHotspots.firstOrNull()?.hotspot
        val ageMin = uiState.newestDataAgeMinutes
        val isFreshEnough = (ageMin != null && ageMin <= 30)
        FieldDashboardDialog(
            latestHotspot = latestHs,
            nearestHotspot = nearestHs,
            isFreshEnoughForFireNow = isFreshEnough,
            dataReceivedTimestamp = uiState.lastReceivedTime,
            deliveryLatencyFormatted = uiState.deliveryLatencyFormatted,
            newHotspotsCount = uiState.newHotspotsCount,
            temporalChange = uiState.temporalChange,
            alertsCount = uiState.alertHistory.size,
            dataQualityReport = uiState.dataQualityReport,
            sourceComparison = uiState.sourceComparison,
            onDismiss = { viewModel.setShowFieldDashboardDialog(false) }
        )
    }

    // Tahap 3: Alert History Dialog
    if (uiState.showAlertHistoryDialog) {
        AlertHistoryDialog(
            alerts = uiState.alertHistory,
            onDismiss = { viewModel.setShowAlertHistoryDialog(false) },
            onClearHistory = { viewModel.clearAlertHistory() }
        )
    }

    // Tahap 3: Watch Areas Dialog
    if (uiState.showWatchAreasDialog) {
        WatchAreasDialog(
            savedAreas = uiState.savedWatchAreas,
            activeAreaId = uiState.activeWatchAreaId,
            currentCenterLat = uiState.effectiveCenterLatitude,
            currentCenterLon = uiState.effectiveCenterLongitude,
            onSelectArea = { area -> viewModel.selectWatchArea(area) },
            onAddArea = { name, lat, lon, radius -> viewModel.saveWatchArea(name, lat, lon, radius) },
            onDeleteArea = { id -> viewModel.deleteWatchArea(id) },
            onDismiss = { viewModel.setShowWatchAreasDialog(false) }
        )
    }

    // Tahap 3: Observation Playback Dialog
    if (uiState.showPlaybackDialog) {
        ObservationPlaybackDialog(
            allHotspots = uiState.allHotspots,
            selectedHotspot = uiState.selectedHotspot,
            onDismiss = { viewModel.setShowPlaybackDialog(false) }
        )
    }

    // Tahap 4: Mode Operasi Lapangan Dialog
    if (uiState.showFieldOperationDialog) {
        FieldOperationDialog(
            userLocation = uiState.userLocation,
            allHotspots = uiState.allHotspots,
            newestDataAgeMinutes = uiState.newestDataAgeMinutes,
            newestDataStatus = uiState.newestDataStatus,
            onDismiss = { viewModel.setShowFieldOperationDialog(false) }
        )
    }

    // Tahap 4: Advanced Map Intelligence Dialog
    if (uiState.showMapIntelligenceDialog) {
        AdvancedMapIntelligenceDialog(
            allHotspots = uiState.allHotspots,
            dataReceivedTimestamp = uiState.lastReceivedTime,
            onDismiss = { viewModel.setShowMapIntelligenceDialog(false) }
        )
    }

    // Tahap 4: Incident Management & Field Verification Dialog
    if (uiState.showIncidentDialog) {
        IncidentManagementDialog(
            incidents = uiState.incidents,
            selectedHotspot = uiState.selectedHotspot,
            currentCenterLat = uiState.effectiveCenterLatitude,
            currentCenterLon = uiState.effectiveCenterLongitude,
            onCreateIncident = { title, lat, lon, fp, satTime, satName ->
                viewModel.createIncident(title, lat, lon, fp, satTime, satName)
            },
            onUpdateVerification = { id, status, notes, observer ->
                viewModel.updateIncidentVerification(id, status, notes, observer)
            },
            onDeleteIncident = { id ->
                viewModel.deleteIncident(id)
            },
            onDismiss = { viewModel.setShowIncidentDialog(false) }
        )
    }

    // Tahap 4: Report Generation & Export Dialog
    if (uiState.showReportExportDialog) {
        ReportExportDialog(
            areaLabel = uiState.effectiveCenterLabel,
            allHotspots = uiState.allHotspots,
            latestObservationTimestamp = uiState.latestObservationTime,
            lastReceivedTimestamp = uiState.lastReceivedTime,
            latencyFormatted = uiState.deliveryLatencyFormatted,
            alerts = uiState.alertHistory,
            incidents = uiState.incidents,
            auditLogs = uiState.auditLogs,
            previousSnapshot = uiState.previousSnapshot,
            currentSnapshot = uiState.currentSnapshot,
            onDismiss = { viewModel.setShowReportExportDialog(false) }
        )
    }
}

@Composable
private fun StatusBannersSection(
    uiState: FireNowUiState,
    onRetry: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Error Banner
        if (uiState.errorMessage != null) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF450A0A)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFEF4444), RoundedCornerShape(12.dp))
                    .testTag("error_banner")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("❌ GAGAL MENGAMBIL DATA SATELIT", fontWeight = FontWeight.Bold, color = Color(0xFFEF4444), fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Penyebab: ${uiState.errorMessage}", color = Color.White, fontSize = 12.sp)
                    if (uiState.lastReceivedTime != null) {
                        Text("Data terakhir berhasil: ${FireAgeCalculator.formatDateTime(uiState.lastReceivedTime)}", color = Color.LightGray, fontSize = 11.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        modifier = Modifier.testTag("retry_button")
                    ) {
                        Text("COBA LAGI", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }

        // Feature 148: DATA SOURCE STALLED ALERT
        if (uiState.isDataStalled && uiState.stallWarningMessage != null) {
            Surface(
                color = Color(0x33FF5252),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF5252)),
                modifier = Modifier.fillMaxWidth().testTag("stall_warning_banner")
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFF5252), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("⚠️ PERINGATAN: SUMBER DATA STALLED", fontWeight = FontWeight.Bold, color = Color(0xFFFF5252), fontSize = 12.sp)
                        Text(uiState.stallWarningMessage!!, color = Color.White, fontSize = 11.sp)
                    }
                }
            }
        }

        // Feature 149: DELIVERY LATENCY SPIKE WARNING
        if (uiState.isDeliveryLatencyHigh && uiState.deliveryLatencyWarningMessage != null) {
            Surface(
                color = Color(0x33FFB74D),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB74D)),
                modifier = Modifier.fillMaxWidth().testTag("latency_warning_banner")
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFFB74D), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("⚠️ LATENSI PENGIRIMAN MENINGKAT", fontWeight = FontWeight.Bold, color = Color(0xFFFFB74D), fontSize = 12.sp)
                        Text(uiState.deliveryLatencyWarningMessage!!, color = Color.White, fontSize = 11.sp)
                    }
                }
            }
        }

        // Cache Notice
        if (uiState.isFromCache) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2D1B00)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFFFB74D), RoundedCornerShape(12.dp))
                    .testTag("cache_banner")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFFB74D), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("⚠️ DATA CACHE (OFFLINE FALLBACK)", fontWeight = FontWeight.Bold, color = Color(0xFFFFB74D), fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Data terakhir diterima: ${FireAgeCalculator.formatDateTime(uiState.lastReceivedTime)}", color = Color.White, fontSize = 12.sp)
                    Text("Umur cache: ${uiState.cacheAgeMinutes?.let { FireAgeCalculator.formatAgeDescription(it) } ?: "Tidak diketahui"}", color = Color.LightGray, fontSize = 11.sp)
                    Text("PERINGATAN: Data ini adalah salinan lokal sebelumnya, BUKAN observasi satelit detik ini.", color = Color(0xFFFFE0B2), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun ProximityAlertSection(
    hotspots: List<Hotspot>,
    userLocation: com.example.data.model.UserLocation?,
    onSelectHotspot: (Hotspot) -> Unit
) {
    if (userLocation == null) return
    val closeHotspot = hotspots
        .filter { it.distanceFromUser != null && it.distanceFromUser <= 5.0 }
        .minByOrNull { it.distanceFromUser ?: Double.MAX_VALUE }

    if (closeHotspot != null) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF7F1D1D)),
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, Color(0xFFFF5252), RoundedCornerShape(12.dp))
                .clickable { onSelectHotspot(closeHotspot) }
                .testTag("proximity_alert_card")
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🚨", fontSize = 28.sp)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "PERINGATAN TITIK TERDEKAT!",
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFF5252),
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Hotspot satelit berjarak ${closeHotspot.distanceFromUser?.let { DistanceCalculator.formatDistanceKm(it) }} dari lokasi Anda.",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Umur data satelit: ${FireAgeCalculator.formatAgeDescription(closeHotspot.satelliteAgeMinutes)} (${closeHotspot.satellite})",
                        color = Color(0xFFFFCDD2),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun FireNowMetricsDashboard(uiState: FireNowUiState) {
    val nearestHotspot = uiState.allHotspots.minByOrNull { it.distanceFromUser ?: Double.MAX_VALUE }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(16.dp))
            .testTag("metrics_dashboard")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📊", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "RINGKASAN PEMANTAUAN",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 13.sp
                    )
                }

                Surface(
                    color = uiState.newestDataStatus.composeColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, uiState.newestDataStatus.composeColor)
                ) {
                    Text(
                        text = "${uiState.newestDataStatus.iconEmoji} ${uiState.newestDataStatus.label}",
                        color = uiState.newestDataStatus.composeColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 4-Quadrant Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricBox(
                    label = "Jumlah Terfilter",
                    value = "${uiState.filteredHotspots.size}",
                    subText = "Total diunduh: ${uiState.allHotspots.size}",
                    accentColor = Color(0xFFFF5722),
                    modifier = Modifier.weight(1f)
                )

                MetricBox(
                    label = "Hotspot Terdekat",
                    value = nearestHotspot?.distanceFromUser?.let { DistanceCalculator.formatDistanceKm(it) } ?: "N/A",
                    subText = nearestHotspot?.satellite ?: "Belum ada titik",
                    accentColor = Color(0xFF38BDF8),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricBox(
                    label = "Umur Satelit Terbaru",
                    value = uiState.newestDataAgeFormatted,
                    subText = "Observasi satelit asli",
                    accentColor = uiState.newestDataStatus.composeColor,
                    modifier = Modifier.weight(1f)
                )

                MetricBox(
                    label = "Delivery Latency",
                    value = uiState.deliveryLatencyFormatted,
                    subText = "Obs → Diterima",
                    accentColor = Color(0xFF00E676),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MetricBox(
    label: String,
    value: String,
    subText: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF0F172A))
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Column {
            Text(label, fontSize = 10.sp, color = Color.LightGray, maxLines = 1)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = accentColor, maxLines = 1)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subText, fontSize = 9.sp, color = Color.Gray, maxLines = 1)
        }
    }
}

@Composable
private fun UserLocationCard(
    userLocation: com.example.data.model.UserLocation?,
    customSearchLocation: com.example.data.model.UserLocation?,
    locationError: String?,
    isLocating: Boolean,
    isStale: Boolean,
    isAccuracyLow: Boolean,
    onRequestLocation: () -> Unit,
    onOpenSearch: () -> Unit,
    onResetToGps: () -> Unit
) {
    val effectiveLoc = userLocation ?: customSearchLocation
    val isCustom = customSearchLocation != null

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(14.dp))
            .testTag("user_location_card")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = if (isCustom) Color(0xFFFFB74D) else Color(0xFF38BDF8),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isCustom) "📍 PUSAT PENCARIAN" else "📍 LOKASI GPS SAYA",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 13.sp
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Search button
                    OutlinedButton(
                        onClick = onOpenSearch,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("GANTI PUSAT", fontSize = 10.sp, color = Color(0xFF38BDF8))
                    }

                    if (isLocating) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Color(0xFF38BDF8))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (effectiveLoc != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Latitude: ${String.format(java.util.Locale.US, "%.5f", effectiveLoc.latitude)}", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Text("Longitude: ${String.format(java.util.Locale.US, "%.5f", effectiveLoc.longitude)}", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
                Spacer(modifier = Modifier.height(3.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Akurasi: ±${effectiveLoc.accuracyMeters.toInt()} meter", color = Color(0xFF38BDF8), fontSize = 11.sp)
                    Text("Label: ${effectiveLoc.provider}", color = Color.LightGray, fontSize = 11.sp)
                }

                // Feature 134: Stale location warning
                if (isStale && !isCustom) {
                    Text("⚠️ LOKASI TIDAK TERKINI (Terakhir diperbarui >15 menit lalu)", color = Color(0xFFFFB74D), fontSize = 10.sp)
                }
                // Feature 135: Accuracy warning
                if (isAccuracyLow && !isCustom) {
                    Text("⚠️ AKURASI GPS RENDAH (>100m)", color = Color(0xFFFFB74D), fontSize = 10.sp)
                }

                if (isCustom) {
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = onResetToGps,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("KEMBALI KE LOKASI GPS SAYA", fontSize = 10.sp, color = Color(0xFF00E676))
                    }
                }
            } else {
                Text(
                    text = locationError ?: "Menunggu deteksi sinyal GPS...",
                    color = Color(0xFFFFB74D),
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedButton(
                    onClick = onRequestLocation,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8)),
                    modifier = Modifier.testTag("request_location_button")
                ) {
                    Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("AKTIFKAN / DETEKSI GPS", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun FiltersSection(
    selectedAgeFilter: AgeFilter,
    selectedDistanceKm: Double,
    isNowOnlyMode: Boolean,
    onAgeFilterSelected: (AgeFilter) -> Unit,
    onDistanceFilterSelected: (Double) -> Unit,
    onToggleNowOnly: (Boolean) -> Unit
) {
    Column {
        // Feature 120: LOCAL FIRE NOW (≤30 MENIT & RADIUS)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "FILTER UMUR OBSERVASI (WAKTU ASLI)",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = Color.White
            )

            Button(
                onClick = { onToggleNowOnly(!isNowOnlyMode) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isNowOnlyMode) Color(0xFFFF5722) else Color(0xFF334155)
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.testTag("toggle_now_only_button")
            ) {
                Text(
                    text = if (isNowOnlyMode) "🔥 LOCAL FIRE NOW: AKTIF" else "MODE LENGKAP",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Feature 123: Age Filter Chips (30m, 1h, 3h, 6h, 12h, 24h, SEMUA)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AgeFilter.values().forEach { filter ->
                FilterChip(
                    selected = selectedAgeFilter == filter,
                    onClick = { onAgeFilterSelected(filter) },
                    label = { Text(filter.label, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFFF5722),
                        selectedLabelColor = Color.White,
                        containerColor = Color(0xFF1E293B),
                        labelColor = Color.LightGray
                    ),
                    modifier = Modifier.testTag("age_filter_${filter.name}")
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Feature 119: Distance Filter Chips (1, 5, 10, 25, 50, 100 km)
        Text(
            text = "FILTER RADIUS JARAK DARI PUSAT",
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(6.dp))

        val distances = listOf(1.0, 5.0, 10.0, 25.0, 50.0, 100.0)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            distances.forEach { dist ->
                FilterChip(
                    selected = selectedDistanceKm == dist,
                    onClick = { onDistanceFilterSelected(dist) },
                    label = { Text("${dist.toInt()} km", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF0284C7),
                        selectedLabelColor = Color.White,
                        containerColor = Color(0xFF1E293B),
                        labelColor = Color.LightGray
                    ),
                    modifier = Modifier.testTag("distance_filter_${dist.toInt()}")
                )
            }
        }
    }
}

/**
 * Feature 121: Empty state when no hotspot found <= 30 min
 * Feature 122: Do not substitute old data automatically
 */
@Composable
private fun NoDataInFilterCard(
    selectedAgeFilter: AgeFilter,
    allHotspotsCount: Int,
    newestDataAgeMinutes: Long?,
    onSelectOneHour: () -> Unit,
    onSelectThreeHours: () -> Unit,
    onSelectAll: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(14.dp))
            .testTag("no_data_card")
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🟢", fontSize = 32.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (selectedAgeFilter == AgeFilter.THIRTY_MINUTES) {
                    "TIDAK ADA HOTSPOT ≤30 MENIT"
                } else {
                    "TIDAK ADA HOTSPOT DALAM KRITERIA INI"
                },
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (selectedAgeFilter == AgeFilter.THIRTY_MINUTES) {
                    "Belum ditemukan hotspot satelit dengan waktu observasi dalam 30 menit terakhir pada radius yang dipilih."
                } else {
                    "Tidak ada titik anomali termal satelit dalam kriteria filter '${selectedAgeFilter.label}'."
                },
                color = Color.LightGray,
                fontSize = 12.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            if (newestDataAgeMinutes != null && newestDataAgeMinutes != Long.MAX_VALUE) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = Color(0xFF0F172A),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "DATA TERBARU YANG TERSEDIA: ${FireAgeCalculator.formatAgeDescription(newestDataAgeMinutes)}",
                        color = Color(0xFFFFB74D),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Feature 122: Explicit actions to widen window
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onSelectOneHour,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    modifier = Modifier.testTag("view_one_hour_button")
                ) {
                    Text("≤1 JAM", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onSelectThreeHours,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                    modifier = Modifier.testTag("view_three_hours_button")
                ) {
                    Text("≤3 JAM", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onSelectAll,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    modifier = Modifier.testTag("view_all_button")
                ) {
                    Text("SEMUA DATA", fontSize = 11.sp)
                }
            }
        }
    }
}

/**
 * Feature 117 & 118: Hotspot Priority Item Card
 */
@Composable
private fun HotspotPriorityItemCard(
    rank: Int,
    item: HotspotWithPriority,
    onClick: () -> Unit
) {
    val hotspot = item.hotspot
    val prio = item.priorityResult

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(prio.priorityLevel.badgeColorHex).copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("hotspot_item_$rank")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Rank Badge
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0F172A))
                        .border(1.dp, Color(prio.priorityLevel.badgeColorHex), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "#$rank",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Priority Badge & Status
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = Color(prio.priorityLevel.badgeColorHex).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = prio.priorityLevel.label,
                                color = Color(prio.priorityLevel.badgeColorHex),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "• ${FireAgeCalculator.formatAgeDescription(hotspot.satelliteAgeMinutes)}",
                            color = Color.LightGray,
                            fontSize = 11.sp
                        )

                        // Tahap 3: New Data Badge
                        if (hotspot.isNewData) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = Color(0xFF0288D1).copy(alpha = 0.3f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "🆕 BARU",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }

                        // Tahap 3: Multi-Satellite Observation Badge
                        if (hotspot.isMultiSatelliteObs) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = Color(0xFF7B1FA2).copy(alpha = 0.3f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "🛰️ MULTI-SAT",
                                    color = Color(0xFFCE93D8),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "Observasi: ${FireAgeCalculator.formatTimeOnly(hotspot.acquisitionTimestamp)} | ${hotspot.satellite}",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Confidence: ${hotspot.confidence ?: "Nominal"} • Brightness: ${hotspot.brightness?.let { "$it K" } ?: "N/A"}",
                            color = Color.LightGray,
                            fontSize = 10.sp
                        )
                        if (hotspot.observationCount > 1) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "• Persisten (${hotspot.observationCount}x)",
                                color = Color(0xFFFFB74D),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (hotspot.locationShiftMeters != null && hotspot.locationShiftMeters > 0) {
                        Text(
                            text = "Pergeseran deteksi: ±${hotspot.locationShiftMeters.toInt()}m dari posisi sebelumnya",
                            color = Color(0xFFFFCC80),
                            fontSize = 9.sp
                        )
                    }
                }

                // Distance on End
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = hotspot.distanceFromUser?.let { DistanceCalculator.formatDistanceKm(it) } ?: "-- km",
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        color = Color(0xFF38BDF8)
                    )
                    Text(
                        text = "dari pusat",
                        fontSize = 9.sp,
                        color = Color.Gray
                    )
                }
            }

            // Feature 118: Compact Explanation line
            if (prio.explanationLines.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                HorizontalDivider(color = Color(0xFF334155))
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "💡 ${prio.explanationLines.first()}",
                    fontSize = 10.sp,
                    color = Color(0xFF94A3B8),
                    maxLines = 1
                )
            }
        }
    }
}
