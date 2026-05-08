package com.example.imagetopdf.features.image_to_pdf.domain

import android.graphics.Bitmap

data class ProcessedImage(
    val bitmap: Bitmap,
    val originalUri: String,
    val pageNumber: Int
)