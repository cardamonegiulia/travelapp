package com.example.travelapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.travelapp.ui.navigation.AppDestination
import com.example.travelapp.ui.theme.NavSelectedBlue
import com.example.travelapp.ui.theme.NavUnselected
import com.example.travelapp.ui.theme.SurfaceWhite

/**
 * Bottom navigation condivisa da tutte le sezioni dell'app.
 *
 * La voce evidenziata non e' uno stato a parte: viene dedotta dal back stack,
 * cosi' la barra resta allineata anche quando la navigazione parte da altrove.
 */
@Composable
fun AppBottomBar(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = currentBackStackEntry?.destination

    NavigationBar(
        containerColor = SurfaceWhite,
        tonalElevation = 0.dp,
        modifier = modifier
    ) {
        AppDestination.bottomBarItems.forEach { destination ->
            val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
            NavigationBarItem(
                selected = selected,
                onClick = { navController.navigateToTopLevel(destination) },
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
                            imageVector = if (selected) {
                                destination.selectedIcon
                            } else {
                                destination.unselectedIcon
                            },
                            contentDescription = destination.label,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                label = {
                    Text(
                        text = destination.label,
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

/**
 * Passa a una sezione principale con il comportamento standard della bottom
 * navigation: un solo livello nel back stack e stato di ogni sezione
 * conservato tra un cambio di scheda e l'altro.
 */
private fun NavController.navigateToTopLevel(
    destination: AppDestination
) {
    val currentRoute = currentDestination?.route

    if (currentRoute != destination.route) {
        navigate(destination.route) {
            launchSingleTop = true
        }
    }
}