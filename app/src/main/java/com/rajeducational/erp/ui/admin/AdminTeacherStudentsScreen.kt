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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminTeacherStudentsScreen(navController: NavController, teacherId: String) {
    var students by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedStudent by remember { mutableStateOf<Map<String, Any>?>(null) }
    val firestore = FirebaseFirestore.getInstance()

    LaunchedEffect(teacherId) {
        firestore.collection("students")
            .whereEqualTo("teacherId", teacherId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    students = snapshot.documents.map { doc ->
                        doc.data ?: emptyMap()
                    }
                }
            }
    }

    val filteredStudents = students.filter {
        val name = it["fullName"] as? String ?: ""
        name.contains(searchQuery, ignoreCase = true)
    }

    if (selectedStudent != null) {
        StudentDetailsView(
            student = selectedStudent!!,
            onBack = { selectedStudent = null }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Teacher's Students", fontWeight = FontWeight.Bold, color = Color.White) },
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
                    items(filteredStudents) { student ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedStudent = student },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    (student["fullName"] as? String) ?: "Unknown",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = AppColors.Navy
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("ID: ${student["id"]}", fontSize = 14.sp, color = AppColors.TextSecondary)
                                Text("Course: ${student["course"]}", fontSize = 14.sp, color = AppColors.TextSecondary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDetailsView(student: Map<String, Any>, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Student Details", fontWeight = FontWeight.Bold, color = Color.White) },
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
                    Text("Name: ${student["fullName"] ?: "N/A"}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AppColors.Navy)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("ID/Roll No: ${student["id"] ?: "N/A"}", fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Email: ${student["email"] ?: "N/A"}", fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("College: ${student["college"] ?: "N/A"}", fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Course: ${student["course"] ?: "N/A"}", fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Session: ${student["session"] ?: "N/A"}", fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Phone: ${student["phone"] ?: "N/A"}", fontSize = 16.sp)
                }
            }
        }
    }
}
