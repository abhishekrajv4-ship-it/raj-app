package com.rajeducational.erp.ui.admin

import androidx.compose.foundation.*
import com.google.firebase.firestore.FirebaseFirestore
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
    val context = androidx.compose.ui.platform.LocalContext.current
    var activeSection by remember { mutableStateOf("dashboard") }
    var hasUnreadAdminMessages by remember { mutableStateOf(false) }
    var isAdminType by remember { mutableStateOf("") }
    var adminPermissions by remember { mutableStateOf<List<String>>(emptyList()) }
    var otherAdmins by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var showAdminPasswordDialog by remember { mutableStateOf<Map<String, Any>?>(null) }
    var adminPasswordInput by remember { mutableStateOf("") }
    
    val featureList = listOf(
        "admin_college_control" to "College Control Centre",
        "admin_fee_structure_control" to "College Fee Structure Control Center",
        "admin_gallery_upload_center" to "Gallery Photo Upload Center",
        "admin_event_control_center" to "Event Control Center",
        "admin_guest_messages_control" to "Guest Messages",
        "admin_announcement_control" to "Common Announcement Control Center",
        "admin_council_voting_control" to "Council Voting Control",
        "admin_student_messages_control" to "Student Messages",
        "admin_fee_reminder_control" to "Fees Control Panel",
        "admin_teacher_qr_control" to "Teacher Registration by QR Code",
        "admin_registered_teachers" to "Registered Teachers",
        "admin_teacher_messages" to "Teachers Messages",
        "admin_statistics_control" to "Dashboard Edit Control Panel",
        "admin_developer_options" to "Developer Options",
        "admin_teacher_review_criteria_control" to "Teacher Review Criteria Control",
        "admin_teacher_reviews" to "Teacher Reviews Monitor",
        "admin_management_review_criteria_control" to "Management Review Criteria Control",
        "admin_management_review_control" to "Management Reviews Monitor"
    )
    
    LaunchedEffect(Unit) {
        FirebaseFirestore.getInstance().collection("guest_messages")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    hasUnreadAdminMessages = snapshot.documents.any { doc ->
                        val rAdmin = doc.getBoolean("readByAdmin")
                        val isRAdmin = doc.getBoolean("isReadByAdmin")
                        rAdmin == false || isRAdmin == false || (rAdmin == null && isRAdmin == null)
                    }
                }
            }
            
        FirebaseFirestore.getInstance().collection("admins")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    otherAdmins = snapshot.documents.map { it.data?.plus("id" to it.id) ?: mapOf() }
                }
            }
    }

    val dashboardMenuItems = if (isAdminType == "superadmin") {
        listOf(AdminMenuItem("superadminpanel", "Super Admin Panel", Icons.Default.AdminPanelSettings))
    } else {
        featureList.filter { adminPermissions.contains(it.first) }.map {
            AdminMenuItem(it.first, it.second, Icons.Default.CheckCircle)
        }
    }

    val superAdminMenuItems = listOf(
        AdminMenuItem("guestportalcontrol", "Guest Portal Control", Icons.Default.ManageAccounts),
        AdminMenuItem("studentportalcontrol", "Student Portal Control", Icons.Default.School),
        AdminMenuItem("teacherportalcontrol", "Teacher Portal Control", Icons.Default.PersonSearch),
        AdminMenuItem("settings", "Settings", Icons.Default.Settings),
        AdminMenuItem("reviewcontrolcenter", "Review Control Center", Icons.Default.Star),
        AdminMenuItem("admin_add_admin", "Add Admin", Icons.Default.PersonAdd),
        AdminMenuItem("admin_monitor_admin", "Monitor Admin Menu", Icons.Default.People)
    )

    // Admin Panel
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(if (isAdminType == "admin") "Admin Portal" else "Admin Panel", fontWeight = FontWeight.Bold) },
            navigationIcon = { 
                if (activeSection != "dashboard") {
                    IconButton(onClick = { 
                        if (activeSection in listOf("guestportalcontrol", "studentportalcontrol", "teacherportalcontrol", "settings", "reviewcontrolcenter")) {
                            activeSection = "superadminpanel"
                        } else {
                            activeSection = "dashboard"
                        }
                    }) { Icon(Icons.Default.ArrowBack, "Back") }
                }
            },
            actions = { IconButton(onClick = { 
                if (isAdminType.isNotEmpty()) {
                    isAdminType = ""
                    activeSection = "dashboard"
                } else {
                    navController.popBackStack() 
                }
            }) { Icon(Icons.Default.ExitToApp, "Logout") } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Admin, titleContentColor = Color.White, navigationIconContentColor = Color.White, actionIconContentColor = Color.White)
        )
    }) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().background(AppColors.Background).verticalScroll(rememberScrollState())) {
            if (activeSection == "dashboard") {
                if (isAdminType == "") {
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
    
                    Text("Admin Profiles", fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp), color = AppColors.Navy)
                    
                    // Super Admin
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp).clickable { 
                            showAdminPasswordDialog = mapOf("username" to "Super Admin", "password" to "2311", "type" to "superadmin")
                            adminPasswordInput = ""
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F4F8)), shape = RoundedCornerShape(10.dp)) {
                                Box(modifier = Modifier.padding(8.dp)) { Icon(Icons.Default.AdminPanelSettings, "Super Admin", tint = AppColors.Navy, modifier = Modifier.size(22.dp)) }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Text("Super Admin Panel", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = AppColors.TextPrimary, modifier = Modifier.weight(1f))
                            Icon(Icons.Default.ChevronRight, "Go", tint = Color.LightGray)
                        }
                    }
                    
                    // Other Admins
                    otherAdmins.forEach { admin ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp).clickable { 
                                showAdminPasswordDialog = admin.plus("type" to "admin")
                                adminPasswordInput = ""
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F4F8)), shape = RoundedCornerShape(10.dp)) {
                                    Box(modifier = Modifier.padding(8.dp)) { Icon(Icons.Default.Person, admin["username"] as? String ?: "", tint = AppColors.Navy, modifier = Modifier.size(22.dp)) }
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Text("${admin["username"]} Menu", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = AppColors.TextPrimary, modifier = Modifier.weight(1f))
                                Icon(Icons.Default.ChevronRight, "Go", tint = Color.LightGray)
                            }
                        }
                    }
                } else if (isAdminType == "superadmin") {
                    Text("Super Admin Panel", fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp), color = AppColors.Navy)
                    superAdminMenuItems.forEach { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp).clickable { 
                                if (item.key.startsWith("admin_")) {
                                    navController.navigate(item.key)
                                } else {
                                    activeSection = item.key 
                                }
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F4F8)), shape = RoundedCornerShape(10.dp)) {
                                    Box(modifier = Modifier.padding(8.dp)) { Icon(item.icon, item.label, tint = AppColors.Navy, modifier = Modifier.size(22.dp)) }
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Text(item.label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = AppColors.TextPrimary, modifier = Modifier.weight(1f))
                                if (item.key == "guestportalcontrol" && hasUnreadAdminMessages) {
                                    Box(modifier = Modifier.size(10.dp).background(Color.Red, androidx.compose.foundation.shape.CircleShape))
                                    Spacer(modifier = Modifier.width(12.dp))
                                }
                                Icon(Icons.Default.ChevronRight, "Go", tint = Color.LightGray)
                            }
                        }
                    }
                } else if (isAdminType == "admin") {
                    Text("Assigned Features", fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp), color = AppColors.Navy)
                    val adminFeatures = featureList.filter { adminPermissions.contains(it.first) }
                    adminFeatures.forEach { (key, label) ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp).clickable { 
                                if (key.startsWith("admin_")) {
                                    navController.navigate(key)
                                } else {
                                    activeSection = key 
                                }
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F4F8)), shape = RoundedCornerShape(10.dp)) {
                                    Box(modifier = Modifier.padding(8.dp)) { Icon(Icons.Default.CheckCircle, label, tint = AppColors.Navy, modifier = Modifier.size(22.dp)) }
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Text(label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = AppColors.TextPrimary, modifier = Modifier.weight(1f))
                                Icon(Icons.Default.ChevronRight, "Go", tint = Color.LightGray)
                            }
                        }
                    }
                }
            } else if (activeSection == "superadminpanel") {
                // Now handled inside dashboard when isAdminType == "superadmin"
                activeSection = "dashboard"
            } else {
                // Placeholder for sub-sections
                if (activeSection == "guestportalcontrol") {
                    Card(modifier = Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(16.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Guest Portal Control", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AppColors.Navy)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { navController.navigate("admin_college_control") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Admin),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("College Control Centre", modifier = Modifier.padding(8.dp))
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { navController.navigate("admin_fee_structure_control") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Admin),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("College Fee Structure Control Center", modifier = Modifier.padding(8.dp))
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { navController.navigate("admin_gallery_upload_center") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Admin),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Gallery Photo Upload Center", modifier = Modifier.padding(8.dp))
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { navController.navigate("admin_event_control_center") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Admin),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Event Control Center", modifier = Modifier.padding(8.dp))
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { navController.navigate("admin_guest_messages_control") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Admin),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Guest Messages", modifier = Modifier.padding(8.dp))
                                    if (hasUnreadAdminMessages) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(modifier = Modifier.size(10.dp).background(Color.Red, androidx.compose.foundation.shape.CircleShape))
                                    }
                                }
                            }
                        }
                    }
                } else if (activeSection == "studentportalcontrol") {
                    Card(modifier = Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(16.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Student Portal Control", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AppColors.Navy)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { navController.navigate("admin_announcement_control") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Admin),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Common Announcement Control Center", modifier = Modifier.padding(8.dp))
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { navController.navigate("admin_council_voting_control") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Admin),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Council Voting Control", modifier = Modifier.padding(8.dp))
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { navController.navigate("admin_student_messages_control") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Admin),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Student Messages", modifier = Modifier.padding(8.dp))
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { navController.navigate("admin_fee_reminder_control") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Admin),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Fees Control Panel", modifier = Modifier.padding(8.dp))
                            }
                        }
                    }
                } else if (activeSection == "teacherportalcontrol") {
                    Card(modifier = Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(16.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Teacher Portal Control", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AppColors.Navy)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { navController.navigate("admin_teacher_qr_control") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Admin),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Teacher Registration by QR Code", modifier = Modifier.padding(8.dp))
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { navController.navigate("admin_registered_teachers") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Admin),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Registered Teachers", modifier = Modifier.padding(8.dp))
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { navController.navigate("admin_teacher_messages") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Admin),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Teachers Messages", modifier = Modifier.padding(8.dp))
                            }
                        }
                    }
                } else if (activeSection == "settings") {
                    Card(modifier = Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(16.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Settings", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AppColors.Navy)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { navController.navigate("admin_statistics_control") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Admin),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Dashboard Edit Control Panel", modifier = Modifier.padding(8.dp))
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { navController.navigate("admin_developer_options") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Admin),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Developer Options", modifier = Modifier.padding(8.dp))
                            }
                        }
                    }
                } else if (activeSection == "reviewcontrolcenter") {
                    Card(modifier = Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(16.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Review Control Center", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AppColors.Navy)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { navController.navigate("admin_teacher_reviews") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Admin),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Teacher Reviews by Students", modifier = Modifier.padding(8.dp))
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { navController.navigate("admin_management_review_control") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Admin),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Management Review Control Center for Teachers", modifier = Modifier.padding(8.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            }
                        }
                    }
                } else {
                    Card(modifier = Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(16.dp)) {
                        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(activeSection.replaceFirstChar { it.uppercase() }, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AppColors.Navy)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Content for $activeSection module", color = AppColors.TextSecondary)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
    
    if (showAdminPasswordDialog != null) {
        AlertDialog(
            onDismissRequest = { showAdminPasswordDialog = null },
            title = { Text("Enter Password for ${showAdminPasswordDialog!!["username"]}") },
            text = {
                OutlinedTextField(
                    value = adminPasswordInput,
                    onValueChange = { adminPasswordInput = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (adminPasswordInput == showAdminPasswordDialog!!["password"]) {
                        isAdminType = showAdminPasswordDialog!!["type"] as? String ?: "admin"
                        adminPermissions = showAdminPasswordDialog!!["permissions"] as? List<String> ?: emptyList()
                        activeSection = "dashboard"
                        showAdminPasswordDialog = null
                    } else {
                        android.widget.Toast.makeText(context, "Incorrect password", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }) { Text("Login") }
            },
            dismissButton = {
                Button(onClick = { showAdminPasswordDialog = null }) { Text("Cancel") }
            }
        )
    }
}
