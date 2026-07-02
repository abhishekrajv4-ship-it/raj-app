package com.rajeducational.erp.ui.admin

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.firestore.FirebaseFirestore
import com.rajeducational.erp.theme.AppColors
import kotlinx.coroutines.launch

data class AdminTeacherContact(
    val id: String,
    val name: String,
    val college: String,
    val course: String,
    val session: String,
    val role: String = "Teacher"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminTeacherMessagesScreen(navController: NavController) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val firestore = FirebaseFirestore.getInstance()
    
    var selectedTeacher by remember { mutableStateOf<AdminTeacherContact?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    
    var teachersList by remember { mutableStateOf<List<AdminTeacherContact>>(emptyList()) }
    var staffsList by remember { mutableStateOf<List<AdminTeacherContact>>(emptyList()) }
    var isListLoading by remember { mutableStateOf(true) }
    var allAdminTeacherChats by remember { mutableStateOf<List<AdminChatMessage>>(emptyList()) }
    
    val teacherList = remember(teachersList, staffsList) { teachersList + staffsList }
    
    var chatMessages by remember { mutableStateOf<List<AdminChatMessage>>(emptyList()) }
    var messageText by remember { mutableStateOf("") }
    var attachmentUri by remember { mutableStateOf<Uri?>(null) }
    var attachmentName by remember { mutableStateOf("") }
    var isUploading by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        firestore.collection("teachers")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    teachersList = snapshot.documents.map { doc ->
                        AdminTeacherContact(
                            id = doc.id,
                            name = doc.getString("name") ?: doc.getString("fullName") ?: "Unknown Teacher",
                            college = doc.getString("collegeName") ?: doc.getString("college") ?: "N/A",
                            course = (doc.get("courses") as? List<*>)?.joinToString(", ") ?: doc.getString("course") ?: doc.getString("departmentName") ?: doc.getString("department") ?: "N/A",
                            session = (doc.get("years") as? List<*>)?.joinToString(", ") ?: doc.getString("designation") ?: "N/A",
                            role = "Teacher"
                        )
                    }
                }
                isListLoading = false
            }
        firestore.collection("staffs")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    staffsList = snapshot.documents.map { doc ->
                        AdminTeacherContact(
                            id = doc.id,
                            name = doc.getString("name") ?: doc.getString("fullName") ?: "Unknown Staff",
                            college = doc.getString("collegeName") ?: doc.getString("college") ?: "N/A",
                            course = doc.getString("departmentName") ?: (doc.get("courses") as? List<*>)?.joinToString(", ") ?: doc.getString("course") ?: doc.getString("department") ?: "N/A",
                            session = (doc.get("years") as? List<*>)?.joinToString(", ") ?: doc.getString("designation") ?: "N/A",
                            role = "Staff"
                        )
                    }
                }
            }
    }

    LaunchedEffect(Unit) {
        firestore.collection("teacher_chats")
            .whereEqualTo("adminId", "admin")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    allAdminTeacherChats = snapshot.documents.mapNotNull { doc ->
                        AdminChatMessage(
                            id = doc.id,
                            studentId = doc.getString("teacherId") ?: "",
                            studentName = doc.getString("teacherName") ?: "",
                            teacherId = doc.getString("adminId") ?: "",
                            teacherName = doc.getString("adminName") ?: "",
                            senderId = doc.getString("senderId") ?: "",
                            senderName = doc.getString("senderName") ?: "",
                            message = doc.getString("message") ?: "",
                            attachmentUrl = doc.getString("attachmentUrl") ?: "",
                            attachmentName = doc.getString("attachmentName") ?: "",
                            timestamp = doc.getLong("timestamp") ?: 0L,
                            isRead = doc.getBoolean("isRead") ?: false
                        )
                    }
                }
            }
    }
    
    DisposableEffect(selectedTeacher) {
        if (selectedTeacher != null) {
            val listener = firestore.collection("teacher_chats")
                .whereEqualTo("teacherId", selectedTeacher!!.id)
                .whereEqualTo("adminId", "admin")
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        for (doc in snapshot.documents) {
                            val senderId = doc.getString("senderId") ?: ""
                            val isRead = doc.getBoolean("isRead") ?: false
                            if (senderId != "admin" && !isRead) {
                                doc.reference.update("isRead", true)
                            }
                        }
                        chatMessages = snapshot.documents.mapNotNull { doc ->
                            AdminChatMessage(
                                id = doc.id,
                                studentId = doc.getString("teacherId") ?: "",
                                studentName = doc.getString("teacherName") ?: "",
                                teacherId = doc.getString("adminId") ?: "",
                                teacherName = doc.getString("adminName") ?: "",
                                senderId = doc.getString("senderId") ?: "",
                                senderName = doc.getString("senderName") ?: "",
                                message = doc.getString("message") ?: "",
                                attachmentUrl = doc.getString("attachmentUrl") ?: "",
                                attachmentName = doc.getString("attachmentName") ?: "",
                                timestamp = doc.getLong("timestamp") ?: 0L,
                                isRead = doc.getBoolean("isRead") ?: false
                            )
                        }.sortedBy { it.timestamp }
                    }
                }
            onDispose {
                listener.remove()
            }
        } else {
            chatMessages = emptyList()
            onDispose {}
        }
    }
    
    val filteredTeachers = remember(searchQuery, teacherList, allAdminTeacherChats) {
        val list = teacherList.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.college.contains(searchQuery, ignoreCase = true) ||
            it.course.contains(searchQuery, ignoreCase = true)
        }
        list.sortedByDescending { teacher ->
            allAdminTeacherChats
                .filter { it.studentId == teacher.id }
                .maxOfOrNull { it.timestamp } ?: 0L
        }
    }
    
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            attachmentUri = uri
            val contentResolver = context.contentResolver
            var name = "attachment"
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        name = it.getString(index)
                    }
                }
            }
            attachmentName = name
        }
    }
    
    fun sendMessage(text: String, fileUrl: String = "", fileName: String = "") {
        if (text.trim().isEmpty() && fileUrl.isEmpty()) return
        if (selectedTeacher == null) return
        
        val msgData = hashMapOf(
            "teacherId" to selectedTeacher!!.id,
            "teacherName" to selectedTeacher!!.name,
            "adminId" to "admin",
            "adminName" to "Admin",
            "senderId" to "admin",
            "senderName" to "Admin",
            "message" to text,
            "attachmentUrl" to fileUrl,
            "attachmentName" to fileName,
            "timestamp" to System.currentTimeMillis(),
            "isRead" to false
        )
        
        firestore.collection("teacher_chats")
            .add(msgData)
            .addOnSuccessListener {
                messageText = ""
                attachmentUri = null
                attachmentName = ""
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to send message", Toast.LENGTH_SHORT).show()
            }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = selectedTeacher?.name ?: "Teacher and Staff Messages",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        if (selectedTeacher != null) {
                            Text(
                                text = "Role: ${selectedTeacher!!.role} | Dept: ${selectedTeacher!!.course}",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectedTeacher != null) {
                            selectedTeacher = null
                        } else {
                            navController.popBackStack()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
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
        if (selectedTeacher == null) {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .background(AppColors.Background)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by Teacher/Staff, College or Dept...", color = AppColors.TextSecondary) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = AppColors.TextSecondary) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = AppColors.TextSecondary)
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = AppColors.Admin,
                        unfocusedBorderColor = Color.LightGray
                    )
                )
                
                if (isListLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AppColors.Admin)
                    }
                } else if (filteredTeachers.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No records found.", color = AppColors.TextSecondary)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredTeachers.size) { index ->
                            val teacher = filteredTeachers[index]
                            val isStaff = teacher.role == "Staff"
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedTeacher = teacher },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .background(
                                                color = if (isStaff) AppColors.Staff.copy(alpha = 0.15f) else AppColors.Admin.copy(alpha = 0.15f),
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = if (isStaff) AppColors.Staff else AppColors.Admin
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.width(16.dp))
                                    
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                            Text(
                                                text = teacher.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = AppColors.Navy
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                color = if (isStaff) AppColors.Staff.copy(alpha = 0.1f) else AppColors.Admin.copy(alpha = 0.1f),
                                                contentColor = if (isStaff) AppColors.Staff else AppColors.Admin,
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = teacher.role,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                            
                                            val unreadCount = allAdminTeacherChats.count { it.studentId == teacher.id && it.senderId == teacher.id && !it.isRead }
                                            if (unreadCount > 0) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .background(Color.Red, CircleShape)
                                                )
                                            }
                                            
                                            Spacer(modifier = Modifier.weight(1f))
                                            
                                            val latestMessage = allAdminTeacherChats.filter { it.studentId == teacher.id }.maxByOrNull { it.timestamp }
                                            if (latestMessage != null) {
                                                Text(
                                                    text = java.text.SimpleDateFormat("dd MMM hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(latestMessage.timestamp)),
                                                    fontSize = 10.sp,
                                                    color = AppColors.TextSecondary
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "College: ${teacher.college} | Dept: ${teacher.course}",
                                            fontSize = 12.sp,
                                            color = AppColors.TextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = Color.LightGray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            val listState = androidx.compose.foundation.lazy.rememberLazyListState()
            
            LaunchedEffect(chatMessages.size) {
                if (chatMessages.isNotEmpty()) {
                    listState.animateScrollToItem(chatMessages.size - 1)
                }
            }
            
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .background(Color(0xFFF5F7FA))
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (chatMessages.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No message history. Send a reply to initiate chat.",
                                        fontSize = 14.sp,
                                        color = AppColors.TextSecondary,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            items(chatMessages.size) { idx ->
                                val msg = chatMessages[idx]
                                val isMe = msg.senderId == "admin"
                                
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
                                ) {
                                    Column(
                                        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
                                        modifier = Modifier.widthIn(max = 280.dp)
                                    ) {
                                        if (!isMe) {
                                            Text(
                                                text = msg.senderName,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = AppColors.TextSecondary,
                                                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                                            )
                                        }
                                        
                                        Card(
                                            shape = RoundedCornerShape(
                                                topStart = 12.dp,
                                                topEnd = 12.dp,
                                                bottomStart = if (isMe) 12.dp else 0.dp,
                                                bottomEnd = if (isMe) 0.dp else 12.dp
                                            ),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isMe) AppColors.Admin else Color.White
                                            ),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                if (msg.message.isNotEmpty()) {
                                                    Text(
                                                        text = msg.message,
                                                        fontSize = 14.sp,
                                                        color = if (isMe) Color.White else AppColors.TextPrimary
                                                    )
                                                }
                                                
                                                if (msg.attachmentUrl.isNotEmpty()) {
                                                    if (msg.message.isNotEmpty()) {
                                                        Spacer(modifier = Modifier.height(8.dp))
                                                    }
                                                    
                                                    val isImage = msg.attachmentName.lowercase().let { name ->
                                                        name.endsWith(".jpg") || name.endsWith(".jpeg") ||
                                                        name.endsWith(".png") || name.endsWith(".gif") || name.endsWith(".webp")
                                                    }
                                                    
                                                    val fullUrl = if (msg.attachmentUrl.startsWith("http")) msg.attachmentUrl else "https://rajapp.matavaishnavieducationaltrust.org/${msg.attachmentUrl}"
                                                    
                                                    if (isImage) {
                                                        AsyncImage(
                                                            model = fullUrl,
                                                            contentDescription = "Attachment Image",
                                                            contentScale = ContentScale.Crop,
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .heightIn(max = 180.dp)
                                                                .clip(RoundedCornerShape(8.dp))
                                                                .background(Color.DarkGray)
                                                                .clickable {
                                                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(fullUrl))
                                                                    context.startActivity(intent)
                                                                }
                                                        )
                                                    } else {
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .background(
                                                                    if (isMe) Color.White.copy(alpha = 0.15f) else Color(0xFFF0F2F5),
                                                                    RoundedCornerShape(6.dp)
                                                                )
                                                                .clickable {
                                                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(fullUrl))
                                                                    context.startActivity(intent)
                                                                }
                                                                .padding(8.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.InsertDriveFile,
                                                                contentDescription = null,
                                                                tint = if (isMe) Color.White else AppColors.Admin,
                                                                modifier = Modifier.size(32.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                            Column(modifier = Modifier.weight(1f)) {
                                                                Text(
                                                                    text = msg.attachmentName,
                                                                    fontSize = 12.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = if (isMe) Color.White else AppColors.Navy,
                                                                    maxLines = 1,
                                                                    overflow = TextOverflow.Ellipsis
                                                                )
                                                                Text(
                                                                    text = "Document File",
                                                                    fontSize = 10.sp,
                                                                    color = if (isMe) Color.White.copy(alpha = 0.7f) else AppColors.TextSecondary
                                                                )
                                                            }
                                                        }
                                                    }
                                                    
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    
                                                    TextButton(
                                                        onClick = {
                                                            try {
                                                                val request = DownloadManager.Request(Uri.parse(fullUrl))
                                                                    .setTitle(msg.attachmentName)
                                                                    .setDescription("Downloading file attachment...")
                                                                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                                                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, msg.attachmentName)
                                                                    .setAllowedOverMetered(true)
                                                                    .setAllowedOverRoaming(true)
                                                                val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                                                                dm.enqueue(request)
                                                                Toast.makeText(context, "Download started...", Toast.LENGTH_SHORT).show()
                                                            } catch (e: Exception) {
                                                                Toast.makeText(context, "Download failed", Toast.LENGTH_SHORT).show()
                                                            }
                                                        },
                                                        colors = ButtonDefaults.textButtonColors(contentColor = if (isMe) Color.White else AppColors.Admin),
                                                        contentPadding = PaddingValues(0.dp),
                                                        modifier = Modifier.height(30.dp)
                                                    ) {
                                                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Download", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                        
                                        val formattedTime = remember(msg.timestamp) {
                                            try {
                                                val sdf = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
                                                sdf.format(java.util.Date(msg.timestamp))
                                            } catch (e: Exception) {
                                                ""
                                            }
                                        }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp)
                                        ) {
                                            Text(
                                                text = formattedTime,
                                                fontSize = 9.sp,
                                                color = AppColors.TextSecondary
                                            )
                                            if (isMe) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                if (msg.isRead) {
                                                    Icon(
                                                        imageVector = Icons.Default.DoneAll,
                                                        contentDescription = "Seen",
                                                        tint = Color(0xFF2196F3),
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                } else {
                                                    Icon(
                                                        imageVector = Icons.Default.Done,
                                                        contentDescription = "Sent",
                                                        tint = Color.Gray,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.White,
                        tonalElevation = 4.dp
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            if (attachmentUri != null) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFFFEBEE))
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.AttachFile, contentDescription = null, tint = AppColors.Admin)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = attachmentName,
                                        fontSize = 13.sp,
                                        color = AppColors.Navy,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = {
                                            attachmentUri = null
                                            attachmentName = ""
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.Gray, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { filePickerLauncher.launch("*/*") }
                                ) {
                                    Icon(Icons.Default.AddCircleOutline, contentDescription = "Add attachment", tint = AppColors.Admin)
                                }
                                
                                OutlinedTextField(
                                    value = messageText,
                                    onValueChange = { messageText = it },
                                    placeholder = { Text("Write a reply to teacher...") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(24.dp),
                                    maxLines = 4,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color(0xFFF9FAFB),
                                        unfocusedContainerColor = Color(0xFFF9FAFB),
                                        focusedBorderColor = AppColors.Admin,
                                        unfocusedBorderColor = Color.LightGray
                                    )
                                )
                                
                                Spacer(modifier = Modifier.width(4.dp))
                                
                                if (isUploading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(36.dp).padding(6.dp),
                                        color = AppColors.Admin,
                                        strokeWidth = 3.dp
                                    )
                                } else {
                                    IconButton(
                                        onClick = {
                                            val textToSend = messageText
                                            if (attachmentUri != null) {
                                                isUploading = true
                                                coroutineScope.launch {
                                                    val photo = uploadEventPhotoToHostinger(attachmentUri!!, context)
                                                    if (photo != null) {
                                                        sendMessage(textToSend, photo.viewUrl, photo.name)
                                                    } else {
                                                        Toast.makeText(context, "Attachment upload failed.", Toast.LENGTH_SHORT).show()
                                                    }
                                                    isUploading = false
                                                }
                                            } else {
                                                sendMessage(textToSend)
                                            }
                                        },
                                        enabled = messageText.trim().isNotEmpty() || attachmentUri != null
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Send,
                                            contentDescription = "Send",
                                            tint = if (messageText.trim().isNotEmpty() || attachmentUri != null) AppColors.Admin else Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
