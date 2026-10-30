package com.example.proyecto_acis.domain

import com.example.proyecto_acis.data.*
import org.junit.Assert.*
import org.junit.Test

class ValidadorCatastroTest {
    private val catastro = Catastro(
        clientes = listOf(Cliente("c1", "Comercial Sur"), Cliente(ValidadorCatastro.CLIENTE_PENDIENTE, "Cliente por identificar")),
        locales = listOf(Local("l1", "c1", "Sucursal centro")),
    )

    @Test fun nombreUnicoPermiteEditarElMismoRegistro() {
        assertNotNull(ValidadorCatastro.cliente(DatosCliente(nombre = " comercial sur "), catastro))
        assertNull(ValidadorCatastro.cliente(DatosCliente("c1", "Comercial Sur"), catastro))
        assertNotNull(ValidadorCatastro.cliente(DatosCliente(nombre = "  "), catastro))
        assertNotNull(ValidadorCatastro.cliente(DatosCliente(nombre = "Cliente por identificar"), Catastro()))
        assertNotNull(ValidadorCatastro.cliente(DatosCliente(ValidadorCatastro.CLIENTE_PENDIENTE, "Nuevo"), catastro))
    }

    @Test fun localRequiereClienteRealYNombreDistinto() {
        assertTrue(ValidadorCatastro.local(DatosLocal(), catastro).hayErrores)
        assertNotNull(ValidadorCatastro.local(DatosLocal(clienteId = "c1", nombre = "sucursal CENTRO"), catastro).nombre)
        assertFalse(ValidadorCatastro.local(DatosLocal("l1", "c1", "Sucursal centro"), catastro).hayErrores)
        assertNotNull(ValidadorCatastro.local(DatosLocal(clienteId = ValidadorCatastro.CLIENTE_PENDIENTE, nombre = "Nuevo"), catastro).cliente)
        assertFalse(ValidadorCatastro.local(DatosLocal(clienteId = "c1", nombre = "Sucursal norte"), catastro).hayErrores)
    }

    @Test fun ubicacionOpcionalExigeParValidoSiSeInforma() {
        assertTrue(ValidadorCatastro.coordenadasValidas(null, null))
        assertTrue(ValidadorCatastro.coordenadasValidas(-36.82, -73.05))
        assertFalse(ValidadorCatastro.coordenadasValidas(null, -73.0))
        assertFalse(ValidadorCatastro.coordenadasValidas(Double.NaN, 0.0))
        assertFalse(ValidadorCatastro.coordenadasValidas(0.0, Double.POSITIVE_INFINITY))
        assertFalse(ValidadorCatastro.coordenadasValidas(-91.0, -73.0))
        assertFalse(ValidadorCatastro.coordenadasValidas(0.0, 181.0))
        assertEquals(-36.82 to -73.05, ValidadorCatastro.leerUbicacion("-36.82, -73.05"))
        assertNull(ValidadorCatastro.leerUbicacion("sin ubicación"))
        assertNull(ValidadorCatastro.leerUbicacion("1,2,3"))
        assertNull(ValidadorCatastro.leerUbicacion("91,0"))
    }
    @Test fun plazosOpcionalesExigenParCompletoYHorasOrdenadas() {
        val datos = DatosCliente(nombre = "Cliente nuevo")
        assertFalse(ValidadorCatastro.erroresCliente(datos, catastro).hayErrores)
        assertFalse(ValidadorCatastro.erroresCliente(datos.copy(horasLlegada = "4", horasResolucion = "24"), catastro).hayErrores)
        assertNotNull(ValidadorCatastro.erroresCliente(datos.copy(horasLlegada = "4"), catastro).resolucion)
        assertNotNull(ValidadorCatastro.erroresCliente(datos.copy(horasLlegada = "0", horasResolucion = "24"), catastro).llegada)
        assertNotNull(ValidadorCatastro.erroresCliente(datos.copy(horasLlegada = "8", horasResolucion = "4"), catastro).resolucion)
        assertNotNull(ValidadorCatastro.erroresCliente(datos.copy(horasLlegada = "1.5", horasResolucion = "24"), catastro).llegada)
        assertNotNull(ValidadorCatastro.erroresCliente(datos.copy(horasLlegada = "4", horasResolucion = "721"), catastro).resolucion)
    }

}
