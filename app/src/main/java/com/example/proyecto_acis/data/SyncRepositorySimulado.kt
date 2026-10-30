package com.example.proyecto_acis.data

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SyncRepositorySimulado(private val database: OrdenesDb,
    private val pausa: suspend () -> Unit = { delay(250) }) : SyncRepository {
    private val _registros = MutableStateFlow(database.leerSync())
    override val registros = _registros.asStateFlow()
    private val mutex = Mutex()
    fun recargar() { _registros.value = database.leerSync() }
    override suspend fun simular(ids: Set<String>, fallar: Boolean): ResultadoEnvio = mutex.withLock {
        recargar()
        val pendientes = registros.value.filter { it.otId in ids && it.estado != EstadoSync.ENVIADO }
        var enviados = 0
        var errores = 0
        var modificados = 0
        for (registro in pendientes) {
            pausa()
            if (database.resultadoSync(registro.otId, registro.revision, fallar, System.currentTimeMillis())) {
                if (fallar) errores++ else enviados++
            } else modificados++
            recargar()
        }
        ResultadoEnvio(enviados, errores, modificados)
    }
}
