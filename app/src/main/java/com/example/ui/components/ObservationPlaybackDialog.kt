package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Satellite
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.example.data.model.Hotspot
import com.example.data.repository.FireAgeCalculator
import kotlinx.coroutines.delay

/**
 * Feature 236–248: OBSERVATION PLAYBACK & TIMELINE MAP TRACKING
 * Visualisasi kronologis observasi satelit TANPA data interpolasi buatan.
 */
@Composable
fun ObservationPlaybackDialog(
    allHotspots: List<Hotspot>,
    selectedHotspot: Hotspot?,
    onDismiss: () -> Unit
) {
    // Sort all hotspots chronologically (oldest to newest)
    val sortedHotspots = remember(allHotspots) {
        allHotspots.sortedBy { it.acquisitionTimestamp }
    }

    var sliderPos by remember { mutableFloatStateOf(1.0f) }
    var isPlaying by remember { mutableStateOf(false) }

    // Playback loop
    LaunchedEffect(isPlaying) {
        if (isPlaying && sortedHotspots.isNotEmpty()) {
            while (isPlaying) {
                delay(800L)
                val nextPos = sliderPos + 0.1f
                if (nextPos >= 1.0f) {
                    sliderPos = 1.0f
                    isPlaying = false
                } else {
                    sliderPos = nextPos
                }
            }
        }
    }

    val visibleCount = if (sortedHotspots.isNotEmpty()) {
        (sortedHotspots.size * sliderPos).toInt().coerceIn(1, sortedHotspots.size)
    } else 0

    val currentHotspotSlice = if (sortedHotspots.isNotEmpty()) {
        sortedHotspots.take(visibleCount)
    } else emptyList()

    val currentObservation = currentHotspotSlice.lastOrNull()

    // Feature 248: Pisahkan DATA TERSEDIA dan DATA TERKINI
    val totalAvailable = allHotspots.size
    val totalVeryFresh = allHotspots.count { it.satelliteAgeMinutes <= 30 }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Timeline, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Riwayat & Playback Observasi", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Feature 248: Data Availability Badge
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Data Tersedia:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$totalAvailable record", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Data Terkini (≤30 min):", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                if (totalVeryFresh > 0) "$totalVeryFresh titik" else "0 titik (Tidak Ada)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (totalVeryFresh > 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Playback Slider Control
                if (sortedHotspots.size > 1) {
                    Text(
                        "Kronologi Observasi Satelit (${visibleCount} dari ${sortedHotspots.size}):",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    Slider(
                        value = sliderPos,
                        onValueChange = {
                            isPlaying = false
                            sliderPos = it
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("observation_playback_slider")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (sliderPos >= 1.0f) sliderPos = 0.1f
                                isPlaying = !isPlaying
                            },
                            modifier = Modifier.testTag("playback_play_pause_btn")
                        ) {
                            Icon(
                                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (currentObservation != null) {
                            Text(
                                "Observasi: ${FireAgeCalculator.formatDateTimeIso(currentObservation.acquisitionTimestamp)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        "ℹ️ Hanya memvisualisasikan data asli yang telah diterima satelit. Tidak ada interpolasi titik api buatan.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Feature 236 & 237: Observation Timeline for selected hotspot or latest
                Text(
                    "Daftar Observasi Satelit (Kronologis):",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(currentHotspotSlice.reversed().take(15)) { hs ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "${hs.satellite} (${hs.instrument})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        FireAgeCalculator.formatDateTimeIso(hs.acquisitionTimestamp),
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Text(
                                    "Koordinat: [${String.format("%.4f", hs.latitude)}, ${String.format("%.4f", hs.longitude)}] • Umur: ${hs.satelliteAgeMinutes}m",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (hs.locationShiftMeters != null && hs.locationShiftMeters > 0) {
                                    Text(
                                        "Pergeseran lokasi anomali termal: ±${hs.locationShiftMeters.toInt()} meter dari observasi sebelumnya",
                                        fontSize = 10.sp,
                                        color = Color(0xFFE65100),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Feature 245, 246, 247: Satellite Pass & Coverage Status
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            "Status Coverage Satelit:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                        Text(
                            "• Coverage: AVAILABLE (VIIRS & MODIS NRT via NASA LANCE)\n• Estimasi pass berikutnya: ESTIMASI (Bergantung siklus orbit heliosinkron satelit polar)\n• Catatan: Satelit tidak memantau konstan tiap detik, melainkan pada jendela lintasan orbit tertentu.",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("close_playback_dialog_btn")
            ) {
                Text("TUTUP")
            }
        }
    )
}
