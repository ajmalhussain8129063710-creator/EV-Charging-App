package com.evcharging.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.evcharging.app.ui.support.UserSupportScreen
import com.evcharging.app.ui.profile.ProfileScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var isDarkTheme by remember { mutableStateOf(true) }
            
            EVChargingAppTheme(darkTheme = isDarkTheme) {
                MainApp(authRepository, isDarkTheme) { isDarkTheme = !isDarkTheme }
            }
        }
    }
}

@Composable
fun MainApp(
    authRepository: AuthRepository,
    isDarkTheme: Boolean,
    onThemeChange: () -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val context = androidx.compose.ui.platform.LocalContext.current

    val startDestination = if (authRepository.isUserLoggedIn()) "home" else "login"

    Scaffold(
        topBar = {
            // Global TopBar removed as per specific requests for all main screens
        },
        bottomBar = {
            if (currentRoute in listOf("home", "navigation", "tripplanner", "wallet", "profile")) {
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
            composable("home") { 
                HomeScreen(
                    navController = navController, 
                    isDarkTheme = isDarkTheme,
                    onThemeChange = onThemeChange
                ) 
            }
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
            composable("support") { com.evcharging.app.ui.support.UserSupportScreen(navController) }
            composable("wallet") { com.evcharging.app.ui.wallet.WalletScreen(navController) }
            composable("profile") { com.evcharging.app.ui.profile.ProfileScreen(navController) }
            composable(
                route = "charging/{bookingId}",
                arguments = listOf(androidx.navigation.navArgument("bookingId") { type = androidx.navigation.NavType.StringType })
            ) { backStackEntry ->
                val bookingId = backStackEntry.arguments?.getString("bookingId") ?: ""
                com.evcharging.app.ui.charging.ChargingScreen(navController, bookingId)
            }
        }
    }
}
