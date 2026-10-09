package com.example.imagetopdf.features.home.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.imagetopdf.R
import com.example.imagetopdf.constants.AppColors
import com.example.imagetopdf.core.preferences.UserPreferences
import com.example.imagetopdf.features.home.model.PdfFileModel
import com.example.imagetopdf.features.home.viewmodel.HomeViewModel
import com.example.imagetopdf.navigation.NavigationRoutes

private data class HomeQuickAction(
    val title: String,
    val subtitle: String,
    val iconRes: Int,
    val tint: Color,
    val bg: Color,
    val route: String
)

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val uiState by viewModel.uiState.collectAsState()
    val userName = remember { UserPreferences.getDisplayName(context) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.loadFiles()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val quickActions = remember {
        listOf(
            HomeQuickAction("Scan", "Camera", R.drawable.qr, AppColors.TealGreen, AppColors.TealGreenBg, NavigationRoutes.ScanDoc.route),
            HomeQuickAction("Images", "To PDF", R.drawable.imapdf, AppColors.Orange, AppColors.OrangeBg, NavigationRoutes.ImageToPdf.route),
            HomeQuickAction("Compress", "Smaller file", R.drawable.compression, AppColors.Blue, AppColors.BlueBg, NavigationRoutes.Compress.route),
            HomeQuickAction("Merge", "Combine", R.drawable.merge, AppColors.Purple, AppColors.PurpleBg, NavigationRoutes.MergePdf.route),
            HomeQuickAction("Sign", "eSign PDF", R.drawable.edit, AppColors.Amber, AppColors.AmberBg, NavigationRoutes.EsignPdf.route),
            HomeQuickAction("View", "Open PDF", R.drawable.viewpdf, AppColors.TealGreen, AppColors.TealGreenBg, NavigationRoutes.PdfViewer.route),
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            HomeHeroHeader(
                userName = userName,
                fileCount = uiState.pdfFiles.size,
                onScanClick = { navController.navigate(NavigationRoutes.ScanDoc.route) }
            )

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                "Quick actions",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(10.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(end = 4.dp)
            ) {
                items(quickActions) { action ->
                    QuickActionChip(action) { navController.navigate(action.route) }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FeatureTile(
                    modifier = Modifier.weight(1f),
                    title = "Image to PDF",
                    subtitle = "Gallery photos",
                    gradient = listOf(Color(0xFF0EA5E9), Color(0xFF6366F1)),
                    iconRes = R.drawable.homeimagetopdf,
                    onClick = { navController.navigate(NavigationRoutes.ImageToPdf.route) }
                )
                FeatureTile(
                    modifier = Modifier.weight(1f),
                    title = "Templates",
                    subtitle = "Ready docs",
                    gradient = listOf(AppColors.DarkBlue, Color(0xFF2563EB)),
                    iconRes = R.drawable.viewpdf,
                    onClick = { navController.navigate(NavigationRoutes.Templates.route) }
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            RecentFilesSection(
                files = uiState.pdfFiles,
                isLoading = uiState.isLoading,
                onRefresh = { viewModel.loadFiles() },
                onSeeAll = { navController.navigate(NavigationRoutes.MyFiles.route) },
                onShare = { viewModel.shareFile(it) },
                onOpen = { file ->
                    navController.navigate(NavigationRoutes.PdfViewer.open(file.path))
                },
                onDelete = { viewModel.deleteFile(it) }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HomeHeroHeader(
    userName: String,
    fileCount: Int,
    onScanClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF1E3A8A), Color(0xFF2563EB), Color(0xFF0891B2))
                    ),
                    RoundedCornerShape(24.dp)
                )
                .padding(20.dp)
        ) {
            Column {
                Text(
                    text = "Hi, ${userName.split(" ").firstOrNull() ?: userName}",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = if (fileCount == 0) "Let's create your first PDF" else "$fileCount PDF${if (fileCount == 1) "" else "s"} in your library",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.85f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                        .clickable(onClick = onScanClick)
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.CameraAlt, null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Scan document", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                        Text("Camera or gallery → PDF", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                    }
                    Icon(Icons.Outlined.ChevronRight, null, tint = Color.White.copy(alpha = 0.9f))
                }
            }
        }
    }
}

@Composable
private fun QuickActionChip(action: HomeQuickAction, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(100.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(action.bg),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(action.iconRes),
                    contentDescription = action.title,
                    modifier = Modifier.size(24.dp),
                    colorFilter = ColorFilter.tint(action.tint)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(action.title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(action.subtitle, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

@Composable
private fun FeatureTile(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    gradient: List<Color>,
    iconRes: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(120.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(gradient), RoundedCornerShape(20.dp))
                .padding(14.dp)
        ) {
            Column(modifier = Modifier.align(Alignment.BottomStart)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(subtitle, color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp)
            }
            Image(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.TopEnd),
                alpha = 0.9f
            )
        }
    }
}

@Composable
fun RecentFilesSection(
    files: List<PdfFileModel>,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onSeeAll: () -> Unit,
    onShare: (PdfFileModel) -> Unit,
    onOpen: (PdfFileModel) -> Unit,
    onDelete: (PdfFileModel) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Recent",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onRefresh, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Outlined.Refresh, "Refresh", tint = AppColors.SlateGray, modifier = Modifier.size(20.dp))
            }
            TextButton(onClick = onSeeAll) {
                Text("See all", color = AppColors.TealColor, fontWeight = FontWeight.SemiBold)
            }
        }
    }
    Spacer(modifier = Modifier.height(10.dp))
    when {
        isLoading -> repeat(3) {
            ShimmerFileItem()
            Spacer(modifier = Modifier.height(8.dp))
        }
        files.isEmpty() -> EmptyFilesState()
        else -> files.take(5).forEach { file ->
            RecentFileItem(
                file = file,
                onShare = { onShare(file) },
                onOpen = { onOpen(file) },
                onDelete = { onDelete(file) }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun RecentFileItem(
    file: PdfFileModel,
    onShare: () -> Unit,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(16.dp))
            .clickable { onOpen() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(listOf(Color(0xFFE0E7FF), Color(0xFFC7D2FE)))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AppColors.DarkBlue)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    file.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(file.dateLabel, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("·", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(file.sizeLabel, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Box {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.MoreVert, "More", tint = AppColors.SlateGray, modifier = Modifier.size(20.dp))
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Share") },
                        onClick = { showMenu = false; onShare() },
                        leadingIcon = { Icon(Icons.Outlined.Share, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Open") },
                        onClick = { showMenu = false; onOpen() },
                        leadingIcon = { Icon(Icons.Outlined.OpenInNew, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = AppColors.Error) },
                        onClick = { showMenu = false; onDelete() },
                        leadingIcon = { Icon(Icons.Outlined.Delete, null, tint = AppColors.Error) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ShimmerFileItem() {
    Card(
        modifier = Modifier.fillMaxWidth().height(78.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(50.dp).clip(RoundedCornerShape(12.dp)).background(AppColors.LightSlate))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.fillMaxWidth(0.55f).height(12.dp).clip(RoundedCornerShape(6.dp)).background(AppColors.LightSlate))
                Box(modifier = Modifier.fillMaxWidth(0.35f).height(10.dp).clip(RoundedCornerShape(6.dp)).background(AppColors.LightSlate))
            }
        }
    }
}

@Composable
private fun EmptyFilesState() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(AppColors.LightSlate.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Description, null, tint = AppColors.SlateGray, modifier = Modifier.size(28.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text("No PDFs yet", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(
                "Scan a doc or convert images to get started",
                fontSize = 13.sp,
                color = AppColors.SlateGray,
                textAlign = TextAlign.Center
            )
        }
    }
}
