package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.window.DialogProperties
import com.example.data.engine.GeodesicBearingCalculator
import com.example.data.engine.HotspotPriorityMatrix
import com.example.data.model.Hotspot
import com.example.data.model.HotspotAgeStatus
import com.example.data.model.UserLocation
import com.example.data.repository.FireAgeCalculator

/**
 * Feature 261–271: FIELD OPERATION MODE (Mode Operasi Lapangan)
 * Didesain khusus untuk pembacaan di luar ruangan dengan kontras tinggi,
 * angka besar, indikasi GPS lock, dan kalkulasi bearing geografis.
 */
@Composable
fun FieldOperationDialog(
    userLocation: UserLocation?,
    allHotspots: List<Hotspot>,
    newestDataAgeMinutes: Long?,
    newestDataStatus: HotspotAgeStatus,
    isNetworkOnline: Boolean = true,
    onDismiss: () -> Unit
) {
    // Feature 264: DAY MODE / NIGHT MODE (Outdoor Contrast toggle)
    var isDayMode by remember { mutableStateOf(false) }
    var followGps by remember { mutableStateOf(true) }

    val bgColor = if (isDayMode) Color(0xFFF1F5F9) else Color(0xFF090D16)
    val cardBg = if (isDayMode) Color(0xFFFFFFFF) else Color(0xFF131D2E)
    val textColor = if (isDayMode) Color(0xFF0F172A) else Color(0xFFFFFFFF)
    val subtextColor = if (isDayMode) Color(0xFF475569) else Color(0xFF94A3B8)
    val borderColor = if (isDayMode) Color(0xFFCBD5E1) else Color(0xFF1E293B)

    // Feature 270: HOTSPOT TERKINI TERDEKAT (≤30m)
    val nearestCurrent = HotspotPriorityMatrix.findNearestCurrentHotspot(allHotspots, 30L)

    // Feature 271: HOTSPOT TERBARU TERDEKAT (terbaru secara observasi)
    val nearestRecent = HotspotPriorityMatrix.findNearestRecentHotspot(allHotspots)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor)
                .testTag("field_operation_dialog"),
            color = bgColor
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Feature 262: FIELD MODE HEADER
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "HARDI MANTANGAI FIRE NOW",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = subtextColor,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "🔥 MODE OPERASI LAPANGAN",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = textColor
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { isDayMode = !isDayMode }) {
                            Icon(
                                imageVector = if (isDayMode) Icons.Default.Nightlight else Icons.Default.LightMode,
                                contentDescription = "Toggle Day/Night Mode",
                                tint = if (isDayMode) Color(0xFFD97706) else Color(0xFFFFD54F)
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Tutup",
                                tint = textColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Feature 262: STATUS BAR (Data, GPS, Network)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Status Data
                    val (dataText, dataColor) = when (newestDataStatus) {
                        HotspotAgeStatus.SANGAT_TERKINI, HotspotAgeStatus.TERKINI -> "🟢 DATA TERKINI" to Color(0xFF10B981)
                        HotspotAgeStatus.TERBARU_TETAPI_TERLAMBAT, HotspotAgeStatus.TERLAMBAT -> "🟡 DATA TERLAMBAT" to Color(0xFFF59E0B)
                        else -> "🔴 HISTORIS" to Color(0xFFEF4444)
                    }
                    StatusPill(text = dataText, color = dataColor, isDayMode = isDayMode, modifier = Modifier.weight(1f))

                    // Status GPS
                    val (gpsText, gpsColor) = if (userLocation != null) {
                        "🟢 GPS AKTIF" to Color(0xFF10B981)
                    } else {
                        "🔴 GPS OFF" to Color(0xFFEF4444)
                    }
                    StatusPill(text = gpsText, color = gpsColor, isDayMode = isDayMode, modifier = Modifier.weight(1f))

                    // Status Network
                    val (netText, netColor) = if (isNetworkOnline) {
                        "🟢 ONLINE" to Color(0xFF10B981)
                    } else {
                        "🔴 OFFLINE" to Color(0xFFEF4444)
                    }
                    StatusPill(text = netText, color = netColor, isDayMode = isDayMode, modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Feature 266: FIELD LOCATION LOCK (Posisi Saya, Akurasi, Waktu Lokasi)
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, borderColor, RoundedCornerShape(14.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.GpsFixed,
                                    contentDescription = null,
                                    tint = Color(0xFF0284C7),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "📍 POSISI SAYA",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = textColor
                                )
                            }
                            if (userLocation != null) {
                                Surface(
                                    color = Color(0xFF0284C7).copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "Akurasi: ±${userLocation.accuracyMeters.toInt()}m",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF38BDF8),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        if (userLocation != null) {
                            Text(
                                text = "Koordinat: ${String.format(java.util.Locale.US, "%.5f, %.5f", userLocation.latitude, userLocation.longitude)}",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = textColor
                            )
                            val locAgeSec = (System.currentTimeMillis() - userLocation.timestamp) / 1000
                            Text(
                                text = "Waktu GPS: ${FireAgeCalculator.formatTimeOnly(userLocation.timestamp)} (${locAgeSec}d lalu)",
                                fontSize = 11.sp,
                                color = subtextColor
                            )
                        } else {
                            Text(
                                text = "Menunggu kuncian sinyal GPS...",
                                fontSize = 12.sp,
                                color = Color(0xFFF59E0B)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Feature 270: HOTSPOT TERKINI TERDEKAT (≤30 MENIT)
                Text(
                    text = "1. HOTSPOT TERKINI TERDEKAT (≤30 MENIT)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color(0xFFFF5722)
                )
                Spacer(modifier = Modifier.height(6.dp))

                if (nearestCurrent != null) {
                    LargeHotspotDisplayCard(
                        hotspot = nearestCurrent,
                        userLocation = userLocation,
                        cardBg = cardBg,
                        borderColor = Color(0xFFFF5722),
                        textColor = textColor,
                        subtextColor = subtextColor,
                        isCurrentMode = true
                    )
                } else {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🟢", fontSize = 28.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "TIDAK ADA HOTSPOT TERKINI DALAM 30 MENIT",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = textColor
                            )
                            Text(
                                text = "Tidak terdeteksi anomali termal satelit yang diobservasi dalam rentang waktu 30 menit terakhir.",
                                fontSize = 11.sp,
                                color = subtextColor,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Feature 271: HOTSPOT TERBARU TERDEKAT (TERSEDIA)
                Text(
                    text = "2. HOTSPOT TERBARU TERDEKAT (DATA TERSEDIA)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color(0xFF0284C7)
                )
                Spacer(modifier = Modifier.height(6.dp))

                if (nearestRecent != null && nearestRecent != nearestCurrent) {
                    LargeHotspotDisplayCard(
                        hotspot = nearestRecent,
                        userLocation = userLocation,
                        cardBg = cardBg,
                        borderColor = Color(0xFF0284C7),
                        textColor = textColor,
                        subtextColor = subtextColor,
                        isCurrentMode = false
                    )
                } else if (nearestRecent != null && nearestRecent == nearestCurrent) {
                    Text(
                        text = "ℹ️ Hotspot terbaru yang tersedia identik dengan hotspot terkini di atas.",
                        fontSize = 11.sp,
                        color = subtextColor
                    )
                } else {
                    Text(
                        text = "Tidak ada rekaman hotspot satelit yang tersedia pada dataset ini.",
                        fontSize = 11.sp,
                        color = subtextColor
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Safety Notice
                Surface(
                    color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Hotspot satelit merupakan anomali radiasi termal permukaan dan BUKAN konfirmasi pasti kebakaran di lapangan. Diperlukan pengamatan darat sebelum tindakan mitigasi.",
                            fontSize = 11.sp,
                            color = if (isDayMode) Color(0xFFB45309) else Color(0xFFFFD54F)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusPill(
    text: String,
    color: Color,
    isDayMode: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        color = color.copy(alpha = if (isDayMode) 0.15f else 0.2f),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

/**
 * Feature 263, 267–269: LARGE HOTSPOT DISPLAY CARD WITH BEARING
 */
@Composable
private fun LargeHotspotDisplayCard(
    hotspot: Hotspot,
    userLocation: UserLocation?,
    cardBg: Color,
    borderColor: Color,
    textColor: Color,
    subtextColor: Color,
    isCurrentMode: Boolean
) {
    val distanceMeters = hotspot.distanceFromUser?.let { it * 1000.0 }
    val displayDist = GeodesicBearingCalculator.formatDisplayDistance(distanceMeters)

    // Feature 267 & 268: Geodesic Bearing
    val bearingDeg = if (userLocation != null) {
        GeodesicBearingCalculator.calculateBearing(
            userLocation.latitude,
            userLocation.longitude,
            hotspot.latitude,
            hotspot.longitude
        )
    } else null

    val cardinalDir = bearingDeg?.let { GeodesicBearingCalculator.bearingToCardinal(it) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, borderColor, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = borderColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (isCurrentMode) "SANGAT TERKINI" else "DATA TERSEDIA",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = borderColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Text(
                    text = "${hotspot.satellite} (${hotspot.instrument})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Feature 263: LARGE NUMBERS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                // Distance display
                Column {
                    Text(
                        text = "JARAK DARI POSISI SAYA",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = subtextColor
                    )
                    Text(
                        text = displayDist,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = borderColor
                    )
                }

                // Data age display
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "UMUR DATA",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = subtextColor
                    )
                    Text(
                        text = "${hotspot.satelliteAgeMinutes} menit",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        color = textColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Feature 267 & 268: BEARING & COMPASS DIRECTION
            if (cardinalDir != null && bearingDeg != null) {
                Surface(
                    color = Color(0xFF0F172A),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Explore,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ARAH BEARING:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Text(
                            text = "$cardinalDir (${bearingDeg.toInt()}°)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF38BDF8)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Observations details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Observasi: ${FireAgeCalculator.formatTimeOnly(hotspot.acquisitionTimestamp)}",
                    fontSize = 12.sp,
                    color = textColor,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Confidence: ${hotspot.confidence ?: "Nominal"}",
                    fontSize = 12.sp,
                    color = subtextColor
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Koordinat: ${String.format(java.util.Locale.US, "%.5f, %.5f", hotspot.latitude, hotspot.longitude)}",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = subtextColor
            )
        }
    }
}
