package com.evcharging.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.evcharging.app.data.AuthRepository
import com.evcharging.app.ui.auth.LoginScreen
import com.evcharging.app.ui.auth.SignUpScreen
import com.evcharging.app.ui.components.BottomNavigationBar
import com.evcharging.app.ui.home.HomeScreen
import com.evcharging.app.ui.navigation.NavigationScreen
import com.evcharging.app.ui.theme.EVChargingAppTheme
import com.evcharging.app.ui.tripplanner.TripPlannerScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EVChargingAppTheme {
                MainApp(authRepository)
            }
        }
    }
}

@Composable
fun MainApp(authRepository: AuthRepository) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val context = androidx.compose.ui.platform.LocalContext.current

    val startDestination = if (authRepository.isUserLoggedIn()) "home" else "login"

    Scaffold(
        topBar = {
            if (currentRoute in listOf("home", "navigation", "tripplanner")) {
                com.evcharging.app.ui.components.TopBar(
                    title = "EV Charging App",
                    onLogoutClick = {
                        authRepository.logout()
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onProfileClick = {
                        android.widget.Toast.makeText(context, "Profile Clicked", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    onSettingsClick = {
                        android.widget.Toast.makeText(context, "Settings Clicked", android.widget.Toast.LENGTH_SHORT).show()
                    }
                )
            }
        },
        bottomBar = {
            if (currentRoute in listOf("home", "navigation", "tripplanner")) {
                BottomNavigationBar(navController = navController)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("login") { LoginScreen(navController) }
            composable("signup") { SignUpScreen(navController) }
            composable("home") { HomeScreen(navController) }
            composable("navigation") { NavigationScreen() }
            composable("tripplanner") { TripPlannerScreen(navController) }
            composable(
                route = "booking_detail/{stationName}",
                arguments = listOf(androidx.navigation.navArgument("stationName") { type = androidx.navigation.NavType.StringType })
            ) { backStackEntry ->
                val stationName = backStackEntry.arguments?.getString("stationName") ?: "Unknown Station"
                com.evcharging.app.ui.booking.BookingDetailScreen(navController, stationName)
            }
            composable("service_center") { com.evcharging.app.ui.service.ServiceCenterScreen(navController) }
        }
    }
}
