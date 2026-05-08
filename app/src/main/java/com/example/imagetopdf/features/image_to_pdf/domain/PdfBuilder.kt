package com.example.imagetopdf.features.image_to_pdf.domain

interface PdfBuilder {
    suspend fun build(images: List<ProcessedImage>, settings: PdfSettings): ByteArray
}
