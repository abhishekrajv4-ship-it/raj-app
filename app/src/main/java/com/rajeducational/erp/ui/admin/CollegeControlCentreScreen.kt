package com.rajeducational.erp.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Delete
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
import com.rajeducational.erp.data.College
import com.rajeducational.erp.theme.AppColors
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollegeControlCentreScreen(navController: NavController) {
    val firestore = FirebaseFirestore.getInstance()
    var colleges by remember { mutableStateOf<List<College>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()
    
    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        fetchColleges(firestore) { fetchedColleges ->
            colleges = fetchedColleges
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("College Control Centre", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.Admin,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = AppColors.Admin
            ) {
                Icon(Icons.Default.Add, "Add College", tint = Color.White)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(AppColors.Background)
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AppColors.Admin)
                }
            } else if (colleges.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No colleges added yet", color = AppColors.TextSecondary)
                }
            } else {
                LazyColumn(modifier = Modifier.padding(16.dp)) {
                    items(colleges) { college ->
                        CollegeAdminCard(college, firestore, onUpdated = {})
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var newCollegeName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add New College") },
            text = {
                OutlinedTextField(
                    value = newCollegeName,
                    onValueChange = { newCollegeName = it },
                    label = { Text("College Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newCollegeName.isNotBlank()) {
                        val newCollege = College(name = newCollegeName)
                        firestore.collection("colleges").add(newCollege).addOnSuccessListener {
                            showAddDialog = false
                        }
                    }
                }) {
                    Text("Add", color = AppColors.Admin)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel", color = AppColors.TextSecondary)
                }
            }
        )
    }
}

@Composable
fun CollegeAdminCard(college: College, firestore: FirebaseFirestore, onUpdated: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var showAddCourse by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Business, contentDescription = null, tint = AppColors.Admin)
                Spacer(modifier = Modifier.width(12.dp))
                Text(college.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AppColors.Navy, modifier = Modifier.weight(1f))
                IconButton(onClick = {
                    if (college.id.isNotEmpty()) {
                        firestore.collection("colleges").document(college.id).delete().addOnSuccessListener {
                            onUpdated()
                        }
                    }
                }) {
                    Icon(Icons.Default.Delete, "Delete", tint = Color.Red)
                }
            }
            
            if (expanded) {
                Divider(modifier = Modifier.padding(vertical = 12.dp))
                
                // Courses Section
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Courses (${college.courses.size})", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = { showAddCourse = true }) {
                        Text("+ Add Course", color = AppColors.Admin)
                    }
                }
                
                college.courses.forEachIndexed { courseIndex, course ->
                    var showAddBatch by remember { mutableStateOf(false) }
                    
                    Column(modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("• ${course.name}", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = AppColors.Navy)
                            Spacer(modifier = Modifier.weight(1f))
                            TextButton(onClick = { showAddBatch = true }, contentPadding = PaddingValues(0.dp)) {
                                Text("+ Batch", fontSize = 12.sp, color = AppColors.Admin)
                            }
                            IconButton(onClick = {
                                val updatedCourses = college.courses.toMutableList().apply { removeAt(courseIndex) }
                                firestore.collection("colleges").document(college.id).update("courses", updatedCourses)
                            }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Delete, "Delete Course", tint = Color.Red, modifier = Modifier.size(16.dp))
                            }
                        }
                        
                        // Batches for this course
                        if (course.yearBatches.isNotEmpty()) {
                            Row(modifier = Modifier.padding(start = 16.dp, top = 4.dp).fillMaxWidth()) {
                                Text("Batches: ", fontSize = 13.sp, color = AppColors.TextSecondary)
                                Text(course.yearBatches.joinToString(", "), fontSize = 13.sp, color = AppColors.TextPrimary)
                            }
                        }

                        if (showAddBatch) {
                            var newBatch by remember { mutableStateOf("") }
                            AlertDialog(
                                onDismissRequest = { showAddBatch = false },
                                title = { Text("Add Batch to ${course.name}") },
                                text = {
                                    OutlinedTextField(
                                        value = newBatch,
                                        onValueChange = { newBatch = it },
                                        label = { Text("Batch Year (e.g., 2024-2028)") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                },
                                confirmButton = {
                                    TextButton(onClick = {
                                        if (newBatch.isNotBlank()) {
                                            val updatedCourses = college.courses.toMutableList()
                                            val updatedCourse = course.copy(yearBatches = course.yearBatches + newBatch)
                                            updatedCourses[courseIndex] = updatedCourse
                                            firestore.collection("colleges").document(college.id).update("courses", updatedCourses).addOnSuccessListener {
                                                showAddBatch = false
                                            }
                                        }
                                    }) { Text("Add", color = AppColors.Admin) }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showAddBatch = false }) { Text("Cancel") }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddCourse) {
        var newCourse by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddCourse = false },
            title = { Text("Add Course to ${college.name}") },
            text = {
                OutlinedTextField(
                    value = newCourse,
                    onValueChange = { newCourse = it },
                    label = { Text("Course Name") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newCourse.isNotBlank()) {
                        val newCourseObj = com.rajeducational.erp.data.Course(name = newCourse)
                        val updatedCourses = college.courses.toMutableList().apply { add(newCourseObj) }
                        firestore.collection("colleges").document(college.id).update("courses", updatedCourses).addOnSuccessListener {
                            showAddCourse = false
                            onUpdated()
                        }
                    }
                }) { Text("Add", color = AppColors.Admin) }
            },
            dismissButton = {
                TextButton(onClick = { showAddCourse = false }) { Text("Cancel") }
            }
        )
    }
}

fun fetchColleges(firestore: FirebaseFirestore, onResult: (List<College>) -> Unit) {
    firestore.collection("colleges").addSnapshotListener { snapshot, _ ->
        if (snapshot != null) {
            val list = snapshot.documents.mapNotNull { doc ->
                doc.toObject(College::class.java)?.copy(id = doc.id)
            }
            onResult(list)
        }
    }
}
