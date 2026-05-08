package com.example.imagetopdf.core.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.imagetopdf.R
import com.example.imagetopdf.navigation.NavigationRoutes

private val NavBarBlue = Color(0xFF1E3A8A)
private val NavBarTeal = Color(0xFF09D7C4)
private val NavBarGray = Color(0xFF94A3B8)
data class BottomNavItem(
    val label: String,
    val route: String,
    val icon: Int
)

private val bottomNavItems = listOf(
    BottomNavItem(
        label = "Home",
        route = NavigationRoutes.Home.route,
        icon = R.drawable.home
    ),
    BottomNavItem(
        label = "My Files",
        route = NavigationRoutes.MyFiles.route,
        icon = R.drawable.folder
    ),
    BottomNavItem(
        label = "Tools",
        route = NavigationRoutes.Tools.route,
        icon = R.drawable.tools
    ),
    BottomNavItem(
        label = "Profile",
        route = NavigationRoutes.Profile.route,
        icon = R.drawable.user


    )
)

@Composable
fun AppNavigationBar(navController: NavController) {

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {

        bottomNavItems.forEach { item ->

            val selected = currentRoute == item.route

            val iconColor by animateColorAsState(
                targetValue = if (selected) NavBarBlue else NavBarGray,
                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                label = ""
            )

            val labelColor by animateColorAsState(
                targetValue = if (selected) NavBarBlue else NavBarGray,
                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                label = ""
            )

            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            popUpTo(NavigationRoutes.Home.route) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },

                icon = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {

                        Icon(
                            painter = painterResource(id = item.icon),
                            contentDescription = item.label,
                            tint = iconColor,
                            // use Color.Unspecified if PNG color issue
                            modifier = Modifier.size(24.dp)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Active Dot Indicator
                        Box(
                            modifier = Modifier
                                .size(if (selected) 6.dp else 0.dp)
                                .clip(CircleShape)
                                .background(NavBarTeal)
                        )
                    }
                },

                label = {
                    Text(
                        text = item.label,
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = labelColor
                    )
                },

                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}