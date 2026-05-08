// features/image_to_pdf/ui/components/ConversionSuccess.kt
package com.example.imagetopdf.features.image_to_pdf.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.imagetopdf.R
import com.example.imagetopdf.constants.AppColors

@Composable
fun ConversionSuccess(
    pdfName: String,
    onDownload: () -> Unit,
    onShare: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
        elevation = CardDefaults.cardElevation(0.dp),
        border = BorderStroke(1.dp, AppColors.SuccessGreen.copy(alpha = .3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(AppColors.SuccessGreen.copy(alpha = .15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.CheckCircle,
                    contentDescription = "Success",
                    tint = AppColors.SuccessGreen,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("PDF created!", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF065F46))
                Text("Saved to Downloads/$pdfName.pdf", fontSize = 12.sp, color = AppColors.SlateGray)
            }
            IconButton(onClick = onDownload) {
                Icon(
                    painter = painterResource(id = R.drawable.downarrow),
                    contentDescription = "Open PDF",
                    modifier = Modifier.size(24.dp)
                )
            }
            IconButton(onClick = onShare) {
                Icon(
                    Icons.Outlined.Share,
                    contentDescription = "Share PDF",
                    tint = AppColors.SuccessGreen,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}