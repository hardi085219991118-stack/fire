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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.example.data.model.DataQualityReport
import com.example.data.model.Hotspot
import com.example.data.model.QualityScoreLevel
import com.example.data.model.SourceComparisonReport
import com.example.data.model.TemporalChangeResult
import com.example.data.repository.DistanceCalculator
import com.example.data.repository.FireAgeCalculator

/**
 * Feature 259: FINAL FIELD DASHBOARD
 * Menjawab 11 pertanyaan inti lapangan secara terstruktur, objektif, dan transparan.
 */
@Composable
fun FieldDashboardDialog(
    latestHotspot: Hotspot?,
    nearestHotspot: Hotspot?,
    isFreshEnoughForFireNow: Boolean,
    dataReceivedTimestamp: Long?,
    deliveryLatencyFormatted: String,
    newHotspotsCount: Int,
    temporalChange: TemporalChangeResult?,
    alertsCount: Int,
    dataQualityReport: DataQualityReport?,
    sourceComparison: SourceComparisonReport?,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Assessment, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Dashboard Lapangan 11 Indikator", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("field_dashboard_list"),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        "Ringkasan 11 Jawaban Pemantauan Lapangan (Feature 259):",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // 1. 🔥 ADA HOTSPOT TERKINI ATAU TIDAK?
                item {
                    FieldAnswerCard(
                        number = "1",
                        question = "🔥 ADA HOTSPOT TERKINI ATAU TIDAK?",
                        answer = if (isFreshEnoughForFireNow && latestHotspot != null) {
                            "ADA (Terdeteksi ${latestHotspot.satelliteAgeMinutes} menit lalu)"
                        } else {
                            "TIDAK ADA DATA FIRE NOW TERKINI (≤30 menit)"
                        },
                        isHighlight = isFreshEnoughForFireNow,
                        highlightColor = if (isFreshEnoughForFireNow) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                }

                // 2. 📍 DIMANA?
                item {
                    val locStr = latestHotspot?.let {
                        "[${String.format("%.4f", it.latitude)}, ${String.format("%.4f", it.longitude)}]"
                    } ?: "Tidak ada titik observasi"
                    FieldAnswerCard(
                        number = "2",
                        question = "📍 DIMANA?",
                        answer = locStr
                    )
                }

                // 3. 📏 SEBERAPA DEKAT?
                item {
                    val distStr = nearestHotspot?.distanceFromUser?.let {
                        "${DistanceCalculator.formatDistanceKm(it)} dari lokasi pengguna"
                    } ?: "Jarak pengguna tidak terukur (GPS belum aktif/titik nihil)"
                    FieldAnswerCard(
                        number = "3",
                        question = "📏 SEBERAPA DEKAT?",
                        answer = distStr
                    )
                }

                // 4. ⏱ DIAMATI KAPAN?
                item {
                    val obsStr = latestHotspot?.let {
                        "${FireAgeCalculator.formatDateTimeIso(it.acquisitionTimestamp)} (Waktu Satelit Asli)"
                    } ?: "Belum ada observasi tercatat"
                    FieldAnswerCard(
                        number = "4",
                        question = "⏱ DIAMATI KAPAN?",
                        answer = obsStr
                    )
                }

                // 5. ⌛ UMURNYA BERAPA?
                item {
                    val ageStr = latestHotspot?.let {
                        "${it.satelliteAgeMinutes} menit (${it.status.label})"
                    } ?: "Tidak tersedia"
                    FieldAnswerCard(
                        number = "5",
                        question = "⌛ UMURNYA BERAPA?",
                        answer = ageStr
                    )
                }

                // 6. 🛰 SATELIT APA?
                item {
                    val satStr = latestHotspot?.let {
                        "${it.satellite} (${it.instrument})"
                    } ?: "Belum ada satelit terpilih"
                    FieldAnswerCard(
                        number = "6",
                        question = "🛰 SATELIT APA?",
                        answer = satStr
                    )
                }

                // 7. 📡 DATA DITERIMA KAPAN?
                item {
                    val recStr = dataReceivedTimestamp?.let {
                        FireAgeCalculator.formatDateTimeIso(it)
                    } ?: "Belum ada data diterima"
                    FieldAnswerCard(
                        number = "7",
                        question = "📡 DATA DITERIMA KAPAN?",
                        answer = recStr
                    )
                }

                // 8. ⚡ BERAPA LATENCY?
                item {
                    FieldAnswerCard(
                        number = "8",
                        question = "⚡ BERAPA LATENCY PENGIRIMAN?",
                        answer = deliveryLatencyFormatted
                    )
                }

                // 9. 🆕 APAKAH INI DATA BARU?
                item {
                    val isNew = (newHotspotsCount > 0)
                    FieldAnswerCard(
                        number = "9",
                        question = "🆕 APAKAH INI DATA BARU?",
                        answer = if (isNew) {
                            "YA (Terdapat $newHotspotsCount record baru pertama kali masuk aplikasi)"
                        } else {
                            "BUKAN (Tidak ada record baru pada refresh ini)"
                        },
                        isHighlight = isNew,
                        highlightColor = if (isNew) Color(0xFF0288D1) else Color.Gray
                    )
                }

                // 10. 📊 APAKAH JUMLAH HOTSPOT BERUBAH?
                item {
                    val changeStr = temporalChange?.summaryMessage ?: "Belum ada pembanding dataset sebelumnya"
                    FieldAnswerCard(
                        number = "10",
                        question = "📊 APAKAH JUMLAH HOTSPOT BERUBAH?",
                        answer = changeStr
                    )
                }

                // 11. ⚠️ APAKAH ADA PERINGATAN?
                item {
                    val alertStr = if (alertsCount > 0) {
                        "ADA ($alertsCount peringatan pemantauan aktif)"
                    } else {
                        "NIHIL (Tidak ada peringatan prioritas tinggi)"
                    }
                    FieldAnswerCard(
                        number = "11",
                        question = "⚠️ APAKAH ADA PERINGATAN?",
                        answer = alertStr,
                        isHighlight = alertsCount > 0,
                        highlightColor = if (alertsCount > 0) Color(0xFFE65100) else Color(0xFF2E7D32)
                    )
                }

                // Data Quality Score (Feature 257 & 258)
                if (dataQualityReport != null) {
                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = when (dataQualityReport.level) {
                                    QualityScoreLevel.HIGH -> Color(0xFF2E7D32).copy(alpha = 0.1f)
                                    QualityScoreLevel.MEDIUM -> Color(0xFFE65100).copy(alpha = 0.1f)
                                    QualityScoreLevel.LOW -> Color(0xFFC62828).copy(alpha = 0.1f)
                                }
                            )
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "SKOR KUALITAS DATA:",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        "${dataQualityReport.level.label} (${dataQualityReport.scorePercent}%)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = when (dataQualityReport.level) {
                                            QualityScoreLevel.HIGH -> Color(0xFF2E7D32)
                                            QualityScoreLevel.MEDIUM -> Color(0xFFE65100)
                                            QualityScoreLevel.LOW -> Color(0xFFC62828)
                                        }
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                dataQualityReport.explanationReasons.forEach { r ->
                                    Text(r, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                }

                // Source Comparison / Failover info (Feature 252-255)
                if (sourceComparison?.failoverWarningMessage != null) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
                        ) {
                            Text(
                                sourceComparison.failoverWarningMessage,
                                modifier = Modifier.padding(8.dp),
                                fontSize = 11.sp,
                                color = Color(0xFFE65100),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("close_field_dashboard_btn")
            ) {
                Text("TUTUP")
            }
        }
    )
}

@Composable
fun FieldAnswerCard(
    number: String,
    question: String,
    answer: String,
    isHighlight: Boolean = false,
    highlightColor: Color = MaterialTheme.colorScheme.primary
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isHighlight) highlightColor.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                question,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = if (isHighlight) highlightColor else MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                answer,
                fontSize = 12.sp,
                fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Normal,
                fontFamily = FontFamily.SansSerif,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
