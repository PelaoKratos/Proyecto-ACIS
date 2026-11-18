package com.example.proyecto_acis.data.local

import android.app.Application
import android.content.Context
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class RoomHistorialTest : HistorialDbCasos() {
    override val context: Context get() = RuntimeEnvironment.getApplication()
}
