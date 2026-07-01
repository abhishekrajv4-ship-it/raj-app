package com.rajeducational.erp.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
    val password:  String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminRegisteredTeachersScreen(navController: NavController) {
    val firestore = FirebaseFirestore.getInstance()
    var teachers by remember { mutableStateOf<List<TeacherModel>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
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
                        course = doc.getString("course") ?: doc.getString("department") ?: "",
                        courses = (doc.get("courses") as? List<String>) ?: emptyList(),
                        years = (doc.get("years") as? List<String>) ?: emptyList(),
                        password = doc.getString("password") ?: ""
                    )
                }
            }
        }
    }

    val filteredTeachers = teachers.filter { it.name.contains(searchQuery, ignoreCase = true) }

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
                    title = { Text("Registered Teachers", fontWeight = FontWeight.Bold, color = Color.White) },
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
                                    Text(teacher.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AppColors.Navy, modifier = Modifier.weight(1f))
                                    IconButton(onClick = { teacherToDelete = teacher }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Teacher", tint = Color.Red)
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
            AlertDialog(
                onDismissRequest = { teacherToDelete = null },
                title = { Text("Delete Teacher") },
                text = { Text("This teacher will be deleted. Confirm that you want to delete ${teacherToDelete?.name}?") },
                confirmButton = {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                try {
                                    firestore.collection("teachers").document(teacherToDelete!!.id).delete().await()
                                    android.widget.Toast.makeText(context, "Teacher deleted", android.widget.Toast.LENGTH_SHORT).show()
                                    teacherToDelete = null
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(context, "Failed to delete teacher", android.widget.Toast.LENGTH_SHORT).show()
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
                title = { Text("Teacher Details", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Name: ${teacher.name}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AppColors.Navy)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Phone: ${teacher.phone}", fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Email: ${teacher.email.ifEmpty { "N/A" }}", fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Address: ${teacher.address}", fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("College: ${teacher.collegeName}", fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Courses: ${if (teacher.courses.isNotEmpty()) teacher.courses.joinToString(", ") else teacher.course}", fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Years/Batches: ${teacher.years.joinToString()}", fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Current Password: ${teacher.password}", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
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
                onClick = { onSeeStudents(teacher.id) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Admin),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("See or Register Students", modifier = Modifier.padding(8.dp), fontSize = 16.sp)
            }
        }
    }
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
                                firestore.collection("teachers").document(teacher.id)
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
