package com.example.proyecto_acis.domain

data class MaterialInforme(val nombre: String = "", val cantidad: String = "")

object MaterialesInforme {
    private val patron = Regex("^(.*?) · (.*)$")

    fun leer(texto: String): Pair<String, List<MaterialInforme>> {
        val anteriores = mutableListOf<String>()
        val lista = mutableListOf<MaterialInforme>()
        texto.lines().filter { it.isNotBlank() }.forEach { linea ->
            val partes = patron.matchEntire(linea)
            if (partes == null) anteriores.add(linea)
            else lista.add(MaterialInforme(partes.groupValues[1], partes.groupValues[2]))
        }
        return anteriores.joinToString("\n") to lista
    }

    fun escribir(anteriores: String, lista: List<MaterialInforme>): String =
        (listOf(anteriores).filter { it.isNotBlank() } + lista.map { "${it.nombre.trim()} · ${it.cantidad.trim()}" }).joinToString("\n")

    fun errorNombre(valor: String): String? = when {
        valor.isBlank() -> "Ingresa el nombre del material."
        valor.contains('\n') || valor.contains(" · ") -> "Usa un nombre en una sola línea."
        else -> null
    }

    fun errorCantidad(valor: String): String? {
        val numero = valor.replace(',', '.').toDoubleOrNull()
        return if (numero == null || !numero.isFinite() || numero <= 0 || !Regex("[0-9]+(?:[.,][0-9]+)?").matches(valor.trim()))
            "Ingresa una cantidad mayor que cero, por ejemplo 1 o 0,5." else null
    }
}
