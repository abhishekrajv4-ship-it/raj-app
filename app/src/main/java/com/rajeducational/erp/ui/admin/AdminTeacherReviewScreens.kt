package com.rajeducational.erp.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.firestore.FirebaseFirestore
import com.rajeducational.erp.theme.AppColors
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ReviewCriteria(val id: String = "", val title: String = "")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminTeacherReviewCriteriaControlScreen(navController: NavController) {
    var criteriaList by remember { mutableStateOf<List<ReviewCriteria>>(emptyList()) }
    var newCriteriaTitle by remember { mutableStateOf("") }
    val firestore = FirebaseFirestore.getInstance()
    val coroutineScope = rememberCoroutineScope()
    var isAdding by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        firestore.collection("teacher_review_criteria")
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
                title = { Text("Teacher Review Control", fontWeight = FontWeight.Bold) },
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
                    Text("Add Judgment Criteria", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AppColors.Navy)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = newCriteriaTitle,
                        onValueChange = { newCriteriaTitle = it },
                        label = { Text("Criteria Title (e.g. Teaching Quality)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            if (newCriteriaTitle.isNotBlank()) {
                                isAdding = true
                                val criteria = ReviewCriteria(title = newCriteriaTitle.trim())
                                firestore.collection("teacher_review_criteria").add(criteria)
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
                                    firestore.collection("teacher_review_criteria").document(criteria.id).delete()
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

data class TeacherReview(
    val id: String = "",
    val teacherId: String = "",
    val teacherName: String = "",
    val college: String = "",
    val course: String = "",
    val timestamp: Long = 0,
    val ratings: Map<String, Int> = emptyMap(),
    val studentId: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminTeacherReviewsScreen(navController: NavController) {
    var reviewsList by remember { mutableStateOf<List<TeacherReview>>(emptyList()) }
    val firestore = FirebaseFirestore.getInstance()
    var isResetting by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var showResetConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        firestore.collection("teacher_reviews")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    reviewsList = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(TeacherReview::class.java)?.copy(id = doc.id)
                    }
                }
            }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Reset All Student Reviews?") },
            text = { Text("Are you sure you want to delete all student reviews? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        isResetting = true
                        firestore.collection("teacher_reviews").get()
                            .addOnSuccessListener { snapshot ->
                                val batch = firestore.batch()
                                for (doc in snapshot.documents) {
                                    batch.delete(doc.reference)
                                }
                                firestore.collection("staff_reviews").get()
                                    .addOnSuccessListener { staffSnapshot ->
                                        for (doc in staffSnapshot.documents) {
                                            batch.delete(doc.reference)
                                        }
                                        batch.commit()
                                            .addOnSuccessListener {
                                                android.widget.Toast.makeText(context, "Student reviews reset successfully", android.widget.Toast.LENGTH_SHORT).show()
                                                isResetting = false
                                                showResetConfirm = false
                                            }
                                            .addOnFailureListener {
                                                isResetting = false
                                            }
                                    }
                                    .addOnFailureListener {
                                        batch.commit()
                                            .addOnSuccessListener {
                                                android.widget.Toast.makeText(context, "Student reviews reset successfully", android.widget.Toast.LENGTH_SHORT).show()
                                                isResetting = false
                                                showResetConfirm = false
                                            }
                                            .addOnFailureListener {
                                                isResetting = false
                                            }
                                    }
                            }
                            .addOnFailureListener {
                                isResetting = false
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Teacher Reviews by Students", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
                actions = {
                    IconButton(onClick = { showResetConfirm = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Reset Reviews", tint = Color.White)
                    }
                    TextButton(onClick = { navController.navigate("admin_teacher_review_criteria_control") }) {
                        Text("Manage Criteria", color = Color.White)
                    }
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
            if (reviewsList.isEmpty()) {
                Text("No reviews submitted yet.", color = AppColors.TextSecondary)
            } else {
                reviewsList.forEach { review ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Teacher: ${review.teacherName}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AppColors.Navy)
                            Text("${review.course} | ${review.college}", fontSize = 14.sp, color = AppColors.TextSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            val dateString = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(review.timestamp))
                            Text("Submitted: $dateString", fontSize = 12.sp, color = Color.Gray)
                            
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            
                            review.ratings.forEach { (criteria, rating) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(criteria, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                    Row {
                                        repeat(5) { i ->
                                            Icon(
                                                imageVector = if (i < rating) Icons.Default.Star else Icons.Default.StarBorder,
                                                contentDescription = null,
                                                tint = Color(0xFFFFC107),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
