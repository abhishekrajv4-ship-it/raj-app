package com.rajeducational.erp.ui.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore
import com.rajeducational.erp.theme.AppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherLoginScreen(navController: NavController) {
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()

    var loginId by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }
    var loginError by remember { mutableStateOf<String?>(null) }
    var isLoggingIn by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Teacher Login", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Teacher, titleContentColor = Color.White, navigationIconContentColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(AppColors.Background)
                .padding(16.dp)
        ) {
            Text("Already a registered user? Login", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppColors.Navy)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = loginId,
                onValueChange = { loginId = it },
                label = { Text("ID (Phone Number)") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = loginPassword,
                onValueChange = { loginPassword = it },
                label = { Text("Password") },
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            
            if (loginError != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(loginError!!, color = Color.Red, fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    if (loginId.isNotBlank() && loginPassword.isNotBlank()) {
                        isLoggingIn = true
                        loginError = null
                        val trimmedId = loginId.trim()
                        val trimmedPassword = loginPassword.trim()
                        firestore.collection("teachers")
                            .whereEqualTo("phone", trimmedId)
                            .whereEqualTo("password", trimmedPassword)
                            .get()
                            .addOnSuccessListener { querySnapshot ->
                                isLoggingIn = false
                                if (!querySnapshot.isEmpty) {
                                    val doc = querySnapshot.documents[0]
                                    val prefs = context.getSharedPreferences("TeacherPrefs", android.content.Context.MODE_PRIVATE)
                                    prefs.edit().apply {
                                        putString("teacher_id", doc.id)
                                        putString("teacher_name", doc.getString("name"))
                                        putString("teacher_phone", doc.getString("phone"))
                                        putString("teacher_email", doc.getString("email"))
                                        putString("teacher_department", doc.getString("departmentName"))
                                        putString("teacher_college", doc.getString("collegeName"))
                                        putString("teacher_course", doc.getString("course") ?: doc.getString("departmentName"))
                                        val courses = doc.get("courses") as? List<String> ?: emptyList()
                                        putStringSet("teacher_courses", courses.toSet())
                                        val years = doc.get("years") as? List<String> ?: emptyList()
                                        putStringSet("teacher_years", years.toSet())
                                        putBoolean("is_logged_in", true)
                                        apply()
                                    }
                                    android.widget.Toast.makeText(context, "Login Successful", android.widget.Toast.LENGTH_SHORT).show()
                                    navController.navigate("teacher_announcements") {
                                        popUpTo("landing")
                                    }
                                } else {
                                    loginError = "Invalid ID or Password"
                                }
                            }
                            .addOnFailureListener {
                                isLoggingIn = false
                                loginError = "Login failed. Please try again."
                            }
                    } else {
                        loginError = "Please enter ID and Password"
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Teacher),
                enabled = !isLoggingIn
            ) {
                Text(if (isLoggingIn) "Logging in..." else "Login")
            }
        }
    }
}
