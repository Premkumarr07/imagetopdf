package com.example.imagetopdf

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.rememberNavController
import com.example.imagetopdf.core.navigation.AppNavigation
import com.example.imagetopdf.core.navigation.AppScaffold
import com.example.imagetopdf.core.preferences.UserPreferences
import com.example.imagetopdf.navigation.NavigationRoutes
import com.example.imagetopdf.ui.theme.ImagetopdfTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    companion object {
        const val PREFS_NAME = "imagetopdf_session"
        const val KEY_LOGGED_IN = "logged_in"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            val prefs = remember { getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
            var settingsRevision by remember { mutableIntStateOf(0) }
            val themeMode = remember(settingsRevision) { UserPreferences.getThemeMode(this) }
            val dynamicColor = remember(settingsRevision) { UserPreferences.getDynamicColor(this) }

            ImagetopdfTheme(themeMode = themeMode, dynamicColor = dynamicColor) {
                val navController = rememberNavController()
                val startDestination = remember {
                    if (prefs.getBoolean(KEY_LOGGED_IN, false)) {
                        NavigationRoutes.Home.route
                    } else {
                        NavigationRoutes.Auth.route
                    }
                }

                AppScaffold(navController = navController) {
                    AppNavigation(
                        navController = navController,
                        startDestination = startDestination,
                        onSettingsChanged = { settingsRevision++ }
                    )
                }
            }
        }
    }
}