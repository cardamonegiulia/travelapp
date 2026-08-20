package com.example.travelapp.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelapp.ui.components.AppTopBar
import com.example.travelapp.ui.components.FavoriteTrip
import com.example.travelapp.ui.components.FavoriteTripCard
import com.example.travelapp.ui.theme.BackgroundLavender
import com.example.travelapp.ui.theme.OutlineGrey
import com.example.travelapp.ui.theme.SurfaceWhite
import com.example.travelapp.ui.theme.TextPrimary
import com.example.travelapp.ui.theme.TextSecondary

/**
 * Schermata "Preferiti": elenco scorrevole dei viaggi salvati dall'utente.
 *
 * Come la schermata Profilo e' puramente presentazionale: la lista [trips]
 * arriva da fuori e ogni interazione viene notificata tramite le lambda, senza
 * conoscere navigazione o ViewModel.
 */
@Composable
fun FavoritesScreen(
    trips: List<FavoriteTrip>,
    onBack: () -> Unit,
    onToggleFavorite: (String) -> Unit,
    onLoadMore: () -> Unit,
    onTripClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    canLoadMore: Boolean = true
) {
    Scaffold(
        modifier = modifier,
        containerColor = BackgroundLavender,
        // Gli inset di sistema sono gia' gestiti dallo Scaffold che ospita il NavHost.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { AppTopBar(title = "Preferiti", onBack = onBack) }
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (trips.isEmpty()) {
                item {
                    Text(
                        text = "Non hai ancora salvato nessun viaggio.",
                        fontSize = 14.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                }
            }

            items(items = trips, key = { trip -> trip.id }) { trip ->
                FavoriteTripCard(
                    trip = trip,
                    onToggleFavorite = onToggleFavorite,
                    onClick = onTripClick
                )
            }

            if (canLoadMore) {
                item {
                    LoadMoreButton(
                        onClick = onLoadMore,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }
            }
        }
    }
}

/** Bottone a pillola "Carica altri", centrato sotto la lista. */
@Composable
private fun LoadMoreButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedButton(
            onClick = onClick,
            shape = CircleShape,
            border = BorderStroke(1.dp, OutlineGrey),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = SurfaceWhite,
                contentColor = TextPrimary
            ),
            contentPadding = PaddingValues(horizontal = 28.dp, vertical = 10.dp)
        ) {
            Text(
                text = "Carica altri",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/** Dati segnaposto usati dall'anteprima e dalla route, in attesa del repository. */
internal val sampleFavoriteTrips = listOf(
    FavoriteTrip(
        id = "1",
        title = "Costiera Amalfitana: Tra Limoni e Mare",
        imageUrl = null,
        days = 5,
        priceFrom = 850
    ),
    FavoriteTrip(
        id = "2",
        title = "Ritiro in Toscana: Vino e Paesaggi",
        imageUrl = null,
        days = 4,
        priceFrom = 1200
    ),
    FavoriteTrip(
        id = "3",
        title = "Weekend Romano: Storia e Dolce Vita",
        imageUrl = null,
        days = 2,
        priceFrom = 450
    ),
    FavoriteTrip(
        id = "4",
        title = "Avventura nelle Dolomiti: Trekking e Relax",
        imageUrl = null,
        days = 6,
        priceFrom = 980
    )
)

@Preview(showBackground = true, showSystemUi = true, name = "Preferiti")
@Composable
private fun FavoritesScreenPreview() {
    var trips by remember { mutableStateOf(sampleFavoriteTrips) }

    MaterialTheme {
        FavoritesScreen(
            trips = trips,
            onBack = {},
            onToggleFavorite = { id ->
                trips = trips.map { trip ->
                    if (trip.id == id) trip.copy(isFavorite = !trip.isFavorite) else trip
                }
            },
            onLoadMore = {},
            onTripClick = {}
        )
    }
}
