package com.example.imagetopdf.features.profile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.imagetopdf.navigation.NavigationRoutes

@Composable
fun ProfileScreen(navController: NavController) {
    val screenBackgroundColor = Color(0xFFF3F4F6)
    val scrollState = rememberScrollState()


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBackgroundColor)
            .padding(horizontal = 16.dp, vertical = 24.dp)
            .verticalScroll(scrollState)
    ) {
        Text(
            text = "Profile",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1F2937),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // 👤 Profile Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profile Initial Circle
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF3B82F6)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "P",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Prem Kumar",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827)
                    )
                    Text(
                        text = "premkumarshankar@gmail.com",
                        fontSize = 14.sp,
                        color = Color(0xFF6B7280)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Surface(
                        color = Color(0xFFFEF3C7),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "PRO MEMBER",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD97706),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 🛡️ Account Management Group (Replaced Workspace & Assets)
        SectionTitle("Account Management")
        ProfileOptionGroup {
            ProfileOptionItem(
                title = "Personal Information",
                icon = Icons.Outlined.Person,
                onClick = { /* Navigate to Personal Info */ }
            )
            Divider(color = Color(0xFFF3F4F6), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
            ProfileOptionItem(
                title = "Password & Security",
                icon = Icons.Outlined.Lock,
                onClick = { /* Navigate to Security */ }
            )
            Divider(color = Color(0xFFF3F4F6), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
            ProfileOptionItem(
                title = "Subscription Plan",
                icon = Icons.Outlined.Star,
                subtitle = "Pro Plan - Active",
                onClick = { /* Navigate to Subscription */ }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 🎨 Preferences Group
        SectionTitle("Preferences")
        ProfileOptionGroup {
            ProfileOptionItem(
                title = "App Theme",
                icon = Icons.Outlined.Person,
                subtitle = "System Default",
                onClick = { /* Open Theme Selector */ }
            )
            Divider(color = Color(0xFFF3F4F6), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
            ProfileOptionItem(
                title = "Notifications",
                icon = Icons.Outlined.Notifications,
                onClick = { /* Navigate to Notifications */ }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 🚪 Logout Button
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { /* Logout Logic */ },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = "Logout",
                    tint = Color(0xFFEF4444)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Log Out",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFEF4444)
                )
            }
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF6B7280),
        modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
    )
}

@Composable
fun ProfileOptionGroup(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            content()
        }
    }
}

@Composable
fun ProfileOptionItem(
    title: String,
    icon: ImageVector,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Leading Icon
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = Color(0xFF4B5563),
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        // Text Column
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1F2937)
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = Color(0xFF9CA3AF)
                )
            }
        }

        // Trailing Arrow
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Navigate",
            tint = Color(0xFFD1D5DB)
        )
    }
}