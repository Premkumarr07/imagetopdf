package com.example.imagetopdf.features.pdf_editor.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import com.example.imagetopdf.R
import com.example.imagetopdf.constants.AppColors
import com.example.imagetopdf.core.utils.PdfAnnotate
import com.example.imagetopdf.core.utils.PdfMerge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private enum class HighlightState { IDLE, SAVING, DONE, ERROR }

@Composable
fun HighlightPdfScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFile by remember { mutableStateOf<File?>(null) }
    var fileName by remember { mutableStateOf("") }
    var fileSize by remember { mutableStateOf("") }
    var pageCount by remember { mutableIntStateOf(0) }
    var currentPage by remember { mutableIntStateOf(0) }
    var highlights by remember { mutableStateOf<List<PdfAnnotate.Highlight>>(emptyList()) }
    var highlightState by remember { mutableStateOf(HighlightState.IDLE) }
    var outputName by remember { mutableStateOf("highlighted_document") }
    var errorMessage by remember { mutableStateOf("") }
    var pageBitmaps by remember { mutableStateOf<Map<Int, android.graphics.Bitmap>>(emptyMap()) }

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
            currentPage = 0
            highlights = emptyList()
            highlightState = HighlightState.IDLE
            pageBitmaps = emptyMap()
        }
    }

    LaunchedEffect(selectedFile) {
        selectedFile?.let { file ->
            withContext(Dispatchers.IO) {
                val bitmaps = mutableMapOf<Int, android.graphics.Bitmap>()
                val fd = android.os.ParcelFileDescriptor.open(file, android.os.ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = android.graphics.pdf.PdfRenderer(fd)
                for (i in 0 until renderer.pageCount) {
                    val page = renderer.openPage(i)
                    val bitmap = android.graphics.Bitmap.createBitmap(page.width, page.height, android.graphics.Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(android.graphics.Color.WHITE)
                    page.render(bitmap, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    bitmaps[i] = bitmap
                }
                renderer.close()
                fd.close()
                pageBitmaps = bitmaps
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
                text = "Highlight PDF",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )
            Text(
                text = "Add highlight annotations to your PDF",
                fontSize = 13.sp,
                color = AppColors.SlateGray
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (selectedUri == null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clickable { launcher.launch("application/pdf") },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp))
                            .background(AppColors.DarkBlue.copy(alpha = 0.05f))
                            .clickable { launcher.launch("application/pdf") },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Outlined.Highlight,
                                contentDescription = null,
                                tint = AppColors.DarkBlue,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Select PDF to highlight",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AppColors.DarkBlue
                            )
                        }
                    }
                }
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
                                .background(AppColors.AmberBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.pdf),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = AppColors.Amber
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
                            currentPage = 0
                            highlights = emptyList()
                            highlightState = HighlightState.IDLE
                            pageBitmaps = emptyMap()
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

                Text(
                    text = "Page preview",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1E293B)
                )
                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(AppColors.LightBg),
                    contentAlignment = Alignment.Center
                ) {
                    val bitmap = pageBitmaps[currentPage]
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Page ${currentPage + 1}",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        CircularProgressIndicator(color = AppColors.DarkBlue)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { if (currentPage > 0) currentPage-- },
                        enabled = currentPage > 0
                    ) {
                        Icon(
                            Icons.Outlined.ChevronLeft,
                            contentDescription = "Previous",
                            tint = if (currentPage > 0) AppColors.DarkBlue else AppColors.LightSlate
                        )
                    }
                    Text(
                        text = "Page ${currentPage + 1} of $pageCount",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1E293B)
                    )
                    IconButton(
                        onClick = { if (currentPage < pageCount - 1) currentPage++ },
                        enabled = currentPage < pageCount - 1
                    ) {
                        Icon(
                            Icons.Outlined.ChevronRight,
                            contentDescription = "Next",
                            tint = if (currentPage < pageCount - 1) AppColors.DarkBlue else AppColors.LightSlate
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val pageHighlights = highlights.filter { it.pageIndex == currentPage }
                        if (pageHighlights.isNotEmpty()) {
                            highlights = highlights.filter { it.pageIndex != currentPage }
                        } else {
                            val bitmap = pageBitmaps[currentPage]
                            if (bitmap != null) {
                                val left = bitmap.width * 0.1f
                                val top = bitmap.height * 0.15f
                                val right = bitmap.width * 0.9f
                                val bottom = bitmap.height * 0.25f
                                highlights = highlights + PdfAnnotate.Highlight(
                                    pageIndex = currentPage,
                                    left = left,
                                    top = top,
                                    right = right,
                                    bottom = bottom
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (highlights.any { it.pageIndex == currentPage }) AppColors.Amber else AppColors.DarkBlue
                    )
                ) {
                    Icon(
                        Icons.Outlined.Highlight,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (highlights.any { it.pageIndex == currentPage }) "Remove highlight from this page" else "Add highlight to this page",
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (highlights.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Highlights: ${highlights.size}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.SlateGray
                    )
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

                if (highlightState == HighlightState.SAVING) {
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

                if (highlightState == HighlightState.DONE) {
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
                            Button(
                                onClick = {
                                    selectedUri = null
                                    selectedFile = null
                                    fileName = ""
                                    fileSize = ""
                                    pageCount = 0
                                    currentPage = 0
                                    highlights = emptyList()
                                    highlightState = HighlightState.IDLE
                                    pageBitmaps = emptyMap()
                                    outputName = "highlighted_document"
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AppColors.DarkBlue)
                            ) {
                                Text("Highlight Another")
                            }
                        }
                    }
                }

                if (highlightState == HighlightState.ERROR) {
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
                        if (highlightState == HighlightState.DONE || highlightState == HighlightState.ERROR) {
                            selectedUri = null
                            selectedFile = null
                            fileName = ""
                            fileSize = ""
                            pageCount = 0
                            currentPage = 0
                            highlights = emptyList()
                            highlightState = HighlightState.IDLE
                            pageBitmaps = emptyMap()
                            outputName = "highlighted_document"
                        } else {
                            if (highlights.isEmpty()) {
                                errorMessage = "Add at least one highlight before saving."
                                highlightState = HighlightState.ERROR
                                return@Button
                            }
                            selectedFile?.let { file ->
                                highlightState = HighlightState.SAVING
                                scope.launch {
                                    val result = withContext(Dispatchers.IO) {
                                        try {
                                            val outputFile = File(context.cacheDir, "${outputName.ifBlank { "highlighted" }}.pdf")
                                            val result = PdfAnnotate.applyHighlights(context, file, outputFile, highlights)
                                            FileProvider.getUriForFile(
                                                context,
                                                "${context.packageName}.provider",
                                                result
                                            )
                                        } catch (e: Exception) {
                                            null
                                        }
                                    }
                                    if (result != null) {
                                        highlightState = HighlightState.DONE
                                    } else {
                                        errorMessage = "Failed to save PDF. Please try again."
                                        highlightState = HighlightState.ERROR
                                    }
                                }
                            }
                        }
                    },
                    enabled = selectedUri != null && highlightState != HighlightState.SAVING,
                    modifier = Modifier
                        .weight(2f)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (highlightState == HighlightState.DONE) AppColors.SuccessGreen else AppColors.DarkBlue,
                        disabledContainerColor = AppColors.LightSlate
                    )
                ) {
                    if (highlightState == HighlightState.DONE) {
                        Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Highlight Again", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(
                            Icons.Outlined.Highlight,
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
