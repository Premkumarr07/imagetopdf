package com.example.imagetopdf.features.myfiles.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.imagetopdf.R
import com.example.imagetopdf.constants.AppColors
import com.example.imagetopdf.core.utils.openPdf
import com.example.imagetopdf.features.home.model.PdfFileModel
import com.example.imagetopdf.features.myfiles.viewmodel.MyFilesViewModel


enum class SortMode(val label: String) {
    DATE_DESC("Newest"),
    DATE_ASC("Oldest"),
    NAME_ASC("Name A–Z"),
    SIZE_DESC("Largest")
}

@Composable
fun MyFilesScreen(
    navController: NavController,
    viewModel: MyFilesViewModel = viewModel()
) {
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            viewModel.addPdfFromUri(it, context)
        }
    }
    val files by viewModel.pdfFiles.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val sortMode by viewModel.sortMode.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.loadFiles() }
    Box(modifier = Modifier.fillMaxSize()) {

        Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF8FAFC))) {

            SearchBar(query = searchQuery, onChange = { viewModel.onSearchQueryChange(it) })

            StorageInfoCard(fileCount = files.size)

            SortRow(
                fileCount = files.size,
                sortMode = sortMode,
                onSort = { viewModel.onSortChange(it) },
                onRefresh = { viewModel.loadFiles() }
            )

            when {
                isLoading -> ShimmerList()
                files.isEmpty() && searchQuery.isNotEmpty() -> NoSearchResults(searchQuery)
                files.isEmpty() -> EmptyFolderState()
                else -> FileList(files = files, onDelete = { viewModel.deleteFile(it) })
            }


        }
        AddPdfFab(
            onUploadClick = {
                launcher.launch("application/pdf")
            },
            modifier = Modifier.align(Alignment.BottomEnd)
                .padding(16.dp)
        )
    }
}

@Composable
private fun SearchBar(query: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value         = query,
        onValueChange = onChange,
        modifier      = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        placeholder   = { Text("Search PDFs…", color = AppColors.SlateGray) },
        leadingIcon   = { Icon(Icons.Outlined.Search, null, tint = AppColors.SlateGray) },
        trailingIcon  = {
            AnimatedVisibility(visible = query.isNotEmpty()) {
                IconButton(onClick = { onChange("") }) {
                    Icon(Icons.Outlined.Close, "Clear", tint = AppColors.SlateGray)
                }
            }
        },
        singleLine = true,
        shape      = RoundedCornerShape(14.dp),
        colors     = OutlinedTextFieldDefaults.colors(
            focusedBorderColor      = AppColors.TealColor,
            unfocusedBorderColor    = AppColors.LightSlate,
            focusedContainerColor   = Color.White,
            unfocusedContainerColor = Color.White
        )
    )
}
@Composable
private fun StorageInfoCard(fileCount: Int) {
    Card(
        modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(AppColors.TopBarDarkBlue, Color(0xFF1D4ED8))))
                .padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = .15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.folderopen),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        colorFilter = ColorFilter.tint(Color.White)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Documents / ImageToPDF", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    Text("$fileCount PDF file${if (fileCount != 1) "s" else ""}", fontSize = 12.sp, color = Color.White.copy(alpha = .75f))
                }
                Icon(Icons.Outlined.KeyboardArrowRight, null, tint = Color.White.copy(alpha = .6f), modifier = Modifier.size(20.dp))
            }
        }
    }
}


@Composable
private fun SortRow(fileCount: Int, sortMode: SortMode, onSort: (SortMode) -> Unit, onRefresh: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text("$fileCount file${if (fileCount != 1) "s" else ""}", fontSize = 13.sp, color = AppColors.SlateGray, fontWeight = FontWeight.Medium)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(onClick = onRefresh, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Outlined.Refresh, "Refresh", tint = AppColors.TealColor, modifier = Modifier.size(18.dp))
            }
            Box {
                OutlinedButton(
                    onClick        = { expanded = true },
                    shape          = RoundedCornerShape(10.dp),
                    border         = androidx.compose.foundation.BorderStroke(1.dp, AppColors.LightSlate),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier       = Modifier.height(34.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.swapvert),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        colorFilter = ColorFilter.tint(AppColors.SlateGray)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(sortMode.label, fontSize = 12.sp, color = AppColors.SlateGray)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    SortMode.entries.forEach { mode ->
                        DropdownMenuItem(
                            text    = { Text(mode.label, fontWeight = if (mode == sortMode) FontWeight.Bold else FontWeight.Normal, color = if (mode == sortMode) AppColors.TealColor else Color(0xFF1E293B)) },
                            onClick = { onSort(mode); expanded = false }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FileList(
    files: List<PdfFileModel>,
    onDelete: (PdfFileModel) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(files, key = { it.path }) { file ->
            FileItem(
                file = file,
                onDelete = { onDelete(file) }
            )
        }
    }
}

@Composable
private fun FileItem(file: PdfFileModel, onDelete: () -> Unit) {
    val context = LocalContext.current

    var showMenu by remember {
        mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth().
    clickable {
        openPdf(file.path, context)
    }, shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(52.dp).clip(RoundedCornerShape(10.dp)).background(AppColors.IndigoBg), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = painterResource(id = R.drawable.folderopen),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        colorFilter = ColorFilter.tint(AppColors.Indigo)
                    )
                    Text("PDF", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = AppColors.Indigo)
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(file.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFF1E293B), maxLines = 1)
                Spacer(modifier = Modifier.height(3.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetaChip(Icons.Outlined.DateRange, file.dateLabel)
                    MetaChip(Icons.Outlined.DateRange, file.sizeLabel)
                }
            }
            Box {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.MoreVert, "More", tint = Color(0xFF94A3B8), modifier = Modifier.size(20.dp))
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(text = { Text("Open") },   onClick = { showMenu = false }, leadingIcon = { Icon(Icons.Outlined.Add, null) })
                    DropdownMenuItem(text = { Text("Share") },  onClick = { showMenu = false }, leadingIcon = { Icon(Icons.Outlined.Share, null) })
                    DropdownMenuItem(text = { Text("Rename") }, onClick = { showMenu = false }, leadingIcon = { Icon(Icons.Outlined.Edit, null) })
                    HorizontalDivider()
                    DropdownMenuItem(text = { Text("Delete", color = AppColors.RedDelete) }, onClick = { showMenu = false; onDelete() }, leadingIcon = { Icon(Icons.Outlined.Delete, null, tint = AppColors.RedDelete) })
                }
            }
        }
    }
}

@Composable
private fun MetaChip(icon: ImageVector, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Icon(icon, null, tint = AppColors.SlateGray, modifier = Modifier.size(11.dp))
        Text(label, fontSize = 11.sp, color = AppColors.SlateGray)
    }
}
@Composable
private fun EmptyFolderState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Image(
                painter = painterResource(id = R.drawable.folderopen),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                colorFilter = ColorFilter.tint(AppColors.LightSlate)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("No PDFs yet", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
            Spacer(modifier = Modifier.height(6.dp))
            Text("Files you convert will appear here.\nSaved to Documents/ImageToPDF/", fontSize = 13.sp, color = AppColors.SlateGray, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun NoSearchResults(query: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Icon(Icons.Outlined.Search, null, tint = AppColors.LightSlate, modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text("No results for \"$query\"", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B))
            Spacer(modifier = Modifier.height(6.dp))
            Text("Try a different file name", fontSize = 13.sp, color = AppColors.SlateGray)
        }
    }
}

@Composable
private fun ShimmerList() {
    LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(6) { ShimmerItem() }
    }
}

@Composable
private fun ShimmerItem() {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(52.dp).clip(RoundedCornerShape(10.dp)).background(AppColors.LightSlate))
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.fillMaxWidth(0.65f).height(13.dp).clip(RoundedCornerShape(6.dp)).background(
                    AppColors.LightSlate))
                Box(modifier = Modifier.fillMaxWidth(0.4f).height(10.dp).clip(RoundedCornerShape(6.dp)).background(
                    AppColors.LightSlate))
            }
        }
    }
}


@Composable
fun AddPdfFab(  onUploadClick: () -> Unit,
                modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {

        FloatingActionButton(
            onClick = { expanded = !expanded },
            containerColor = AppColors.TealColor
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add PDF")
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {

            DropdownMenuItem(
                text = { Text("Scan PDF") },
                leadingIcon = {
                    Image(
                    painter            = painterResource(R.drawable.qr),
                    contentDescription = "SCAN DOC",
                    modifier           = Modifier.size(32.dp)
                )},
                onClick = {
                    expanded = false
                    // TODO: Open camera scanner
                }
            )

            DropdownMenuItem(
                text = { Text("Upload from Device") },
                leadingIcon = { Icon(Icons.Outlined.Add, null) },
                onClick = {
                    expanded = false
                    onUploadClick()
                    // TODO: Open file picker
                }

            )

            DropdownMenuItem(
                text = { Text("Create Blank PDF") },
                leadingIcon = { Icon(Icons.Outlined.Add, null) },
                onClick = {
                    expanded = false
                    //   TODO: Create new PDF
                }
            )
        }
    }
}