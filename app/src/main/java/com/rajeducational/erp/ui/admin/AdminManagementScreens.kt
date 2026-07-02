package com.rajeducational.erp.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore
import com.rajeducational.erp.theme.AppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAddAdminScreen(navController: NavController) {
    var adminName by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var step by remember { mutableStateOf(1) }
    var isSubmitting by remember { mutableStateOf(false) }

    val firestore = FirebaseFirestore.getInstance()
    
    val featureList = listOf(
        "admin_college_control" to "College Control Centre",
        "admin_fee_structure_control" to "College Fee Structure Control Center",
        "admin_gallery_upload_center" to "Gallery Photo Upload Center",
        "admin_event_control_center" to "Event Control Center",
        "admin_guest_messages_control" to "Guest Messages",
        "admin_announcement_control" to "Common Announcement Control Center",
        "admin_view_students" to "View Students",
        "admin_council_voting_control" to "Council Voting Control",
        "admin_student_messages_control" to "Student Messages",
        "admin_fee_reminder_control" to "Fees Control Panel",
        "admin_teacher_qr_control" to "Teacher and Staff Registration by QR Code",
        "admin_registered_teachers" to "Registered Teachers and Staff",
        "admin_teacher_messages" to "Teacher and Staff Messages",
        "admin_teacher_staff_attendance_control" to "Teacher and Staff Attendance Control",
        "admin_daily_teaching_plan_control" to "Teacher Daily Teaching Plan",
        "admin_statistics_control" to "Dashboard Edit Control Panel",
        "admin_developer_options" to "Developer Options",
        "admin_give_holiday" to "Give a Holiday",
        "admin_teacher_review_criteria_control" to "Teacher Review Criteria Control",
        "admin_teacher_reviews" to "Teacher Reviews Monitor",
        "admin_management_review_criteria_control" to "Management Review Criteria Control",
        "admin_management_review_control" to "Management Reviews Monitor",
        "admin_add_admin" to "Add Admin / Monitor Admins",
        "admin_generate_reports" to "Generate Reports Control"
    )
    
    val selectedFeatures = remember { mutableStateListOf<String>() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (step == 1) "Add Admin" else "Assign Features") },
                navigationIcon = { 
                    IconButton(onClick = { 
                        if (step == 2) step = 1 else navController.popBackStack() 
                    }) { Icon(Icons.Default.ArrowBack, "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.Admin, 
                    titleContentColor = Color.White, 
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(AppColors.Background)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            if (step == 1) {
                Button(
                    onClick = { navController.navigate("admin_monitor_admin") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Admin)
                ) {
                    Icon(Icons.Default.People, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Monitor Admins")
                }
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Admin Details", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AppColors.Navy)
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = adminName,
                            onValueChange = { adminName = it },
                            label = { Text("Admin Username") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = PasswordVisualTransformation()
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { 
                                if (adminName.isNotBlank() && password.isNotBlank()) step = 2 
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Admin)
                        ) {
                            Text("Next")
                        }
                    }
                }
            } else {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Select Features for $adminName", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AppColors.Navy)
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        featureList.forEach { (key, label) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { 
                                        if (selectedFeatures.contains(key)) selectedFeatures.remove(key)
                                        else selectedFeatures.add(key)
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = selectedFeatures.contains(key),
                                    onCheckedChange = { 
                                        if (it) selectedFeatures.add(key) else selectedFeatures.remove(key)
                                    }
                                )
                                Text(label, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { 
                                isSubmitting = true
                                val newAdmin = hashMapOf(
                                    "username" to adminName,
                                    "password" to password,
                                    "permissions" to selectedFeatures.toList()
                                )
                                firestore.collection("admins").add(newAdmin)
                                    .addOnSuccessListener {
                                        navController.popBackStack()
                                    }
                                    .addOnFailureListener {
                                        isSubmitting = false
                                    }
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Admin),
                            enabled = !isSubmitting
                        ) {
                            Text(if (isSubmitting) "Saving..." else "Save Admin")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMonitorAdminsScreen(navController: NavController) {
    val firestore = FirebaseFirestore.getInstance()
    var admins by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var selectedAdmin by remember { mutableStateOf<Map<String, Any>?>(null) }
    
    val featureList = listOf(
        "admin_college_control" to "College Control Centre",
        "admin_fee_structure_control" to "College Fee Structure Control Center",
        "admin_gallery_upload_center" to "Gallery Photo Upload Center",
        "admin_event_control_center" to "Event Control Center",
        "admin_guest_messages_control" to "Guest Messages",
        "admin_announcement_control" to "Common Announcement Control Center",
        "admin_view_students" to "View Students",
        "admin_council_voting_control" to "Council Voting Control",
        "admin_student_messages_control" to "Student Messages",
        "admin_fee_reminder_control" to "Fees Control Panel",
        "admin_teacher_qr_control" to "Teacher and Staff Registration by QR Code",
        "admin_registered_teachers" to "Registered Teachers and Staff",
        "admin_teacher_messages" to "Teacher and Staff Messages",
        "admin_teacher_staff_attendance_control" to "Teacher and Staff Attendance Control",
        "admin_daily_teaching_plan_control" to "Teacher Daily Teaching Plan",
        "admin_statistics_control" to "Dashboard Edit Control Panel",
        "admin_developer_options" to "Developer Options",
        "admin_give_holiday" to "Give a Holiday",
        "admin_teacher_review_criteria_control" to "Teacher Review Criteria Control",
        "admin_teacher_reviews" to "Teacher Reviews Monitor",
        "admin_management_review_criteria_control" to "Management Review Criteria Control",
        "admin_management_review_control" to "Management Reviews Monitor",
        "admin_add_admin" to "Add Admin / Monitor Admins",
        "admin_generate_reports" to "Generate Reports Control"
    )

    LaunchedEffect(Unit) {
        firestore.collection("admins").addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                admins = snapshot.documents.map { it.data?.plus("id" to it.id) ?: mapOf() }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (selectedAdmin == null) "Monitor Admins" else "Edit Admin") },
                navigationIcon = { 
                    IconButton(onClick = { 
                        if (selectedAdmin != null) selectedAdmin = null else navController.popBackStack() 
                    }) { Icon(Icons.Default.ArrowBack, "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.Admin, 
                    titleContentColor = Color.White, 
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(AppColors.Background)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            if (selectedAdmin == null) {
                if (admins.isEmpty()) {
                    Text("No admins found.", modifier = Modifier.padding(16.dp))
                } else {
                    admins.forEach { admin ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { selectedAdmin = admin },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(admin["username"] as? String ?: "", fontWeight = FontWeight.Bold)
                                    val permissions = admin["permissions"] as? List<*> ?: emptyList<Any>()
                                    Text("${permissions.size} features assigned", fontSize = 12.sp, color = AppColors.TextSecondary)
                                }
                                Icon(Icons.Default.Edit, "Edit", tint = AppColors.Admin)
                            }
                        }
                    }
                }
            } else {
                var editPassword by remember { mutableStateOf(selectedAdmin!!["password"] as? String ?: "") }
                var isUpdating by remember { mutableStateOf(false) }
                val currentPerms = selectedAdmin!!["permissions"] as? List<String> ?: emptyList()
                val selectedFeatures = remember { mutableStateListOf<String>().apply { addAll(currentPerms) } }
                
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Admin: ${selectedAdmin!!["username"]}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AppColors.Navy)
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = editPassword,
                            onValueChange = { editPassword = it },
                            label = { Text("Update Password") },
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = PasswordVisualTransformation()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text("Assigned Features", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        featureList.forEach { (key, label) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { 
                                        if (selectedFeatures.contains(key)) selectedFeatures.remove(key)
                                        else selectedFeatures.add(key)
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = selectedFeatures.contains(key),
                                    onCheckedChange = { 
                                        if (it) selectedFeatures.add(key) else selectedFeatures.remove(key)
                                    }
                                )
                                Text(label, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Button(
                                onClick = {
                                    val id = selectedAdmin!!["id"] as String
                                    firestore.collection("admins").document(id).delete().addOnSuccessListener {
                                        selectedAdmin = null
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                            ) {
                                Text("Delete")
                            }
                            
                            Button(
                                onClick = { 
                                    isUpdating = true
                                    val id = selectedAdmin!!["id"] as String
                                    firestore.collection("admins").document(id)
                                        .update(
                                            mapOf(
                                                "password" to editPassword,
                                                "permissions" to selectedFeatures.toList()
                                            )
                                        )
                                        .addOnSuccessListener {
                                            selectedAdmin = null
                                            isUpdating = false
                                        }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Admin)
                            ) {
                                Text(if (isUpdating) "Saving..." else "Update")
                            }
                        }
                    }
                }
            }
        }
    }
}
