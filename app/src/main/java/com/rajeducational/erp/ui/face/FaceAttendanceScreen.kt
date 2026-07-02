package com.rajeducational.erp.ui.face

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import androidx.camera.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.firebase.firestore.FirebaseFirestore
import com.rajeducational.erp.theme.AppColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale

@kotlin.OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun FaceAttendanceScreen(navController: NavController, userType: String) { // userType is "IN" or "OUT"
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val firestore = remember { FirebaseFirestore.getInstance() }

    val prefs = remember { context.getSharedPreferences("AttendanceScanPrefs", Context.MODE_PRIVATE) }
    val todayStr = remember { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date()) }

    var statusMessage by remember { mutableStateOf("Ready to scan student QR codes.") }
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var recentScans by remember { mutableStateOf<List<String>>(emptyList()) }

    fun speak(textHi: String, textEn: String, utteranceId: String) {
        statusMessage = textEn
        tts?.speak(textHi, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
        val savedStr = prefs.getString("student_scans_$todayStr", "") ?: ""
        if (savedStr.isNotEmpty()) {
            recentScans = savedStr.split("\n")
        }
    }

    DisposableEffect(Unit) {
        var textToSpeech: TextToSpeech? = null
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val locale = Locale("hi", "IN")
                val result = textToSpeech?.setLanguage(locale)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    textToSpeech?.setLanguage(Locale.US)
                }
                textToSpeech?.speak("कृपया छात्र का क्यूआर कोड दिखाएं।", TextToSpeech.QUEUE_FLUSH, null, "ready")
            }
        }
        tts = textToSpeech
        onDispose {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Student QR Attendance (${if (userType == "IN") "Time In" else "Time Out"})", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Teacher)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.Background)
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = AppColors.Teacher.copy(alpha = 0.1f))
            ) {
                Text(
                    text = statusMessage,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.Navy,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(280.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (!cameraPermissionState.status.isGranted) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.QrCodeScanner,
                            contentDescription = "Camera Permission Required",
                            tint = Color.Gray,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { cameraPermissionState.launchPermissionRequest() },
                            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Teacher)
                        ) {
                            Text("Allow Camera")
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        FaceCameraPreview(
                            state = FaceRegState.IDLE,
                            onFrontFaceCaptured = {},
                            onLivenessVerified = {},
                            onFaceDetected = { _, _, _ -> },
                            onBarcodeScanned = { code ->
                                if (!isProcessing) {
                                    isProcessing = true
                                    coroutineScope.launch {
                                        try {
                                            if (code.startsWith("student_attendance_qr:")) {
                                                val parts = code.split(":")
                                                if (parts.size >= 2) {
                                                    val scannedStudentId = parts[1]
                                                    val stdDoc = firestore.collection("students").document(scannedStudentId).get().await()
                                                    if (stdDoc.exists()) {
                                                        val studentName = stdDoc.getString("fullName") ?: stdDoc.getString("name") ?: "Student"
                                                        val course = stdDoc.getString("course") ?: stdDoc.getString("class") ?: "College"
                                                        val college = stdDoc.getString("college") ?: ""
                                                        val batch = stdDoc.getString("session") ?: stdDoc.getString("batch") ?: ""

                                                        val holidayCheck = com.rajeducational.erp.utils.HolidayHelper.checkHolidayForStudent(college, course, batch)
                                                        if (holidayCheck.isHoliday) {
                                                            if (holidayCheck.holidayName.equals("Sunday", ignoreCase = true)) {
                                                                speak("आज रविवार है, आज उपस्थिति नहीं ली जाएगी।", "Today is Sunday, today attendance won't be taken.", "sunday_error")
                                                                statusMessage = "Sunday: Attendance won't be taken."
                                                            } else {
                                                                speak("आज छुट्टी है, आज उपस्थिति नहीं ली जाएगी।", "Today is a holiday (${holidayCheck.holidayName}), today attendance won't be taken.", "holiday_error")
                                                                statusMessage = "Holiday: ${holidayCheck.holidayName}"
                                                            }
                                                            delay(3000)
                                                            isProcessing = false
                                                            return@launch
                                                        }

                                                        val scannedKey = "${scannedStudentId}_${userType}"
                                                        val alreadyScanned = prefs.getBoolean("student_scanned_${todayStr}_$scannedKey", false)

                                                        if (alreadyScanned) {
                                                            speak("${studentName}, आपका स्कैनिंग पहले ही हो चुका है। अब आप जा सकते हैं।", "Your scanning is already done. You can go now.", "scanned_already")
                                                            statusMessage = "Already Scanned: $studentName ($userType)"
                                                            delay(3000)
                                                        } else {
                                                            val attendanceData = hashMapOf(
                                                                "studentId" to scannedStudentId,
                                                                "studentName" to studentName,
                                                                "role" to "Student",
                                                                "course" to course,
                                                                "timestamp" to System.currentTimeMillis(),
                                                                "type" to userType,
                                                                "status" to "Present"
                                                            )
                                                            firestore.collection("attendance").add(attendanceData).await()

                                                            val displayTime = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date())
                                                            val displayStr = "$studentName - $userType ($displayTime)"
                                                            val newList = (listOf(displayStr) + recentScans).take(50)
                                                            recentScans = newList

                                                            prefs.edit()
                                                                .putString("student_scans_$todayStr", newList.joinToString("\n"))
                                                                .putBoolean("student_scanned_${todayStr}_$scannedKey", true)
                                                                .apply()

                                                            speak("${studentName}, आपकी उपस्थिति दर्ज हो गई है।", "Attendance marked for ${studentName} (${userType})", "scanned_ok")
                                                            delay(2000)
                                                        }
                                                    } else {
                                                        speak("यह छात्र पंजीकृत नहीं है।", "Student profile not found in database.", "scanned_error")
                                                        delay(2000)
                                                    }
                                                }
                                            } else if (code.startsWith("teacher_attendance_qr:") || code.startsWith("staff_attendance_qr:")) {
                                                speak("आप एक शिक्षक या स्टाफ हैं। आप छात्र का क्यूआर कोड ही स्कैन कर सकते हैं।", "Teachers cannot scan teacher or staff QR codes.", "wrong_role_error")
                                                delay(3000)
                                            } else {
                                                speak("अमान्य क्यूआर कोड।", "Invalid QR code format.", "invalid_qr")
                                                delay(2000)
                                            }
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                            statusMessage = "Error: ${e.message}"
                                            delay(2000)
                                        } finally {
                                            isProcessing = false
                                        }
                                    }
                                }
                            }
                        )

                        // Green boundary visual guide represent "QR code scanning area"
                        Box(
                            modifier = Modifier
                                .size(200.dp)
                                .border(3.dp, Color.Green.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                        )

                        // Help text overlaid at bottom
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Text(
                                text = "Align QR code inside the box",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 11.sp,
                                modifier = Modifier
                                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Recent Scans History",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.Navy,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (recentScans.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No scans yet. Point camera at student QR code.",
                            color = AppColors.TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(recentScans) { scan ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF1F8E9), RoundedCornerShape(8.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, "Scanned", tint = Color(0xFF4CAF50))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = scan,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF2E7D32)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
