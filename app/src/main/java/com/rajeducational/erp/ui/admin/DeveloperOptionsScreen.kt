package com.rajeducational.erp.ui.admin

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore
import com.rajeducational.erp.theme.AppColors
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperOptionsScreen(navController: NavController) {
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()
    var isProcessing by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Developer Options", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Background)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Testing Data", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AppColors.Navy)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            if (!isProcessing) {
                                isProcessing = true
                                GlobalScope.launch {
                                    try {
                                        feedTestingData(firestore)
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Testing data added successfully", Toast.LENGTH_LONG).show()
                                            isProcessing = false
                                        }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                            isProcessing = false
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.Admin),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isProcessing
                    ) {
                        Text(if (isProcessing) "Processing..." else "Feed testing data", modifier = Modifier.padding(8.dp))
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = {
                            if (!isProcessing) {
                                isProcessing = true
                                GlobalScope.launch {
                                    try {
                                        factoryResetData(firestore)
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "All data reset successfully", Toast.LENGTH_LONG).show()
                                            isProcessing = false
                                        }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                            isProcessing = false
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isProcessing
                    ) {
                        Text(if (isProcessing) "Processing..." else "Delete factory reset all the data", modifier = Modifier.padding(8.dp))
                    }
                }
            }
        }
    }
}

private suspend fun feedTestingData(firestore: FirebaseFirestore) {
    val batch = firestore.batch()
    
    val generatedTeachers = mutableListOf<String>()
    
    // Add 10 Teachers
    for (i in 1..10) {
        val teacherId = UUID.randomUUID().toString()
        generatedTeachers.add(teacherId)
        val docRef = firestore.collection("teachers").document(teacherId)
        batch.set(docRef, mapOf(
            "id" to teacherId,
            "name" to "Dummy Teacher $i",
            "phone" to "987654321${i.toString().padStart(2, '0')}",
            "email" to "teacher$i@test.com",
            "password" to "password123",
            "collegeId" to "test_college_id",
            "collegeName" to "Test College",
            "course" to "Test Course",
            "years" to listOf("1st Year", "2nd Year", "3rd Year"),
            "address" to "Teacher Address $i",
            "timestamp" to System.currentTimeMillis() - (i * 1000000)
        ))
    }
    
    val generatedStudents = mutableListOf<String>()
    
    // Add 15 Students
    for (i in 1..15) {
        val studentId = "STU" + System.currentTimeMillis().toString().takeLast(6) + i
        generatedStudents.add(studentId)
        val docRef = firestore.collection("students").document(studentId)
        batch.set(docRef, mapOf(
            "id" to studentId,
            "fullName" to "Dummy Student $i",
            "phone" to "876543210${i.toString().padStart(2, '0')}",
            "email" to "student$i@test.com",
            "password" to "password123",
            "college" to "Test College",
            "course" to "Test Course",
            "session" to "1st Year",
            "address" to "Student Address $i",
            "teacherId" to generatedTeachers[i % generatedTeachers.size],
            "timestamp" to System.currentTimeMillis() - (i * 1000000)
        ))
    }
    
    // Add Announcements for Students
    for (i in 1..15) {
        val docRef = firestore.collection("announcements_student").document()
        batch.set(docRef, mapOf(
            "title" to "Dummy Student Announcement $i",
            "content" to "This is a testing announcement for students. Description $i.",
            "timestamp" to System.currentTimeMillis() - (i * 500000)
        ))
    }
    
    // Add Announcements for Teachers
    for (i in 1..15) {
        val docRef = firestore.collection("announcements_teacher").document()
        batch.set(docRef, mapOf(
            "title" to "Dummy Teacher Announcement $i",
            "content" to "This is a testing announcement for teachers. Description $i.",
            "timestamp" to System.currentTimeMillis() - (i * 500000)
        ))
    }
    
    // Add Messages (student_chats)
    for (i in 1..15) {
        val docRef = firestore.collection("student_chats").document()
        val senderIsStudent = i % 2 == 0
        val sId = generatedStudents[i % generatedStudents.size]
        val tId = generatedTeachers[i % generatedTeachers.size]
        
        batch.set(docRef, mapOf(
            "studentId" to sId,
            "teacherId" to tId,
            "senderId" to if (senderIsStudent) sId else tId,
            "senderName" to if (senderIsStudent) "Dummy Student ${i % 15}" else "Dummy Teacher ${i % 10}",
            "message" to "Hello, this is a testing message $i",
            "timestamp" to System.currentTimeMillis() - (i * 100000),
            "isRead" to false
        ))
    }
    
    // Add Teacher Reviews
    for (i in 1..15) {
        val docRef = firestore.collection("teacher_reviews").document()
        val sId = generatedStudents[i % generatedStudents.size]
        val tId = generatedTeachers[i % generatedTeachers.size]
        batch.set(docRef, mapOf(
            "id" to docRef.id,
            "teacherId" to tId,
            "studentId" to sId,
            "rating" to (3 + (i % 3)), // 3, 4, 5
            "comment" to "This is a testing review $i.",
            "timestamp" to System.currentTimeMillis() - (i * 100000),
            "type" to "student"
        ))
    }
    
    batch.commit().await()
}

private suspend fun factoryResetData(firestore: FirebaseFirestore) {
    val collectionsToDelete = listOf(
        "students",
        "teachers",
        "announcements_student",
        "announcements_teacher",
        "student_chats",
        "teacher_reviews",
        "guests",
        "guest_replies"
    )
    
    for (collection in collectionsToDelete) {
        val snapshot = firestore.collection(collection).get().await()
        for (document in snapshot.documents) {
            firestore.collection(collection).document(document.id).delete().await()
        }
    }
}
