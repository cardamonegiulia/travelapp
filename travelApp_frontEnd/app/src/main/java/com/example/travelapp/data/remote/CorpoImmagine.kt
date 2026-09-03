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

class ImmagineNonCaricabile(message: String) : Exception(message)

object CorpoImmagine {

    private const val LATO_MASSIMO_PX = 1024

    private const val QUALITA_JPEG = 85

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

        return riduciAlLimite(grezza)
    }

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

    private fun raddrizza(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        val orientamento = try {
            apri(context, uri).use {
                ExifInterface(it).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            }
        } catch (e: IOException) {
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
