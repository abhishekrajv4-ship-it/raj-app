package com.rajeducational.erp.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
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
import com.rajeducational.erp.ui.student.TeacherData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminManagementReviewCriteriaControlScreen(navController: NavController) {
    var criteriaList by remember { mutableStateOf<List<ReviewCriteria>>(emptyList()) }
    var newCriteriaTitle by remember { mutableStateOf("") }
    val firestore = FirebaseFirestore.getInstance()
    var isAdding by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        firestore.collection("management_review_criteria")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    criteriaList = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(ReviewCriteria::class.java)?.copy(id = doc.id)
                    }
                }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Management Review Criteria", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Admin, titleContentColor = Color.White, navigationIconContentColor = Color.White)
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
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Add Management Judgment Criteria", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AppColors.Navy)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = newCriteriaTitle,
                        onValueChange = { newCriteriaTitle = it },
                        label = { Text("Criteria Title (e.g. Punctuality)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            if (newCriteriaTitle.isNotBlank()) {
                                isAdding = true
                                val criteria = ReviewCriteria(title = newCriteriaTitle.trim())
                                firestore.collection("management_review_criteria").add(criteria)
                                    .addOnSuccessListener {
                                        newCriteriaTitle = ""
                                        isAdding = false
                                    }
                                    .addOnFailureListener {
                                        isAdding = false
                                    }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.Admin),
                        enabled = !isAdding && newCriteriaTitle.isNotBlank()
                    ) {
                        Text(if (isAdding) "Adding..." else "Add Criteria")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Existing Criteria", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppColors.Navy)
            Spacer(modifier = Modifier.height(8.dp))

            if (criteriaList.isEmpty()) {
                Text("No criteria added yet.", color = AppColors.TextSecondary)
            } else {
                criteriaList.forEach { criteria ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(criteria.title, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                            IconButton(
                                onClick = {
                                    firestore.collection("management_review_criteria").document(criteria.id).delete()
                                }
                            ) {
                                Icon(Icons.Default.Delete, "Delete", tint = Color.Red)
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
fun AdminManagementReviewControlScreen(navController: NavController) {
    var searchQuery by remember { mutableStateOf("") }
    var teachersList by remember { mutableStateOf<List<TeacherData>>(emptyList()) }
    var reviewCriteriaList by remember { mutableStateOf<List<ReviewCriteria>>(emptyList()) }
    val firestore = FirebaseFirestore.getInstance()

    var selectedTeacher by remember { mutableStateOf<TeacherData?>(null) }
    var showReviewDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        firestore.collection("teachers").get().addOnSuccessListener { snapshot ->
            teachersList = snapshot.documents.mapNotNull { doc ->
                val name = doc.getString("name") ?: ""
                val collegeName = doc.getString("collegeName") ?: ""
                val course = doc.getString("course") ?: ""
                val years = doc.get("years") as? List<String> ?: emptyList()
                TeacherData(id = doc.id, name = name, collegeName = collegeName, course = course, years = years)
            }
        }
        firestore.collection("management_review_criteria").get().addOnSuccessListener { snapshot ->
            reviewCriteriaList = snapshot.documents.mapNotNull { doc ->
                doc.toObject(ReviewCriteria::class.java)?.copy(id = doc.id)
            }
        }
    }

    val filteredTeachers = teachersList.filter {
        it.name.contains(searchQuery, ignoreCase = true) || 
        it.collegeName.contains(searchQuery, ignoreCase = true) ||
        it.course.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Management Review Center", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
                actions = {
                    TextButton(onClick = { navController.navigate("admin_management_review_criteria_control") }) {
                        Text("Manage Review Criteria", color = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Admin, titleContentColor = Color.White, actionIconContentColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(AppColors.Background)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search teachers...") },
                leadingIcon = { Icon(Icons.Default.Search, "Search") },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White)
            )
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredTeachers) { teacher ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable {
                            selectedTeacher = teacher
                            showReviewDialog = true
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(teacher.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AppColors.Navy)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("${teacher.course} | ${teacher.collegeName}", fontSize = 14.sp, color = AppColors.TextSecondary)
                            if (teacher.years.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Batches: ${teacher.years.joinToString(", ")}", fontSize = 12.sp, color = AppColors.TextSecondary)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showReviewDialog && selectedTeacher != null) {
        ManagementReviewDialog(
            teacher = selectedTeacher!!,
            criteriaList = reviewCriteriaList,
            onDismiss = { showReviewDialog = false },
            onSubmit = { ratings ->
                val review = TeacherReview(
                    teacherId = selectedTeacher!!.id,
                    teacherName = selectedTeacher!!.name,
                    college = selectedTeacher!!.collegeName,
                    course = selectedTeacher!!.course,
                    timestamp = System.currentTimeMillis(),
                    ratings = ratings
                )
                // Overwrite the management review for this teacher (or just store it by teacherId)
                firestore.collection("management_reviews").document(selectedTeacher!!.id).set(review)
                    .addOnSuccessListener {
                        showReviewDialog = false
                        android.widget.Toast.makeText(navController.context, "Review submitted successfully", android.widget.Toast.LENGTH_SHORT).show()
                    }
            }
        )
    }
}

@Composable
fun ManagementReviewDialog(
    teacher: TeacherData,
    criteriaList: List<ReviewCriteria>,
    onDismiss: () -> Unit,
    onSubmit: (Map<String, Int>) -> Unit
) {
    var ratings by remember { mutableStateOf(criteriaList.associate { it.title to 0 }) }
    var isSubmitting by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Review ${teacher.name}", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                if (criteriaList.isEmpty()) {
                    Text("No review criteria available.", color = AppColors.TextSecondary)
                } else {
                    criteriaList.forEach { criteria ->
                        Column(modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth()) {
                            Text(criteria.title, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(8.dp))
                            val currentRating = ratings[criteria.title] ?: 0
                            
                            Slider(
                                value = currentRating.toFloat(),
                                onValueChange = { newValue ->
                                    ratings = ratings.toMutableMap().apply {
                                        this[criteria.title] = newValue.toInt()
                                    }
                                },
                                valueRange = 0f..10f,
                                steps = 9
                            )
                            Text("Score: $currentRating / 10", fontSize = 12.sp, color = AppColors.TextSecondary)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isSubmitting = true
                    onSubmit(ratings)
                },
                enabled = !isSubmitting && criteriaList.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Admin)
            ) {
                Text(if (isSubmitting) "Submitting..." else "Submit Review")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = AppColors.TextSecondary)
            }
        }
    )
}
