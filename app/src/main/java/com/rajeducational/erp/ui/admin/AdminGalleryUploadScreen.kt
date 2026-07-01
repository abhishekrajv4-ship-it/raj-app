package com.rajeducational.erp.ui.admin

import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.firestore.FirebaseFirestore
import com.rajeducational.erp.data.GalleryPhoto
import com.rajeducational.erp.theme.AppColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminGalleryUploadScreen(navController: NavController) {
    val firestore = FirebaseFirestore.getInstance()
    var photos by remember { mutableStateOf<List<GalleryPhoto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isUploading by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        firestore.collection("gallery_photos").addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(GalleryPhoto::class.java)?.copy(id = doc.id)
                }
                photos = list
                isLoading = false
            }
        }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            coroutineScope.launch {
                isUploading = true
                var successCount = 0
                for (uri in uris) {
                    val success = uploadGalleryPhotoToHostinger(uri, context, firestore)
                    if (success) successCount++
                }
                isUploading = false
                if (successCount == uris.size) {
                    Toast.makeText(context, "All $successCount photos uploaded successfully", Toast.LENGTH_SHORT).show()
                } else if (successCount > 0) {
                    Toast.makeText(context, "$successCount/${uris.size} photos uploaded", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Upload failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    var selectedPhotoUrl by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gallery Photo Upload Center", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.Admin,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            if (!isUploading) {
                FloatingActionButton(
                    onClick = { launcher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    containerColor = AppColors.Admin
                ) {
                    Icon(Icons.Default.Upload, "Upload Photo", tint = Color.White)
                }
            } else {
                CircularProgressIndicator(color = AppColors.Admin)
            }
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
            } else if (photos.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No photos uploaded yet.", color = AppColors.TextSecondary)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.padding(8.dp),
                    contentPadding = PaddingValues(4.dp)
                ) {
                    items(photos) { photo ->
                        Card(
                            modifier = Modifier
                                .padding(4.dp)
                                .aspectRatio(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                var url = photo.viewUrl
                                if (!url.startsWith("http")) {
                                    url = "https://rajapp.matavaishnavieducationaltrust.org/$url"
                                }
                                AsyncImage(
                                    model = url,
                                    contentDescription = photo.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clickable {
                                            selectedPhotoUrl = url
                                        }
                                )
                                IconButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            val success = deleteFileFromHostinger(photo.deleteUrl)
                                            if (success) {
                                                firestore.collection("gallery_photos").document(photo.id).delete()
                                            } else {
                                                Toast.makeText(context, "Delete failed", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(bottomStart = 8.dp))
                                        .size(32.dp)
                                ) {
                                    Icon(Icons.Default.Delete, "Delete", tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
        
        if (selectedPhotoUrl != null) {
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { selectedPhotoUrl = null },
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                    AsyncImage(
                        model = selectedPhotoUrl,
                        contentDescription = "Full Screen Photo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                    IconButton(
                        onClick = { selectedPhotoUrl = null },
                        modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).background(Color.Black.copy(alpha=0.5f), androidx.compose.foundation.shape.CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }
            }
        }
    }
}

private suspend fun uploadGalleryPhotoToHostinger(
    uri: Uri,
    context: android.content.Context,
    firestore: FirebaseFirestore
): Boolean {
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
            val fileName = resultName ?: "gallery_photo.jpg"

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
                .addFormDataPart("category", "photos")
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
                        val galleryPhoto = GalleryPhoto(
                            id = UUID.randomUUID().toString(),
                            name = dataObj.optString("original_name"),
                            viewUrl = dataObj.optString("view_url"),
                            deleteUrl = dataObj.optString("delete_url"),
                            fileId = dataObj.optInt("id")
                        )
                        firestore.collection("gallery_photos").document(galleryPhoto.id).set(galleryPhoto).await()
                        true
                    } else {
                        false
                    }
                } else {
                    false
                }
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
