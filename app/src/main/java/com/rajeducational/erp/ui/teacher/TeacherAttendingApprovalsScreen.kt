package com.rajeducational.erp.ui.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore
import com.rajeducational.erp.theme.AppColors
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherAttendingApprovalsScreen(navController: NavController) {
    val firestore = FirebaseFirestore.getInstance()
    var pendingStudents by remember { mutableStateOf<List<PendingStudent>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedStudent by remember { mutableStateOf<PendingStudent?>(null) }
    var studentToApprove by remember { mutableStateOf<PendingStudent?>(null) }
    var studentToDecline by remember { mutableStateOf<PendingStudent?>(null) }
    val coroutineScope = rememberCoroutineScope()
    
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("TeacherPrefs", android.content.Context.MODE_PRIVATE)
        val teacherId = prefs.getString("teacher_id", "") ?: ""
        var teacherCollege = prefs.getString("teacher_college", "") ?: ""
        var teacherCourses = prefs.getStringSet("teacher_courses", emptySet()) ?: emptySet()
        var teacherYears = prefs.getStringSet("teacher_years", emptySet()) ?: emptySet()
        
        if (teacherCourses.isEmpty() && teacherId.isNotEmpty()) {
            try {
                val doc = firestore.collection("teachers").document(teacherId).get().await()
                teacherCollege = doc.getString("collegeName") ?: doc.getString("college") ?: ""
                val courses = doc.get("courses") as? List<String> ?: listOf(doc.getString("course") ?: doc.getString("departmentName") ?: "")
                teacherCourses = courses.toSet()
                val years = doc.get("years") as? List<String> ?: emptyList()
                teacherYears = years.toSet()
                
                prefs.edit()
                    .putString("teacher_college", teacherCollege)
                    .putStringSet("teacher_courses", teacherCourses)
                    .putStringSet("teacher_years", teacherYears)
                    .apply()
            } catch(e: Exception) {}
        }
        
        firestore.collection("students")
            .whereEqualTo("isAttending", true)
            .whereEqualTo("approvalStatus", "pending")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val students = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(PendingStudent::class.java)?.copy(id = doc.id)
                    }.filter { student ->
                        student.college == teacherCollege &&
                        teacherCourses.contains(student.course) &&
                        teacherYears.contains(student.session)
                    }
                    pendingStudents = students
                }
                isLoading = false
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Attending Approvals", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Teacher)
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AppColors.Teacher)
            }
        } else if (pendingStudents.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No pending approvals", fontSize = 16.sp, color = AppColors.TextSecondary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .background(AppColors.Background)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(pendingStudents) { student ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedStudent = student },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(student.fullName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AppColors.Navy)
                                Text("${student.college} - ${student.course}", fontSize = 14.sp, color = AppColors.TextSecondary)
                                Text("Time: ${SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date())}", fontSize = 12.sp, color = AppColors.TextSecondary)
                            }
                            IconButton(onClick = { studentToDecline = student }) {
                                Icon(Icons.Default.Close, "Decline", tint = Color.Red)
                            }
                            IconButton(onClick = { studentToApprove = student }) {
                                Icon(Icons.Default.Check, "Approve", tint = Color(0xFF4CAF50))
                            }
                        }
                    }
                }
            }
        }
    }
    
    
    studentToApprove?.let { student ->
        AlertDialog(
            onDismissRequest = { studentToApprove = null },
            title = { Text("Accept this student?", fontWeight = FontWeight.Bold, color = AppColors.Navy) },
            text = { Text("Confirm Accept this student.") },
            confirmButton = {
                TextButton(onClick = {
                    coroutineScope.launch {
                        firestore.collection("students").document(student.id)
                            .update("approvalStatus", "approved").await()
                        studentToApprove = null
                    }
                }) {
                    Text("Accept this student", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { studentToApprove = null }) {
                    Text("Cancel", color = AppColors.TextSecondary)
                }
            }
        )
    }

    studentToDecline?.let { student ->
        AlertDialog(
            onDismissRequest = { studentToDecline = null },
            title = { Text("Decline this student?", fontWeight = FontWeight.Bold, color = AppColors.Navy) },
            text = { Text("Confirm Decline this student.") },
            confirmButton = {
                TextButton(onClick = {
                    coroutineScope.launch {
                        firestore.collection("students").document(student.id)
                            .update("approvalStatus", "declined").await()
                        studentToDecline = null
                    }
                }) {
                    Text("Decline this student", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { studentToDecline = null }) {
                    Text("Cancel", color = AppColors.TextSecondary)
                }
            }
        )
    }

    selectedStudent?.let { student ->
        AlertDialog(
            onDismissRequest = { selectedStudent = null },
            title = { Text("Student Details", fontWeight = FontWeight.Bold, color = AppColors.Navy) },
            text = {
                Column {
                    Text("Name: ${student.fullName}", fontSize = 16.sp, modifier = Modifier.padding(bottom = 4.dp))
                    Text("Email: ${student.email}", fontSize = 16.sp, modifier = Modifier.padding(bottom = 4.dp))
                    Text("Phone: ${student.phone}", fontSize = 16.sp, modifier = Modifier.padding(bottom = 4.dp))
                    Text("Address: ${student.address}", fontSize = 16.sp, modifier = Modifier.padding(bottom = 4.dp))
                    Text("College: ${student.college}", fontSize = 16.sp, modifier = Modifier.padding(bottom = 4.dp))
                    Text("Course: ${student.course}", fontSize = 16.sp, modifier = Modifier.padding(bottom = 4.dp))
                    Text("Session: ${student.session}", fontSize = 16.sp, modifier = Modifier.padding(bottom = 4.dp))
                }
            },
            confirmButton = {
                TextButton(onClick = { 
                    coroutineScope.launch {
                        firestore.collection("students").document(student.id)
                            .update("approvalStatus", "approved").await()
                        selectedStudent = null
                    }
                }) {
                    Text("Approve", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    coroutineScope.launch {
                        firestore.collection("students").document(student.id)
                            .update("approvalStatus", "declined").await()
                        selectedStudent = null
                    }
                }) {
                    Text("Decline", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
