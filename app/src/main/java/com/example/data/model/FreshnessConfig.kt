package com.example.data.model

/**
 * Feature 173: DATA FRESHNESS SETTINGS
 * Configurable thresholds for observation age classification.
 */
data class FreshnessConfig(
    val veryFreshMinutes: Long = 30L,
    val freshMinutes: Long = 60L,
    val delayedMinutes: Long = 180L,
    val oldMinutes: Long = 360L
) {
    companion object {
        val DEFAULT = FreshnessConfig()
    }
}
