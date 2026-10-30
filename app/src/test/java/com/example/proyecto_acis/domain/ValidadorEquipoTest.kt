package com.example.proyecto_acis.domain

import com.example.proyecto_acis.data.Catastro
import com.example.proyecto_acis.data.Cliente
import com.example.proyecto_acis.data.Equipo
import com.example.proyecto_acis.data.Local
import org.junit.Assert.*
import org.junit.Test

class ValidadorEquipoTest {
    private val catastro = Catastro(
        clientes = listOf(Cliente("c1", "Cliente")),
        locales = listOf(Local("l1", "c1", "Local centro"), Local("l2", "c1", "Local norte")),
        equipos = listOf(Equipo("e1", "l1", "Refrigerador", "Refrigeración")),
    )

    @Test fun exigeLocalNombreYTipo() {
        val errores = ValidadorEquipo.validar(DatosEquipo(), catastro)
        assertNotNull(errores.local)
        assertNotNull(errores.nombre)
        assertNotNull(errores.tipo)
    }

    @Test fun impideDuplicadosEnElLocalPeroPermiteElMismoNombreEnOtro() {
        val datos = DatosEquipo(localId = "l1", nombre = " refrigerador ", tipo = "Refrigeración")
        assertNotNull(ValidadorEquipo.validar(datos, catastro).nombre)
        assertFalse(ValidadorEquipo.validar(datos.copy(localId = "l2"), catastro).hayErrores)
        assertFalse(ValidadorEquipo.validar(datos.copy(id = "e1"), catastro).hayErrores)
    }

    @Test fun equipoConOrdenesConservaSuLocalAlEditar() {
        val datos = DatosEquipo(id = "e1", localId = "l2", nombre = "Refrigerador", tipo = "Refrigeración")
        assertNotNull(ValidadorEquipo.validar(datos, catastro, setOf("e1")).local)
        assertFalse(ValidadorEquipo.validar(datos, catastro).hayErrores)
    }
}
