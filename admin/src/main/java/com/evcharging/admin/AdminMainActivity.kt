package com.evcharging.admin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.evcharging.admin.ui.navigation.AdminNavGraph
import com.evcharging.admin.ui.theme.AdminTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AdminMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Use the theme from the app module if accessible, or just MaterialTheme for now.
            // Since we copied resources, we can try to use the theme if we had a Theme.kt file.
            // For now, standard MaterialTheme is fine, or we can create a basic theme wrapper.
                AdminTheme {
                    val navController = rememberNavController()
                    AdminNavGraph(navController = navController)
                }
        }
    }
}
