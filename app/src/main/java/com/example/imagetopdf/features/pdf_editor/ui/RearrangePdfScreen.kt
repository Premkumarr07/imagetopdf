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
import androidx.compose.ui.graphics.asImageBitmap
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
import com.example.imagetopdf.R
import com.example.imagetopdf.constants.AppColors
import com.example.imagetopdf.core.utils.PdfMerge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private enum class RearrangeState { IDLE, SAVING, DONE, ERROR }

@Composable
fun RearrangePdfScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFile by remember { mutableStateOf<File?>(null) }
    var fileName by remember { mutableStateOf("") }
    var fileSize by remember { mutableStateOf("") }
    var pageCount by remember { mutableIntStateOf(0) }
    var pageOrder by remember { mutableStateOf<List<Int>>(emptyList()) }
    var rearrangeState by remember { mutableStateOf(RearrangeState.IDLE) }
    var outputName by remember { mutableStateOf("rearranged_document") }
    var errorMessage by remember { mutableStateOf("") }

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
            rearrangeState = RearrangeState.IDLE
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
                text = "Rearrange PDF",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )
            Text(
                text = "Change the order of pages in your PDF",
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
                                Icons.Outlined.SwapVert,
                                contentDescription = null,
                                tint = AppColors.DarkBlue,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Select PDF to rearrange",
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
                            pageOrder = emptyList()
                            rearrangeState = RearrangeState.IDLE
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
                    text = "Page order",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1E293B)
                )
                Text(
                    text = "Use arrows to reorder pages",
                    fontSize = 12.sp,
                    color = AppColors.SlateGray
                )

                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(pageOrder) { index, originalIndex ->
                        RearrangePageCard(
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
                            }
                        )
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

                if (rearrangeState == RearrangeState.SAVING) {
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

                if (rearrangeState == RearrangeState.DONE) {
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
                                    pageOrder = emptyList()
                                    rearrangeState = RearrangeState.IDLE
                                    outputName = "rearranged_document"
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AppColors.DarkBlue)
                            ) {
                                Text("Rearrange Another")
                            }
                        }
                    }
                }

                if (rearrangeState == RearrangeState.ERROR) {
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
                        if (rearrangeState == RearrangeState.DONE || rearrangeState == RearrangeState.ERROR) {
                            selectedUri = null
                            selectedFile = null
                            fileName = ""
                            fileSize = ""
                            pageCount = 0
                            pageOrder = emptyList()
                            rearrangeState = RearrangeState.IDLE
                            outputName = "rearranged_document"
                        } else {
                            selectedFile?.let { file ->
                                rearrangeState = RearrangeState.SAVING
                                scope.launch {
                                    val result = withContext(Dispatchers.IO) {
                                        try {
                                            val outputFile = File(context.cacheDir, "${outputName.ifBlank { "rearranged" }}.pdf")
                                            val success = PdfMerge.extractPages(context, file, outputFile, pageOrder)
                                            if (success) {
                                                FileProvider.getUriForFile(
                                                    context,
                                                    "${context.packageName}.provider",
                                                    outputFile
                                                )
                                            } else null
                                        } catch (e: Exception) {
                                            null
                                        }
                                    }
                                    if (result != null) {
                                        rearrangeState = RearrangeState.DONE
                                    } else {
                                        errorMessage = "Failed to save PDF. Please try again."
                                        rearrangeState = RearrangeState.ERROR
                                    }
                                }
                            }
                        }
                    },
                    enabled = selectedUri != null && pageOrder.isNotEmpty() && rearrangeState != RearrangeState.SAVING,
                    modifier = Modifier
                        .weight(2f)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (rearrangeState == RearrangeState.DONE) AppColors.SuccessGreen else AppColors.DarkBlue,
                        disabledContainerColor = AppColors.LightSlate
                    )
                ) {
                    if (rearrangeState == RearrangeState.DONE) {
                        Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Rearrange Again", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(
                            Icons.Outlined.SwapVert,
                            contentDescription = "Rearrange",
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

@Composable
private fun RearrangePageCard(
    file: File?,
    pageIndex: Int,
    displayIndex: Int,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(file, pageIndex) {
        bitmap = withContext(Dispatchers.IO) {
            file?.let { PdfMerge.renderPageToBitmap(it, pageIndex, 0.3f) }
        }
    }

    Card(
        modifier = Modifier.width(100.dp),
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
                    .height(120.dp)
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

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${displayIndex + 1}",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1E293B)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onMoveUp,
                    enabled = displayIndex > 0,
                    modifier = Modifier.size(24.dp)
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
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Outlined.ArrowDownward,
                        contentDescription = "Move down",
                        modifier = Modifier.size(14.dp),
                        tint = AppColors.SlateGray
                    )
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
