package com.example.imagetopdf.features.image_to_pdf.domain

import android.net.Uri

interface ImageProcessor {
    suspend fun process(uri: Uri, settings: PdfSettings): ProcessedImage
}