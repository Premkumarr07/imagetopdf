package com.example.imagetopdf

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.example.imagetopdf.core.navigation.AppNavigation
import com.example.imagetopdf.core.navigation.AppScaffold
import com.example.imagetopdf.ui.theme.ImagetopdfTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            ImagetopdfTheme {
                val navController = rememberNavController()

                AppScaffold(navController = navController) {
                    AppNavigation(navController = navController)
                }
            }
        }
    }
}