package com.rajeducational.erp.ui.admin

import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
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
import com.rajeducational.erp.data.Event
import com.rajeducational.erp.data.GalleryPhoto
import com.rajeducational.erp.theme.AppColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminEventControlScreen(navController: NavController) {
    var events by remember { mutableStateOf<List<Event>>(emptyList()) }
    val firestore = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var showAddDialog by remember { mutableStateOf(false) }
    var isUploading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        firestore.collection("events").addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                events = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Event::class.java)?.copy(id = doc.id)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Event Control Center", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
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
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = AppColors.Admin,
                contentColor = Color.White
            ) {
                Text("Add Event")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().background(AppColors.Background)) {
            if (events.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No events found.", color = AppColors.TextSecondary)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
                    items(events) { event ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(event.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AppColors.Navy)
                                Text("${event.date} ${event.month} | ${event.place}", fontSize = 14.sp, color = AppColors.TextSecondary)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(event.description, fontSize = 14.sp, color = AppColors.TextPrimary)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Photos: ${event.photos.size}", fontSize = 12.sp, color = AppColors.TextSecondary)
                                
                                Button(
                                    onClick = {
                                        firestore.collection("events").document(event.id).delete()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                    modifier = Modifier.align(Alignment.End).padding(top = 8.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Delete")
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showAddDialog) {
            AddEventDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { name, date, month, place, desc, uris ->
                    coroutineScope.launch {
                        isUploading = true
                        val uploadedPhotos = mutableListOf<GalleryPhoto>()
                        for (uri in uris) {
                            val photo = uploadEventPhotoToHostinger(uri, context)
                            if (photo != null) {
                                uploadedPhotos.add(photo)
                                // Add to gallery_photos collection
                                firestore.collection("gallery_photos").document(photo.id).set(photo).await()
                            }
                        }
                        
                        val newEvent = Event(
                            id = UUID.randomUUID().toString(),
                            name = name,
                            date = date,
                            month = month,
                            place = place,
                            description = desc,
                            photos = uploadedPhotos
                        )
                        firestore.collection("events").document(newEvent.id).set(newEvent).await()
                        isUploading = false
                        showAddDialog = false
                        Toast.makeText(context, "Event added successfully", Toast.LENGTH_SHORT).show()
                    }
                },
                isUploading = isUploading
            )
        }
    }
}

@Composable
fun AddEventDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, String, String, String, List<Uri>) -> Unit,
    isUploading: Boolean
) {
    var name by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var month by remember { mutableStateOf("") }
    var place by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
        if (uris.isNotEmpty()) {
            selectedUris = uris
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isUploading) onDismiss() },
        title = { Text("Add New Event") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Event Name") }, modifier = Modifier.fillMaxWidth())
                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Date (e.g. 15)") }, modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(value = month, onValueChange = { month = it }, label = { Text("Month (e.g. Jul)") }, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = place, onValueChange = { place = it }, label = { Text("Place") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), minLines = 3)
                
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { launcher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                    Text(if (selectedUris.isEmpty()) "Select Photos" else "${selectedUris.size} Photos Selected")
                }
                if (isUploading) {
                    Spacer(modifier = Modifier.height(16.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text("Uploading...", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAdd(name, date, month, place, description, selectedUris) },
                enabled = name.isNotBlank() && date.isNotBlank() && !isUploading
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isUploading) { Text("Cancel") }
        }
    )
}

suspend fun uploadEventPhotoToHostinger(
    uri: Uri,
    context: android.content.Context
): GalleryPhoto? {
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
            val fileName = resultName ?: "event_photo.jpg"

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
                .addFormDataPart("category", "events")
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
                        return@withContext GalleryPhoto(
                            id = UUID.randomUUID().toString(),
                            name = dataObj.optString("original_name"),
                            viewUrl = dataObj.optString("view_url"),
                            deleteUrl = dataObj.optString("delete_url"),
                            fileId = dataObj.optInt("id")
                        )
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
