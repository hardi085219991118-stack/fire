package com.example.data.engine

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class StructuredErrorLog(
    val timestamp: Long = System.currentTimeMillis(),
    val component: String,
    val errorType: String,
    val httpStatus: Int? = null,
    val message: String,
    val retryCount: Int = 0
)

/**
 * Feature 289, 290, 335–338: SYSTEM OBSERVABILITY, ERROR LOGGING & SECURITY SANITIZATION
 */
object ErrorObservability {

    private val errorLogs = mutableListOf<StructuredErrorLog>()
    private val sourceIncidents = mutableListOf<SourceIncident>()
    private var consecutiveFailures = 0

    private val secretRegex = Regex("(?i)(key|token|auth|bearer|secret|password)=[a-zA-Z0-9_-]{8,}")
    private val hexKeyRegex = Regex("[a-fA-F0-9]{32}")

    /**
     * Feature 336: LOG SANITIZATION
     * Menghapus API key, Token, dan rahasia sebelum dicatat ke log atau tampilan.
     */
    fun sanitizeMessage(raw: String): String {
        var clean = secretRegex.replace(raw) { mr ->
            val param = mr.groupValues[1]
            "$param=[REDACTED]"
        }
        clean = hexKeyRegex.replace(clean, "[REDACTED_KEY]")
        return clean
    }

    /**
     * Feature 338: STRUCTURED ERROR LOGGING
     */
    fun recordError(
        component: String,
        errorType: String,
        httpStatus: Int?,
        rawMessage: String,
        retryCount: Int = 0
    ) {
        val sanitized = sanitizeMessage(rawMessage)
        val log = StructuredErrorLog(
            timestamp = System.currentTimeMillis(),
            component = component,
            errorType = errorType,
            httpStatus = httpStatus,
            message = sanitized,
            retryCount = retryCount
        )

        synchronized(errorLogs) {
            errorLogs.add(0, log)
            if (errorLogs.size > 100) errorLogs.removeAt(errorLogs.lastIndex)
        }

        // Feature 290: SOURCE INCIDENT TRACKING
        if (httpStatus != null && httpStatus >= 400) {
            consecutiveFailures++
            if (consecutiveFailures >= 2) {
                val incident = SourceIncident(
                    timestamp = System.currentTimeMillis(),
                    sourceName = component,
                    httpStatus = httpStatus,
                    errorMessage = sanitized,
                    consecutiveCount = consecutiveFailures
                )
                synchronized(sourceIncidents) {
                    sourceIncidents.add(0, incident)
                    if (sourceIncidents.size > 20) sourceIncidents.removeAt(sourceIncidents.lastIndex)
                }
            }
        }
    }

    fun recordSuccess() {
        consecutiveFailures = 0
    }

    fun getErrorLogs(): List<StructuredErrorLog> = synchronized(errorLogs) { errorLogs.toList() }

    fun getSourceIncidents(): List<SourceIncident> = synchronized(sourceIncidents) { sourceIncidents.toList() }

    fun clearLogs() {
        synchronized(errorLogs) { errorLogs.clear() }
        synchronized(sourceIncidents) { sourceIncidents.clear() }
    }
}
