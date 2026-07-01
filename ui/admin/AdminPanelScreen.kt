package com.rajeducational.erp.ui.admin

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.rajeducational.erp.theme.AppColors

data class AdminMenuItem(val key: String, val label: String, val icon: ImageVector)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(navController: NavController) {
    var isLoggedIn by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var activeSection by remember { mutableStateOf("dashboard") }

    val menuItems = listOf(
        AdminMenuItem("announcements", "Announcements", Icons.Default.Campaign),
        AdminMenuItem("notices", "Notices", Icons.Default.Notifications),
        AdminMenuItem("queries", "Queries", Icons.Default.QuestionAnswer),
        AdminMenuItem("gallery", "Gallery", Icons.Default.Image),
        AdminMenuItem("fees", "Fees", Icons.Default.Payment),
        AdminMenuItem("studentids", "App IDs", Icons.Default.Badge),
        AdminMenuItem("teachers", "Teachers", Icons.Default.School),
        AdminMenuItem("events", "Events", Icons.Default.Event),
        AdminMenuItem("messages", "Messages", Icons.Default.Chat),
        AdminMenuItem("settings", "Settings", Icons.Default.Settings),
        AdminMenuItem("reports", "Reports", Icons.Default.BarChart),
        AdminMenuItem("teacherreports", "Teacher Reports", Icons.Default.Description),
        AdminMenuItem("ratingcriteria", "Rating Criteria", Icons.Default.Tune),
    )

    if (!isLoggedIn) {
        // Login Screen
        Scaffold(topBar = { TopAppBar(title = { Text("Admin Login", fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, "Back") } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Admin, titleContentColor = Color.White, navigationIconContentColor = Color.White)) }) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize().background(AppColors.Background).padding(16.dp)) {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Shield, "Admin", tint = AppColors.Admin, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(20.dp))
                        OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Username") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation(), shape = RoundedCornerShape(10.dp))
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(onClick = { isLoggedIn = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = AppColors.Admin), shape = RoundedCornerShape(12.dp)) { Text("Login", modifier = Modifier.padding(8.dp)) }
                    }
                }
            }
        }
        return
    }

    // Admin Panel
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Admin Panel", fontWeight = FontWeight.Bold) },
            navigationIcon = { if (activeSection != "dashboard") IconButton(onClick = { activeSection = "dashboard" }) { Icon(Icons.Default.ArrowBack, "Back") } },
            actions = { IconButton(onClick = { isLoggedIn = false; navController.popBackStack() }) { Icon(Icons.Default.ExitToApp, "Logout") } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Admin, titleContentColor = Color.White, navigationIconContentColor = Color.White, actionIconContentColor = Color.White)
        )
    }) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().background(AppColors.Background).verticalScroll(rememberScrollState())) {
            if (activeSection == "dashboard") {
                // Dashboard Stats
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf("3" to "Students", "1" to "Teachers", "7" to "Colleges").forEach { (num, label) ->
                        Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(num, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = AppColors.Navy)
                                Text(label, fontSize = 11.sp, color = AppColors.TextSecondary)
                            }
                        }
                    }
                }

                // Vertical Menu
                Text("Admin Menu", fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp), color = AppColors.Navy)
                menuItems.forEach { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp).clickable { activeSection = item.key },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F4F8)), shape = RoundedCornerShape(10.dp)) {
                                Box(modifier = Modifier.padding(8.dp)) { Icon(item.icon, item.label, tint = AppColors.Navy, modifier = Modifier.size(22.dp)) }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(item.label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = AppColors.TextPrimary, modifier = Modifier.weight(1f))
                            Icon(Icons.Default.ChevronRight, "Go", tint = Color.LightGray)
                        }
                    }
                }
            } else {
                // Placeholder for sub-sections
                Card(modifier = Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(activeSection.replaceFirstChar { it.uppercase() }, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AppColors.Navy)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Content for $activeSection module", color = AppColors.TextSecondary)
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
