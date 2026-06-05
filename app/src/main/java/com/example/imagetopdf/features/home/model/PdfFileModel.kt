package com.example.imagetopdf.features.home.model

import android.net.Uri

data class PdfFileModel(
    val name: String,
    val path: String,
    val sizeLabel: String,
    val dateLabel: String,
    val lastModified: Long,
    val sizeBytes: Long,
    val uri: Uri? = null
)