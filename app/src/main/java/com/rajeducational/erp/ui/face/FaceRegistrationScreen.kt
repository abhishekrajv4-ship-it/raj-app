package com.rajeducational.erp.ui.face

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.Image
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.rajeducational.erp.theme.AppColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.Locale
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.util.UUID

enum class FaceRegState {
    IDLE,
    WAITING_FRONT,
    WAITING_TURN,
    READY_TO_SUBMIT,
    UPLOADING,
    SUCCESS,
    ERROR
}

@kotlin.OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun FaceRegistrationScreen(navController: NavController, userType: String, userId: String) {
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    var state by remember { mutableStateOf(FaceRegState.IDLE) }
    var statusMessage by remember { mutableStateOf("This is a very important part. Please give a clear and bright photo for your profile picture. This will also be used for attendance.") }
    var capturedFrontBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var detectedFaceBounds by remember { mutableStateOf<Rect?>(null) }
    var imageWidth by remember { mutableStateOf(1) }
    var imageHeight by remember { mutableStateOf(1) }

    fun speak(textHi: String, textEn: String, utteranceId: String) {
        statusMessage = textEn
        tts?.speak(textHi, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    DisposableEffect(Unit) {
        val textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val locale = Locale("hi", "IN")
                val result = tts?.setLanguage(locale)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.setLanguage(Locale.US)
                }
                // Speak initial message
                speak(
                    "यह एक बहुत ही महत्वपूर्ण हिस्सा है। कृपया अपनी प्रोफाइल पिक्चर के लिए एक साफ और उज्ज्वल फोटो दें। इसका उपयोग उपस्थिति के लिए भी किया जाएगा।",
                    "This is a very important part. Please give a clear and bright photo for your profile picture.",
                    "initial"
                )
            }
        }
        textToSpeech.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                coroutineScope.launch(Dispatchers.Main) {
                    when (utteranceId) {
                        "initial" -> {
                            speak("कृपया प्रोफाइल पिक्चर के लिए सीधे देखें।", "Please look straight for the profile picture.", "look_straight")
                        }
                        "look_straight" -> {
                            state = FaceRegState.WAITING_FRONT
                        }
                        "front_done" -> {
                            state = FaceRegState.WAITING_TURN
                        }
                        "upload_done" -> {
                            state = FaceRegState.SUCCESS
                            kotlinx.coroutines.delay(1000)
                            if (userType == "non_attending" || userType == "student") {
                                android.widget.Toast.makeText(context, "Registration Submitted. Wait for your approval. And remember your password.", android.widget.Toast.LENGTH_LONG).show()
                                navController.navigate("landing") { popUpTo(0) }
                            } else if (!navController.popBackStack()) {
                                if (userType == "teacher") {
                                    navController.navigate("teacher_announcements") { popUpTo(0) }
                                } else if (userType == "staff") {
                                    navController.navigate("staff_announcements") { popUpTo(0) }
                                } else {
                                    navController.navigate("student_dashboard") { popUpTo(0) }
                                }
                            }
                        }
                    }
                }
            }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {}
        })
        tts = textToSpeech
        onDispose {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Face Registration", color = Color.White, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = if (userType == "teacher") AppColors.Teacher else AppColors.Student)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = statusMessage,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Box(
                modifier = Modifier
                    .size(300.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray),
                contentAlignment = Alignment.Center
            ) {
                if (!cameraPermissionState.status.isGranted) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Face,
                            contentDescription = "Camera Permission Required",
                            tint = Color.Gray,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Camera Access Required",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = AppColors.Navy
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { cameraPermissionState.launchPermissionRequest() },
                            colors = ButtonDefaults.buttonColors(containerColor = if (userType == "teacher") AppColors.Teacher else AppColors.Student)
                        ) {
                            Text("Allow Camera")
                        }
                    }
                } else if (state == FaceRegState.IDLE || state == FaceRegState.WAITING_FRONT || state == FaceRegState.WAITING_TURN) {
                    FaceCameraPreview(
                        state = state,
                        onFrontFaceCaptured = { bitmap ->
                            if (state == FaceRegState.WAITING_FRONT) {
                                capturedFrontBitmap = bitmap
                                state = FaceRegState.IDLE
                                speak("आपका फ्रंट फेस रिकॉग्निशन हो गया है। अब कृपया अधिक सटीक प्रोफाइल तस्वीर के लिए अपना बायां या दायां चेहरा प्रोफाइल दिखाएं।", "Your front face recognition has been done. Now, kindly show your left or right face profile for more accurate profile picture.", "front_done")
                            }
                        },
                        onLivenessVerified = {
                            if (state == FaceRegState.WAITING_TURN) {
                                state = FaceRegState.UPLOADING
                                statusMessage = "Uploading profile..."
                                coroutineScope.launch {
                                    try {
                                        val url = uploadBitmapToFirebase(capturedFrontBitmap!!)
                                        val firestore = FirebaseFirestore.getInstance()
                                        val collection = when (userType) {
                                            "teacher" -> "teachers"
                                            "staff" -> "staffs"
                                            else -> "students"
                                        }
                                        firestore.collection(collection).document(userId)
                                            .set(mapOf("profileUrl" to url), SetOptions.merge())
                                            .await()
                                        
                                        speak("आपका फेस स्कैन सफलतापूर्वक पूरा हो गया है। डैशबोर्ड में जा रहे हैं।", "Your face scan has been successfully completed. Entering dashboard.", "upload_done")
                                    } catch (e: Exception) {
                                        state = FaceRegState.ERROR
                                        speak("त्रुटि हुई। कृपया पुनः प्रयास करें।", "Error occurred: ${e.message}", "error")
                                    }
                                }
                            }
                        },
                        onFaceDetected = { bounds, w, h ->
                            detectedFaceBounds = bounds
                            imageWidth = w
                            imageHeight = h
                        }
                    )
                    
                    if (detectedFaceBounds != null && !detectedFaceBounds!!.isEmpty) {
                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                            val scaleX = size.width / imageWidth
                            val scaleY = size.height / imageHeight
                            // The front camera might be horizontally flipped depending on the device,
                            // we do a simple mapping here. Assuming fillMaxSize matches PreviewView scaleType FILL_CENTER
                            val scale = maxOf(scaleX, scaleY)
                            val offsetX = (size.width - imageWidth * scale) / 2f
                            val offsetY = (size.height - imageHeight * scale) / 2f

                            val rectLeft = size.width - (detectedFaceBounds!!.right * scale + offsetX)
                            val rectTop = detectedFaceBounds!!.top * scale + offsetY
                            val rectRight = size.width - (detectedFaceBounds!!.left * scale + offsetX)
                            val rectBottom = detectedFaceBounds!!.bottom * scale + offsetY

                            drawRect(
                                color = Color.Green,
                                topLeft = androidx.compose.ui.geometry.Offset(rectLeft, rectTop),
                                size = androidx.compose.ui.geometry.Size(rectRight - rectLeft, rectBottom - rectTop),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 8f)
                            )
                        }
                    }
                } else if (capturedFrontBitmap != null) {
                    Image(
                        bitmap = capturedFrontBitmap!!.asImageBitmap(),
                        contentDescription = "Profile Picture",
                        modifier = Modifier.fillMaxSize()
                    )
                    if (state == FaceRegState.SUCCESS) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = Color.Green,
                            modifier = Modifier
                                .size(80.dp)
                                .align(Alignment.Center)
                        )
                    } else if (state == FaceRegState.UPLOADING) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (state == FaceRegState.WAITING_FRONT) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "यह एक महत्वपूर्ण हिस्सा है। अपनी प्रोफाइल के लिए एक साफ और उज्ज्वल फोटो दें। इसका उपयोग उपस्थिति के लिए भी होगा। कृपया सामने देखें।",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

suspend fun uploadBitmapToFirebase(bitmap: Bitmap): String = withContext(Dispatchers.IO) {
    val baos = ByteArrayOutputStream()
    // compress to a smaller size to save Firestore space
    val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 200, 200, true)
    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 60, baos)
    val data = baos.toByteArray()
    "data:image/jpeg;base64," + android.util.Base64.encodeToString(data, android.util.Base64.NO_WRAP)
}

@kotlin.OptIn(androidx.camera.core.ExperimentalGetImage::class)
@Composable
fun FaceCameraPreview(
    state: FaceRegState,
    onFrontFaceCaptured: (Bitmap) -> Unit,
    onLivenessVerified: () -> Unit,
    onFaceDetected: (Rect, Int, Int) -> Unit = { _, _, _ -> },
    onBarcodeScanned: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val updatedState by rememberUpdatedState(newValue = state)
    val updatedOnFrontFaceCaptured by rememberUpdatedState(newValue = onFrontFaceCaptured)
    val updatedOnLivenessVerified by rememberUpdatedState(newValue = onLivenessVerified)
    val updatedOnFaceDetected by rememberUpdatedState(newValue = onFaceDetected)
    val updatedOnBarcodeScanned by rememberUpdatedState(newValue = onBarcodeScanned)

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                    .build()

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                val detectorOptions = FaceDetectorOptions.Builder()
                    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                    .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                    .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                    .build()
                val faceDetector = FaceDetection.getClient(detectorOptions)

                val barcodeScannerOptions = BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                    .build()
                val barcodeScanner = BarcodeScanning.getClient(barcodeScannerOptions)

                var lastProcessingTimeMs = 0L

                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { imageProxy ->
                    val mediaImage = imageProxy.image
                    val currentTime = System.currentTimeMillis()
                    
                    if (mediaImage != null) {
                        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                        
                        // First run barcode scanner
                        barcodeScanner.process(image)
                            .addOnSuccessListener { barcodes ->
                                for (barcode in barcodes) {
                                    val code = barcode.rawValue
                                    if (code != null) {
                                        updatedOnBarcodeScanned(code)
                                    }
                                }
                            }
                            .addOnFailureListener {
                                it.printStackTrace()
                            }
                            .addOnCompleteListener {
                                // Now run face detector
                                faceDetector.process(image)
                                    .addOnSuccessListener { faces ->
                                        if (faces.isNotEmpty()) {
                                            val face = faces.first()
                                            // Callback with bounds, width, and height. Width/Height are swapped if rotation is 90 or 270.
                                            val isPortrait = imageProxy.imageInfo.rotationDegrees == 90 || imageProxy.imageInfo.rotationDegrees == 270
                                            val imgW = if (isPortrait) image.height else image.width
                                            val imgH = if (isPortrait) image.width else image.height
                                            updatedOnFaceDetected(face.boundingBox, imgW, imgH)

                                            val rotY = face.headEulerAngleY // Left/Right
                                            if (currentTime - lastProcessingTimeMs > 500) {
                                                if (updatedState == FaceRegState.WAITING_FRONT) {
                                                    if (rotY > -20 && rotY < 20) {
                                                        // Capture front face
                                                        val bitmap = imageProxy.toBitmap()
                                                        if (bitmap != null) {
                                                            val rotatedBitmap = rotateBitmap(bitmap, imageProxy.imageInfo.rotationDegrees.toFloat())
                                                            val flippedBitmap = flipBitmapHorizontal(rotatedBitmap)
                                                            updatedOnFrontFaceCaptured(flippedBitmap)
                                                            lastProcessingTimeMs = currentTime
                                                        }
                                                    }
                                                } else if (updatedState == FaceRegState.WAITING_TURN) {
                                                    if (rotY > 20 || rotY < -20) {
                                                        updatedOnLivenessVerified()
                                                        lastProcessingTimeMs = currentTime
                                                    }
                                                }
                                            }
                                        } else {
                                            updatedOnFaceDetected(Rect(), 1, 1) // Clear box
                                        }
                                    }
                                    .addOnFailureListener {
                                        it.printStackTrace()
                                    }
                                    .addOnCompleteListener {
                                        imageProxy.close()
                                    }
                            }
                    } else {
                        imageProxy.close()
                    }
                }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

fun rotateBitmap(source: Bitmap, angle: Float): Bitmap {
    val matrix = Matrix()
    matrix.postRotate(angle)
    return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
}

fun flipBitmapHorizontal(source: Bitmap): Bitmap {
    val matrix = Matrix()
    matrix.postScale(-1f, 1f, source.width / 2f, source.height / 2f)
    return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
}
