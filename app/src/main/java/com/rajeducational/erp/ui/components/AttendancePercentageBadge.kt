package com.rajeducational.erp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import com.rajeducational.erp.theme.AppColors
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun AttendancePercentageBadge(
    studentId: String,
    modifier: Modifier = Modifier,
    textColor: Color = Color.White,
    backgroundColor: Color = AppColors.Navy
) {
    var percentage by remember { mutableIntStateOf(-1) }
    
    LaunchedEffect(studentId) {
        if (studentId.isNotEmpty()) {
            try {
                val db = FirebaseFirestore.getInstance()
                
                // Get user role and details to filter holidays specifically
                var userCollege = ""
                var userCourse = ""
                var userBatch = ""
                var isStaffOrTeacher = false
                
                val studentDoc = db.collection("students").document(studentId).get().await()
                if (studentDoc.exists()) {
                    userCollege = studentDoc.getString("college") ?: ""
                    userCourse = studentDoc.getString("course") ?: ""
                    userBatch = studentDoc.getString("session") ?: ""
                } else {
                    val teacherDoc = db.collection("teachers").document(studentId).get().await()
                    if (teacherDoc.exists()) {
                        userCollege = teacherDoc.getString("collegeName") ?: ""
                        isStaffOrTeacher = true
                    } else {
                        val staffDoc = db.collection("staffs").document(studentId).get().await()
                        if (staffDoc.exists()) {
                            userCollege = staffDoc.getString("collegeName") ?: ""
                            isStaffOrTeacher = true
                        }
                    }
                }

                // Get student's attendance records
                val userSnapshot = db.collection("attendance")
                    .whereEqualTo("studentId", studentId)
                    .get().await()
                
                // Fetch holidays
                val holidayDocs = db.collection("holidays").get().await().documents
                val holidayList = holidayDocs.mapNotNull { doc ->
                    val d = doc.getString("date") ?: ""
                    val name = doc.getString("name") ?: "Holiday"
                    val isAll = doc.getBoolean("isAll") ?: false
                    val selectedColleges = doc.get("selectedColleges") as? List<*>
                    val selectedCourses = doc.get("selectedCourses") as? List<*>
                    val selectedBatches = doc.get("selectedBatches") as? List<*>
                    
                    mapOf(
                        "date" to d,
                        "name" to name,
                        "isAll" to isAll,
                        "colleges" to selectedColleges,
                        "courses" to selectedCourses,
                        "batches" to selectedBatches
                    )
                }

                val isHolidayForUser = { dateStr: String ->
                    if (isStaffOrTeacher) {
                        com.rajeducational.erp.utils.HolidayHelper.isHolidayForTeacherStaff(dateStr, userCollege, holidayList)
                    } else {
                        com.rajeducational.erp.utils.HolidayHelper.isHolidayForStudent(dateStr, userCollege, userCourse, userBatch, holidayList)
                    }
                }
                
                val userDays = userSnapshot.documents.mapNotNull { doc ->
                    doc.getLong("timestamp")
                }.filter { ts ->
                    val cal = Calendar.getInstance()
                    cal.timeInMillis = ts
                    cal.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY
                }.map { ts ->
                    val cal = Calendar.getInstance()
                    cal.timeInMillis = ts
                    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(cal.time)
                }.filter { !isHolidayForUser(it) }.toSet().size
                
                // Fetch overall school working days
                val allSnapshot = db.collection("attendance").limit(1000).get().await()
                val totalDays = allSnapshot.documents.mapNotNull { doc ->
                    doc.getLong("timestamp")
                }.filter { ts ->
                    val cal = Calendar.getInstance()
                    cal.timeInMillis = ts
                    cal.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY
                }.map { ts ->
                    val cal = Calendar.getInstance()
                    cal.timeInMillis = ts
                    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(cal.time)
                }.filter { !isHolidayForUser(it) }.toSet().size.coerceAtLeast(1)
                
                percentage = ((userDays.toFloat() / totalDays.toFloat()) * 100).toInt().coerceAtMost(100)
            } catch (e: Exception) {
                e.printStackTrace()
                percentage = 0
            }
        }
    }
    
    if (percentage >= 0) {
        Surface(
            color = backgroundColor,
            shape = RoundedCornerShape(4.dp),
            modifier = modifier
        ) {
            Text(
                text = "$percentage% Attendance",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    } else {
        Surface(
            color = Color.LightGray.copy(alpha = 0.5f),
            shape = RoundedCornerShape(4.dp),
            modifier = modifier
        ) {
            Text(
                text = "Calculating...",
                fontSize = 10.sp,
                color = AppColors.TextSecondary,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
fun AttendanceStatsCard(
    studentId: String,
    modifier: Modifier = Modifier,
    cardColor: Color = Color.White
) {
    var presentDays by remember { mutableIntStateOf(0) }
    var totalDays by remember { mutableIntStateOf(0) }
    var percentage by remember { mutableIntStateOf(-1) }
    var isLoading by remember { mutableStateOf(true) }
    
    LaunchedEffect(studentId) {
        if (studentId.isNotEmpty()) {
            try {
                val db = FirebaseFirestore.getInstance()
                
                // Get user role and details to filter holidays specifically
                var userCollege = ""
                var userCourse = ""
                var userBatch = ""
                var isStaffOrTeacher = false
                
                val studentDoc = db.collection("students").document(studentId).get().await()
                if (studentDoc.exists()) {
                    userCollege = studentDoc.getString("college") ?: ""
                    userCourse = studentDoc.getString("course") ?: ""
                    userBatch = studentDoc.getString("session") ?: ""
                } else {
                    val teacherDoc = db.collection("teachers").document(studentId).get().await()
                    if (teacherDoc.exists()) {
                        userCollege = teacherDoc.getString("collegeName") ?: ""
                        isStaffOrTeacher = true
                    } else {
                        val staffDoc = db.collection("staffs").document(studentId).get().await()
                        if (staffDoc.exists()) {
                            userCollege = staffDoc.getString("collegeName") ?: ""
                            isStaffOrTeacher = true
                        }
                    }
                }

                val userSnapshot = db.collection("attendance")
                    .whereEqualTo("studentId", studentId)
                    .get().await()
                
                // Fetch holidays
                val holidayDocs = db.collection("holidays").get().await().documents
                val holidayList = holidayDocs.mapNotNull { doc ->
                    val d = doc.getString("date") ?: ""
                    val name = doc.getString("name") ?: "Holiday"
                    val isAll = doc.getBoolean("isAll") ?: false
                    val selectedColleges = doc.get("selectedColleges") as? List<*>
                    val selectedCourses = doc.get("selectedCourses") as? List<*>
                    val selectedBatches = doc.get("selectedBatches") as? List<*>
                    
                    mapOf(
                        "date" to d,
                        "name" to name,
                        "isAll" to isAll,
                        "colleges" to selectedColleges,
                        "courses" to selectedCourses,
                        "batches" to selectedBatches
                    )
                }

                val isHolidayForUser = { dateStr: String ->
                    if (isStaffOrTeacher) {
                        com.rajeducational.erp.utils.HolidayHelper.isHolidayForTeacherStaff(dateStr, userCollege, holidayList)
                    } else {
                        com.rajeducational.erp.utils.HolidayHelper.isHolidayForStudent(dateStr, userCollege, userCourse, userBatch, holidayList)
                    }
                }
                
                presentDays = userSnapshot.documents.mapNotNull { doc ->
                    doc.getLong("timestamp")
                }.filter { ts ->
                    val cal = Calendar.getInstance()
                    cal.timeInMillis = ts
                    cal.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY
                }.map { ts ->
                    val cal = Calendar.getInstance()
                    cal.timeInMillis = ts
                    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(cal.time)
                }.filter { !isHolidayForUser(it) }.toSet().size
                
                val allSnapshot = db.collection("attendance").limit(1000).get().await()
                totalDays = allSnapshot.documents.mapNotNull { doc ->
                    doc.getLong("timestamp")
                }.filter { ts ->
                    val cal = Calendar.getInstance()
                    cal.timeInMillis = ts
                    cal.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY
                }.map { ts ->
                    val cal = Calendar.getInstance()
                    cal.timeInMillis = ts
                    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(cal.time)
                }.filter { !isHolidayForUser(it) }.toSet().size.coerceAtLeast(1)
                
                percentage = ((presentDays.toFloat() / totalDays.toFloat()) * 100).toInt().coerceAtMost(100)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Attendance Statistics",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.Navy
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AppColors.Student)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Your Attendance: $percentage%",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (percentage >= 75) Color(0xFF2E7D32) else Color(0xFFD32F2F)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Present: $presentDays days | Total Class Days: $totalDays days",
                            fontSize = 13.sp,
                            color = AppColors.TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (percentage >= 75) "Great job! Your attendance is in good standing." else "Warning: Try to improve your attendance to stay above 75%.",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (percentage >= 75) Color(0xFF2E7D32) else Color(0xFFD32F2F)
                        )
                    }
                    
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .background(
                                if (percentage >= 75) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                                RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$percentage%",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = if (percentage >= 75) Color(0xFF2E7D32) else Color(0xFFFF1744)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                LinearProgressIndicator(
                    progress = { percentage / 100f },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = if (percentage >= 75) Color(0xFF2E7D32) else Color(0xFFD32F2F),
                    trackColor = Color.LightGray.copy(alpha = 0.3f)
                )
            }
        }
    }
}
