// features/image_to_pdf/ui/components/ImageThumbnailStrip.kt
package com.example.imagetopdf.features.image_to_pdf.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.imagetopdf.constants.AppColors

@Composable
fun ImageThumbnailStrip(
    uris: List<Uri>,
    onRemove: (Uri) -> Unit,
    onAddMore: () -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        itemsIndexed(uris) { index, uri ->
            Box(modifier = Modifier.animateItem()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(uri).crossfade(true).build(),
                    contentDescription = "Image ${index + 1}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(110.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .shadow(4.dp, RoundedCornerShape(14.dp))
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(AppColors.DarkBlue.copy(alpha = .75f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("P${index + 1}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444))
                        .clickable { onRemove(uri) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(14.dp))
                }
            }
        }
        item {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(AppColors.LightSlate.copy(alpha = .5f))
                    .border(2.dp, AppColors.LightSlate, RoundedCornerShape(14.dp))
                    .clickable { onAddMore() },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.Add, contentDescription = "Add more", tint = AppColors.SlateGray, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Add more", fontSize = 11.sp, color = AppColors.SlateGray)
                }
            }
        }
    }
}