package com.example.imagetopdf.features.profile.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.imagetopdf.BuildConfig
import com.example.imagetopdf.MainActivity
import com.example.imagetopdf.constants.AppColors
import com.example.imagetopdf.core.preferences.DefaultPdfQuality
import com.example.imagetopdf.core.preferences.ThemeMode
import com.example.imagetopdf.core.preferences.UserPreferences
import com.example.imagetopdf.navigation.NavigationRoutes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    onSettingsChanged: () -> Unit = {}
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()

    var displayName by remember { mutableStateOf(UserPreferences.getDisplayName(context)) }
    var email by remember { mutableStateOf(UserPreferences.getEmail(context)) }
    var themeMode by remember { mutableStateOf(UserPreferences.getThemeMode(context)) }
    var dynamicColor by remember { mutableStateOf(UserPreferences.getDynamicColor(context)) }
    var notificationsOn by remember { mutableStateOf(UserPreferences.notificationsEnabled(context)) }
    var notifyOnDone by remember { mutableStateOf(UserPreferences.notifyOnConversionDone(context)) }
    var defaultQuality by remember { mutableStateOf(UserPreferences.getDefaultQuality(context)) }
    var saveToDownloads by remember { mutableStateOf(UserPreferences.saveToDownloads(context)) }

    var showProfileDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showThemeSheet by remember { mutableStateOf(false) }
    var showQualitySheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        displayName = UserPreferences.getDisplayName(context)
        email = UserPreferences.getEmail(context)
        themeMode = UserPreferences.getThemeMode(context)
        dynamicColor = UserPreferences.getDynamicColor(context)
        notificationsOn = UserPreferences.notificationsEnabled(context)
        notifyOnDone = UserPreferences.notifyOnConversionDone(context)
        defaultQuality = UserPreferences.getDefaultQuality(context)
        saveToDownloads = UserPreferences.saveToDownloads(context)
    }

    val initial = displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "U"

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(AppColors.DarkBlue, Color(0xFF2563EB))
                            ),
                            RoundedCornerShape(20.dp)
                        )
                        .padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initial,
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = displayName,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            if (email.isNotBlank()) {
                                Text(
                                    text = email,
                                    fontSize = 13.sp,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                color = Color.White.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "Local account · on-device only",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.9f),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        IconButton(onClick = { showProfileDialog = true }) {
                            Icon(Icons.Outlined.Edit, contentDescription = "Edit profile", tint = Color.White)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            SettingsSectionTitle("Account")
            SettingsGroup {
                SettingsRow(
                    title = "Personal information",
                    subtitle = "Name and email",
                    icon = Icons.Outlined.Person,
                    iconTint = AppColors.DarkBlue,
                    iconBg = Color(0xFFE0E7FF),
                    onClick = { showProfileDialog = true }
                )
                SettingsDivider()
                SettingsRow(
                    title = "Password",
                    subtitle = "Change app passcode",
                    icon = Icons.Outlined.Lock,
                    iconTint = Color(0xFF7C3AED),
                    iconBg = Color(0xFFEDE9FE),
                    onClick = { showPasswordDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            SettingsSectionTitle("Appearance")
            SettingsGroup {
                SettingsRow(
                    title = "Theme",
                    subtitle = themeMode.label,
                    icon = Icons.Outlined.DarkMode,
                    iconTint = Color(0xFF0F766E),
                    iconBg = Color(0xFFCCFBF1),
                    onClick = { showThemeSheet = true }
                )
                SettingsDivider()
                SettingsToggleRow(
                    title = "Material You colors",
                    subtitle = "Match wallpaper on Android 12+",
                    icon = Icons.Outlined.Palette,
                    iconTint = Color(0xFFDB2777),
                    iconBg = Color(0xFFFCE7F3),
                    checked = dynamicColor,
                    onCheckedChange = {
                        dynamicColor = it
                        UserPreferences.setDynamicColor(context, it)
                        onSettingsChanged()
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            SettingsSectionTitle("PDF defaults")
            SettingsGroup {
                SettingsRow(
                    title = "Default quality",
                    subtitle = defaultQuality.label,
                    icon = Icons.Outlined.Tune,
                    iconTint = Color(0xFFEA580C),
                    iconBg = Color(0xFFFFEDD5),
                    onClick = { showQualitySheet = true }
                )
                SettingsDivider()
                SettingsToggleRow(
                    title = "Also save to Downloads",
                    subtitle = "When exporting new PDFs",
                    icon = Icons.Outlined.Folder,
                    iconTint = AppColors.DarkBlue,
                    iconBg = Color(0xFFE0E7FF),
                    checked = saveToDownloads,
                    onCheckedChange = {
                        saveToDownloads = it
                        UserPreferences.setSaveToDownloads(context, it)
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            SettingsSectionTitle("Notifications")
            SettingsGroup {
                SettingsToggleRow(
                    title = "Allow notifications",
                    subtitle = "Master switch",
                    icon = Icons.Outlined.Notifications,
                    iconTint = Color(0xFFCA8A04),
                    iconBg = Color(0xFFFEF9C3),
                    checked = notificationsOn,
                    onCheckedChange = {
                        notificationsOn = it
                        UserPreferences.setNotificationsEnabled(context, it)
                        if (!it) notifyOnDone = false
                    }
                )
                SettingsDivider()
                SettingsToggleRow(
                    title = "Conversion finished",
                    subtitle = "When a PDF is ready",
                    icon = Icons.Outlined.CheckCircle,
                    iconTint = AppColors.SuccessGreen,
                    iconBg = Color(0xFFD1FAE5),
                    checked = notifyOnDone && notificationsOn,
                    enabled = notificationsOn,
                    onCheckedChange = {
                        notifyOnDone = it
                        UserPreferences.setNotifyOnConversionDone(context, it)
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            SettingsSectionTitle("Support")
            SettingsGroup {
                SettingsRow(
                    title = "Help & feedback",
                    subtitle = "Send us an email",
                    icon = Icons.AutoMirrored.Outlined.HelpOutline,
                    iconTint = Color(0xFF64748B),
                    iconBg = Color(0xFFF1F5F9),
                    onClick = {
                        val mail = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:")
                            putExtra(Intent.EXTRA_EMAIL, arrayOf("support@example.com"))
                            putExtra(Intent.EXTRA_SUBJECT, "ImageToPDF feedback")
                        }
                        try {
                            context.startActivity(mail)
                        } catch (_: Exception) {
                            Toast.makeText(context, "No email app found", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                SettingsDivider()
                SettingsRow(
                    title = "App version",
                    subtitle = BuildConfig.VERSION_NAME,
                    icon = Icons.Outlined.Info,
                    iconTint = Color(0xFF64748B),
                    iconBg = Color(0xFFF1F5F9),
                    onClick = {}
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedButton(
                onClick = {
                    context.getSharedPreferences(MainActivity.PREFS_NAME, android.content.Context.MODE_PRIVATE)
                        .edit()
                        .putBoolean(MainActivity.KEY_LOGGED_IN, false)
                        .apply()
                    navController.navigate(NavigationRoutes.Auth.route) {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Error)
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Log out", fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(88.dp))
        }
    }

    if (showProfileDialog) {
        var nameField by remember { mutableStateOf(displayName) }
        var emailField by remember { mutableStateOf(email) }
        AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            title = { Text("Personal information") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = nameField,
                        onValueChange = { nameField = it },
                        label = { Text("Display name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = emailField,
                        onValueChange = { emailField = it },
                        label = { Text("Email") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = KeyboardType.Email
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        UserPreferences.setProfile(context, nameField, emailField)
                        displayName = UserPreferences.getDisplayName(context)
                        email = UserPreferences.getEmail(context)
                        showProfileDialog = false
                        onSettingsChanged()
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showProfileDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showPasswordDialog) {
        var newPass by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPasswordDialog = false },
            title = { Text("Update password") },
            text = {
                Column {
                    Text(
                        "This only affects sign-in on this phone. Use at least 4 characters.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newPass,
                        onValueChange = { newPass = it },
                        label = { Text("New password") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newPass.length < 4) {
                            Toast.makeText(context, "Use at least 4 characters", Toast.LENGTH_SHORT).show()
                        } else {
                            UserPreferences.setLocalPassword(context, newPass)
                            showPasswordDialog = false
                            Toast.makeText(context, "Password updated", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showThemeSheet) {
        ModalBottomSheet(onDismissRequest = { showThemeSheet = false }) {
            Text(
                "Theme",
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            ThemeMode.entries.forEach { mode ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            themeMode = mode
                            UserPreferences.setThemeMode(context, mode)
                            onSettingsChanged()
                            showThemeSheet = false
                        }
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = themeMode == mode,
                        onClick = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(mode.label, fontSize = 16.sp)
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showQualitySheet) {
        ModalBottomSheet(onDismissRequest = { showQualitySheet = false }) {
            Text(
                "Default PDF quality",
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            DefaultPdfQuality.entries.forEach { q ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            defaultQuality = q
                            UserPreferences.setDefaultQuality(context, q)
                            showQualitySheet = false
                        }
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = defaultQuality == q, onClick = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(q.label, fontSize = 16.sp)
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column { content() }
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 72.dp),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    )
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String?,
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsIconBox(icon, iconTint, iconBg)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            if (subtitle != null) {
                Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String?,
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsIconBox(icon, iconTint, iconBg)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 1f else 0.5f)
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@Composable
private fun SettingsIconBox(icon: ImageVector, tint: Color, bg: Color) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
    }
}
