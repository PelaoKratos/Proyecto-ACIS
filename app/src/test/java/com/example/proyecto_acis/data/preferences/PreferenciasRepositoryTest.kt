package com.example.proyecto_acis.data.preferences

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.PreferencesSerializer
import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.proyecto_acis.domain.model.Perfil
import com.example.proyecto_acis.domain.model.PreferenciasUsuario
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import okio.Path.Companion.toPath
import okio.FileSystem

class PreferenciasRepositoryTest {
    @get:Rule val carpeta = TemporaryFolder()

    @Test fun inicialmenteNoRecuerdaNingunPerfil() = runBlocking {
        conRepositorio { assertEquals(PreferenciasUsuario(), it.leer()) }
    }

    @Test fun cambiarPerfilNoActivaElRecuerdoSinPermiso() = runBlocking {
        conRepositorio {
            it.actualizarAcceso(Perfil.TECNICO, "Brian Bravo")
            assertEquals(PreferenciasUsuario(), it.leer())
        }
    }

    @Test fun conservaElTecnicoAlReabrirElArchivo() = runBlocking {
        conRepositorio { it.recordar(true, Perfil.TECNICO, "Brian Bravo") }
        conRepositorio {
            assertEquals(PreferenciasUsuario(true, Perfil.TECNICO, "Brian Bravo"), it.leer())
        }
    }

    @Test fun guardaElUltimoPerfilYPermiteOlvidarlo() = runBlocking {
        conRepositorio {
            it.recordar(true, Perfil.TECNICO, "Brian Bravo")
            it.actualizarAcceso(Perfil.CLIENTE, "Brian Bravo")
            assertEquals(Perfil.CLIENTE, it.leer().perfil)
            it.recordar(false, Perfil.CLIENTE, "Brian Bravo")
        }
        conRepositorio { assertEquals(PreferenciasUsuario(), it.leer()) }
    }

    @Test fun unPerfilDesconocidoPermiteVolverALaSeleccion() = runBlocking {
        val trabajo = SupervisorJob()
        try {
            val almacen = PreferenceDataStoreFactory.create(
                scope = CoroutineScope(Dispatchers.IO + trabajo),
                storage = almacenamiento()
            )
            almacen.edit {
                it[booleanPreferencesKey("recordar_perfil")] = true
                it[stringPreferencesKey("ultimo_perfil")] = "PERFIL_FUTURO"
            }
            assertEquals(PreferenciasUsuario(), PreferenciasRepository(almacen).leer())
        } finally { trabajo.cancelAndJoin() }
    }

    @Test fun unTecnicoVacioNoGuardaPreferenciasParciales() = runBlocking {
        conRepositorio {
            try {
                it.recordar(true, Perfil.TECNICO, "")
                fail("Debe pedir un técnico")
            } catch (_: IllegalArgumentException) { }
            assertEquals(PreferenciasUsuario(), it.leer())
        }
    }

    private suspend fun conRepositorio(accion: suspend (PreferenciasRepository) -> Unit) {
        val trabajo = SupervisorJob()
        try {
            val almacen = PreferenceDataStoreFactory.create(
                scope = CoroutineScope(Dispatchers.IO + trabajo),
                storage = almacenamiento()
            )
            accion(PreferenciasRepository(almacen))
        } finally { trabajo.cancelAndJoin() }
    }

    // Okio permite reemplazar el archivo de prueba también en Windows.
    private fun almacenamiento() = OkioStorage(FileSystem.SYSTEM, PreferencesSerializer) {
        carpeta.root.resolve("ajustes.preferences_pb").absolutePath.toPath()
    }
}
