package com.example.imagetopdf.features.image_to_pdf.data

import android.net.Uri
import com.example.imagetopdf.features.image_to_pdf.domain.ConvertResult
import com.example.imagetopdf.features.image_to_pdf.domain.PdfSettings

interface ImageRepository {
    suspend fun convertImagesToPdf(
        uris: List<Uri>,
        settings: PdfSettings
    ): ConvertResult
}