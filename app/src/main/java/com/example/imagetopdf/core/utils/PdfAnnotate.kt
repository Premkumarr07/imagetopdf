package com.example.imagetopdf.core.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileOutputStream

object PdfAnnotate {

    data class Highlight(
        val pageIndex: Int,
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float
    )

    data class Signature(
        val pageIndex: Int,
        val left: Float,
        val top: Float,
        val width: Float,
        val height: Float,
        val bitmap: Bitmap
    )

    fun applyHighlights(
        context: Context,
        inputFile: File,
        outputFile: File,
        highlights: List<Highlight>
    ): File {
        val document = PdfDocument()
        try {
            val fd = ParcelFileDescriptor.open(inputFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(fd)

            for (i in 0 until renderer.pageCount) {
                val page = renderer.openPage(i)
                val pageInfo = PdfDocument.PageInfo.Builder(page.width, page.height, i + 1).create()
                val pdfPage = document.startPage(pageInfo)
                val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                val canvas = pdfPage.canvas
                canvas.drawBitmap(bitmap, 0f, 0f, null)

                val pageHighlights = highlights.filter { it.pageIndex == i }
                val paint = Paint().apply {
                    color = Color.argb(128, 255, 255, 0)
                    style = Paint.Style.FILL
                    isAntiAlias = true
                }
                for (h in pageHighlights) {
                    canvas.drawRect(RectF(h.left, h.top, h.right, h.bottom), paint)
                }

                bitmap.recycle()
                document.finishPage(pdfPage)
                page.close()
            }

            renderer.close()
            fd.close()
            FileOutputStream(outputFile).use { document.writeTo(it) }
            return outputFile
        } catch (e: Exception) {
            com.example.imagetopdf.core.logging.AppLogger.e(e)
            return inputFile
        } finally {
            document.close()
        }
    }

    fun applySignature(
        context: Context,
        inputFile: File,
        outputFile: File,
        signature: Signature
    ): File {
        val document = PdfDocument()
        try {
            val fd = ParcelFileDescriptor.open(inputFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(fd)

            for (i in 0 until renderer.pageCount) {
                val page = renderer.openPage(i)
                val pageInfo = PdfDocument.PageInfo.Builder(page.width, page.height, i + 1).create()
                val pdfPage = document.startPage(pageInfo)
                val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                val canvas = pdfPage.canvas
                canvas.drawBitmap(bitmap, 0f, 0f, null)

                if (i == signature.pageIndex) {
                    val scaledBitmap = Bitmap.createScaledBitmap(
                        signature.bitmap,
                        signature.width.toInt(),
                        signature.height.toInt(),
                        true
                    )
                    canvas.drawBitmap(scaledBitmap, signature.left, signature.top, null)
                    if (scaledBitmap != signature.bitmap) scaledBitmap.recycle()
                }

                bitmap.recycle()
                document.finishPage(pdfPage)
                page.close()
            }

            renderer.close()
            fd.close()
            FileOutputStream(outputFile).use { document.writeTo(it) }
            return outputFile
        } catch (e: Exception) {
            com.example.imagetopdf.core.logging.AppLogger.e(e)
            return inputFile
        } finally {
            document.close()
        }
    }
}
