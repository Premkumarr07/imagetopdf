package com.example.imagetopdf.features.tools.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.imagetopdf.R
import com.example.imagetopdf.constants.AppColors
import com.example.imagetopdf.navigation.NavigationRoutes

data class ToolItem(
    val iconRes:  Int,
    val title:     String,
    val subtitle:  String,
    val iconColor: Color,
    val iconBg:    Color,
    val route:     String? = null
)

data class ToolCategory(val heading: String, val items: List<ToolItem>)

private val popularTools = listOf(
    ToolItem(iconRes  = R.drawable.compression,     "Compress",  "", AppColors.Blue,      AppColors.BlueBg,      NavigationRoutes.Compress.route),
    ToolItem(iconRes  = R.drawable.viewpdf, "View PDF",  "", AppColors.TealGreen, AppColors.TealGreenBg),
    ToolItem(iconRes  = R.drawable.edit,      "Edit PDF",  "", AppColors.Purple,    AppColors.PurpleBg,    NavigationRoutes.PdfEditor.route),
)

private val toolCategories = listOf(
    ToolCategory("Create PDF", listOf(
        ToolItem(iconRes  = R.drawable.imapdf,          "Images → PDF",   "Convert images to a PDF file",      AppColors.Orange,    AppColors.OrangeBg,    NavigationRoutes.ImageToPdf.route),
        ToolItem(iconRes  = R.drawable.qr, "Scan PDF",       "Scan documents with camera",         AppColors.TealGreen, AppColors.TealGreenBg, NavigationRoutes.ScanDoc.route),
    )),
    ToolCategory("Manage PDF", listOf(
        ToolItem(iconRes  = R.drawable.compression,        "Compress",       "Reduce size (mb) of your PDF file",  AppColors.Blue,   AppColors.BlueBg,   NavigationRoutes.Compress.route),
        ToolItem(iconRes  = R.drawable.merge,       "Merge PDFs",     "Join 2 or more PDF files together",  AppColors.Purple, AppColors.PurpleBg, NavigationRoutes.MergePdf.route),
        ToolItem(iconRes  = R.drawable.splitpdf,       "Split PDF",      "Break a PDF into different pages",   AppColors.Orange, AppColors.OrangeBg),
        ToolItem(iconRes  = R.drawable.layer,     "Rearrange PDF",  "Change page order in any PDF",       AppColors.Amber,  AppColors.AmberBg),
        ToolItem(iconRes  = R.drawable.encrypt,            "Encrypt PDF",    "Password protect your PDF",          AppColors.Rose,   AppColors.RoseBg,   NavigationRoutes.Encrypt.route),
    )),
    ToolCategory("Convert from PDF", listOf(
        ToolItem(iconRes  = R.drawable.pdftojpg,          "PDF → JPG",      "Convert a PDF file into JPG images", AppColors.TealGreen, AppColors.TealGreenBg),
        ToolItem(iconRes  = R.drawable.word,     "PDF → Word",     "Convert PDF to editable Word doc",   AppColors.Blue,      AppColors.BlueBg),
    )),
    ToolCategory("Edit PDF", listOf(
        ToolItem(iconRes  = R.drawable.edit,          "Write on PDF",   "Annotate using Pen Tool on PDF",     AppColors.Purple,    AppColors.PurpleBg,    NavigationRoutes.PdfEditor.route),
        ToolItem(iconRes  = R.drawable.edit,      "Highlight PDF",  "Highlight text on a PDF",            AppColors.Amber,     AppColors.AmberBg),
        ToolItem(iconRes  = R.drawable.edit,     "eSign PDF",      "Put your sign on your PDF",          AppColors.TealGreen, AppColors.TealGreenBg),
    )),
)

@Composable
fun ToolsScreen(navController: NavController) {
    LazyColumn(
        modifier       = Modifier.fillMaxSize().background(AppColors.BgColor),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            SectionHeader("Popular")
            Spacer(modifier = Modifier.height(12.dp))
            PopularGrid(tools = popularTools, navController = navController)
            Spacer(modifier = Modifier.height(20.dp))
        }
        item {
            ScanBanner(onClick = { navController.navigate(NavigationRoutes.ScanDoc.route) })
            Spacer(modifier = Modifier.height(24.dp))
        }
        toolCategories.forEach { category ->
            item {
                SectionHeader(category.heading)
                Spacer(modifier = Modifier.height(8.dp))
                CategoryCard(items = category.items, navController = navController)
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B), modifier = Modifier.padding(horizontal = 16.dp))
}

@Composable
private fun PopularGrid(tools: List<ToolItem>, navController: NavController) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        tools.forEach { tool ->
            Card(
                modifier  = Modifier.weight(1f).aspectRatio(1f).clickable { tool.route?.let { navController.navigate(it) } },
                shape     = RoundedCornerShape(16.dp),
                colors    = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(tool.iconBg), contentAlignment = Alignment.Center) {
                            Image(
                                painter = painterResource(id = tool.iconRes),
                                contentDescription = tool.title,
                                modifier = Modifier.size(26.dp),
                                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(tool.iconColor)
                            )
                        }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(tool.title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B))
                }
            }
        }
    }
}

@Composable
private fun ScanBanner(onClick: () -> Unit) {
    Card(
        modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clickable { onClick() },
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(AppColors.TopBarDarkBlue, Color(0xFF0EA5E9))))
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = .15f)), contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(id = R.drawable.qr),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp))
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("SCAN PDF →", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Scan & make your PDF", fontSize = 12.sp, color = Color.White.copy(alpha = .8f))
                }
                Button(
                    onClick        = onClick,
                    shape          = RoundedCornerShape(50.dp),
                    colors         = ButtonDefaults.buttonColors(containerColor = AppColors.TealGreen, contentColor = Color.White),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("SCAN PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun CategoryCard(items: List<ToolItem>, navController: NavController) {
    Card(
        modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column {
            items.forEachIndexed { index, tool ->
                ToolRow(tool = tool) { tool.route?.let { navController.navigate(it) } }
                if (index < items.lastIndex) {
                    HorizontalDivider(modifier = Modifier.padding(start = 72.dp, end = 16.dp), color = AppColors.DivColor, thickness = 1.dp)
                }
            }
        }
    }
}

@Composable
private fun ToolRow(tool: ToolItem, onClick: () -> Unit) {
    Row(
        modifier          = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(tool.iconBg), contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(id = tool.iconRes),
                contentDescription = tool.title,
                modifier = Modifier.size(22.dp),
                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(tool.iconColor)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(tool.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B))
            if (tool.subtitle.isNotEmpty()) Text(tool.subtitle, fontSize = 12.sp, color = AppColors.SlateGray)
        }
        Icon(Icons.Outlined.KeyboardArrowRight, null, tint = Color(0xFFCBD5E1), modifier = Modifier.size(20.dp))
    }
}