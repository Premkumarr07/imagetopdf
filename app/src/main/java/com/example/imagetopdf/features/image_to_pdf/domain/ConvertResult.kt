package com.example.imagetopdf.features.image_to_pdf.domain

import android.net.Uri

sealed class ConvertResult {
    data object Idle : ConvertResult()
    data class Converting(val progress: Float) : ConvertResult()
    data class Success(val uri: Uri, val fileName: String) : ConvertResult()
    data class Error(val message: String) : ConvertResult()
}
