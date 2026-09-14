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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import com.example.data.repository.FireAgeCalculator

@Composable
fun TimeComparisonCard(
    currentDeviceTime: Long,
    latestObservationTime: Long?,
    lastReceivedTime: Long?,
    lastCheckTime: Long?,
    deliveryLatencyMinutes: Long?,
    satelliteAgeMinutes: Long?,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(16.dp))
            .testTag("time_comparison_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Title Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "PEMISAHAN & PERBANDINGAN 3 WAKTU",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3-Way Time Columns
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TimeBadgeBox(
                    title = "1. OBSERVASI SATELIT",
                    timeStr = FireAgeCalculator.formatTimeOnly(latestObservationTime),
                    subText = if (satelliteAgeMinutes != null && satelliteAgeMinutes != Long.MAX_VALUE) {
                        "${satelliteAgeMinutes}m lalu"
                    } else "Belum ada",
                    accentColor = Color(0xFF00E676),
                    modifier = Modifier.weight(1f)
                )

                TimeBadgeBox(
                    title = "2. DATA DITERIMA",
                    timeStr = FireAgeCalculator.formatTimeOnly(lastReceivedTime),
                    subText = if (deliveryLatencyMinutes != null) "Latensi +${deliveryLatencyMinutes}m" else "NRT",
                    accentColor = Color(0xFF38BDF8),
                    modifier = Modifier.weight(1f)
                )

                TimeBadgeBox(
                    title = "3. DICEK APLIKASI",
                    timeStr = FireAgeCalculator.formatTimeOnly(lastCheckTime),
                    subText = "Waktu Perangkat",
                    accentColor = Color(0xFFFFB74D),
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider(
                color = Color(0xFF334155),
                modifier = Modifier.padding(vertical = 12.dp)
            )

            // Exact Chronological Comparison
            TimeComparisonRow(
                label = "CURRENT DEVICE TIME",
                value = FireAgeCalculator.formatDateTime(currentDeviceTime),
                color = Color.White
            )
            TimeComparisonRow(
                label = "LATEST SATELLITE OBSERVATION",
                value = FireAgeCalculator.formatDateTime(latestObservationTime),
                color = Color(0xFF00E676)
            )
            TimeComparisonRow(
                label = "SATELLITE DATA AGE",
                value = if (satelliteAgeMinutes != null && satelliteAgeMinutes != Long.MAX_VALUE) {
                    FireAgeCalculator.formatAgeDescription(satelliteAgeMinutes)
                } else "Data tidak tersedia",
                color = if (satelliteAgeMinutes != null && satelliteAgeMinutes <= 60) Color(0xFF00E676) else Color(0xFFFFB74D)
            )
            TimeComparisonRow(
                label = "LATEST DATA RECEIVED",
                value = FireAgeCalculator.formatDateTime(lastReceivedTime),
                color = Color(0xFF38BDF8)
            )
            TimeComparisonRow(
                label = "DATA DELIVERY LATENCY",
                value = deliveryLatencyMinutes?.let { "$it menit" } ?: "Tidak tercatat / Menunggu response",
                color = Color.LightGray
            )
        }
    }
}

@Composable
private fun TimeBadgeBox(
    title: String,
    timeStr: String,
    subText: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF0F172A))
            .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
            .padding(8.dp)
    ) {
        Column {
            Text(
                text = title,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = timeStr,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subText,
                fontSize = 9.sp,
                color = Color.LightGray
            )
        }
    }
}

@Composable
private fun TimeComparisonRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color.LightGray
        )
        Text(
            text = value,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = color,
            fontFamily = FontFamily.Monospace
        )
    }
}
