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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.travelapp.ui.theme.BackgroundLavender
import com.example.travelapp.ui.theme.BadgeBlue
import com.example.travelapp.ui.theme.BadgeGrey
import com.example.travelapp.ui.theme.BadgeIndigo
import com.example.travelapp.ui.theme.BadgePurple
import com.example.travelapp.ui.theme.BadgeTeal
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
    onBookingsClick: () -> Unit,
    onPaymentsClick: () -> Unit,
    onReviewsClick: () -> Unit,
    onToggleDarkMode: (Boolean) -> Unit,
    onChangePassword: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    onPhotoMessageShown: () -> Unit = {}
) {
    val activityItems = listOf(
        ActivityItem(
            title = "Le mie prenotazioni",
            icon = Icons.Default.MailOutline,
            iconTint = IconBlue,
            badgeColor = BadgeBlue,
            onClick = onBookingsClick
        ),
        ActivityItem(
            title = "I miei pagamenti",
            icon = ProfileIcons.CreditCard,
            iconTint = IconTeal,
            badgeColor = BadgeTeal,
            onClick = onPaymentsClick
        ),
        ActivityItem(
            title = "Recensioni scritte",
            icon = ProfileIcons.Review,
            iconTint = IconPurple,
            badgeColor = BadgePurple,
            onClick = onReviewsClick
        )
    )

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.photoMessage) {
        state.photoMessage?.let { messaggio ->
            snackbarHostState.showSnackbar(messaggio)
            onPhotoMessageShown()
        }
    }

    val isViaggiatore = !state.ruolo.contains("ORGANIZZATORE", ignoreCase = true) &&
            !state.ruolo.contains("ADMIN", ignoreCase = true)

    Scaffold(
        modifier = modifier,
        containerColor = BackgroundLavender,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { AppTopBar(title = "Profilo", onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Intestazione Utente per tutti i ruoli
            ProfileHeaderCard(
                name = state.name,
                email = state.email,
                avatarUrl = state.avatarUrl,
                isPhotoUploading = state.isPhotoUploading,
                onAddProfilePhoto = onAddProfilePhoto
            )

            // Sezione Attività: VISIBILE SOLO AI VIAGGIATORI
            if (isViaggiatore) {
                SectionTitle(
                    text = "Le mie attività",
                    modifier = Modifier.padding(top = 14.dp)
                )
                activityItems.forEach { item ->
                    ProfileMenuRow(
                        icon = item.icon,
                        title = item.title,
                        iconTint = item.iconTint,
                        badgeColor = item.badgeColor,
                        onClick = item.onClick
                    )
                }
            }

            // Impostazioni per tutti i ruoli
            SectionTitle(
                text = "Impostazioni",
                modifier = Modifier.padding(top = 14.dp)
            )
            ProfileSwitchRow(
                icon = ProfileIcons.DarkMode,
                title = "Tema scuro",
                iconTint = IconIndigo,
                badgeColor = BadgeIndigo,
                checked = state.isDarkModeEnabled,
                onCheckedChange = onToggleDarkMode
            )
            ProfileMenuRow(
                icon = Icons.Default.Lock,
                title = "Cambia password",
                iconTint = IconGrey,
                badgeColor = BadgeGrey,
                onClick = onChangePassword
            )

            LogoutButton(
                onClick = onLogout,
                modifier = Modifier.padding(top = 20.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private data class ActivityItem(
    val title: String,
    val icon: ImageVector,
    val iconTint: Color,
    val badgeColor: Color,
    val onClick: () -> Unit
)

@Preview(showBackground = true, showSystemUi = true, name = "Profilo")
@Composable
private fun ProfileScreenPreview() {
    val state = ProfiloUiState(
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
            onReviewsClick = {},
            onToggleDarkMode = {},
            onChangePassword = {},
            onLogout = {}
        )
    }
}