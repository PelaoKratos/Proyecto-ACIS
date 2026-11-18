package com.example.proyecto_acis.device.location

import android.Manifest
import android.app.Application
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.SystemClock
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class UbicacionCheckInTest {
    @Test fun sinPermisoContinuaSinUbicacion() = runBlocking {
        val app = RuntimeEnvironment.getApplication()
        shadowOf(app).denyPermissions(Manifest.permission.ACCESS_COARSE_LOCATION)
        assertNull(obtenerUbicacionCheckIn(app))
    }

    @Test fun conPermisoConservaLaUbicacionReciente() = runBlocking {
        val app = RuntimeEnvironment.getApplication()
        shadowOf(app).grantPermissions(Manifest.permission.ACCESS_COARSE_LOCATION)
        val manager = app.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        shadowOf(manager).setProviderEnabled(LocationManager.NETWORK_PROVIDER, true)
        shadowOf(manager).setLastKnownLocation(LocationManager.NETWORK_PROVIDER,
            Location(LocationManager.NETWORK_PROVIDER).apply {
                latitude = -36.82
                longitude = -73.05
                accuracy = 50f
                elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
            })
        assertEquals("-36.82000, -73.05000", obtenerUbicacionCheckIn(app))
    }
}
