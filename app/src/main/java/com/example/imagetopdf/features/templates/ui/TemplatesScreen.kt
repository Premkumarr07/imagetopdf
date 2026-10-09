package com.example.imagetopdf.features.templates.ui

import android.content.Context
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import com.example.imagetopdf.constants.AppColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class PdfTemplate(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val iconColor: Color,
    val iconBg: Color,
    val category: String,
    val fields: List<TemplateField>
)

data class TemplateField(
    val key: String,
    val label: String,
    val placeholder: String,
    val singleLine: Boolean = true
)

private val templates = listOf(
    PdfTemplate(
        id = "resume",
        title = "Resume / CV",
        description = "Professional resume template",
        icon = Icons.Outlined.Person,
        iconColor = AppColors.Blue,
        iconBg = AppColors.BlueBg,
        category = "Professional",
        fields = listOf(
            TemplateField("name", "Full Name", "John Doe"),
            TemplateField("title", "Job Title", "Software Engineer"),
            TemplateField("email", "Email", "john@example.com"),
            TemplateField("phone", "Phone", "+1 234 567 890"),
            TemplateField("summary", "Professional Summary", "Experienced developer with...", false),
            TemplateField("experience", "Work Experience", "Company - Role (Year)\nResponsibilities...", false),
            TemplateField("education", "Education", "Degree, University, Year"),
            TemplateField("skills", "Skills", "Kotlin, Java, Python, SQL")
        )
    ),
    PdfTemplate(
        id = "invoice",
        title = "Invoice",
        description = "Business invoice template",
        icon = Icons.Outlined.Receipt,
        iconColor = AppColors.TealGreen,
        iconBg = AppColors.TealGreenBg,
        category = "Business",
        fields = listOf(
            TemplateField("businessName", "Business Name", "Your Company"),
            TemplateField("clientName", "Client Name", "Client Company"),
            TemplateField("invoiceNumber", "Invoice Number", "INV-001"),
            TemplateField("date", "Date", "2024-01-15"),
            TemplateField("items", "Items / Services", "Item 1 - $100\nItem 2 - $200", false),
            TemplateField("total", "Total Amount", "$300"),
            TemplateField("notes", "Notes", "Payment due in 30 days")
        )
    ),
    PdfTemplate(
        id = "letter",
        title = "Formal Letter",
        description = "Business letter format",
        icon = Icons.Outlined.Mail,
        iconColor = AppColors.Purple,
        iconBg = AppColors.PurpleBg,
        category = "Professional",
        fields = listOf(
            TemplateField("senderName", "Your Name", "John Doe"),
            TemplateField("senderAddress", "Your Address", "123 Main St, City"),
            TemplateField("date", "Date", "January 15, 2024"),
            TemplateField("recipientName", "Recipient Name", "Jane Smith"),
            TemplateField("recipientAddress", "Recipient Address", "456 Oak Ave, City"),
            TemplateField("subject", "Subject", "Regarding..."),
            TemplateField("body", "Letter Body", "Dear Sir/Madam,\n\nI am writing to...", false),
            TemplateField("closing", "Closing", "Sincerely,\nJohn Doe")
        )
    ),
    PdfTemplate(
        id = "meeting_notes",
        title = "Meeting Notes",
        description = "Meeting minutes template",
        icon = Icons.Outlined.Notes,
        iconColor = AppColors.Orange,
        iconBg = AppColors.OrangeBg,
        category = "Business",
        fields = listOf(
            TemplateField("meetingTitle", "Meeting Title", "Weekly Standup"),
            TemplateField("date", "Date", "2024-01-15"),
            TemplateField("time", "Time", "10:00 AM - 11:00 AM"),
            TemplateField("attendees", "Attendees", "John, Jane, Bob"),
            TemplateField("agenda", "Agenda", "1. Project updates\n2. Blockers\n3. Next steps", false),
            TemplateField("notes", "Discussion Notes", "Key points discussed...", false),
            TemplateField("actionItems", "Action Items", "1. John to...\n2. Jane to...", false)
        )
    ),
    PdfTemplate(
        id = "report",
        title = "Report",
        description = "Project or research report",
        icon = Icons.Outlined.Description,
        iconColor = AppColors.Rose,
        iconBg = AppColors.RoseBg,
        category = "Academic",
        fields = listOf(
            TemplateField("title", "Report Title", "Project Report"),
            TemplateField("author", "Author", "John Doe"),
            TemplateField("date", "Date", "2024-01-15"),
            TemplateField("abstract", "Abstract", "Brief summary of the report...", false),
            TemplateField("introduction", "Introduction", "Background and context...", false),
            TemplateField("body", "Main Content", "Detailed findings and analysis...", false),
            TemplateField("conclusion", "Conclusion", "Summary and recommendations...", false)
        )
    ),
    PdfTemplate(
        id = "certificate",
        title = "Certificate",
        description = "Achievement certificate",
        icon = Icons.Outlined.EmojiEvents,
        iconColor = AppColors.Amber,
        iconBg = AppColors.AmberBg,
        category = "Professional",
        fields = listOf(
            TemplateField("title", "Certificate Title", "Certificate of Achievement"),
            TemplateField("recipient", "Recipient Name", "John Doe"),
            TemplateField("achievement", "Achievement", "For outstanding performance in..."),
            TemplateField("date", "Date", "January 15, 2024"),
            TemplateField("issuer", "Issued By", "Organization Name"),
            TemplateField("signature", "Signature", "Authorized Signature")
        )
    )
)

@Composable
fun TemplatesScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedTemplate by remember { mutableStateOf<PdfTemplate?>(null) }
    var fieldValues by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var isGenerating by remember { mutableStateOf(false) }
    var generatedUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var showPreview by remember { mutableStateOf(false) }

    if (selectedTemplate != null) {
        TemplateEditor(
            template = selectedTemplate!!,
            fieldValues = fieldValues,
            onFieldChange = { key, value ->
                fieldValues = fieldValues + (key to value)
            },
            onBack = {
                selectedTemplate = null
                fieldValues = emptyMap()
                generatedUri = null
                showPreview = false
            },
            onGenerate = {
                isGenerating = true
                scope.launch {
                    val result = withContext(Dispatchers.IO) {
                        try {
                            val outputFile = File(context.cacheDir, "${selectedTemplate!!.id}_${System.currentTimeMillis()}.pdf")
                            generateTemplatePdf(context, selectedTemplate!!, fieldValues, outputFile)
                            FileProvider.getUriForFile(context, "${context.packageName}.provider", outputFile)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    generatedUri = result
                    isGenerating = false
                    showPreview = result != null
                }
            },
            isGenerating = isGenerating,
            generatedUri = generatedUri,
            showPreview = showPreview
        )
    } else {
        TemplateList(
            templates = templates,
            onSelect = { template ->
                selectedTemplate = template
                fieldValues = template.fields.associate { it.key to "" }
            }
        )
    }
}

@Composable
private fun TemplateList(
    templates: List<PdfTemplate>,
    onSelect: (PdfTemplate) -> Unit
) {
    val categories = templates.groupBy { it.category }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.LightBg),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Templates",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B),
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Text(
                text = "Choose a template and fill in the details",
                fontSize = 13.sp,
                color = AppColors.SlateGray,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(modifier = Modifier.height(20.dp))
        }

        categories.forEach { (category, items) ->
            item {
                Text(
                    text = category,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.SlateGray,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }
            items(items) { template ->
                TemplateCard(template = template, onClick = { onSelect(template) })
            }
        }
    }
}

@Composable
private fun TemplateCard(template: PdfTemplate, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.CardWhite),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(template.iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    template.icon,
                    contentDescription = null,
                    tint = template.iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = template.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1E293B)
                )
                Text(
                    text = template.description,
                    fontSize = 12.sp,
                    color = AppColors.SlateGray
                )
            }
            Icon(
                Icons.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = AppColors.LightSlate,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun TemplateEditor(
    template: PdfTemplate,
    fieldValues: Map<String, String>,
    onFieldChange: (String, String) -> Unit,
    onBack: () -> Unit,
    onGenerate: () -> Unit,
    isGenerating: Boolean,
    generatedUri: android.net.Uri?,
    showPreview: Boolean
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.LightBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFF1E293B)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = template.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                Text(
                    text = "Fill in the fields below",
                    fontSize = 12.sp,
                    color = AppColors.SlateGray
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            template.fields.forEach { field ->
                val value = fieldValues[field.key] ?: ""
                Text(
                    text = field.label,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1E293B)
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = value,
                    onValueChange = { onFieldChange(field.key, it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(field.placeholder, color = AppColors.LightSlate) },
                    singleLine = field.singleLine,
                    minLines = if (field.singleLine) 1 else 3,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppColors.AccentTeal,
                        unfocusedBorderColor = AppColors.LightSlate,
                        focusedContainerColor = AppColors.CardWhite,
                        unfocusedContainerColor = AppColors.CardWhite
                    )
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            if (showPreview && generatedUri != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = AppColors.TealGreenBg),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = AppColors.TealGreen,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "PDF generated successfully",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TealGreen
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    generatedUri?.let { uri ->
                                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                            setDataAndType(uri, "application/pdf")
                                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(android.content.Intent.createChooser(intent, "Open PDF"))
                                    }
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Open")
                            }
                            Button(
                                onClick = onBack,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AppColors.DarkBlue)
                            ) {
                                Text("New Template")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }

        Surface(shadowElevation = 12.dp, color = AppColors.CardWhite) {
            Button(
                onClick = onGenerate,
                enabled = !isGenerating,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.DarkBlue,
                    disabledContainerColor = AppColors.LightSlate
                )
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Generating...", fontWeight = FontWeight.Bold)
                } else {
                    Icon(
                        Icons.Outlined.PictureAsPdf,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generate PDF", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun generateTemplatePdf(
    context: Context,
    template: PdfTemplate,
    fieldValues: Map<String, String>,
    outputFile: File
) {
    val document = PdfDocument()
    try {
        val pageWidth = 595
        val pageHeight = 842
        val margin = 40f
        val lineHeight = 18f

        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        val titlePaint = Paint().apply {
            color = AndroidColor.BLACK
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val labelPaint = Paint().apply {
            color = AndroidColor.parseColor("#1E293B")
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val valuePaint = Paint().apply {
            color = AndroidColor.parseColor("#374151")
            textSize = 13f
            isAntiAlias = true
        }

        val dividerPaint = Paint().apply {
            color = AndroidColor.parseColor("#E2E8F0")
            strokeWidth = 1f
        }

        canvas.drawColor(AndroidColor.WHITE)

        var y = margin + 20f

        canvas.drawText(template.title, margin, y, titlePaint)
        y += 40f

        canvas.drawLine(margin, y, pageWidth - margin, y, dividerPaint)
        y += 30f

        for (field in template.fields) {
            val value = fieldValues[field.key]?.trim() ?: ""
            if (value.isEmpty()) continue

            canvas.drawText("${field.label}:", margin, y, labelPaint)
            y += lineHeight

            val words = value.split(" ")
            var line = ""
            for (word in words) {
                val testLine = if (line.isEmpty()) word else "$line $word"
                if (valuePaint.measureText(testLine) > pageWidth - 2 * margin) {
                    canvas.drawText(line, margin + 10f, y, valuePaint)
                    y += lineHeight
                    line = word
                } else {
                    line = testLine
                }
            }
            if (line.isNotEmpty()) {
                canvas.drawText(line, margin + 10f, y, valuePaint)
                y += lineHeight
            }

            y += 8f

            if (y > pageHeight - margin - 40f) {
                document.finishPage(page)
                val newPageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 2).create()
                val newPage = document.startPage(newPageInfo)
                val newCanvas = newPage.canvas
                newCanvas.drawColor(AndroidColor.WHITE)
                y = margin + 20f
            }
        }

        document.finishPage(page)
        FileOutputStream(outputFile).use { document.writeTo(it) }
    } catch (e: Exception) {
        com.example.imagetopdf.core.logging.AppLogger.e(e)
    } finally {
        document.close()
    }
}
