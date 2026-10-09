package com.example.imagetopdf.features.pdf_editor.ui

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.ui.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.example.imagetopdf.features.image_to_pdf.ui.components.EmptyPickerCard
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import com.example.imagetopdf.R
import com.example.imagetopdf.constants.AppColors
import com.example.imagetopdf.core.utils.PdfMerge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

private enum class EditorState { IDLE, SAVING, DONE, ERROR }

data class PageAnnotation(
    val pageIndex: Int,
    val text: String,
    val x: Float,
    val y: Float
)

@Composable
fun PdfEditorScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFile by remember { mutableStateOf<File?>(null) }
    var fileName by remember { mutableStateOf("") }
    var fileSize by remember { mutableStateOf("") }
    var pageCount by remember { mutableIntStateOf(0) }
    var pageOrder by remember { mutableStateOf<List<Int>>(emptyList()) }
    var annotations by remember { mutableStateOf<List<PageAnnotation>>(emptyList()) }
    var editorState by remember { mutableStateOf(EditorState.IDLE) }
    var outputName by remember { mutableStateOf("edited_document") }
    var errorMessage by remember { mutableStateOf("") }
    var showAddTextDialog by remember { mutableStateOf(false) }
    var annotationText by remember { mutableStateOf("") }
    var selectedPageForAnnotation by remember { mutableIntStateOf(0) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedUri = it
            val file = uriToFile(context, it)
            selectedFile = file
            fileName = file?.name ?: "Selected PDF"
            fileSize = file?.let { f -> formatSize(f.length()) } ?: ""
            val count = file?.let { PdfMerge.getPageCount(it) } ?: 0
            pageCount = count
            pageOrder = (0 until count).toList()
            annotations = emptyList()
            editorState = EditorState.IDLE
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
                text = "PDF Editor",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )
            Text(
                text = "Reorder, delete pages and add text annotations",
                fontSize = 13.sp,
                color = AppColors.SlateGray
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (selectedUri == null) {
                EmptyPickerCard(onClick = { launcher.launch("application/pdf") })
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = AppColors.CardWhite),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(AppColors.PurpleBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.pdf),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = AppColors.Purple
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = fileName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1E293B),
                                maxLines = 1
                            )
                            Text(
                                text = "$fileSize · $pageCount pages",
                                fontSize = 12.sp,
                                color = AppColors.SlateGray
                            )
                        }
                        IconButton(onClick = {
                            selectedUri = null
                            selectedFile = null
                            fileName = ""
                            fileSize = ""
                            pageCount = 0
                            pageOrder = emptyList()
                            annotations = emptyList()
                            editorState = EditorState.IDLE
                        }) {
                            Icon(
                                Icons.Outlined.Close,
                                contentDescription = "Remove",
                                tint = AppColors.SlateGray
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Pages",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = "${pageOrder.size} of $pageCount",
                        fontSize = 12.sp,
                        color = AppColors.SlateGray
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(pageOrder) { index, originalIndex ->
                        PageThumbnailCard(
                            file = selectedFile,
                            pageIndex = originalIndex,
                            displayIndex = index,
                            onMoveUp = {
                                if (index > 0) {
                                    pageOrder = pageOrder.toMutableList().apply {
                                        val item = removeAt(index)
                                        add(index - 1, item)
                                    }
                                }
                            },
                            onMoveDown = {
                                if (index < pageOrder.size - 1) {
                                    pageOrder = pageOrder.toMutableList().apply {
                                        val item = removeAt(index)
                                        add(index + 1, item)
                                    }
                                }
                            },
                            onDelete = {
                                pageOrder = pageOrder.filterIndexed { i, _ -> i != index }
                            },
                            onAddText = {
                                selectedPageForAnnotation = originalIndex
                                showAddTextDialog = true
                            }
                        )
                    }
                }

                if (annotations.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Text Annotations",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1E293B)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    annotations.forEach { ann ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = AppColors.CardWhite),
                            elevation = CardDefaults.cardElevation(1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Outlined.TextFields,
                                    contentDescription = null,
                                    tint = AppColors.Purple,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = ann.text,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF1E293B),
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "Page ${ann.pageIndex + 1}",
                                        fontSize = 11.sp,
                                        color = AppColors.SlateGray
                                    )
                                }
                                IconButton(onClick = {
                                    annotations = annotations.filter { it != ann }
                                }) {
                                    Icon(
                                        Icons.Outlined.Delete,
                                        contentDescription = "Remove",
                                        tint = AppColors.RedDelete,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Output file name",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1E293B)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = outputName,
                    onValueChange = { outputName = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    trailingIcon = {
                        Text(
                            text = ".pdf",
                            color = AppColors.SlateGray,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppColors.AccentTeal,
                        unfocusedBorderColor = AppColors.LightSlate,
                        focusedContainerColor = AppColors.CardWhite,
                        unfocusedContainerColor = AppColors.CardWhite
                    )
                )

                if (editorState == EditorState.SAVING) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = AppColors.CardWhite),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(48.dp),
                                color = AppColors.DarkBlue,
                                strokeWidth = 4.dp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Saving...",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1E293B)
                            )
                        }
                    }
                }

                if (editorState == EditorState.DONE) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = AppColors.TealGreenBg),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = AppColors.TealGreen,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "PDF saved successfully",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.TealGreen
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        selectedUri?.let { uri ->
                                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                                setDataAndType(uri, "application/pdf")
                                                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(android.content.Intent.createChooser(intent, "Open PDF"))
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Open")
                                }
                                Button(
                                    onClick = {
                                        selectedUri = null
                                        selectedFile = null
                                        fileName = ""
                                        fileSize = ""
                                        pageCount = 0
                                        pageOrder = emptyList()
                                        annotations = emptyList()
                                        editorState = EditorState.IDLE
                                        outputName = "edited_document"
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.DarkBlue)
                                ) {
                                    Text("Edit Another")
                                }
                            }
                        }
                    }
                }

                if (editorState == EditorState.ERROR) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2)),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Outlined.Error,
                                contentDescription = null,
                                tint = AppColors.RedDelete,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = errorMessage,
                                fontSize = 14.sp,
                                color = AppColors.RedDelete,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }

        Surface(shadowElevation = 12.dp, color = AppColors.CardWhite) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { launcher.launch("application/pdf") },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, AppColors.DarkBlue),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.DarkBlue)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.pdf),
                        contentDescription = "Browse",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Browse", fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = {
                        if (editorState == EditorState.DONE || editorState == EditorState.ERROR) {
                            selectedUri = null
                            selectedFile = null
                            fileName = ""
                            fileSize = ""
                            pageCount = 0
                            pageOrder = emptyList()
                            annotations = emptyList()
                            editorState = EditorState.IDLE
                            outputName = "edited_document"
                        } else {
                            selectedFile?.let { file ->
                                if (pageOrder.isEmpty()) {
                                    errorMessage = "No pages remaining. Add at least one page."
                                    editorState = EditorState.ERROR
                                    return@Button
                                }
                                editorState = EditorState.SAVING
                                scope.launch {
                                    val result = withContext(Dispatchers.IO) {
                                        try {
                                            val tempFile = File(context.cacheDir, "temp_edit_${System.currentTimeMillis()}.pdf")
                                            val reorderSuccess = PdfMerge.extractPages(context, file, tempFile, pageOrder)
                                            if (!reorderSuccess) return@withContext null

                                            val finalFile = if (annotations.isEmpty()) {
                                                tempFile
                                            } else {
                                                applyAnnotations(context, tempFile, annotations, File(context.cacheDir, "final_${System.currentTimeMillis()}.pdf"))
                                            }

                                            val outputFile = File(context.cacheDir, "${outputName.ifBlank { "edited" }}.pdf")
                                            finalFile.copyTo(outputFile, overwrite = true)
                                            tempFile.delete()
                                            if (finalFile != outputFile) finalFile.delete()

                                            FileProvider.getUriForFile(
                                                context,
                                                "${context.packageName}.provider",
                                                outputFile
                                            )
                                        } catch (e: Exception) {
                                            null
                                        }
                                    }
                                    if (result != null) {
                                        editorState = EditorState.DONE
                                    } else {
                                        errorMessage = "Failed to save PDF. Please try again."
                                        editorState = EditorState.ERROR
                                    }
                                }
                            }
                        }
                    },
                    enabled = selectedUri != null && pageOrder.isNotEmpty() && editorState != EditorState.SAVING,
                    modifier = Modifier
                        .weight(2f)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (editorState == EditorState.DONE) AppColors.SuccessGreen else AppColors.DarkBlue,
                        disabledContainerColor = AppColors.LightSlate
                    )
                ) {
                    if (editorState == EditorState.DONE) {
                        Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Edit Again", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(
                            painter = painterResource(id = R.drawable.edit),
                            contentDescription = "Save",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save PDF", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showAddTextDialog) {
        AlertDialog(
            onDismissRequest = { showAddTextDialog = false },
            title = { Text("Add Text Annotation") },
            text = {
                Column {
                    Text("Page ${selectedPageForAnnotation + 1}", fontSize = 13.sp, color = AppColors.SlateGray)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = annotationText,
                        onValueChange = { annotationText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Enter text to add") },
                        singleLine = false,
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (annotationText.isNotBlank()) {
                            annotations = annotations + PageAnnotation(
                                pageIndex = selectedPageForAnnotation,
                                text = annotationText.trim(),
                                x = 50f,
                                y = 100f
                            )
                            annotationText = ""
                            showAddTextDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.DarkBlue)
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    annotationText = ""
                    showAddTextDialog = false
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun PageThumbnailCard(
    file: File?,
    pageIndex: Int,
    displayIndex: Int,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
    onAddText: () -> Unit
) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(file, pageIndex) {
        bitmap = withContext(Dispatchers.IO) {
            file?.let { PdfMerge.renderPageToBitmap(it, pageIndex, 0.3f) }
        }
    }

    Card(
        modifier = Modifier.width(120.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.CardWhite),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(AppColors.LightBg),
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap!!.asImageBitmap(),
                        contentDescription = "Page ${pageIndex + 1}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Page ${displayIndex + 1}",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1E293B)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onMoveUp,
                    enabled = displayIndex > 0,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Outlined.ArrowUpward,
                        contentDescription = "Move up",
                        modifier = Modifier.size(14.dp),
                        tint = if (displayIndex > 0) AppColors.SlateGray else AppColors.LightSlate
                    )
                }
                IconButton(
                    onClick = onMoveDown,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Outlined.ArrowDownward,
                        contentDescription = "Move down",
                        modifier = Modifier.size(14.dp),
                        tint = AppColors.SlateGray
                    )
                }
                IconButton(
                    onClick = onAddText,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Outlined.TextFields,
                        contentDescription = "Add text",
                        modifier = Modifier.size(14.dp),
                        tint = AppColors.Purple
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "Delete",
                        modifier = Modifier.size(14.dp),
                        tint = AppColors.RedDelete
                    )
                }
            }
        }
    }
}

private fun applyAnnotations(
    context: android.content.Context,
    inputFile: File,
    annotations: List<PageAnnotation>,
    outputFile: File
): File {
    val document = PdfDocument()
    try {
        val fd = android.os.ParcelFileDescriptor.open(inputFile, android.os.ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = android.graphics.pdf.PdfRenderer(fd)

        for (i in 0 until renderer.pageCount) {
            val page = renderer.openPage(i)
            val pageInfo = PdfDocument.PageInfo.Builder(page.width, page.height, i + 1).create()
            val pdfPage = document.startPage(pageInfo)
            val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(AndroidColor.WHITE)
            page.render(bitmap, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

            val canvas = pdfPage.canvas
            canvas.drawBitmap(bitmap, 0f, 0f, null)

            val pageAnnotations = annotations.filter { it.pageIndex == i }
            for (ann in pageAnnotations) {
                val paint = Paint().apply {
                    color = AndroidColor.BLACK
                    textSize = 24f
                    typeface = Typeface.DEFAULT_BOLD
                    isAntiAlias = true
                }
                canvas.drawText(ann.text, ann.x, ann.y, paint)
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

private fun uriToFile(context: android.content.Context, uri: Uri): File? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val file = File(context.cacheDir, "temp_${System.currentTimeMillis()}.pdf")
        file.outputStream().use { output ->
            inputStream.copyTo(output)
        }
        inputStream.close()
        file
    } catch (e: Exception) {
        null
    }
}

private fun formatSize(bytes: Long): String {
    return when {
        bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0)
        bytes >= 1024 -> "%.0f KB".format(bytes / 1024.0)
        else -> "$bytes B"
    }
}
