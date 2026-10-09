package com.example.imagetopdf.navigation

import android.net.Uri

sealed class NavigationRoutes(val route: String) {

    object Auth : NavigationRoutes("auth")

    object Home : NavigationRoutes("home")
    object MyFiles : NavigationRoutes("my_files")
    object Tools : NavigationRoutes("tools")
    object Profile : NavigationRoutes("profile")

    object ImageToPdf : NavigationRoutes("image_to_pdf")
    object ScanDoc : NavigationRoutes("scan_doc")
    object Compress : NavigationRoutes("compress")
    object Encrypt : NavigationRoutes("encrypt")
    object MergePdf : NavigationRoutes("merge_pdf")
    object PdfEditor : NavigationRoutes("pdf_editor")
    object Templates : NavigationRoutes("templates")
    object SplitPdf : NavigationRoutes("split_pdf")
    object RearrangePdf : NavigationRoutes("rearrange_pdf")
    object PdfToJpg : NavigationRoutes("pdf_to_jpg")
    object PdfToWord : NavigationRoutes("pdf_to_word")
    object PdfViewer : NavigationRoutes("pdf_viewer?pdfPath={pdfPath}") {
        fun open(path: String? = null): String {
            if (path.isNullOrBlank()) return "pdf_viewer?pdfPath="
            return "pdf_viewer?pdfPath=${Uri.encode(path)}"
        }
    }
    object HighlightPdf : NavigationRoutes("highlight_pdf")
    object EsignPdf : NavigationRoutes("esign_pdf")
}

val bottomNavRoutes = listOf(
    NavigationRoutes.Home.route,
    NavigationRoutes.MyFiles.route,
    NavigationRoutes.Tools.route,
    NavigationRoutes.Profile.route,
    NavigationRoutes.ImageToPdf.route,
)

val topBarRoutes = listOf(
    NavigationRoutes.Home.route,
    NavigationRoutes.MyFiles.route,
    NavigationRoutes.Tools.route,
    NavigationRoutes.Profile.route,
    NavigationRoutes.ImageToPdf.route,
)
