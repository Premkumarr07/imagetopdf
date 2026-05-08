// features/image_to_pdf/ui/components/EmptyPickerCard.kt
package com.example.imagetopdf.features.image_to_pdf.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.imagetopdf.constants.AppColors

@Composable
fun EmptyPickerCard(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFEFF6FF), Color(0xFFE0F2FE))
                )
            )
            .border(
                width = 2.dp,
                brush = Brush.horizontalGradient(
                    listOf(AppColors.DarkBlue.copy(alpha = .3f), AppColors.AccentTeal.copy(alpha = .4f))
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(AppColors.DarkBlue.copy(alpha = .08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Pick images",
                    tint = AppColors.DarkBlue,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Tap to pick images",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.DarkBlue
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "JPG, PNG, WEBP supported",
                fontSize = 13.sp,
                color = AppColors.SlateGray,
                textAlign = TextAlign.Center
            )
        }
    }
}