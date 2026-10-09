package com.example.imagetopdf.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.imagetopdf.MainActivity
import com.example.imagetopdf.core.preferences.UserPreferences
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.imagetopdf.features.auth.ui.LoginScreen
import com.example.imagetopdf.features.home.ui.HomeScreen
import com.example.imagetopdf.features.onboarding.ui.OnboardingScreen
import com.example.imagetopdf.features.image_to_pdf.ui.ImageToPdfScreen
import com.example.imagetopdf.features.myfiles.ui.MyFilesScreen
import com.example.imagetopdf.features.pdf_editor.ui.*
import com.example.imagetopdf.features.profile.ui.ProfileScreen
import com.example.imagetopdf.features.templates.ui.TemplatesScreen
import com.example.imagetopdf.features.tools.ui.ToolsScreen
import com.example.imagetopdf.navigation.NavigationRoutes
import com.example.imagetopdf.navigation.NavigationRoutes.PdfViewerPathCodec

private fun navigateAfterLogin(
    context: android.content.Context,
    navController: NavHostController
) {
    val next = if (UserPreferences.hasCompletedOnboarding(context)) {
        NavigationRoutes.Home.route
    } else {
        NavigationRoutes.Onboarding.route
    }
    navController.navigate(next) {
        popUpTo(NavigationRoutes.Auth.route) { inclusive = true }
    }
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    startDestination: String = NavigationRoutes.Home.route,
    onSettingsChanged: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(NavigationRoutes.Auth.route) {
            val context = LocalContext.current
            LoginScreen(
                onLoginSuccess = { signedInEmail ->
                    UserPreferences.setEmailFromLogin(context, signedInEmail)
                    context.getSharedPreferences(MainActivity.PREFS_NAME, android.content.Context.MODE_PRIVATE)
                        .edit()
                        .putBoolean(MainActivity.KEY_LOGGED_IN, true)
                        .apply()
                    navigateAfterLogin(context, navController)
                }
            )
        }

        composable(NavigationRoutes.Onboarding.route) {
            val context = LocalContext.current
            OnboardingScreen(
                onFinished = {
                    UserPreferences.setOnboardingComplete(context)
                    navController.navigate(NavigationRoutes.Home.route) {
                        popUpTo(NavigationRoutes.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

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
            ProfileScreen(
                navController = navController,
                onSettingsChanged = onSettingsChanged
            )
        }

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

        composable(NavigationRoutes.SplitPdf.route) {
            SplitPdfScreen(navController = navController)
        }

        composable(NavigationRoutes.RearrangePdf.route) {
            RearrangePdfScreen(navController = navController)
        }

        composable(NavigationRoutes.PdfToJpg.route) {
            PdfToJpgScreen(navController = navController)
        }

        composable(NavigationRoutes.PdfToWord.route) {
            PdfToWordScreen(navController = navController)
        }

        composable(
            route = NavigationRoutes.PdfViewer.route,
            arguments = listOf(
                navArgument("pdfPath") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { entry ->
            val rawPath = entry.arguments?.getString("pdfPath")?.takeIf { it.isNotEmpty() }
            val path = rawPath?.let(PdfViewerPathCodec::decode)
            PdfViewerScreen(navController = navController, initialPath = path)
        }

        composable(NavigationRoutes.HighlightPdf.route) {
            HighlightPdfScreen(navController = navController)
        }

        composable(NavigationRoutes.EsignPdf.route) {
            EsignPdfScreen(navController = navController)
        }
    }
}
