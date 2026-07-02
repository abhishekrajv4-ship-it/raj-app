package com.rajeducational.erp.ui.staff

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore
import com.rajeducational.erp.data.College
import com.rajeducational.erp.theme.AppColors
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffRegistrationScreen(navController: NavController) {
    val firestore = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var departmentName by remember { mutableStateOf("") }

    var colleges by remember { mutableStateOf<List<College>>(emptyList()) }
    var selectedCollege by remember { mutableStateOf<College?>(null) }
    var selectedCourses by remember { mutableStateOf(setOf<com.rajeducational.erp.data.Course>()) }
    var selectedYears by remember { mutableStateOf(setOf<String>()) }

    var expandedCollege by remember { mutableStateOf(false) }
    var expandedCourse by remember { mutableStateOf(false) }

    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            val snapshot = firestore.collection("colleges").get().await()
            colleges = snapshot.documents.mapNotNull { doc ->
                doc.toObject(College::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Staff Details", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Staff)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.Background)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text("Complete Registration", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = AppColors.Navy)
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Address") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = departmentName,
                        onValueChange = { departmentName = it },
                        label = { Text("Department Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Your ID will be your phone number.", fontSize = 12.sp, color = AppColors.Staff, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirm Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
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
                    Text("College & Department", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AppColors.Navy)
                    Spacer(modifier = Modifier.height(12.dp))

                    // College Dropdown
                    ExposedDropdownMenuBox(
                        expanded = expandedCollege,
                        onExpandedChange = { expandedCollege = !expandedCollege },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedCollege?.name ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Select College") },
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
                                        selectedCollege = college
                                        selectedCourses = emptySet()
                                        selectedYears = emptySet()
                                        expandedCollege = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Courses Checkboxes
                    if (selectedCollege != null) {
                        Text("Select Courses/Departments", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        val courseOptions = selectedCollege!!.courses
                        if (courseOptions.isEmpty()) {
                            Text("No courses available for this college", color = AppColors.TextSecondary, fontSize = 14.sp)
                        }
                        courseOptions.forEach { course ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (selectedCourses.contains(course)) {
                                            selectedCourses = selectedCourses - course
                                        } else {
                                            selectedCourses = selectedCourses + course
                                        }
                                        selectedYears = emptySet() // Reset years on course change
                                    }
                                    .padding(vertical = 4.dp)
                            ) {
                                Checkbox(
                                    checked = selectedCourses.contains(course),
                                    onCheckedChange = { checked ->
                                        if (checked) {
                                            selectedCourses = selectedCourses + course
                                        } else {
                                            selectedCourses = selectedCourses - course
                                        }
                                        selectedYears = emptySet()
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = AppColors.Staff)
                                )
                                Text(course.name)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Years Checkboxes
                    if (selectedCourses.isNotEmpty()) {
                        Text("Select Years", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        val yearOptions = selectedCourses.flatMap { it.yearBatches }.distinct().sorted()
                        if (yearOptions.isEmpty()) {
                            Text("No years available for selected courses", color = AppColors.TextSecondary, fontSize = 14.sp)
                        }
                        yearOptions.forEach { year ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (selectedYears.contains(year)) {
                                            selectedYears = selectedYears - year
                                        } else {
                                            selectedYears = selectedYears + year
                                        }
                                    }
                                    .padding(vertical = 4.dp)
                            ) {
                                Checkbox(
                                    checked = selectedYears.contains(year),
                                    onCheckedChange = { checked ->
                                        if (checked) {
                                            selectedYears = selectedYears + year
                                        } else {
                                            selectedYears = selectedYears - year
                                        }
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = AppColors.Staff)
                                )
                                Text(year)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (name.isNotBlank() && phone.isNotBlank() && address.isNotBlank() && departmentName.isNotBlank() && password.isNotBlank() && selectedCollege != null && selectedCourses.isNotEmpty() && selectedYears.isNotEmpty()) {
                        if (password != confirmPassword) {
                            android.widget.Toast.makeText(context, "Passwords do not match", android.widget.Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isSaving = true
                        val trimmedPhone = phone.trim()
                        val trimmedPassword = password.trim()
                        val courseNames = selectedCourses.map { it.name }
                        val firstCourseName = courseNames.firstOrNull() ?: ""
                        coroutineScope.launch {
                            try {
                                val existing = firestore.collection("staffs")
                                    .whereEqualTo("phone", trimmedPhone)
                                    .get()
                                    .await()
                                
                                if (!existing.isEmpty) {
                                    android.widget.Toast.makeText(context, "Already registered, please login", android.widget.Toast.LENGTH_LONG).show()
                                    isSaving = false
                                    return@launch
                                }

                                val staffData = hashMapOf(
                                    "name" to name.trim(),
                                    "phone" to trimmedPhone,
                                    "email" to email.trim(),
                                    "address" to address.trim(),
                                    "departmentName" to departmentName.trim(),
                                    "password" to trimmedPassword,
                                    "collegeId" to selectedCollege!!.id,
                                    "collegeName" to selectedCollege!!.name,
                                    "course" to firstCourseName,
                                    "courses" to courseNames,
                                    "years" to selectedYears.toList(),
                                    "timestamp" to System.currentTimeMillis()
                                )
                                val docRef = firestore.collection("staffs").add(staffData).await()
                                
                                // Save locally
                                val prefs = context.getSharedPreferences("StaffPrefs", android.content.Context.MODE_PRIVATE)
                                prefs.edit().apply {
                                    putString("staff_id", docRef.id)
                                    putString("staff_name", name.trim())
                                    putString("staff_phone", trimmedPhone)
                                    putString("staff_email", email.trim())
                                    putString("staff_department", departmentName.trim())
                                    putString("staff_college", selectedCollege!!.name)
                                    putString("staff_course", firstCourseName)
                                    putStringSet("staff_courses", courseNames.toSet())
                                    putStringSet("staff_years", selectedYears)
                                    putBoolean("is_logged_in", true)
                                    apply()
                                }
                                
                                android.widget.Toast.makeText(context, "Registration successful", android.widget.Toast.LENGTH_SHORT).show()
                                navController.navigate("face_registration/staff/${docRef.id}") {
                                    popUpTo(0)
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                android.widget.Toast.makeText(context, "Error saving details", android.widget.Toast.LENGTH_SHORT).show()
                            } finally {
                                isSaving = false
                            }
                        }
                    } else {
                        android.widget.Toast.makeText(context, "Please fill all required fields", android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Staff),
                shape = RoundedCornerShape(12.dp),
                enabled = !isSaving
            ) {
                Text(if (isSaving) "Saving..." else "Save Details & Login", modifier = Modifier.padding(8.dp), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
