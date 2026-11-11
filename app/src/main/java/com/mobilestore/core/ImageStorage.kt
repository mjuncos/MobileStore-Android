package com.mobilestore.core

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * El Photo Picker entrega un permiso de lectura temporal sobre la Uri, que
 * se pierde al reiniciar la app. Para que la imagen elegida sobreviva,
 * copiamos el archivo al almacenamiento privado de la aplicacion.
 *
 * Al usar el Photo Picker + almacenamiento interno no hace falta pedir
 * READ_MEDIA_IMAGES ni READ_EXTERNAL_STORAGE (punto 3.6: permisos solo
 * cuando son necesarios).
 */
object ImageStorage {

    private const val DIR_NAME = "product_images"

    suspend fun persist(context: Context, source: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(context.filesDir, DIR_NAME).apply { mkdirs() }
            val target = File(dir, "${UUID.randomUUID()}.jpg")

            context.contentResolver.openInputStream(source)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            } ?: return@runCatching null

            Uri.fromFile(target).toString()
        }.getOrNull()
    }

    /** Borra la copia local si el usuario quita la imagen del formulario. */
    fun deleteIfLocal(value: String?) {
        val path = value ?: return
        if (!path.startsWith("file://")) return
        runCatching { File(Uri.parse(path).path!!).delete() }
    }
}
