package com.rajeducational.erp.ui.admin

import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Upload
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
import com.rajeducational.erp.data.College
import com.rajeducational.erp.data.FeeFile
import com.rajeducational.erp.theme.AppColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import okhttp3.RequestBody.Companion.toRequestBody

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeeStructureControlScreen(navController: NavController) {
    val firestore = FirebaseFirestore.getInstance()
    var colleges by remember { mutableStateOf<List<College>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        firestore.collection("colleges").addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(College::class.java)?.copy(id = doc.id)
                }
                colleges = list
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fee Structure Control Center", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, "Back") }
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
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AppColors.Admin)
                }
            } else if (colleges.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No colleges available", color = AppColors.TextSecondary)
                }
            } else {
                LazyColumn(modifier = Modifier.padding(16.dp)) {
                    items(colleges) { college ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { navController.navigate("admin_fee_course_list/${college.id}") },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Business, contentDescription = null, tint = AppColors.Admin)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(college.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AppColors.Navy)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeeCourseListScreen(navController: NavController, collegeId: String) {
    val firestore = FirebaseFirestore.getInstance()
    var college by remember { mutableStateOf<College?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(collegeId) {
        if (collegeId.isNotEmpty()) {
            firestore.collection("colleges").document(collegeId).addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    college = snapshot.toObject(College::class.java)?.copy(id = snapshot.id)
                    isLoading = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(college?.name ?: "Loading...", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, "Back") }
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
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AppColors.Admin)
                }
            } else if (college == null || college!!.courses.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No courses available", color = AppColors.TextSecondary)
                }
            } else {
                LazyColumn(modifier = Modifier.padding(16.dp)) {
                    items(college!!.courses.size) { index ->
                        val course = college!!.courses[index]
                        FeeStructureCourseCard(college!!, course, index, firestore)
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun FeeStructureCourseCard(college: College, course: com.rajeducational.erp.data.Course, courseIndex: Int, firestore: FirebaseFirestore) {
    var expanded by remember { mutableStateOf(false) }
    var feeText by remember { mutableStateOf(course.feeStructureText) }
    var isUploading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                isUploading = true
                uploadFileToHostinger(uri, context, college, course, courseIndex, firestore) { success ->
                    isUploading = false
                    if (success) {
                        Toast.makeText(context, "Uploaded successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Upload failed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(course.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AppColors.Navy)
            }

            if (expanded) {
                Divider(modifier = Modifier.padding(vertical = 12.dp))

                OutlinedTextField(
                    value = feeText,
                    onValueChange = { feeText = it },
                    label = { Text("Fee Description Text") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        val updatedCourses = college.courses.toMutableList()
                        updatedCourses[courseIndex] = course.copy(feeStructureText = feeText)
                        firestore.collection("colleges").document(college.id).update("courses", updatedCourses)
                            .addOnSuccessListener { Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show() }
                    },
                    modifier = Modifier.align(Alignment.End),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Admin)
                ) {
                    Text("Save Text")
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Attachments", fontWeight = FontWeight.SemiBold, color = AppColors.Navy)
                Spacer(modifier = Modifier.height(8.dp))
                
                course.feeFiles.forEach { feeFile ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically, 
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                var url = feeFile.viewUrl
                                if (!url.startsWith("http")) {
                                    url = "https://rajapp.matavaishnavieducationaltrust.org/$url"
                                }
                                com.rajeducational.erp.ui.common.PdfOpener.openPdf(context, url, coroutineScope)
                            }
                    ) {
                        Icon(Icons.Default.Description, contentDescription = null, tint = AppColors.Admin)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(feeFile.originalName, modifier = Modifier.weight(1f), fontSize = 14.sp)
                        IconButton(onClick = {
                            coroutineScope.launch {
                                val success = deleteFileFromHostinger(feeFile.deleteUrl)
                                if (success) {
                                    val newFiles = course.feeFiles.filter { it.id != feeFile.id }
                                    val updatedCourses = college.courses.toMutableList()
                                    updatedCourses[courseIndex] = course.copy(feeFiles = newFiles)
                                    firestore.collection("colleges").document(college.id).update("courses", updatedCourses)
                                } else {
                                    Toast.makeText(context, "Delete failed", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }) {
                            Icon(Icons.Default.Delete, "Delete", tint = Color.Red, modifier = Modifier.size(20.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (isUploading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally).size(24.dp), color = AppColors.Admin)
                } else {
                    OutlinedButton(
                        onClick = { launcher.launch("*/*") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Upload, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Upload PDF / Image")
                    }
                }
            }
        }
    }
}

suspend fun uploadFileToHostinger(
    uri: Uri,
    context: android.content.Context,
    college: College,
    course: com.rajeducational.erp.data.Course,
    courseIndex: Int,
    firestore: FirebaseFirestore,
    onResult: (Boolean) -> Unit
) {
    withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val file = File(context.cacheDir, getFileName(contentResolver, uri))
            val inputStream = contentResolver.openInputStream(uri)
            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()

            val client = OkHttpClient()
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.name, file.asRequestBody(contentResolver.getType(uri)?.toMediaTypeOrNull()))
                .addFormDataPart("category", "documents")
                .addFormDataPart("uploaded_by", "admin")
                .build()

            val request = Request.Builder()
                .url("https://rajapp.matavaishnavieducationaltrust.org/upload.php")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (response.isSuccessful && responseBody != null) {
                val jsonResponse = JSONObject(responseBody)
                if (jsonResponse.optBoolean("success")) {
                    val dataObj = jsonResponse.optJSONObject("data")
                    if (dataObj != null) {
                        val feeFile = FeeFile(
                            id = dataObj.optInt("id"),
                            originalName = dataObj.optString("original_name"),
                            fileType = dataObj.optString("file_type"),
                            viewUrl = dataObj.optString("view_url"),
                            deleteUrl = dataObj.optString("delete_url")
                        )
                        val newFiles = course.feeFiles + feeFile
                        val updatedCourses = college.courses.toMutableList()
                        updatedCourses[courseIndex] = course.copy(feeFiles = newFiles)
                        firestore.collection("colleges").document(college.id).update("courses", updatedCourses).addOnSuccessListener {
                            onResult(true)
                        }
                    } else {
                        onResult(false)
                    }
                } else {
                    onResult(false)
                }
            } else {
                onResult(false)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            onResult(false)
        }
    }
}

suspend fun deleteFileFromHostinger(deleteUrl: String): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            val fullUrl = "https://rajapp.matavaishnavieducationaltrust.org/$deleteUrl"
            val client = OkHttpClient()
            val request = Request.Builder()
                .url(fullUrl)
                .delete()
                .build()
            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

private fun getFileName(contentResolver: android.content.ContentResolver, uri: Uri): String {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = contentResolver.query(uri, null, null, null, null)
        try {
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) result = cursor.getString(index)
            }
        } finally {
            cursor?.close()
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/')
        if (cut != null && cut != -1) {
            result = result.substring(cut + 1)
        }
    }
    return result ?: "upload_file"
}
