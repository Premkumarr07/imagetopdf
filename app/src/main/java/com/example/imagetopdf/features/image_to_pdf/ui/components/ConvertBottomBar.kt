// features/image_to_pdf/ui/components/ConvertBottomBar.kt
package com.example.imagetopdf.features.image_to_pdf.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.imagetopdf.R
import com.example.imagetopdf.constants.AppColors

@Composable
fun ConvertBottomBar(
    enabled: Boolean,
    isDone: Boolean,
    imageCount: Int,
    onPickImages: () -> Unit,
    onConvert: () -> Unit
) {
    Surface(shadowElevation = 12.dp, color = AppColors.CardWhite) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onPickImages,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.5.dp, AppColors.DarkBlue),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.DarkBlue)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.photolibrary),
                    contentDescription = "Gallery",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Gallery", fontWeight = FontWeight.SemiBold)
            }

            Button(
                onClick = onConvert,
                enabled = enabled,
                modifier = Modifier
                    .weight(2f)
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDone) AppColors.SuccessGreen else AppColors.DarkBlue,
                    disabledContainerColor = AppColors.LightSlate
                )
            ) {
                if (isDone) {
                    Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Convert Again", fontWeight = FontWeight.Bold)
                } else {
                    Icon(
                        painter = painterResource(id = R.drawable.pdf),
                        contentDescription = "PDF",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (imageCount == 0) "Convert to PDF" else "Convert $imageCount Image${if (imageCount != 1) "s" else ""}",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}