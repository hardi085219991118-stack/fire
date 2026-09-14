package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.engine.DataExportHelper
import com.example.data.engine.DataSnapshot
import com.example.data.engine.DataSnapshotEngine
import com.example.data.engine.ErrorObservability
import com.example.data.engine.SnapshotComparison
import com.example.data.model.DataAuditLog
import com.example.data.model.FireAlert
import com.example.data.model.Hotspot
import com.example.data.model.IncidentEntity
import com.example.data.repository.FireAgeCalculator

/**
 * Feature 319–338: REPORT GENERATION, DATA EXPORT & OBSERVABILITY DIALOG
 */
@Composable
fun ReportExportDialog(
    areaLabel: String,
    allHotspots: List<Hotspot>,
    latestObservationTimestamp: Long?,
    lastReceivedTimestamp: Long?,
    latencyFormatted: String,
    alerts: List<FireAlert>,
    incidents: List<IncidentEntity>,
    auditLogs: List<DataAuditLog>,
    previousSnapshot: DataSnapshot?,
    currentSnapshot: DataSnapshot?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf(0) } // 0 = Laporan, 1 = Export, 2 = Snapshot, 3 = Error Logs

    val reportText = remember(allHotspots, latestObservationTimestamp, lastReceivedTimestamp, alerts, incidents) {
        DataExportHelper.generateLaporanPemantauan(
            areaLabel = areaLabel,
            hotspots = allHotspots,
            latestObservationTimestamp = latestObservationTimestamp,
            lastReceivedTimestamp = lastReceivedTimestamp,
            latencyFormatted = latencyFormatted,
            alerts = alerts,
            incidents = incidents
        )
    }

    val snapshotComparison = remember(previousSnapshot, currentSnapshot) {
        if (previousSnapshot != null && currentSnapshot != null) {
            DataSnapshotEngine.compareSnapshots(previousSnapshot, currentSnapshot)
        } else null
    }

    val errorLogs = remember { ErrorObservability.getErrorLogs() }
    val sourceIncidents = remember { ErrorObservability.getSourceIncidents() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF090D16))
                .testTag("report_export_dialog"),
            color = Color(0xFF090D16)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "DOKUMENTASI, AUDIT & EXPORT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8),
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "📄 LAPORAN & AUDIT DATA",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Navigation Tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TabButton("1. LAPORAN", 0, activeTab) { activeTab = 0 }
                    TabButton("2. EXPORT", 1, activeTab) { activeTab = 1 }
                    TabButton("3. SNAPSHOT", 2, activeTab) { activeTab = 2 }
                    TabButton("4. SYSTEM LOG", 3, activeTab) { activeTab = 3 }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tab Content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    when (activeTab) {
                        0 -> {
                            // TAB 1: LAPORAN PEMANTAUAN RESMI (Feature 319–322)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Pratinjau Teks Laporan Resmi:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Button(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Laporan Fire Now", reportText))
                                        Toast.makeText(context, "Laporan disalin ke clipboard", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("SALIN TEKS", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF131D2E)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(10.dp))
                            ) {
                                Text(
                                    text = reportText,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFFE2E8F0),
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        }

                        1 -> {
                            // TAB 2: EXPORT CSV & JSON (Feature 323–328)
                            Text("Pilihan Format Ekspor Data Nyata:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Seluruh data diekspor secara jujur tanpa secret, token, atau API key.", fontSize = 11.sp, color = Color.LightGray)
                            Spacer(modifier = Modifier.height(10.dp))

                            ExportActionCard(
                                title = "📊 Export Hotspot Aktif (CSV)",
                                description = "Format spreadsheet standar berisi ${allHotspots.size} titik hotspot, koordinat, waktu observasi UTC, instrumen, dan status.",
                                onExport = {
                                    val csv = DataExportHelper.exportHotspotsToCsv(allHotspots, lastReceivedTimestamp)
                                    copyToClipboard(context, "hotspots.csv", csv)
                                }
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            ExportActionCard(
                                title = "🔧 Export Hotspot Teknis (JSON)",
                                description = "Format terstruktur untuk integrasi sistem geospasial atau audit data teknis.",
                                onExport = {
                                    val json = DataExportHelper.exportHotspotsToJson(allHotspots, lastReceivedTimestamp)
                                    copyToClipboard(context, "hotspots.json", json)
                                }
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            ExportActionCard(
                                title = "🛡️ Export Audit Log Transmisi (CSV)",
                                description = "Catatan audit ${auditLogs.size} riwayat panggilan data, status respons HTTP, dan latensi transmisi.",
                                onExport = {
                                    val csv = DataExportHelper.exportAuditLogsToCsv(auditLogs)
                                    copyToClipboard(context, "audit_logs.csv", csv)
                                }
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            ExportActionCard(
                                title = "📝 Export Catatan Pemantauan / Incidents (CSV)",
                                description = "Daftar ${incidents.size} catatan verifikasi lapangan yang dibuat petugas.",
                                onExport = {
                                    val csv = DataExportHelper.exportIncidentsToCsv(incidents)
                                    copyToClipboard(context, "incidents.csv", csv)
                                }
                            )
                        }

                        2 -> {
                            // TAB 3: SNAPSHOT & COMPARISON (Feature 329–331)
                            Text("Analisis Perbandingan Snapshot Data:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Spacer(modifier = Modifier.height(8.dp))

                            if (snapshotComparison != null) {
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131D2E)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, Color(0xFF0284C7), RoundedCornerShape(12.dp))
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text("PERBANDINGAN PEMBARUAN TERBARU", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(text = snapshotComparison.explanation, fontSize = 11.sp, color = Color.White)
                                    }
                                }
                            } else {
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131D2E)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text("Snapshot Saat Ini:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                                        Text("• Total Hotspot: ${currentSnapshot?.recordCount ?: allHotspots.size} titik", fontSize = 11.sp, color = Color.White)
                                        Text("• Checksum SHA-256: ${currentSnapshot?.checksum?.take(16) ?: "--"}...", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.LightGray)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Perbandingan snapshot otomatis terbentuk saat pembaruan data berikutnya masuk.", fontSize = 10.sp, color = Color.Gray)
                                    }
                                }
                            }
                        }

                        3 -> {
                            // TAB 4: SYSTEM LOG & OBSERVABILITY (Feature 335–338)
                            Text("Structured System & Error Observability:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Log telah disanitasi dari segala API key, token, dan rahasia.", fontSize = 11.sp, color = Color.LightGray)
                            Spacer(modifier = Modifier.height(8.dp))

                            if (sourceIncidents.isNotEmpty()) {
                                Surface(
                                    color = Color(0xFF450A0A),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("⚠️ SOURCE INCIDENTS TERDETEKSI", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFFFCA5A5))
                                        sourceIncidents.forEach { inc ->
                                            Text("• [HTTP ${inc.httpStatus}] ${inc.sourceName} gagal ${inc.consecutiveCount}x: ${inc.errorMessage}", fontSize = 10.sp, color = Color.White)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                            }

                            if (errorLogs.isEmpty()) {
                                Text("Tidak ada error sistem tercatat. Seluruh koneksi dan operasi normal.", fontSize = 11.sp, color = Color(0xFF10B981))
                            } else {
                                errorLogs.forEach { log ->
                                    Card(
                                        shape = RoundedCornerShape(8.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF131D2E)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(text = log.component, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFFF59E0B))
                                                Text(text = FireAgeCalculator.formatTimeOnly(log.timestamp), fontSize = 10.sp, color = Color.Gray)
                                            }
                                            Text(text = "${log.errorType} | ${log.message}", fontSize = 10.sp, color = Color.LightGray)
                                        }
                                    }
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
private fun TabButton(
    title: String,
    tabIndex: Int,
    activeTab: Int,
    onClick: () -> Unit
) {
    val isSelected = activeTab == tabIndex
    Surface(
        color = if (isSelected) Color(0xFF0284C7) else Color(0xFF1E293B),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
            modifier = Modifier.height(28.dp)
        ) {
            Text(
                text = title,
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) Color.White else Color.LightGray
            )
        }
    }
}

@Composable
private fun ExportActionCard(
    title: String,
    description: String,
    onExport: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131D2E)),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(10.dp))
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                Spacer(modifier = Modifier.height(2.dp))
                Text(description, fontSize = 10.sp, color = Color.LightGray)
            }
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(
                onClick = onExport,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(30.dp)
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("SALIN", fontSize = 10.sp, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, "$label disalin ke clipboard", Toast.LENGTH_SHORT).show()
}
