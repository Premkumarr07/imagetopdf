        package com.example.imagetopdf.features.home.ui

        import androidx.compose.foundation.Image
        import androidx.compose.foundation.background
        import androidx.compose.foundation.clickable
        import androidx.compose.foundation.layout.*
        import androidx.compose.foundation.rememberScrollState
        import androidx.compose.foundation.shape.RoundedCornerShape
        import androidx.compose.foundation.verticalScroll
        import androidx.compose.material.icons.Icons
        import androidx.compose.material.icons.filled.MoreVert
        import androidx.compose.material.icons.outlined.Delete
        import androidx.compose.material.icons.outlined.Edit
        import androidx.compose.material.icons.outlined.Lock
        import androidx.compose.material.icons.outlined.Share
        import androidx.compose.material3.*
        import androidx.compose.material3.CardDefaults
        import androidx.compose.runtime.Composable
        import androidx.compose.runtime.LaunchedEffect
        import androidx.compose.runtime.collectAsState
        import androidx.compose.runtime.getValue
        import androidx.compose.runtime.mutableStateOf
        import androidx.compose.runtime.remember
        import androidx.compose.runtime.setValue
        import androidx.compose.ui.Alignment
        import androidx.compose.ui.Modifier
        import androidx.compose.ui.draw.clip
        import androidx.compose.ui.graphics.Brush
        import androidx.compose.ui.graphics.Color
        import androidx.compose.ui.graphics.painter.Painter
        import androidx.compose.ui.res.painterResource
        import androidx.compose.ui.text.font.FontWeight
        import androidx.compose.ui.text.style.TextAlign
        import androidx.compose.ui.unit.dp
        import androidx.compose.ui.unit.sp
        import androidx.hilt.navigation.compose.hiltViewModel
        import androidx.lifecycle.viewmodel.compose.viewModel
        import androidx.navigation.NavController
        import com.example.imagetopdf.R
        import com.example.imagetopdf.constants.AppColors
        import com.example.imagetopdf.features.home.model.PdfFileModel
        import com.example.imagetopdf.features.home.viewmodel.HomeViewModel
        import com.example.imagetopdf.navigation.NavigationRoutes
        import androidx.lifecycle.compose.collectAsStateWithLifecycle


        @Composable
        fun HomeScreen(navController: NavController,
                       viewModel: HomeViewModel = hiltViewModel()) {
            val scrollState = rememberScrollState()
            val uiState by viewModel.uiState.collectAsState()
            LaunchedEffect(Unit) { viewModel.loadFiles() }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppColors.Background)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                        .verticalScroll(scrollState)
                ) {
                    Spacer(modifier = Modifier.height(18.dp))

                    QuickToolsSection(
                        primaryTeal   = AppColors.BgColor,
                        navController = navController
                    )

                    Spacer(modifier = Modifier.height(32.dp))
                    AppFolderBanner()
                    Spacer(modifier = Modifier.height(20.dp))

                    RecentFilesSection(
                        files = uiState.pdfFiles,  isLoading = uiState.isLoading,
                        onRefresh = { viewModel.loadFiles() },
                        onShare = { viewModel.shareFile(it) },
                        onOpen = { viewModel.openFile(it) },
                        onDelete = { viewModel.deleteFile(it) }
                        )

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        @Composable
        private fun AppFolderBanner(){
            Card (modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(contentColor = Color.White),
                elevation = CardDefaults.cardElevation(1.dp)
            ){
                Row(modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(42.dp).clip(RoundedCornerShape(10.dp)).background(Brush.linearGradient(listOf(AppColors.TopBarDarkBlue,
                            AppColors.TealColor))),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.folderopen),
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Documents/ImageTOPDF", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AppColors.TextPrimary)
                            Text("All converted PDFs are saved here", fontSize = 12.sp, color= AppColors.SlateGray)
                        }

                }
            }
        }

        @Composable
        fun QuickToolsSection(primaryTeal: Color, navController: NavController) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                // Top row: Large card + two small cards
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Large — Images → PDF
                    Card(
                        modifier = Modifier
                            .weight(1.1f)
                            .fillMaxHeight()
                            .clickable { navController.navigate(NavigationRoutes.ImageToPdf.route) },
                        shape  = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = primaryTeal),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)

                    ) {
                        Column(
                            modifier             = Modifier.fillMaxSize().padding(16.dp),
                            verticalArrangement  = Arrangement.Center,
                            horizontalAlignment  = Alignment.CenterHorizontally
                        ) {
                            Image(
                                painter            = painterResource(id = R.drawable.homeimagetopdf),
                                contentDescription = null,
                                modifier           = Modifier.size(120.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text       = "Select Image",
                                fontSize   = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color      = Color.Black
                            )
                        }
                    }

                    Column(
                        modifier            = Modifier.weight(1f).fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SmallToolCard(
                            iconRes  = R.drawable.qr,
                            title    = "SCAN DOC",
                            modifier = Modifier.weight(1f),
                            onClick  = { navController.navigate(NavigationRoutes.ScanDoc.route) }
                        )
                        SmallToolCard(
                            iconRes  = R.drawable.compression,
                            title    = "COMPRESS",
                            modifier = Modifier.weight(1f),
                            onClick  = { navController.navigate(NavigationRoutes.Compress.route) }
                        )
                    }
                }

                // Bottom row: Encrypt + Merge
                Row(
                    modifier            = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SmallToolCard(
                        iconRes  = R.drawable.encrypt,
                        title    = "ENCRYPT",
                        modifier = Modifier.weight(1f).height(80.dp),
                        onClick  = { navController.navigate(NavigationRoutes.Encrypt.route) }
                    )
                    SmallToolCard(
                        iconRes  = R.drawable.merge,
                        title    = "MERGE PDFs",
                        modifier = Modifier.weight(1f).height(80.dp),
                        onClick  = { navController.navigate(NavigationRoutes.MergePdf.route) }
                    )
                }
            }
        }

        @Composable
        fun SmallToolCard(
            iconRes:  Int,
            title:    String,
            subtitle: String?  = null,
            modifier: Modifier = Modifier,
            onClick:  () -> Unit = {}
        ) {
            Card(
                modifier = modifier.clickable { onClick() },
                shape    = RoundedCornerShape(12.dp),
                colors   = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier            = Modifier.fillMaxSize().padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter            = painterResource(id = iconRes),
                        contentDescription = title,
                        modifier           = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text       = title,
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = Color(0xFF1E293B)
                    )
                    if (subtitle != null) {
                        Text(
                            text       = subtitle,
                            fontSize   = 10.sp,
                            color      = Color(0xFF64748B),
                            lineHeight = 12.sp
                        )
                    }
                }
            }
        }
        @Composable
        fun RecentFilesSection(files: List<PdfFileModel>, isLoading: Boolean, onRefresh: () -> Unit,onShare: (PdfFileModel) -> Unit,
                               onOpen: (PdfFileModel) -> Unit,
                               onDelete: (PdfFileModel) -> Unit) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Recent Files", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onRefresh, modifier = Modifier.size(32.dp)) {
                        Image(
                            painter = painterResource(id = R.drawable.refresh),
                            "Refresh",
                            modifier = Modifier.size(18.dp)
                        )            }
                    TextButton(onClick = {}) { Text("See all", color = AppColors.TealColor) }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            when {
                isLoading    -> repeat(3) { ShimmerFileItem(); Spacer(modifier = Modifier.height(8.dp)) }
                files.isEmpty() -> EmptyFilesState()
                else         -> files.take(5).forEach { file -> RecentFileItem(file = file, onShare = { onShare(file) }, onOpen = { onOpen(file) }, onDelete = { onDelete(file) }); Spacer(modifier = Modifier.height(8.dp)) }
            }
        }



            @Composable
            fun RecentFileItem(file: PdfFileModel,  onShare: () -> Unit,
                               onOpen: () -> Unit,
                               onDelete: () -> Unit) {
                var showMenu by remember { mutableStateOf(false) }
                Card(modifier = Modifier.fillMaxWidth().height(76.dp), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp)) {
                    Row(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(
                            AppColors.IndigoBg), contentAlignment = Alignment.Center) {
                            Text("PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AppColors.Indigo)
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(file.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFF1E293B), maxLines = 1)
                            Text(file.dateLabel, fontSize = 12.sp, color = Color(0xFF94A3B8))
                        }
                        Text(file.sizeLabel, fontSize = 12.sp, color = Color(0xFF94A3B8))
                        Spacer(modifier = Modifier.width(8.dp))
                        Box {
                            IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.MoreVert, "More", tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                            }
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                DropdownMenuItem(text = { Text("Share") }, onClick = { showMenu = false }, leadingIcon = { Icon(Icons.Outlined.Share, null) })
                                DropdownMenuItem(text = { Text("Open") }, onClick = { showMenu = false }, leadingIcon = { Icon(Icons.Outlined.Edit, null) })
                                DropdownMenuItem(text = { Text("Delete", color = Color(0xFFEF4444)) }, onClick = { showMenu = false }, leadingIcon = { Icon(Icons.Outlined.Delete, null, tint = Color(0xFFEF4444)) })
                            }
                        }
                    }
                }
            }


        @Composable
        private fun ShimmerFileItem() {
            Card(modifier = Modifier.fillMaxWidth().height(76.dp), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp)) {
                Row(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(AppColors.LightSlate))
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.fillMaxWidth(0.6f).height(12.dp).clip(RoundedCornerShape(6.dp)).background(AppColors.LightSlate))
                        Box(modifier = Modifier.fillMaxWidth(0.35f).height(10.dp).clip(RoundedCornerShape(6.dp)).background(AppColors.LightSlate))
                    }
                    Box(modifier = Modifier.width(40.dp).height(10.dp).clip(RoundedCornerShape(6.dp)).background(AppColors.LightSlate))
                }
            }
        }

        @Composable
        private fun EmptyFilesState() {
            Box(modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(16.dp)).background(
                AppColors.LightSlate.copy(alpha = .4f)), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = painterResource(id = R.drawable.folderopen),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("No PDFs yet", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B))
                    Text("Convert images to get started", fontSize = 13.sp, color = AppColors.SlateGray, textAlign = TextAlign.Center)
                }
            }
        }