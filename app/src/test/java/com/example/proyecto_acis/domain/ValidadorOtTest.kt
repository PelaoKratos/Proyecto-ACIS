package com.example.proyecto_acis.domain

import org.junit.Assert.*
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale

class ValidadorOtTest {
    @Test fun asignacionRequiereTecnicoDisponibleYHoraValida() {
        val tecnicos = listOf("Técnico demo", "Brian Bravo")
        assertNotNull(ValidadorOt.asignacion("", tecnicos, "09:00").tecnico)
        assertNotNull(ValidadorOt.asignacion("Otro", tecnicos, "09:00").tecnico)
        assertNotNull(ValidadorOt.asignacion("Brian Bravo", tecnicos, "25:00").hora)
        assertFalse(ValidadorOt.asignacion("Brian Bravo", tecnicos, "15:00").hayErrores)
    }

    @Test fun rechazaFechasInexistentesYHorasFueraDeRango() {
        val errores = ValidadorOt.crear(DatosCrearOt(fecha = "31/02/2026", hora = "25:10"))
        assertNotNull(errores.fecha)
        assertNotNull(errores.hora)
        val validas = ValidadorOt.crear(DatosCrearOt(fecha = "28/02/2026", hora = "23:10"))
        assertNull(validas.fecha)
        assertNull(validas.hora)
    }

    @Test fun postergarRequiereUnMomentoFuturo() {
        val ahora = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ROOT).parse("29/10/2026 12:00")!!.time
        assertNotNull(ValidadorOt.postergacion("29/10/2026 11:59", ahora))
        assertNotNull(ValidadorOt.postergacion("29/10/2026 12:00", ahora))
        assertNull(ValidadorOt.postergacion("29/10/2026 12:01", ahora))
        assertNotNull(ValidadorOt.postergacion("30/02/2026 12:00", ahora))
    }
}
