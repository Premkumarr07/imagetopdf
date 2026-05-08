package com.example.imagetopdf.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.imagetopdf.features.home.ui.HomeScreen
import com.example.imagetopdf.features.image_to_pdf.ui.ImageToPdfScreen
import com.example.imagetopdf.features.myfiles.ui.MyFilesScreen
import com.example.imagetopdf.features.pdf_editor.ui.CompressScreen
import com.example.imagetopdf.features.pdf_editor.ui.EncryptScreen
import com.example.imagetopdf.features.pdf_editor.ui.MergePdfScreen
import com.example.imagetopdf.features.pdf_editor.ui.PdfEditorScreen
import com.example.imagetopdf.features.pdf_editor.ui.ScanDocScreen
import com.example.imagetopdf.features.profile.ui.ProfileScreen
import com.example.imagetopdf.features.templates.ui.TemplatesScreen
import com.example.imagetopdf.features.tools.ui.ToolsScreen
import com.example.imagetopdf.navigation.NavigationRoutes

/**
 * Central NavHost that maps every [NavigationRoutes] to its screen composable.
 *
 * Add new routes here as features grow. The scaffold (top bar + bottom nav)
 * is handled by [AppScaffold] — screens themselves stay clean.
 *
 * @param navController The app-level [NavHostController].
 * @param modifier      Optional modifier forwarded to [NavHost].
 */
@Composable
fun AppNavigation(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController    = navController,
        startDestination = NavigationRoutes.Home.route,
        modifier         = modifier
    ) {
        // ── Main tabs ────────────────────────────────────────────────────────
        composable(NavigationRoutes.Home.route) {
            HomeScreen(navController = navController)
        }

        composable(NavigationRoutes.MyFiles.route) {
            MyFilesScreen(navController = navController)
        }

        composable(NavigationRoutes.Tools.route) {
            ToolsScreen(navController = navController)
        }

        composable(NavigationRoutes.Profile.route) {
            ProfileScreen(navController = navController)
        }

        // ── Feature screens ──────────────────────────────────────────────────
        composable(NavigationRoutes.ImageToPdf.route) {
            ImageToPdfScreen(navController = navController)
        }

        composable(NavigationRoutes.ScanDoc.route) {
            ScanDocScreen(navController = navController)
        }

        composable(NavigationRoutes.Compress.route) {
            CompressScreen(navController = navController)
        }

        composable(NavigationRoutes.Encrypt.route) {
            EncryptScreen(navController = navController)
        }

        composable(NavigationRoutes.MergePdf.route) {
            MergePdfScreen(navController = navController)
        }

        composable(NavigationRoutes.PdfEditor.route) {
            PdfEditorScreen(navController = navController)
        }

        composable(NavigationRoutes.Templates.route) {
            TemplatesScreen(navController = navController)
        }
    }
}