package com.rajeducational.erp.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
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
import com.rajeducational.erp.theme.AppColors
import com.rajeducational.erp.data.College
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminFeeReminderControlScreen(navController: NavController) {
    val firestore = FirebaseFirestore.getInstance()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    
    var currentScreen by remember { mutableStateOf("menu") } // menu, specific, bulk
    var isProcessing by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        when(currentScreen) {
                            "specific" -> "Specific Student Reminder"
                            "bulk" -> "Bulk Fee Reminder"
                            else -> "Fees Control Panel"
                        }, 
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { 
                        if (currentScreen == "menu") {
                            navController.popBackStack()
                        } else {
                            currentScreen = "menu"
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.Admin,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(AppColors.Background)
        ) {
            when (currentScreen) {
                "menu" -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Button(
                            onClick = { currentScreen = "specific" },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).height(60.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Admin),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Specific Student Reminder", fontSize = 18.sp)
                        }
                        
                        Button(
                            onClick = { currentScreen = "bulk" },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).height(60.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Admin),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Bulk Fee Reminder", fontSize = 18.sp)
                        }
                    }
                }
                "specific" -> {
                    SpecificFeeReminderSection(firestore, isProcessing) { p -> isProcessing = p }
                }
                "bulk" -> {
                    BulkFeeReminderSection(firestore, isProcessing) { p -> isProcessing = p }
                }
            }
        }
    }
}

@Composable
fun ResetRemindersButton(
    firestore: FirebaseFirestore,
    isProcessing: Boolean,
    setProcessing: (Boolean) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    
    Button(
        onClick = {
            coroutineScope.launch {
                setProcessing(true)
                try {
                    val allReminders = firestore.collection("students")
                        .whereEqualTo("hasFeeReminder", true)
                        .get().await()
                    
                    val batch = firestore.batch()
                    allReminders.documents.forEach { doc ->
                        batch.update(doc.reference, mapOf(
                            "hasFeeReminder" to false,
                            "feeReminderText" to null,
                            "feeReminderExpiry" to null
                        ))
                    }
                    batch.commit().await()
                    android.widget.Toast.makeText(context, "All reminders reset!", android.widget.Toast.LENGTH_SHORT).show()
                } catch(e: Exception) {
                    android.widget.Toast.makeText(context, "Error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
                setProcessing(false)
            }
        },
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha=0.8f)),
        shape = RoundedCornerShape(8.dp),
        enabled = !isProcessing
    ) {
        Text("Reset All Fee Reminders", color = Color.White)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpecificFeeReminderSection(
    firestore: FirebaseFirestore,
    isProcessing: Boolean,
    setProcessing: (Boolean) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var students by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    val selectedStudentIds = remember { mutableStateListOf<String>() }
    var reminderDays by remember { mutableStateOf("7") }
    var reminderText by remember { mutableStateOf("Your fees are pending. Please pay immediately.") }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        firestore.collection("students").addSnapshotListener { snap, _ ->
            if (snap != null) {
                students = snap.documents.mapNotNull { it.data?.plus("id" to it.id) }
            }
        }
    }

    val filteredStudents = students.filter { 
        (it["fullName"] as? String)?.contains(searchQuery, ignoreCase = true) == true ||
        (it["college"] as? String)?.contains(searchQuery, ignoreCase = true) == true
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        ResetRemindersButton(firestore, isProcessing, setProcessing)

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search by name or college...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            shape = RoundedCornerShape(12.dp)
        )
        
        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = reminderDays,
                onValueChange = { reminderDays = it },
                label = { Text("Days to show") },
                modifier = Modifier.weight(1f).padding(end = 8.dp),
                shape = RoundedCornerShape(8.dp),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
            )
            OutlinedTextField(
                value = reminderText,
                onValueChange = { reminderText = it },
                label = { Text("Message") },
                modifier = Modifier.weight(2f),
                shape = RoundedCornerShape(8.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                val days = reminderDays.toLongOrNull() ?: 7
                val expiry = System.currentTimeMillis() + (days * 24 * 60 * 60 * 1000)
                coroutineScope.launch {
                    setProcessing(true)
                    try {
                        val batch = firestore.batch()
                        selectedStudentIds.forEach { id ->
                            batch.update(firestore.collection("students").document(id), mapOf(
                                "hasFeeReminder" to true,
                                "feeReminderText" to reminderText,
                                "feeReminderExpiry" to expiry
                            ))
                        }
                        batch.commit().await()
                        android.widget.Toast.makeText(context, "Reminders sent!", android.widget.Toast.LENGTH_SHORT).show()
                        selectedStudentIds.clear()
                    } catch(e: Exception) {
                        android.widget.Toast.makeText(context, "Error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                    }
                    setProcessing(false)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Admin),
            enabled = !isProcessing && selectedStudentIds.isNotEmpty() && reminderText.isNotBlank()
        ) {
            Text("Send Fee Reminder (${selectedStudentIds.size})")
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(filteredStudents) { student ->
                val id = student["id"] as? String ?: return@items
                val name = student["fullName"] as? String ?: "Unknown"
                val college = student["college"] as? String ?: ""
                val course = student["course"] as? String ?: ""
                val session = student["session"] as? String ?: ""

                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable {
                        if (selectedStudentIds.contains(id)) selectedStudentIds.remove(id)
                        else selectedStudentIds.add(id)
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AppColors.Navy)
                            Text("$college | $course | $session", fontSize = 12.sp, color = AppColors.TextSecondary)
                        }
                        Checkbox(
                            checked = selectedStudentIds.contains(id),
                            onCheckedChange = { checked ->
                                if (checked) selectedStudentIds.add(id)
                                else selectedStudentIds.remove(id)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BulkFeeReminderSection(
    firestore: FirebaseFirestore,
    isProcessing: Boolean,
    setProcessing: (Boolean) -> Unit
) {
    var students by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var collegesData by remember { mutableStateOf<List<College>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        firestore.collection("students").addSnapshotListener { snap, _ ->
            if (snap != null) {
                students = snap.documents.mapNotNull { it.data?.plus("id" to it.id) }
            }
        }
        firestore.collection("colleges").addSnapshotListener { snap, _ ->
            if (snap != null) {
                collegesData = snap.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(College::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        null
                    }
                }
            }
        }
    }

    val selectedColleges = remember { mutableStateListOf<String>() }
    val selectedCourses = remember { mutableStateListOf<String>() }
    val selectedSessions = remember { mutableStateListOf<String>() }

    val availableColleges = remember(collegesData) { 
        collegesData.map { it.name }.filter { it.isNotBlank() }.distinct().sorted() 
    }
    
    val availableCourses = remember(collegesData, selectedColleges.toList()) { 
        val filteredColleges = if (selectedColleges.isNotEmpty()) collegesData.filter { it.name in selectedColleges } else collegesData
        filteredColleges.flatMap { it.courses }.map { it.name }.filter { it.isNotBlank() }.distinct().sorted() 
    }
    
    val availableSessions = remember(collegesData, selectedColleges.toList(), selectedCourses.toList()) { 
        val filteredColleges = if (selectedColleges.isNotEmpty()) collegesData.filter { it.name in selectedColleges } else collegesData
        val allCourses = filteredColleges.flatMap { it.courses }
        val filteredCourses = if (selectedCourses.isNotEmpty()) allCourses.filter { it.name in selectedCourses } else allCourses
        filteredCourses.flatMap { it.yearBatches }.filter { it.isNotBlank() }.distinct().sorted() 
    }

    var reminderDays by remember { mutableStateOf("7") }
    var reminderText by remember { mutableStateOf("Your fees are pending. Please pay immediately.") }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        ResetRemindersButton(firestore, isProcessing, setProcessing)

        LazyColumn(modifier = Modifier.weight(1f)) {
            item {
                Text("Select Colleges", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                if (availableColleges.isEmpty()) Text("No colleges found", color = AppColors.TextSecondary, fontSize = 12.sp)
                availableColleges.forEach { college ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable {
                        if (selectedColleges.contains(college)) selectedColleges.remove(college) else selectedColleges.add(college)
                    }) {
                        Checkbox(checked = selectedColleges.contains(college), onCheckedChange = { 
                            if (it) selectedColleges.add(college) else selectedColleges.remove(college) 
                        })
                        Text(college)
                    }
                }
                Divider()
            }
            item {
                Text("Select Courses", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                if (availableCourses.isEmpty()) Text("No courses found", color = AppColors.TextSecondary, fontSize = 12.sp)
                availableCourses.forEach { course ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable {
                        if (selectedCourses.contains(course)) selectedCourses.remove(course) else selectedCourses.add(course)
                    }) {
                        Checkbox(checked = selectedCourses.contains(course), onCheckedChange = { 
                            if (it) selectedCourses.add(course) else selectedCourses.remove(course) 
                        })
                        Text(course)
                    }
                }
                Divider()
            }
            item {
                Text("Select Sessions", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                if (availableSessions.isEmpty()) Text("No sessions found", color = AppColors.TextSecondary, fontSize = 12.sp)
                availableSessions.forEach { session ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable {
                        if (selectedSessions.contains(session)) selectedSessions.remove(session) else selectedSessions.add(session)
                    }) {
                        Checkbox(checked = selectedSessions.contains(session), onCheckedChange = { 
                            if (it) selectedSessions.add(session) else selectedSessions.remove(session) 
                        })
                        Text(session)
                    }
                }
                Divider()
            }
            item {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = reminderDays,
                    onValueChange = { reminderDays = it },
                    label = { Text("Days to show") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = reminderText,
                    onValueChange = { reminderText = it },
                    label = { Text("Reminder Description") },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val days = reminderDays.toLongOrNull() ?: 7
                        val expiry = System.currentTimeMillis() + (days * 24 * 60 * 60 * 1000)
                        
                        coroutineScope.launch {
                            setProcessing(true)
                            try {
                                val targetStudents = students.filter { student ->
                                    val matchCollege = selectedColleges.isEmpty() || selectedColleges.contains(student["college"] as? String)
                                    val matchCourse = selectedCourses.isEmpty() || selectedCourses.contains(student["course"] as? String)
                                    val matchSession = selectedSessions.isEmpty() || selectedSessions.contains(student["session"] as? String)
                                    matchCollege && matchCourse && matchSession
                                }
                                
                                if (targetStudents.isEmpty()) {
                                    android.widget.Toast.makeText(context, "No students found matching criteria.", android.widget.Toast.LENGTH_SHORT).show()
                                } else {
                                    val batch = firestore.batch()
                                    targetStudents.forEach { doc ->
                                        val id = doc["id"] as? String
                                        if (id != null) {
                                            batch.update(firestore.collection("students").document(id), mapOf(
                                                "hasFeeReminder" to true,
                                                "feeReminderText" to reminderText,
                                                "feeReminderExpiry" to expiry
                                            ))
                                        }
                                    }
                                    batch.commit().await()
                                    android.widget.Toast.makeText(context, "Sent to ${targetStudents.size} students!", android.widget.Toast.LENGTH_SHORT).show()
                                    
                                    // Reset selections
                                    selectedColleges.clear()
                                    selectedCourses.clear()
                                    selectedSessions.clear()
                                }
                            } catch(e: Exception) {
                                android.widget.Toast.makeText(context, "Error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                            }
                            setProcessing(false)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Admin),
                    enabled = !isProcessing && reminderText.isNotBlank() && (selectedColleges.isNotEmpty() || selectedCourses.isNotEmpty() || selectedSessions.isNotEmpty())
                ) {
                    Text("Submit Bulk Reminder")
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

