package com.rajeducational.erp.ui.teacher

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.rajeducational.erp.theme.AppColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

enum class ReportStep {
    SELECT_STUDENTS,
    SELECT_DATE_TYPE,
    GENERATE_REPORT
}

data class ReportStudentData(val id: String, val name: String, val batch: String, val year: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherAttendanceReportIndividualScreen(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var step by remember { mutableStateOf(ReportStep.SELECT_STUDENTS) }
    var searchQuery by remember { mutableStateOf("") }
    
    var students by remember { mutableStateOf<List<ReportStudentData>>(emptyList()) }
    var selectedStudentIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isLoadingStudents by remember { mutableStateOf(true) }
    
    var dateType by remember { mutableStateOf<String?>(null) } // "SINGLE" or "RANGE"
    var singleDateStr by remember { mutableStateOf("") }
    var startDateStr by remember { mutableStateOf("") }
    var endDateStr by remember { mutableStateOf("") }
    
    var isGenerating by remember { mutableStateOf(false) }
    var generatedPdfUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            val snapshot = FirebaseFirestore.getInstance().collection("students").get().await()
            val list = snapshot.documents.mapNotNull { doc ->
                val name = doc.getString("fullName") ?: return@mapNotNull null
                val batch = doc.getString("batch") ?: ""
                val year = doc.getString("year") ?: ""
                ReportStudentData(doc.id, name, batch, year)
            }
            students = list
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoadingStudents = false
        }
    }

    val filteredStudents = students.filter { it.name.contains(searchQuery, ignoreCase = true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Individual Report", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (step != ReportStep.SELECT_STUDENTS) {
                            step = ReportStep.SELECT_STUDENTS
                            generatedPdfUrl = null
                        } else {
                            navController.popBackStack()
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Teacher)
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
            if (step == ReportStep.SELECT_STUDENTS) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search Student") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Search, "Search") }
                )

                if (isLoadingStudents) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally), color = AppColors.Teacher)
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(filteredStudents) { student ->
                            val isSelected = selectedStudentIds.contains(student.id)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedStudentIds = if (isSelected) {
                                            selectedStudentIds - student.id
                                        } else {
                                            selectedStudentIds + student.id
                                        }
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = null,
                                    colors = CheckboxDefaults.colors(checkedColor = AppColors.Teacher)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(student.name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                                    if (student.batch.isNotBlank() || student.year.isNotBlank()) {
                                        Text("${student.year} | ${student.batch}", fontSize = 12.sp, color = AppColors.TextSecondary)
                                    }
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            if (selectedStudentIds.isNotEmpty()) {
                                step = ReportStep.SELECT_DATE_TYPE
                            } else {
                                Toast.makeText(context, "Select at least one student", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.Teacher)
                    ) {
                        Text("Done", color = Color.White, fontSize = 16.sp)
                    }
                }
            } else if (step == ReportStep.SELECT_DATE_TYPE) {
                Text("Select Report Type", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppColors.Navy)
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = dateType == "SINGLE",
                        onClick = { dateType = "SINGLE" },
                        colors = RadioButtonDefaults.colors(selectedColor = AppColors.Teacher)
                    )
                    Text("Single Day Report", modifier = Modifier.clickable { dateType = "SINGLE" })
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = dateType == "RANGE",
                        onClick = { dateType = "RANGE" },
                        colors = RadioButtonDefaults.colors(selectedColor = AppColors.Teacher)
                    )
                    Text("Report Between Dates", modifier = Modifier.clickable { dateType = "RANGE" })
                }

                if (dateType == "SINGLE") {
                    var showSinglePicker by remember { mutableStateOf(false) }
                    OutlinedTextField(
                        value = singleDateStr,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Select Date") },
                        modifier = Modifier.fillMaxWidth().clickable { showSinglePicker = true },
                        trailingIcon = {
                            IconButton(onClick = { showSinglePicker = true }) {
                                Icon(Icons.Default.DateRange, "Calendar")
                            }
                        }
                    )
                    if (showSinglePicker) {
                        val cal = Calendar.getInstance()
                        android.app.DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                val selectedDate = Calendar.getInstance().apply { set(year, month, dayOfMonth) }
                                val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                singleDateStr = format.format(selectedDate.time)
                                showSinglePicker = false
                            },
                            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
                        ).apply {
                            setOnCancelListener { showSinglePicker = false }
                        }.show()
                    }
                } else if (dateType == "RANGE") {
                    var showStartPicker by remember { mutableStateOf(false) }
                    var showEndPicker by remember { mutableStateOf(false) }

                    OutlinedTextField(
                        value = startDateStr,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Start Date") },
                        modifier = Modifier.fillMaxWidth().clickable { showStartPicker = true },
                        trailingIcon = {
                            IconButton(onClick = { showStartPicker = true }) {
                                Icon(Icons.Default.DateRange, "Calendar")
                            }
                        }
                    )
                    
                    OutlinedTextField(
                        value = endDateStr,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("End Date") },
                        modifier = Modifier.fillMaxWidth().clickable { showEndPicker = true },
                        trailingIcon = {
                            IconButton(onClick = { showEndPicker = true }) {
                                Icon(Icons.Default.DateRange, "Calendar")
                            }
                        }
                    )

                    if (showStartPicker) {
                        val cal = Calendar.getInstance()
                        android.app.DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                val selectedDate = Calendar.getInstance().apply { set(year, month, dayOfMonth) }
                                startDateStr = format.format(selectedDate.time)
                                showStartPicker = false
                            },
                            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
                        ).apply {
                            setOnCancelListener { showStartPicker = false }
                        }.show()
                    }
                    if (showEndPicker) {
                        val cal = Calendar.getInstance()
                        android.app.DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                val selectedDate = Calendar.getInstance().apply { set(year, month, dayOfMonth) }
                                endDateStr = format.format(selectedDate.time)
                                showEndPicker = false
                            },
                            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
                        ).apply {
                            setOnCancelListener { showEndPicker = false }
                        }.show()
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = {
                        val finalStart = if (dateType == "SINGLE") singleDateStr else startDateStr
                        val finalEnd = if (dateType == "SINGLE") singleDateStr else endDateStr

                        if (finalStart.isEmpty() || finalEnd.isEmpty()) {
                            Toast.makeText(context, "Please select dates", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        
                        isGenerating = true
                        step = ReportStep.GENERATE_REPORT
                        
                        coroutineScope.launch {
                            val selectedNames = students.filter { it.id in selectedStudentIds }.map { it.name }
                            val pdfUrl = generateIndividualReport(context, selectedNames, finalStart, finalEnd)
                            generatedPdfUrl = pdfUrl
                            isGenerating = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    enabled = dateType != null && !isGenerating,
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Teacher),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generate & Save PDF", color = Color.White, fontSize = 16.sp)
                }
            } else if (step == ReportStep.GENERATE_REPORT) {
                if (isGenerating) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = AppColors.Teacher)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Generating PDF Report...", fontSize = 16.sp)
                        }
                    }
                } else {
                    if (generatedPdfUrl != null) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Success", tint = Color(0xFF4CAF50), modifier = Modifier.size(64.dp))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Report Generated Successfully", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppColors.Navy)
                                Spacer(modifier = Modifier.height(24.dp))
                                
                                Button(
                                    onClick = {
                                        com.rajeducational.erp.ui.common.PdfOpener.openPdf(context, generatedPdfUrl ?: "", coroutineScope)
                                    },
                                    modifier = Modifier.fillMaxWidth().height(50.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Teacher)
                                ) {
                                    Text("Open PDF", color = Color.White, fontSize = 16.sp)
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                OutlinedButton(
                                    onClick = { 
                                        step = ReportStep.SELECT_STUDENTS
                                        selectedStudentIds = emptySet()
                                        generatedPdfUrl = null
                                    },
                                    modifier = Modifier.fillMaxWidth().height(50.dp)
                                ) {
                                    Text("Generate Another Report", color = AppColors.Teacher)
                                }
                            }
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Close, contentDescription = "Error", tint = Color.Red, modifier = Modifier.size(64.dp))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Failed to Generate Report", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppColors.Navy)
                                Spacer(modifier = Modifier.height(24.dp))
                                OutlinedButton(
                                    onClick = { 
                                        step = ReportStep.SELECT_STUDENTS
                                        selectedStudentIds = emptySet()
                                        generatedPdfUrl = null
                                    },
                                    modifier = Modifier.fillMaxWidth().height(50.dp)
                                ) {
                                    Text("Try Again", color = AppColors.Teacher)
                                }
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
fun TeacherAttendanceReportClassScreen(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var step by remember { mutableStateOf(ReportStep.SELECT_DATE_TYPE) }
    
    var dateType by remember { mutableStateOf<String?>(null) } // "SINGLE" or "RANGE"
    var singleDateStr by remember { mutableStateOf("") }
    var startDateStr by remember { mutableStateOf("") }
    var endDateStr by remember { mutableStateOf("") }
    
    var isGenerating by remember { mutableStateOf(false) }
    var generatedPdfUrl by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Class Report", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (step == ReportStep.GENERATE_REPORT) {
                            step = ReportStep.SELECT_DATE_TYPE
                            generatedPdfUrl = null
                        } else {
                            navController.popBackStack()
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Teacher)
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
            if (step == ReportStep.SELECT_DATE_TYPE) {
                Text("Select Report Type", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppColors.Navy)
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = dateType == "SINGLE",
                        onClick = { dateType = "SINGLE" },
                        colors = RadioButtonDefaults.colors(selectedColor = AppColors.Teacher)
                    )
                    Text("Single Day Report", modifier = Modifier.clickable { dateType = "SINGLE" })
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = dateType == "RANGE",
                        onClick = { dateType = "RANGE" },
                        colors = RadioButtonDefaults.colors(selectedColor = AppColors.Teacher)
                    )
                    Text("Report Between Dates", modifier = Modifier.clickable { dateType = "RANGE" })
                }

                if (dateType == "SINGLE") {
                    var showSinglePicker by remember { mutableStateOf(false) }
                    OutlinedTextField(
                        value = singleDateStr,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Select Date") },
                        modifier = Modifier.fillMaxWidth().clickable { showSinglePicker = true },
                        trailingIcon = {
                            IconButton(onClick = { showSinglePicker = true }) {
                                Icon(Icons.Default.DateRange, "Calendar")
                            }
                        }
                    )
                    if (showSinglePicker) {
                        val cal = Calendar.getInstance()
                        android.app.DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                val selectedDate = Calendar.getInstance().apply { set(year, month, dayOfMonth) }
                                val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                singleDateStr = format.format(selectedDate.time)
                                showSinglePicker = false
                            },
                            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
                        ).apply {
                            setOnCancelListener { showSinglePicker = false }
                        }.show()
                    }
                } else if (dateType == "RANGE") {
                    var showStartPicker by remember { mutableStateOf(false) }
                    var showEndPicker by remember { mutableStateOf(false) }

                    OutlinedTextField(
                        value = startDateStr,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Start Date") },
                        modifier = Modifier.fillMaxWidth().clickable { showStartPicker = true },
                        trailingIcon = {
                            IconButton(onClick = { showStartPicker = true }) {
                                Icon(Icons.Default.DateRange, "Calendar")
                            }
                        }
                    )
                    
                    OutlinedTextField(
                        value = endDateStr,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("End Date") },
                        modifier = Modifier.fillMaxWidth().clickable { showEndPicker = true },
                        trailingIcon = {
                            IconButton(onClick = { showEndPicker = true }) {
                                Icon(Icons.Default.DateRange, "Calendar")
                            }
                        }
                    )

                    if (showStartPicker) {
                        val cal = Calendar.getInstance()
                        android.app.DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                val selectedDate = Calendar.getInstance().apply { set(year, month, dayOfMonth) }
                                startDateStr = format.format(selectedDate.time)
                                showStartPicker = false
                            },
                            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
                        ).apply {
                            setOnCancelListener { showStartPicker = false }
                        }.show()
                    }
                    if (showEndPicker) {
                        val cal = Calendar.getInstance()
                        android.app.DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                val selectedDate = Calendar.getInstance().apply { set(year, month, dayOfMonth) }
                                endDateStr = format.format(selectedDate.time)
                                showEndPicker = false
                            },
                            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
                        ).apply {
                            setOnCancelListener { showEndPicker = false }
                        }.show()
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = {
                        val finalStart = if (dateType == "SINGLE") singleDateStr else startDateStr
                        val finalEnd = if (dateType == "SINGLE") singleDateStr else endDateStr

                        if (finalStart.isEmpty() || finalEnd.isEmpty()) {
                            Toast.makeText(context, "Please select dates", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        
                        isGenerating = true
                        step = ReportStep.GENERATE_REPORT
                        
                        coroutineScope.launch {
                            val pdfUrl = generateClassReport(context, finalStart, finalEnd)
                            generatedPdfUrl = pdfUrl
                            isGenerating = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    enabled = dateType != null && !isGenerating,
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Teacher),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generate & Save PDF", color = Color.White, fontSize = 16.sp)
                }
            } else if (step == ReportStep.GENERATE_REPORT) {
                if (isGenerating) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = AppColors.Teacher)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Generating PDF Report...", fontSize = 16.sp)
                        }
                    }
                } else {
                    if (generatedPdfUrl != null) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Success", tint = Color(0xFF4CAF50), modifier = Modifier.size(64.dp))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Report Generated Successfully", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppColors.Navy)
                                Spacer(modifier = Modifier.height(24.dp))
                                
                                Button(
                                    onClick = {
                                        com.rajeducational.erp.ui.common.PdfOpener.openPdf(context, generatedPdfUrl ?: "", coroutineScope)
                                    },
                                    modifier = Modifier.fillMaxWidth().height(50.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Teacher)
                                ) {
                                    Text("Open PDF", color = Color.White, fontSize = 16.sp)
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                OutlinedButton(
                                    onClick = { 
                                        step = ReportStep.SELECT_DATE_TYPE
                                        generatedPdfUrl = null
                                    },
                                    modifier = Modifier.fillMaxWidth().height(50.dp)
                                ) {
                                    Text("Generate Another Report", color = AppColors.Teacher)
                                }
                            }
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Close, contentDescription = "Error", tint = Color.Red, modifier = Modifier.size(64.dp))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Failed to Generate Report", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppColors.Navy)
                                Spacer(modifier = Modifier.height(24.dp))
                                OutlinedButton(
                                    onClick = { 
                                        step = ReportStep.SELECT_DATE_TYPE
                                        generatedPdfUrl = null
                                    },
                                    modifier = Modifier.fillMaxWidth().height(50.dp)
                                ) {
                                    Text("Try Again", color = AppColors.Teacher)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

data class RawRecord(val timestamp: Long, val type: String, val status: String, val name: String)
data class PdfRecord(val studentName: String, var timeIn: String = "--", var timeOut: String = "--", var status: String = "Absent")

suspend fun generateIndividualReport(context: Context, studentNames: List<String>, startDateStr: String, endDateStr: String): String? {
    val prefs = context.getSharedPreferences("TeacherPrefs", Context.MODE_PRIVATE)
    val teacherName = prefs.getString("teacher_name", "Teacher") ?: "Teacher"
    val teacherCourse = prefs.getString("teacher_course", "") ?: ""
    val teacherCollege = prefs.getString("teacher_college", "") ?: ""
    
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
    val startTimestamp = startCal.timeInMillis

    val endCal = Calendar.getInstance()
    if (endDateStr.isNotEmpty()) {
        try { format.parse(endDateStr)?.let { endCal.time = it } } catch (e: Exception) {}
    }
    endCal.set(Calendar.HOUR_OF_DAY, 23)
    endCal.set(Calendar.MINUTE, 59)
    val endTimestamp = endCal.timeInMillis

    return try {
        val records = mutableListOf<RawRecord>()
        val chunks = studentNames.chunked(10)
        for (chunk in chunks) {
            val snapshot = FirebaseFirestore.getInstance().collection("attendance")
                .whereIn("studentName", chunk)
                .get().await()

            val chunkRecords = snapshot.documents.mapNotNull { doc ->
                val timestamp = doc.getLong("timestamp") ?: 0L
                val type = doc.getString("type") ?: ""
                val status = doc.getString("status") ?: ""
                val name = doc.getString("studentName") ?: ""
                if (timestamp in startTimestamp..endTimestamp) {
                    RawRecord(timestamp, type, status, name)
                } else null
            }
            records.addAll(chunkRecords)
        }
        
        val db = FirebaseFirestore.getInstance()
        val holidaySnapshot = db.collection("holidays").get().await()
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

        val studentProfiles = mutableMapOf<String, Map<String, String>>()
        val studentsSnap = db.collection("students")
            .whereEqualTo("college", teacherCollege)
            .get().await()
        studentsSnap.documents.forEach { doc ->
            val name = doc.getString("fullName") ?: ""
            if (name.isNotEmpty()) {
                studentProfiles[name] = mapOf(
                    "college" to (doc.getString("college") ?: ""),
                    "course" to (doc.getString("course") ?: ""),
                    "session" to (doc.getString("session") ?: "")
                )
            }
        }

        val recordsByDate = mutableMapOf<String, MutableList<PdfRecord>>()
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
            val dayRecords = mutableListOf<PdfRecord>()
            
            for (student in studentNames) {
                val sProfile = studentProfiles[student]
                val college = sProfile?.get("college") ?: teacherCollege
                val course = sProfile?.get("course") ?: teacherCourse
                val batch = sProfile?.get("session") ?: ""
                
                val isSunday = currentDayCal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
                val isHoliday = if (isSunday) false else com.rajeducational.erp.utils.HolidayHelper.isHolidayForStudent(
                    dateStr, college, course, batch, holidayList
                )
                
                if (isSunday) {
                    dayRecords.add(PdfRecord(student, "Sunday", "Sunday", "Sunday"))
                } else if (isHoliday) {
                    dayRecords.add(PdfRecord(student, "Holiday", "Holiday", "Holiday"))
                } else {
                    val studentRaw = records.filter { format.format(Date(it.timestamp)) == dateStr && it.name == student }
                    if (studentRaw.isEmpty()) {
                        val statusStr = if (currentDayCal.after(today)) "Upcoming" else "Absent"
                        dayRecords.add(PdfRecord(student, "--", "--", statusStr))
                    } else {
                        var tIn = "--"
                        var tOut = "--"
                        var st = "Present"
                        studentRaw.forEach { raw ->
                            if (raw.type.equals("IN", ignoreCase = true)) {
                                tIn = timeFormat.format(Date(raw.timestamp))
                                if (raw.status.contains("Late", ignoreCase = true)) st = "Late"
                            } else if (raw.type.equals("OUT", ignoreCase = true)) {
                                tOut = timeFormat.format(Date(raw.timestamp))
                            }
                        }
                        dayRecords.add(PdfRecord(student, tIn, tOut, st))
                    }
                }
            }
            recordsByDate[dateStr] = dayRecords
        }

        val studentNamesStr = if (studentNames.size == 1) studentNames.first() else "${studentNames.size} Students"
        createAndUploadPdf(context, "Individual Report: $studentNamesStr", teacherName, teacherCourse, teacherCollege, startDateStr, endDateStr, recordsByDate)
    } catch (e: Exception) {
        e.printStackTrace()
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Failed to generate report", Toast.LENGTH_SHORT).show()
        }
        null
    }
}

suspend fun generateClassReport(context: Context, startDateStr: String, endDateStr: String): String? {
    val prefs = context.getSharedPreferences("TeacherPrefs", Context.MODE_PRIVATE)
    val teacherName = prefs.getString("teacher_name", "Teacher") ?: "Teacher"
    val teacherCourse = prefs.getString("teacher_course", "") ?: ""
    val teacherCollege = prefs.getString("teacher_college", "") ?: ""
    val teacherId = prefs.getString("teacher_id", "") ?: ""
    
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
    val startTimestamp = startCal.timeInMillis

    val endCal = Calendar.getInstance()
    if (endDateStr.isNotEmpty()) {
        try { format.parse(endDateStr)?.let { endCal.time = it } } catch (e: Exception) {}
    }
    endCal.set(Calendar.HOUR_OF_DAY, 23)
    endCal.set(Calendar.MINUTE, 59)
    val endTimestamp = endCal.timeInMillis

    return try {
        val query = FirebaseFirestore.getInstance().collection("attendance")
            .whereEqualTo("teacherId", teacherId)
        
        val snapshot = query.get().await()
        val records = snapshot.documents.mapNotNull { doc ->
            val timestamp = doc.getLong("timestamp") ?: 0L
            val type = doc.getString("type") ?: ""
            val status = doc.getString("status") ?: ""
            val stuName = doc.getString("studentName") ?: "Unknown"
            if (timestamp in startTimestamp..endTimestamp) {
                RawRecord(timestamp, type, status, stuName)
            } else null
        }

        val teacherCourses = prefs.getStringSet("teacher_courses", setOf(prefs.getString("teacher_course", ""))) ?: setOf()
        val teacherYears = prefs.getStringSet("teacher_years", emptySet()) ?: emptySet()
        val studentsSnapshot = FirebaseFirestore.getInstance().collection("students")
            .whereEqualTo("college", teacherCollege)
            .get().await()
            
        val allStudentNames = studentsSnapshot.documents.filter {
            val c = it.getString("course") ?: ""
            val s = it.getString("session") ?: ""
            teacherCourses.contains(c) && teacherYears.contains(s)
        }.mapNotNull { it.getString("fullName") }.sorted()

        val db = FirebaseFirestore.getInstance()
        val holidaySnapshot = db.collection("holidays").get().await()
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

        val studentProfiles = mutableMapOf<String, Map<String, String>>()
        studentsSnapshot.documents.forEach { doc ->
            val name = doc.getString("fullName") ?: ""
            if (name.isNotEmpty()) {
                studentProfiles[name] = mapOf(
                    "college" to (doc.getString("college") ?: ""),
                    "course" to (doc.getString("course") ?: ""),
                    "session" to (doc.getString("session") ?: "")
                )
            }
        }

        val recordsByDate = mutableMapOf<String, MutableList<PdfRecord>>()
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
            val dayRecords = mutableListOf<PdfRecord>()
            
            for (student in allStudentNames) {
                val sProfile = studentProfiles[student]
                val college = sProfile?.get("college") ?: teacherCollege
                val course = sProfile?.get("course") ?: teacherCourse
                val batch = sProfile?.get("session") ?: ""
                
                val isSunday = currentDayCal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
                val isHoliday = if (isSunday) false else com.rajeducational.erp.utils.HolidayHelper.isHolidayForStudent(
                    dateStr, college, course, batch, holidayList
                )
                
                if (isSunday) {
                    dayRecords.add(PdfRecord(student, "Sunday", "Sunday", "Sunday"))
                } else if (isHoliday) {
                    dayRecords.add(PdfRecord(student, "Holiday", "Holiday", "Holiday"))
                } else {
                    val studentRaw = records.filter { format.format(Date(it.timestamp)) == dateStr && it.name == student }
                    if (studentRaw.isEmpty()) {
                        val statusStr = if (currentDayCal.after(today)) "Upcoming" else "Absent"
                        dayRecords.add(PdfRecord(student, "--", "--", statusStr))
                    } else {
                        var tIn = "--"
                        var tOut = "--"
                        var st = "Present"
                        studentRaw.forEach { raw ->
                            if (raw.type.equals("IN", ignoreCase = true)) {
                                tIn = timeFormat.format(Date(raw.timestamp))
                                if (raw.status.contains("Late", ignoreCase = true)) st = "Late"
                            } else if (raw.type.equals("OUT", ignoreCase = true)) {
                                tOut = timeFormat.format(Date(raw.timestamp))
                            }
                        }
                        dayRecords.add(PdfRecord(student, tIn, tOut, st))
                    }
                }
            }
            recordsByDate[dateStr] = dayRecords
        }

        createAndUploadPdf(context, "Class Report", teacherName, teacherCourse, teacherCollege, startDateStr, endDateStr, recordsByDate)
    } catch (e: Exception) {
        e.printStackTrace()
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Failed to generate report", Toast.LENGTH_SHORT).show()
        }
        null
    }
}

suspend fun createAndUploadPdf(
    context: Context,
    title: String,
    teacherName: String,
    teacherCourse: String,
    teacherCollege: String,
    startDateStr: String,
    endDateStr: String,
    recordsByDate: Map<String, List<PdfRecord>>
): String? {
    val pdfDocument = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
    var page = pdfDocument.startPage(pageInfo)
    var canvas = page.canvas
    val paint = Paint()

    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paint.textSize = 18f
    val generatedDate = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()).format(Date())
    canvas.drawText("Attendance Report", 50f, 50f, paint)

    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    paint.textSize = 12f
    canvas.drawText("Generated On: $generatedDate", 50f, 80f, paint)
    canvas.drawText("Title: $title", 50f, 100f, paint)
    canvas.drawText("Teacher: $teacherName", 50f, 120f, paint)
    canvas.drawText("Course: $teacherCourse", 50f, 140f, paint)
    canvas.drawText("College: $teacherCollege", 50f, 160f, paint)
    val filterText = "Date Filter: " + (if (startDateStr.isEmpty()) "Start" else startDateStr) + " to " + (if (endDateStr.isEmpty()) "End" else endDateStr)
    canvas.drawText(filterText, 50f, 180f, paint)

    canvas.drawLine(50f, 200f, 545f, 200f, paint)

    var yPosition = 220f

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
        paint.textSize = 12f
        canvas.drawText("Student Name", 50f, yPosition, paint)
        canvas.drawText("Time In", 250f, yPosition, paint)
        canvas.drawText("Time Out", 350f, yPosition, paint)
        canvas.drawText("Status", 450f, yPosition, paint)
        yPosition += 10f
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
                canvas.drawText("Student Name", 50f, yPosition, paint)
                canvas.drawText("Time In", 250f, yPosition, paint)
                canvas.drawText("Time Out", 350f, yPosition, paint)
                canvas.drawText("Status", 450f, yPosition, paint)
                yPosition += 10f
                canvas.drawLine(50f, yPosition, 545f, yPosition, paint)
                yPosition += 15f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }
            
            canvas.drawText(record.studentName, 50f, yPosition, paint)
            canvas.drawText(record.timeIn, 250f, yPosition, paint)
            canvas.drawText(record.timeOut, 350f, yPosition, paint)
            
            if (record.status.equals("Late", ignoreCase = true)) {
                paint.color = android.graphics.Color.RED
            } else if (record.status.equals("Absent", ignoreCase = true)) {
                paint.color = android.graphics.Color.GRAY
            }
            canvas.drawText(record.status, 450f, yPosition, paint)
            paint.color = android.graphics.Color.BLACK
            
            yPosition += 20f
        }
        yPosition += 10f
    }

    pdfDocument.finishPage(page)

    val fileName = "report_${System.currentTimeMillis()}.pdf"
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
            "teacherName" to teacherName,
            "url" to url,
            "timestamp" to System.currentTimeMillis()
        )
        com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("reports").add(reportData).await()

        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Report generated and saved successfully!", Toast.LENGTH_LONG).show()
        }
        url
    } catch (e: Exception) {
        e.printStackTrace()
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "PDF generated locally (Upload to server failed).", Toast.LENGTH_LONG).show()
        }
        localUri?.toString()
    }
}
