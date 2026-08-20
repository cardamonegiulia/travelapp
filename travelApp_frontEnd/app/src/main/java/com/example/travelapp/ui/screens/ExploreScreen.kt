package com.example.travelapp.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.example.travelapp.ui.components.AppTopBar
import com.example.travelapp.ui.theme.BackgroundLavender
import com.example.travelapp.ui.theme.TextSecondary

/** Segnaposto della sezione "Explore", in attesa della UI definitiva. */
@Composable
fun ExploreScreen(modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        containerColor = BackgroundLavender,
        // Gli inset di sistema sono gia' gestiti dallo Scaffold che ospita il NavHost.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { AppTopBar(title = "Explore") }
    ) { innerPadding ->
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Text(
                text = "Sezione Explore in arrivo",
                fontSize = 15.sp,
                color = TextSecondary
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
