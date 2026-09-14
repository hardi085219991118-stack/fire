package com.example.ui.components

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.FieldVerificationStatus
import com.example.data.model.Hotspot
import com.example.data.model.IncidentEntity
import com.example.data.model.IncidentStatus
import com.example.data.repository.DistanceCalculator
import com.example.data.repository.FireAgeCalculator

/**
 * Feature 301–318: INCIDENT MANAGEMENT & GROUND FIELD VERIFICATION
 */
@Composable
fun IncidentManagementDialog(
    incidents: List<IncidentEntity>,
    selectedHotspot: Hotspot?,
    currentCenterLat: Double,
    currentCenterLon: Double,
    onCreateIncident: (title: String, lat: Double, lon: Double, linkedFp: String?, satTime: Long?, satName: String?) -> Unit,
    onUpdateVerification: (id: String, status: FieldVerificationStatus, notes: String, observer: String) -> Unit,
    onDeleteIncident: (id: String) -> Unit,
    onDismiss: () -> Unit
) {
    var showCreateForm by remember { mutableStateOf(false) }
    var selectedIncidentForVerification by remember { mutableStateOf<IncidentEntity?>(null) }

    // Form inputs
    var newTitle by remember {
        mutableStateOf(selectedHotspot?.let { "Pemantauan Anomali ${it.satellite} #${it.id.takeLast(4)}" } ?: "Catatan Pemantauan Baru")
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF090D16))
                .testTag("incident_management_dialog"),
            color = Color(0xFF090D16)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "MANAJEMEN PEMANTAUAN & VERIFIKASI",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8),
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "📋 CATATAN INCIDENT & LAPANGAN",
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

                // Create Action
                if (!showCreateForm) {
                    Button(
                        onClick = { showCreateForm = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("create_incident_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("BUAT CATATAN PEMANTAUAN BARU", fontWeight = FontWeight.Bold)
                    }
                } else {
                    // Create Form Card
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF131D2E)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(14.dp))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Buat Catatan Pemantauan",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = newTitle,
                                onValueChange = { newTitle = it },
                                label = { Text("Judul Catatan Pemantauan") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF38BDF8)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            val lat = selectedHotspot?.latitude ?: currentCenterLat
                            val lon = selectedHotspot?.longitude ?: currentCenterLon
                            Text(
                                text = "Titik Acuan: ${String.format(java.util.Locale.US, "%.5f, %.5f", lat, lon)}",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.LightGray
                            )

                            if (selectedHotspot != null) {
                                Text(
                                    text = "Terkait Hotspot Satelit: ${selectedHotspot.satellite} (${FireAgeCalculator.formatTimeOnly(selectedHotspot.acquisitionTimestamp)})",
                                    fontSize = 11.sp,
                                    color = Color(0xFFFFB74D)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { showCreateForm = false },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Batal", color = Color.LightGray)
                                }
                                Button(
                                    onClick = {
                                        onCreateIncident(
                                            newTitle.ifBlank { "Catatan Pemantauan" },
                                            lat,
                                            lon,
                                            selectedHotspot?.fingerprint ?: selectedHotspot?.id,
                                            selectedHotspot?.acquisitionTimestamp,
                                            selectedHotspot?.satellite
                                        )
                                        showCreateForm = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Simpan", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Verification Form if editing
                selectedIncidentForVerification?.let { inc ->
                    var fieldNotesInput by remember { mutableStateOf(inc.fieldNotes) }
                    var observerInput by remember { mutableStateOf(inc.fieldObserverName) }
                    var selectedStatus by remember { mutableStateOf(inc.fieldVerificationStatus) }

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFF00E676), RoundedCornerShape(14.dp))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Form Verifikasi Lapangan: ${inc.title}",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00E676),
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            OutlinedTextField(
                                value = observerInput,
                                onValueChange = { observerInput = it },
                                label = { Text("Nama Petugas / Pengamat") },
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            OutlinedTextField(
                                value = fieldNotesInput,
                                onValueChange = { fieldNotesInput = it },
                                label = { Text("Catatan Hasil Verifikasi Lapangan") },
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text("Status Verifikasi Lapangan:", fontSize = 11.sp, color = Color.LightGray)
                            FieldVerificationStatus.values().forEach { st ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                ) {
                                    androidx.compose.material3.RadioButton(
                                        selected = selectedStatus == st,
                                        onClick = { selectedStatus = st }
                                    )
                                    Text(text = st.label, fontSize = 11.sp, color = Color.White)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { selectedIncidentForVerification = null },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Batal", color = Color.LightGray)
                                }
                                Button(
                                    onClick = {
                                        onUpdateVerification(inc.id, selectedStatus, fieldNotesInput, observerInput)
                                        selectedIncidentForVerification = null
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Update Verifikasi", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Incidents List
                Text(
                    text = "Daftar Catatan Pemantauan (${incidents.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(6.dp))

                if (incidents.isEmpty()) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF131D2E)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Belum ada catatan pemantauan atau verifikasi lapangan dibuat.", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                } else {
                    incidents.forEach { inc ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF131D2E)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = inc.title,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Surface(
                                        color = Color(0xFF0284C7).copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = inc.status.label,
                                            color = Color(0xFF38BDF8),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "Status Lapangan: ${inc.fieldVerificationStatus.label}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (inc.fieldVerificationStatus == FieldVerificationStatus.DIVERIFIKASI_LAPANGAN) Color(0xFF00E676) else Color(0xFFFFB74D)
                                )

                                Text(
                                    text = "Dibuat: ${FireAgeCalculator.formatDateTime(inc.createdAt)}",
                                    fontSize = 10.sp,
                                    color = Color.LightGray
                                )

                                if (inc.fieldNotes.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "📝 Catatan: ${inc.fieldNotes}",
                                        fontSize = 11.sp,
                                        color = Color(0xFFE2E8F0)
                                    )
                                }

                                if (inc.satelliteObservationTimestamp != null) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    // Feature 310: VERIFICATION TIMELINE
                                    Surface(
                                        color = Color(0xFF0F172A),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(6.dp)) {
                                            Text("Timeline Kronologis:", fontSize = 10.sp, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                                            Text("1. Observasi Satelit: ${FireAgeCalculator.formatTimeOnly(inc.satelliteObservationTimestamp)} (${inc.satelliteName ?: "VIIRS"})", fontSize = 9.sp, color = Color.LightGray)
                                            Text("2. Pembuatan Catatan: ${FireAgeCalculator.formatTimeOnly(inc.createdAt)}", fontSize = 9.sp, color = Color.LightGray)
                                            if (inc.fieldObservationTimestamp != null) {
                                                Text("3. Verifikasi Darat: ${FireAgeCalculator.formatTimeOnly(inc.fieldObservationTimestamp)}", fontSize = 9.sp, color = Color.LightGray)
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    OutlinedButton(
                                        onClick = { selectedIncidentForVerification = inc },
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Verifikasi", fontSize = 10.sp, color = Color(0xFF00E676))
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(
                                        onClick = { onDeleteIncident(inc.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
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
