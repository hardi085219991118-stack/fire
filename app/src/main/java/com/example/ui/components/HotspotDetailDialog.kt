package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Hotspot
import com.example.data.repository.DistanceCalculator
import com.example.data.repository.FireAgeCalculator

@Composable
fun HotspotDetailDialog(
    hotspot: Hotspot,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF475569), RoundedCornerShape(20.dp))
                .testTag("hotspot_detail_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header with Status Badge & Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(hotspot.status.composeColor)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "TITIK HOTSPOT SATELIT",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_detail_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.LightGray)
                    }
                }

                // Age status banner
                Surface(
                    color = hotspot.status.composeColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = hotspot.status.iconEmoji,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = hotspot.status.label,
                                fontWeight = FontWeight.Bold,
                                color = hotspot.status.composeColor,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Umur observasi: ${FireAgeCalculator.formatAgeDescription(hotspot.satelliteAgeMinutes)}",
                                color = Color.White,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // Feature 118: Priority Explanation
                if (hotspot.priorityResult != null) {
                    val prio = hotspot.priorityResult
                    Surface(
                        color = Color(prio.priorityLevel.badgeColorHex).copy(alpha = 0.18f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "🔥 PRIORITAS PEMANTAUAN: ${prio.priorityLevel.label}",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(prio.priorityLevel.badgeColorHex),
                                    fontSize = 12.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            prio.explanationLines.forEach { exp ->
                                Text(
                                    text = "• $exp",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }

                // Feature 140: FIRE HOTSPOT DETAIL TIMELINE
                Surface(
                    color = Color(0xFF0F172A),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "⏱️ TIMELINE DATA SATELIT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        TimelineStep(
                            label = "1. Observasi Satelit",
                            timeStr = FireAgeCalculator.formatDateTime(hotspot.acquisitionTimestamp),
                            subText = "Waktu satelit mendeteksi anomali termal"
                        )
                        TimelineStep(
                            label = "2. Diterima Server/Aplikasi",
                            timeStr = FireAgeCalculator.formatDateTime(hotspot.dataReceivedTimestamp),
                            subText = "Latensi transmisi: ${hotspot.deliveryLatencyMinutes?.let { "$it menit" } ?: "N/A"}"
                        )
                        TimelineStep(
                            label = "3. Dicek Perangkat Anda",
                            timeStr = FireAgeCalculator.formatDateTime(System.currentTimeMillis()),
                            subText = "Umur data saat ini: ${FireAgeCalculator.formatAgeDescription(hotspot.satelliteAgeMinutes)}"
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFF334155), modifier = Modifier.padding(vertical = 8.dp))

                // Feature 169: Verification Status
                DetailItem(label = "Status Deteksi", value = "HOTSPOT SATELIT TERDETEKSI")
                DetailItem(label = "Status Lapangan", value = "VERIFIKASI: BELUM DILAKUKAN")

                // Detail Rows with 6 decimals (Feature 128)
                DetailItem(label = "Latitude", value = String.format(java.util.Locale.US, "%.6f", hotspot.latitude))
                DetailItem(label = "Longitude", value = String.format(java.util.Locale.US, "%.6f", hotspot.longitude))
                DetailItem(
                    label = "Jarak dari Pengguna",
                    value = hotspot.distanceFromUser?.let { DistanceCalculator.formatDistanceKm(it) } ?: "Lokasi GPS belum tersedia"
                )
                DetailItem(label = "Waktu Observasi Satelit", value = FireAgeCalculator.formatDateTime(hotspot.acquisitionTimestamp))
                DetailItem(label = "Waktu Diterima Aplikasi", value = FireAgeCalculator.formatDateTime(hotspot.dataReceivedTimestamp))
                DetailItem(
                    label = "Latensi Pengiriman (Delivery Latency)",
                    value = hotspot.deliveryLatencyMinutes?.let { "$it menit" } ?: "Tidak tercatat"
                )
                DetailItem(label = "Satelit", value = hotspot.satellite)
                DetailItem(label = "Instrumen", value = hotspot.instrument)
                DetailItem(
                    label = "Brightness (Kecerahan)",
                    value = hotspot.brightness?.let { "$it K" } ?: "N/A"
                )
                if (hotspot.bright_ti5 != null) {
                    DetailItem(label = "Brightness T5 (TI-5)", value = "${hotspot.bright_ti5} K")
                }
                DetailItem(label = "Confidence Satelit", value = hotspot.confidence ?: "Nominal")
                if (hotspot.frp != null) {
                    DetailItem(label = "FRP (Fire Radiative Power)", value = "${hotspot.frp} MW")
                }
                if (hotspot.daynight != null) {
                    DetailItem(
                        label = "Waktu Deteksi",
                        value = if (hotspot.daynight.uppercase() == "D") "Siang Hari (Day)" else "Malam Hari (Night)"
                    )
                }
                DetailItem(label = "Sumber Data", value = hotspot.source)

                Spacer(modifier = Modifier.height(12.dp))

                // Important Thermal Disclaimer Note
                Surface(
                    color = Color(0x33FFB74D),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFFFFB74D),
                            modifier = Modifier.size(16.dp).padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Catatan: Ini merupakan deteksi anomali termal satelit, bukan konfirmasi kebakaran mutlak di lapangan.",
                            color = Color(0xFFFFE0B2),
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dismiss_dialog_button")
                ) {
                    Text("TUTUP", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun TimelineStep(
    label: String,
    timeStr: String,
    subText: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text("•", color = Color(0xFF38BDF8), fontSize = 14.sp, modifier = Modifier.padding(end = 6.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Text(timeStr, color = Color(0xFF94A3B8), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
            Text(subText, color = Color(0xFF64748B), fontSize = 10.sp)
        }
    }
}

@Composable
private fun DetailItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.LightGray,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}
