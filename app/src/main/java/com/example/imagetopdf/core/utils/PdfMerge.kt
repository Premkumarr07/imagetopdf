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

object PdfMerge {

    fun mergePdfs(context: Context, inputFiles: List<File>, outputFile: File): Boolean {
        val document = PdfDocument()
        try {
            var pageIndex = 1
            for (file in inputFiles) {
                val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(fd)
                for (i in 0 until renderer.pageCount) {
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
                renderer.close()
                fd.close()
            }
            FileOutputStream(outputFile).use { document.writeTo(it) }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        } finally {
            document.close()
        }
    }

    fun extractPages(context: Context, inputFile: File, outputFile: File, pageIndices: List<Int>): Boolean {
        val document = PdfDocument()
        try {
            val fd = ParcelFileDescriptor.open(inputFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(fd)
            var pageIndex = 1
            for (idx in pageIndices) {
                if (idx < 0 || idx >= renderer.pageCount) continue
                val page = renderer.openPage(idx)
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
            renderer.close()
            fd.close()
            FileOutputStream(outputFile).use { document.writeTo(it) }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        } finally {
            document.close()
        }
    }

    fun getPageCount(file: File): Int {
        return try {
            val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(fd)
            val count = renderer.pageCount
            renderer.close()
            fd.close()
            count
        } catch (e: Exception) {
            0
        }
    }

    fun renderPageToBitmap(file: File, pageIndex: Int, scale: Float = 1f): Bitmap? {
        return try {
            val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(fd)
            if (pageIndex >= renderer.pageCount) {
                renderer.close()
                fd.close()
                return null
            }
            val page = renderer.openPage(pageIndex)
            val width = (page.width * scale).toInt()
            val height = (page.height * scale).toInt()
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            renderer.close()
            fd.close()
            bitmap
        } catch (e: Exception) {
            null
        }
    }
}
