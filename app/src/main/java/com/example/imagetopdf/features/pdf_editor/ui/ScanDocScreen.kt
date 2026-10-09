package com.example.imagetopdf.features.pdf_editor.ui

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import com.example.imagetopdf.R
import com.example.imagetopdf.constants.AppColors
import com.example.imagetopdf.navigation.NavigationRoutes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

private enum class ScanState { IDLE, PROCESSING, DONE, ERROR }

@Composable
fun ScanDocScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var scanState by remember { mutableStateOf(ScanState.IDLE) }
    var outputName by remember { mutableStateOf("scanned_document") }
    var errorMessage by remember { mutableStateOf("") }
    var savedUri by remember { mutableStateOf<Uri?>(null) }
    var savedPath by remember { mutableStateOf<String?>(null) }
    var cornerPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var imageSize by remember { mutableStateOf(IntSize.Zero) }
    var filterMode by remember { mutableStateOf("original") }
    var flatBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var rotationSteps by remember { mutableIntStateOf(0) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val bmp = loadBitmapFromUri(context, it)
            if (bmp != null) {
                capturedBitmap = bmp
                flatBitmap = null
                cornerPoints = emptyList()
                rotationSteps = 0
                scanState = ScanState.IDLE
                savedUri = null
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            capturedBitmap = bitmap
            flatBitmap = null
            cornerPoints = emptyList()
            rotationSteps = 0
            scanState = ScanState.IDLE
            savedUri = null
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraLauncher.launch(null)
        }
    }

    fun openCamera() {
        when {
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                cameraLauncher.launch(null)
            }
            else -> {
                permissionLauncher.launch(Manifest.permission.CAMERA)
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
                text = "Scan Document",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )
            Text(
                text = "Capture a document and convert it to PDF",
                fontSize = 13.sp,
                color = AppColors.SlateGray
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (capturedBitmap == null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF0F172A), Color(0xFF1E3A8A))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val pad = 48f
                            drawRoundRect(
                                color = Color.White.copy(alpha = 0.15f),
                                topLeft = androidx.compose.ui.geometry.Offset(pad, pad),
                                size = androidx.compose.ui.geometry.Size(size.width - pad * 2, size.height - pad * 2),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f),
                                style = Stroke(width = 2f)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Outlined.CameraAlt, null, tint = Color.White, modifier = Modifier.size(52.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Document scanner", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
                            Text("Align page inside the frame", fontSize = 12.sp, color = Color.White.copy(alpha = 0.75f))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { openCamera() },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.DarkBlue)
                    ) {
                        Icon(Icons.Outlined.CameraAlt, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Camera", fontWeight = FontWeight.SemiBold)
                    }
                    OutlinedButton(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Outlined.PhotoLibrary, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Gallery", fontWeight = FontWeight.SemiBold)
                    }
                }
            } else {
                val source = capturedBitmap!!
                val rotated = remember(source, rotationSteps) { rotateBitmap(source, rotationSteps) }
                val fitRect = remember(imageSize, rotated) {
                    calculateFitRect(imageSize.width, imageSize.height, rotated.width, rotated.height)
                }
                val displayBitmap = remember(rotated, flatBitmap, filterMode) {
                    val base = flatBitmap ?: rotated
                    if (flatBitmap == null) base
                    else when (filterMode) {
                        "grayscale" -> applyGrayscale(base)
                        "enhanced" -> applyEnhanced(base)
                        else -> base
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black)
                        .onGloballyPositioned { imageSize = IntSize(it.size.width, it.size.height) },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = displayBitmap.asImageBitmap(),
                        contentDescription = "Scanned document",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )

                    if (flatBitmap == null && cornerPoints.isNotEmpty()) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            if (cornerPoints.size == 4) {
                                val path = Path().apply {
                                    moveTo(cornerPoints[0].x, cornerPoints[0].y)
                                    lineTo(cornerPoints[1].x, cornerPoints[1].y)
                                    lineTo(cornerPoints[2].x, cornerPoints[2].y)
                                    lineTo(cornerPoints[3].x, cornerPoints[3].y)
                                    close()
                                }
                                drawPath(path, Color(0xFF09D7C4), style = Stroke(width = 3f))
                            }
                            cornerPoints.forEach { pt ->
                                drawCircle(Color(0xFF09D7C4), 12f, pt)
                                drawCircle(Color.White, 8f, pt)
                            }
                        }
                    }

                    if (flatBitmap == null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(cornerPoints.size) {
                                    detectTapGestures { offset ->
                                        if (cornerPoints.size < 4) {
                                            cornerPoints = cornerPoints + offset
                                        }
                                    }
                                }
                                .pointerInput(cornerPoints) {
                                    if (cornerPoints.size == 4) {
                                        detectDragGestures { change, dragAmount ->
                                            change.consume()
                                            val nearest = cornerPoints.withIndex().minByOrNull {
                                                (it.value - change.position).getDistance()
                                            }
                                            if (nearest != null) {
                                                cornerPoints = cornerPoints.toMutableList().apply {
                                                    set(nearest.index, this[nearest.index] + dragAmount)
                                                }
                                            }
                                        }
                                    }
                                }
                        )
                    }
                }

                if (flatBitmap == null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = when {
                            cornerPoints.isEmpty() -> "Tap corners: top-left → top-right → bottom-right → bottom-left"
                            cornerPoints.size < 4 -> "Corner ${cornerPoints.size} of 4"
                            else -> "Drag the dots to fine-tune the crop"
                        },
                        fontSize = 12.sp,
                        color = AppColors.SlateGray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (flatBitmap == null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ScanToolButton(Icons.Outlined.RotateLeft, "Rotate") {
                            rotationSteps = (rotationSteps + 3) % 4
                            cornerPoints = emptyList()
                        }
                        ScanToolButton(Icons.Outlined.Crop, "Auto") {
                            val inset = 0.06f
                            val w = rotated.width.toFloat()
                            val h = rotated.height.toFloat()
                            val bmpCorners = listOf(
                                Offset(w * inset, h * inset),
                                Offset(w * (1 - inset), h * inset),
                                Offset(w * (1 - inset), h * (1 - inset)),
                                Offset(w * inset, h * (1 - inset))
                            )
                            cornerPoints = bmpCorners.map { bitmapOffsetToView(it, fitRect, rotated) }
                        }
                        ScanToolButton(Icons.Outlined.CropFree, "Full") {
                            flatBitmap = rotated.copy(rotated.config ?: Bitmap.Config.ARGB_8888, true)
                        }
                        ScanToolButton(Icons.Outlined.Refresh, "Retake") {
                            capturedBitmap = null
                            flatBitmap = null
                            cornerPoints = emptyList()
                            rotationSteps = 0
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { cornerPoints = emptyList() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("Clear corners") }

                        Button(
                            onClick = {
                                if (cornerPoints.size == 4) {
                                    scope.launch {
                                        val mapped = cornerPoints.map {
                                            viewOffsetToBitmap(it, fitRect, rotated)
                                        }
                                        val result = withContext(Dispatchers.IO) {
                                            applyPerspectiveCorrection(rotated, mapped)
                                        }
                                        flatBitmap = result
                                    }
                                }
                            },
                            enabled = cornerPoints.size == 4,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AppColors.DarkBlue)
                        ) { Text("Crop & straighten") }
                    }
                }

                if (flatBitmap != null) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Filter",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1E293B)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = filterMode == "original",
                            onClick = { filterMode = "original" },
                            label = { Text("Original") },
                            shape = RoundedCornerShape(8.dp)
                        )
                        FilterChip(
                            selected = filterMode == "grayscale",
                            onClick = { filterMode = "grayscale" },
                            label = { Text("B&W") },
                            shape = RoundedCornerShape(8.dp)
                        )
                        FilterChip(
                            selected = filterMode == "enhanced",
                            onClick = { filterMode = "enhanced" },
                            label = { Text("Enhanced") },
                            shape = RoundedCornerShape(8.dp)
                        )
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

                    if (scanState == ScanState.PROCESSING) {
                        Spacer(modifier = Modifier.height(16.dp))
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
                                    text = "Creating PDF...",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1E293B)
                                )
                            }
                        }
                    }

                    if (scanState == ScanState.DONE) {
                        Spacer(modifier = Modifier.height(16.dp))
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
                                    text = "PDF created successfully",
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
                                            savedPath?.let { path ->
                                                navController.navigate(NavigationRoutes.PdfViewer.open(path))
                                            }
                                        },
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("Open")
                                    }
                                    Button(
                                        onClick = {
                                            capturedBitmap = null
                                            flatBitmap = null
                                            cornerPoints = emptyList()
                                            scanState = ScanState.IDLE
                                            savedUri = null
                                            outputName = "scanned_document"
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.DarkBlue)
                                    ) {
                                        Text("Scan Another")
                                    }
                                }
                            }
                        }
                    }

                    if (scanState == ScanState.ERROR) {
                        Spacer(modifier = Modifier.height(16.dp))
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
                    onClick = { openCamera() },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, AppColors.DarkBlue),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.DarkBlue)
                ) {
                    Icon(
                        Icons.Outlined.CameraAlt,
                        contentDescription = "Camera",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Camera", fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = {
                        if (scanState == ScanState.DONE || scanState == ScanState.ERROR) {
                            capturedBitmap = null
                            flatBitmap = null
                            cornerPoints = emptyList()
                            scanState = ScanState.IDLE
                            savedUri = null
                            outputName = "scanned_document"
                        } else {
                            val base = flatBitmap ?: return@Button
                            scanState = ScanState.PROCESSING
                            scope.launch {
                                val result = withContext(Dispatchers.IO) {
                                    try {
                                        val finalBitmap = when (filterMode) {
                                            "grayscale" -> applyGrayscale(base)
                                            "enhanced" -> applyEnhanced(base)
                                            else -> base
                                        }
                                        saveBitmapAsPdf(context, finalBitmap, outputName.ifBlank { "scanned" })
                                    } catch (e: Exception) {
                                        null
                                    }
                                }
                                if (result != null) {
                                    savedUri = result.shareUri
                                    savedPath = result.file.absolutePath
                                    scanState = ScanState.DONE
                                } else {
                                    errorMessage = "Failed to create PDF. Please try again."
                                    scanState = ScanState.ERROR
                                }
                            }
                        }
                    },
                    enabled = flatBitmap != null && scanState != ScanState.PROCESSING,
                    modifier = Modifier
                        .weight(2f)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (scanState == ScanState.DONE) AppColors.SuccessGreen else AppColors.DarkBlue,
                        disabledContainerColor = AppColors.LightSlate
                    )
                ) {
                    if (scanState == ScanState.DONE) {
                        Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Scan Again", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(
                            Icons.Outlined.PictureAsPdf,
                            contentDescription = "Save",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save as PDF", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ScanToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, label, modifier = Modifier.size(20.dp), tint = AppColors.DarkBlue)
        Text(label, fontSize = 10.sp, color = AppColors.SlateGray)
    }
}

private fun Offset.getDistance(): Float {
    val dx = x
    val dy = y
    return kotlin.math.sqrt(dx * dx + dy * dy)
}

private fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    return try {
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
    } catch (_: Exception) {
        null
    }
}

private fun rotateBitmap(source: Bitmap, steps: Int): Bitmap {
    if (steps == 0) return source
    val matrix = Matrix().apply { postRotate(90f * steps) }
    return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
}

private fun calculateFitRect(viewW: Int, viewH: Int, bmpW: Int, bmpH: Int): RectF {
    if (viewW <= 0 || viewH <= 0) return RectF(0f, 0f, bmpW.toFloat(), bmpH.toFloat())
    val viewRatio = viewW / viewH.toFloat()
    val bmpRatio = bmpW / bmpH.toFloat()
    return if (bmpRatio > viewRatio) {
        val h = viewW / bmpRatio
        val top = (viewH - h) / 2f
        RectF(0f, top, viewW.toFloat(), top + h)
    } else {
        val w = viewH * bmpRatio
        val left = (viewW - w) / 2f
        RectF(left, 0f, left + w, viewH.toFloat())
    }
}

private fun viewOffsetToBitmap(offset: Offset, rect: RectF, bitmap: Bitmap): Offset {
    if (rect.width() <= 0f || rect.height() <= 0f) return offset
    val x = ((offset.x - rect.left) / rect.width() * bitmap.width).coerceIn(0f, bitmap.width.toFloat())
    val y = ((offset.y - rect.top) / rect.height() * bitmap.height).coerceIn(0f, bitmap.height.toFloat())
    return Offset(x, y)
}

private fun bitmapOffsetToView(offset: Offset, rect: RectF, bitmap: Bitmap): Offset {
    val x = rect.left + offset.x / bitmap.width * rect.width()
    val y = rect.top + offset.y / bitmap.height * rect.height()
    return Offset(x, y)
}

private fun applyPerspectiveCorrection(bitmap: Bitmap, corners: List<Offset>): Bitmap {
    val width = bitmap.width
    val height = bitmap.height

    val src = floatArrayOf(
        corners[0].x, corners[0].y,
        corners[1].x, corners[1].y,
        corners[2].x, corners[2].y,
        corners[3].x, corners[3].y
    )

    val maxDist = corners.maxOf { it.getDistance() }
    val outWidth = (maxDist * 1.5f).toInt().coerceIn(100, 2000)
    val outHeight = (outWidth * height.toFloat() / width.toFloat()).toInt().coerceIn(100, 2000)

    val dst = floatArrayOf(
        0f, 0f,
        outWidth.toFloat(), 0f,
        outWidth.toFloat(), outHeight.toFloat(),
        0f, outHeight.toFloat()
    )

    val matrix = Matrix()
    matrix.setPolyToPoly(src, 0, dst, 0, 4)

    return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true)
}

private fun applyGrayscale(bitmap: Bitmap): Bitmap {
    val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(result)
    val paint = Paint()
    val matrix = android.graphics.ColorMatrix()
    matrix.setSaturation(0f)
    paint.colorFilter = android.graphics.ColorMatrixColorFilter(matrix)
    canvas.drawBitmap(bitmap, 0f, 0f, paint)
    return result
}

private fun applyEnhanced(bitmap: Bitmap): Bitmap {
    val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(result)
    val paint = Paint()
    val matrix = android.graphics.ColorMatrix()
    matrix.setSaturation(0f)
    matrix.postConcat(android.graphics.ColorMatrix(floatArrayOf(
        1.2f, 0f, 0f, 0f, -20f,
        0f, 1.2f, 0f, 0f, -20f,
        0f, 0f, 1.2f, 0f, -20f,
        0f, 0f, 0f, 1f, 0f
    )))
    paint.colorFilter = android.graphics.ColorMatrixColorFilter(matrix)
    canvas.drawBitmap(bitmap, 0f, 0f, paint)
    return result
}

private fun saveBitmapAsPdf(
    context: Context,
    bitmap: Bitmap,
    pdfName: String
): com.example.imagetopdf.core.utils.PdfSaveHelper.SaveResult? {
    val document = PdfDocument()
    try {
        val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, 1).create()
        val page = document.startPage(pageInfo)
        page.canvas.drawBitmap(bitmap, 0f, 0f, null)
        document.finishPage(page)
        val bytes = java.io.ByteArrayOutputStream().also { document.writeTo(it) }.toByteArray()
        return com.example.imagetopdf.core.utils.PdfSaveHelper.savePdfBytes(context, bytes, pdfName)
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    } finally {
        document.close()
    }
}
