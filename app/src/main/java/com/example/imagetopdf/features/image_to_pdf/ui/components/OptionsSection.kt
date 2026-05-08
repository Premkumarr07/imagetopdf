// features/image_to_pdf/ui/components/OptionsSection.kt
package com.example.imagetopdf.features.image_to_pdf.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.imagetopdf.constants.AppColors

@Composable
fun OptionsSection(
    pdfName: String,
    onNameChange: (String) -> Unit,
    selectedSize: String,
    onSizeChange: (String) -> Unit,
    selectedQuality: String,
    onQualityChange: (String) -> Unit
) {
    Column {
        // File Name Input
        Text(
            text = "Output file name",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1E293B)
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = pdfName,
            onValueChange = onNameChange,
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

        Spacer(modifier = Modifier.height(28.dp))

        // Page Size & Quality Options
        Text("Options", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B))
        Spacer(modifier = Modifier.height(10.dp))

        // Page size
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf("A4", "Letter", "Auto").forEach { size ->
                val selected = selectedSize == size
                FilterChip(
                    selected = selected,
                    onClick = { onSizeChange(size) },
                    label = { Text(size, fontSize = 13.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AppColors.DarkBlue,
                        selectedLabelColor = Color.White,
                        containerColor = AppColors.CardWhite,
                        labelColor = AppColors.SlateGray
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        borderColor = AppColors.LightSlate,
                        selectedBorderColor = AppColors.DarkBlue
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Quality
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf("Low", "Medium", "High").forEach { q ->
                val selected = selectedQuality == q
                FilterChip(
                    selected = selected,
                    onClick = { onQualityChange(q) },
                    label = { Text(q, fontSize = 13.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AppColors.AccentTeal,
                        selectedLabelColor = Color.White,
                        containerColor = AppColors.CardWhite,
                        labelColor = AppColors.SlateGray
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        borderColor = AppColors.LightSlate,
                        selectedBorderColor = AppColors.AccentTeal
                    )
                )
            }
        }
    }
}