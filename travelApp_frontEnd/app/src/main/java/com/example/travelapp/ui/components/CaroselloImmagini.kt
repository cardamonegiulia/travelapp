package com.example.travelapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelapp.domain.model.ImmagineResponse

/**
 * Galleria a scorrimento orizzontale delle foto di un itinerario o di un'attivita'.
 *
 * Con una sola foto (o nessuna) si comporta esattamente come [AuthedAsyncImage]: niente
 * pallini ne' contatore, perche' non c'e' niente da scorrere.
 */
@Composable
fun CaroselloImmagini(
    immagini: List<ImmagineResponse>,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {

    val pagerState = rememberPagerState(
        pageCount = { immagini.size.coerceAtLeast(1) }
    )

    Box(modifier = modifier) {

        if (immagini.isEmpty()) {

            // Nessuna foto: resta il segnaposto gestito da AuthedAsyncImage.
            AuthedAsyncImage(
                url = null,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

        } else {

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { pagina ->

                AuthedAsyncImage(
                    url = immagini[pagina].url,
                    contentDescription = contentDescription,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        if (immagini.size > 1) {

            ContatoreFoto(
                posizione = pagerState.currentPage + 1,
                totale = immagini.size
            )

            PallinePagine(
                paginaCorrente = pagerState.currentPage,
                totale = immagini.size
            )
        }
    }
}

/** "3 / 7" in alto a destra: dice subito quante foto ci sono. */
@Composable
private fun BoxScope.ContatoreFoto(
    posizione: Int,
    totale: Int
) {

    Box(
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(12.dp)
            .background(
                Color.Black.copy(alpha = 0.55f),
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {

        Text(
            text = "$posizione / $totale",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun BoxScope.PallinePagine(
    paginaCorrente: Int,
    totale: Int
) {

    Row(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            // Un solo testo per l'intera fila: i pallini sono una decorazione, letti uno
            // per uno direbbero soltanto "immagine, immagine, immagine".
            .semantics {
                this.contentDescription =
                    "Foto ${paginaCorrente + 1} di $totale"
            },
        horizontalArrangement = Arrangement.Center
    ) {

        repeat(totale) { indice ->

            Box(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .size(
                        if (indice == paginaCorrente) 9.dp else 7.dp
                    )
                    .background(
                        color =
                            if (indice == paginaCorrente) {
                                Color.White
                            } else {
                                Color.White.copy(alpha = 0.5f)
                            },
                        shape = CircleShape
                    )
            )
        }
    }
}
