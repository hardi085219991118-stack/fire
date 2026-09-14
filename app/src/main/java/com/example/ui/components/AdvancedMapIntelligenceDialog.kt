package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.engine.AdvancedMapIntelligence
import com.example.data.model.Hotspot

/**
 * Feature 275–290: ADVANCED MAP INTELLIGENCE DIALOG
 * Heatmap, Density Grid, Temporal Trends, Age Distribution & Source Observability.
 */
@Composable
fun AdvancedMapIntelligenceDialog(
    allHotspots: List<Hotspot>,
    dataReceivedTimestamp: Long?,
    onDismiss: () -> Unit
) {
    var selectedHeatmapAgeFilter by remember { mutableStateOf(30L) }

    val heatmapResult = remember(allHotspots, selectedHeatmapAgeFilter) {
        AdvancedMapIntelligence.computeHeatmap(allHotspots, selectedHeatmapAgeFilter)
    }

    val densityGrid = remember(allHotspots) {
        AdvancedMapIntelligence.computeDensityGrid(allHotspots)
    }

    val densityComparison = remember(allHotspots) {
        AdvancedMapIntelligence.compareDensityTimeWindows(allHotspots)
    }

    val areaRankings = remember(densityGrid) {
        AdvancedMapIntelligence.rankAreasByHotspotConcentration(densityGrid)
    }

    val temporalTrend = remember(allHotspots) {
        AdvancedMapIntelligence.computeTemporalTrend(allHotspots)
    }

    val ageDistribution = remember(allHotspots) {
        AdvancedMapIntelligence.computeAgeDistribution(allHotspots)
    }

    val sourceAnalytics = remember(allHotspots, dataReceivedTimestamp) {
        AdvancedMapIntelligence.computeSourceAnalytics(allHotspots, dataReceivedTimestamp)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0B1120))
                .testTag("advanced_map_intelligence_dialog"),
            color = Color(0xFF0B1120)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "KECERDASAN PETA GEOSPASIAL",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8),
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "🌐 ANALISIS HEATMAP & TREN TEMPORAL",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 1. HEATMAP SECTION (Fitur 275–278)
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131D2E)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(14.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "🔥 HEATMAP KEPADATAN ANOMALI TERMAL",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Dihitung murni dari koordinat nyata satelit tanpa titik rekaan.",
                            fontSize = 11.sp,
                            color = Color.LightGray
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Time Filters (Fitur 276)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val filters = listOf(30L to "30 Menit", 60L to "1 Jam", 180L to "3 Jam", 360L to "6 Jam", 720L to "12 Jam", 1440L to "24 Jam")
                            filters.forEach { (mins, label) ->
                                FilterChip(
                                    selected = selectedHeatmapAgeFilter == mins,
                                    onClick = { selectedHeatmapAgeFilter = mins },
                                    label = { Text(label, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFFFF5722),
                                        selectedLabelColor = Color.White,
                                        containerColor = Color(0xFF0F172A),
                                        labelColor = Color.LightGray
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (heatmapResult.emptyStateMessage != null) {
                            Surface(
                                color = Color(0xFF0F172A),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = heatmapResult.emptyStateMessage,
                                    color = Color(0xFFFFB74D),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        } else {
                            Text(
                                text = "Titik Panas Aktif dalam Heatmap: ${heatmapResult.points.size} anomali",
                                color = Color(0xFF00E676),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "ℹ️ ${heatmapResult.disclaimer}",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2. DENSITY COMPARISON & ALERT (Fitur 280 & 282)
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131D2E)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(14.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "📊 PERBANDINGAN RENTANG WAKTU KEPADATAN",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            DensityStatColumn("≤30m", "${densityComparison.count30Min} titik", Color(0xFFFF5722))
                            DensityStatColumn("≤1 Jam", "${densityComparison.count1Hour} titik", Color(0xFFFF9800))
                            DensityStatColumn("≤3 Jam", "${densityComparison.count3Hours} titik", Color(0xFF0284C7))
                            DensityStatColumn("≤6 Jam", "${densityComparison.count6Hours} titik", Color.Gray)
                        }

                        if (densityComparison.concentrationAlert != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                color = Color(0xFF450A0A),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = densityComparison.concentrationAlert,
                                    color = Color(0xFFFCA5A5),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 3. AREA HOTSPOT RANKING (Fitur 281)
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131D2E)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(14.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "📍 AREA DENGAN KONSENTRASI HOTSPOT TERTINGGI",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        if (areaRankings.isEmpty()) {
                            Text("Belum ada klaster konsentrasi hotspot teramati.", color = Color.Gray, fontSize = 11.sp)
                        } else {
                            areaRankings.forEach { rank ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = rank.areaName, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                        Text(text = rank.description, color = Color.LightGray, fontSize = 10.sp)
                                    }
                                    Surface(
                                        color = Color(0xFF0284C7).copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "${rank.hotspotCount} Titik",
                                            color = Color(0xFF38BDF8),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 4. TEMPORAL TREND & GAP (Fitur 283–285)
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131D2E)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(14.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Timeline, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "TREN OBSERVASI TEMPORAL SATELIT",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color.White
                            )
                        }
                        Text(
                            text = "Berdasarkan acquisitionTimestamp orbit satelit nyata (bukan waktu request aplikasi).",
                            fontSize = 10.sp,
                            color = Color.LightGray
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        if (temporalTrend.isEmpty()) {
                            Text("Tidak ada data temporal untuk ditampilkan.", color = Color.Gray, fontSize = 11.sp)
                        } else {
                            val maxCount = temporalTrend.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                temporalTrend.forEach { bucket ->
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.width(42.dp)
                                    ) {
                                        if (bucket.isDataGap) {
                                            Text("GAP", fontSize = 9.sp, color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Box(
                                                modifier = Modifier
                                                    .width(12.dp)
                                                    .height(60.dp)
                                                    .background(Color(0xFF334155), RoundedCornerShape(2.dp))
                                            )
                                        } else {
                                            Text("${bucket.count}", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            val barHeight = ((bucket.count.toFloat() / maxCount) * 60).dp.coerceAtLeast(4.dp)
                                            Box(
                                                modifier = Modifier
                                                    .width(16.dp)
                                                    .height(barHeight)
                                                    .background(Color(0xFFFF5722), RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(text = bucket.timeLabelUtc, fontSize = 9.sp, color = Color.LightGray)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 5. HOTSPOT AGE DISTRIBUTION (Fitur 286)
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131D2E)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(14.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "⏳ DISTRIBUSI UMUR ANOMALI TERMAL",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val total = allHotspots.size.coerceAtLeast(1).toFloat()
                        AgeDistRow("≤30 menit (Sangat Terkini)", ageDistribution.countUnder30Min, total, Color(0xFF10B981))
                        AgeDistRow("31–60 menit (Terkini)", ageDistribution.count31To60Min, total, Color(0xFF38BDF8))
                        AgeDistRow("1–3 jam (Terlambat)", ageDistribution.count1To3Hours, total, Color(0xFFF59E0B))
                        AgeDistRow("3–6 jam (Sangat Terlambat)", ageDistribution.count3To6Hours, total, Color(0xFFEA580C))
                        AgeDistRow(">6 jam (Historis)", ageDistribution.countAbove6Hours, total, Color(0xFFEF4444))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 6. SOURCE FRESHNESS & LATENCY COMPARISON (Fitur 287 & 288)
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131D2E)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(14.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "🛰️ PERBANDINGAN SUMBER & LATENSI SATELIT",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        if (sourceAnalytics.isEmpty()) {
                            Text("Belum ada data sumber yang dapat dibandingkan.", color = Color.Gray, fontSize = 11.sp)
                        } else {
                            sourceAnalytics.forEach { src ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(text = src.sourceName, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = "Umur Data: ${src.dataAgeMinutes?.let { "$it menit" } ?: "N/A"} • Latensi: ${src.latencyMinutes?.let { "$it menit" } ?: "N/A"}",
                                            color = Color.LightGray,
                                            fontSize = 10.sp
                                        )
                                    }
                                    Text(
                                        text = "${src.recordCount} records",
                                        color = Color(0xFF38BDF8),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DensityStatColumn(label: String, count: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 10.sp, color = Color.LightGray)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = count, fontSize = 13.sp, fontWeight = FontWeight.Black, color = color)
    }
}

@Composable
private fun AgeDistRow(label: String, count: Int, total: Float, color: Color) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, fontSize = 11.sp, color = Color.LightGray)
            Text(text = "$count titik", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
        }
        Spacer(modifier = Modifier.height(2.dp))
        LinearProgressIndicator(
            progress = { count / total },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
            color = color,
            trackColor = Color(0xFF0F172A)
        )
    }
}
