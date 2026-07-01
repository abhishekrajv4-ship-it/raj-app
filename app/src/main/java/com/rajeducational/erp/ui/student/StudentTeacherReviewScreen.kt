package com.rajeducational.erp.ui.student

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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
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
import kotlinx.coroutines.tasks.await

data class TeacherData(
    val id: String = "",
    val name: String = "",
    val collegeName: String = "",
    val course: String = "",
    val years: List<String> = emptyList()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentTeacherReviewScreen(navController: NavController) {
    var searchQuery by remember { mutableStateOf("") }
    var teachersList by remember { mutableStateOf<List<TeacherData>>(emptyList()) }
    var reviewCriteriaList by remember { mutableStateOf<List<com.rajeducational.erp.ui.admin.ReviewCriteria>>(emptyList()) }
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
        firestore.collection("teacher_review_criteria").get().addOnSuccessListener { snapshot ->
            reviewCriteriaList = snapshot.documents.mapNotNull { doc ->
                doc.toObject(com.rajeducational.erp.ui.admin.ReviewCriteria::class.java)?.copy(id = doc.id)
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
                title = { Text("Teacher Reviews", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Student, titleContentColor = Color.White, navigationIconContentColor = Color.White)
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
            
            Text(
                text = "Note: Your review will be anonymous. Your identity won't be revealed to the teacher when you submit a review.",
                fontSize = 12.sp,
                color = AppColors.TextSecondary,
                modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp)
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
        ReviewDialog(
            teacher = selectedTeacher!!,
            criteriaList = reviewCriteriaList,
            onDismiss = { showReviewDialog = false },
            onSubmit = { ratings ->
                val review = com.rajeducational.erp.ui.admin.TeacherReview(
                    teacherId = selectedTeacher!!.id,
                    teacherName = selectedTeacher!!.name,
                    college = selectedTeacher!!.collegeName,
                    course = selectedTeacher!!.course,
                    timestamp = System.currentTimeMillis(),
                    ratings = ratings
                )
                firestore.collection("teacher_reviews").add(review)
                    .addOnSuccessListener {
                        showReviewDialog = false
                        android.widget.Toast.makeText(navController.context, "Review submitted successfully", android.widget.Toast.LENGTH_SHORT).show()
                    }
            }
        )
    }
}

@Composable
fun ReviewDialog(
    teacher: TeacherData,
    criteriaList: List<com.rajeducational.erp.ui.admin.ReviewCriteria>,
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
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                val currentRating = ratings[criteria.title] ?: 0
                                repeat(5) { index ->
                                    Icon(
                                        imageVector = if (index < currentRating) Icons.Default.Star else Icons.Default.StarBorder,
                                        contentDescription = "Star ${index + 1}",
                                        tint = Color(0xFFFFC107),
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clickable {
                                                ratings = ratings.toMutableMap().apply {
                                                    this[criteria.title] = index + 1
                                                }
                                            }
                                    )
                                }
                            }
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
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Student)
            ) {
                Text(if (isSubmitting) "Submitting..." else "Submit Anonymous Review")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = AppColors.TextSecondary)
            }
        }
    )
}
