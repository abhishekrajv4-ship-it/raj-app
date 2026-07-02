package com.rajeducational.erp.ui.admin

import android.app.DatePickerDialog
import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.rajeducational.erp.theme.AppColors

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

data class TeacherStaffData(val id: String, val name: String, val role: String)

data class RawRecordTS(
    val timestamp: Long,
    val type: String,
    val status: String,
    val name: String,
    val role: String,
    val id: String
)

data class PdfRecordTS(
    val name: String,
    val role: String,
    val timeIn: String,
    val timeOut: String,
    val status: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminTeacherStaffAttendanceControlScreen(navController: NavController) {
    val firestore = FirebaseFirestore.getInstance()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Attendance Control", "Attendance Settings", "Reports & Logs")

    // Settings States
    var timeInStart by remember { mutableStateOf("09:00") }
    var timeInEnd by remember { mutableStateOf("10:00") }
    var timeOutStart by remember { mutableStateOf("17:00") }
    var timeOutEnd by remember { mutableStateOf("18:00") }
    var isSavingSettings by remember { mutableStateOf(false) }

    // Reports States (Live Logs)
    var attendanceLogs by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var filteredLogs by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var isReportsLoading by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedRoleFilter by remember { mutableStateOf("All") } // "All", "Teacher", "Staff"

    // PDF Reports Configuration States
    var reportSubTab by remember { mutableStateOf("LOGS") } // "LOGS" or "PDF"
    var pdfReportOption by remember { mutableStateOf("INDIVIDUAL") } // "INDIVIDUAL" or "ALL"
    var selectedReportUser by remember { mutableStateOf<TeacherStaffData?>(null) }
    var reportStartDateStr by remember { mutableStateOf("") }
    var reportEndDateStr by remember { mutableStateOf("") }
    var isGeneratingReport by remember { mutableStateOf(false) }
    var generatedReportUrl by remember { mutableStateOf<String?>(null) }

    // Dropdown / Searchable teacher and staff list
    var allUsersList by remember { mutableStateOf<List<TeacherStaffData>>(emptyList()) }

    // Initialize Dates with Today
    val todayStr = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()) }
    LaunchedEffect(Unit) {
        reportStartDateStr = todayStr
        reportEndDateStr = todayStr
    }

    // Load Settings
    LaunchedEffect(Unit) {
        try {
            val doc = firestore.collection("settings").document("teacher_staff_attendance").get().await()
            if (doc.exists()) {
                timeInStart = doc.getString("timeInStart") ?: "09:00"
                timeInEnd = doc.getString("timeInEnd") ?: "10:00"
                timeOutStart = doc.getString("timeOutStart") ?: "17:00"
                timeOutEnd = doc.getString("timeOutEnd") ?: "18:00"
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Load Teachers & Staff for PDF generator selection
    LaunchedEffect(Unit) {
        try {
            val tSnapshot = firestore.collection("teachers").get().await()
            val tList = tSnapshot.documents.mapNotNull { doc ->
                val name = doc.getString("name") ?: return@mapNotNull null
                TeacherStaffData(doc.id, name, "Teacher")
            }

            val sSnapshot = firestore.collection("staffs").get().await()
            val sList = sSnapshot.documents.mapNotNull { doc ->
                val name = doc.getString("name") ?: return@mapNotNull null
                TeacherStaffData(doc.id, name, "Staff")
            }
            allUsersList = tList + sList
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Load Live Logs
    LaunchedEffect(selectedTab, reportSubTab) {
        if (selectedTab == 2 && reportSubTab == "LOGS") {
            isReportsLoading = true
            try {
                val snapshot = firestore.collection("attendance")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(200)
                    .get().await()

                val logs = snapshot.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    val role = data["role"] as? String ?: ""
                    if (role == "Teacher" || role == "Staff") {
                        data.plus("id" to doc.id)
                    } else {
                        null
                    }
                }
                attendanceLogs = logs
                filteredLogs = logs
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isReportsLoading = false
            }
        }
    }

    // Filter Logs for real-time list
    LaunchedEffect(searchQuery, selectedRoleFilter, attendanceLogs) {
        var temp = attendanceLogs
        if (selectedRoleFilter != "All") {
            temp = temp.filter { (it["role"] as? String) == selectedRoleFilter }
        }
        if (searchQuery.isNotBlank()) {
            temp = temp.filter {
                val name = (it["studentName"] as? String) ?: ""
                name.contains(searchQuery, ignoreCase = true)
            }
        }
        filteredLogs = temp
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Teacher & Staff Portal", fontWeight = FontWeight.Bold, color = Color.White) },
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
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = AppColors.Admin
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                    )
                }
            }

            if (selectedTab == 0) {
                // 1. Attendance Control Tab
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            text = "Attendance Scanner Control",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.Navy
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { navController.navigate("face_attendance_scanner_ts/IN") },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(AppColors.Admin.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Face, "Time In Scanner", tint = AppColors.Admin)
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text("Take Attendance (Time In)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AppColors.Navy)
                                    Text("Face & QR Recognition for Teachers & Staff", fontSize = 12.sp, color = AppColors.TextSecondary)
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(Icons.Default.ChevronRight, "Go", tint = AppColors.TextSecondary)
                            }
                        }
                    }

                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { navController.navigate("face_attendance_scanner_ts/OUT") },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(Color.Red.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Face, "Time Out Scanner", tint = Color.Red)
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text("Attendance (Time Out)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AppColors.Navy)
                                    Text("Face & QR Recognition for Teachers & Staff", fontSize = 12.sp, color = AppColors.TextSecondary)
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(Icons.Default.ChevronRight, "Go", tint = AppColors.TextSecondary)
                            }
                        }
                    }
                }
            } else if (selectedTab == 1) {
                // 2. Attendance Settings Tab
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            text = "Attendance Settings",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.Navy
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Time In Window Settings", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AppColors.Navy)
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(
                                        value = timeInStart,
                                        onValueChange = { timeInStart = it },
                                        label = { Text("Start Time (HH:mm)") },
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    OutlinedTextField(
                                        value = timeInEnd,
                                        onValueChange = { timeInEnd = it },
                                        label = { Text("End Time (HH:mm)") },
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                Text("Time Out Window Settings", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AppColors.Navy)
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(
                                        value = timeOutStart,
                                        onValueChange = { timeOutStart = it },
                                        label = { Text("Start Time (HH:mm)") },
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    OutlinedTextField(
                                        value = timeOutEnd,
                                        onValueChange = { timeOutEnd = it },
                                        label = { Text("End Time (HH:mm)") },
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                Button(
                                    onClick = {
                                        isSavingSettings = true
                                        coroutineScope.launch {
                                            try {
                                                firestore.collection("settings").document("teacher_staff_attendance")
                                                    .set(
                                                        mapOf(
                                                            "timeInStart" to timeInStart,
                                                            "timeInEnd" to timeInEnd,
                                                            "timeOutStart" to timeOutStart,
                                                            "timeOutEnd" to timeOutEnd
                                                        )
                                                    ).await()
                                                android.widget.Toast.makeText(context, "Settings saved successfully", android.widget.Toast.LENGTH_SHORT).show()
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                                android.widget.Toast.makeText(context, "Failed to save settings", android.widget.Toast.LENGTH_SHORT).show()
                                            } finally {
                                                isSavingSettings = false
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Admin),
                                    shape = RoundedCornerShape(8.dp),
                                    enabled = !isSavingSettings
                                ) {
                                    if (isSavingSettings) {
                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                                    } else {
                                        Text("Save Attendance Settings", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (selectedTab == 2) {
                // 3. Reports & Logs Tab
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Segmented selection for Logs vs PDF Reports
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { reportSubTab = "LOGS" },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (reportSubTab == "LOGS") AppColors.Admin else Color.White,
                                contentColor = if (reportSubTab == "LOGS") Color.White else AppColors.Admin
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.Admin),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.List, contentDescription = "Logs", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Live Logs", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { reportSubTab = "PDF" },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (reportSubTab == "PDF") AppColors.Admin else Color.White,
                                contentColor = if (reportSubTab == "PDF") Color.White else AppColors.Admin
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.Admin),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF Reports", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("PDF Reports", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (reportSubTab == "LOGS") {
                        // LOGS VIEW
                        Text(
                            text = "Live Log History",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.Navy
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Role filter & search
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search by name...") },
                            leadingIcon = { Icon(Icons.Default.Search, "Search") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("All", "Teacher", "Staff").forEach { role ->
                                FilterChip(
                                    selected = selectedRoleFilter == role,
                                    onClick = { selectedRoleFilter = role },
                                    label = { Text(role) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = AppColors.Admin.copy(alpha = 0.15f),
                                        selectedLabelColor = AppColors.Admin
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (isReportsLoading) {
                            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = AppColors.Admin)
                            }
                        } else if (filteredLogs.isEmpty()) {
                            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text("No logs found", color = AppColors.TextSecondary, textAlign = TextAlign.Center)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(filteredLogs) { log ->
                                    val name = log["studentName"] as? String ?: "N/A"
                                    val role = log["role"] as? String ?: "Teacher"
                                    val method = log["method"] as? String ?: "Facial Recognition"
                                    val timestamp = log["timestamp"] as? Long ?: 0L
                                    val status = log["status"] as? String ?: "Present"
                                    val type = log["type"] as? String ?: "IN"

                                    val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                                    val formattedDate = if (timestamp > 0) sdf.format(Date(timestamp)) else "N/A"

                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        shape = RoundedCornerShape(12.dp),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(45.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (role == "Teacher") AppColors.Teacher.copy(alpha = 0.15f)
                                                        else AppColors.Staff.copy(alpha = 0.15f)
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = name.firstOrNull()?.toString() ?: "U",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 18.sp,
                                                    color = if (role == "Teacher") AppColors.Teacher else AppColors.Staff
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(12.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = name,
                                                        fontSize = 15.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = AppColors.Navy
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Surface(
                                                        color = if (role == "Teacher") AppColors.Teacher.copy(alpha = 0.15f) else AppColors.Staff.copy(alpha = 0.15f),
                                                        shape = RoundedCornerShape(4.dp)
                                                    ) {
                                                        Text(
                                                            text = role.uppercase(),
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = if (role == "Teacher") AppColors.Teacher else AppColors.Staff,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = "$method | $type",
                                                    fontSize = 12.sp,
                                                    color = AppColors.TextSecondary
                                                )
                                                Text(
                                                    text = formattedDate,
                                                    fontSize = 11.sp,
                                                    color = Color.Gray
                                                )
                                            }

                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = if (status.contains("Late", ignoreCase = true)) "Late" else "On Time",
                                                    color = if (status.contains("Late", ignoreCase = true)) Color.Red else Color(0xFF2E7D32),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // PDF REPORTS VIEW
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            item {
                                Text(
                                    text = "PDF Attendance Report Generator",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AppColors.Navy
                                )
                                Text(
                                    text = "Generate and upload official report files",
                                    fontSize = 12.sp,
                                    color = AppColors.TextSecondary
                                )
                            }

                            // Individual vs All Filter selection
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilterChip(
                                        selected = pdfReportOption == "INDIVIDUAL",
                                        onClick = {
                                            pdfReportOption = "INDIVIDUAL"
                                            generatedReportUrl = null
                                        },
                                        label = { Text("Search Individual Report") },
                                        modifier = Modifier.weight(1f),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = AppColors.Admin.copy(alpha = 0.15f),
                                            selectedLabelColor = AppColors.Admin
                                        )
                                    )
                                    FilterChip(
                                        selected = pdfReportOption == "ALL",
                                        onClick = {
                                            pdfReportOption = "ALL"
                                            selectedReportUser = null
                                            generatedReportUrl = null
                                        },
                                        label = { Text("Search All Teachers") },
                                        modifier = Modifier.weight(1f),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = AppColors.Admin.copy(alpha = 0.15f),
                                            selectedLabelColor = AppColors.Admin
                                        )
                                    )
                                }
                            }

                            // 1. Search and Select User for Individual option
                            if (pdfReportOption == "INDIVIDUAL") {
                                item {
                                    var userSearchQuery by remember { mutableStateOf("") }
                                    var isDropdownExpanded by remember { mutableStateOf(false) }

                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            text = "1. Search report of individual teacher or staff",
                                            fontWeight = FontWeight.Bold,
                                            color = AppColors.Navy,
                                            fontSize = 14.sp
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))

                                        if (selectedReportUser == null) {
                                            OutlinedTextField(
                                                value = userSearchQuery,
                                                onValueChange = {
                                                    userSearchQuery = it
                                                    isDropdownExpanded = it.isNotEmpty()
                                                },
                                                placeholder = { Text("Type name to search...") },
                                                leadingIcon = { Icon(Icons.Default.Person, "Person") },
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(8.dp),
                                                singleLine = true
                                            )

                                            if (isDropdownExpanded) {
                                                val suggestions = allUsersList.filter { it.name.contains(userSearchQuery, ignoreCase = true) }
                                                Card(
                                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp).heightIn(max = 200.dp),
                                                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                                                        items(suggestions) { u ->
                                                            Row(
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .clickable {
                                                                        selectedReportUser = u
                                                                        userSearchQuery = ""
                                                                        isDropdownExpanded = false
                                                                    }
                                                                    .padding(12.dp),
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Icon(Icons.Default.Person, "User", tint = AppColors.Admin, modifier = Modifier.size(18.dp))
                                                                Spacer(modifier = Modifier.width(8.dp))
                                                                Column {
                                                                    Text(u.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AppColors.Navy)
                                                                    Text(u.role, fontSize = 11.sp, color = AppColors.TextSecondary)
                                                                }
                                                            }
                                                            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                                                        }
                                                    }
                                                }
                                            }
                                        } else {
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = CardDefaults.cardColors(containerColor = AppColors.Admin.copy(alpha = 0.05f)),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(Icons.Default.CheckCircle, "Selected", tint = Color(0xFF2E7D32))
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(selectedReportUser!!.name, fontWeight = FontWeight.Bold, color = AppColors.Navy, fontSize = 15.sp)
                                                        Text("Role: ${selectedReportUser!!.role}", fontSize = 12.sp, color = AppColors.TextSecondary)
                                                    }
                                                    IconButton(onClick = { selectedReportUser = null }) {
                                                        Icon(Icons.Default.Close, "Clear Selection", tint = Color.Red)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                item {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            text = "1. Search report of all teachers",
                                            fontWeight = FontWeight.Bold,
                                            color = AppColors.Navy,
                                            fontSize = 14.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Surface(
                                            color = AppColors.Admin.copy(alpha = 0.05f),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "PDF will include daily log summary of all registered Teachers and Staff members.",
                                                fontSize = 13.sp,
                                                color = AppColors.Navy,
                                                modifier = Modifier.padding(12.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // 2. Date Selection (Shared)
                            item {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = "2. Select Date Range",
                                        fontWeight = FontWeight.Bold,
                                        color = AppColors.Navy,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = reportStartDateStr,
                                            onValueChange = {},
                                            label = { Text("Start Date") },
                                            readOnly = true,
                                            modifier = Modifier.weight(1f),
                                            trailingIcon = {
                                                IconButton(onClick = {
                                                    val calendar = Calendar.getInstance()
                                                    DatePickerDialog(
                                                        context,
                                                        { _, year, month, day ->
                                                            val selectedDate = Calendar.getInstance().apply { set(year, month, day) }
                                                            reportStartDateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(selectedDate.time)
                                                        },
                                                        calendar.get(Calendar.YEAR),
                                                        calendar.get(Calendar.MONTH),
                                                        calendar.get(Calendar.DAY_OF_MONTH)
                                                    ).show()
                                                }) {
                                                    Icon(Icons.Default.DateRange, "Select Date", tint = AppColors.Admin)
                                                }
                                            }
                                        )

                                        OutlinedTextField(
                                            value = reportEndDateStr,
                                            onValueChange = {},
                                            label = { Text("End Date") },
                                            readOnly = true,
                                            modifier = Modifier.weight(1f),
                                            trailingIcon = {
                                                IconButton(onClick = {
                                                    val calendar = Calendar.getInstance()
                                                    DatePickerDialog(
                                                        context,
                                                        { _, year, month, day ->
                                                            val selectedDate = Calendar.getInstance().apply { set(year, month, day) }
                                                            reportEndDateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(selectedDate.time)
                                                        },
                                                        calendar.get(Calendar.YEAR),
                                                        calendar.get(Calendar.MONTH),
                                                        calendar.get(Calendar.DAY_OF_MONTH)
                                                    ).show()
                                                }) {
                                                    Icon(Icons.Default.DateRange, "Select Date", tint = AppColors.Admin)
                                                }
                                            }
                                        )
                                    }
                                }
                            }

                            // 3. Generate Trigger Button
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        if (pdfReportOption == "INDIVIDUAL" && selectedReportUser == null) {
                                            Toast.makeText(context, "Please select a Teacher or Staff member first", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        isGeneratingReport = true
                                        coroutineScope.launch {
                                            try {
                                                val url = if (pdfReportOption == "INDIVIDUAL") {
                                                    generateIndividualTeacherStaffReport(context, selectedReportUser!!, reportStartDateStr, reportEndDateStr)
                                                } else {
                                                    generateAllTeachersStaffReport(context, allUsersList, reportStartDateStr, reportEndDateStr)
                                                }
                                                if (url != null) {
                                                    generatedReportUrl = url
                                                    Toast.makeText(context, "PDF Report generated successfully!", Toast.LENGTH_LONG).show()
                                                } else {
                                                    Toast.makeText(context, "Failed to generate PDF Report", Toast.LENGTH_SHORT).show()
                                                }
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                                Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                            } finally {
                                                isGeneratingReport = false
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Admin),
                                    shape = RoundedCornerShape(12.dp),
                                    enabled = !isGeneratingReport
                                ) {
                                    if (isGeneratingReport) {
                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text("Generating PDF...", fontWeight = FontWeight.Bold)
                                    } else {
                                        Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF")
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (pdfReportOption == "INDIVIDUAL") "Generate Individual Report PDF" else "Generate All Teachers Report PDF",
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // 4. Output view link block
                            if (generatedReportUrl != null) {
                                item {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.CheckCircle, "Success", tint = Color(0xFF2E7D32), modifier = Modifier.size(28.dp))
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text("Report Ready to View/Download!", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                            }
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Button(
                                                onClick = {
                                                    com.rajeducational.erp.ui.common.PdfOpener.openPdf(context, generatedReportUrl ?: "", coroutineScope)
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Icon(Icons.Default.OpenInNew, "Open", tint = Color.White)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Open / Share PDF", fontWeight = FontWeight.Bold)
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
}

suspend fun generateIndividualTeacherStaffReport(
    context: Context,
    user: TeacherStaffData,
    startDateStr: String,
    endDateStr: String
): String? {
    val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    val startCal = Calendar.getInstance()
    if (startDateStr.isNotEmpty()) {
        try { format.parse(startDateStr)?.let { startCal.time = it } } catch (e: Exception) {}
    }
    startCal.set(Calendar.HOUR_OF_DAY, 0)
    startCal.set(Calendar.MINUTE, 0)
    startCal.set(Calendar.SECOND, 0)
    startCal.set(Calendar.MILLISECOND, 0)
    val startTimestamp = startCal.timeInMillis

    val endCal = Calendar.getInstance()
    if (endDateStr.isNotEmpty()) {
        try { format.parse(endDateStr)?.let { endCal.time = it } } catch (e: Exception) {}
    }
    endCal.set(Calendar.HOUR_OF_DAY, 23)
    endCal.set(Calendar.MINUTE, 59)
    endCal.set(Calendar.SECOND, 59)
    endCal.set(Calendar.MILLISECOND, 999)
    val endTimestamp = endCal.timeInMillis

    return try {
        val snapshot = FirebaseFirestore.getInstance().collection("attendance")
            .whereEqualTo("studentId", user.id)
            .get().await()

        val rawRecords = snapshot.documents.mapNotNull { doc ->
            val ts = doc.getLong("timestamp") ?: 0L
            val type = doc.getString("type") ?: ""
            val status = doc.getString("status") ?: ""
            val name = doc.getString("studentName") ?: user.name
            val role = doc.getString("role") ?: user.role
            if (ts in startTimestamp..endTimestamp) {
                RawRecordTS(ts, type, status, name, role, user.id)
            } else null
        }

        val recordsByDate = mutableMapOf<String, List<PdfRecordTS>>()
        val daysCount = ((endTimestamp - startTimestamp) / (1000 * 60 * 60 * 24)).toInt() + 1

        for (i in 0 until daysCount) {
            val currentDayCal = Calendar.getInstance().apply {
                timeInMillis = startTimestamp
                add(Calendar.DAY_OF_YEAR, i)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val dateStr = format.format(currentDayCal.time)

            val dayRaw = rawRecords.filter { format.format(Date(it.timestamp)) == dateStr }
            val isSunday = currentDayCal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY

            val pdfRec = if (isSunday) {
                PdfRecordTS(user.name, user.role, "Sunday", "Sunday", "Sunday")
            } else if (dayRaw.isEmpty()) {
                val statusStr = if (currentDayCal.after(today)) "Upcoming" else "Absent"
                PdfRecordTS(user.name, user.role, "--", "--", statusStr)
            } else {
                var tIn = "--"
                var tOut = "--"
                var st = "Present"
                dayRaw.forEach { raw ->
                    if (raw.type.equals("IN", ignoreCase = true)) {
                        tIn = timeFormat.format(Date(raw.timestamp))
                        if (raw.status.contains("Late", ignoreCase = true)) {
                            st = "Late"
                        }
                    } else if (raw.type.equals("OUT", ignoreCase = true)) {
                        tOut = timeFormat.format(Date(raw.timestamp))
                    }
                }
                PdfRecordTS(user.name, user.role, tIn, tOut, st)
            }
            recordsByDate[dateStr] = listOf(pdfRec)
        }

        createAndUploadTSPdf(
            context = context,
            title = "Individual Report: ${user.name} (${user.role})",
            startDateStr = startDateStr,
            endDateStr = endDateStr,
            recordsByDate = recordsByDate
        )
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

suspend fun generateAllTeachersStaffReport(
    context: Context,
    allUsers: List<TeacherStaffData>,
    startDateStr: String,
    endDateStr: String
): String? {
    val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    val startCal = Calendar.getInstance()
    if (startDateStr.isNotEmpty()) {
        try { format.parse(startDateStr)?.let { startCal.time = it } } catch (e: Exception) {}
    }
    startCal.set(Calendar.HOUR_OF_DAY, 0)
    startCal.set(Calendar.MINUTE, 0)
    startCal.set(Calendar.SECOND, 0)
    startCal.set(Calendar.MILLISECOND, 0)
    val startTimestamp = startCal.timeInMillis

    val endCal = Calendar.getInstance()
    if (endDateStr.isNotEmpty()) {
        try { format.parse(endDateStr)?.let { endCal.time = it } } catch (e: Exception) {}
    }
    endCal.set(Calendar.HOUR_OF_DAY, 23)
    endCal.set(Calendar.MINUTE, 59)
    endCal.set(Calendar.SECOND, 59)
    endCal.set(Calendar.MILLISECOND, 999)
    val endTimestamp = endCal.timeInMillis

    return try {
        val snapshot = FirebaseFirestore.getInstance().collection("attendance")
            .get().await()

        val rawRecords = snapshot.documents.mapNotNull { doc ->
            val ts = doc.getLong("timestamp") ?: 0L
            val type = doc.getString("type") ?: ""
            val status = doc.getString("status") ?: ""
            val name = doc.getString("studentName") ?: ""
            val role = doc.getString("role") ?: ""
            val studentId = doc.getString("studentId") ?: ""
            if (ts in startTimestamp..endTimestamp && (role == "Teacher" || role == "Staff")) {
                RawRecordTS(ts, type, status, name, role, studentId)
            } else null
        }

        val recordsByDate = mutableMapOf<String, List<PdfRecordTS>>()
        val daysCount = ((endTimestamp - startTimestamp) / (1000 * 60 * 60 * 24)).toInt() + 1

        for (i in 0 until daysCount) {
            val currentDayCal = Calendar.getInstance().apply {
                timeInMillis = startTimestamp
                add(Calendar.DAY_OF_YEAR, i)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val dateStr = format.format(currentDayCal.time)

            val dayRecords = mutableListOf<PdfRecordTS>()

            val isSunday = currentDayCal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
            for (user in allUsers) {
                if (isSunday) {
                    dayRecords.add(PdfRecordTS(user.name, user.role, "Sunday", "Sunday", "Sunday"))
                } else {
                    val userRaw = rawRecords.filter { format.format(Date(it.timestamp)) == dateStr && it.id == user.id }
                    if (userRaw.isEmpty()) {
                        val statusStr = if (currentDayCal.after(today)) "Upcoming" else "Absent"
                        dayRecords.add(PdfRecordTS(user.name, user.role, "--", "--", statusStr))
                    } else {
                        var tIn = "--"
                        var tOut = "--"
                        var st = "Present"
                        userRaw.forEach { raw ->
                            if (raw.type.equals("IN", ignoreCase = true)) {
                                    tIn = timeFormat.format(Date(raw.timestamp))
                                    if (raw.status.contains("Late", ignoreCase = true)) {
                                        st = "Late"
                                    }
                            } else if (raw.type.equals("OUT", ignoreCase = true)) {
                                tOut = timeFormat.format(Date(raw.timestamp))
                            }
                        }
                        dayRecords.add(PdfRecordTS(user.name, user.role, tIn, tOut, st))
                    }
                }
            }
            recordsByDate[dateStr] = dayRecords
        }

        createAndUploadTSPdf(
            context = context,
            title = "All Teachers & Staff Attendance Report",
            startDateStr = startDateStr,
            endDateStr = endDateStr,
            recordsByDate = recordsByDate
        )
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

suspend fun createAndUploadTSPdf(
    context: Context,
    title: String,
    startDateStr: String,
    endDateStr: String,
    recordsByDate: Map<String, List<PdfRecordTS>>
): String? {
    val pdfDocument = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
    var page = pdfDocument.startPage(pageInfo)
    var canvas = page.canvas
    val paint = Paint()

    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paint.textSize = 18f
    val generatedDate = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()).format(Date())
    canvas.drawText("Teacher & Staff Attendance Report", 50f, 50f, paint)

    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    paint.textSize = 12f
    canvas.drawText("Generated On: $generatedDate", 50f, 80f, paint)
    canvas.drawText("Title: $title", 50f, 100f, paint)
    val filterText = "Date Filter: " + (if (startDateStr.isEmpty()) "Start" else startDateStr) + " to " + (if (endDateStr.isEmpty()) "End" else endDateStr)
    canvas.drawText(filterText, 50f, 120f, paint)

    canvas.drawLine(50f, 140f, 545f, 140f, paint)

    var yPosition = 160f

    for ((dateStr, records) in recordsByDate) {
        if (yPosition > 750f) {
            pdfDocument.finishPage(page)
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas
            yPosition = 50f
        }

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = android.graphics.Color.BLUE
        paint.textSize = 14f
        canvas.drawText("Date: $dateStr", 50f, yPosition, paint)
        yPosition += 20f

        paint.color = android.graphics.Color.BLACK
        paint.textSize = 11f
        canvas.drawText("Name", 50f, yPosition, paint)
        canvas.drawText("Role", 220f, yPosition, paint)
        canvas.drawText("Time In", 310f, yPosition, paint)
        canvas.drawText("Time Out", 390f, yPosition, paint)
        canvas.drawText("Status", 470f, yPosition, paint)
        yPosition += 8f
        canvas.drawLine(50f, yPosition, 545f, yPosition, paint)
        yPosition += 15f

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

        for (record in records) {
            if (yPosition > 800f) {
                pdfDocument.finishPage(page)
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                yPosition = 50f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("Name", 50f, yPosition, paint)
                canvas.drawText("Role", 220f, yPosition, paint)
                canvas.drawText("Time In", 310f, yPosition, paint)
                canvas.drawText("Time Out", 390f, yPosition, paint)
                canvas.drawText("Status", 470f, yPosition, paint)
                yPosition += 8f
                canvas.drawLine(50f, yPosition, 545f, yPosition, paint)
                yPosition += 15f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }

            val displayName = if (record.name.length > 22) record.name.substring(0, 20) + ".." else record.name
            canvas.drawText(displayName, 50f, yPosition, paint)
            canvas.drawText(record.role, 220f, yPosition, paint)
            canvas.drawText(record.timeIn, 310f, yPosition, paint)
            canvas.drawText(record.timeOut, 390f, yPosition, paint)

            if (record.status.equals("Late", ignoreCase = true)) {
                paint.color = android.graphics.Color.RED
            } else if (record.status.equals("Absent", ignoreCase = true)) {
                paint.color = android.graphics.Color.GRAY
            } else {
                paint.color = android.graphics.Color.rgb(46, 125, 50) // Green
            }
            canvas.drawText(record.status, 470f, yPosition, paint)
            paint.color = android.graphics.Color.BLACK

            yPosition += 20f
        }
        yPosition += 10f
    }

    pdfDocument.finishPage(page)

    val fileName = "ts_report_${System.currentTimeMillis()}.pdf"
    val file = java.io.File(context.cacheDir, fileName)
    var localUri: android.net.Uri? = null

    return try {
        pdfDocument.writeTo(java.io.FileOutputStream(file))
        pdfDocument.close()

        localUri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.provider", file)

        val storageRef = com.google.firebase.storage.FirebaseStorage.getInstance().reference.child("reports/$fileName")
        storageRef.putFile(android.net.Uri.fromFile(file)).await()

        val url = storageRef.downloadUrl.await().toString()
        val reportData = hashMapOf(
            "title" to title,
            "teacherName" to "Admin",
            "url" to url,
            "timestamp" to System.currentTimeMillis()
        )
        com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("reports").add(reportData).await()
        url
    } catch (e: Exception) {
        e.printStackTrace()
        localUri?.toString()
    }
}


@Composable
fun DailyTeachingPlanAdminTab(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val firestore = remember { FirebaseFirestore.getInstance() }

    var calendarState by remember { mutableStateOf(Calendar.getInstance()) }
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val dateString = dateFormatter.format(calendarState.time)

    var plansDataList by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var isGeneratingPdf by remember { mutableStateOf(false) }
    var generatedPdfUrl by remember { mutableStateOf<String?>(null) }

    // Fetch daily teaching plans for selected date
    LaunchedEffect(dateString) {
        isLoading = true
        generatedPdfUrl = null
        try {
            val snapshot = firestore.collection("daily_teaching_plans")
                .whereEqualTo("date", dateString)
                .get().await()
            
            plansDataList = snapshot.documents.map { doc ->
                doc.data ?: emptyMap()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Error fetching plans: " + e.message, Toast.LENGTH_SHORT).show()
        } finally {
            isLoading = false
        }
    }

    // Date picker dialog
    fun showDatePicker() {
        val year = calendarState.get(Calendar.YEAR)
        val month = calendarState.get(Calendar.MONTH)
        val day = calendarState.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(context, { _, selectedYear, selectedMonth, selectedDay ->
            val newCal = Calendar.getInstance()
            newCal.set(Calendar.YEAR, selectedYear)
            newCal.set(Calendar.MONTH, selectedMonth)
            newCal.set(Calendar.DAY_OF_MONTH, selectedDay)
            calendarState = newCal
        }, year, month, day).show()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Teacher Daily Teaching Plans",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.Navy
            )
            Text(
                text = "Select a date to view entries and generate a structured PDF report.",
                fontSize = 12.sp,
                color = AppColors.TextSecondary
            )
        }

        // Date selector
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = {
                        val newCal = calendarState.clone() as Calendar
                        newCal.add(Calendar.DATE, -1)
                        calendarState = newCal
                    }) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Prev Day", tint = AppColors.Admin)
                    }

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showDatePicker() }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CalendarToday, contentDescription = "Calendar", tint = AppColors.Admin, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = dateString,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.Navy
                        )
                    }

                    IconButton(onClick = {
                        val newCal = calendarState.clone() as Calendar
                        newCal.add(Calendar.DATE, 1)
                        calendarState = newCal
                    }) {
                        Icon(Icons.Default.ArrowForwardIos, contentDescription = "Next Day", tint = AppColors.Admin)
                    }
                }
            }
        }

        if (isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AppColors.Admin)
                }
            }
        } else {
            // Summary and generate PDF button
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Daily Summary",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = AppColors.Navy
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Total Teachers with Plans: " + plansDataList.size,
                            fontSize = 14.sp,
                            color = AppColors.TextSecondary
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (plansDataList.isEmpty()) {
                                    Toast.makeText(context, "No teaching plans found for this date", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                scope.launch {
                                    isGeneratingPdf = true
                                    try {
                                        generatedPdfUrl = generateDailyTeachingPlanPdf(context, dateString, plansDataList)
                                        if (generatedPdfUrl != null) {
                                            Toast.makeText(context, "PDF generated successfully!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Failed to generate PDF Report", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Error: " + e.message, Toast.LENGTH_SHORT).show()
                                    } finally {
                                        isGeneratingPdf = false
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Admin),
                            shape = RoundedCornerShape(8.dp),
                            enabled = !isGeneratingPdf && plansDataList.isNotEmpty()
                        ) {
                            if (isGeneratingPdf) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                            } else {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Generate Teaching Plan PDF", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            if (generatedPdfUrl != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, "Success", tint = Color(0xFF2E7D32), modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Report Ready to View!", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    com.rajeducational.erp.ui.common.PdfOpener.openPdf(context, generatedPdfUrl ?: "", scope)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.OpenInNew, "Open", tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Open / Share PDF", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            if (plansDataList.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No plans filled for this date yet.",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                plansDataList.forEachIndexed { index, plan ->
                    item {
                        val tName = plan["teacherName"] as? String ?: "Teacher"
                        val tCollege = plan["collegeName"] as? String ?: "Raj Nursing Institute"
                        val plans = plan["plans"] as? List<Map<String, Any>> ?: emptyList()

                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(AppColors.Admin.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = (index + 1).toString(),
                                            fontWeight = FontWeight.Bold,
                                            color = AppColors.Admin
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(tName, fontWeight = FontWeight.Bold, color = AppColors.Navy, fontSize = 15.sp)
                                        Text(tCollege, fontSize = 12.sp, color = AppColors.TextSecondary)
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                Divider(color = Color.LightGray.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(8.dp))

                                plans.forEachIndexed { planIndex, item ->
                                    val time = item["time"] as? String ?: ""
                                    val cls = item["className"] as? String ?: item["class"] as? String ?: ""
                                    val topic = item["topicName"] as? String ?: item["topic"] as? String ?: ""

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "" + (planIndex + 1) + ".",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = AppColors.TextSecondary,
                                            modifier = Modifier.width(20.dp)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Time: " + time, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                            Text("Class: " + cls, fontSize = 13.sp)
                                            Text("Topic: " + topic, fontSize = 13.sp, color = AppColors.Navy, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                    if (planIndex < plans.size - 1) {
                                        Spacer(modifier = Modifier.height(4.dp))
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

suspend fun generateDailyTeachingPlanPdf(
    context: Context,
    dateString: String,
    plansList: List<Map<String, Any>>
): String? {
    val pdfDocument = android.graphics.pdf.PdfDocument()
    val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
    var page = pdfDocument.startPage(pageInfo)
    var canvas = page.canvas
    val paint = android.graphics.Paint()

    paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    
    // Header
    paint.textSize = 18f
    paint.color = android.graphics.Color.BLACK
    canvas.drawText("Raj Educational Group", 50f, 50f, paint)

    paint.textSize = 14f
    canvas.drawText("Daily Teaching Plan - 2026", 50f, 75f, paint)

    paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
    paint.textSize = 12f
    canvas.drawText("Date: " + dateString, 50f, 100f, paint)
    
    canvas.drawLine(50f, 120f, 545f, 120f, paint)

    var yPosition = 145f

    plansList.forEachIndexed { teacherIdx, planDoc ->
        val teacherName = planDoc["teacherName"] as? String ?: "Teacher"
        val plans = planDoc["plans"] as? List<Map<String, Any>> ?: emptyList()

        if (plans.isNotEmpty()) {
            if (teacherIdx > 0 && yPosition > 145f) {
                yPosition += 15f
            }

            if (yPosition + 55f > 800f) {
                pdfDocument.finishPage(page)
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                yPosition = 50f
            }

            // Draw Table Headers for this teacher
            paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            paint.textSize = 11f
            canvas.drawText("S.NO", 50f, yPosition, paint)
            canvas.drawText("TEACHER NAME", 90f, yPosition, paint)
            canvas.drawText("TIME", 210f, yPosition, paint)
            canvas.drawText("CLASS", 320f, yPosition, paint)
            canvas.drawText("TOPIC NAME", 430f, yPosition, paint)
            
            yPosition += 8f
            canvas.drawLine(50f, yPosition, 545f, yPosition, paint)
            yPosition += 18f

            paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)

            var serialNumber = 1

            plans.forEach { planItem ->
                if (yPosition > 800f) {
                    pdfDocument.finishPage(page)
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    yPosition = 50f

                    // Draw Table Headers on new page
                    paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                    paint.textSize = 11f
                    canvas.drawText("S.NO", 50f, yPosition, paint)
                    canvas.drawText("TEACHER NAME", 90f, yPosition, paint)
                    canvas.drawText("TIME", 210f, yPosition, paint)
                    canvas.drawText("CLASS", 320f, yPosition, paint)
                    canvas.drawText("TOPIC NAME", 430f, yPosition, paint)
                    
                    yPosition += 8f
                    canvas.drawLine(50f, yPosition, 545f, yPosition, paint)
                    yPosition += 18f
                    paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
                }

                val time = planItem["time"] as? String ?: ""
                val cls = planItem["className"] as? String ?: planItem["class"] as? String ?: ""
                val topic = planItem["topicName"] as? String ?: planItem["topic"] as? String ?: ""

                fun truncate(text: String, maxLength: Int): String {
                    return if (text.length > maxLength) text.substring(0, maxLength - 2) + ".." else text
                }

                canvas.drawText(serialNumber.toString(), 50f, yPosition, paint)
                canvas.drawText(truncate(teacherName, 20), 90f, yPosition, paint)
                canvas.drawText(truncate(time, 18), 210f, yPosition, paint)
                canvas.drawText(truncate(cls, 18), 320f, yPosition, paint)
                canvas.drawText(truncate(topic, 22), 430f, yPosition, paint)

                serialNumber++
                yPosition += 22f
            }
        }
    }

    pdfDocument.finishPage(page)

    val fileName = "daily_teaching_plan_" + System.currentTimeMillis() + ".pdf"
    val file = java.io.File(context.cacheDir, fileName)
    var localUri: android.net.Uri? = null

    return try {
        pdfDocument.writeTo(java.io.FileOutputStream(file))
        pdfDocument.close()

        localUri = androidx.core.content.FileProvider.getUriForFile(context, context.packageName + ".provider", file)

        try {
            val storageRef = com.google.firebase.storage.FirebaseStorage.getInstance().reference.child("reports/" + fileName)
            storageRef.putFile(android.net.Uri.fromFile(file)).await()

            val url = storageRef.downloadUrl.await().toString()
            val reportData = hashMapOf(
                "title" to "Daily Teaching Plan Report - " + dateString,
                "teacherName" to "Admin",
                "url" to url,
                "timestamp" to System.currentTimeMillis()
            )
            com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("reports").add(reportData).await()
        } catch (storageEx: Exception) {
            storageEx.printStackTrace()
        }

        localUri?.toString()
    } catch (e: Exception) {
        e.printStackTrace()
        localUri?.toString()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDailyTeachingPlanControlScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daily Teaching Plan Control", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.Admin,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(AppColors.Background)
        ) {
            DailyTeachingPlanAdminTab(navController = navController)
        }
    }
}
