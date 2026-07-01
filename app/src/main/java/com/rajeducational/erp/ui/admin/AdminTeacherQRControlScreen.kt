package com.rajeducational.erp.ui.admin

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.rajeducational.erp.theme.AppColors
import kotlinx.coroutines.delay
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminTeacherQRControlScreen(navController: NavController) {
    val firestore = FirebaseFirestore.getInstance()
    var currentToken by remember { mutableStateOf("") }
    var timeRemaining by remember { mutableStateOf(120) }

    // Generate new token every 2 minutes
    LaunchedEffect(Unit) {
        while (true) {
            val newToken = UUID.randomUUID().toString()
            currentToken = newToken
            timeRemaining = 120
            
            firestore.collection("settings").document("teacher_qr")
                .set(mapOf("token" to newToken, "timestamp" to System.currentTimeMillis()))
            
            // Countdown timer
            for (i in 120 downTo 1) {
                delay(1000L)
                timeRemaining = i - 1
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Teacher QR Registration", fontWeight = FontWeight.Bold, color = Color.White) },
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Ask Teacher to scan this QR Code",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.Navy,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                "This QR code will expire in ${timeRemaining / 60}:${(timeRemaining % 60).toString().padStart(2, '0')}",
                fontSize = 16.sp,
                color = if (timeRemaining < 30) Color.Red else AppColors.TextSecondary,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            if (currentToken.isNotEmpty()) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    val qrBitmap = generateQrCode(currentToken)
                    if (qrBitmap != null) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "QR Code",
                            modifier = Modifier
                                .padding(24.dp)
                                .size(250.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            Text(
                "Note: The QR code is valid for 2 minutes and is specifically for initial Teacher authorization & registration.",
                fontSize = 14.sp,
                color = AppColors.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

fun generateQrCode(content: String): Bitmap? {
    try {
        val width = 500
        val height = 500
        val bitMatrix = MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, width, height)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
        return bitmap
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}
