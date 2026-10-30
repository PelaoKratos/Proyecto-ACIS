package com.example.proyecto_acis.data

import kotlinx.coroutines.flow.StateFlow

enum class EstadoSync(val etiqueta: String) {
    LOCAL("Guardada en este dispositivo"), PENDIENTE("Pendiente de envío simulado"),
    ENVIADO("Envío simulado completado"), ERROR("Error de envío simulado")
}
data class RegistroSync(val otId: String, val revision: Long, val estado: EstadoSync, val intentoMillis: Long? = null)
data class ResultadoEnvio(val enviados: Int, val errores: Int, val modificados: Int)
interface SyncRepository {
    val registros: StateFlow<List<RegistroSync>>
    suspend fun simular(ids: Set<String>, fallar: Boolean): ResultadoEnvio
}
