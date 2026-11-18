package com.example.proyecto_acis.data.local

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import com.example.proyecto_acis.data.repository.OrdenesRepository
import com.example.proyecto_acis.domain.model.EstadoOt
import com.example.proyecto_acis.domain.model.OrdenTrabajo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AlmacenEvidenciasTest {
    private val context get() = RuntimeEnvironment.getApplication()
    private val carpeta get() = File(context.filesDir, "evidencias").apply { mkdirs() }

    @Test fun copiaUnaImagenAunqueSeBorreElOriginal() {
        val original = imagen()
        val copia = AlmacenEvidencias(context).importar(uri(original), "OT-999", "Después")
        assertTrue(copia.name.startsWith("OT-999_Después_"))
        assertNotEquals(original, copia)
        original.delete()
        val imagen = BitmapFactory.decodeFile(copia.path)
        assertNotNull(imagen)
        assertEquals(40, imagen.width)
        assertEquals(20, imagen.height)
        imagen.recycle()
    }

    @Test fun rechazaArchivosQueNoSonImagenesSinDejarCopias() {
        val original = File(carpeta, "incorrecta.txt").apply { writeText("Esto no es una foto") }
        val antes = carpeta.list()!!.toSet()
        try {
            AlmacenEvidencias(context).importar(uri(original), "OT-999", "Antes")
            fail("Debe rechazar el archivo")
        } catch (_: ImagenNoValida) { }
        assertEquals(antes, carpeta.list()!!.toSet())
        assertTrue(context.cacheDir.list()!!.none { it.startsWith("evidencia_") })
    }

    @Test fun limitaElTamanoAntesDeDecodificar() {
        val original = imagen()
        val antes = carpeta.list()!!.toSet()
        try {
            AlmacenEvidencias(context, limiteBytes = 8).importar(uri(original), "OT-999", "Antes")
            fail("Debe rechazar un archivo que supera el límite")
        } catch (_: ImagenNoValida) { }
        assertEquals(antes, carpeta.list()!!.toSet())
    }

    @Test fun cancelarLaImportacionLimpiaLosTemporales() {
        val original = imagen()
        val antes = carpeta.list()!!.toSet()
        try {
            AlmacenEvidencias(context).importar(uri(original), "OT-999", "Otra") {
                throw CancellationException("Pantalla cerrada")
            }
            fail("Debe propagar la cancelación")
        } catch (_: CancellationException) { }
        assertEquals(antes, carpeta.list()!!.toSet())
        assertTrue(context.cacheDir.list()!!.none { it.startsWith("evidencia_") })
    }

    @Test fun reduceLasImagenesGrandesParaNoCargarLaPantalla() {
        val original = imagen(4096, 32)
        val copia = AlmacenEvidencias(context).importar(uri(original), "OT-999", "Antes")
        val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(copia.path, limites)
        assertTrue(limites.outWidth in 1..2048)
        assertTrue(limites.outHeight > 0)
    }

    @Test fun conservaLaFotoEnLaOrdenAlReabrirRoom() = runBlocking(Dispatchers.IO) {
        conOrden { db, repo, nombre ->
            val original = imagen()
            assertTrue(repo.importarFoto("OT-999", uri(original), "Antes"))
            val foto = repo.ordenes.value.single().fotosEvidencia.single()
            original.delete()
            db.close()
            OrdenesDb(context, nombre).use { reabierta ->
                assertEquals(listOf(foto), reabierta.leer().single().fotosEvidencia)
                assertNotNull(BitmapFactory.decodeFile(File(carpeta, foto).path))
            }
        }
    }

    @Test fun unFalloEnRoomNoDejaUnaFotoHuerfana() = runBlocking(Dispatchers.IO) {
        conOrden { db, repo, _ ->
            val original = imagen()
            val archivos = carpeta.list()!!.toSet()
            val revision = db.leerSync().single()
            db.writableDatabase.execSQL("CREATE TRIGGER fallo_foto BEFORE UPDATE ON ordenes BEGIN SELECT RAISE(ABORT, 'fallo'); END")
            assertFalse(repo.importarFoto("OT-999", uri(original), "Antes"))
            assertEquals(archivos, carpeta.list()!!.toSet())
            assertTrue(db.leer().single().fotosEvidencia.isEmpty())
            assertEquals(revision, db.leerSync().single())
        }
    }

    @Test fun noImportaFotosSiLaOrdenYaNoEstaEnVisita() = runBlocking(Dispatchers.IO) {
        conOrden { db, repo, _ ->
            assertTrue(db.actualizar(db.leer().single().copy(estado = EstadoOt.CERRADA), EstadoOt.EN_VISITA))
            assertFalse(repo.importarFoto("OT-INEXISTENTE", android.net.Uri.EMPTY, "Antes"))
            assertFalse(repo.importarFoto("OT-999", uri(imagen()), "Antes"))
            assertTrue(db.leer().single().fotosEvidencia.isEmpty())
        }
    }

    private suspend fun conOrden(accion: suspend (OrdenesDb, OrdenesRepository, String) -> Unit) {
        val nombre = "fotos_${UUID.randomUUID()}.db"
        val db = OrdenesDb(context, nombre)
        try {
            assertTrue(db.insertar(OrdenTrabajo("OT-999", "Local", "Equipo", "Revisar", "20/11/2026 09:00",
                estado = EstadoOt.EN_VISITA)))
            val repo = OrdenesRepository(context, db, "${nombre}_preferencias")
            repo.inicializar()
            accion(db, repo, nombre)
        } finally {
            db.close()
            context.deleteDatabase(nombre)
        }
    }

    private fun imagen(ancho: Int = 40, alto: Int = 20): File {
        val archivo = File(carpeta, "original_${UUID.randomUUID()}.png")
        val bitmap = Bitmap.createBitmap(ancho, alto, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.BLUE)
        archivo.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        bitmap.recycle()
        return archivo
    }

    private fun uri(archivo: File): android.net.Uri {
        val uri = android.net.Uri.parse("content://galeria-prueba/${archivo.name}")
        // El selector entrega un content URI; aquí controlamos su contenido.
        shadowOf(context.contentResolver).registerInputStream(uri, archivo.inputStream())
        return uri
    }
}
