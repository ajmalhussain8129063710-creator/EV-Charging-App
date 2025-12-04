package com.evcharging.admin.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.evcharging.admin.ui.auth.LoginScreen
import com.evcharging.admin.ui.auth.SignupScreen
import com.evcharging.admin.ui.home.AdminHomeScreen

@Composable
fun AdminNavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = AdminScreen.Login.route) {
        composable(AdminScreen.Login.route) {
            LoginScreen(navController = navController)
        }
        composable(AdminScreen.Signup.route) {
            SignupScreen(navController = navController)
        }
        composable(AdminScreen.Home.route) {
            AdminHomeScreen(rootNavController = navController)
        }
    }
}
