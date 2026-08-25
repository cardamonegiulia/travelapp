package com.example.travelapp.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

/** Il file scelto non è un'immagine che si riesca a leggere e preparare per l'upload. */
class ImmagineNonCaricabile(message: String) : Exception(message)

/**
 * Prepara la parte multipart a partire dall'URI restituito dal photo picker di sistema.
 *
 * L'immagine **non** viene spedita com'è: viene decodificata, ridotta a [LATO_MASSIMO_PX]
 * sul lato lungo e ricompressa in JPEG. La ragione è misurata, non teorica: il backend,
 * ricevuto il file, lo ricarica su object storage (Cloudflare R2) passando per la
 * connessione di casa, che in upload viaggia intorno ai 150 KB/s. Una foto da fotocamera —
 * 3 MB — impiega lì fra i venti e i quaranta secondi, cioè più del timeout della
 * transazione lato server e più del `readTimeout` del client: l'utente vede la rotella
 * girare a lungo e poi un errore. Ridotta a 1024 px la stessa foto pesa 150-250 KB e il
 * giro si chiude in qualche secondo — che per un avatar mostrato a poche decine di dp è
 * comunque più risoluzione di quanta ne serva.
 *
 * Ricomprimere risolve da sé anche il vincolo che il backend impone sul formato (JPEG o
 * PNG, con l'estensione del nome coerente col contenuto reale, che legge dai primi byte):
 * qualunque cosa restituisca il picker — HEIC compreso, che è il formato predefinito di
 * parecchie fotocamere — quello che parte è sempre un JPEG di nome `foto.jpg`.
 */
object CorpoImmagine {

    /** Lato lungo dell'immagine spedita. Abbondante per un avatar, economico da caricare. */
    private const val LATO_MASSIMO_PX = 1024

    /** Sopra l'85 il file cresce in fretta senza che si veda la differenza. */
    private const val QUALITA_JPEG = 85

    /**
     * @throws ImmagineNonCaricabile se l'URI non è leggibile o non contiene un'immagine che
     *         il sistema sappia decodificare
     */
    fun da(context: Context, uri: Uri, nomeCampo: String = "file"): MultipartBody.Part {
        val bitmap = decodificaRidotta(context, uri)
        val jpeg = try {
            comprimi(raddrizza(context, uri, bitmap))
        } finally {
            bitmap.recycle()
        }

        return MultipartBody.Part.createFormData(
            nomeCampo,
            "foto.jpg",
            jpeg.toRequestBody("image/jpeg".toMediaType())
        )
    }

    /**
     * Decodifica l'immagine già rimpicciolita.
     *
     * Due passaggi, e servono entrambi: il primo legge solo l'intestazione
     * (`inJustDecodeBounds`) per sapere quanto è grande il file senza allocare niente, il
     * secondo decodifica saltando pixel (`inSampleSize`, che accetta solo potenze di due).
     * Senza il primo passaggio una foto da 12 megapixel occuperebbe una quarantina di MB di
     * heap prima ancora di poter essere ridotta, e su un telefono modesto è un
     * `OutOfMemoryError`.
     */
    private fun decodificaRidotta(context: Context, uri: Uri): Bitmap {
        val misura = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        apri(context, uri).use { BitmapFactory.decodeStream(it, null, misura) }

        if (misura.outWidth <= 0 || misura.outHeight <= 0) {
            throw ImmagineNonCaricabile("Il file scelto non è un'immagine leggibile")
        }

        val opzioni = BitmapFactory.Options().apply {
            inSampleSize = fattoreCampionamento(misura.outWidth, misura.outHeight)
        }

        val grezza = try {
            apri(context, uri).use { BitmapFactory.decodeStream(it, null, opzioni) }
        } catch (e: OutOfMemoryError) {
            throw ImmagineNonCaricabile("L'immagine è troppo grande per essere elaborata")
        } ?: throw ImmagineNonCaricabile("Il file scelto non è un'immagine leggibile")

        // `inSampleSize` si ferma alla potenza di due immediatamente sopra il bersaglio (una
        // 4000x3000 diventa 1000x750 con fattore 4, ma una 2600x1950 resta 1300x975 con
        // fattore 2): questa seconda scalatura porta il lato lungo esattamente al limite.
        return riduciAlLimite(grezza)
    }

    /** La potenza di due più grande che lascia ancora il lato lungo sopra il limite. */
    private fun fattoreCampionamento(larghezza: Int, altezza: Int): Int {
        var fattore = 1
        while (maxOf(larghezza, altezza) / (fattore * 2) >= LATO_MASSIMO_PX) {
            fattore *= 2
        }
        return fattore
    }

    private fun riduciAlLimite(bitmap: Bitmap): Bitmap {
        val latoLungo = maxOf(bitmap.width, bitmap.height)
        if (latoLungo <= LATO_MASSIMO_PX) {
            return bitmap
        }

        val scala = LATO_MASSIMO_PX.toFloat() / latoLungo
        val ridotta = Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * scala).toInt().coerceAtLeast(1),
            (bitmap.height * scala).toInt().coerceAtLeast(1),
            true
        )
        if (ridotta !== bitmap) {
            bitmap.recycle()
        }
        return ridotta
    }

    /**
     * Applica la rotazione dichiarata nell'EXIF.
     *
     * Le fotocamere salvano i pixel sempre nello stesso verso e annotano a parte come va
     * girata l'immagine. Ricomprimendo si perdono i metadati, quindi la rotazione va
     * incorporata nei pixel adesso: senza, i ritratti arriverebbero coricati.
     */
    private fun raddrizza(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        val orientamento = try {
            apri(context, uri).use {
                ExifInterface(it).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            }
        } catch (e: IOException) {
            // l'EXIF è un extra: se manca o è illeggibile si spedisce l'immagine com'è
            ExifInterface.ORIENTATION_NORMAL
        }

        val matrice = Matrix()
        when (orientamento) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrice.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrice.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrice.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrice.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrice.postScale(1f, -1f)
            else -> return bitmap
        }

        return try {
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrice, true)
        } catch (e: OutOfMemoryError) {
            bitmap
        }
    }

    /**
     * Comprime in JPEG, che non ha canale alfa: le zone trasparenti di un PNG diventerebbero
     * nere, quindi l'immagine viene prima appoggiata su un fondo bianco.
     */
    private fun comprimi(bitmap: Bitmap): ByteArray {
        val opaca = if (bitmap.hasAlpha()) suFondoBianco(bitmap) else bitmap

        val uscita = ByteArrayOutputStream()
        val riuscita = opaca.compress(Bitmap.CompressFormat.JPEG, QUALITA_JPEG, uscita)
        if (opaca !== bitmap) {
            opaca.recycle()
        }
        if (!riuscita) {
            throw ImmagineNonCaricabile("Non è stato possibile preparare l'immagine")
        }
        return uscita.toByteArray()
    }

    private fun suFondoBianco(bitmap: Bitmap): Bitmap {
        val piatta = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        Canvas(piatta).apply {
            drawColor(Color.WHITE)
            drawBitmap(bitmap, 0f, 0f, null)
        }
        return piatta
    }

    /**
     * Il permesso di leggere l'URI del picker vale per la sessione corrente: se decade, o se
     * il provider non risponde, è un errore da mostrare all'utente, non un guasto.
     */
    private fun apri(context: Context, uri: Uri): InputStream =
        try {
            context.contentResolver.openInputStream(uri)
                ?: throw ImmagineNonCaricabile("Immagine non leggibile")
        } catch (e: IOException) {
            throw ImmagineNonCaricabile("Immagine non leggibile")
        } catch (e: SecurityException) {
            throw ImmagineNonCaricabile("Immagine non più accessibile: riprova a selezionarla")
        }
}
