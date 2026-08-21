package com.example.travelapp.data.remote

import android.content.Context
import android.net.Uri
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okio.BufferedSink
import okio.source
import java.io.IOException

/** Il file scelto non è in un formato che il backend accetta, o è troppo grande. */
class ImmagineNonCaricabile(message: String) : Exception(message)

/**
 * Costruisce la parte multipart a partire dall'URI restituito dal photo picker di sistema.
 *
 * Due vincoli del backend guidano tutto quello che c'è qui dentro (vedi
 * `ImmagineStorageService`): il file deve essere JPEG o PNG **e** l'estensione dichiarata
 * nel nome deve coincidere col contenuto reale, che il server riconosce leggendo i primi
 * byte. Un `content://` non ha un'estensione, quindi il nome va costruito — e va costruito
 * a partire dal contenuto, non da quello che dichiara il content resolver, altrimenti si
 * manderebbe un `.png` con dentro un JPEG e il server lo rifiuterebbe (400).
 */
object CorpoImmagine {

    /** Stesso limite di `app.storage.immagini.max-size-byte`: meglio dirlo prima di caricare 5 MB. */
    private const val DIMENSIONE_MASSIMA_BYTE = 5L * 1024 * 1024

    private const val BYTE_INTESTAZIONE = 8

    private val JPEG = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())
    private val PNG = byteArrayOf(
        0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    )

    /**
     * @throws ImmagineNonCaricabile se il file non è leggibile, non è JPEG/PNG o supera il limite
     */
    fun da(context: Context, uri: Uri, nomeCampo: String = "file"): MultipartBody.Part {
        val formato = riconosciFormato(context, uri)
        val dimensione = dimensione(context, uri)

        if (dimensione != null && dimensione > DIMENSIONE_MASSIMA_BYTE) {
            throw ImmagineNonCaricabile("L'immagine supera i 5 MB consentiti")
        }

        val corpo = CorpoUri(context, uri, formato.contentType.toMediaType(), dimensione)
        return MultipartBody.Part.createFormData(nomeCampo, "foto.${formato.estensione}", corpo)
    }

    private enum class Formato(val contentType: String, val estensione: String) {
        JPG("image/jpeg", "jpg"),
        PNG("image/png", "png")
    }

    private fun riconosciFormato(context: Context, uri: Uri): Formato {
        val intestazione = try {
            context.contentResolver.openInputStream(uri).use { flusso ->
                flusso ?: throw ImmagineNonCaricabile("Immagine non leggibile")
                ByteArray(BYTE_INTESTAZIONE).also { flusso.read(it) }
            }
        } catch (e: IOException) {
            throw ImmagineNonCaricabile("Immagine non leggibile")
        }

        return when {
            intestazione.iniziaCon(PNG) -> Formato.PNG
            intestazione.iniziaCon(JPEG) -> Formato.JPG
            else -> throw ImmagineNonCaricabile("Sono accettate solo immagini JPEG o PNG")
        }
    }

    private fun ByteArray.iniziaCon(firma: ByteArray): Boolean =
        size >= firma.size && firma.indices.all { this[it] == firma[it] }

    /** `null` quando il provider non la dichiara: in quel caso decide il limite del server. */
    private fun dimensione(context: Context, uri: Uri): Long? =
        try {
            context.contentResolver.openAssetFileDescriptor(uri, "r")
                ?.use { it.length.takeIf { lunghezza -> lunghezza >= 0 } }
        } catch (e: IOException) {
            null
        }

    /**
     * Corpo che legge direttamente dal content resolver invece di copiare il file in
     * memoria: una foto da 5 MB non deve passare per un `ByteArray`.
     */
    private class CorpoUri(
        private val context: Context,
        private val uri: Uri,
        private val tipo: MediaType,
        private val dimensione: Long?
    ) : RequestBody() {

        override fun contentType(): MediaType = tipo

        override fun contentLength(): Long = dimensione ?: -1L

        override fun writeTo(sink: BufferedSink) {
            val flusso = context.contentResolver.openInputStream(uri)
                ?: throw IOException("Immagine non piu' leggibile: $uri")
            flusso.use { sink.writeAll(it.source()) }
        }
    }
}
