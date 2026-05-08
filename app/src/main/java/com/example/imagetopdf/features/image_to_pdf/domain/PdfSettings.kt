package com.example.imagetopdf.features.image_to_pdf.domain

data class PdfSettings(
    val pageSize: String = "A4",
    val quality: String = "High",
    val fileName: String = "document_${System.currentTimeMillis()}",
    val fullBleed: Boolean = false,
    val addPageNumbers: Boolean = false
)