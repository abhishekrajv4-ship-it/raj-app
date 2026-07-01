package com.rajeducational.erp.ui.teacher

import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.firebase.firestore.FirebaseFirestore
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.rajeducational.erp.theme.AppColors
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun TeacherAuthScreen(navController: NavController) {
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()
    
    var scannedCode by remember { mutableStateOf<String?>(null) }
    var isChecking by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("TeacherPrefs", android.content.Context.MODE_PRIVATE)
        if (prefs.getBoolean("is_logged_in", false)) {
            navController.navigate("teacher_announcements") {
                popUpTo("landing")
            }
        }
    }

    LaunchedEffect(scannedCode) {
        if (scannedCode != null && !isChecking) {
            isChecking = true
            try {
                val doc = firestore.collection("settings").document("teacher_qr").get().await()
                val token = doc.getString("token")
                val timestamp = doc.getLong("timestamp") ?: 0L
                val currentTime = System.currentTimeMillis()
                
                if (scannedCode == token) {
                    if (currentTime - timestamp <= 120 * 1000) { // 2 minutes
                        // Valid Token
                        android.widget.Toast.makeText(context, "Authorization Successful", android.widget.Toast.LENGTH_SHORT).show()
                        navController.navigate("teacher_registration") {
                            popUpTo("teacher_auth") { inclusive = true }
                        }
                    } else {
                        authError = "QR Code Expired"
                    }
                } else {
                    authError = "Invalid QR Code"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                authError = "Error verifying code"
            } finally {
                kotlinx.coroutines.delay(240000)
                isChecking = false
                scannedCode = null // reset to allow scanning again
            }
        }
    }

    Scaffold(
        topBar = { 
            TopAppBar(
                title = { Text("Teacher Scanner", fontWeight = FontWeight.Bold) }, 
                navigationIcon = { 
                    IconButton(onClick = { navController.popBackStack() }) { 
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") 
                    } 
                },
                actions = {
                    TextButton(onClick = { navController.navigate("teacher_login") }) {
                        Text("Login", color = Color.White)
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Icon(Icons.Default.QrCodeScanner, "Scanner", tint = AppColors.Teacher, modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text("Authorize yourself by scanning the QR code.", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Navy, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(modifier = Modifier.height(8.dp))
            Text("The QR code is available in the Admin panel.", fontSize = 14.sp, color = AppColors.TextSecondary)
            
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
                        QRScannerPreview { code ->
                            if (!isChecking) {
                                scannedCode = code
                            }
                        }
                        if (isChecking) {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center),
                                color = AppColors.Teacher
                            )
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Camera permission is required to scan the QR code.", textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { cameraPermissionState.launchPermissionRequest() },
                            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Teacher)
                        ) {
                            Text("Grant Permission")
                        }
                    }
                }
            }

            if (authError != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(authError!!, color = Color.Red, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                "Note: Please scan the QR code which you will get from the admin panel person. After scanning the QR code, you will be able to register.",
                fontSize = 14.sp,
                color = AppColors.TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun QRScannerPreview(onCodeScanned: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val options = BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                        .build()
                    val scanner = BarcodeScanning.getClient(options)

                    val imageAnalyzer = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also {
                            it.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { imageProxy ->
                                @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
                                val mediaImage = imageProxy.image
                                if (mediaImage != null) {
                                    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                    scanner.process(image)
                                        .addOnSuccessListener { barcodes ->
                                            for (barcode in barcodes) {
                                                barcode.rawValue?.let { code ->
                                                    onCodeScanned(code)
                                                }
                                            }
                                        }
                                        .addOnCompleteListener {
                                            imageProxy.close()
                                        }
                                } else {
                                    imageProxy.close()
                                }
                            }
                        }

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalyzer
                        )
                    } catch (exc: Exception) {
                        Log.e("QRScanner", "Use case binding failed", exc)
                    }
                } catch (e: Exception) {
                    Log.e("QRScanner", "Camera initialization failed", e)
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}
