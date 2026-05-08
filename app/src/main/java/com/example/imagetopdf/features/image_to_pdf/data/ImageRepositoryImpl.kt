// features/image_to_pdf/data/ImageRepositoryImpl.kt
package com.example.imagetopdf.features.image_to_pdf.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.imagetopdf.features.image_to_pdf.domain.ConvertResult
import com.example.imagetopdf.features.image_to_pdf.domain.ImageProcessor
import com.example.imagetopdf.features.image_to_pdf.domain.PdfBuilder
import com.example.imagetopdf.features.image_to_pdf.domain.PdfSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

class ImageRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val imageProcessor: ImageProcessor,
    private val pdfBuilder: PdfBuilder
) : ImageRepository {

    override suspend fun convertImagesToPdf(
        uris: List<Uri>,
        settings: PdfSettings
    ): ConvertResult = withContext(Dispatchers.IO) {
        try {
            // Stage 1: Process images
            val processedImages = uris.mapIndexed { index, uri ->
                imageProcessor.process(uri, settings).copy(pageNumber = index + 1)
            }

            // Stage 2: Build PDF
            val pdfBytes = pdfBuilder.build(processedImages, settings)

            // Stage 3: Save to storage
            val savedUri = savePdfToStorage(pdfBytes, settings.fileName)
                ?: return@withContext ConvertResult.Error("Failed to save PDF to storage")

            ConvertResult.Success(savedUri, settings.fileName)

        } catch (e: Exception) {
            e.printStackTrace()
            ConvertResult.Error(e.message ?: "Conversion failed")
        }
    }

    private fun savePdfToStorage(pdfBytes: ByteArray, fileName: String): Uri? {
        val cacheFile = File(context.cacheDir, "$fileName.pdf")
        FileOutputStream(cacheFile).use { it.write(pdfBytes) }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveUsingMediaStore(cacheFile, fileName)
        } else {
            saveLegacy(cacheFile, fileName)
        }
    }

    private fun saveUsingMediaStore(cacheFile: File, fileName: String): Uri? {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, "$fileName.pdf")
            put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)

        uri?.let {
            resolver.openOutputStream(it)?.use { out ->
                cacheFile.inputStream().copyTo(out)
            }
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(it, values, null, null)
        }
        return uri
    }

    @Suppress("DEPRECATION")
    private fun saveLegacy(cacheFile: File, fileName: String): Uri? {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        downloadsDir.mkdirs()
        val destFile = File(downloadsDir, "$fileName.pdf")
        cacheFile.copyTo(destFile, overwrite = true)
        return FileProvider.getUriForFile(context, "${context.packageName}.provider", destFile)
    }
}