package com.example.proyecto_acis.ui.state

import com.example.proyecto_acis.domain.model.PrioridadOt
import com.example.proyecto_acis.domain.model.TipoOt
import com.example.proyecto_acis.domain.validation.DatosCrearOt
import org.junit.Assert.*
import org.junit.Test

class EstadoCrearOrdenTest {
    private val datos = DatosCrearOt(
        local = "Sucursal centro", equipo = "Refrigerador", descripcion = "No enfría",
        tipo = TipoOt.CORRECTIVA, prioridad = PrioridadOt.MEDIA,
        fecha = "20/11/2026", hora = "09:00",
    )

    @Test fun abrirUnFormularioVacioNoMuestraErroresAntesDeIntentarContinuar() {
        val estado = EstadoCrearOrden()
        assertFalse(estado.errores.hayErrores)
        assertFalse(estado.validarPaso())
    }

    @Test fun primerPasoPermiteContinuarSinExigirDatosDeLasEtapasSiguientes() {
        val estado = EstadoCrearOrden(datos = DatosCrearOt(local = "Sucursal", equipo = "Equipo"))
        assertTrue(estado.validarPaso())
        assertFalse(estado.copy(paso = 2).validarPaso())
    }

    @Test fun corregirUnCampoActualizaSuErrorSinOcultarLosOtros() {
        val estado = EstadoCrearOrden(datos = datos.copy(fecha = "31/02/2026", hora = "25:00"),
            paso = 3, mostrarErrores = true)
        assertNotNull(estado.errores.fecha)
        val corregido = estado.copy(datos = estado.datos.copy(fecha = "20/11/2026"))
        assertNull(corregido.errores.fecha)
        assertNotNull(corregido.errores.hora)
        assertFalse(corregido.validarPaso())
    }

    @Test fun ultimoPasoRevisaElFormularioCompletoIncluidosDatosAnteriores() {
        assertTrue(EstadoCrearOrden(datos = datos, paso = 3).validarPaso())
        assertFalse(EstadoCrearOrden(datos = datos.copy(equipo = ""), paso = 3).validarPaso())
    }
}
