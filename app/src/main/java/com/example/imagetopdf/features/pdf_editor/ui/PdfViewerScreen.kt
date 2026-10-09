package com.example.imagetopdf.features.pdf_editor.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import com.example.imagetopdf.R
import com.example.imagetopdf.constants.AppColors
import com.example.imagetopdf.core.utils.PdfMerge
import com.example.imagetopdf.core.utils.PdfSaveHelper
import com.example.imagetopdf.navigation.NavigationRoutes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

private val pdfRenderMutex = Mutex()

private val ReaderBgTop = Color(0xFF0B1220)
private val ReaderBgBottom = Color(0xFF1A2332)
private val ReaderSurface = Color(0xFF1E293B)
private val ReaderAccent = Color(0xFF38BDF8)
private val ReaderMuted = Color(0xFF94A3B8)

private data class ViewerBreadcrumb(
    val parentLabel: String,
    val onParentClick: () -> Unit
)

private fun resolveViewerBreadcrumb(navController: NavController): ViewerBreadcrumb {
    val previousRoute = navController.previousBackStackEntry?.destination?.route
    return when (previousRoute) {
        NavigationRoutes.Home.route -> ViewerBreadcrumb("Home") { navController.popBackStack() }
        NavigationRoutes.MyFiles.route -> ViewerBreadcrumb("My Files") { navController.popBackStack() }
        NavigationRoutes.Tools.route -> ViewerBreadcrumb("Tools") { navController.popBackStack() }
        NavigationRoutes.ImageToPdf.route -> ViewerBreadcrumb("Images → PDF") { navController.popBackStack() }
        NavigationRoutes.ScanDoc.route -> ViewerBreadcrumb("Scan") { navController.popBackStack() }
        else -> ViewerBreadcrumb("Library") { navController.popBackStack() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    navController: NavController,
    initialPath: String? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val breadcrumb = remember(navController) { resolveViewerBreadcrumb(navController) }
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val maxRenderWidthPx = remember {
        (context.resources.displayMetrics.widthPixels * 1.35f).roundToInt()
    }

    var selectedFile by remember { mutableStateOf<File?>(null) }
    var isOpeningDocument by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var visiblePage by remember { mutableIntStateOf(0) }
    var fileName by remember { mutableStateOf("") }
    var fileSizeLabel by remember { mutableStateOf("") }
    var pageCount by remember { mutableIntStateOf(0) }
    var chromeVisible by remember { mutableStateOf(true) }
    var showPageGrid by remember { mutableStateOf(false) }
    var showGoToPage by remember { mutableStateOf(false) }
    var goToPageInput by remember { mutableStateOf("1") }

    val pageBitmaps = remember { mutableStateMapOf<Int, Bitmap>() }
    val thumbBitmaps = remember { mutableStateMapOf<Int, Bitmap>() }
    var loadingPages by remember { mutableStateOf(setOf<Int>()) }

    fun clearDocument() {
        pageBitmaps.values.forEach { it.recycle() }
        thumbBitmaps.values.forEach { it.recycle() }
        pageBitmaps.clear()
        thumbBitmaps.clear()
        selectedFile = null
        loadError = null
        fileName = ""
        fileSizeLabel = ""
        pageCount = 0
        loadingPages = emptySet()
        isOpeningDocument = false
        visiblePage = 0
    }

    fun downloadCurrentPdf() {
        val file = selectedFile ?: return
        if (isDownloading) return
        isDownloading = true
        scope.launch {
            val saved = withContext(Dispatchers.IO) {
                PdfSaveHelper.exportToDownloads(context, file, fileName.ifBlank { file.nameWithoutExtension })
            }
            isDownloading = false
            snackbarHostState.showSnackbar(
                if (saved) "Saved to Downloads/PDFMaker" else "Could not save to Downloads"
            )
        }
    }

    fun bindFile(file: File) {
        if (!file.exists()) {
            loadError = "This file is no longer on your device."
            selectedFile = null
            isOpeningDocument = false
            pageCount = 0
            return
        }
        pageBitmaps.values.forEach { it.recycle() }
        thumbBitmaps.values.forEach { it.recycle() }
        pageBitmaps.clear()
        thumbBitmaps.clear()
        loadError = null
        isOpeningDocument = true
        visiblePage = 0
        fileName = file.nameWithoutExtension
        fileSizeLabel = formatFileSize(file.length())
        scope.launch {
            val count = withContext(Dispatchers.IO) { PdfMerge.getPageCount(file) }
            if (count <= 0) {
                selectedFile = null
                pageCount = 0
                loadError = "Couldn't read this PDF. It may be password-protected or damaged."
                isOpeningDocument = false
                return@launch
            }
            selectedFile = file
            pageCount = count
            isOpeningDocument = false
            listState.scrollToItem(0)
        }
    }

    LaunchedEffect(initialPath) {
        val path = initialPath?.trim()
        if (!path.isNullOrEmpty()) {
            bindFile(File(path))
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val file = uriToCacheFile(context, it)
            if (file != null) bindFile(file) else loadError = "Could not open that PDF."
        }
    }

    suspend fun ensurePageRendered(index: Int) {
        val file = selectedFile ?: return
        if (index < 0 || index >= pageCount) return
        if (pageBitmaps.containsKey(index) || loadingPages.contains(index)) return
        loadingPages = loadingPages + index
        val bitmap = withContext(Dispatchers.IO) {
            pdfRenderMutex.withLock {
                renderPdfPage(file, index, maxRenderWidthPx)
            }
        }
        if (bitmap != null) {
            pageBitmaps[index] = bitmap
        } else if (index == 0 && pageBitmaps.isEmpty()) {
            loadError = "Couldn't render this PDF. Try opening it in another app."
            selectedFile = null
            pageCount = 0
        }
        loadingPages = loadingPages - index
    }

    LaunchedEffect(selectedFile, pageCount) {
        val file = selectedFile
        if (file == null || pageCount == 0) return@LaunchedEffect
        ensurePageRendered(0)
        if (pageCount > 1) ensurePageRendered(1)
    }

    LaunchedEffect(listState, selectedFile, pageCount) {
        if (selectedFile == null || pageCount == 0) return@LaunchedEffect
        snapshotFlow {
            val info = listState.layoutInfo
            val indices = info.visibleItemsInfo.map { it.index }.toSet()
            val first = info.visibleItemsInfo.firstOrNull()?.index ?: 0
            indices to first
        }.collect { (indices, first) ->
            visiblePage = first.coerceIn(0, pageCount - 1)
            indices.forEach { ensurePageRendered(it) }
            val min = indices.minOrNull() ?: first
            val max = indices.maxOrNull() ?: first
            if (min > 0) ensurePageRendered(min - 1)
            if (max < pageCount - 1) ensurePageRendered(max + 1)
        }
    }

    DisposableEffect(selectedFile) {
        onDispose {
            pageBitmaps.values.forEach { it.recycle() }
            thumbBitmaps.values.forEach { it.recycle() }
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(ReaderBgTop, ReaderBgBottom)))
    ) {
        when {
            loadError != null -> {
                ViewerMessage(
                    title = "Can't open PDF",
                    subtitle = loadError ?: "",
                    onPrimary = { picker.launch("application/pdf") },
                    primaryLabel = "Choose another file"
                )
            }

            selectedFile == null -> {
                ViewerEmptyState(
                    breadcrumb = breadcrumb,
                    onPick = { picker.launch("application/pdf") }
                )
            }

            isOpeningDocument || (selectedFile != null && pageCount == 0) -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ReaderAccent)
                }
            }

            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = { chromeVisible = !chromeVisible })
                        },
                    contentPadding = PaddingValues(
                        top = if (chromeVisible) 112.dp else 24.dp,
                        bottom = if (chromeVisible) 200.dp else 40.dp,
                        start = 12.dp,
                        end = 12.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    items(
                        count = pageCount,
                        key = { it }
                    ) { page ->
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Page ${page + 1}",
                                color = ReaderMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                            )
                            val bitmap = pageBitmaps[page]
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                if (bitmap != null) {
                                    ZoomablePage(
                                        bitmap = bitmap.asImageBitmap(),
                                        pageKey = page
                                    )
                                } else {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(280.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        CircularProgressIndicator(
                                            color = ReaderAccent,
                                            strokeWidth = 3.dp,
                                            modifier = Modifier.size(40.dp)
                                        )
                                        Spacer(Modifier.height(12.dp))
                                        Text(
                                            "Rendering page ${page + 1}",
                                            color = ReaderMuted,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = if (chromeVisible) 180.dp else 48.dp)
                        .navigationBarsPadding()
                )

                AnimatedVisibility(
                    visible = chromeVisible,
                    enter = fadeIn() + slideInVertically { -it / 2 },
                    exit = fadeOut() + slideOutVertically { -it / 2 },
                    modifier = Modifier.align(Alignment.TopCenter)
                ) {
                    ViewerTopBar(
                        breadcrumb = breadcrumb,
                        title = fileName,
                        meta = "$fileSizeLabel · $pageCount pages",
                        onBack = {
                            when {
                                selectedFile != null && initialPath == null -> clearDocument()
                                else -> navController.popBackStack()
                            }
                        },
                        onDownload = { downloadCurrentPdf() },
                        isDownloading = isDownloading,
                        onShare = { selectedFile?.let { sharePdf(context, it) } },
                        onOpenExternal = { selectedFile?.let { openExternal(context, it) } }
                    )
                }

                AnimatedVisibility(
                    visible = chromeVisible,
                    enter = fadeIn() + slideInVertically { it / 2 },
                    exit = fadeOut() + slideOutVertically { it / 2 },
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    ViewerBottomChrome(
                        currentPage = visiblePage,
                        pageCount = pageCount,
                        onDownload = { downloadCurrentPdf() },
                        isDownloading = isDownloading,
                        onShowGrid = {
                            showPageGrid = true
                            scope.launch {
                                for (i in 0 until pageCount.coerceAtMost(24)) {
                                    if (!thumbBitmaps.containsKey(i)) {
                                        val thumb = withContext(Dispatchers.IO) {
                                            selectedFile?.let { file ->
                                                pdfRenderMutex.withLock {
                                                    renderPdfPage(file, i, 180)
                                                }
                                            }
                                        }
                                        if (thumb != null) thumbBitmaps[i] = thumb
                                    }
                                }
                            }
                        },
                        onGoToPage = {
                            goToPageInput = (visiblePage + 1).toString()
                            showGoToPage = true
                        },
                        onPageSelected = { index ->
                            scope.launch { listState.animateScrollToItem(index) }
                        }
                    )
                }

                if (!chromeVisible) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 24.dp)
                            .navigationBarsPadding(),
                        shape = RoundedCornerShape(20.dp),
                        color = ReaderSurface.copy(alpha = 0.92f)
                    ) {
                        Text(
                            text = "${visiblePage + 1} / $pageCount",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }

    if (showGoToPage && pageCount > 0) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showGoToPage = false },
            title = { Text("Go to page") },
            text = {
                androidx.compose.material3.OutlinedTextField(
                    value = goToPageInput,
                    onValueChange = { goToPageInput = it.filter { c -> c.isDigit() }.take(5) },
                    singleLine = true,
                    label = { Text("Page (1–$pageCount)") }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val target = goToPageInput.toIntOrNull()?.minus(1) ?: 0
                        val clamped = target.coerceIn(0, pageCount - 1)
                        scope.launch { listState.animateScrollToItem(clamped) }
                        showGoToPage = false
                    }
                ) { Text("Go") }
            },
            dismissButton = {
                TextButton(onClick = { showGoToPage = false }) { Text("Cancel") }
            }
        )
    }

    if (showPageGrid) {
        ModalBottomSheet(
            onDismissRequest = { showPageGrid = false },
            sheetState = sheetState,
            containerColor = ReaderSurface
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(
                    "All pages",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(Modifier.height(12.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.height(360.dp)
                ) {
                    items((0 until pageCount).toList()) { index ->
                        val selected = index == visiblePage
                        val thumb = thumbBitmaps[index] ?: pageBitmaps[index]
                        Surface(
                            onClick = {
                                scope.launch {
                                    listState.animateScrollToItem(index)
                                    showPageGrid = false
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF0F172A),
                            modifier = Modifier
                                .height(120.dp)
                                .then(
                                    if (selected) Modifier.border(2.dp, ReaderAccent, RoundedCornerShape(10.dp))
                                    else Modifier
                                )
                        ) {
                            Box(Modifier.fillMaxSize()) {
                                if (thumb != null) {
                                    Image(
                                        bitmap = thumb.asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(4.dp)
                                            .clip(RoundedCornerShape(6.dp)),
                                        contentScale = ContentScale.Fit
                                    )
                                } else {
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(22.dp),
                                            color = ReaderAccent,
                                            strokeWidth = 2.dp
                                        )
                                    }
                                }
                                Text(
                                    "${index + 1}",
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(6.dp)
                                        .background(Color.Black.copy(0.55f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                    color = Color.White,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ZoomablePage(
    bitmap: androidx.compose.ui.graphics.ImageBitmap,
    pageKey: Int
) {
    var scale by remember(pageKey) { mutableFloatStateOf(1f) }
    var offset by remember(pageKey) { mutableStateOf(Offset.Zero) }

    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        val newScale = (scale * zoomChange).coerceIn(1f, 4.5f)
        scale = newScale
        if (newScale > 1f) {
            offset += panChange
        } else {
            offset = Offset.Zero
        }
    }

    Image(
        bitmap = bitmap,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(24.dp, RoundedCornerShape(12.dp), clip = false)
            .clip(RoundedCornerShape(12.dp))
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
            }
            .transformable(transformState)
            .pointerInput(pageKey) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > 1.05f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2.5f
                            offset = Offset.Zero
                        }
                    }
                )
            }
    )
}

@Composable
private fun ViewerBreadcrumbRow(breadcrumb: ViewerBreadcrumb) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = breadcrumb.parentLabel,
            color = ReaderAccent,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.clickable { breadcrumb.onParentClick() }
        )
        Icon(
            Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = ReaderMuted,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = "View PDF",
            color = ReaderMuted,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun ViewerTopBar(
    breadcrumb: ViewerBreadcrumb,
    title: String,
    meta: String,
    onBack: () -> Unit,
    onDownload: () -> Unit,
    isDownloading: Boolean,
    onShare: () -> Unit,
    onOpenExternal: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xE60B1220), Color(0x000B1220))
                )
            )
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, null, tint = Color.White)
            }
            ViewerBreadcrumbRow(breadcrumb)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(meta, color = ReaderMuted, fontSize = 12.sp, maxLines = 1)
            }
            IconButton(onClick = onDownload, enabled = !isDownloading) {
                if (isDownloading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = ReaderAccent,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.downarrow),
                        contentDescription = "Save to Downloads",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            IconButton(onClick = onShare) {
                Icon(Icons.Outlined.Share, null, tint = Color.White)
            }
            IconButton(onClick = onOpenExternal) {
                Icon(Icons.AutoMirrored.Outlined.OpenInNew, null, tint = Color.White)
            }
        }
    }
}

@Composable
private fun ViewerBottomChrome(
    currentPage: Int,
    pageCount: Int,
    onDownload: () -> Unit,
    isDownloading: Boolean,
    onShowGrid: () -> Unit,
    onGoToPage: () -> Unit,
    onPageSelected: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0x001A2332), Color(0xF01A2332))
                )
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Page ${currentPage + 1} of $pageCount",
                color = Color.White,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FilledTonalIconButton(
                    onClick = onDownload,
                    enabled = !isDownloading,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = ReaderAccent.copy(alpha = 0.25f),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.height(40.dp)
                ) {
                    if (isDownloading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = ReaderAccent,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.downarrow),
                            contentDescription = "Download",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                IconButton(
                    onClick = onGoToPage,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = ReaderSurface.copy(0.8f)
                    ),
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Outlined.ZoomIn, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                IconButton(
                    onClick = onShowGrid,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = ReaderSurface.copy(0.8f)
                    ),
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Outlined.GridView, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { (currentPage + 1).toFloat() / pageCount.coerceAtLeast(1) },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = ReaderAccent,
            trackColor = ReaderSurface
        )
        Spacer(Modifier.height(8.dp))
        Slider(
            value = currentPage.toFloat(),
            onValueChange = { value ->
                onPageSelected(value.roundToInt().coerceIn(0, pageCount - 1))
            },
            valueRange = 0f..(pageCount - 1).coerceAtLeast(0).toFloat(),
            steps = (pageCount - 2).coerceAtLeast(0),
            colors = SliderDefaults.colors(
                thumbColor = ReaderAccent,
                activeTrackColor = ReaderAccent,
                inactiveTrackColor = ReaderSurface
            )
        )
        Text(
            "Scroll vertically · Pinch or double-tap to zoom",
            color = ReaderMuted,
            fontSize = 11.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun ViewerEmptyState(
    breadcrumb: ViewerBreadcrumb,
    onPick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .align(Alignment.TopStart)
        ) {
            ViewerBreadcrumbRow(breadcrumb)
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        Surface(
            shape = CircleShape,
            color = ReaderSurface,
            modifier = Modifier.size(96.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Outlined.PictureAsPdf,
                    contentDescription = null,
                    tint = ReaderAccent,
                    modifier = Modifier.size(44.dp)
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(
            "Open a PDF",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Scroll through pages vertically, zoom in, and save to Downloads anytime.",
            color = ReaderMuted,
            fontSize = 14.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = onPick,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.DarkBlue)
        ) {
            Icon(painterResource(R.drawable.pdf), null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Browse files", fontWeight = FontWeight.Bold)
        }
        }
    }
}

@Composable
private fun ViewerMessage(
    title: String,
    subtitle: String,
    onPrimary: () -> Unit,
    primaryLabel: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(subtitle, color = ReaderMuted, fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        OutlinedButton(onClick = onPrimary) {
            Text(primaryLabel, color = ReaderAccent)
        }
    }
}

private fun renderPdfPage(file: File, pageIndex: Int, targetWidthPx: Int): Bitmap? {
    return try {
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
            PdfRenderer(fd).use { renderer ->
                if (pageIndex < 0 || pageIndex >= renderer.pageCount) return null
                renderer.openPage(pageIndex).use { page ->
                    val scale = targetWidthPx.toFloat() / page.width.coerceAtLeast(1)
                    val w = (page.width * scale).toInt().coerceAtLeast(1)
                    val h = (page.height * scale).toInt().coerceAtLeast(1)
                    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(android.graphics.Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bitmap
                }
            }
        }
    } catch (_: Exception) {
        null
    }
}

private fun uriToCacheFile(context: android.content.Context, uri: Uri): File? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val file = File(context.cacheDir, "viewer_${System.currentTimeMillis()}.pdf")
        file.outputStream().use { output -> inputStream.copyTo(output) }
        inputStream.close()
        file
    } catch (_: Exception) {
        null
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0)
        bytes >= 1_024 -> "%.0f KB".format(bytes / 1_024.0)
        else -> "$bytes B"
    }
}

private fun sharePdf(context: android.content.Context, file: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share PDF"))
}

private fun openExternal(context: android.content.Context, file: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/pdf")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(Intent.createChooser(intent, "Open with"))
}
