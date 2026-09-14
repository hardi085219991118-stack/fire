package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Hotspot
import com.example.data.model.HotspotAgeStatus
import com.example.data.model.UserLocation
import com.example.ui.MapLayerType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.InputStream
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tan

@Composable
fun InteractiveTileMap(
    userLocation: UserLocation?,
    hotspots: List<Hotspot>,
    layerType: MapLayerType,
    onLayerChanged: (MapLayerType) -> Unit,
    selectedHotspot: Hotspot?,
    onHotspotSelected: (Hotspot) -> Unit,
    onCenterOnUser: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Default coordinate: Hardi Mantangai (Central Kalimantan) ~ -2.44, 114.47 if user location null
    val defaultLat = userLocation?.latitude ?: -2.44
    val defaultLon = userLocation?.longitude ?: 114.47

    var centerLat by remember { mutableDoubleStateOf(defaultLat) }
    var centerLon by remember { mutableDoubleStateOf(defaultLon) }
    var zoomLevel by remember { mutableIntStateOf(10) }

    // Synchronize to user location when first detected
    LaunchedEffect(userLocation) {
        if (userLocation != null) {
            centerLat = userLocation.latitude
            centerLon = userLocation.longitude
        }
    }

    // Tile cache in memory
    val tileBitmaps = remember { mutableStateMapOf<String, Bitmap?>() }
    var mapTileLoadError by remember { mutableStateOf<String?>(null) }
    var showLayerMenu by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val okHttpClient = remember { OkHttpClient.Builder().build() }

    fun getTileUrl(z: Int, x: Int, y: Int, type: MapLayerType): String {
        val maxTile = (1 shl z)
        val validX = ((x % maxTile) + maxTile) % maxTile
        val validY = y.coerceIn(0, maxTile - 1)
        return when (type) {
            MapLayerType.SATELLITE ->
                "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/$z/$validY/$validX"
            MapLayerType.HYBRID ->
                "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/$z/$validY/$validX"
            MapLayerType.ROAD ->
                "https://tile.openstreetmap.org/$z/$validY/$validX.png"
            MapLayerType.TERRAIN ->
                "https://server.arcgisonline.com/ArcGIS/rest/services/World_Topo_Map/MapServer/tile/$z/$validY/$validX"
        }
    }

    fun loadTile(z: Int, x: Int, y: Int, type: MapLayerType) {
        val key = "${type.name}_${z}_${x}_$y"
        if (tileBitmaps.containsKey(key)) return

        scope.launch(Dispatchers.IO) {
            try {
                val url = getTileUrl(z, x, y, type)
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "HardiMantangaiFireNow/1.0 (OpenSatelliteMap)")
                    .build()
                val response = okHttpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val stream: InputStream? = response.body?.byteStream()
                    if (stream != null) {
                        val bitmap = BitmapFactory.decodeStream(stream)
                        if (bitmap != null) {
                            withContext(Dispatchers.Main) {
                                tileBitmaps[key] = bitmap
                                mapTileLoadError = null
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // If network failed
                if (tileBitmaps.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        mapTileLoadError = "Koneksi peta lambat atau offline (${e.localizedMessage ?: "Error"})"
                    }
                }
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0F172A))
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(16.dp))
    ) {
        val viewWidth = constraints.maxWidth.toFloat()
        val viewHeight = constraints.maxHeight.toFloat()
        val tileSize = 256f

        // Convert lat/lon to global pixel coordinates at current zoom level
        fun latLonToPixels(lat: Double, lon: Double, z: Int): Offset {
            val n = 2.0.pow(z.toDouble())
            val x = ((lon + 180.0) / 360.0 * n * tileSize).toFloat()
            val latRad = Math.toRadians(lat.coerceIn(-85.0511, 85.0511))
            val y = ((1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / PI) / 2.0 * n * tileSize).toFloat()
            return Offset(x, y)
        }

        fun pixelsToLatLon(px: Float, py: Float, z: Int): Pair<Double, Double> {
            val n = 2.0.pow(z.toDouble())
            val lon = px / (n * tileSize) * 360.0 - 180.0
            val yNorm = 1.0 - 2.0 * (py / (n * tileSize))
            val lat = Math.toDegrees(kotlin.math.atan(kotlin.math.sinh(PI * yNorm)))
            return Pair(lat, lon)
        }

        val centerPx = latLonToPixels(centerLat, centerLon, zoomLevel)

        // Preload visible tiles
        val minX = floor((centerPx.x - viewWidth / 2) / tileSize).toInt()
        val maxX = floor((centerPx.x + viewWidth / 2) / tileSize).toInt()
        val minY = floor((centerPx.y - viewHeight / 2) / tileSize).toInt()
        val maxY = floor((centerPx.y + viewHeight / 2) / tileSize).toInt()

        LaunchedEffect(zoomLevel, minX, maxX, minY, maxY, layerType) {
            for (x in minX..maxX) {
                for (y in minY..maxY) {
                    loadTile(zoomLevel, x, y, layerType)
                }
            }
        }

        // Tap and Gesture Handler
        val gestureModifier = Modifier
            .fillMaxSize()
            .pointerInput(zoomLevel, centerPx) {
                detectTransformGestures { _, pan, zoom, _ ->
                    if (zoom != 1f) {
                        val newZoom = if (zoom > 1.05f) zoomLevel + 1 else if (zoom < 0.95f) zoomLevel - 1 else zoomLevel
                        zoomLevel = newZoom.coerceIn(3, 16)
                    }
                    if (pan.x != 0f || pan.y != 0f) {
                        val newCenterPx = Offset(centerPx.x - pan.x, centerPx.y - pan.y)
                        val (newLat, newLon) = pixelsToLatLon(newCenterPx.x, newCenterPx.y, zoomLevel)
                        centerLat = newLat.coerceIn(-85.0, 85.0)
                        centerLon = ((newLon + 180.0) % 360.0 + 360.0) % 360.0 - 180.0
                    }
                }
            }
            .pointerInput(hotspots, centerPx, zoomLevel) {
                detectTapGestures { tapOffset ->
                    // Find if any hotspot marker was tapped
                    val tapThreshold = 40f
                    val tapped = hotspots.firstOrNull { hs ->
                        val hsPx = latLonToPixels(hs.latitude, hs.longitude, zoomLevel)
                        val screenX = hsPx.x - (centerPx.x - viewWidth / 2)
                        val screenY = hsPx.y - (centerPx.y - viewHeight / 2)
                        val dist = kotlin.math.hypot(tapOffset.x - screenX, tapOffset.y - screenY)
                        dist <= tapThreshold
                    }
                    if (tapped != null) {
                        onHotspotSelected(tapped)
                    }
                }
            }

        Canvas(modifier = gestureModifier) {
            val originX = centerPx.x - viewWidth / 2
            val originY = centerPx.y - viewHeight / 2

            // 1. Draw Tiles
            for (x in minX..maxX) {
                for (y in minY..maxY) {
                    val key = "${layerType.name}_${zoomLevel}_${x}_$y"
                    val bmp = tileBitmaps[key]
                    val screenTileX = x * tileSize - originX
                    val screenTileY = y * tileSize - originY

                    if (bmp != null && !bmp.isRecycled) {
                        val img = bmp.asImageBitmap()
                        drawImage(
                            image = img,
                            dstOffset = IntOffset(screenTileX.toInt(), screenTileY.toInt()),
                            dstSize = IntSize(tileSize.toInt(), tileSize.toInt())
                        )
                    } else {
                        // Tile grid placeholder
                        drawRect(
                            color = Color(0xFF1E293B),
                            topLeft = Offset(screenTileX, screenTileY),
                            size = Size(tileSize, tileSize)
                        )
                        drawRect(
                            color = Color(0xFF334155),
                            topLeft = Offset(screenTileX, screenTileY),
                            size = Size(tileSize, tileSize),
                            style = Stroke(width = 1f)
                        )
                    }
                }
            }

            // 2. Draw User Location if available
            if (userLocation != null) {
                val userPx = latLonToPixels(userLocation.latitude, userLocation.longitude, zoomLevel)
                val userScreenX = userPx.x - originX
                val userScreenY = userPx.y - originY

                // Accuracy circle
                drawCircle(
                    color = Color(0x3338BDF8),
                    radius = 28f,
                    center = Offset(userScreenX, userScreenY)
                )
                // Outer ring
                drawCircle(
                    color = Color(0xFF0284C7),
                    radius = 12f,
                    center = Offset(userScreenX, userScreenY),
                    style = Stroke(width = 3f)
                )
                // Center dot
                drawCircle(
                    color = Color(0xFF38BDF8),
                    radius = 8f,
                    center = Offset(userScreenX, userScreenY)
                )
            }

            // 3. Draw Hotspot Markers (Categorized by age status)
            // Draw older hotspots first, newest hotspots on top!
            val sortedHotspots = hotspots.sortedByDescending { it.satelliteAgeMinutes }
            for (hs in sortedHotspots) {
                val hsPx = latLonToPixels(hs.latitude, hs.longitude, zoomLevel)
                val hsScreenX = hsPx.x - originX
                val hsScreenY = hsPx.y - originY

                // Only draw if within screen bounds + padding
                if (hsScreenX in -30f..(viewWidth + 30f) && hsScreenY in -30f..(viewHeight + 30f)) {
                    val isSelected = selectedHotspot?.id == hs.id
                    val markerColor = hs.status.composeColor

                    // Glow circle for fresh hotspots
                    if (hs.status == HotspotAgeStatus.SANGAT_TERKINI || hs.status == HotspotAgeStatus.TERKINI) {
                        drawCircle(
                            color = markerColor.copy(alpha = 0.35f),
                            radius = if (isSelected) 28f else 18f,
                            center = Offset(hsScreenX, hsScreenY)
                        )
                    }

                    // Main marker pin circle
                    drawCircle(
                        color = markerColor,
                        radius = if (isSelected) 14f else 9f,
                        center = Offset(hsScreenX, hsScreenY)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = if (isSelected) 14f else 9f,
                        center = Offset(hsScreenX, hsScreenY),
                        style = Stroke(width = 2f)
                    )

                    // Draw flame icon or dot in center
                    if (hs.status == HotspotAgeStatus.SANGAT_TERKINI) {
                        drawCircle(
                            color = Color(0xFFFF5722),
                            radius = if (isSelected) 7f else 4.5f,
                            center = Offset(hsScreenX, hsScreenY)
                        )
                    }
                }
            }
        }

        // Tile Load Error notification if network is down
        if (mapTileLoadError != null && tileBitmaps.isEmpty()) {
            Surface(
                color = Color(0xCC000000),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "⚠️ PETA TIDAK DAPAT DIMUAT",
                        color = Color(0xFFFF5252),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = mapTileLoadError ?: "Periksa koneksi internet untuk memuat tile satelit.",
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            mapTileLoadError = null
                            for (x in minX..maxX) {
                                for (y in minY..maxY) {
                                    loadTile(zoomLevel, x, y, layerType)
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        modifier = Modifier.testTag("retry_map_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("COBA LAGI", fontSize = 12.sp)
                    }
                }
            }
        }

        // Map Control Buttons (Top-End)
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Layer Switcher
            Box {
                IconButton(
                    onClick = { showLayerMenu = !showLayerMenu },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color(0xCC1E293B),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .border(1.dp, Color(0xFF475569), CircleShape)
                        .testTag("map_layer_button")
                ) {
                    Icon(Icons.Default.Layers, contentDescription = "Pilih Layer Peta", modifier = Modifier.size(20.dp))
                }

                DropdownMenu(
                    expanded = showLayerMenu,
                    onDismissRequest = { showLayerMenu = false }
                ) {
                    MapLayerType.values().forEach { type ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "${type.label} ${if (type == layerType) "✓" else ""}",
                                    fontWeight = if (type == layerType) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                onLayerChanged(type)
                                showLayerMenu = false
                            }
                        )
                    }
                }
            }

            // My Location Button
            IconButton(
                onClick = {
                    if (userLocation != null) {
                        centerLat = userLocation.latitude
                        centerLon = userLocation.longitude
                        zoomLevel = 11
                    }
                    onCenterOnUser()
                },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color(0xCC1E293B),
                    contentColor = if (userLocation != null) Color(0xFF38BDF8) else Color.Gray
                ),
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .border(1.dp, Color(0xFF475569), CircleShape)
                    .testTag("my_location_button")
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "Lokasi Saya", modifier = Modifier.size(20.dp))
            }

            // Zoom In
            IconButton(
                onClick = { zoomLevel = (zoomLevel + 1).coerceAtMost(16) },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color(0xCC1E293B),
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .border(1.dp, Color(0xFF475569), CircleShape)
                    .testTag("zoom_in_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Perbesar", modifier = Modifier.size(20.dp))
            }

            // Zoom Out
            IconButton(
                onClick = { zoomLevel = (zoomLevel - 1).coerceAtLeast(3) },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color(0xCC1E293B),
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .border(1.dp, Color(0xFF475569), CircleShape)
                    .testTag("zoom_out_button")
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Perkecil", modifier = Modifier.size(20.dp))
            }
        }

        // Map Legend / Scale Indicator (Bottom-Start)
        Surface(
            color = Color(0xCC0F172A),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp)
                .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF00E676)))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("≤30m", color = Color.White, fontSize = 10.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF4CAF50)))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("≤1j", color = Color.White, fontSize = 10.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFFFD600)))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("≤3j", color = Color.White, fontSize = 10.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFFF9100)))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("≤6j", color = Color.White, fontSize = 10.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFFF1744)))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(">6j", color = Color.White, fontSize = 10.sp)
                }
            }
        }
    }
}
