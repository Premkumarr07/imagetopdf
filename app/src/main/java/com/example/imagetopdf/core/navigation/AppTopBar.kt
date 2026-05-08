package com.example.imagetopdf.core.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.example.imagetopdf.navigation.NavigationRoutes

private val TopBarDarkBlue = Color(0xFF1E3A8A)
private val TopBarAccentBlue = Color(0xFF1D4ED8)

/**
 * Reusable Top App Bar.
 *
 * Behaviour:
 * - On [NavigationRoutes.Home] → shows title "My Documents" + Settings + Notification icons.
 * - On other main screens  → shows the screen title + Settings icon.
 * - On feature/detail screens → shows a Back arrow + screen title.
 *
 * @param currentRoute  The currently active route string.
 * @param navController Used to pop the back-stack when the back arrow is pressed.
 * @param onSettingsClick Optional override for the settings icon click.
 * @param onSearchClick   Optional override for the search icon click.
 * @param onNotificationClick Optional override for the notification icon click.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    currentRoute: String?,
    navController: NavController,
    onSettingsClick: () -> Unit = { navController.navigate(NavigationRoutes.Profile.route) },
    onSearchClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {}
) {
    val isHome = currentRoute == NavigationRoutes.Home.route
    val isMainRoute = currentRoute in listOf(
        NavigationRoutes.Home.route,
        NavigationRoutes.MyFiles.route,
        NavigationRoutes.Tools.route,
        NavigationRoutes.Profile.route,
        NavigationRoutes.ImageToPdf.route
    )

    val title = when (currentRoute) {
        NavigationRoutes.Home.route      -> "My Documents"
        NavigationRoutes.MyFiles.route   -> "My Files"
        NavigationRoutes.Tools.route     -> "Tools"
        NavigationRoutes.Profile.route   -> "Profile"
        NavigationRoutes.ImageToPdf.route -> "Images → PDF"
        NavigationRoutes.ScanDoc.route   -> "Scan Document"
        NavigationRoutes.Compress.route  -> "Compress PDF"
        NavigationRoutes.Encrypt.route   -> "Encrypt PDF"
        NavigationRoutes.MergePdf.route  -> "Merge PDFs"
        NavigationRoutes.PdfEditor.route -> "PDF Editor"
        NavigationRoutes.Templates.route -> "Templates"
        else                             -> "My Documents"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(TopBarDarkBlue, TopBarAccentBlue)
                )
            )
            .statusBarsPadding()
            .height(64.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp)
            ,horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AnimatedVisibility(
                    visible = !isMainRoute,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                }

                Text(
                    text = title,
                    fontSize = if (isHome) 20.sp else 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(start = if (isMainRoute) 8.dp else 0.dp)
                )
            }

            // Right: action icons
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isHome) {
                    IconButton(onClick = onSearchClick) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "Search",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = onNotificationClick) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = "Notifications",
                            tint = Color.White
                        )
                    }
                }
                IconButton(onClick = onSettingsClick) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = "Settings",
                        tint = Color.White
                    )
                }
            }
        }
    }
}