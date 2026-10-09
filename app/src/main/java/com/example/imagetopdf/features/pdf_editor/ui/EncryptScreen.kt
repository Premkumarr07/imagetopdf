package com.example.imagetopdf.features.pdf_editor.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.example.imagetopdf.features.image_to_pdf.ui.components.EmptyPickerCard
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import com.example.imagetopdf.R
import com.example.imagetopdf.constants.AppColors
import com.example.imagetopdf.core.utils.PdfEncryption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private enum class EncryptState { IDLE, ENCRYPTING, DONE, ERROR }

@Composable
fun EncryptScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFile by remember { mutableStateOf<File?>(null) }
    var fileName by remember { mutableStateOf("") }
    var fileSize by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var encryptState by remember { mutableStateOf(EncryptState.IDLE) }
    var progress by remember { mutableStateOf(0f) }
    var outputName by remember { mutableStateOf("encrypted_document") }
    var errorMessage by remember { mutableStateOf("") }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedUri = it
            val file = uriToFile(context, it)
            selectedFile = file
            fileName = file?.name ?: "Selected PDF"
            fileSize = file?.let { f -> formatSize(f.length()) } ?: ""
            encryptState = EncryptState.IDLE
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.LightBg)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Encrypt PDF",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )
            Text(
                text = "Add password protection to your PDF",
                fontSize = 13.sp,
                color = AppColors.SlateGray
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (selectedUri == null) {
                EmptyPickerCard(onClick = { launcher.launch("application/pdf") })
            } else {
                SelectedFileCard(
                    fileName = fileName,
                    fileSize = fileSize,
                    onRemove = {
                        selectedUri = null
                        selectedFile = null
                        fileName = ""
                        fileSize = ""
                        encryptState = EncryptState.IDLE
                    }
                )
            }

            AnimatedVisibility(
                visible = selectedUri != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Output file name",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1E293B)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = outputName,
                        onValueChange = { outputName = it },
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

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Set password",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1E293B)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("Enter password") },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                    contentDescription = null,
                                    tint = AppColors.SlateGray
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppColors.AccentTeal,
                            unfocusedBorderColor = AppColors.LightSlate,
                            focusedContainerColor = AppColors.CardWhite,
                            unfocusedContainerColor = AppColors.CardWhite
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("Confirm password") },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppColors.AccentTeal,
                            unfocusedBorderColor = AppColors.LightSlate,
                            focusedContainerColor = AppColors.CardWhite,
                            unfocusedContainerColor = AppColors.CardWhite
                        ),
                        isError = confirmPassword.isNotEmpty() && password != confirmPassword,
                        supportingText = if (confirmPassword.isNotEmpty() && password != confirmPassword) {
                            { Text("Passwords do not match", color = AppColors.RedDelete) }
                        } else null
                    )

                    if (password.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val strength = getPasswordStrength(password)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Strength:", fontSize = 12.sp, color = AppColors.SlateGray)
                            Text(
                                text = strength.first,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = strength.second
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = encryptState != EncryptState.IDLE,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(24.dp))
                    when (encryptState) {
                        EncryptState.ENCRYPTING -> {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = AppColors.CardWhite),
                                elevation = CardDefaults.cardElevation(1.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    CircularProgressIndicator(
                                        progress = { progress },
                                        modifier = Modifier.size(48.dp),
                                        color = AppColors.DarkBlue,
                                        strokeWidth = 4.dp
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Encrypting...",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF1E293B)
                                    )
                                }
                            }
                        }
                        EncryptState.DONE -> {
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
                                        Icons.Outlined.Lock,
                                        contentDescription = null,
                                        tint = AppColors.TealGreen,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "PDF encrypted successfully",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AppColors.TealGreen
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Password protection applied",
                                        fontSize = 13.sp,
                                        color = Color(0xFF1E293B)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                selectedUri?.let { uri ->
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
                                            onClick = {
                                                selectedUri = null
                                                selectedFile = null
                                                fileName = ""
                                                fileSize = ""
                                                password = ""
                                                confirmPassword = ""
                                                encryptState = EncryptState.IDLE
                                                outputName = "encrypted_document"
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = AppColors.DarkBlue)
                                        ) {
                                            Text("Encrypt Another")
                                        }
                                    }
                                }
                            }
                        }
                        EncryptState.ERROR -> {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2)),
                                elevation = CardDefaults.cardElevation(1.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Outlined.Error,
                                        contentDescription = null,
                                        tint = AppColors.RedDelete,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = errorMessage,
                                        fontSize = 14.sp,
                                        color = AppColors.RedDelete,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                        else -> {}
                    }
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }

        Surface(shadowElevation = 12.dp, color = AppColors.CardWhite) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { launcher.launch("application/pdf") },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, AppColors.DarkBlue),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.DarkBlue)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.pdf),
                        contentDescription = "Browse",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Browse", fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = {
                        if (encryptState == EncryptState.DONE || encryptState == EncryptState.ERROR) {
                            selectedUri = null
                            selectedFile = null
                            fileName = ""
                            fileSize = ""
                            password = ""
                            confirmPassword = ""
                            encryptState = EncryptState.IDLE
                            outputName = "encrypted_document"
                        } else {
                            if (password.length < 4) {
                                errorMessage = "Password must be at least 4 characters"
                                encryptState = EncryptState.ERROR
                                return@Button
                            }
                            if (password != confirmPassword) {
                                errorMessage = "Passwords do not match"
                                encryptState = EncryptState.ERROR
                                return@Button
                            }
                            selectedFile?.let { file ->
                                encryptState = EncryptState.ENCRYPTING
                                scope.launch {
                                    val result = withContext(Dispatchers.IO) {
                                        try {
                                            val bytes = file.readBytes()
                                            val encrypted = PdfEncryption.encrypt(bytes, password)
                                            val outputFile = File(context.cacheDir, "${outputName.ifBlank { "encrypted" }}.pdf")
                                            outputFile.writeBytes(encrypted)
                                            val uri = FileProvider.getUriForFile(
                                                context,
                                                "${context.packageName}.provider",
                                                outputFile
                                            )
                                            uri
                                        } catch (e: Exception) {
                                            null
                                        }
                                    }
                                    if (result != null) {
                                        encryptState = EncryptState.DONE
                                    } else {
                                        errorMessage = "Encryption failed. Please try again."
                                        encryptState = EncryptState.ERROR
                                    }
                                }
                            }
                        }
                    },
                    enabled = selectedUri != null && password.isNotEmpty() && encryptState != EncryptState.ENCRYPTING,
                    modifier = Modifier
                        .weight(2f)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (encryptState == EncryptState.DONE) AppColors.SuccessGreen else AppColors.DarkBlue,
                        disabledContainerColor = AppColors.LightSlate
                    )
                ) {
                    if (encryptState == EncryptState.DONE) {
                        Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Encrypt Again", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(
                            painter = painterResource(id = R.drawable.encrypt),
                            contentDescription = "Encrypt",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Encrypt", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectedFileCard(fileName: String, fileSize: String, onRemove: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.CardWhite),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(AppColors.RoseBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.pdf),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = AppColors.Rose
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = fileName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1E293B),
                    maxLines = 1
                )
                Text(
                    text = fileSize,
                    fontSize = 12.sp,
                    color = AppColors.SlateGray
                )
            }
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = "Remove",
                    tint = AppColors.SlateGray
                )
            }
        }
    }
}

private fun getPasswordStrength(password: String): Pair<String, Color> {
    return when {
        password.length < 6 -> Pair("Weak", AppColors.RedDelete)
        password.length < 10 -> Pair("Medium", AppColors.Orange)
        else -> Pair("Strong", AppColors.TealGreen)
    }
}

private fun uriToFile(context: android.content.Context, uri: Uri): File? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val file = File(context.cacheDir, "temp_${System.currentTimeMillis()}.pdf")
        file.outputStream().use { output ->
            inputStream.copyTo(output)
        }
        inputStream.close()
        file
    } catch (e: Exception) {
        null
    }
}

private fun formatSize(bytes: Long): String {
    return when {
        bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0)
        bytes >= 1024 -> "%.0f KB".format(bytes / 1024.0)
        else -> "$bytes B"
    }
}
