package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MonitoringSession
import com.example.data.repository.FireAgeCalculator

/**
 * Feature 206–213: MONITORING STATUS & HEALTH CARD
 */
@Composable
fun MonitoringStatusCard(
    session: MonitoringSession?,
    latestObsTimestamp: Long?,
    latestDataAgeFormatted: String,
    onStartMonitoring: () -> Unit,
    onStopMonitoring: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isMonitoring = session?.isActive == true

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("monitoring_status_card"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isMonitoring) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(if (isMonitoring) Color(0xFF2E7D32) else Color.Gray, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        if (isMonitoring) "🟢 PEMANTAUAN AKTIF" else "⚪ PEMANTAUAN TIDAK AKTIF",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (isMonitoring) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (!isMonitoring) {
                    Button(
                        onClick = onStartMonitoring,
                        modifier = Modifier.testTag("start_monitoring_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                    ) {
                        Icon(Icons.Default.PlayCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("MULAI PEMANTAUAN", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    OutlinedButton(
                        onClick = onStopMonitoring,
                        modifier = Modifier.testTag("stop_monitoring_btn"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC62828))
                    ) {
                        Icon(Icons.Default.PauseCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("HENTIKAN", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (isMonitoring && session != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        "MONITORING HEALTH (Sesi: ${session.sessionId.takeLast(6)})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1B5E20)
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Area Dipantau:", fontSize = 11.sp, color = Color.DarkGray)
                        Text("${session.locationName} (R: ${session.radiusKm.toInt()} km)", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Pemeriksaan Terakhir:", fontSize = 11.sp, color = Color.DarkGray)
                        Text(
                            session.lastCheckTime?.let { FireAgeCalculator.formatTimeOnly(it) } ?: "Sedang berlangsung",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Observasi Satelit Terbaru:", fontSize = 11.sp, color = Color.DarkGray)
                        Text(
                            latestObsTimestamp?.let { FireAgeCalculator.formatTimeOnly(it) } ?: "Tidak tersedia",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Umur Data:", fontSize = 11.sp, color = Color.DarkGray)
                        Text(latestDataAgeFormatted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    if (session.missedChecksCount > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "⚠️ Pembaruan terlewat (${session.missedChecksCount}x): ${session.lastMissedReason ?: "koneksi/rate-limit"}",
                                fontSize = 10.sp,
                                color = Color(0xFFE65100),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}
