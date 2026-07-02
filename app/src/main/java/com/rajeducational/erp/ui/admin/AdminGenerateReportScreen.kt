package com.rajeducational.erp.ui.admin

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
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
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.rajeducational.erp.data.College
import com.rajeducational.erp.data.Course
import com.rajeducational.erp.theme.AppColors
import com.rajeducational.erp.utils.HolidayHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminGenerateReportScreen(navController: NavController) {
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()
    val coroutineScope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Student Report, 1: Teacher Report
    var colleges by remember { mutableStateOf<List<College>>(emptyList()) }
    var isLoadingColleges by remember { mutableStateOf(true) }

    // Selected state
    val selectedColleges = remember { mutableStateListOf<College>() }
    val selectedCourses = remember { mutableStateListOf<Course>() }
    val selectedBatches = remember { mutableStateListOf<String>() }

    // Date Range state
    var startDateStr by remember { mutableStateOf("") }
    var endDateStr by remember { mutableStateOf("") }

    // Report Generation States
    var isGenerating by remember { mutableStateOf(false) }
    var generatedFileUri by remember { mutableStateOf<Uri?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    // Load colleges
    LaunchedEffect(Unit) {
        try {
            firestore.collection("colleges").addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    colleges = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(College::class.java)?.copy(id = doc.id)
                    }
                }
                isLoadingColleges = false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            isLoadingColleges = false
        }
    }

    // Helper: Select all / Deselect all
    fun toggleSelectAllColleges() {
        if (selectedColleges.size == colleges.size) {
            selectedColleges.clear()
            selectedCourses.clear()
            selectedBatches.clear()
        } else {
            selectedColleges.clear()
            selectedColleges.addAll(colleges)
        }
    }

    fun toggleSelectAllCourses(availableCourses: List<Course>) {
        val allSelected = availableCourses.all { selectedCourses.contains(it) }
        if (allSelected) {
            selectedCourses.removeAll(availableCourses)
            // also remove their batches
            val batchesToRemove = availableCourses.flatMap { it.yearBatches }.distinct()
            selectedBatches.removeAll(batchesToRemove)
        } else {
            availableCourses.forEach {
                if (!selectedCourses.contains(it)) {
                    selectedCourses.add(it)
                }
                it.yearBatches.forEach { b ->
                    if (!selectedBatches.contains(b)) {
                        selectedBatches.add(b)
                    }
                }
            }
        }
    }

    fun toggleSelectAllBatches(availableBatches: List<String>) {
        val allSelected = availableBatches.all { selectedBatches.contains(it) }
        if (allSelected) {
            selectedBatches.removeAll(availableBatches)
        } else {
            availableBatches.forEach {
                if (!selectedBatches.contains(it)) {
                    selectedBatches.add(it)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Generate Attendance Reports", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Admin, titleContentColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(AppColors.Background)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = AppColors.Admin
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Student Report", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Teacher Report", fontWeight = FontWeight.Bold) }
                )
            }

            if (isLoadingColleges) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AppColors.Admin)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    // Date picker card
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Select Date Range", fontWeight = FontWeight.Bold, color = AppColors.Navy, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                var showStartPicker by remember { mutableStateOf(false) }
                                var showEndPicker by remember { mutableStateOf(false) }

                                OutlinedTextField(
                                    value = startDateStr,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Start Date") },
                                    modifier = Modifier.weight(1f).clickable { showStartPicker = true },
                                    enabled = false,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        disabledTextColor = AppColors.TextPrimary,
                                        disabledBorderColor = Color.LightGray,
                                        disabledLabelColor = AppColors.TextSecondary
                                    ),
                                    trailingIcon = {
                                        IconButton(onClick = { showStartPicker = true }) {
                                            Icon(Icons.Default.CalendarMonth, "Start Date")
                                        }
                                    }
                                )

                                OutlinedTextField(
                                    value = endDateStr,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("End Date") },
                                    modifier = Modifier.weight(1f).clickable { showEndPicker = true },
                                    enabled = false,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        disabledTextColor = AppColors.TextPrimary,
                                        disabledBorderColor = Color.LightGray,
                                        disabledLabelColor = AppColors.TextSecondary
                                    ),
                                    trailingIcon = {
                                        IconButton(onClick = { showEndPicker = true }) {
                                            Icon(Icons.Default.CalendarMonth, "End Date")
                                        }
                                    }
                                )

                                if (showStartPicker) {
                                    val cal = Calendar.getInstance()
                                    DatePickerDialog(
                                        context,
                                        { _, y, m, d ->
                                            val c = Calendar.getInstance().apply { set(y, m, d) }
                                            startDateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(c.time)
                                            showStartPicker = false
                                        },
                                        cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
                                    ).apply {
                                        setOnCancelListener { showStartPicker = false }
                                    }.show()
                                }

                                if (showEndPicker) {
                                    val cal = Calendar.getInstance()
                                    DatePickerDialog(
                                        context,
                                        { _, y, m, d ->
                                            val c = Calendar.getInstance().apply { set(y, m, d) }
                                            endDateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(c.time)
                                            showEndPicker = false
                                        },
                                        cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
                                    ).apply {
                                        setOnCancelListener { showEndPicker = false }
                                    }.show()
                                }
                            }
                        }
                    }

                    // Colleges card
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Select Colleges", fontWeight = FontWeight.Bold, color = AppColors.Navy, fontSize = 16.sp, modifier = Modifier.weight(1f))
                                TextButton(onClick = { toggleSelectAllColleges() }) {
                                    Text(if (selectedColleges.size == colleges.size) "Deselect All" else "Select All", color = AppColors.Admin, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            colleges.forEach { college ->
                                val isChecked = selectedColleges.contains(college)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (isChecked) {
                                                selectedColleges.remove(college)
                                                // Also remove associated courses and batches
                                                selectedCourses.removeAll(college.courses)
                                                val remainingCourses = selectedColleges.flatMap { it.courses }
                                                val activeBatches = remainingCourses.flatMap { it.yearBatches }.distinct()
                                                selectedBatches.retainAll(activeBatches)
                                            } else {
                                                selectedColleges.add(college)
                                            }
                                        }
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isChecked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                        contentDescription = null,
                                        tint = if (isChecked) AppColors.Admin else Color.Gray,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(college.name, fontSize = 14.sp, color = AppColors.TextPrimary)
                                }
                            }
                        }
                    }

                    // Courses card
                    val availableCourses = remember(selectedColleges.size) {
                        selectedColleges.flatMap { it.courses }.distinctBy { it.name }
                    }
                    if (availableCourses.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Select Courses", fontWeight = FontWeight.Bold, color = AppColors.Navy, fontSize = 16.sp, modifier = Modifier.weight(1f))
                                    TextButton(onClick = { toggleSelectAllCourses(availableCourses) }) {
                                        Text(if (availableCourses.all { selectedCourses.contains(it) }) "Deselect All" else "Select All", color = AppColors.Admin, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))

                                availableCourses.forEach { course ->
                                    val isChecked = selectedCourses.contains(course)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                if (isChecked) {
                                                    selectedCourses.remove(course)
                                                    // Remove associated batches
                                                    selectedBatches.removeAll(course.yearBatches)
                                                } else {
                                                    selectedCourses.add(course)
                                                    // Auto select associated batches
                                                    course.yearBatches.forEach { b ->
                                                        if (!selectedBatches.contains(b)) selectedBatches.add(b)
                                                    }
                                                }
                                            }
                                            .padding(vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (isChecked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                            contentDescription = null,
                                            tint = if (isChecked) AppColors.Admin else Color.Gray,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(course.name, fontSize = 14.sp, color = AppColors.TextPrimary)
                                    }
                                }
                            }
                        }
                    }

                    // Batches card
                    val availableBatches = remember(selectedCourses.size) {
                        selectedCourses.flatMap { it.yearBatches }.distinct()
                    }
                    if (availableBatches.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Select Batches/Years", fontWeight = FontWeight.Bold, color = AppColors.Navy, fontSize = 16.sp, modifier = Modifier.weight(1f))
                                    TextButton(onClick = { toggleSelectAllBatches(availableBatches) }) {
                                        Text(if (availableBatches.all { selectedBatches.contains(it) }) "Deselect All" else "Select All", color = AppColors.Admin, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))

                                availableBatches.forEach { batch ->
                                    val isChecked = selectedBatches.contains(batch)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                if (isChecked) {
                                                    selectedBatches.remove(batch)
                                                } else {
                                                    selectedBatches.add(batch)
                                                }
                                            }
                                            .padding(vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (isChecked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                            contentDescription = null,
                                            tint = if (isChecked) AppColors.Admin else Color.Gray,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(batch, fontSize = 14.sp, color = AppColors.TextPrimary)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    if (isGenerating) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(color = AppColors.Admin)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("Generating and compiling PDF... Please wait", color = AppColors.TextSecondary, fontSize = 14.sp)
                        }
                    } else {
                        Button(
                            onClick = {
                                if (startDateStr.isEmpty() || endDateStr.isEmpty()) {
                                    Toast.makeText(context, "Please select Start Date and End Date", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (selectedColleges.isEmpty()) {
                                    Toast.makeText(context, "Please select at least one college", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (selectedCourses.isEmpty()) {
                                    Toast.makeText(context, "Please select at least one course", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (selectedBatches.isEmpty()) {
                                    Toast.makeText(context, "Please select at least one batch/year", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                isGenerating = true
                                coroutineScope.launch {
                                    val uri = generateReportPDF(
                                        context = context,
                                        isStudent = selectedTab == 0,
                                        colleges = selectedColleges.toList(),
                                        courses = selectedCourses.toList(),
                                        batches = selectedBatches.toList(),
                                        startDateStr = startDateStr,
                                        endDateStr = endDateStr
                                    )
                                    isGenerating = false
                                    if (uri != null) {
                                        generatedFileUri = uri
                                        showSuccessDialog = true
                                    } else {
                                        Toast.makeText(context, "Failed to generate report PDF", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Admin)
                        ) {
                            Icon(Icons.Default.Assessment, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate Attendance Report", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }

    if (showSuccessDialog && generatedFileUri != null) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            title = {
                Text(
                    "Report Generated!",
                    fontWeight = FontWeight.Bold,
                    color = AppColors.Navy,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Your attendance report has been compiled and saved successfully.",
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp,
                        color = AppColors.TextSecondary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        generatedFileUri?.let { uri ->
                            com.rajeducational.erp.ui.common.PdfOpener.openPdf(context, uri.toString(), coroutineScope)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Admin)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open Report")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        try {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/pdf"
                                putExtra(Intent.EXTRA_STREAM, generatedFileUri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Attendance Report"))
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error sharing file: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share")
                }
            }
        )
    }
}

// Stateful Pdf Page Helper to eliminate page boundary calculations
class PdfPageHelper(val pdfDocument: PdfDocument, val pageInfo: PdfDocument.PageInfo) {
    var page = pdfDocument.startPage(pageInfo)
    var canvas = page.canvas
    var yPosition = 50f
    val paint = Paint()

    fun checkPageBreak(requiredSpace: Float = 25f) {
        if (yPosition + requiredSpace > 790f) {
            pdfDocument.finishPage(page)
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas
            yPosition = 50f
        }
    }

    fun drawText(text: String, x: Float, size: Float, isBold: Boolean = false, color: Int = android.graphics.Color.BLACK) {
        checkPageBreak(size + 6f)
        paint.typeface = Typeface.create(Typeface.DEFAULT, if (isBold) Typeface.BOLD else Typeface.NORMAL)
        paint.textSize = size
        paint.color = color
        canvas.drawText(text, x, yPosition, paint)
    }

    fun drawLine(x1: Float, x2: Float, strokeWidth: Float = 1f, color: Int = android.graphics.Color.LTGRAY) {
        checkPageBreak(12f)
        paint.color = color
        paint.strokeWidth = strokeWidth
        canvas.drawLine(x1, yPosition, x2, yPosition, paint)
    }

    fun advance(amount: Float) {
        yPosition += amount
    }

    fun finish() {
        pdfDocument.finishPage(page)
    }
}

data class AttendanceRaw(
    val timestamp: Long,
    val type: String,
    val status: String,
    val studentName: String,
    val studentId: String,
    val role: String
)

data class StudentModel(
    val id: String = "",
    val fullName: String = "",
    val college: String = "",
    val course: String = "",
    val session: String = ""
)

data class TeacherData(
    val id: String = "",
    val name: String = "",
    val collegeName: String = "",
    val courses: List<String> = emptyList(),
    val years: List<String> = emptyList(),
    val role: String = "Teacher"
)

// Background PDF compiler and generator task
suspend fun generateReportPDF(
    context: Context,
    isStudent: Boolean,
    colleges: List<College>,
    courses: List<Course>,
    batches: List<String>,
    startDateStr: String,
    endDateStr: String
): Uri? {
    return withContext(Dispatchers.IO) {
        val firestore = FirebaseFirestore.getInstance()
        val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // 1. Convert start/end dates to milliseconds range
        val startCal = Calendar.getInstance()
        try { format.parse(startDateStr)?.let { startCal.time = it } } catch (e: Exception) {}
        startCal.set(Calendar.HOUR_OF_DAY, 0)
        startCal.set(Calendar.MINUTE, 0)
        startCal.set(Calendar.SECOND, 0)
        startCal.set(Calendar.MILLISECOND, 0)
        val startTimestamp = startCal.timeInMillis

        val endCal = Calendar.getInstance()
        try { format.parse(endDateStr)?.let { endCal.time = it } } catch (e: Exception) {}
        endCal.set(Calendar.HOUR_OF_DAY, 23)
        endCal.set(Calendar.MINUTE, 59)
        endCal.set(Calendar.SECOND, 59)
        endCal.set(Calendar.MILLISECOND, 999)
        val endTimestamp = endCal.timeInMillis

        try {
            // 2. Fetch Holiday List
            val holidaySnapshot = firestore.collection("holidays").get().await()
            val holidayList = holidaySnapshot.documents.mapNotNull { doc ->
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

            // 3. Fetch entire attendance log for this date range in memory
            val attendanceSnapshot = firestore.collection("attendance")
                .whereGreaterThanOrEqualTo("timestamp", startTimestamp)
                .whereLessThanOrEqualTo("timestamp", endTimestamp)
                .get().await()

            val allLogs = attendanceSnapshot.documents.mapNotNull { doc ->
                val ts = doc.getLong("timestamp") ?: 0L
                val type = doc.getString("type") ?: ""
                val status = doc.getString("status") ?: ""
                val studentName = doc.getString("studentName") ?: doc.getString("name") ?: ""
                val studentId = doc.getString("studentId") ?: ""
                val role = doc.getString("role") ?: "Student"
                AttendanceRaw(ts, type, status, studentName, studentId, role)
            }

            // Initialize PDF setup
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size
            val helper = PdfPageHelper(pdfDocument, pageInfo)

            // Draw Cover Header
            val generatedDate = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()).format(Date())
            helper.drawText("RAJ EDUCATIONAL ERP", 50f, 18f, isBold = true, color = android.graphics.Color.DKGRAY)
            helper.advance(24f)
            val reportTitle = if (isStudent) "STUDENT ATTENDANCE CONSOLIDATED REPORT" else "TEACHER ATTENDANCE CONSOLIDATED REPORT"
            helper.drawText(reportTitle, 50f, 14f, isBold = true, color = android.graphics.Color.BLUE)
            helper.advance(18f)
            helper.drawText("Report Period: $startDateStr to $endDateStr", 50f, 11f, isBold = false, color = android.graphics.Color.BLACK)
            helper.advance(14f)
            helper.drawText("Generated On: $generatedDate", 50f, 10f, isBold = false, color = android.graphics.Color.GRAY)
            helper.advance(10f)
            helper.drawLine(50f, 545f, strokeWidth = 1.5f, color = android.graphics.Color.BLACK)
            helper.advance(25f)

            // Calculate days in the range
            val daysCount = (((endTimestamp - startTimestamp) / (1000 * 60 * 60 * 24)).toInt() + 1).coerceAtLeast(1)

            if (isStudent) {
                // Fetch Students
                val studentsSnap = firestore.collection("students").get().await()
                val allStudents = studentsSnap.documents.mapNotNull { doc ->
                    val id = doc.id
                    val fullName = doc.getString("fullName") ?: ""
                    val col = doc.getString("college") ?: ""
                    val course = doc.getString("course") ?: ""
                    val session = doc.getString("session") ?: ""
                    if (fullName.isNotEmpty()) {
                        StudentModel(id, fullName, col, course, session)
                    } else null
                }

                // Sequential rendering for each College, Course, Batch
                for (college in colleges) {
                    val matchingCourses = college.courses.filter { courses.contains(it) }
                    for (course in matchingCourses) {
                        val matchingBatches = course.yearBatches.filter { batches.contains(it) }
                        for (batch in matchingBatches) {
                            val activeStudents = allStudents.filter {
                                it.college == college.name && it.course == course.name && it.session == batch
                            }

                            if (activeStudents.isNotEmpty()) {
                                helper.checkPageBreak(120f)
                                helper.drawText("College: ${college.name}", 50f, 12f, isBold = true, color = android.graphics.Color.parseColor("#0D47A1"))
                                helper.advance(16f)
                                helper.drawText("Course: ${course.name} | Batch: $batch", 50f, 11f, isBold = true, color = android.graphics.Color.parseColor("#37474F"))
                                helper.advance(14f)
                                helper.drawLine(50f, 545f, strokeWidth = 0.8f, color = android.graphics.Color.GRAY)
                                helper.advance(16f)

                                // Group by date sequentially
                                for (dayIdx in 0 until daysCount) {
                                    val currentDayCal = Calendar.getInstance().apply {
                                        timeInMillis = startTimestamp
                                        add(Calendar.DAY_OF_YEAR, dayIdx)
                                    }
                                    val dateStr = format.format(currentDayCal.time)

                                    val isHoliday = HolidayHelper.isHolidayForStudent(
                                        dateStr, college.name, course.name, batch, holidayList
                                    )

                                    helper.checkPageBreak(50f)
                                    helper.drawText("Date: $dateStr" + (if (isHoliday) " (Holiday)" else ""), 50f, 10f, isBold = true, color = android.graphics.Color.parseColor("#E65100"))
                                    helper.advance(14f)

                                    if (isHoliday) {
                                        helper.drawText("School closed due to official holiday.", 65f, 10f, isBold = false, color = android.graphics.Color.GRAY)
                                        helper.advance(16f)
                                        continue
                                    }

                                    // Table Headers
                                    helper.drawText("Student Name", 50f, 9f, isBold = true)
                                    helper.drawText("In Time", 250f, 9f, isBold = true)
                                    helper.drawText("Out Time", 350f, 9f, isBold = true)
                                    helper.drawText("Status", 450f, 9f, isBold = true)
                                    helper.advance(10f)
                                    helper.drawLine(50f, 545f, strokeWidth = 0.5f, color = android.graphics.Color.LTGRAY)
                                    helper.advance(14f)

                                    for (student in activeStudents) {
                                        helper.checkPageBreak(22f)

                                        // Find attendance for this specific student on this date
                                        val dayLogs = allLogs.filter {
                                            it.studentId == student.id && format.format(Date(it.timestamp)) == dateStr
                                        }

                                        var timeIn = "--"
                                        var timeOut = "--"
                                        var status = if (currentDayCal.after(today)) "Upcoming" else "Absent"
                                        var statusColor = if (currentDayCal.after(today)) android.graphics.Color.GRAY else android.graphics.Color.RED

                                        if (dayLogs.isNotEmpty()) {
                                            status = "Present"
                                            statusColor = android.graphics.Color.parseColor("#2E7D32")
                                            dayLogs.forEach { log ->
                                                if (log.type.equals("IN", ignoreCase = true)) {
                                                    timeIn = timeFormat.format(Date(log.timestamp))
                                                    if (log.status.contains("Late", ignoreCase = true)) {
                                                        status = "Late"
                                                        statusColor = android.graphics.Color.parseColor("#FF8F00")
                                                    }
                                                } else if (log.type.equals("OUT", ignoreCase = true)) {
                                                    timeOut = timeFormat.format(Date(log.timestamp))
                                                }
                                            }
                                        }

                                        helper.drawText(student.fullName, 50f, 9f, isBold = false)
                                        helper.drawText(timeIn, 250f, 9f, isBold = false)
                                        helper.drawText(timeOut, 350f, 9f, isBold = false)
                                        helper.drawText(status, 450f, 9f, isBold = true, color = statusColor)
                                        helper.advance(14f)
                                    }
                                    helper.advance(8f)
                                }
                                helper.advance(18f)
                            }
                        }
                    }
                }
            } else {
                // Fetch Teachers
                val teachersSnap = firestore.collection("teachers").get().await()
                val allTeachers = teachersSnap.documents.mapNotNull { doc ->
                    val id = doc.id
                    val name = doc.getString("name") ?: ""
                    val col = doc.getString("collegeName") ?: ""
                    val cList = doc.get("courses") as? List<*>
                    val coursesStrings = cList?.mapNotNull { it?.toString() } ?: emptyList()
                    val yList = doc.get("years") as? List<*>
                    val yearsStrings = yList?.mapNotNull { it?.toString() } ?: emptyList()
                    if (name.isNotEmpty()) {
                        TeacherData(id, name, col, coursesStrings, yearsStrings)
                    } else null
                }

                // Sequential rendering for each College, Course, Batch (for teachers)
                for (college in colleges) {
                    val matchingCourses = college.courses.filter { courses.contains(it) }
                    for (course in matchingCourses) {
                        val matchingBatches = course.yearBatches.filter { batches.contains(it) }
                        for (batch in matchingBatches) {
                            val activeTeachers = allTeachers.filter {
                                it.collegeName == college.name && 
                                it.courses.contains(course.name) && 
                                it.years.contains(batch)
                            }

                            if (activeTeachers.isNotEmpty()) {
                                helper.checkPageBreak(120f)
                                helper.drawText("College: ${college.name}", 50f, 12f, isBold = true, color = android.graphics.Color.parseColor("#0D47A1"))
                                helper.advance(16f)
                                helper.drawText("Course: ${course.name} | Batch: $batch (Teachers)", 50f, 11f, isBold = true, color = android.graphics.Color.parseColor("#37474F"))
                                helper.advance(14f)
                                helper.drawLine(50f, 545f, strokeWidth = 0.8f, color = android.graphics.Color.GRAY)
                                helper.advance(16f)

                                for (dayIdx in 0 until daysCount) {
                                    val currentDayCal = Calendar.getInstance().apply {
                                        timeInMillis = startTimestamp
                                        add(Calendar.DAY_OF_YEAR, dayIdx)
                                    }
                                    val dateStr = format.format(currentDayCal.time)

                                    val isHoliday = HolidayHelper.isHolidayForStudent(
                                        dateStr, college.name, course.name, batch, holidayList
                                    )

                                    helper.checkPageBreak(50f)
                                    helper.drawText("Date: $dateStr" + (if (isHoliday) " (Holiday)" else ""), 50f, 10f, isBold = true, color = android.graphics.Color.parseColor("#E65100"))
                                    helper.advance(14f)

                                    if (isHoliday) {
                                        helper.drawText("School closed due to official holiday.", 65f, 10f, isBold = false, color = android.graphics.Color.GRAY)
                                        helper.advance(16f)
                                        continue
                                    }

                                    // Table Headers
                                    helper.drawText("Teacher Name", 50f, 9f, isBold = true)
                                    helper.drawText("In Time", 250f, 9f, isBold = true)
                                    helper.drawText("Out Time", 350f, 9f, isBold = true)
                                    helper.drawText("Status", 450f, 9f, isBold = true)
                                    helper.advance(10f)
                                    helper.drawLine(50f, 545f, strokeWidth = 0.5f, color = android.graphics.Color.LTGRAY)
                                    helper.advance(14f)

                                    for (teacher in activeTeachers) {
                                        helper.checkPageBreak(22f)

                                        // Find attendance for this specific teacher on this date
                                        val dayLogs = allLogs.filter {
                                            it.studentId == teacher.id && format.format(Date(it.timestamp)) == dateStr
                                        }

                                        var timeIn = "--"
                                        var timeOut = "--"
                                        var status = if (currentDayCal.after(today)) "Upcoming" else "Absent"
                                        var statusColor = if (currentDayCal.after(today)) android.graphics.Color.GRAY else android.graphics.Color.RED

                                        if (dayLogs.isNotEmpty()) {
                                            status = "Present"
                                            statusColor = android.graphics.Color.parseColor("#2E7D32")
                                            dayLogs.forEach { log ->
                                                if (log.type.equals("IN", ignoreCase = true)) {
                                                    timeIn = timeFormat.format(Date(log.timestamp))
                                                    if (log.status.contains("Late", ignoreCase = true)) {
                                                        status = "Late"
                                                        statusColor = android.graphics.Color.parseColor("#FF8F00")
                                                    }
                                                } else if (log.type.equals("OUT", ignoreCase = true)) {
                                                    timeOut = timeFormat.format(Date(log.timestamp))
                                                }
                                            }
                                        }

                                        helper.drawText(teacher.name, 50f, 9f, isBold = false)
                                        helper.drawText(timeIn, 250f, 9f, isBold = false)
                                        helper.drawText(timeOut, 350f, 9f, isBold = false)
                                        helper.drawText(status, 450f, 9f, isBold = true, color = statusColor)
                                        helper.advance(14f)
                                    }
                                    helper.advance(8f)
                                }
                                helper.advance(18f)
                            }
                        }
                    }
                }
            }

            helper.finish()

            // Save PDF to cache or external files dir
            val fileName = "Attendance_Report_${System.currentTimeMillis()}.pdf"
            val file = File(context.cacheDir, fileName)
            FileOutputStream(file).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()

            // Optional upload to firebase storage
            try {
                val storageRef = FirebaseStorage.getInstance().reference.child("reports/$fileName")
                storageRef.putFile(Uri.fromFile(file)).await()
                val firestoreUrl = storageRef.downloadUrl.await().toString()

                val reportMeta = hashMapOf(
                    "title" to (if (isStudent) "Student Consolidated Report" else "Teacher Consolidated Report"),
                    "url" to firestoreUrl,
                    "timestamp" to System.currentTimeMillis()
                )
                firestore.collection("reports").add(reportMeta).await()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Return local URI shareable with FileProvider
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
