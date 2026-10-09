package com.example.imagetopdf.core.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileOutputStream

object PdfToWord {

    fun convertPdfToDocx(context: Context, inputFile: File, outputFile: File): Boolean {
        return try {
            val fd = ParcelFileDescriptor.open(inputFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(fd)

            val html = buildString {
                append("<html><head><style>")
                append("body { font-family: Arial, sans-serif; margin: 40px; line-height: 1.6; }")
                append("h1 { color: #1E3A8A; font-size: 24pt; }")
                append("h2 { color: #1E3A8A; font-size: 18pt; }")
                append("p { font-size: 12pt; color: #333; }")
                append(".page-break { page-break-after: always; }")
                append("</style></head><body>")

                for (i in 0 until renderer.pageCount) {
                    if (i > 0) append("<div class='page-break'></div>")
                    append("<h2>Page ${i + 1}</h2>")

                    val page = renderer.openPage(i)
                    val width = page.width
                    val height = page.height
                    val scale = 2f
                    val bitmap = Bitmap.createBitmap(
                        (width * scale).toInt(),
                        (height * scale).toInt(),
                        Bitmap.Config.ARGB_8888
                    )
                    bitmap.eraseColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()

                    val text = extractTextFromBitmap(bitmap)
                    if (text.isNotBlank()) {
                        val paragraphs = text.split("\n").filter { it.isNotBlank() }
                        for (para in paragraphs) {
                            append("<p>${escapeHtml(para)}</p>")
                        }
                    } else {
                        append("<p>[This page contains an image or non-extractable content]</p>")
                    }

                    bitmap.recycle()
                }

                append("</body></html>")
            }

            fd.close()

            outputFile.writeText(html)
            true
        } catch (e: Exception) {
            com.example.imagetopdf.core.logging.AppLogger.e(e)
            false
        }
    }

    private fun extractTextFromBitmap(bitmap: Bitmap): String {
        return try {
            val scaled = Bitmap.createScaledBitmap(bitmap, bitmap.width / 2, bitmap.height / 2, true)
            val text = simpleTextExtraction(scaled)
            if (scaled != bitmap) scaled.recycle()
            text
        } catch (e: Exception) {
            ""
        }
    }

    private fun simpleTextExtraction(bitmap: Bitmap): String {
        val sb = StringBuilder()
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        var lastRow = -1
        for (y in 0 until height step 4) {
            var hasContent = false
            for (x in 0 until width step 2) {
                val pixel = pixels[y * width + x]
                val brightness = (Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3
                if (brightness < 200) {
                    hasContent = true
                    break
                }
            }
            if (hasContent && lastRow != y) {
                if (sb.isNotEmpty() && y - lastRow > 8) sb.append("\n")
                lastRow = y
            }
        }

        return sb.toString()
    }

    private fun escapeHtml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
    }
}
