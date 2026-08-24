package com.example.travelapp.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.travelapp.domain.model.Itinerario
import com.example.travelapp.domain.model.SingolaAttivita
import com.example.travelapp.ui.catalog.CatalogScreen
import com.example.travelapp.ui.theme.BackgroundLavender

@Composable
fun ExploreScreen(
    modifier: Modifier = Modifier,
    onItinerarioClick: (Itinerario) -> Unit = {},
    onAttivitaClick: (SingolaAttivita) -> Unit = {}
) {
    Scaffold(
        modifier = modifier,
        containerColor = BackgroundLavender,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            CatalogScreen(
                onItinerarioClick = onItinerarioClick,
                onAttivitaClick = onAttivitaClick
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Explore")
@Composable
private fun ExploreScreenPreview() {
    MaterialTheme {
        ExploreScreen()
    }
}