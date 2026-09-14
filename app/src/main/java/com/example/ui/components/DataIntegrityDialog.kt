package com.example.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.window.Dialog
import com.example.data.engine.DataIntegrityReport
import com.example.data.repository.FireAgeCalculator

/**
 * Feature 126 & 178: SOURCE RESPONSE COUNTER & DATA INTEGRITY REPORT
 * Feature 142 & 143: SOURCE HEALTH & API HEALTH CHECK TRIGGER
 * Feature 151: MULTI-SATELLITE COMPARISON
 */
@Composable
fun DataIntegrityDialog(
    integrityReport: DataIntegrityReport?,
    clockSyncWarning: String?,
    serverTimeOffsetMs: Long,
    isDataStalled: Boolean,
    stallMessage: String?,
    isLatencyHigh: Boolean,
    latencyMessage: String?,
    isDataGap: Boolean,
    dataGapMessage: String?,
    multiSatelliteSummary: Map<String, Long>,
    isRunningHealthCheck: Boolean,
    onRunHealthCheck: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF334155), RoundedCornerShape(20.dp))
                .testTag("data_integrity_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF00E676))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AUDIT INTEGRITAS DATA",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.LightGray)
                    }
                }

                Text(
                    text = "Prinsip Kejujuran Data 100%: Menjamin tidak ada titik api dummy, manipulasi timestamp, atau klaim real-time palsu.",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8),
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                HorizontalDivider(color = Color(0xFF334155), modifier = Modifier.padding(vertical = 8.dp))

                // Feature 126: Source Counter Table
                if (integrityReport != null) {
                    Surface(
                        color = Color(0xFF0F172A),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "PENGHITUNG RESPON SUMBER (INVARIANT)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF38BDF8)
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            AuditRow("1. Baris Data Diterima (Received)", "${integrityReport.recordsReceived}")
                            AuditRow("2. Baris Data Valid (Valid)", "${integrityReport.recordsValid}")
                            AuditRow("3. Baris Ditolak / Rusak (Rejected)", "${integrityReport.recordsRejected}")
                            AuditRow("4. Baris Ditampilkan (Displayed)", "${integrityReport.recordsDisplayed}")
                            AuditRow("5. Duplikat Dihapus (Deduplicated)", "${integrityReport.duplicatesRemoved}")

                            Spacer(modifier = Modifier.height(6.dp))
                            val invariantText = if (integrityReport.isContractSatisfied) {
                                "✅ Invariant Terpenuhi: Displayed (${integrityReport.recordsDisplayed}) ≤ Valid (${integrityReport.recordsValid}) ≤ Received (${integrityReport.recordsReceived})"
                            } else {
                                "⚠️ Invariant Gagal: Displayed > Valid atau Valid > Received"
                            }
                            Text(
                                text = invariantText,
                                fontSize = 10.sp,
                                color = if (integrityReport.isContractSatisfied) Color(0xFF00E676) else Color(0xFFFF5252),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Clock Sync Status (Feature 105 & 106)
                Surface(
                    color = if (clockSyncWarning != null) Color(0x33FF9800) else Color(0xFF0F172A),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "SINKRONISASI JAM SERVER NASA",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (clockSyncWarning != null) Color(0xFFFF9800) else Color(0xFF94A3B8)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val offsetSec = serverTimeOffsetMs / 1000
                        AuditRow("Offset Jam Perangkat vs Server", "$offsetSec detik")
                        if (clockSyncWarning != null) {
                            Text(text = clockSyncWarning, color = Color(0xFFFFB74D), fontSize = 11.sp)
                        } else {
                            Text(text = "✅ Jam perangkat sinkron dengan waktu acuan NASA.", color = Color(0xFF00E676), fontSize = 11.sp)
                        }
                    }
                }

                // Warnings: Stall, Latency Spike, Gap (Feature 148, 149, 150)
                if (isDataStalled || isLatencyHigh || isDataGap) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = Color(0x33FFB74D),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            if (isDataStalled && stallMessage != null) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFFB74D), modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(stallMessage, fontSize = 11.sp, color = Color(0xFFFFE0B2))
                                }
                            }
                            if (isLatencyHigh && latencyMessage != null) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFFB74D), modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(latencyMessage, fontSize = 11.sp, color = Color(0xFFFFE0B2))
                                }
                            }
                            if (isDataGap && dataGapMessage != null) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFFB74D), modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(dataGapMessage, fontSize = 11.sp, color = Color(0xFFFFE0B2))
                                }
                            }
                        }
                    }
                }

                // Feature 151: Multi-Satellite Comparison
                if (multiSatelliteSummary.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = Color(0xFF0F172A),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "🛰️ OBSERVASI TERAKHIR PER SATELIT",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF38BDF8)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            multiSatelliteSummary.forEach { (sat, ts) ->
                                AuditRow(sat, FireAgeCalculator.formatDateTime(ts))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Feature 143: Test Data Source Button
                Button(
                    onClick = onRunHealthCheck,
                    enabled = !isRunningHealthCheck,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    modifier = Modifier.fillMaxWidth().testTag("test_source_button")
                ) {
                    if (isRunningHealthCheck) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("MEMERIKSA KONEKSI NASA FIRMS...", fontSize = 12.sp)
                    } else {
                        Text("⚡ TEST DATA SOURCE (HEALTH CHECK)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun AuditRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
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
