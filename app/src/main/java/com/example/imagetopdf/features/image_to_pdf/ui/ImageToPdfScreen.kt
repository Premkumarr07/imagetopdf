package com.example.imagetopdf.features.image_to_pdf.ui

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.imagetopdf.R
import com.example.imagetopdf.constants.AppColors
import com.example.imagetopdf.features.image_to_pdf.ui.components.ConversionProgress
import com.example.imagetopdf.features.image_to_pdf.ui.components.ConversionSuccess
import com.example.imagetopdf.features.image_to_pdf.ui.components.ConvertBottomBar
import com.example.imagetopdf.features.image_to_pdf.ui.components.EmptyPickerCard
import com.example.imagetopdf.features.image_to_pdf.ui.components.ErrorCard
import com.example.imagetopdf.features.image_to_pdf.ui.components.ImageThumbnailStrip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

// ── Page size options in points (1 pt = 1/72 inch) ───────────────────────────
private val PAGE_SIZES = mapOf(
    "A4"     to Pair(595, 842),
    "Letter" to Pair(612, 792),
    "Auto"   to null                // will be set per-image
)

// ── Quality → JPEG compression ────────────────────────────────────────────────
private val QUALITY_MAP = mapOf(
    "Low"    to 50,
    "Medium" to 75,
    "High"   to 95
)

private enum class ConvertState { IDLE, CONVERTING, DONE, ERROR }

// ─────────────────────────────────────────────────────────────────────────────
// Real PDF conversion helper
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Converts a list of image [Uri]s into a PDF file saved in the app's cache,
 * then copies it to the public Downloads folder.
 *
 * @return The [Uri] of the saved PDF in Downloads, or null on failure.
 */
suspend fun convertImagesToPdf(
    context:   Context,
    uris:      List<Uri>,
    pdfName:   String,
    pageSize:  String,
    quality:   String
): Uri? = withContext(Dispatchers.IO) {
    val jpegQuality = QUALITY_MAP[quality] ?: 95
    val pdfDocument = PdfDocument()

    try {
        uris.forEachIndexed { index, uri ->
            // 1. Decode the bitmap
            val inputStream: InputStream = context.contentResolver.openInputStream(uri) ?: return@forEachIndexed
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            if (originalBitmap == null) return@forEachIndexed

            // 2. Determine page dimensions
            val (pageW, pageH) = when (pageSize) {
                "Auto" -> Pair(originalBitmap.width, originalBitmap.height)
                else   -> PAGE_SIZES[pageSize] ?: Pair(595, 842)
            }

            // 3. Scale bitmap to fit the page while keeping aspect ratio
            val bitmapRatio = originalBitmap.width.toFloat() / originalBitmap.height.toFloat()
            val pageRatio   = pageW.toFloat() / pageH.toFloat()

            val (drawW, drawH) = if (bitmapRatio > pageRatio) {
                // Image is wider — fit to page width
                val w = pageW
                val h = (pageW / bitmapRatio).toInt()
                Pair(w, h)
            } else {
                // Image is taller — fit to page height
                val h = pageH
                val w = (pageH * bitmapRatio).toInt()
                Pair(w, h)
            }

            val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, drawW, drawH, true)
            originalBitmap.recycle()

            // 4. Create a PDF page and draw the bitmap centred on it
            val pageInfo = PdfDocument.PageInfo.Builder(pageW, pageH, index + 1).create()
            val page     = pdfDocument.startPage(pageInfo)
            val canvas: Canvas = page.canvas
            val paint   = Paint().apply { isAntiAlias = true }

            // White background
            canvas.drawColor(android.graphics.Color.WHITE)

            // Centre the image
            val left = ((pageW - drawW) / 2).toFloat()
            val top  = ((pageH - drawH) / 2).toFloat()
            canvas.drawBitmap(scaledBitmap, left, top, paint)
            scaledBitmap.recycle()

            pdfDocument.finishPage(page)
        }

        // 5. Write to cache file first
        val cacheFile = File(context.cacheDir, "$pdfName.pdf")
        FileOutputStream(cacheFile).use { pdfDocument.writeTo(it) }

        // 6. Save to Downloads (works on Android Q+ and legacy)
        val savedUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, "$pdfName.pdf")
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            uri?.let {
                resolver.openOutputStream(it)?.use { out -> cacheFile.inputStream().copyTo(out) }
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(it, values, null, null)
            }
            uri
        } else {
            @Suppress("DEPRECATION")
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            downloadsDir.mkdirs()
            val destFile = File(downloadsDir, "$pdfName.pdf")
            cacheFile.copyTo(destFile, overwrite = true)
            FileProvider.getUriForFile(context, "${context.packageName}.provider", destFile)
        }

        return@withContext savedUri

    } catch (e: Exception) {
        e.printStackTrace()
        return@withContext null
    } finally {
        pdfDocument.close()
    }
}


@Composable
fun ImageToPdfScreen(navController: NavController) {
    val context = LocalContext.current

    var selectedUris  by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var convertState  by remember { mutableStateOf(ConvertState.IDLE) }
    var progress      by remember { mutableStateOf(0f) }
    var pdfName       by remember { mutableStateOf("document_${System.currentTimeMillis()}") }
    var savedPdfUri   by remember { mutableStateOf<Uri?>(null) }
    var errorMessage  by remember { mutableStateOf("") }
    var selectedSize  by remember { mutableStateOf("A4") }
    var selectedQuality by remember { mutableStateOf("High") }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            selectedUris = (selectedUris + uris).distinct()
            convertState = ConvertState.IDLE
            savedPdfUri  = null
        }
    }

    // Real conversion coroutine
    LaunchedEffect(convertState) {
        if (convertState == ConvertState.CONVERTING) {
            progress    = 0f
            savedPdfUri = null

            val progressJob = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                repeat(80) { i ->
                    kotlinx.coroutines.delay(30L)
                    progress = (i + 1) / 90f
                }
            }

            val resultUri = convertImagesToPdf(
                context  = context,
                uris     = selectedUris,
                pdfName  = pdfName.ifBlank { "document" },
                pageSize = selectedSize,
                quality  = selectedQuality
            )

            progressJob.cancel()
            progress = 1f

            if (resultUri != null) {
                savedPdfUri  = resultUri
                convertState = ConvertState.DONE
            } else {
                errorMessage = "Conversion failed. Please try again."
                convertState = ConvertState.ERROR
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.LightBg)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text       = "Selected Images",
                fontSize   = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color      = Color(0xFF1E293B)
            )
            Text(
                text     = "${selectedUris.size} image${if (selectedUris.size != 1) "s" else ""} selected",
                fontSize = 13.sp,
                color    = AppColors.SlateGray
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedUris.isEmpty()) {
                EmptyPickerCard(onClick = { launcher.launch("image/*") })
            } else {
                ImageThumbnailStrip(
                    uris      = selectedUris,
                    onRemove  = { uri ->
                        selectedUris = selectedUris - uri
                        if (selectedUris.isEmpty()) {
                            convertState = ConvertState.IDLE
                            savedPdfUri  = null
                        }
                    },
                    onAddMore = { launcher.launch("image/*") }
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // PDF name field
            AnimatedVisibility(
                visible = selectedUris.isNotEmpty(),
                enter   = fadeIn() + expandVertically(),
                exit    = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Text(
                        text       = "Output file name",
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = Color(0xFF1E293B)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value         = pdfName,
                        onValueChange = { pdfName = it },
                        modifier      = Modifier.fillMaxWidth(),
                        singleLine    = true,
                        trailingIcon  = {
                            Text(
                                text     = ".pdf",
                                color    = AppColors.SlateGray,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                        },
                        shape  = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor      = AppColors.AccentTeal,
                            unfocusedBorderColor    = AppColors.LightSlate,
                            focusedContainerColor   = AppColors.CardWhite,
                            unfocusedContainerColor = AppColors.CardWhite
                        )
                    )
                    Spacer(modifier = Modifier.height(28.dp))
                }
            }

            // Options
            AnimatedVisibility(
                visible = selectedUris.isNotEmpty(),
                enter   = fadeIn() + expandVertically(),
                exit    = fadeOut() + shrinkVertically()
            ) {
                OptionsRow(
                    selectedSize    = selectedSize,
                    selectedQuality = selectedQuality,
                    onSizeChange    = { selectedSize = it },
                    onQualityChange = { selectedQuality = it }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            AnimatedVisibility(
                visible = convertState != ConvertState.IDLE,
                enter   = fadeIn() + expandVertically(),
                exit    = fadeOut() + shrinkVertically()
            ) {
                when (convertState) {
                    ConvertState.CONVERTING -> ConversionProgress(progress)

                    ConvertState.DONE -> ConversionSuccess(
                        pdfName    = pdfName.ifBlank { "document" },
                        onDownload = {
                            // PDF is already saved in Downloads — open it so user sees it
                            savedPdfUri?.let { uri ->
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, "application/pdf")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(Intent.createChooser(intent, "Open PDF"))
                            }
                        },
                        onShare = {
                            savedPdfUri?.let { uri ->
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/pdf"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share PDF"))
                            }
                        }
                    )

                    ConvertState.ERROR -> ErrorCard(message = errorMessage)

                    else -> {}
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }

        ConvertBottomBar(
            enabled      = selectedUris.isNotEmpty() && convertState != ConvertState.CONVERTING,
            isDone       = convertState == ConvertState.DONE,
            imageCount   = selectedUris.size,
            onPickImages = { launcher.launch("image/*") },
            onConvert    = {
                if (convertState == ConvertState.DONE || convertState == ConvertState.ERROR) {
                    selectedUris  = emptyList()
                    convertState  = ConvertState.IDLE
                    savedPdfUri   = null
                    pdfName       = "document_${System.currentTimeMillis()}"
                } else {
                    convertState = ConvertState.CONVERTING
                }
            }
        )
    }
}

@Composable
private fun OptionsRow(
    selectedSize:    String,
    selectedQuality: String,
    onSizeChange:    (String) -> Unit,
    onQualityChange: (String) -> Unit
) {
    Column {
        Text("Options", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B))
        Spacer(modifier = Modifier.height(10.dp))

        // Page size
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf("A4", "Letter", "Auto").forEach { size ->
                val selected = selectedSize == size
                FilterChip(
                    selected = selected,
                    onClick  = { onSizeChange(size) },
                    label    = { Text(size, fontSize = 13.sp) },
                    colors   = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AppColors.DarkBlue,
                        selectedLabelColor     = Color.White,
                        containerColor         = AppColors.CardWhite,
                        labelColor             = AppColors.SlateGray
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled             = true,
                        selected            = selected,
                        borderColor         = AppColors.LightSlate,
                        selectedBorderColor = AppColors.DarkBlue
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Quality
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf("Low", "Medium", "High").forEach { q ->
                val selected = selectedQuality == q
                FilterChip(
                    selected = selected,
                    onClick  = { onQualityChange(q) },
                    label    = { Text(q, fontSize = 13.sp) },
                    colors   = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AppColors.AccentTeal,
                        selectedLabelColor     = Color.White,
                        containerColor         = AppColors.CardWhite,
                        labelColor             = AppColors.SlateGray
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled             = true,
                        selected            = selected,
                        borderColor         = AppColors.LightSlate,
                        selectedBorderColor = AppColors.AccentTeal
                    )
                )
            }
        }
    }
}
