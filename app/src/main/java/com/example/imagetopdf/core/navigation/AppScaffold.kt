package com.example.imagetopdf.core.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.imagetopdf.core.ui.AppSnackbarHost
import com.example.imagetopdf.core.ui.LocalAppSnackbarHostState
import com.example.imagetopdf.core.ui.rememberAppSnackbarHostState
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.imagetopdf.navigation.NavigationRoutes
import com.example.imagetopdf.navigation.bottomNavRoutes
import com.example.imagetopdf.navigation.topBarRoutes

/**
 * Root Scaffold that wires [AppTopBar] and [AppNavigationBar] together.
 *
 * - Top bar is shown only on routes listed in [topBarRoutes].
 * - Bottom nav is shown only on routes listed in [bottomNavRoutes].
 * - Both animate in/out with slide transitions.
 *
 * Usage – wrap your NavHost with this composable inside [MainActivity]:
 * ```kotlin
 * AppScaffold(navController = navController) {
 *     AppNavigation(navController = navController)
 * }
 * ```
 *
 * @param navController The single app [NavController].
 * @param content       The [NavHost] or other content to display inside the scaffold.
 */
@Composable
fun AppScaffold(
    navController: NavController,
    content: @Composable () -> Unit
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val snackbarHostState = rememberAppSnackbarHostState()

    if (currentRoute == NavigationRoutes.Auth.route || currentRoute == NavigationRoutes.Onboarding.route) {
        CompositionLocalProvider(LocalAppSnackbarHostState provides snackbarHostState) {
            Box(modifier = Modifier.fillMaxSize()) {
                content()
                AppSnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
        return
    }

    val showTopBar    = currentRoute in topBarRoutes
    val showBottomNav = currentRoute in bottomNavRoutes

    CompositionLocalProvider(LocalAppSnackbarHostState provides snackbarHostState) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { AppSnackbarHost(snackbarHostState) },
        topBar = {
            AnimatedVisibility(
                visible = showTopBar,
                enter = slideInVertically(initialOffsetY = { -it }),
                exit  = slideOutVertically(targetOffsetY  = { -it })
            ) {
                AppTopBar(
                    currentRoute  = currentRoute,
                    navController = navController
                )
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomNav,
                enter = slideInVertically(initialOffsetY = { it }),
                exit  = slideOutVertically(targetOffsetY  = { it })
            ) {
                AppNavigationBar(navController = navController)
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            content()
        }
    }
    }
}