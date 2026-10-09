package com.example.imagetopdf.features.pdf_editor.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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

private enum class MergeState { IDLE, MERGING, DONE, ERROR }

data class MergeFileItem(
    val uri: Uri,
    val name: String,
    val size: String,
    val pageCount: Int
)

@Composable
fun MergePdfScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var files by remember { mutableStateOf<List<MergeFileItem>>(emptyList()) }
    var mergeState by remember { mutableStateOf(MergeState.IDLE) }
    var progress by remember { mutableStateOf(0f) }
    var outputName by remember { mutableStateOf("merged_document") }
    var errorMessage by remember { mutableStateOf("") }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            val newFiles = uris.map { uri ->
                val file = uriToFile(context, uri)
                MergeFileItem(
                    uri = uri,
                    name = file?.name ?: "PDF",
                    size = file?.let { formatSize(it.length()) } ?: "",
                    pageCount = file?.let { PdfMerge.getPageCount(it) } ?: 0
                )
            }
            files = (files + newFiles).distinctBy { it.uri.toString() }
            mergeState = MergeState.IDLE
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
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Merge PDFs",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )
            Text(
                text = "Combine multiple PDF files into one",
                fontSize = 13.sp,
                color = AppColors.SlateGray
            )

            Spacer(modifier = Modifier.height(20.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { launcher.launch("application/pdf") },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(AppColors.DarkBlue.copy(alpha = 0.05f))
                        .clickable { launcher.launch("application/pdf") },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(AppColors.DarkBlue.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                tint = AppColors.DarkBlue,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Add PDF files",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.DarkBlue
                        )
                        Text(
                            text = "Select 2 or more PDFs to merge",
                            fontSize = 12.sp,
                            color = AppColors.SlateGray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (files.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${files.size} file${if (files.size != 1) "s" else ""} selected",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = "Total: ${files.sumOf { it.pageCount }} pages",
                        fontSize = 12.sp,
                        color = AppColors.SlateGray
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    itemsIndexed(files) { index, file ->
                        MergeFileRow(
                            index = index,
                            file = file,
                            onMoveUp = {
                                if (index > 0) {
                                    files = files.toMutableList().apply {
                                        val item = removeAt(index)
                                        add(index - 1, item)
                                    }
                                }
                            },
                            onMoveDown = {
                                if (index < files.size - 1) {
                                    files = files.toMutableList().apply {
                                        val item = removeAt(index)
                                        add(index + 1, item)
                                    }
                                }
                            },
                            onRemove = {
                                files = files.filterIndexed { i, _ -> i != index }
                                if (files.isEmpty()) mergeState = MergeState.IDLE
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

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
            }

            AnimatedVisibility(
                visible = mergeState != MergeState.IDLE,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(20.dp))
                    when (mergeState) {
                        MergeState.MERGING -> {
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
                                        progress = { progress },
                                        modifier = Modifier.size(48.dp),
                                        color = AppColors.DarkBlue,
                                        strokeWidth = 4.dp
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Merging PDFs...",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF1E293B)
                                    )
                                }
                            }
                        }
                        MergeState.DONE -> {
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
                                        text = "PDFs merged successfully",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AppColors.TealGreen
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${files.size} files combined into one PDF",
                                        fontSize = 13.sp,
                                        color = Color(0xFF1E293B)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                files.forEach { uri ->
                                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                                        setDataAndType(uri.uri, "application/pdf")
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
                                                files = emptyList()
                                                mergeState = MergeState.IDLE
                                                outputName = "merged_document"
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = AppColors.DarkBlue)
                                        ) {
                                            Text("Merge More")
                                        }
                                    }
                                }
                            }
                        }
                        MergeState.ERROR -> {
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
                        else -> {}
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
                        if (mergeState == MergeState.DONE || mergeState == MergeState.ERROR) {
                            files = emptyList()
                            mergeState = MergeState.IDLE
                            outputName = "merged_document"
                        } else {
                            if (files.size < 2) {
                                errorMessage = "Select at least 2 PDF files to merge"
                                mergeState = MergeState.ERROR
                                return@Button
                            }
                            mergeState = MergeState.MERGING
                            scope.launch {
                                val result = withContext(Dispatchers.IO) {
                                    try {
                                        val inputFiles = files.mapNotNull { uriToFile(context, it.uri) }
                                        if (inputFiles.size < 2) return@withContext null
                                        val outputFile = File(context.cacheDir, "${outputName.ifBlank { "merged" }}.pdf")
                                        val success = PdfMerge.mergePdfs(context, inputFiles, outputFile)
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
                                    mergeState = MergeState.DONE
                                } else {
                                    errorMessage = "Merge failed. Please try again."
                                    mergeState = MergeState.ERROR
                                }
                            }
                        }
                    },
                    enabled = files.size >= 2 && mergeState != MergeState.MERGING,
                    modifier = Modifier
                        .weight(2f)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (mergeState == MergeState.DONE) AppColors.SuccessGreen else AppColors.DarkBlue,
                        disabledContainerColor = AppColors.LightSlate
                    )
                ) {
                    if (mergeState == MergeState.DONE) {
                        Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Merge Again", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(
                            painter = painterResource(id = R.drawable.merge),
                            contentDescription = "Merge",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Merge", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun MergeFileRow(
    index: Int,
    file: MergeFileItem,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.CardWhite),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(AppColors.PurpleBg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${index + 1}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.Purple
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1E293B),
                    maxLines = 1
                )
                Text(
                    text = "${file.size} · ${file.pageCount} pages",
                    fontSize = 11.sp,
                    color = AppColors.SlateGray
                )
            }
            IconButton(onClick = onMoveUp, enabled = index > 0) {
                Icon(
                    Icons.Outlined.ArrowUpward,
                    contentDescription = "Move up",
                    tint = if (index > 0) AppColors.SlateGray else AppColors.LightSlate,
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(onClick = onMoveDown, enabled = index < Int.MAX_VALUE) {
                Icon(
                    Icons.Outlined.ArrowDownward,
                    contentDescription = "Move down",
                    tint = AppColors.SlateGray,
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = "Remove",
                    tint = AppColors.RedDelete,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private fun uriToFile(context: android.content.Context, uri: Uri): File? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val file = File(context.cacheDir, "temp_${System.currentTimeMillis()}_${uri.hashCode()}.pdf")
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
