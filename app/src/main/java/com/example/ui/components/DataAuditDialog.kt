package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import com.example.data.model.DataAuditLog
import com.example.data.repository.FireAgeCalculator

@Composable
fun DataAuditDialog(
    logs: List<DataAuditLog>,
    onDismiss: () -> Unit,
    onClearLogs: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .border(1.dp, Color(0xFF334155), RoundedCornerShape(20.dp))
                .testTag("data_audit_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
            ) {
                // Title & Action Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = null,
                            tint = Color(0xFF00E676),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "DATA AUDIT LOG",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_audit_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.LightGray)
                    }
                }

                Text(
                    text = "Bukti otentik pertukaran data dengan server resmi NASA FIRMS.",
                    fontSize = 11.sp,
                    color = Color.LightGray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                HorizontalDivider(color = Color(0xFF1E293B))

                if (logs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Belum ada riwayat audit permintaan data.",
                            color = Color.Gray,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(logs) { log ->
                            AuditItemCard(log = log)
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFF1E293B))
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onClearLogs,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5252)),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("clear_audit_button")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("HAPUS LOG", fontSize = 11.sp)
                    }

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("TUTUP", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun AuditItemCard(log: DataAuditLog) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (log.isSuccess) Color(0xFF10B981) else Color(0xFFEF4444),
                RoundedCornerShape(12.dp)
            )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header: Status & Source
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (log.isSuccess) Color(0xFF10B981) else Color(0xFFEF4444))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (log.isSuccess) "SUKSES (${log.httpStatusCode ?: 200})" else "GAGAL (${log.httpStatusCode ?: "ERROR"})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (log.isSuccess) Color(0xFF10B981) else Color(0xFFEF4444)
                    )
                }

                Text(
                    text = log.source,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            AuditRow("Request Time", FireAgeCalculator.formatDateTime(log.requestTime))
            AuditRow("Response Time", FireAgeCalculator.formatDateTime(log.responseTime))
            AuditRow("Endpoint URL", log.endpointUrl, isMonospace = true)
            AuditRow("Number Received", "${log.numberReceived} records")
            AuditRow("Number Valid", "${log.numberValid} records")
            AuditRow("Number Rejected", "${log.numberRejected} records")

            if (log.rejectReason != null) {
                AuditRow("Reject Reason", log.rejectReason, isWarning = true)
            }

            if (log.latestAcquisitionTimestamp != null) {
                AuditRow("Latest Acquisition", FireAgeCalculator.formatDateTime(log.latestAcquisitionTimestamp))
            }
            if (log.oldestAcquisitionTimestamp != null) {
                AuditRow("Oldest Acquisition", FireAgeCalculator.formatDateTime(log.oldestAcquisitionTimestamp))
            }
            if (log.latestSatellite != null) {
                AuditRow("Latest Satellite", log.latestSatellite)
            }
            if (log.latestDataAgeMinutes != null) {
                AuditRow("Data Age", "${log.latestDataAgeMinutes} menit")
            }
            if (log.deliveryLatencyMinutes != null) {
                AuditRow("Delivery Latency", "${log.deliveryLatencyMinutes} menit")
            }
            if (log.errorMessage != null) {
                AuditRow("Error Detail", log.errorMessage, isError = true)
            }
        }
    }
}

@Composable
private fun AuditRow(
    label: String,
    value: String,
    isMonospace: Boolean = false,
    isWarning: Boolean = false,
    isError: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            color = Color.LightGray,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            fontSize = 10.sp,
            color = when {
                isError -> Color(0xFFFF5252)
                isWarning -> Color(0xFFFFB74D)
                else -> Color.White
            },
            fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default,
            modifier = Modifier.weight(0.6f),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}
