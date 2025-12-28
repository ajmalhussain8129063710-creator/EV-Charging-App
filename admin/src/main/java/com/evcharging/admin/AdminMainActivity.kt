package com.evcharging.admin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.evcharging.admin.ui.navigation.AdminNavGraph
import com.evcharging.admin.ui.theme.AdminTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AdminMainActivity : ComponentActivity() {
    
    @javax.inject.Inject
    lateinit var themeStore: com.evcharging.admin.data.ThemeStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val isDarkTheme by themeStore.isDarkTheme.collectAsState(initial = true)
            AdminTheme(darkTheme = isDarkTheme) {
                val navController = rememberNavController()
                AdminNavGraph(navController = navController)
            }
        }
    }
}
