package com.example.imagetopdf.core.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileOutputStream

object PdfSplit {

    fun splitPdf(context: Context, inputFile: File, outputDir: File, ranges: List<IntRange>): List<File> {
        val results = mutableListOf<File>()
        try {
            val fd = ParcelFileDescriptor.open(inputFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(fd)

            for ((index, range) in ranges.withIndex()) {
                val document = PdfDocument()
                try {
                    var pageIndex = 1
                    for (i in range) {
                        if (i < 0 || i >= renderer.pageCount) continue
                        val page = renderer.openPage(i)
                        val pageInfo = PdfDocument.PageInfo.Builder(page.width, page.height, pageIndex).create()
                        val pdfPage = document.startPage(pageInfo)
                        val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                        bitmap.eraseColor(Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        pdfPage.canvas.drawBitmap(bitmap, 0f, 0f, null)
                        bitmap.recycle()
                        document.finishPage(pdfPage)
                        page.close()
                        pageIndex++
                    }
                    val outputFile = File(outputDir, "split_${index + 1}.pdf")
                    FileOutputStream(outputFile).use { document.writeTo(it) }
                    results.add(outputFile)
                } finally {
                    document.close()
                }
            }

            renderer.close()
            fd.close()
        } catch (e: Exception) {
            com.example.imagetopdf.core.logging.AppLogger.e(e)
        }
        return results
    }

    fun extractPage(context: Context, inputFile: File, outputFile: File, pageIndex: Int): Boolean {
        val document = PdfDocument()
        try {
            val fd = ParcelFileDescriptor.open(inputFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(fd)
            if (pageIndex < 0 || pageIndex >= renderer.pageCount) {
                renderer.close()
                fd.close()
                return false
            }
            val page = renderer.openPage(pageIndex)
            val pageInfo = PdfDocument.PageInfo.Builder(page.width, page.height, 1).create()
            val pdfPage = document.startPage(pageInfo)
            val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            pdfPage.canvas.drawBitmap(bitmap, 0f, 0f, null)
            bitmap.recycle()
            document.finishPage(pdfPage)
            page.close()
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
