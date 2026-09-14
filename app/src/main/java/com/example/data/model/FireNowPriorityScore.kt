package com.example.data.model

import java.util.Locale

/**
 * Feature 112–118: FIRE NOW PRIORITY SCORE & EXPLANATION
 *
 * PENTING:
 * - Score digunakan HANYA untuk mengurutkan hotspot pada PRIORITAS PEMANTAUAN.
 * - JANGAN PERNAH menyebut score sebagai probabilitas kebakaran.
 * - Score mempertimbangkan:
 *   1. Umur data satelit (makin baru = prioritas makin tinggi)
 *   2. Jarak geografis dari pengguna (makin dekat = prioritas makin tinggi)
 *   3. Confidence asli satelit (High > Nominal > Low)
 *   4. Brightness asli (anomali termal, bukan vonis kebakaran pasti)
 * - Tampilkan HANYA: PRIORITAS PEMANTAUAN (SANGAT TINGGI / TINGGI / SEDANG / RENDAH)
 */
data class FireNowPriorityResult(
    val score: Double,
    val priorityLevel: PriorityLevel,
    val explanationLines: List<String>
) {
    val explanationText: String
        get() = explanationLines.joinToString("\n• ", prefix = "Prioritas ${priorityLevel.label.lowercase(Locale.ROOT)} karena:\n• ")
}

enum class PriorityLevel(val label: String, val badgeColorHex: Long) {
    SANGAT_TINGGI("SANGAT TINGGI", 0xFFFF1744),
    TINGGI("TINGGI", 0xFFFF9100),
    SEDANG("SEDANG", 0xFFFFD600),
    RENDAH("RENDAH", 0xFF00E676),
    HISTORIS("HISTORIS", 0xFF9E9E9E)
}

object FireNowPriorityScore {

    fun calculate(
        ageMinutes: Long,
        distanceKm: Double?,
        confidence: String?,
        brightness: Double?
    ): FireNowPriorityResult {
        var score = 0.0
        val explanations = mutableListOf<String>()

        // 1. Umur Data (Max 50 poin)
        when {
            ageMinutes <= 15 -> {
                score += 50.0
                explanations.add("Data observasi satelit sangat baru ($ageMinutes menit)")
            }
            ageMinutes <= 30 -> {
                score += 42.0
                explanations.add("Data observasi masih dalam jendela Fire Now ($ageMinutes menit)")
            }
            ageMinutes <= 60 -> {
                score += 30.0
                explanations.add("Data observasi berumur $ageMinutes menit")
            }
            ageMinutes <= 180 -> {
                score += 15.0
                explanations.add("Data observasi terlambat (${ageMinutes / 60} jam ${ageMinutes % 60} menit)")
            }
            ageMinutes <= 360 -> {
                score += 5.0
                explanations.add("Data observasi lama (${ageMinutes / 60} jam)")
            }
            else -> {
                score += 0.0
                explanations.add("Data observasi historis (>6 jam)")
            }
        }

        // 2. Jarak dari Pengguna (Max 30 poin)
        if (distanceKm != null) {
            when {
                distanceKm <= 2.0 -> {
                    score += 30.0
                    explanations.add(String.format(Locale.US, "Jarak sangat dekat (%.1f km)", distanceKm))
                }
                distanceKm <= 5.0 -> {
                    score += 24.0
                    explanations.add(String.format(Locale.US, "Jarak dekat (%.1f km)", distanceKm))
                }
                distanceKm <= 15.0 -> {
                    score += 18.0
                    explanations.add(String.format(Locale.US, "Jarak menengah (%.1f km)", distanceKm))
                }
                distanceKm <= 25.0 -> {
                    score += 10.0
                    explanations.add(String.format(Locale.US, "Dalam radius pemantauan lokal (%.1f km)", distanceKm))
                }
                else -> {
                    score += 2.0
                    explanations.add(String.format(Locale.US, "Di luar radius lokal (%.1f km)", distanceKm))
                }
            }
        } else {
            explanations.add("Lokasi GPS pengguna belum tersedia")
        }

        // 3. Tingkat Kepercayaan Satelit (Max 12 poin)
        val confLower = confidence?.lowercase(Locale.ROOT) ?: "nominal"
        when {
            confLower.contains("high") || confLower.contains("h") || (confLower.toIntOrNull() ?: 0) >= 80 -> {
                score += 12.0
                explanations.add("Confidence satelit: High (Tinggi)")
            }
            confLower.contains("nominal") || confLower.contains("n") || (confLower.toIntOrNull() ?: 0) in 50..79 -> {
                score += 7.0
                explanations.add("Confidence satelit: Nominal (Sedang)")
            }
            else -> {
                score += 2.0
                explanations.add("Confidence satelit: Low (Rendah)")
            }
        }

        // 4. Intensitas Anomali Termal / Brightness (Max 8 poin)
        if (brightness != null && brightness > 350.0) {
            score += 8.0
            explanations.add(String.format(Locale.US, "Anomali termal terdeteksi kuat (%.1f K)", brightness))
        } else if (brightness != null) {
            score += 4.0
            explanations.add(String.format(Locale.US, "Suhu kecerahan terdeteksi %.1f K", brightness))
        }

        val level = when {
            ageMinutes > 360 -> PriorityLevel.HISTORIS
            score >= 75.0 -> PriorityLevel.SANGAT_TINGGI
            score >= 50.0 -> PriorityLevel.TINGGI
            score >= 25.0 -> PriorityLevel.SEDANG
            else -> PriorityLevel.RENDAH
        }

        return FireNowPriorityResult(
            score = score,
            priorityLevel = level,
            explanationLines = explanations
        )
    }
}
