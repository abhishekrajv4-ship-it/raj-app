package com.rajeducational.erp.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore
import com.rajeducational.erp.theme.AppColors
import com.rajeducational.erp.ui.components.ProfileImage
import com.rajeducational.erp.ui.components.AttendancePercentageBadge
import com.rajeducational.erp.ui.components.AttendanceStatsCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminViewStudentsScreen(navController: NavController) {
    var search by remember { mutableStateOf("") }
    var allStudents by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    val firestore = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        firestore.collection("students").addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                allStudents = snapshot.documents.mapNotNull { doc -> 
                    doc.data?.plus("docId" to doc.id)
                }
            }
        }
    }

    val filteredStudents = allStudents.filter {
        val name = it["fullName"] as? String ?: ""
        val id = it["id"] as? String ?: ""
        val course = it["course"] as? String ?: ""
        name.contains(search, ignoreCase = true) || id.contains(search, ignoreCase = true) || course.contains(search, ignoreCase = true)
    }

    var selectedStudent by remember { mutableStateOf<Map<String, Any>?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<Map<String, Any>?>(null) }
    var showResetPassword by remember { mutableStateOf<Map<String, Any>?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("View Students") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.Admin,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().background(AppColors.Background)) {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                label = { Text("Search by name, ID, course...") },
                leadingIcon = { Icon(Icons.Default.Search, "Search") },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(10.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                items(filteredStudents.size) { index ->
                    val student = filteredStudents[index]
                    val name = student["fullName"] as? String ?: "Unknown"
                    val id = student["id"] as? String ?: ""
                    val profileUrl = student["profileUrl"] as? String

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { selectedStudent = student },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (profileUrl.isNullOrEmpty()) {
                                Box(
                                    modifier = Modifier.size(40.dp).clip(CircleShape).background(AppColors.Admin),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(name.firstOrNull()?.toString() ?: "?", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                ProfileImage(
                                    urlOrBase64 = profileUrl,
                                    modifier = Modifier.size(40.dp).clip(CircleShape),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(name, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f, fill = false))
                                    val isAttending = student["isAttending"] as? Boolean ?: true
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        color = if (isAttending) AppColors.Student.copy(alpha = 0.1f) else Color.Red.copy(alpha = 0.1f),
                                        contentColor = if (isAttending) AppColors.Student else Color.Red,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = if (isAttending) "Attending" else "Non-attending",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(id, fontSize = 12.sp, color = AppColors.TextSecondary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    AttendancePercentageBadge(
                                        studentId = id,
                                        backgroundColor = AppColors.Admin.copy(alpha = 0.15f),
                                        textColor = AppColors.Admin
                                    )
                                }
                            }
                            
                            IconButton(onClick = { showResetPassword = student }) {
                                Icon(Icons.Default.LockReset, contentDescription = "Reset Password", tint = AppColors.Admin)
                            }
                            IconButton(onClick = { showDeleteConfirm = student }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedStudent != null) {
        val student = selectedStudent!!
        AlertDialog(
            onDismissRequest = { selectedStudent = null },
            title = { Text("Student Details") },
            text = {
                Column {
                    Text("Name: ${student["fullName"] ?: ""}")
                    Text("ID: ${student["id"] ?: ""}")
                    Text("Email: ${student["email"] ?: ""}")
                    Text("Course: ${student["course"] ?: ""}")
                    Text("Session: ${student["session"] ?: ""}")
                    Text("Batch: ${student["batch"] ?: ""}")
                    Text("Year: ${student["year"] ?: ""}")
                    Text("DOB: ${student["dob"] ?: ""}")
                    Text("Phone: ${student["phoneNumber"] ?: ""}")
                    Text("Gender: ${student["gender"] ?: ""}")
                    Text("Password: ${student["password"] ?: ""}")
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    AttendanceStatsCard(
                        studentId = student["id"] as? String ?: "",
                        cardColor = AppColors.Admin.copy(alpha = 0.05f)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedStudent = null }) {
                    Text("Close")
                }
            }
        )
    }

    if (showDeleteConfirm != null) {
        val student = showDeleteConfirm!!
        val docId = student["docId"] as? String
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("Delete Student") },
            text = { Text("Are you sure you want to delete ${student["fullName"]}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (docId != null) {
                            firestore.collection("students").document(docId).delete()
                                .addOnSuccessListener {
                                    android.widget.Toast.makeText(context, "Student deleted", android.widget.Toast.LENGTH_SHORT).show()
                                    showDeleteConfirm = null
                                }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showResetPassword != null) {
        val student = showResetPassword!!
        val docId = student["docId"] as? String
        var newPassword by remember { mutableStateOf("") }
        var isSaving by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showResetPassword = null },
            title = { Text("Reset Password for ${student["fullName"]}") },
            text = {
                Column {
                    Text("Current Password: ${student["password"] ?: "Not Set"}")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("New Password") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newPassword.isNotBlank() && docId != null) {
                            isSaving = true
                            firestore.collection("students").document(docId)
                                .update("password", newPassword)
                                .addOnSuccessListener {
                                    isSaving = false
                                    android.widget.Toast.makeText(context, "Password reset successfully", android.widget.Toast.LENGTH_SHORT).show()
                                    showResetPassword = null
                                }
                        }
                    },
                    enabled = !isSaving
                ) {
                    Text(if (isSaving) "Saving..." else "Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetPassword = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
