package com.example.travelapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.travelapp.ui.components.AppTopBar
import com.example.travelapp.ui.components.LogoutButton
import com.example.travelapp.ui.components.ProfileHeaderCard
import com.example.travelapp.ui.components.ProfileIcons
import com.example.travelapp.ui.components.ProfileMenuRow
import com.example.travelapp.ui.components.ProfileSwitchRow
import com.example.travelapp.ui.components.SectionTitle
import com.example.travelapp.ui.profilo.ProfiloUiState
import com.example.travelapp.ui.theme.BadgeBlue
import com.example.travelapp.ui.theme.BadgeGrey
import com.example.travelapp.ui.theme.BadgeIndigo
import com.example.travelapp.ui.theme.BadgePurple
import com.example.travelapp.ui.theme.BadgeTeal
import com.example.travelapp.ui.theme.BackgroundLavender
import com.example.travelapp.ui.theme.IconBlue
import com.example.travelapp.ui.theme.IconGrey
import com.example.travelapp.ui.theme.IconIndigo
import com.example.travelapp.ui.theme.IconPurple
import com.example.travelapp.ui.theme.IconTeal

@Composable
fun ProfileScreen(
    state: ProfiloUiState,
    onBack: () -> Unit,
    onAddProfilePhoto: () -> Unit,

    // Viaggiatore
    onBookingsClick: () -> Unit = {},
    onPaymentsClick: () -> Unit = {},
    onFavoritesClick: () -> Unit = {},
    onReviewsClick: () -> Unit = {},

    // Organizzatore
    onCreaItinerarioClick: () -> Unit = {},
    onCreaAttivitaClick: () -> Unit = {},
    onLeMieOfferteClick: () -> Unit = {},

    // Admin
    onGestioneOfferteAdminClick: () -> Unit = {},
    onGestioneUtentiAdminClick: () -> Unit = {},

    // Impostazioni
    onToggleDarkMode: (Boolean) -> Unit = {},
    onChangePassword: () -> Unit = {},
    onLogout: () -> Unit = {},

    modifier: Modifier = Modifier,
    onPhotoMessageShown: () -> Unit = {}
) {

    val snackbarHostState = remember {
        SnackbarHostState()
    }

    LaunchedEffect(state.photoMessage) {
        state.photoMessage?.let { messaggio ->
            snackbarHostState.showSnackbar(messaggio)
            onPhotoMessageShown()
        }
    }

    /*
     * ============================================================
     * RUOLO
     * ============================================================
     */

    val isAdmin =
        state.ruolo.contains(
            "ADMIN",
            ignoreCase = true
        )

    val isOrganizzatore =
        state.ruolo.contains(
            "ORGANIZZATORE",
            ignoreCase = true
        )

    Scaffold(
        modifier = modifier,
        containerColor = BackgroundLavender,
        contentWindowInsets = WindowInsets(
            0,
            0,
            0,
            0
        ),
        topBar = {
            AppTopBar(
                title = "Profilo",
                onBack = onBack
            )
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState
            )
        }
    ) { innerPadding ->

        Column(
            verticalArrangement =
                Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(
                    horizontal = 16.dp,
                    vertical = 8.dp
                )
        ) {

            /*
             * ============================================================
             * HEADER
             * ============================================================
             */

            ProfileHeaderCard(
                name = state.name,
                email = state.email,
                avatarUrl = state.avatarUrl,
                isPhotoUploading =
                    state.isPhotoUploading,
                onAddProfilePhoto =
                    onAddProfilePhoto
            )

            /*
             * ============================================================
             * ADMIN
             * ============================================================
             */

            if (isAdmin) {

                SectionTitle(
                    text = "Pannello Amministrazione",
                    modifier =
                        Modifier.padding(top = 14.dp)
                )

                ProfileMenuRow(
                    icon =
                        Icons.AutoMirrored.Filled.List,
                    title =
                        "Tutte le offerte della piattaforma",
                    iconTint =
                        IconPurple,
                    badgeColor =
                        BadgePurple,
                    onClick =
                        onGestioneOfferteAdminClick
                )

                ProfileMenuRow(
                    icon =
                        Icons.Default.Person,
                    title =
                        "Gestione utenti registrati",
                    iconTint =
                        IconIndigo,
                    badgeColor =
                        BadgeIndigo,
                    onClick =
                        onGestioneUtentiAdminClick
                )
            }

            /*
             * ============================================================
             * ATTIVITÀ VIAGGIATORE
             * ============================================================
             */

            if (!isOrganizzatore) {

                SectionTitle(
                    text = "Le mie attività",
                    modifier =
                        Modifier.padding(top = 14.dp)
                )

                ProfileMenuRow(
                    icon =
                        Icons.Default.MailOutline,
                    title =
                        "Le mie prenotazioni",
                    iconTint =
                        IconBlue,
                    badgeColor =
                        BadgeBlue,
                    onClick =
                        onBookingsClick
                )

                ProfileMenuRow(
                    icon =
                        ProfileIcons.CreditCard,
                    title =
                        "I miei pagamenti",
                    iconTint =
                        IconTeal,
                    badgeColor =
                        BadgeTeal,
                    onClick =
                        onPaymentsClick
                )

                ProfileMenuRow(
                    icon =
                        Icons.Default.Favorite,
                    title =
                        "Preferiti",
                    iconTint =
                        IconPurple,
                    badgeColor =
                        BadgePurple,
                    onClick =
                        onFavoritesClick
                )

                ProfileMenuRow(
                    icon =
                        ProfileIcons.Review,
                    title =
                        "Recensioni scritte",
                    iconTint =
                        IconPurple,
                    badgeColor =
                        BadgePurple,
                    onClick =
                        onReviewsClick
                )
            }

            /*
             * ============================================================
             * IMPOSTAZIONI
             * ============================================================
             */

            SectionTitle(
                text = "Impostazioni",
                modifier =
                    Modifier.padding(top = 14.dp)
            )

            ProfileSwitchRow(
                icon =
                    ProfileIcons.DarkMode,
                title =
                    "Tema scuro",
                iconTint =
                    IconIndigo,
                badgeColor =
                    BadgeIndigo,
                checked =
                    state.isDarkModeEnabled,
                onCheckedChange =
                    onToggleDarkMode
            )

            ProfileMenuRow(
                icon =
                    Icons.Default.Lock,
                title =
                    "Cambia password",
                iconTint =
                    IconGrey,
                badgeColor =
                    BadgeGrey,
                onClick =
                    onChangePassword
            )

            LogoutButton(
                onClick = onLogout,
                modifier =
                    Modifier.padding(top = 20.dp)
            )

            Spacer(
                modifier =
                    Modifier.height(16.dp)
            )
        }
    }
}

@Preview(
    showBackground = true,
    showSystemUi = true,
    name = "Profilo"
)
@Composable
private fun ProfileScreenPreview() {

    val state =
        ProfiloUiState(
            name = "Mario Rossi",
            email = "mario@example.it",
            ruolo = "VIAGGIATORE",
            avatarUrl = null,
            isDarkModeEnabled = false
        )

    MaterialTheme {

        ProfileScreen(
            state = state,

            onBack = {},
            onAddProfilePhoto = {},

            onBookingsClick = {},
            onPaymentsClick = {},
            onFavoritesClick = {},
            onReviewsClick = {},

            onCreaItinerarioClick = {},
            onCreaAttivitaClick = {},
            onLeMieOfferteClick = {},

            onGestioneOfferteAdminClick = {},
            onGestioneUtentiAdminClick = {},

            onToggleDarkMode = {},
            onChangePassword = {},
            onLogout = {}
        )
    }
}