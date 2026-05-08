// features/image_to_pdf/data/ImageProcessorImpl.kt
package com.example.imagetopdf.features.image_to_pdf.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import com.example.imagetopdf.features.image_to_pdf.domain.ImageProcessor
import com.example.imagetopdf.features.image_to_pdf.domain.PdfSettings
import com.example.imagetopdf.features.image_to_pdf.domain.ProcessedImage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import javax.inject.Inject

class ImageProcessorImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : ImageProcessor {

    companion object {
        private val QUALITY_MAP = mapOf(
            "Low" to 50,
            "Medium" to 75,
            "High" to 95
        )
    }

    override suspend fun process(uri: Uri, settings: PdfSettings): ProcessedImage =
        withContext(Dispatchers.IO) {
            val inputStream: InputStream = context.contentResolver.openInputStream(uri)
                ?: throw IllegalArgumentException("Cannot open URI: $uri")

            val originalBitmap = BitmapFactory.decodeStream(inputStream)
                ?: throw IllegalArgumentException("Cannot decode bitmap from URI: $uri")
            inputStream.close()

            // Auto-orient using EXIF
            val orientedBitmap = fixOrientation(uri, originalBitmap)

            // Compress based on quality setting
            val jpegQuality = QUALITY_MAP[settings.quality] ?: 95
            val compressedBitmap = compressBitmap(orientedBitmap, jpegQuality)

            ProcessedImage(
                bitmap = compressedBitmap,
                originalUri = uri.toString(),
                pageNumber = 0 // Will be set by repository
            )
        }

    private fun fixOrientation(uri: Uri, bitmap: Bitmap): Bitmap {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return bitmap
        val exif = ExifInterface(inputStream)
        inputStream.close()

        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )

        val rotationDegrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }

        return if (rotationDegrees != 0f) {
            val matrix = Matrix().apply { postRotate(rotationDegrees) }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else {
            bitmap
        }
    }

    private fun compressBitmap(bitmap: Bitmap, quality: Int): Bitmap {
        // For High quality, return original; for others, compress via JPEG re-encoding
        if (quality >= 95) return bitmap

        val outputStream = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        val byteArray = outputStream.toByteArray()
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
    }
}