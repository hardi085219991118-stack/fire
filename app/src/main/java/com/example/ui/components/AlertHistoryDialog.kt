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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AlertSeverity
import com.example.data.model.FireAlert
import com.example.data.repository.DistanceCalculator
import com.example.data.repository.FireAgeCalculator

/**
 * Feature 233, 234, 235: RIWAYAT PERINGATAN (ALERT HISTORY)
 * Menampilkan histori notifikasi/alert pemantauan dengan pembedaan ketat antara
 * Waktu Observasi Satelit dan Waktu Aplikasi Memberikan Alert (alertedAt).
 */
@Composable
fun AlertHistoryDialog(
    alerts: List<FireAlert>,
    onDismiss: () -> Unit,
    onClearHistory: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Riwayat Peringatan (${alerts.size})",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Peringatan dibuat berdasarkan anomali termal satelit yang baru masuk dan memenuhi kriteria filter. BUKAN konfirmasi kebakaran di lapangan.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (alerts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Belum ada riwayat peringatan.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(alerts) { alert ->
                            AlertCardItem(alert = alert)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("close_alert_history_btn")
            ) {
                Text("TUTUP")
            }
        },
        dismissButton = {
            if (alerts.isNotEmpty()) {
                OutlinedButton(
                    onClick = onClearHistory,
                    modifier = Modifier.testTag("clear_alert_history_btn")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("BERSIHKAN")
                }
            }
        }
    )
}

@Composable
fun AlertCardItem(alert: FireAlert) {
    val severityColor = when (alert.severity) {
        AlertSeverity.CRITICAL -> Color(0xFFB71C1C)
        AlertSeverity.HIGH -> Color(0xFFD32F2F)
        AlertSeverity.MEDIUM -> Color(0xFFE65100)
        AlertSeverity.LOW -> Color(0xFFF57C00)
        AlertSeverity.INFO -> Color(0xFF0288D1)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("alert_item_${alert.alertId}"),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = severityColor.copy(alpha = 0.08f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    alert.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = severityColor
                )
                Box(
                    modifier = Modifier
                        .background(severityColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        alert.severity.label,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = severityColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Feature 234 & 235: Observasi vs AlertedAt
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "Waktu Observasi Satelit:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    FireAgeCalculator.formatDateTimeIso(alert.observationTimestamp),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Monospace
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "Waktu Alert Aplikasi:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    FireAgeCalculator.formatDateTimeIso(alert.alertedAt),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Monospace
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 6.dp),
                color = severityColor.copy(alpha = 0.2f)
            )

            // Explanations
            alert.explanationLines.forEach { line ->
                Text(
                    line,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Feature 230: Safety Notice
            Text(
                "⚠️ ${alert.safetyDisclaimer}",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFC62828),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
