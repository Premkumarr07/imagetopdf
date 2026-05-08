package com.example.imagetopdf.navigation

sealed class NavigationRoutes(val route: String) {

    // Auth
    object Auth : NavigationRoutes("auth")

    // Main
    object Home : NavigationRoutes("home")
    object MyFiles : NavigationRoutes("my_files")
    object Tools : NavigationRoutes("tools")
    object Profile : NavigationRoutes("profile")

    // Features
    object ImageToPdf : NavigationRoutes("image_to_pdf")
    object ScanDoc : NavigationRoutes("scan_doc")
    object Compress : NavigationRoutes("compress")
    object Encrypt : NavigationRoutes("encrypt")
    object MergePdf : NavigationRoutes("merge_pdf")
    object PdfEditor : NavigationRoutes("pdf_editor")
    object Templates : NavigationRoutes("templates")
}

/** Routes that show the bottom navigation bar */
val bottomNavRoutes = listOf(
    NavigationRoutes.Home.route,
    NavigationRoutes.MyFiles.route,
    NavigationRoutes.Tools.route,
    NavigationRoutes.Profile.route,
    NavigationRoutes.ImageToPdf.route,

    )

/** Routes that show the top app bar */
val topBarRoutes = listOf(
    NavigationRoutes.Home.route,
    NavigationRoutes.MyFiles.route,
    NavigationRoutes.Tools.route,
    NavigationRoutes.Profile.route,
    NavigationRoutes.ImageToPdf.route,


    )