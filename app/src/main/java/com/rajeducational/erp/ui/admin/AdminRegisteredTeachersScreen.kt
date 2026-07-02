package com.rajeducational.erp.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore
import com.rajeducational.erp.theme.AppColors
import com.rajeducational.erp.ui.components.AttendancePercentageBadge
import com.rajeducational.erp.ui.components.AttendanceStatsCard
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class TeacherModel(
    val id: String = "",
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val collegeName: String = "",
    val course: String = "",
    val courses: List<String> = emptyList(),
    val years: List<String> = emptyList(),
    val password:  String = "",
    val role: String = "Teacher",
    val adminFeatures: Map<String, Boolean> = emptyMap()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminRegisteredTeachersScreen(navController: NavController) {
    val firestore = FirebaseFirestore.getInstance()
    var teachers by remember { mutableStateOf<List<TeacherModel>>(emptyList()) }
    var staffs by remember { mutableStateOf<List<TeacherModel>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) } // 0: All, 1: Teachers, 2: Staff
    var selectedTeacher by remember { mutableStateOf<TeacherModel?>(null) }
    var showResetDialog by remember { mutableStateOf(false) }
    var teacherToDelete by remember { mutableStateOf<TeacherModel?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        firestore.collection("teachers").addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                teachers = snapshot.documents.mapNotNull { doc ->
                    TeacherModel(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        phone = doc.getString("phone") ?: "",
                        email = doc.getString("email") ?: "",
                        address = doc.getString("address") ?: "",
                        collegeName = doc.getString("collegeName") ?: doc.getString("college") ?: "",
                        course = doc.getString("course") ?: doc.getString("departmentName") ?: doc.getString("department") ?: "",
                        courses = (doc.get("courses") as? List<String>) ?: emptyList(),
                        years = (doc.get("years") as? List<String>) ?: emptyList(),
                        password = doc.getString("password") ?: "",
                        role = "Teacher",
                        adminFeatures = (doc.get("adminFeatures") as? Map<String, Any>)?.mapValues { it.value as? Boolean ?: false } ?: emptyMap()
                    )
                }
            }
        }
        firestore.collection("staffs").addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                staffs = snapshot.documents.mapNotNull { doc ->
                    TeacherModel(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        phone = doc.getString("phone") ?: "",
                        email = doc.getString("email") ?: "",
                        address = doc.getString("address") ?: "",
                        collegeName = doc.getString("collegeName") ?: doc.getString("college") ?: "",
                        course = doc.getString("departmentName") ?: doc.getString("course") ?: doc.getString("department") ?: "",
                        courses = (doc.get("courses") as? List<String>) ?: emptyList(),
                        years = (doc.get("years") as? List<String>) ?: emptyList(),
                        password = doc.getString("password") ?: "",
                        role = "Staff",
                        adminFeatures = (doc.get("adminFeatures") as? Map<String, Any>)?.mapValues { it.value as? Boolean ?: false } ?: emptyMap()
                    )
                }
            }
        }
    }

    val combinedList = (teachers + staffs).sortedBy { it.name }
    val filteredTeachers = when (selectedTab) {
        1 -> teachers
        2 -> staffs
        else -> combinedList
    }.filter { it.name.contains(searchQuery, ignoreCase = true) }

    if (selectedTeacher != null) {
        TeacherDetailsView(
            teacher = selectedTeacher!!,
            onBack = { selectedTeacher = null },
            onResetPassword = { showResetDialog = true },
            onSeeStudents = { teacherId ->
                navController.navigate("admin_teacher_students/$teacherId")
            }
        )
        
        if (showResetDialog) {
            ResetPasswordDialog(
                teacher = selectedTeacher!!,
                onDismiss = { showResetDialog = false },
                onSuccess = { newPassword ->
                    selectedTeacher = selectedTeacher?.copy(password = newPassword)
                    showResetDialog = false
                }
            )
        }
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Registered Teachers and Staff", fontWeight = FontWeight.Bold, color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Admin)
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppColors.Background)
                    .padding(padding)
            ) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.White,
                    contentColor = AppColors.Admin
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("All", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Teachers", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Staff", fontWeight = FontWeight.Bold) }
                    )
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by name...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredTeachers) { teacher ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedTeacher = teacher },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(teacher.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AppColors.Navy)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Surface(
                                                color = if (teacher.role == "Staff") AppColors.Staff.copy(alpha = 0.1f) else AppColors.Admin.copy(alpha = 0.1f),
                                                contentColor = if (teacher.role == "Staff") AppColors.Staff else AppColors.Admin,
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = teacher.role,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        AttendancePercentageBadge(
                                            studentId = teacher.id,
                                            backgroundColor = if (teacher.role == "Staff") AppColors.Staff.copy(alpha = 0.15f) else AppColors.Teacher.copy(alpha = 0.15f),
                                            textColor = if (teacher.role == "Staff") AppColors.Staff else AppColors.Teacher
                                        )
                                    }
                                    IconButton(onClick = { teacherToDelete = teacher }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("College: ${teacher.collegeName}", fontSize = 14.sp, color = AppColors.TextSecondary)
                                Text("Phone: ${teacher.phone}", fontSize = 14.sp, color = AppColors.TextSecondary)
                            }
                        }
                    }
                }
            }
        }

        if (teacherToDelete != null) {
            AlertDialog(modifier = Modifier.fillMaxHeight(0.8f).fillMaxWidth(),
                onDismissRequest = { teacherToDelete = null },
                title = { Text("Delete ${teacherToDelete?.role}") },
                text = { Text("This ${teacherToDelete?.role?.lowercase()} will be deleted. Confirm that you want to delete ${teacherToDelete?.name}?") },
                confirmButton = {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                try {
                                    val collection = if (teacherToDelete!!.role == "Staff") "staffs" else "teachers"
                                    firestore.collection(collection).document(teacherToDelete!!.id).delete().await()
                                    android.widget.Toast.makeText(context, "${teacherToDelete!!.role} deleted", android.widget.Toast.LENGTH_SHORT).show()
                                    teacherToDelete = null
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(context, "Failed to delete", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { teacherToDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherDetailsView(teacher: TeacherModel, onBack: () -> Unit, onResetPassword: () -> Unit, onSeeStudents: (String) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Teacher and Staff Details", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Admin)
            )
        }
    ) { padding ->
        var showAdminFeaturesDialog by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.Background)
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(androidx.compose.foundation.rememberScrollState())
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Name: ${teacher.name}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AppColors.Navy)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Role: ${teacher.role}", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = if (teacher.role == "Staff") AppColors.Staff else AppColors.Admin)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Phone: ${teacher.phone}", fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Email: ${teacher.email.ifEmpty { "N/A" }}", fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Address: ${teacher.address}", fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("College: ${teacher.collegeName}", fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Department/Course: ${if (teacher.courses.isNotEmpty()) teacher.courses.joinToString(", ") else teacher.course}", fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Years/Batches: ${teacher.years.joinToString()}", fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Current Password: ${teacher.password}", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            AttendanceStatsCard(
                studentId = teacher.id,
                cardColor = Color.White
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onResetPassword,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Admin),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Reset Password", modifier = Modifier.padding(8.dp), fontSize = 16.sp)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { showAdminFeaturesDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Admin),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Admin Features", modifier = Modifier.padding(8.dp), fontSize = 16.sp)
            }

            if (teacher.role == "Teacher") {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { onSeeStudents(teacher.id) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Admin),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("See or Register Students", modifier = Modifier.padding(8.dp), fontSize = 16.sp)
                }
            }
        }

        if (showAdminFeaturesDialog) {
            AdminFeaturesDialog(teacher = teacher, onDismiss = { showAdminFeaturesDialog = false })
        }
    }
}

@Composable
fun AdminFeaturesDialog(teacher: TeacherModel, onDismiss: () -> Unit) {
    val firestore = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    var isSaving by remember { mutableStateOf(false) }
    
    // Feature list
    val features = listOf(
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
    
    var currentFeatures by remember { mutableStateOf(teacher.adminFeatures) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Admin Features for ${teacher.name}") },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                Text("Toggle the features this ${teacher.role.lowercase()} is allowed to access:", fontSize = 14.sp)
                Spacer(modifier = Modifier.height(16.dp))
                features.forEach { (key, title) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val current = currentFeatures[key] ?: false
                                currentFeatures = currentFeatures + (key to !current)
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = currentFeatures[key] == true,
                            onCheckedChange = { checked ->
                                currentFeatures = currentFeatures + (key to checked)
                            }
                        )
                        Text(title, fontSize = 16.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isSaving = true
                    val collection = if (teacher.role == "Staff") "staffs" else "teachers"
                    firestore.collection(collection).document(teacher.id)
                        .update("adminFeatures", currentFeatures)
                        .addOnSuccessListener {
                            android.widget.Toast.makeText(context, "Features updated successfully", android.widget.Toast.LENGTH_SHORT).show()
                            isSaving = false
                            onDismiss()
                        }
                        .addOnFailureListener {
                            android.widget.Toast.makeText(context, "Failed to update features", android.widget.Toast.LENGTH_SHORT).show()
                            isSaving = false
                        }
                },
                enabled = !isSaving
            ) {
                Text(if (isSaving) "Saving..." else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ResetPasswordDialog(teacher: TeacherModel, onDismiss: () -> Unit, onSuccess: (String) -> Unit) {
    var newPassword by remember { mutableStateOf("") }
    var isUpdating by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reset Password") },
        text = {
            Column {
                Text("Enter new password for ${teacher.name}")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("New Password") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newPassword.isNotBlank()) {
                        isUpdating = true
                        coroutineScope.launch {
                            try {
                                val collection = if (teacher.role == "Staff") "staffs" else "teachers"
                                firestore.collection(collection).document(teacher.id)
                                    .update("password", newPassword)
                                    .await()
                                android.widget.Toast.makeText(context, "Password updated", android.widget.Toast.LENGTH_SHORT).show()
                                onSuccess(newPassword)
                            } catch (e: Exception) {
                                e.printStackTrace()
                                android.widget.Toast.makeText(context, "Failed to update", android.widget.Toast.LENGTH_SHORT).show()
                                isUpdating = false
                            }
                        }
                    }
                },
                enabled = !isUpdating
            ) {
                Text(if (isUpdating) "Updating..." else "Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
