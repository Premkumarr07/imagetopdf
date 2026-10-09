package com.example.imagetopdf.core.utils

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.imagetopdf.core.preferences.UserPreferences
import java.io.File
import java.io.FileOutputStream

object PdfSaveHelper {

    data class SaveResult(
        val file: File,
        val shareUri: Uri
    )

    fun savePdfBytes(context: Context, pdfBytes: ByteArray, baseName: String): SaveResult? {
        return try {
            val cleanName = baseName.removeSuffix(".pdf").ifBlank { "document" }
            val appFile = writeToAppFolder(context, pdfBytes, cleanName)
            if (UserPreferences.saveToDownloads(context)) {
                copyToDownloads(context, pdfBytes, cleanName)
            }
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                appFile
            )
            SaveResult(appFile, uri)
        } catch (e: Exception) {
            com.example.imagetopdf.core.logging.AppLogger.e(e)
            null
        }
    }

    fun savePdfFile(context: Context, sourceFile: File, baseName: String): SaveResult? {
        val bytes = sourceFile.readBytes()
        return savePdfBytes(context, bytes, baseName)
    }

    /** Copies the file into Downloads/PDFMaker (always, regardless of settings). */
    fun exportToDownloads(context: Context, sourceFile: File, baseName: String = sourceFile.nameWithoutExtension): Boolean {
        return try {
            val cleanName = baseName.removeSuffix(".pdf").ifBlank { "document" }
            copyToDownloads(context, sourceFile.readBytes(), cleanName)
            true
        } catch (e: Exception) {
            com.example.imagetopdf.core.logging.AppLogger.e(e)
            false
        }
    }

    private fun writeToAppFolder(context: Context, bytes: ByteArray, baseName: String): File {
        val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
        val folder = File(baseDir, "PDFMaker")
        folder.mkdirs()
        var file = File(folder, "$baseName.pdf")
        var counter = 1
        while (file.exists()) {
            file = File(folder, "${baseName}_$counter.pdf")
            counter++
        }
        FileOutputStream(file).use { it.write(bytes) }
        return file
    }

    private fun copyToDownloads(context: Context, pdfBytes: ByteArray, fileName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, "$fileName.pdf")
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/PDFMaker")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            uri?.let {
                resolver.openOutputStream(it)?.use { out -> out.write(pdfBytes) }
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(it, values, null, null)
            }
        } else {
            @Suppress("DEPRECATION")
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val appDir = File(downloadsDir, "PDFMaker").apply { mkdirs() }
            FileOutputStream(File(appDir, "$fileName.pdf")).use { it.write(pdfBytes) }
        }
    }
}
