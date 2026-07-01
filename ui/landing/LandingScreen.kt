package com.rajeducational.erp.ui.landing

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.rajeducational.erp.theme.AppColors

data class RoleTile(val key: String, val label: String, val desc: String, val icon: String, val color: Color)

@Composable
fun LandingScreen(navController: NavController) {
    val roles = listOf(
        RoleTile("student", "Student", "Access your dashboard", "school", AppColors.Student),
        RoleTile("teacher", "Teacher", "Manage your classes", "easel", AppColors.Teacher),
        RoleTile("guest", "Guest", "Explore our institution", "eye", AppColors.Guest),
        RoleTile("admin", "Admin", "Control centre", "shield", AppColors.Admin),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .verticalScroll(rememberScrollState())
    ) {
        // Header with Logo
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                .padding(vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo placeholder - replace with actual logo
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(AppColors.Navy),
                contentAlignment = Alignment.Center
            ) {
                Text("REG", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Raj Educational Group",
                fontSize = 22.sp, fontWeight = FontWeight.Bold, color = AppColors.Navy,
                textAlign = TextAlign.Center
            )
            Text("Excellence in Education", fontSize = 14.sp, color = AppColors.TextSecondary)
        }

        // Social Links
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            SocialButton("WhatsApp", Color(0xFF25D366)) { /* open whatsapp link */ }
            Spacer(modifier = Modifier.width(10.dp))
            SocialButton("Instagram", Color(0xFFE1306C)) { /* open instagram link */ }
            Spacer(modifier = Modifier.width(10.dp))
            SocialButton("Facebook", Color(0xFF1877F2)) { /* open facebook link */ }
        }

        // Stats Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .background(AppColors.Navy, RoundedCornerShape(16.dp))
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            StatItem("7", "Colleges")
            StatItem("13", "Courses")
            StatItem("0", "Students")
        }

        // Section Title
        Text(
            "Select your role",
            fontSize = 17.sp, fontWeight = FontWeight.Bold, color = AppColors.Navy,
            modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 12.dp)
        )

        // Role Tiles - 2x2 Grid
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                roles.take(2).forEach { role ->
                    RoleTileCard(
                        role = role,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            when (role.key) {
                                "student" -> navController.navigate("student_auth")
                                "teacher" -> navController.navigate("teacher_auth")
                                "guest" -> navController.navigate("guest_portal")
                                "admin" -> navController.navigate("admin_panel")
                            }
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                roles.drop(2).forEach { role ->
                    RoleTileCard(
                        role = role,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            when (role.key) {
                                "guest" -> navController.navigate("guest_portal")
                                "admin" -> navController.navigate("admin_panel")
                            }
                        }
                    )
                }
            }
        }

        // Footer
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("College ERP Portal", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Navy)
            Text("Patna, Bihar, India", fontSize = 12.sp, color = AppColors.TextSecondary)
        }
    }
}

@Composable
fun RoleTileCard(role: RoleTile, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.5.dp, role.color)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(role.color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = role.label, tint = role.color, modifier = Modifier.size(28.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(role.label, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = role.color)
            Text(role.desc, fontSize = 12.sp, color = AppColors.TextSecondary, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text(label, fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
    }
}

@Composable
fun SocialButton(text: String, color: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(24.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}
