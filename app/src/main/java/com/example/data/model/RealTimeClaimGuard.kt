package com.example.data.model

/**
 * Feature 171 & 172: REAL-TIME CLAIM GUARD
 * Sistem ini secara ketat mencegah UI menggunakan kata "LIVE", "NOW", "SEKARANG", "REAL-TIME"
 * jika umur data melebihi configured threshold (default: <=30 menit).
 *
 * Jika threshold terlampaui, otomatis diubah menjadi status jujur:
 * "TERLAMBAT", "DATA LAMA", atau "HISTORIS".
 */
object RealTimeClaimGuard {

    private val FORBIDDEN_WORDS_FOR_OLD_DATA = listOf(
        "live", "now", "sekarang", "real-time", "api aktif sekarang", "kebakaran saat ini"
    )

    /**
     * Memeriksa apakah klaim real-time diizinkan untuk umur data tertentu.
     */
    fun isRealTimeClaimAllowed(dataAgeMinutes: Long?, thresholdMinutes: Long = 30L): Boolean {
        if (dataAgeMinutes == null || dataAgeMinutes < 0) return false
        return dataAgeMinutes <= thresholdMinutes
    }

    /**
     * Memfilter string klaim UI: jika umur data melebihi threshold, ganti istilah tidak jujur
     * menjadi status transparan.
     */
    fun sanitizeClaim(
        requestedClaim: String,
        dataAgeMinutes: Long?,
        thresholdMinutes: Long = 30L
    ): String {
        if (isRealTimeClaimAllowed(dataAgeMinutes, thresholdMinutes)) {
            return requestedClaim
        }

        var sanitized = requestedClaim
        val age = dataAgeMinutes ?: Long.MAX_VALUE

        val replacement = when {
            age <= 60 -> "TERKINI (BUKAN LIVE)"
            age <= 180 -> "DATA TERLAMBAT"
            age <= 360 -> "DATA LAMA"
            else -> "DATA HISTORIS"
        }

        for (word in FORBIDDEN_WORDS_FOR_OLD_DATA) {
            val regex = Regex("(?i)\\b$word\\b")
            if (regex.containsMatchIn(sanitized)) {
                sanitized = regex.replace(sanitized, replacement)
            }
        }

        return sanitized
    }

    /**
     * Mengembalikan status badge jujur berdasarkan umur data.
     */
    fun getHonestBadgeTitle(dataAgeMinutes: Long?): String {
        if (dataAgeMinutes == null) return "⚫ DATA TIDAK TERSEDIA"
        return when {
            dataAgeMinutes <= 30 -> "🟢 DATA SANGAT TERKINI"
            dataAgeMinutes <= 60 -> "🟢 DATA TERKINI"
            dataAgeMinutes <= 180 -> "🟡 DATA TERLAMBAT"
            dataAgeMinutes <= 360 -> "🟠 DATA LAMA"
            else -> "🔴 DATA HISTORIS"
        }
    }
}
