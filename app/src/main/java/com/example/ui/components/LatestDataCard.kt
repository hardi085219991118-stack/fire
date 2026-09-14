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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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
import com.example.data.model.HotspotAgeStatus
import com.example.data.repository.FireAgeCalculator

/**
 * Feature 111: LATEST DATA CARD
 *
 * Card:
 * 🔥 DATA SATELIT TERBARU
 * Observasi: HH:MM:SS WIB
 * Umur: XX menit XX detik
 * Diterima aplikasi: HH:MM:SS WIB
 * Delivery latency: XX menit XX detik
 * Sumber: NASA FIRMS
 * Status: TERKINI / TERLAMBAT / HISTORIS
 */
@Composable
fun LatestDataCard(
    latestObservationTime: Long?,
    latestSatelliteName: String?,
    dataReceivedTime: Long?,
    deliveryLatencyFormatted: String,
    newestDataAgeFormatted: String,
    status: HotspotAgeStatus,
    realDataBadge: String,
    sourceName: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(16.dp))
            .testTag("latest_data_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(status.composeColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "🔥 DATA SATELIT TERBARU",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                }

                // Feature 141: Real Data Badge
                Surface(
                    color = if (realDataBadge.contains("VERIFIED")) Color(0x3300E676) else Color(0x33FFB74D),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = realDataBadge,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (realDataBadge.contains("VERIFIED")) Color(0xFF00E676) else Color(0xFFFFB74D),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Primary Stat: Second-Level Age
            Surface(
                color = status.composeColor.copy(alpha = 0.12f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "UMUR OBSERVASI",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.LightGray
                        )
                        Text(
                            text = newestDataAgeFormatted,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Text(
                        text = status.label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = status.composeColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFF334155))
            Spacer(modifier = Modifier.height(8.dp))

            // Data Rows
            LatestRowItem(
                label = "Waktu Observasi Satelit",
                value = latestObservationTime?.let { FireAgeCalculator.formatDateTime(it) } ?: "Belum ada observasi"
            )
            LatestRowItem(
                label = "Satelit / Sensor",
                value = latestSatelliteName ?: "NASA FIRMS (VIIRS/MODIS)"
            )
            LatestRowItem(
                label = "Diterima Aplikasi",
                value = dataReceivedTime?.let { FireAgeCalculator.formatDateTime(it) } ?: "Belum pernah diambil"
            )
            LatestRowItem(
                label = "Delivery Latency",
                value = deliveryLatencyFormatted
            )
            LatestRowItem(
                label = "Sumber Data Resmi",
                value = sourceName
            )
        }
    }
}

@Composable
private fun LatestRowItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 11.sp, color = Color(0xFF94A3B8))
        Text(
            text = value,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            fontFamily = FontFamily.Monospace
        )
    }
}
