package com.example.imagetopdf.features.pdf_editor.ui

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import androidx.compose.foundation.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
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

private enum class EsignState { IDLE, SAVING, DONE, ERROR }

@Composable
fun EsignPdfScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFile by remember { mutableStateOf<File?>(null) }
    var fileName by remember { mutableStateOf("") }
    var fileSize by remember { mutableStateOf("") }
    var pageCount by remember { mutableIntStateOf(0) }
    var currentPage by remember { mutableIntStateOf(0) }
    var signatureBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var signatureOffset by remember { mutableStateOf(Offset.Zero) }
    var signatureSize by remember { mutableStateOf(IntSize.Zero) }
    var esignState by remember { mutableStateOf(EsignState.IDLE) }
    var outputName by remember { mutableStateOf("signed_document") }
    var errorMessage by remember { mutableStateOf("") }
    var pageBitmaps by remember { mutableStateOf<Map<Int, Bitmap>>(emptyMap()) }
    var showSignaturePad by remember { mutableStateOf(false) }

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
            esignState = EsignState.IDLE
            pageBitmaps = emptyMap()
        }
    }

    LaunchedEffect(selectedFile) {
        selectedFile?.let { file ->
            withContext(Dispatchers.IO) {
                val bitmaps = mutableMapOf<Int, Bitmap>()
                val fd = android.os.ParcelFileDescriptor.open(file, android.os.ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = android.graphics.pdf.PdfRenderer(fd)
                for (i in 0 until renderer.pageCount) {
                    val page = renderer.openPage(i)
                    val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(AndroidColor.WHITE)
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
                text = "eSign PDF",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )
            Text(
                text = "Add your signature to a PDF document",
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
                                Icons.Outlined.Draw,
                                contentDescription = null,
                                tint = AppColors.DarkBlue,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Select PDF to sign",
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
                            currentPage = 0
                            signatureBitmap = null
                            esignState = EsignState.IDLE
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
                        .background(AppColors.LightBg)
                        .onGloballyPositioned { signatureSize = IntSize(it.size.width, it.size.height) },
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

                    if (signatureBitmap != null && signatureSize != IntSize.Zero) {
                        val sigBitmap = signatureBitmap!!
                        val sigWidth = 150.dp
                        val sigHeight = 60.dp
                        val sigWidthPx = with(density) { sigWidth.toPx() }
                        val sigHeightPx = with(density) { sigHeight.toPx() }

                        Box(
                            modifier = Modifier
                                .size(sigWidth, sigHeight)
                                .graphicsLayer {
                                    translationX = signatureOffset.x
                                    translationY = signatureOffset.y
                                }
                                .pointerInput(Unit) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        signatureOffset = Offset(
                                            signatureOffset.x + dragAmount.x,
                                            signatureOffset.y + dragAmount.y
                                        )
                                    }
                                }
                        ) {
                            Image(
                                bitmap = sigBitmap.asImageBitmap(),
                                contentDescription = "Signature",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { showSignaturePad = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Draw,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Draw Signature")
                    }

                    if (signatureBitmap != null) {
                        OutlinedButton(
                            onClick = { signatureBitmap = null },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Clear")
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

                if (esignState == EsignState.SAVING) {
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

                if (esignState == EsignState.DONE) {
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
                                text = "PDF signed successfully",
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
                                    signatureBitmap = null
                                    esignState = EsignState.IDLE
                                    pageBitmaps = emptyMap()
                                    outputName = "signed_document"
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AppColors.DarkBlue)
                            ) {
                                Text("Sign Another")
                            }
                        }
                    }
                }

                if (esignState == EsignState.ERROR) {
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
                        if (esignState == EsignState.DONE || esignState == EsignState.ERROR) {
                            selectedUri = null
                            selectedFile = null
                            fileName = ""
                            fileSize = ""
                            pageCount = 0
                            currentPage = 0
                            signatureBitmap = null
                            esignState = EsignState.IDLE
                            pageBitmaps = emptyMap()
                            outputName = "signed_document"
                        } else {
                            if (signatureBitmap == null) {
                                errorMessage = "Draw a signature first."
                                esignState = EsignState.ERROR
                                return@Button
                            }
                            selectedFile?.let { file ->
                                esignState = EsignState.SAVING
                                scope.launch {
                                    val result = withContext(Dispatchers.IO) {
                                        try {
                                            val sigBitmap = signatureBitmap!!
                                            val pageBitmap = pageBitmaps[currentPage]
                                            if (pageBitmap == null) return@withContext null

                                            val sigWidth = sigBitmap.width.toFloat()
                                            val sigHeight = sigBitmap.height.toFloat()
                                            val sigLeft = signatureOffset.x
                                            val sigTop = signatureOffset.y

                                            val outputFile = File(context.cacheDir, "${outputName.ifBlank { "signed" }}.pdf")
                                            val signature = PdfAnnotate.Signature(
                                                pageIndex = currentPage,
                                                left = sigLeft,
                                                top = sigTop,
                                                width = sigWidth,
                                                height = sigHeight,
                                                bitmap = sigBitmap
                                            )
                                            val result = PdfAnnotate.applySignature(context, file, outputFile, signature)
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
                                        esignState = EsignState.DONE
                                    } else {
                                        errorMessage = "Failed to save PDF. Please try again."
                                        esignState = EsignState.ERROR
                                    }
                                }
                            }
                        }
                    },
                    enabled = selectedUri != null && esignState != EsignState.SAVING,
                    modifier = Modifier
                        .weight(2f)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (esignState == EsignState.DONE) AppColors.SuccessGreen else AppColors.DarkBlue,
                        disabledContainerColor = AppColors.LightSlate
                    )
                ) {
                    if (esignState == EsignState.DONE) {
                        Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sign Again", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(
                            Icons.Outlined.Draw,
                            contentDescription = "Sign",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save PDF", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showSignaturePad) {
        SignaturePadDialog(
            onDismiss = { showSignaturePad = false },
            onSave = { bitmap ->
                signatureBitmap = bitmap
                signatureOffset = Offset(50f, 50f)
                showSignaturePad = false
            }
        )
    }
}

@Composable
private fun SignaturePadDialog(
    onDismiss: () -> Unit,
    onSave: (Bitmap) -> Unit
) {
    var paths by remember { mutableStateOf<List<List<Offset>>>(emptyList()) }
    var currentPath by remember { mutableStateOf<List<Offset>>(emptyList()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Draw your signature") },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                currentPath = listOf(offset)
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                currentPath = currentPath + change.position
                            },
                            onDragEnd = {
                                if (currentPath.isNotEmpty()) {
                                    paths = paths + listOf(currentPath)
                                    currentPath = emptyList()
                                }
                            }
                        )
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val allPaths = paths + if (currentPath.isNotEmpty()) listOf(currentPath) else emptyList()
                    for (path in allPaths) {
                        for (i in 0 until path.size - 1) {
                            drawLine(
                                color = Color.Black,
                                start = path[i],
                                end = path[i + 1],
                                strokeWidth = 4f
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (paths.isEmpty()) return@Button
                    val bitmap = Bitmap.createBitmap(600, 250, Bitmap.Config.ARGB_8888)
                    val canvas = AndroidCanvas(bitmap)
                    canvas.drawColor(AndroidColor.WHITE)
                    val paint = Paint().apply {
                        color = AndroidColor.BLACK
                        strokeWidth = 4f
                        isAntiAlias = true
                    }
                    for (path in paths) {
                        for (i in 0 until path.size - 1) {
                            canvas.drawLine(path[i].x, path[i].y, path[i + 1].x, path[i + 1].y, paint)
                        }
                    }
                    onSave(bitmap)
                },
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.DarkBlue)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                paths = emptyList()
                currentPath = emptyList()
                onDismiss()
            }) {
                Text("Cancel")
            }
        }
    )
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
