// features/image_to_pdf/data/PdfBuilderImpl.kt
package com.example.imagetopdf.features.image_to_pdf.data

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import com.example.imagetopdf.features.image_to_pdf.domain.PdfBuilder
import com.example.imagetopdf.features.image_to_pdf.domain.PdfSettings
import com.example.imagetopdf.features.image_to_pdf.domain.ProcessedImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject

class PdfBuilderImpl @Inject constructor() : PdfBuilder {

    companion object {
        private val PAGE_SIZES = mapOf(
            "A4" to Pair(595, 842),
            "Letter" to Pair(612, 792)
        )
        private val QUALITY_MAP = mapOf(
            "Low" to 50,
            "Medium" to 75,
            "High" to 95
        )
    }

    override suspend fun build(images: List<ProcessedImage>, settings: PdfSettings): ByteArray =
        withContext(Dispatchers.Default) {
            val pdfDocument = PdfDocument()

            try {
                images.forEach { processedImage ->
                    val bitmap = processedImage.bitmap
                    val (pageW, pageH) = when (settings.pageSize) {
                        "Auto" -> Pair(bitmap.width, bitmap.height)
                        else -> PAGE_SIZES[settings.pageSize] ?: Pair(595, 842)
                    }

                    // Scale bitmap to fit page
                    val scaledBitmap = scaleBitmapToFit(bitmap, pageW, pageH)

                    val pageInfo = PdfDocument.PageInfo.Builder(pageW, pageH, processedImage.pageNumber).create()
                    val page = pdfDocument.startPage(pageInfo)
                    val canvas: Canvas = page.canvas

                    // White background
                    canvas.drawColor(android.graphics.Color.WHITE)

                    // Center image
                    val paint = Paint().apply { isAntiAlias = true }
                    val left = ((pageW - scaledBitmap.width) / 2).toFloat()
                    val top = ((pageH - scaledBitmap.height) / 2).toFloat()
                    canvas.drawBitmap(scaledBitmap, left, top, paint)

                    // Page numbers (Pro feature)
                    if (settings.addPageNumbers) {
                        drawPageNumber(canvas, processedImage.pageNumber, images.size, pageW, pageH)
                    }

                    pdfDocument.finishPage(page)
                }

                ByteArrayOutputStream().use { output ->
                    pdfDocument.writeTo(output)
                    output.toByteArray()
                }
            } finally {
                pdfDocument.close()
            }
        }

    private fun scaleBitmapToFit(bitmap: Bitmap, pageW: Int, pageH: Int): Bitmap {
        val bitmapRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
        val pageRatio = pageW.toFloat() / pageH.toFloat()

        val (drawW, drawH) = if (bitmapRatio > pageRatio) {
            val w = pageW
            val h = (pageW / bitmapRatio).toInt()
            Pair(w, h)
        } else {
            val h = pageH
            val w = (pageH * bitmapRatio).toInt()
            Pair(w, h)
        }

        return Bitmap.createScaledBitmap(bitmap, drawW, drawH, true)
    }

    private fun drawPageNumber(canvas: Canvas, current: Int, total: Int, pageW: Int, pageH: Int) {
        val paint = Paint().apply {
            textSize = 24f
            color = android.graphics.Color.GRAY
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("$current / $total", pageW - 40f, pageH - 20f, paint)
    }
}