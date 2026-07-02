package com.rajeducational.erp.ui.staff

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore
import com.rajeducational.erp.theme.AppColors
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.rajeducational.erp.ui.components.ProfileImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffAttendanceSettingsScreen(navController: NavController) {
    val firestore = FirebaseFirestore.getInstance()
    var timeInStart by remember { mutableStateOf("") }
    var timeInEnd by remember { mutableStateOf("") }
    var timeOutStart by remember { mutableStateOf("") }
    var timeOutEnd by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            val doc = firestore.collection("settings").document("attendance").get().await()
            if (doc.exists()) {
                timeInStart = doc.getString("timeInStart") ?: "10:00"
                timeInEnd = doc.getString("timeInEnd") ?: "10:30"
                timeOutStart = doc.getString("timeOutStart") ?: "16:00"
                timeOutEnd = doc.getString("timeOutEnd") ?: "16:30"
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Attendance Settings", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Staff)
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AppColors.Staff)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                Text("Time In Timing", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppColors.Navy)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = timeInStart,
                        onValueChange = { timeInStart = it },
                        label = { Text("Start Time (e.g. 10:00)") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = timeInEnd,
                        onValueChange = { timeInEnd = it },
                        label = { Text("End Time (e.g. 10:30)") },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text("Time Out Timing", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppColors.Navy)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = timeOutStart,
                        onValueChange = { timeOutStart = it },
                        label = { Text("Start Time (e.g. 16:00)") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = timeOutEnd,
                        onValueChange = { timeOutEnd = it },
                        label = { Text("End Time (e.g. 16:30)") },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = {
                        val data = hashMapOf(
                            "timeInStart" to timeInStart,
                            "timeInEnd" to timeInEnd,
                            "timeOutStart" to timeOutStart,
                            "timeOutEnd" to timeOutEnd
                        )
                        firestore.collection("settings").document("attendance").set(data)
                        navController.popBackStack()
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Staff),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Save Settings", color = Color.White, fontSize = 16.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffAttendanceReportControlScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Report Control", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Staff)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { navController.navigate("staff_attendance_report_individual") },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, "Individual Report", tint = AppColors.Staff)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Search Report of Individual Student", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ChevronRight, "Go", tint = AppColors.TextSecondary)
                }
            }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { navController.navigate("staff_attendance_report_class") },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Group, "Class Report", tint = AppColors.Staff)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Search Report of Full Class", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ChevronRight, "Go", tint = AppColors.TextSecondary)
                }
            }
        }
    }
}

data class StaffData(val id: String, val name: String, val profileUrl: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffAttendanceReportsScreen(navController: NavController, staffId: String) {
    var attendanceRecords by remember { mutableStateOf<List<AttendanceRecord>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(staffId) {
        try {
            // Assume the attendance collection has staffId for staffs attendance
            // Or maybe it's "studentId" used for staff as well. Let's query by studentId for staffs too
            // In FaceAttendanceScreen it uses "studentId" to store the recognized ID (which is the document ID from staffs/students)
            val snapshot = FirebaseFirestore.getInstance().collection("attendance")
                .whereEqualTo("studentId", staffId).get().await()
                
            val list = snapshot.documents.mapNotNull { doc ->
                val type = doc.getString("type") ?: ""
                val timestamp = doc.getLong("timestamp") ?: 0L
                AttendanceRecord(doc.id, type, timestamp)
            }.sortedByDescending { it.timestamp }
            attendanceRecords = list
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daily Reports", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Staff)
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AppColors.Staff)
            }
        } else if (attendanceRecords.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No attendance records found.", color = AppColors.TextSecondary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
            ) {
                items(attendanceRecords) { record ->
                    val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(record.timestamp))
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(if (record.type == "IN") "Time In" else "Time Out", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = if (record.type == "IN") Color(0xFF4CAF50) else Color(0xFFF44336))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(dateStr, fontSize = 14.sp, color = AppColors.TextSecondary)
                            }
                        }
                    }
                }
            }
        }
    }
}

data class AttendanceRecord(val id: String, val type: String, val timestamp: Long)
