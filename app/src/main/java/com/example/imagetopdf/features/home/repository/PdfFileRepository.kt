
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.imagetopdf.features.home.model.PdfFileModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PdfFileRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val APP_FOLDER_NAME = "PDFMaker"
        private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    }

    /**
     * Get app-specific directory that doesn't require permissions on Android 10+
     * Uses /Android/data/[package]/files/Documents/PDFMaker/
     */
    fun getAppFolder(): File {
        val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            ?: context.filesDir
        val appFolder = File(baseDir, APP_FOLDER_NAME)
        if (!appFolder.exists()) {
            appFolder.mkdirs()
        }
        return appFolder
    }

    /**
     * Load PDFs from app-specific folder (no permissions needed)
     */
    suspend fun loadPdfFiles(): List<PdfFileModel> = withContext(Dispatchers.IO) {
        val folder = getAppFolder()
        folder.listFiles { file ->
            file.isFile && file.extension.equals("pdf", ignoreCase = true)
        }
            ?.sortedByDescending { it.lastModified() }
            ?.map { file ->
                PdfFileModel(
                    name = file.nameWithoutExtension,
                    path = file.absolutePath,
                    sizeLabel = formatSize(file.length()),
                    dateLabel = dateFormat.format(Date(file.lastModified())),
                    uri = getFileUri(file)
                )
            }
            ?: emptyList()
    }

    /**
     * Create new PDF file in app folder
     */
    fun createPdfFile(baseName: String): File {
        val folder = getAppFolder()
        var file = File(folder, "$baseName.pdf")
        var counter = 1
        while (file.exists()) {
            file = File(folder, "${baseName}_$counter.pdf")
            counter++
        }
        return file
    }

    /**
     * Save PDF bytes to app folder
     */
    suspend fun savePdfToAppFolder(bytes: ByteArray, fileName: String): File = withContext(Dispatchers.IO) {
        val file = createPdfFile(fileName)
        FileOutputStream(file).use { it.write(bytes) }
        file
    }

    /**
     * Save PDF to Downloads using MediaStore (Android 10+ compatible)
     */
    suspend fun saveToDownloads(pdfBytes: ByteArray, fileName: String): Uri? = withContext(Dispatchers.IO) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Use MediaStore for Android 10+
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, "$fileName.pdf")
                    put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/PDFMaker")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }

                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)

                uri?.let {
                    resolver.openOutputStream(it)?.use { outputStream ->
                        outputStream.write(pdfBytes)
                    }
                    values.clear()
                    values.put(MediaStore.Downloads.IS_PENDING, 0)
                    resolver.update(it, values, null, null)
                }
                uri
            } else {
                // Legacy approach for Android 9 and below
                @Suppress("DEPRECATION")
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val appDir = File(downloadsDir, "PDFMaker").apply { mkdirs() }
                val file = File(appDir, "$fileName.pdf")
                FileOutputStream(file).use { it.write(pdfBytes) }
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Get content URI for file sharing
     */
    fun getFileUri(file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    /**
     * Delete PDF file
     */
    suspend fun deleteFile(filePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            File(filePath).delete()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Share PDF via system share sheet
     */
    fun sharePdf(uri: Uri) {
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = android.content.Intent.createChooser(intent, "Share PDF")
        chooser.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    /**
     * Open PDF with external viewer
     */
    fun openPdf(uri: Uri) {
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(android.content.Intent.createChooser(intent, "Open PDF"))
    }

    private fun formatSize(bytes: Long): String {
        return when {
            bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0)
            bytes >= 1_024 -> "%.0f KB".format(bytes / 1_024.0)
            else -> "$bytes B"
        }
    }
}