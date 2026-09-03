package com.example.travelapp.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Icone disegnate a mano, per i casi che il set Material di base non copre.
 *
 * Stesso stile delle Material "outlined" gia' usate altrove: tratto sottile, nessun
 * riempimento, viewport 24x24. Il colore resta nero perche' e' il `tint` di [androidx.compose.material3.Icon]
 * a decidere quello effettivo.
 */
object AppIcons

private var orologioCache: ImageVector? = null

/** Orologio: durata di un'attivita'. Nel set base non c'e' nessuna icona di tempo. */
val AppIcons.Orologio: ImageVector
    get() {
        orologioCache?.let { return it }

        val icona = ImageVector.Builder(
            name = "Orologio",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.6f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                // quadrante: due semicerchi, perche' un arco solo non chiude il cerchio
                moveTo(12f, 3.5f)
                arcToRelative(8.5f, 8.5f, 0f, false, true, 0f, 17f)
                arcToRelative(8.5f, 8.5f, 0f, false, true, 0f, -17f)
                close()

                // lancette
                moveTo(12f, 7.4f)
                lineTo(12f, 12.2f)
                lineTo(15.4f, 14f)
            }
        }.build()

        orologioCache = icona
        return icona
    }
