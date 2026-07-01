package com.rajeducational.erp.ui.admin

import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore
import com.rajeducational.erp.theme.AppColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminStatisticsControlScreen(navController: NavController) {
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var galleryDescription by remember { mutableStateOf("") }
    var logoUrl by remember { mutableStateOf("") }
    
    var whatsappLink by remember { mutableStateOf("https://whatsapp.com/channel/0029VamEW996xCSGsYZevr1I") }
    var instagramLink by remember { mutableStateOf("https://www.instagram.com/rajeducationalgroup.official/") }
    var facebookLink by remember { mutableStateOf("https://www.facebook.com/rajeducationalgroup") }
    var youtubeLink by remember { mutableStateOf("https://www.youtube.com/@Rajeducationalgroupofficial") }
    var websiteLink by remember { mutableStateOf("https://rajeducationalgroup.org") }
    var phoneNumber by remember { mutableStateOf("7485036111") }
    
    var isUploading by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    
    val firestore = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    LaunchedEffect(Unit) {
        val doc = firestore.collection("app_settings").document("general").get().await()
        if (doc.exists()) {
            name = doc.getString("name") ?: ""
            address = doc.getString("address") ?: ""
            description = doc.getString("description") ?: ""
            galleryDescription = doc.getString("galleryDescription") ?: ""
            logoUrl = doc.getString("logoUrl") ?: ""
            doc.getString("whatsappLink")?.let { if (it.isNotBlank()) whatsappLink = it }
            doc.getString("instagramLink")?.let { if (it.isNotBlank()) instagramLink = it }
            doc.getString("facebookLink")?.let { if (it.isNotBlank()) facebookLink = it }
            doc.getString("youtubeLink")?.let { if (it.isNotBlank()) youtubeLink = it }
            doc.getString("websiteLink")?.let { if (it.isNotBlank()) websiteLink = it }
            doc.getString("phoneNumber")?.let { if (it.isNotBlank()) phoneNumber = it }
        }
    }
    
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                isUploading = true
                val uploadedUrl = uploadLogoToHostinger(uri, context)
                if (uploadedUrl != null) {
                    logoUrl = uploadedUrl
                    Toast.makeText(context, "Logo uploaded successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Logo upload failed", Toast.LENGTH_SHORT).show()
                }
                isUploading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard Edit Control Panel", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text("General App Settings", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppColors.Navy)
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("College Name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Address") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = galleryDescription,
                onValueChange = { galleryDescription = it },
                label = { Text("Gallery Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("Social Links & Contact", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Navy)
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                label = { Text("Phone Number") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = whatsappLink,
                onValueChange = { whatsappLink = it },
                label = { Text("WhatsApp Link") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = instagramLink,
                onValueChange = { instagramLink = it },
                label = { Text("Instagram Link") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = facebookLink,
                onValueChange = { facebookLink = it },
                label = { Text("Facebook Link") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = youtubeLink,
                onValueChange = { youtubeLink = it },
                label = { Text("YouTube Link") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = websiteLink,
                onValueChange = { websiteLink = it },
                label = { Text("Website Link") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("College Logo", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Navy)
            Spacer(modifier = Modifier.height(8.dp))
            
            if (logoUrl.isNotEmpty()) {
                var actualUrl = logoUrl
                if (!actualUrl.startsWith("http")) actualUrl = "https://rajapp.matavaishnavieducationaltrust.org/$actualUrl"
                
                Box(modifier = Modifier.size(100.dp).clip(RoundedCornerShape(16.dp))) {
                    coil.compose.AsyncImage(
                        model = actualUrl,
                        contentDescription = "Logo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            Button(
                onClick = { launcher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                enabled = !isUploading
            ) {
                if (isUploading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Uploading...")
                } else {
                    Icon(Icons.Default.Upload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Upload New Logo")
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = {
                    coroutineScope.launch {
                        isSaving = true
                        try {
                            val data = mapOf(
                                "name" to name,
                                "address" to address,
                                "description" to description,
                                "galleryDescription" to galleryDescription,
                                "logoUrl" to logoUrl,
                                "whatsappLink" to whatsappLink,
                                "instagramLink" to instagramLink,
                                "facebookLink" to facebookLink,
                                "youtubeLink" to youtubeLink,
                                "websiteLink" to websiteLink,
                                "phoneNumber" to phoneNumber
                            )
                            firestore.collection("app_settings").document("general").set(data).await()
                            Toast.makeText(context, "Settings saved successfully", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Failed to save settings", Toast.LENGTH_SHORT).show()
                        }
                        isSaving = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Admin),
                enabled = !isSaving && !isUploading
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                } else {
                    Text("Save Settings")
                }
            }
        }
    }
}

private suspend fun uploadLogoToHostinger(
    uri: Uri,
    context: android.content.Context
): String? {
    return withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            var resultName: String? = null
            if (uri.scheme == "content") {
                val cursor = contentResolver.query(uri, null, null, null, null)
                try {
                    if (cursor != null && cursor.moveToFirst()) {
                        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (index != -1) resultName = cursor.getString(index)
                    }
                } finally {
                    cursor?.close()
                }
            }
            if (resultName == null) {
                resultName = uri.path
                val cut = resultName?.lastIndexOf('/')
                if (cut != null && cut != -1) {
                    resultName = resultName.substring(cut + 1)
                }
            }
            val fileName = resultName ?: "logo.jpg"

            val file = File(context.cacheDir, fileName)
            val inputStream = contentResolver.openInputStream(uri)
            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()

            val client = OkHttpClient()
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.name, file.asRequestBody(contentResolver.getType(uri)?.toMediaTypeOrNull()))
                .addFormDataPart("category", "logo")
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
                        return@withContext dataObj.optString("view_url")
                    }
                }
            }
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
