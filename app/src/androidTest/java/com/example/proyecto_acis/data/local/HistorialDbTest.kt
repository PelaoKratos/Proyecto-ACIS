package com.example.proyecto_acis.data.local

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HistorialDbTest : HistorialDbCasos() {
    override val context: Context get() = InstrumentationRegistry.getInstrumentation().targetContext
}
