package com.example.travelapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelapp.ui.components.LogoutButton
import com.example.travelapp.ui.components.ProfileHeaderCard
import com.example.travelapp.ui.components.ProfileIcons
import com.example.travelapp.ui.components.ProfileMenuRow
import com.example.travelapp.ui.components.ProfileSwitchRow
import com.example.travelapp.ui.components.SectionTitle
import com.example.travelapp.ui.theme.BackgroundLavender
import com.example.travelapp.ui.theme.BadgeBlue
import com.example.travelapp.ui.theme.BadgeGrey
import com.example.travelapp.ui.theme.BadgeIndigo
import com.example.travelapp.ui.theme.BadgePink
import com.example.travelapp.ui.theme.BadgePurple
import com.example.travelapp.ui.theme.BadgeTeal
import com.example.travelapp.ui.theme.IconBlue
import com.example.travelapp.ui.theme.IconGrey
import com.example.travelapp.ui.theme.IconIndigo
import com.example.travelapp.ui.theme.IconPink
import com.example.travelapp.ui.theme.IconPurple
import com.example.travelapp.ui.theme.IconTeal
import com.example.travelapp.ui.theme.NavSelectedBlue
import com.example.travelapp.ui.theme.NavUnselected
import com.example.travelapp.ui.theme.SurfaceWhite
import com.example.travelapp.ui.theme.TextPrimary

/** Sezioni raggiungibili dalla bottom navigation. */
enum class ProfileTab(val label: String, val icon: ImageVector) {
    EXPLORE("Explore", ProfileIcons.Explore),
    BOOKINGS("Bookings", ProfileIcons.Ticket),
    FAVORITES("Favorites", Icons.Default.Favorite),
    PROFILE("Profilo", Icons.Default.Person)
}

/** Stato osservabile della schermata profilo. */
data class ProfileUiState(
    val name: String = "",
    val email: String = "",
    val avatarUrl: String? = null,
    val isDarkModeEnabled: Boolean = false,
    val selectedTab: ProfileTab = ProfileTab.PROFILE
)

/**
 * Schermata "Profilo": intestazione utente, scorciatoie alle attività,
 * impostazioni e logout.
 *
 * La schermata è puramente presentazionale: riceve [state] e notifica le
 * interazioni tramite le lambda, senza conoscere navigazione o ViewModel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    state: ProfileUiState,
    onBack: () -> Unit,
    onEditProfile: () -> Unit,
    onBookingsClick: () -> Unit,
    onPaymentsClick: () -> Unit,
    onFavoritesClick: () -> Unit,
    onReviewsClick: () -> Unit,
    onToggleDarkMode: (Boolean) -> Unit,
    onChangePassword: () -> Unit,
    onLogout: () -> Unit,
    onNavigate: (ProfileTab) -> Unit,
    modifier: Modifier = Modifier
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
            title = "Preferiti",
            icon = Icons.Default.Favorite,
            iconTint = IconPink,
            badgeColor = BadgePink,
            onClick = onFavoritesClick
        ),
        ActivityItem(
            title = "Recensioni scritte",
            icon = ProfileIcons.Review,
            iconTint = IconPurple,
            badgeColor = BadgePurple,
            onClick = onReviewsClick
        )
    )

    Scaffold(
        modifier = modifier,
        containerColor = BackgroundLavender,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Profilo",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Indietro",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        bottomBar = {
            ProfileBottomBar(
                selectedTab = state.selectedTab,
                onNavigate = onNavigate
            )
        }
    ) { innerPadding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            ProfileHeaderCard(
                name = state.name,
                email = state.email,
                avatarUrl = state.avatarUrl,
                onEditProfile = onEditProfile
            )

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
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

/** Voce della sezione "Le mie attività". */
private data class ActivityItem(
    val title: String,
    val icon: ImageVector,
    val iconTint: Color,
    val badgeColor: Color,
    val onClick: () -> Unit
)

@Composable
private fun ProfileBottomBar(
    selectedTab: ProfileTab,
    onNavigate: (ProfileTab) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        containerColor = SurfaceWhite,
        tonalElevation = 0.dp,
        modifier = modifier
    ) {
        ProfileTab.entries.forEach { tab ->
            val selected = tab == selectedTab
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(tab) },
                icon = {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                color = if (selected) NavSelectedBlue else Color.Transparent,
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                label = {
                    Text(
                        text = tab.label,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    // Il badge circolare è già disegnato dall'icona: l'indicatore
                    // a pillola di Material3 va reso invisibile.
                    indicatorColor = Color.Transparent,
                    selectedIconColor = Color.White,
                    selectedTextColor = NavSelectedBlue,
                    unselectedIconColor = NavUnselected,
                    unselectedTextColor = NavUnselected
                )
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Profilo")
@Composable
private fun ProfileScreenPreview() {
    var state by remember {
        mutableStateOf(
            ProfileUiState(
                name = "Mario Rossi",
                email = "mario@example.it",
                avatarUrl = null,
                isDarkModeEnabled = false,
                selectedTab = ProfileTab.PROFILE
            )
        )
    }

    MaterialTheme {
        ProfileScreen(
            state = state,
            onBack = {},
            onEditProfile = {},
            onBookingsClick = {},
            onPaymentsClick = {},
            onFavoritesClick = {},
            onReviewsClick = {},
            onToggleDarkMode = { enabled -> state = state.copy(isDarkModeEnabled = enabled) },
            onChangePassword = {},
            onLogout = {},
            onNavigate = { tab -> state = state.copy(selectedTab = tab) }
        )
    }
}
