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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.example.imagetopdf.core.utils.PdfCompress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private enum class CompressState { IDLE, COMPRESSING, DONE, ERROR }

private val QUALITY_OPTIONS = listOf(
    "Low" to "Smallest file, lower quality",
    "Medium" to "Balanced size and quality",
    "High" to "Best quality, larger file"
)

@Composable
fun CompressScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFile by remember { mutableStateOf<File?>(null) }
    var originalSize by remember { mutableStateOf("") }
    var compressState by remember { mutableStateOf(CompressState.IDLE) }
    var progress by remember { mutableStateOf(0f) }
    var outputName by remember { mutableStateOf("compressed_document") }
    var selectedQuality by remember { mutableStateOf("Medium") }

    LaunchedEffect(Unit) {
        selectedQuality = com.example.imagetopdf.core.preferences.UserPreferences.qualityLabel(context)
    }
    var compressedSize by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedUri = it
            val file = uriToFile(context, it)
            selectedFile = file
            originalSize = file?.let { f -> formatSize(f.length()) } ?: ""
            compressState = CompressState.IDLE
            compressedSize = ""
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
                text = "Compress PDF",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )
            Text(
                text = "Reduce the file size of your PDF",
                fontSize = 13.sp,
                color = AppColors.SlateGray
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (selectedUri == null) {
                EmptyPickerCard(onClick = { launcher.launch("application/pdf") })
            } else {
                SelectedFileCard(
                    fileName = selectedFile?.name ?: "Selected PDF",
                    fileSize = originalSize,
                    onRemove = {
                        selectedUri = null
                        selectedFile = null
                        originalSize = ""
                        compressState = CompressState.IDLE
                        compressedSize = ""
                    }
                )
            }

            AnimatedVisibility(
                visible = selectedUri != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(24.dp))

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

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Compression level",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1E293B)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    QUALITY_OPTIONS.forEach { (quality, description) ->
                        val selected = selectedQuality == quality
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { selectedQuality = quality },
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
                                    onClick = { selectedQuality = quality },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = AppColors.DarkBlue
                                    )
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = quality,
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
                }
            }

            AnimatedVisibility(
                visible = compressState != CompressState.IDLE,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(24.dp))
                    when (compressState) {
                        CompressState.COMPRESSING -> {
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
                                        text = "Compressing...",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF1E293B)
                                    )
                                }
                            }
                        }
                        CompressState.DONE -> {
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
                                        text = "Compression complete",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AppColors.TealGreen
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "$originalSize → $compressedSize",
                                        fontSize = 14.sp,
                                        color = Color(0xFF1E293B)
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
                                                originalSize = ""
                                                compressState = CompressState.IDLE
                                                compressedSize = ""
                                                outputName = "compressed_document"
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = AppColors.DarkBlue)
                                        ) {
                                            Text("Compress Another")
                                        }
                                    }
                                }
                            }
                        }
                        CompressState.ERROR -> {
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
                        if (compressState == CompressState.DONE || compressState == CompressState.ERROR) {
                            selectedUri = null
                            selectedFile = null
                            originalSize = ""
                            compressState = CompressState.IDLE
                            compressedSize = ""
                            outputName = "compressed_document"
                        } else {
                            selectedFile?.let { file ->
                                compressState = CompressState.COMPRESSING
                                scope.launch {
                                    val quality = when (selectedQuality) {
                                        "Low" -> 20
                                        "Medium" -> 50
                                        else -> 80
                                    }
                                    val result = withContext(Dispatchers.IO) {
                                        val outputFile = File(context.cacheDir, "${outputName.ifBlank { "compressed" }}.pdf")
                                        val success = PdfCompress.compressPdf(context, file, outputFile, quality)
                                        if (success) {
                                            val uri = FileProvider.getUriForFile(
                                                context,
                                                "${context.packageName}.provider",
                                                outputFile
                                            )
                                            Pair(uri, outputFile.length())
                                        } else null
                                    }
                                    if (result != null) {
                                        compressedSize = formatSize(result.second)
                                        compressState = CompressState.DONE
                                    } else {
                                        errorMessage = "Compression failed. Please try again."
                                        compressState = CompressState.ERROR
                                    }
                                }
                            }
                        }
                    },
                    enabled = selectedUri != null && compressState != CompressState.COMPRESSING,
                    modifier = Modifier
                        .weight(2f)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (compressState == CompressState.DONE) AppColors.SuccessGreen else AppColors.DarkBlue,
                        disabledContainerColor = AppColors.LightSlate
                    )
                ) {
                    if (compressState == CompressState.DONE) {
                        Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Compress Again", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(
                            painter = painterResource(id = R.drawable.compression),
                            contentDescription = "Compress",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Compress", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectedFileCard(fileName: String, fileSize: String, onRemove: () -> Unit) {
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
                    .background(AppColors.BlueBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.pdf),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = AppColors.Blue
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
                    text = fileSize,
                    fontSize = 12.sp,
                    color = AppColors.SlateGray
                )
            }
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = "Remove",
                    tint = AppColors.SlateGray
                )
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
