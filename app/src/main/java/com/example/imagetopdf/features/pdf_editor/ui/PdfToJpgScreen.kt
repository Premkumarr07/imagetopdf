package com.example.imagetopdf.features.pdf_editor.ui

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
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
import java.io.FileOutputStream

private enum class JpgState { IDLE, CONVERTING, DONE, ERROR }

@Composable
fun PdfToJpgScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFile by remember { mutableStateOf<File?>(null) }
    var fileName by remember { mutableStateOf("") }
    var fileSize by remember { mutableStateOf("") }
    var pageCount by remember { mutableIntStateOf(0) }
    var jpgQuality by remember { mutableIntStateOf(90) }
    var jpgState by remember { mutableStateOf(JpgState.IDLE) }
    var outputFiles by remember { mutableStateOf<List<File>>(emptyList()) }
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
            jpgState = JpgState.IDLE
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
                text = "PDF to JPG",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )
            Text(
                text = "Convert PDF pages to JPG images",
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
                                Icons.Outlined.Image,
                                contentDescription = null,
                                tint = AppColors.DarkBlue,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Select PDF to convert",
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
                                .background(AppColors.TealGreenBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.pdf),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = AppColors.TealGreen
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
                            jpgState = JpgState.IDLE
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
                    text = "Image quality",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1E293B)
                )
                Spacer(modifier = Modifier.height(10.dp))

                listOf(
                    "Low (60%)" to 60,
                    "Medium (80%)" to 80,
                    "High (95%)" to 95
                ).forEach { (label, quality) ->
                    val selected = jpgQuality == quality
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { jpgQuality = quality },
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
                                onClick = { jpgQuality = quality },
                                colors = RadioButtonDefaults.colors(selectedColor = AppColors.DarkBlue)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = label,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1E293B)
                            )
                        }
                    }
                }

                if (jpgState == JpgState.CONVERTING) {
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
                                text = "Converting...",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1E293B)
                            )
                        }
                    }
                }

                if (jpgState == JpgState.DONE) {
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
                                text = "Conversion complete",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.TealGreen
                            )
                            Text(
                                text = "${outputFiles.size} JPG images created",
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
                                    jpgState = JpgState.IDLE
                                    outputFiles = emptyList()
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AppColors.DarkBlue)
                            ) {
                                Text("Convert Another")
                            }
                        }
                    }
                }

                if (jpgState == JpgState.ERROR) {
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
                        if (jpgState == JpgState.DONE || jpgState == JpgState.ERROR) {
                            selectedUri = null
                            selectedFile = null
                            fileName = ""
                            fileSize = ""
                            pageCount = 0
                            jpgState = JpgState.IDLE
                            outputFiles = emptyList()
                        } else {
                            selectedFile?.let { file ->
                                jpgState = JpgState.CONVERTING
                                scope.launch {
                                    val result = withContext(Dispatchers.IO) {
                                        try {
                                            convertPdfToJpg(context, file, jpgQuality)
                                        } catch (e: Exception) {
                                            null
                                        }
                                    }
                                    if (result != null && result.isNotEmpty()) {
                                        outputFiles = result
                                        jpgState = JpgState.DONE
                                    } else {
                                        errorMessage = "Conversion failed. Please try again."
                                        jpgState = JpgState.ERROR
                                    }
                                }
                            }
                        }
                    },
                    enabled = selectedUri != null && jpgState != JpgState.CONVERTING,
                    modifier = Modifier
                        .weight(2f)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (jpgState == JpgState.DONE) AppColors.SuccessGreen else AppColors.DarkBlue,
                        disabledContainerColor = AppColors.LightSlate
                    )
                ) {
                    if (jpgState == JpgState.DONE) {
                        Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Convert Again", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(
                            Icons.Outlined.Image,
                            contentDescription = "Convert",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Convert", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun convertPdfToJpg(context: android.content.Context, inputFile: File, quality: Int): List<File> {
    val results = mutableListOf<File>()
    val fd = ParcelFileDescriptor.open(inputFile, ParcelFileDescriptor.MODE_READ_ONLY)
    val renderer = PdfRenderer(fd)
    val outputDir = File(context.cacheDir, "jpg_output")
    outputDir.mkdirs()

    for (i in 0 until renderer.pageCount) {
        val page = renderer.openPage(i)
        val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.WHITE)
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        page.close()

        val outputFile = File(outputDir, "page_${i + 1}.jpg")
        FileOutputStream(outputFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
        }
        bitmap.recycle()
        results.add(outputFile)
    }

    renderer.close()
    fd.close()
    return results
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
