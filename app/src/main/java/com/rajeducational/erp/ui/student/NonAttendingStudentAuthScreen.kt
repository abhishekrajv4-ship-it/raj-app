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
fun NonAttendingStudentAuthScreen(navController: NavController) {
    var mode by remember { mutableStateOf("register") } // register, login
    
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
                        if (mode == "login") "Student Login" else "Student Details", 
                        fontWeight = FontWeight.Bold, 
                        color = Color.White
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { 
                        if (mode == "register") {
                            navController.popBackStack() 
                        } else {
                            mode = "register" 
                            error = ""
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (mode == "register") {
                        TextButton(onClick = { mode = "login"; error = "" }) {
                            Text("Login", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    } else if (mode == "login") {
                        TextButton(onClick = { mode = "register"; error = "" }) {
                            Text("Register", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Red, 
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
                                            "session" to selectedSession,
                                            "isAttending" to false,
                                            "approvalStatus" to "pending"
                                        )
                                        
                                        firestore.collection("students").document(newAppId).set(studentData).await()
                                        
                                        // Do NOT set logged in here, must wait for approval!
                                        val sharedPrefs = context.getSharedPreferences("StudentPrefs", android.content.Context.MODE_PRIVATE)
                                        sharedPrefs.edit().apply {
                                            putString("pending_student_id", newAppId)
                                            apply()
                                        }
                                        
                                        android.widget.Toast.makeText(context, "Registration Submitted. Please complete face registration.", android.widget.Toast.LENGTH_SHORT).show()
                                        navController.navigate("face_registration/non_attending/$newAppId") {
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
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red), 
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
                                                val isAttending = doc.getBoolean("isAttending") ?: true
                                                val status = doc.getString("approvalStatus") ?: "approved"
                                                
                                                if (isAttending) {
                                                    error = "This login is only for Non-Attending Students."
                                                } else if (status == "pending") {
                                                    error = "Your account is pending approval from the teacher."
                                                } else if (status == "declined") {
                                                    error = "Your registration was declined."
                                                } else {
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
                                                        putBoolean("is_attending", false)
                                                        putBoolean("is_logged_in", true)
                                                        apply()
                                                    }
                                                    android.widget.Toast.makeText(context, "Login Successful", android.widget.Toast.LENGTH_SHORT).show()
                                                    navController.navigate("student_dashboard") {
                                                        popUpTo("landing")
                                                    }
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
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red), 
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

