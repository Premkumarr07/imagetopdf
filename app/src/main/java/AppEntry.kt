//package com.example.imagetopdf.core.navigation
//
//import androidx.compose.runtime.Composable
//import androidx.navigation.compose.NavHost
//import androidx.navigation.compose.composable
//import androidx.navigation.compose.rememberNavController
//import com.example.imagetopdf.core.navigation.AppRoutes
//
//@Composable
//fun AppEntry() {
//    val navController = rememberNavController()
//
//    NavHost(
//        navController = navController,
//        startDestination = AppRoutes.Home.route
//    ) {
//
//        composable(AppRoutes.Home.route) {
//            HomeScreen(navController = navController)
//        }
//
//        // composable(AppRoutes.MyFiles.route) { ... }
//        // composable(AppRoutes.Tools.route) { ... }
//        // composable(AppRoutes.Profile.route) { ... }
//    }
//}