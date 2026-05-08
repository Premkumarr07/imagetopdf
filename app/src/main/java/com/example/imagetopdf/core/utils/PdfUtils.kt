package com.example.imagetopdf.core.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

fun openPdf(path: String, context: Context) {
    val file = File(path)

    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",
        file
    )
3
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/pdf")
        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
    }

    context.startActivity(intent)
}