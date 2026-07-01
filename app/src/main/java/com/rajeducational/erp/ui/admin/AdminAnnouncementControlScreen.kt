package com.rajeducational.erp.ui.admin

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.google.firebase.firestore.Query
import com.rajeducational.erp.data.Announcement
import com.rajeducational.erp.theme.AppColors
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAnnouncementControlScreen(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val firestore = FirebaseFirestore.getInstance()

    var subject by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var attachmentUri by remember { mutableStateOf<Uri?>(null) }
    var externalUrl by remember { mutableStateOf("") }
    var isUploading by remember { mutableStateOf(false) }

    var announcements by remember { mutableStateOf<List<Announcement>>(emptyList()) }

    LaunchedEffect(Unit) {
        firestore.collection("announcements")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    announcements = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Announcement::class.java)?.copy(id = doc.id)
                    }
                }
            }
    }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        attachmentUri = uri
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Announcement Control", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Admin)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Create Announcement", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppColors.Navy)
                Spacer(modifier = Modifier.height(16.dp))
    
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Subject") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
    
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = externalUrl,
                    onValueChange = { externalUrl = it },
                    label = { Text("URL Link (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
    
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = { filePicker.launch("*/*") }, colors = ButtonDefaults.buttonColors(containerColor = AppColors.Admin)) {
                        Text("Select Attachment")
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    if (attachmentUri != null) {
                        Text("File Selected", color = Color.Green)
                    }
                }
    
                Spacer(modifier = Modifier.height(16.dp))
    
                Button(
                    onClick = {
                        coroutineScope.launch {
                            if (subject.isNotBlank() && description.isNotBlank()) {
                                isUploading = true
                                try {
                                    var uploadedUrl = ""
                                    if (attachmentUri != null) {
                                        val photo = uploadEventPhotoToHostinger(attachmentUri!!, context)
                                        if (photo != null) {
                                            uploadedUrl = "https://rajapp.matavaishnavieducationaltrust.org/${photo.viewUrl}"
                                        }
                                    }
                                    val newAnnouncement = Announcement(
                                        subject = subject,
                                        description = description,
                                        attachmentUrl = uploadedUrl,
                                        url = externalUrl,
                                        timestamp = System.currentTimeMillis(),
                                        senderName = "Admin",
                                        senderRole = "Admin"
                                    )
                                    firestore.collection("announcements").add(newAnnouncement).await()
                                    subject = ""
                                    description = ""
                                    externalUrl = ""
                                    attachmentUri = null
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                } finally {
                                    isUploading = false
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Admin),
                    enabled = !isUploading
                ) {
                    Text(if (isUploading) "Publishing..." else "Publish Announcement")
                }
    
                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Past Announcements", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppColors.Navy)
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(announcements) { announcement ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(announcement.subject, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            val formatter = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault())
                            val dateString = formatter.format(java.util.Date(announcement.timestamp))
                            val sender = if (announcement.senderName.isNotEmpty()) announcement.senderName else "Admin"
                            Text("By $sender | $dateString", fontSize = 12.sp, color = AppColors.Admin)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(announcement.description, fontSize = 14.sp, color = AppColors.TextSecondary, maxLines = 2)
                        }
                        IconButton(onClick = {
                            firestore.collection("announcements").document(announcement.id).delete()
                        }) {
                            Icon(Icons.Default.Delete, "Delete", tint = Color.Red)
                        }
                    }
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
