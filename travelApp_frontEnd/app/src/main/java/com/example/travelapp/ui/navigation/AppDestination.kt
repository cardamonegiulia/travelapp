package com.example.travelapp.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Person
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.travelapp.ui.components.ProfileIcons

/**
 * Destinazioni della navigazione principale.
 *
 * Le route vivono solo qui: chi naviga passa una [AppDestination], mai una
 * stringa scritta a mano.
 */
sealed class AppDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    // Explore e Bookings usano lo stesso vettore nei due stati: ProfileIcons
    // non ha (ancora) le varianti outlined, che arriverebbero solo aggiungendo
    // material-icons-extended.
    data object Explore : AppDestination(
        route = "explore",
        label = "Explore",
        selectedIcon = ProfileIcons.Explore,
        unselectedIcon = ProfileIcons.Explore
    )

    data object Bookings : AppDestination(
        route = "bookings",
        label = "Bookings",
        selectedIcon = ProfileIcons.Ticket,
        unselectedIcon = ProfileIcons.Ticket
    )

    data object Favorites : AppDestination(
        route = "favorites",
        label = "Favorites",
        selectedIcon = Icons.Filled.Favorite,
        unselectedIcon = Icons.Outlined.Favorite
    )

    data object Profile : AppDestination(
        route = "profile",
        label = "Profilo",
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person
    )

    data object Payments : AppDestination(
        route = "payments",
        label = "Pagamenti",
        selectedIcon = ProfileIcons.CreditCard,
        unselectedIcon = ProfileIcons.CreditCard
    )

    data object BookingStep1 : AppDestination(
        route = "booking_step_1",
        label = "Prenotazione",
        selectedIcon = ProfileIcons.Ticket,
        unselectedIcon = ProfileIcons.Ticket
    )

    data object BookingStep2 : AppDestination(
        route = "booking_step_2",
        label = "Pagamento",
        selectedIcon = ProfileIcons.CreditCard,
        unselectedIcon = ProfileIcons.CreditCard
    )

    data object BookingSuccess : AppDestination(
        route = "booking_success",
        label = "Successo",
        selectedIcon = ProfileIcons.Ticket,
        unselectedIcon = ProfileIcons.Ticket
    )

    companion object {
        /** Voci della bottom navigation, nell'ordine in cui compaiono. */
        val bottomBarItems: List<AppDestination> = listOf(Explore, Bookings, Favorites, Profile)

        /** Schermata mostrata all'avvio dell'app. */
        val start: AppDestination = Explore
    }
}
