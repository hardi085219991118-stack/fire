package com.example.data.model

import androidx.compose.ui.graphics.Color

/**
 * Status umur data satelit sesuai spesifikasi wajib:
 * 0–30 menit: SANGAT TERKINI (🟢)
 * 31–60 menit: TERKINI (🟢)
 * 61–180 menit: TERBARU TETAPI TERLAMBAT (🟡)
 * 181–360 menit: TERLAMBAT (🟠)
 * >360 menit: HISTORIS (🔴)
 * Tidak diketahui: WAKTU TIDAK DIKETAHUI (⚪)
 */
enum class HotspotAgeStatus(
    val label: String,
    val badgeText: String,
    val iconEmoji: String,
    val colorHex: Long,
    val isEligibleForFireNow: Boolean
) {
    SANGAT_TERKINI(
        label = "SANGAT TERKINI",
        badgeText = "0-30 Menit",
        iconEmoji = "🔥",
        colorHex = 0xFF00E676, // Bright Green
        isEligibleForFireNow = true
    ),
    TERKINI(
        label = "TERKINI",
        badgeText = "31-60 Menit",
        iconEmoji = "🟢",
        colorHex = 0xFF4CAF50, // Green
        isEligibleForFireNow = true
    ),
    TERBARU_TETAPI_TERLAMBAT(
        label = "TERBARU TETAPI TERLAMBAT",
        badgeText = "1-3 Jam",
        iconEmoji = "🟡",
        colorHex = 0xFFFFD600, // Amber Yellow
        isEligibleForFireNow = false
    ),
    TERLAMBAT(
        label = "TERLAMBAT",
        badgeText = "3-6 Jam",
        iconEmoji = "🟠",
        colorHex = 0xFFFF9100, // Orange
        isEligibleForFireNow = false
    ),
    HISTORIS(
        label = "HISTORIS",
        badgeText = ">6 Jam",
        iconEmoji = "🔴",
        colorHex = 0xFFFF1744, // Red
        isEligibleForFireNow = false
    ),
    UNKNOWN(
        label = "WAKTU TIDAK DIKETAHUI",
        badgeText = "?",
        iconEmoji = "⚪",
        colorHex = 0xFF9E9E9E, // Grey
        isEligibleForFireNow = false
    );

    val composeColor: Color
        get() = Color(colorHex)
}
