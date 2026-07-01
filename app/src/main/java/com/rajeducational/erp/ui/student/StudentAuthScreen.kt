package com.rajeducational.erp.ui.student

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.rajeducational.erp.theme.AppColors
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun StudentAuthScreen(navController: NavController) {
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)
    var mode by remember { mutableStateOf("scan_teacher_qr") } // scan_teacher_qr, register, login
    
    // Auth variables
    var loginPhone by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }
    
    // Reg variables
    var studentName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    
    // Dynamic Dropdowns
    var colleges by remember { mutableStateOf<List<com.rajeducational.erp.data.College>>(emptyList()) }
    var selectedCollegeObj by remember { mutableStateOf<com.rajeducational.erp.data.College?>(null) }
    var selectedCourseObj by remember { mutableStateOf<com.rajeducational.erp.data.Course?>(null) }
    var selectedSession by remember { mutableStateOf("") }
    
    var expandedCollege by remember { mutableStateOf(false) }
    var expandedCourse by remember { mutableStateOf(false) }
    var expandedSession by remember { mutableStateOf(false) }
    
    var scannedTeacherId by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var isChecking by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()

    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("StudentPrefs", android.content.Context.MODE_PRIVATE)
        if (prefs.getBoolean("is_logged_in", false)) {
            navController.navigate("student_dashboard") {
                popUpTo("landing")
            }
        }
        
        // Fetch colleges
        try {
            val snapshot = firestore.collection("colleges").get().await()
            colleges = snapshot.documents.mapNotNull { doc ->
                doc.toObject(com.rajeducational.erp.data.College::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        if (mode == "scan_teacher_qr") "Student Scanner" 
                        else if (mode == "login") "Student Login" 
                        else "Student Details", 
                        fontWeight = FontWeight.Bold, 
                        color = Color.White
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { 
                        if (mode == "scan_teacher_qr") {
                            navController.popBackStack() 
                        } else {
                            mode = "scan_teacher_qr" 
                            error = ""
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (mode == "scan_teacher_qr") {
                        TextButton(onClick = { mode = "login"; error = "" }) {
                            Text("Login", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    } else if (mode == "login") {
                        TextButton(onClick = { mode = "scan_teacher_qr"; error = "" }) {
                            Text("Register", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.Student, 
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (mode) {
                "scan_teacher_qr" -> {
                    Spacer(modifier = Modifier.height(16.dp))
                    Icon(Icons.Default.QrCodeScanner, "Scanner", tint = AppColors.Student, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Authorize yourself by scanning the QR code.", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Navy, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("The QR code is available in the Teacher panel.", fontSize = 14.sp, color = AppColors.TextSecondary)
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    if (cameraPermissionState.status.isGranted) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(350.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.Black)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                if (!isChecking) {
                                    com.rajeducational.erp.ui.teacher.QRScannerPreview { code ->
                                        if (!isChecking) {
                                            try {
                                                val jsonObject = org.json.JSONObject(code)
                                                if (jsonObject.getString("type") == "teacher_student_reg") {
                                                    val tid = jsonObject.getString("teacherId")
                                                    val token = jsonObject.getString("token")
                                                    
                                                    isChecking = true
                                                    coroutineScope.launch {
                                                        try {
                                                            val doc = firestore.collection("settings").document("student_qr_$tid").get().await()
                                                            if (doc.exists()) {
                                                                val serverToken = doc.getString("token")
                                                                val timestamp = doc.getLong("timestamp") ?: 0L
                                                                val currentTime = System.currentTimeMillis()
                                                                
                                                                if (serverToken == token && (currentTime - timestamp <= 120 * 1000)) {
                                                                    scannedTeacherId = tid
                                                                    mode = "register"
                                                                    error = ""
                                                                } else {
                                                                    error = "QR Code Expired. Please ask the teacher to show the refreshed QR."
                                                                }
                                                            } else {
                                                                error = "Teacher not active or invalid QR session."
                                                            }
                                                        } catch (e: Exception) {
                                                            error = "Verification failed: ${e.message}"
                                                        } finally {
                                                            kotlinx.coroutines.delay(240000)
                                                            isChecking = false
                                                        }
                                                    }
                                                } else {
                                                    error = "Invalid QR Code for student registration"
                                                    isChecking = true
                                                    coroutineScope.launch {
                                                        kotlinx.coroutines.delay(240000)
                                                        isChecking = false
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                error = "Invalid QR Code format"
                                                isChecking = true
                                                coroutineScope.launch {
                                                    kotlinx.coroutines.delay(240000)
                                                    isChecking = false
                                                }
                                            }
                                        }
                                    }
                                }
                                if (isChecking) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.align(Alignment.Center),
                                        color = AppColors.Student
                                    )
                                }
                            }
                        }
                    } else {
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Camera permission is required to scan the QR code.", textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(onClick = { cameraPermissionState.launchPermissionRequest() }, colors = ButtonDefaults.buttonColors(containerColor = AppColors.Student)) {
                                    Text("Grant Permission")
                                }
                            }
                        }
                    }
                    
                    if (error.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(error, color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(
                        "Note: Please scan the QR code which you will get from the teacher. After scanning the QR code, you will be able to register.",
                        fontSize = 14.sp,
                        color = AppColors.TextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                }
                "register" -> {
                    Text("Complete Registration", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = AppColors.Navy)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(), 
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            OutlinedTextField(
                                value = studentName, 
                                onValueChange = { studentName = it }, 
                                label = { Text("Student Name *") }, 
                                modifier = Modifier.fillMaxWidth(), 
                                shape = RoundedCornerShape(10.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            OutlinedTextField(
                                value = phone, 
                                onValueChange = { phone = it }, 
                                label = { Text("Phone Number *") }, 
                                modifier = Modifier.fillMaxWidth(), 
                                shape = RoundedCornerShape(10.dp), 
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            OutlinedTextField(
                                value = email, 
                                onValueChange = { email = it }, 
                                label = { Text("Email ID (Optional)") }, 
                                modifier = Modifier.fillMaxWidth(), 
                                shape = RoundedCornerShape(10.dp), 
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Email)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            OutlinedTextField(
                                value = address, 
                                onValueChange = { address = it }, 
                                label = { Text("Address *") }, 
                                modifier = Modifier.fillMaxWidth(), 
                                shape = RoundedCornerShape(10.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text("Your ID for login will be your phone number.", fontSize = 12.sp, color = AppColors.Student, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            OutlinedTextField(
                                value = password, 
                                onValueChange = { password = it }, 
                                label = { Text("Password *") }, 
                                modifier = Modifier.fillMaxWidth(), 
                                visualTransformation = PasswordVisualTransformation(), 
                                shape = RoundedCornerShape(10.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            OutlinedTextField(
                                value = confirmPassword, 
                                onValueChange = { confirmPassword = it }, 
                                label = { Text("Confirm Password *") }, 
                                modifier = Modifier.fillMaxWidth(), 
                                visualTransformation = PasswordVisualTransformation(), 
                                shape = RoundedCornerShape(10.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Note: Remember this password for login. This is important for login.", fontSize = 12.sp, color = AppColors.TextSecondary)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(), 
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("College & Course Selection", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AppColors.Navy)
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // College Dropdown
                            ExposedDropdownMenuBox(
                                expanded = expandedCollege,
                                onExpandedChange = { expandedCollege = !expandedCollege },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = selectedCollegeObj?.name ?: "",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Select College *") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCollege) },
                                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = expandedCollege,
                                    onDismissRequest = { expandedCollege = false }
                                ) {
                                    colleges.forEach { college ->
                                        DropdownMenuItem(
                                            text = { Text(college.name) },
                                            onClick = {
                                                selectedCollegeObj = college
                                                selectedCourseObj = null
                                                selectedSession = ""
                                                expandedCollege = false
                                            }
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Course Dropdown
                            if (selectedCollegeObj != null) {
                                ExposedDropdownMenuBox(
                                    expanded = expandedCourse,
                                    onExpandedChange = { expandedCourse = !expandedCourse },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedTextField(
                                        value = selectedCourseObj?.name ?: "",
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Select Course *") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCourse) },
                                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                        modifier = Modifier
                                            .menuAnchor()
                                            .fillMaxWidth()
                                    )
                                    ExposedDropdownMenu(
                                        expanded = expandedCourse,
                                        onDismissRequest = { expandedCourse = false }
                                    ) {
                                        selectedCollegeObj!!.courses.forEach { course ->
                                            DropdownMenuItem(
                                                text = { Text(course.name) },
                                                onClick = {
                                                    selectedCourseObj = course
                                                    selectedSession = ""
                                                    expandedCourse = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Session Year Dropdown
                            if (selectedCourseObj != null) {
                                ExposedDropdownMenuBox(
                                    expanded = expandedSession,
                                    onExpandedChange = { expandedSession = !expandedSession },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedTextField(
                                        value = selectedSession,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Select Course Year (Session) *") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSession) },
                                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                        modifier = Modifier
                                            .menuAnchor()
                                            .fillMaxWidth()
                                    )
                                    ExposedDropdownMenu(
                                        expanded = expandedSession,
                                        onDismissRequest = { expandedSession = false }
                                    ) {
                                        selectedCourseObj!!.yearBatches.forEach { yearBatch ->
                                            DropdownMenuItem(
                                                text = { Text(yearBatch) },
                                                onClick = {
                                                    selectedSession = yearBatch
                                                    expandedSession = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    if (error.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(error, color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { 
                            if (studentName.isBlank() || phone.isBlank() || address.isBlank() || password.isBlank() || confirmPassword.isBlank() || selectedCollegeObj == null || selectedCourseObj == null || selectedSession.isBlank()) {
                                error = "Please fill all mandatory fields (*)"
                            } else if (password != confirmPassword) {
                                error = "Passwords do not match"
                            } else {
                                isSaving = true
                                val trimmedPhone = phone.trim()
                                val trimmedPassword = password.trim()
                                coroutineScope.launch {
                                    try {
                                        // Check if already registered
                                        val existing = firestore.collection("students")
                                            .whereEqualTo("phone", trimmedPhone)
                                            .get()
                                            .await()
                                        
                                        if (!existing.isEmpty) {
                                            error = "Phone number is already registered. Please Login."
                                            isSaving = false
                                            return@launch
                                        }
                                        
                                        // Generate a unique App ID for the student
                                        val newAppId = "STU" + System.currentTimeMillis().toString().takeLast(6)
                                        
                                        val studentData = hashMapOf(
                                            "id" to newAppId,
                                            "fullName" to studentName.trim(),
                                            "phone" to trimmedPhone,
                                            "email" to email.trim(),
                                            "address" to address.trim(),
                                            "password" to trimmedPassword,
                                            "college" to selectedCollegeObj!!.name,
                                            "course" to selectedCourseObj!!.name,
                                            "session" to selectedSession
                                        )
                                        if (scannedTeacherId.isNotEmpty()) {
                                            studentData["teacherId"] = scannedTeacherId
                                        }
                                        
                                        firestore.collection("students").document(newAppId).set(studentData).await()
                                        
                                        val sharedPrefs = context.getSharedPreferences("StudentPrefs", android.content.Context.MODE_PRIVATE)
                                        sharedPrefs.edit().apply {
                                            putString("student_id", newAppId)
                                            putString("student_name", studentName.trim())
                                            putString("student_phone", trimmedPhone)
                                            putString("student_email", email.trim())
                                            putString("student_address", address.trim())
                                            putString("student_college", selectedCollegeObj!!.name)
                                            putString("student_course", selectedCourseObj!!.name)
                                            putString("student_session", selectedSession)
                                            putBoolean("is_logged_in", true)
                                            apply()
                                        }
                                        
                                        android.widget.Toast.makeText(context, "Registration Successful", android.widget.Toast.LENGTH_SHORT).show()
                                        navController.navigate("student_dashboard") {
                                            popUpTo("landing")
                                        }
                                    } catch (e: Exception) {
                                        error = "Registration failed: ${e.message}"
                                    } finally {
                                        isSaving = false
                                    }
                                }
                            }
                        }, 
                        modifier = Modifier.fillMaxWidth(), 
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.Student), 
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isSaving
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Text("Register", modifier = Modifier.padding(8.dp), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
                "login" -> {
                    Text("Student Login", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = AppColors.Navy)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(), 
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            OutlinedTextField(
                                value = loginPhone, 
                                onValueChange = { loginPhone = it }, 
                                label = { Text("Phone Number") }, 
                                modifier = Modifier.fillMaxWidth(), 
                                shape = RoundedCornerShape(10.dp),
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            OutlinedTextField(
                                value = loginPassword, 
                                onValueChange = { loginPassword = it }, 
                                label = { Text("Password") }, 
                                modifier = Modifier.fillMaxWidth(), 
                                visualTransformation = PasswordVisualTransformation(), 
                                shape = RoundedCornerShape(10.dp)
                            )
                            
                            if (error.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(error, color = Color.Red, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { 
                                    if (loginPhone.isBlank() || loginPassword.isBlank()) {
                                        error = "Please enter Phone and Password"
                                        return@Button
                                    }
                                    isSaving = true
                                    error = ""
                                    val trimmedPhone = loginPhone.trim()
                                    val trimmedPassword = loginPassword.trim()
                                    coroutineScope.launch {
                                        try {
                                            val snapshot = firestore.collection("students")
                                                .whereEqualTo("phone", trimmedPhone)
                                                .whereEqualTo("password", trimmedPassword)
                                                .get()
                                                .await()
                                            
                                            if (!snapshot.isEmpty) {
                                                val doc = snapshot.documents[0]
                                                val prefs = context.getSharedPreferences("StudentPrefs", android.content.Context.MODE_PRIVATE)
                                                prefs.edit().apply {
                                                    putString("student_id", doc.id)
                                                    putString("student_name", doc.getString("fullName"))
                                                    putString("student_phone", doc.getString("phone"))
                                                    putString("student_email", doc.getString("email"))
                                                    putString("student_address", doc.getString("address"))
                                                    putString("student_college", doc.getString("college"))
                                                    putString("student_course", doc.getString("course"))
                                                    putString("student_session", doc.getString("session"))
                                                    putBoolean("is_logged_in", true)
                                                    apply()
                                                }
                                                android.widget.Toast.makeText(context, "Login Successful", android.widget.Toast.LENGTH_SHORT).show()
                                                navController.navigate("student_dashboard") {
                                                    popUpTo("landing")
                                                }
                                            } else {
                                                error = "Invalid Phone Number or Password"
                                            }
                                        } catch (e: Exception) {
                                            error = "Login failed: ${e.message}"
                                        } finally {
                                            isSaving = false
                                        }
                                    }
                                }, 
                                modifier = Modifier.fillMaxWidth(), 
                                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Student), 
                                shape = RoundedCornerShape(12.dp),
                                enabled = !isSaving
                            ) { 
                                if (isSaving) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                } else {
                                    Text("Login", modifier = Modifier.padding(8.dp), fontWeight = FontWeight.Bold, fontSize = 16.sp) 
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

