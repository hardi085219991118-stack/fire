package com.example.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import com.example.data.model.UserLocation
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

class LocationHelper(private val context: Context) {

    private val fusedClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): UserLocation? = withContext(Dispatchers.IO) {
        try {
            // First try high accuracy current location from FusedLocationProvider
            val cts = CancellationTokenSource()
            val location: Location? = suspendCancellableCoroutine { continuation ->
                fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                    .addOnSuccessListener { loc ->
                        if (continuation.isActive) continuation.resume(loc)
                    }
                    .addOnFailureListener {
                        if (continuation.isActive) continuation.resume(null)
                    }
                    .addOnCanceledListener {
                        if (continuation.isActive) continuation.resume(null)
                    }
            }

            if (location != null) {
                return@withContext UserLocation(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    accuracyMeters = location.accuracy,
                    timestamp = location.time,
                    provider = location.provider ?: "FusedLocation"
                )
            }

            // Fallback to last known location
            val lastLoc: Location? = suspendCancellableCoroutine { continuation ->
                fusedClient.lastLocation
                    .addOnSuccessListener { loc ->
                        if (continuation.isActive) continuation.resume(loc)
                    }
                    .addOnFailureListener {
                        if (continuation.isActive) continuation.resume(null)
                    }
            }

            if (lastLoc != null) {
                return@withContext UserLocation(
                    latitude = lastLoc.latitude,
                    longitude = lastLoc.longitude,
                    accuracyMeters = lastLoc.accuracy,
                    timestamp = lastLoc.time,
                    provider = "LastKnown_${lastLoc.provider ?: "GPS"}"
                )
            }

            // Fallback to system LocationManager if Google Play Services didn't return
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            if (lm != null) {
                val gpsLoc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                val netLoc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                val best = gpsLoc ?: netLoc
                if (best != null) {
                    return@withContext UserLocation(
                        latitude = best.latitude,
                        longitude = best.longitude,
                        accuracyMeters = best.accuracy,
                        timestamp = best.time,
                        provider = best.provider ?: "SystemLM"
                    )
                }
            }

            null
        } catch (_: Exception) {
            null
        }
    }
}
