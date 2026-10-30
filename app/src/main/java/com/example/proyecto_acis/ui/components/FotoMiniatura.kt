package com.example.proyecto_acis.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun FotoMiniatura(nombre: String, etiqueta: String, onEliminar: (() -> Unit)? = null) {
    val context = LocalContext.current
    var ampliada by remember { mutableStateOf(false) }
    var cargando by remember(nombre) { mutableStateOf(true) }
    val foto by produceState<ImageBitmap?>(null, nombre) {
        value = withContext(Dispatchers.IO) {
            val archivo = File(File(context.filesDir, "evidencias"), nombre)
            val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(archivo.path, limites)
            var muestra = 1
            while (maxOf(limites.outWidth, limites.outHeight) / muestra > 1000) muestra *= 2
            val opciones = BitmapFactory.Options().apply { inSampleSize = muestra }
            BitmapFactory.decodeFile(archivo.path, opciones)?.asImageBitmap()
        }
        cargando = false
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(etiqueta, style = MaterialTheme.typography.labelLarge)
        Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.background) {
            val imagen = foto
            if (imagen != null) Image(imagen, contentDescription = etiqueta,
                modifier = Modifier.fillMaxWidth().aspectRatio(1.25f), contentScale = ContentScale.Fit)
            else Box(Modifier.fillMaxWidth().height(110.dp).padding(8.dp)) {
                Text(if (cargando) "Cargando foto…" else "Foto no disponible", style = MaterialTheme.typography.bodySmall)
            }
        }
        if (foto != null) TextButton(onClick = { ampliada = true }) { Text("Ampliar foto") }
        if (onEliminar != null) TextButton(onClick = onEliminar) { Text("Eliminar foto") }
    }
    if (ampliada) Dialog(onDismissRequest = { ampliada = false }) {
        Surface(shape = MaterialTheme.shapes.medium) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(etiqueta, style = MaterialTheme.typography.titleMedium)
                foto?.let { Image(it, contentDescription = etiqueta,
                    modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp), contentScale = ContentScale.Fit) }
                TextButton(onClick = { ampliada = false }, modifier = Modifier.fillMaxWidth()) { Text("Cerrar") }
            }
        }
    }
}
