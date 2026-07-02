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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.firestore.FirebaseFirestore
import com.rajeducational.erp.theme.AppColors
import com.rajeducational.erp.ui.student.TeacherData
import coil.compose.AsyncImage
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.layout.ContentScale

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
    var reviewsMap by remember { mutableStateOf<Map<String, TeacherReview?>>(emptyMap()) }

    var selectedTeacher by remember { mutableStateOf<TeacherData?>(null) }
    var showReviewDialog by remember { mutableStateOf(false) }
    
    var showResetConfirm by remember { mutableStateOf(false) }
    var isResetting by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        firestore.collection("teachers").get().addOnSuccessListener { snapshot ->
            val teachers = snapshot.documents.mapNotNull { doc ->
                val name = doc.getString("name") ?: ""
                val collegeName = doc.getString("collegeName") ?: ""
                val course = doc.getString("course") ?: ""
                val years = doc.get("years") as? List<String> ?: emptyList()
                val profileUrl = doc.getString("profileUrl") ?: ""
                TeacherData(id = doc.id, name = name, collegeName = collegeName, course = course, years = years, isTeacher = true, profileUrl = profileUrl)
            }
            firestore.collection("staffs").get().addOnSuccessListener { staffSnapshot ->
                val staff = staffSnapshot.documents.mapNotNull { doc ->
                    val name = doc.getString("name") ?: ""
                    val collegeName = doc.getString("collegeName") ?: ""
                    val course = doc.getString("course") ?: ""
                    val years = doc.get("years") as? List<String> ?: emptyList()
                    val profileUrl = doc.getString("profileUrl") ?: ""
                    TeacherData(id = doc.id, name = name, collegeName = collegeName, course = course, years = years, isTeacher = false, profileUrl = profileUrl)
                }
                
                firestore.collection("management_reviews").get().addOnSuccessListener { reviewSnapshot ->
                    val reviews = reviewSnapshot.documents.associate { doc ->
                        doc.id to doc.toObject(TeacherReview::class.java)
                    }
                    reviewsMap = reviews
                    teachersList = (teachers + staff).map { t ->
                        val review = reviewsMap[t.id]
                        if (review != null && review.ratings.isNotEmpty()) {
                             val avg = review.ratings.values.average()
                             t.copy(rating = avg * 10) 
                        } else {
                            t
                        }
                    }
                }
            }
        }
        firestore.collection("management_review_criteria").get().addOnSuccessListener { snapshot ->
            reviewCriteriaList = snapshot.documents.mapNotNull { doc ->
                doc.toObject(ReviewCriteria::class.java)?.copy(id = doc.id)
            }
        }
    }
    
    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Reset All Management Reviews?") },
            text = { Text("Are you sure you want to delete all management reviews? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        isResetting = true
                        firestore.collection("management_reviews").get()
                            .addOnSuccessListener { snapshot ->
                                val batch = firestore.batch()
                                for (doc in snapshot.documents) {
                                    batch.delete(doc.reference)
                                }
                                batch.commit()
                                    .addOnSuccessListener {
                                        android.widget.Toast.makeText(context, "Management reviews reset successfully", android.widget.Toast.LENGTH_SHORT).show()
                                        isResetting = false
                                        showResetConfirm = false
                                        reviewsMap = emptyMap()
                                        teachersList = teachersList.map { it.copy(rating = 0.0) }
                                    }
                            }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) { Text(if (isResetting) "Resetting..." else "Reset") }
            },
            dismissButton = {
                Button(onClick = { showResetConfirm = false }) { Text("Cancel") }
            }
        )
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
                    IconButton(onClick = { showResetConfirm = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Reset Reviews", tint = Color.White)
                    }
                    TextButton(onClick = { navController.navigate("admin_management_review_criteria_control") }) {
                        Text("Manage Criteria", color = Color.White)
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
                label = { Text("Search teachers/staff...") },
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
                    val isLocked = reviewsMap[teacher.id] != null
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable(enabled = !isLocked) {
                            selectedTeacher = teacher
                            showReviewDialog = true
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = if (isLocked) Color.LightGray else Color.White)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (teacher.profileUrl.isNotEmpty()) {
                                coil.compose.AsyncImage(
                                    model = teacher.profileUrl,
                                    contentDescription = "Profile Picture",
                                    modifier = Modifier.size(50.dp).clip(androidx.compose.foundation.shape.CircleShape),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                            } else {
                                Box(modifier = Modifier.size(50.dp).clip(androidx.compose.foundation.shape.CircleShape).background(Color.Gray), contentAlignment = Alignment.Center) {
                                    Text(teacher.name.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text(teacher.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AppColors.Navy)
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(if (teacher.isTeacher) "Teacher" else "Staff", fontSize = 12.sp, color = if (teacher.isTeacher) Color.Blue else Color.Green)
                                        Text("${"%.1f".format(teacher.rating)}%", fontSize = 12.sp, color = AppColors.Admin, fontWeight = FontWeight.Bold)
                                        if (isLocked) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Icon(androidx.compose.material.icons.Icons.Default.Lock, contentDescription = "Locked", tint = Color.DarkGray, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
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
    }

    if (showReviewDialog && selectedTeacher != null) {
        ManagementReviewDialog(
            teacher = selectedTeacher!!,
            existingReview = reviewsMap[selectedTeacher!!.id],
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
                        // Update local state to reflect review submission
                        val updatedReviewsMap = reviewsMap.toMutableMap()
                        updatedReviewsMap[selectedTeacher!!.id] = review
                        reviewsMap = updatedReviewsMap
                        
                        teachersList = teachersList.map { t ->
                            if (t.id == selectedTeacher!!.id) {
                                 val avg = review.ratings.values.average()
                                 t.copy(rating = avg * 10) 
                            } else {
                                t
                            }
                        }
                        
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
    existingReview: TeacherReview?,
    criteriaList: List<ReviewCriteria>,
    onDismiss: () -> Unit,
    onSubmit: (Map<String, Int>) -> Unit
) {
    var ratings by remember { mutableStateOf(existingReview?.ratings ?: criteriaList.associate { it.title to 0 }) }
    var isSubmitting by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (existingReview != null) "Review for ${teacher.name} (Already Reviewed)" else "Review ${teacher.name}", fontWeight = FontWeight.Bold)
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
                                    if (existingReview == null) {
                                        ratings = ratings.toMutableMap().apply {
                                            this[criteria.title] = newValue.toInt()
                                        }
                                    }
                                },
                                valueRange = 0f..10f,
                                steps = 9,
                                enabled = existingReview == null
                            )
                            Text("Score: $currentRating / 10", fontSize = 12.sp, color = AppColors.TextSecondary)
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (existingReview == null) {
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
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (existingReview != null) "Close" else "Cancel", color = AppColors.TextSecondary)
            }
        }
    )
}
