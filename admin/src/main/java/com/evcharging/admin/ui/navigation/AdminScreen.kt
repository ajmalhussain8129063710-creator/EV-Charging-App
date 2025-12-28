package com.evcharging.admin.ui.navigation

sealed class AdminScreen(val route: String) {
    object Login : AdminScreen("login")
    object Signup : AdminScreen("signup")
    object Home : AdminScreen("home")
    object Wallet : AdminScreen("wallet")
    object Dining : AdminScreen("dining")
    object Promotions : AdminScreen("promotions")
    object Profile : AdminScreen("profile")
    object Settings : AdminScreen("settings")
    object Support : AdminScreen("support")
    object Services : AdminScreen("services")
    object ServiceAnalytics : AdminScreen("service_analytics")
}
