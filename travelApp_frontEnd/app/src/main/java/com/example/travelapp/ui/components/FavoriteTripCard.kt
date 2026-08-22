package com.example.travelapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelapp.ui.theme.AccentOrange
import com.example.travelapp.ui.theme.CoverPlaceholderEnd
import com.example.travelapp.ui.theme.CoverPlaceholderIcon
import com.example.travelapp.ui.theme.CoverPlaceholderStart
import com.example.travelapp.ui.theme.IconPink
import com.example.travelapp.ui.theme.SurfaceWhite
import com.example.travelapp.ui.theme.TextPrimary
import com.example.travelapp.ui.theme.TextSecondary
import java.text.NumberFormat
import java.util.Locale

private val TripCardShape = RoundedCornerShape(16.dp)
private val CoverShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
private val CoverHeight = 140.dp

/** Viaggio mostrato nell'elenco dei preferiti. */
data class FavoriteTrip(
    val id: String,
    val title: String,
    val imageUrl: String?,
    val days: Int,
    val priceFrom: Int,
    val isFavorite: Boolean = true
)

/**
 * Card di un viaggio: copertina con il cuore dei preferiti, titolo, durata e
 * prezzo di partenza.
 *
 * Componente presentazionale riusabile anche fuori dalla schermata "Preferiti"
 * (es. nei risultati di ricerca): riceve il dato da mostrare e si limita a
 * notificare i tocchi.
 */
@Composable
fun FavoriteTripCard(
    trip: FavoriteTrip,
    onToggleFavorite: (String) -> Unit,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = { onClick(trip.id) },
        shape = TripCardShape,
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            TripCover(imageUrl = trip.imageUrl)
            FavoriteBadge(
                isFavorite = trip.isFavorite,
                onClick = { onToggleFavorite(trip.id) },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
            )
        }

        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(
                text = trip.title,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = formatDuration(trip.days),
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "A partire da",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Text(
                    text = formatPrice(trip.priceFrom),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentOrange
                )
            }
        }
    }
}

/**
 * Copertina del viaggio, con angoli arrotondati solo in alto.
 *
 * [imageUrl] fa gia' parte del modello, ma il modulo non dipende da una libreria
 * di image loading: finche' non viene aggiunta (es. Coil) si mostra un
 * segnaposto, e bastera' sostituirlo con
 * `AsyncImage(model = imageUrl, contentScale = ContentScale.Crop)`.
 */
@Composable
private fun TripCover(
    imageUrl: String?,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .height(CoverHeight)
            .clip(CoverShape)
            .background(
                Brush.linearGradient(listOf(CoverPlaceholderStart, CoverPlaceholderEnd))
            )
    ) {
        // TODO: sostituire con AsyncImage(model = imageUrl) quando Coil sara' disponibile.
        Icon(
            imageVector = Icons.Default.Place,
            contentDescription = null,
            tint = CoverPlaceholderIcon,
            modifier = Modifier.size(40.dp)
        )
    }
}

/** Cuore dei preferiti sovrapposto alla copertina. */
@Composable
private fun FavoriteBadge(
    isFavorite: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.85f))
            .clickable(onClick = onClick)
    ) {
        Icon(
            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            contentDescription = if (isFavorite) {
                "Rimuovi dai preferiti"
            } else {
                "Aggiungi ai preferiti"
            },
            tint = IconPink,
            modifier = Modifier.size(18.dp)
        )
    }
}

/** Durata del viaggio, es. "5 giorni". */
private fun formatDuration(days: Int): String =
    if (days == 1) "1 giorno" else days.toString() + " giorni"

/** Prezzo con separatore delle migliaia all'italiana, es. "1.200". */
private fun formatPrice(amount: Int): String =
    "€ " + NumberFormat.getIntegerInstance(Locale.ITALY).format(amount)
