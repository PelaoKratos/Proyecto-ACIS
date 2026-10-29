package com.example.proyecto_acis.data

import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import android.os.SystemClock
import java.util.Locale
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

suspend fun obtenerUbicacionCheckIn(context: Context): String? {
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val reciente = manager.getProviders(true).mapNotNull { proveedor ->
        runCatching { manager.getLastKnownLocation(proveedor) }.getOrNull()
    }.filter { SystemClock.elapsedRealtimeNanos() - it.elapsedRealtimeNanos in 0..120_000_000_000L }
        .minByOrNull { it.accuracy }
    if (reciente != null) return String.format(Locale.US, "%.5f, %.5f", reciente.latitude, reciente.longitude)
    val ubicacion = withTimeoutOrNull(12000) {
        suspendCancellableCoroutine<Location?> { espera ->
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    runCatching { manager.removeUpdates(this) }
                    if (espera.isActive) espera.resume(location)
                }
            }
            espera.invokeOnCancellation { runCatching { manager.removeUpdates(listener) } }
            try {
                val proveedores = manager.getProviders(true).filter { it != LocationManager.PASSIVE_PROVIDER }
                val solicitudes = proveedores.count { proveedor ->
                    runCatching { manager.requestSingleUpdate(proveedor, listener, Looper.getMainLooper()) }.isSuccess
                }
                if (solicitudes == 0 && espera.isActive) espera.resume(null)
            } catch (_: Exception) {
                if (espera.isActive) espera.resume(null)
            }
        }
    }
    return ubicacion?.let { String.format(Locale.US, "%.5f, %.5f", it.latitude, it.longitude) }
}
