package com.example.proyecto_acis.domain

import com.example.proyecto_acis.data.Catastro

data class DatosCliente(val id: String? = null, val nombre: String = "",
    val horasLlegada: String = "", val horasResolucion: String = "")
data class ErroresCliente(val nombre: String? = null, val llegada: String? = null, val resolucion: String? = null) {
    val hayErrores get() = nombre != null || llegada != null || resolucion != null
}
data class DatosLocal(
    val id: String? = null,
    val clienteId: String = "",
    val nombre: String = "",
    val direccion: String = "",
    val latitud: Double? = null,
    val longitud: Double? = null,
)
data class ErroresLocal(val cliente: String? = null, val nombre: String? = null, val ubicacion: String? = null) {
    val hayErrores: Boolean get() = cliente != null || nombre != null || ubicacion != null
}

object ValidadorCatastro {
    const val CLIENTE_PENDIENTE = "cliente-por-identificar"

    private fun nombreCliente(datos: DatosCliente, catastro: Catastro): String? = when {
        datos.id == CLIENTE_PENDIENTE -> "Identifica el cliente editando cada local asociado."
        datos.nombre.trim().equals("Cliente por identificar", ignoreCase = true) -> "Usa el nombre real del cliente."
        datos.nombre.isBlank() -> "Ingresa el nombre del cliente."
        catastro.clientes.any { it.id != datos.id && it.nombre.equals(datos.nombre.trim(), ignoreCase = true) } ->
            "Ya existe un cliente con ese nombre."
        else -> null
    }

    fun erroresCliente(datos: DatosCliente, catastro: Catastro): ErroresCliente {
        val llegada = datos.horasLlegada.trim()
        val resolucion = datos.horasResolucion.trim()
        val sinAcuerdo = llegada.isBlank() && resolucion.isBlank()
        fun errorHoras(valor: String): String? = when {
            sinAcuerdo -> null
            valor.toIntOrNull() !in 1..720 -> "Ingresa horas enteras entre 1 y 720, o deja ambos plazos vacíos."
            else -> null
        }
        return ErroresCliente(nombreCliente(datos, catastro), errorHoras(llegada),
            errorHoras(resolucion) ?: if (!sinAcuerdo && llegada.toIntOrNull() != null &&
                resolucion.toIntOrNull() != null && resolucion.toInt() < llegada.toInt())
                "El plazo de resolución debe ser igual o mayor al de llegada." else null)
    }
    fun cliente(datos: DatosCliente, catastro: Catastro): String? = erroresCliente(datos, catastro).let {
        it.nombre ?: it.llegada ?: it.resolucion
    }

    fun local(datos: DatosLocal, catastro: Catastro): ErroresLocal = ErroresLocal(
        cliente = if (datos.clienteId == CLIENTE_PENDIENTE || catastro.clientes.none { it.id == datos.clienteId }) "Selecciona un cliente del catálogo." else null,
        nombre = when {
            datos.nombre.isBlank() -> "Ingresa el nombre del local."
            catastro.locales.any { it.id != datos.id && it.nombre.equals(datos.nombre.trim(), ignoreCase = true) } ->
                "Ya existe un local con ese nombre. Usa un nombre que lo distinga."
            else -> null
        },
        ubicacion = if (coordenadasValidas(datos.latitud, datos.longitud)) null else "Vuelve a registrar la ubicación del local.",
    )

    fun coordenadasValidas(latitud: Double?, longitud: Double?): Boolean =
        (latitud == null && longitud == null) ||
            (latitud != null && longitud != null && latitud.isFinite() && longitud.isFinite() &&
                latitud in -90.0..90.0 && longitud in -180.0..180.0)

    fun leerUbicacion(texto: String): Pair<Double, Double>? {
        val partes = texto.split(',')
        if (partes.size != 2) return null
        val latitud = partes[0].trim().toDoubleOrNull() ?: return null
        val longitud = partes[1].trim().toDoubleOrNull() ?: return null
        return if (coordenadasValidas(latitud, longitud)) latitud to longitud else null
    }
}
