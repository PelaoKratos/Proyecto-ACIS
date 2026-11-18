package com.example.proyecto_acis.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.annotation.WorkerThread
import java.io.File
import java.io.IOException
import java.util.UUID

class ImagenNoValida(mensaje: String) : IOException(mensaje)

internal class AlmacenEvidencias(private val context: Context,
    private val limiteBytes: Long = 20L * 1024 * 1024) {

    @WorkerThread
    fun importar(uri: Uri, ordenId: String, tipo: String, asegurarActivo: () -> Unit = {}): File {
        require(ordenId.isNotBlank() && ordenId == File(ordenId).name && '\\' !in ordenId)
        require(tipo in listOf("Antes", "Después", "Otra"))
        val carpeta = File(context.filesDir, "evidencias").apply { check(isDirectory || mkdirs()) }
        val temporal = File.createTempFile("evidencia_", ".tmp", context.cacheDir)
        val destino = File(carpeta, "${ordenId}_${tipo}_${UUID.randomUUID()}.jpg")
        val bitmaps = mutableListOf<Bitmap>()
        var completa = false
        try {
            context.contentResolver.openInputStream(uri)?.use { entrada ->
                temporal.outputStream().use { salida ->
                    val buffer = ByteArray(8192)
                    var total = 0L
                    while (true) {
                        asegurarActivo()
                        val leidos = entrada.read(buffer)
                        if (leidos < 0) break
                        total += leidos
                        if (total > limiteBytes) throw ImagenNoValida("La foto es demasiado grande. Elige una de hasta 20 MB.")
                        salida.write(buffer, 0, leidos)
                    }
                }
            } ?: throw IOException("No se pudo abrir la imagen.")

            val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            temporal.inputStream().use { BitmapFactory.decodeStream(it, null, limites) }
            if (limites.outWidth <= 0 || limites.outHeight <= 0)
                throw ImagenNoValida("El archivo no es una imagen válida o su formato no es compatible.")
            var muestra = 1
            while (maxOf(limites.outWidth, limites.outHeight) / muestra > 2048) muestra *= 2
            asegurarActivo()
            val original = temporal.inputStream().use { entrada ->
                BitmapFactory.decodeStream(entrada, null, BitmapFactory.Options().apply {
                    inSampleSize = muestra; inPreferredConfig = Bitmap.Config.ARGB_8888
                })
            } ?: throw ImagenNoValida("No se pudo leer la foto. Elige otra imagen.")
            bitmaps.add(original)
            val matriz = orientacion(temporal)
            val orientada = if (matriz.isIdentity) original
                else Bitmap.createBitmap(original, 0, 0, original.width, original.height, matriz, true).also(bitmaps::add)
            val imagen = if (!orientada.hasAlpha()) orientada else {
                Bitmap.createBitmap(orientada.width, orientada.height, Bitmap.Config.ARGB_8888).also {
                    bitmaps.add(it)
                    Canvas(it).apply { drawColor(Color.WHITE); drawBitmap(orientada, 0f, 0f, null) }
                }
            }
            asegurarActivo()
            destino.outputStream().use { check(imagen.compress(Bitmap.CompressFormat.JPEG, 88, it)) }
            asegurarActivo()
            completa = true
            return destino
        } finally {
            bitmaps.distinct().forEach { it.recycle() }
            temporal.delete()
            if (!completa) destino.delete()
        }
    }

    private fun orientacion(archivo: File): Matrix {
        val valor = try { ExifInterface(archivo.path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) }
            catch (_: IOException) { ExifInterface.ORIENTATION_NORMAL }
        return Matrix().apply {
            when (valor) {
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> setScale(-1f, 1f)
                ExifInterface.ORIENTATION_ROTATE_180 -> setRotate(180f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> setScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> { setRotate(90f); postScale(-1f, 1f) }
                ExifInterface.ORIENTATION_ROTATE_90 -> setRotate(90f)
                ExifInterface.ORIENTATION_TRANSVERSE -> { setRotate(-90f); postScale(-1f, 1f) }
                ExifInterface.ORIENTATION_ROTATE_270 -> setRotate(270f)
            }
        }
    }
}
