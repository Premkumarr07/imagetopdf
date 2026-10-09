package com.example.imagetopdf.features.pdf_editor.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import com.example.imagetopdf.R
import com.example.imagetopdf.constants.AppColors
import com.example.imagetopdf.core.utils.PdfMerge
import com.example.imagetopdf.core.utils.PdfSplit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private enum class SplitState { IDLE, SPLITTING, DONE, ERROR }

@Composable
fun SplitPdfScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFile by remember { mutableStateOf<File?>(null) }
    var fileName by remember { mutableStateOf("") }
    var fileSize by remember { mutableStateOf("") }
    var pageCount by remember { mutableIntStateOf(0) }
    var splitMode by remember { mutableStateOf("all") }
    var rangeInput by remember { mutableStateOf("") }
    var splitState by remember { mutableStateOf(SplitState.IDLE) }
    var errorMessage by remember { mutableStateOf("") }
    var outputFiles by remember { mutableStateOf<List<File>>(emptyList()) }

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
            splitState = SplitState.IDLE
            outputFiles = emptyList()
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
                text = "Split PDF",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )
            Text(
                text = "Break a PDF into separate files",
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
                                Icons.Outlined.ContentCut,
                                contentDescription = null,
                                tint = AppColors.DarkBlue,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Select PDF to split",
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
                                .background(AppColors.OrangeBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.pdf),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = AppColors.Orange
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
                            splitState = SplitState.IDLE
                            outputFiles = emptyList()
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
                    text = "Split mode",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1E293B)
                )
                Spacer(modifier = Modifier.height(10.dp))

                SplitModeOption(
                    title = "Every page",
                    description = "Each page becomes a separate PDF",
                    selected = splitMode == "all",
                    onClick = { splitMode = "all" }
                )
                Spacer(modifier = Modifier.height(8.dp))
                SplitModeOption(
                    title = "Custom ranges",
                    description = "Specify page ranges (e.g., 1-3, 4-6, 7-10)",
                    selected = splitMode == "custom",
                    onClick = { splitMode = "custom" }
                )

                if (splitMode == "custom") {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = rangeInput,
                        onValueChange = { rangeInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("1-3, 4-6, 7-10") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppColors.AccentTeal,
                            unfocusedBorderColor = AppColors.LightSlate,
                            focusedContainerColor = AppColors.CardWhite,
                            unfocusedContainerColor = AppColors.CardWhite
                        )
                    )
                }

                if (splitState == SplitState.SPLITTING) {
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
                                text = "Splitting PDF...",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1E293B)
                            )
                        }
                    }
                }

                if (splitState == SplitState.DONE) {
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
                                text = "Split complete",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.TealGreen
                            )
                            Text(
                                text = "${outputFiles.size} files created",
                                fontSize = 13.sp,
                                color = Color(0xFF1E293B)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    selectedUri = null
                                    selectedFile = null
                                    fileName = ""
                                    fileSize = ""
                                    pageCount = 0
                                    splitState = SplitState.IDLE
                                    outputFiles = emptyList()
                                    rangeInput = ""
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AppColors.DarkBlue)
                            ) {
                                Text("Split Another")
                            }
                        }
                    }
                }

                if (splitState == SplitState.ERROR) {
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
                        if (splitState == SplitState.DONE || splitState == SplitState.ERROR) {
                            selectedUri = null
                            selectedFile = null
                            fileName = ""
                            fileSize = ""
                            pageCount = 0
                            splitState = SplitState.IDLE
                            outputFiles = emptyList()
                            rangeInput = ""
                        } else {
                            selectedFile?.let { file ->
                                splitState = SplitState.SPLITTING
                                scope.launch {
                                    val result = withContext(Dispatchers.IO) {
                                        try {
                                            val ranges = if (splitMode == "all") {
                                                (0 until pageCount).map { it..it }
                                            } else {
                                                parseRanges(rangeInput, pageCount)
                                            }
                                            if (ranges.isEmpty()) return@withContext null
                                            val outputDir = File(context.cacheDir, "split_output")
                                            outputDir.mkdirs()
                                            PdfSplit.splitPdf(context, file, outputDir, ranges)
                                        } catch (e: Exception) {
                                            null
                                        }
                                    }
                                    if (result != null && result.isNotEmpty()) {
                                        outputFiles = result
                                        splitState = SplitState.DONE
                                    } else {
                                        errorMessage = "Failed to split PDF. Check your page ranges."
                                        splitState = SplitState.ERROR
                                    }
                                }
                            }
                        }
                    },
                    enabled = selectedUri != null && splitState != SplitState.SPLITTING,
                    modifier = Modifier
                        .weight(2f)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (splitState == SplitState.DONE) AppColors.SuccessGreen else AppColors.DarkBlue,
                        disabledContainerColor = AppColors.LightSlate
                    )
                ) {
                    if (splitState == SplitState.DONE) {
                        Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Split Again", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(
                            Icons.Outlined.ContentCut,
                            contentDescription = "Split",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Split", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SplitModeOption(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) AppColors.DarkBlue.copy(alpha = 0.05f) else AppColors.CardWhite
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) AppColors.DarkBlue else AppColors.LightSlate
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = AppColors.DarkBlue)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1E293B)
                )
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = AppColors.SlateGray
                )
            }
        }
    }
}

private fun parseRanges(input: String, maxPage: Int): List<IntRange> {
    if (input.isBlank()) return emptyList()
    return input.split(",").mapNotNull { part ->
        val trimmed = part.trim()
        val match = Regex("(\\d+)-(\\d+)").find(trimmed)
        if (match != null) {
            val start = match.groupValues[1].toIntOrNull() ?: return@mapNotNull null
            val end = match.groupValues[2].toIntOrNull() ?: return@mapNotNull null
            if (start in 1..maxPage && end in 1..maxPage && start <= end) {
                (start - 1) until end
            } else null
        } else {
            val single = trimmed.toIntOrNull()
            if (single != null && single in 1..maxPage) {
                (single - 1) until single
            } else null
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
