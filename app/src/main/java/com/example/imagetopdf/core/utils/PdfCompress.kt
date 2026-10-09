package com.example.imagetopdf.core.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileOutputStream

object PdfCompress {

    fun compressPdf(context: Context, inputFile: File, outputFile: File, quality: Int): Boolean {
        val document = PdfDocument()
        try {
            val fd = ParcelFileDescriptor.open(inputFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(fd)

            for (i in 0 until renderer.pageCount) {
                val page = renderer.openPage(i)
                val scale = when (quality) {
                    in 0..30 -> 0.5f
                    in 31..60 -> 0.7f
                    else -> 0.85f
                }
                val width = (page.width * scale).toInt().coerceAtLeast(1)
                val height = (page.height * scale).toInt().coerceAtLeast(1)
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                val pageInfo = PdfDocument.PageInfo.Builder(page.width, page.height, i + 1).create()
                val pdfPage = document.startPage(pageInfo)
                val canvas = pdfPage.canvas
                canvas.drawColor(Color.WHITE)
                val left = ((page.width - width) / 2).toFloat()
                val top = ((page.height - height) / 2).toFloat()
                canvas.drawBitmap(bitmap, left, top, null)
                bitmap.recycle()
                document.finishPage(pdfPage)
            }

            renderer.close()
            fd.close()
            FileOutputStream(outputFile).use { document.writeTo(it) }
            return true
        } catch (e: Exception) {
            com.example.imagetopdf.core.logging.AppLogger.e(e)
            return false
        } finally {
            document.close()
        }
    }
}
