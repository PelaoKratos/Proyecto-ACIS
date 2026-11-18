package com.example.proyecto_acis.device.maps

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.proyecto_acis.domain.validation.ValidadorCatastro

fun buscarLocalEnMapa(context: Context, local: String): Boolean {
    if (local.isBlank()) return false
    val uri = Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(local)}")
    return try {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}

fun abrirUbicacionVisita(context: Context, ubicacion: String): Boolean {
    val coordenadas = com.example.proyecto_acis.domain.validation.ValidadorCatastro.leerUbicacion(ubicacion) ?: return false
    return buscarLocalEnMapa(context, "${coordenadas.first},${coordenadas.second}")
}
