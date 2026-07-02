package com.rajeducational.erp.ui.student

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp



import androidx.navigation.NavController
import com.rajeducational.erp.theme.AppColors
import com.rajeducational.erp.ui.components.AttendancePercentageBadge
import com.rajeducational.erp.ui.components.AttendanceStatsCard
import kotlinx.coroutines.launch

// ===== STUDENT PROFILE =====
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentProfileScreen(navController: NavController) {
    Scaffold(topBar = { TopAppBar(title = { Text("My Profile") }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, "Back") } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = getStudentThemeColor(), titleContentColor = Color.White, navigationIconContentColor = Color.White)) }) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().background(AppColors.Background).verticalScroll(rememberScrollState()).padding(16.dp)) {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.size(64.dp).clip(CircleShape).background(getStudentThemeColor()), contentAlignment = Alignment.Center) { Text("T", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold) }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Test Student", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AppColors.Navy)
                    Text("RNI-2024-001", fontSize = 13.sp, color = AppColors.TextSecondary)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    listOf("College" to "Raj Nursing Institute", "Course" to "GNM", "Session" to "2024-25", "Email" to "test@test.com", "Mobile" to "9876543210", "Gender" to "Male", "DOB" to "2000-01-01", "State" to "Bihar").forEach { (k, v) ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(k, fontSize = 13.sp, color = AppColors.TextSecondary)
                            Text(v, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AppColors.TextPrimary)
                        }
                        HorizontalDivider(color = Color(0xFFF0F0F0))
                    }
                }
            }
        }
    }
}

// ===== STUDENT FEES =====
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentFeesScreen(navController: NavController) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
    
    val sharedPrefs = context.getSharedPreferences("StudentPrefs", android.content.Context.MODE_PRIVATE)
    val studentId = sharedPrefs.getString("student_id", null)
    
    var studentProfile by remember { mutableStateOf<Map<String, Any>?>(null) }
    
    LaunchedEffect(studentId) {
        if (studentId != null) {
            firestore.collection("students").document(studentId!!).addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    studentProfile = snapshot.data
                }
            }
        }
    }

    val hasFeeReminder = studentProfile?.get("hasFeeReminder") as? Boolean ?: false
    val feeReminderExpiry = studentProfile?.get("feeReminderExpiry") as? Long ?: 0L
    val feeReminderText = studentProfile?.get("feeReminderText") as? String ?: ""
    val isFeeReminderActive = hasFeeReminder && System.currentTimeMillis() < feeReminderExpiry

    Scaffold(topBar = { 
        TopAppBar(
            title = { Text("Fee Reminder") }, 
            colors = TopAppBarDefaults.topAppBarColors(containerColor = getStudentThemeColor(), titleContentColor = Color.White),
            navigationIcon = {
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                }
            }
        ) 
    }) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().background(AppColors.Background).verticalScroll(rememberScrollState()).padding(16.dp)) {
            if (isFeeReminderActive) {
                // Fee Reminder
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFCE4EC)), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, "Pending", tint = AppColors.Error)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Fees Pending", color = AppColors.Error, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(feeReminderText, color = AppColors.Navy, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        val dateFormat = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault())
                        Text("Due Date: ${dateFormat.format(java.util.Date(feeReminderExpiry))}", color = AppColors.Error, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, "Clear", tint = Color(0xFF4CAF50))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("No pending fees or reminders.", color = Color(0xFF388E3C), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            // Fee document image placeholder
            Card(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Description, "No doc", tint = Color.LightGray, modifier = Modifier.size(48.dp))
                    Text("Fee document not yet published", color = AppColors.TextSecondary)
                }
            }
        }
    }
}

// ===== STUDENT RATINGS =====
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentRatingsScreen(navController: NavController) {
    Scaffold(topBar = { TopAppBar(title = { Text("Rate Teachers") }, colors = TopAppBarDefaults.topAppBarColors(containerColor = getStudentThemeColor(), titleContentColor = Color.White)) }) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().background(AppColors.Background).verticalScroll(rememberScrollState())) {
            Text("Your ratings are anonymous. Teachers cannot see who rated them.", fontSize = 12.sp, color = AppColors.TextSecondary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(16.dp))
            // Teacher rating cards
            listOf("Dr. Test Teacher" to "Nursing", "Prof. Kumar" to "Pharmacy").forEach { (name, dept) ->
                var rating by remember { mutableIntStateOf(0) }
                var feedback by remember { mutableStateOf("") }
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(getStudentThemeColor()), contentAlignment = Alignment.Center) { Text(name.first().toString(), color = Color.White, fontWeight = FontWeight.Bold) }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column { Text(name, fontWeight = FontWeight.SemiBold); Text("$dept", fontSize = 12.sp, color = AppColors.TextSecondary) }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row { (1..5).forEach { i -> IconButton(onClick = { rating = i }) { Icon(if (i <= rating) Icons.Default.Star else Icons.Default.StarOutline, "Star", tint = AppColors.StarYellow, modifier = Modifier.size(32.dp)) } } }
                        OutlinedTextField(value = feedback, onValueChange = { feedback = it }, label = { Text("Optional feedback (anonymous)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), maxLines = 3)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = {}, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = getStudentThemeColor()), shape = RoundedCornerShape(10.dp)) { Text("Submit Rating") }
                    }
                }
            }
        }
    }
}

// ===== STUDENT GALLERY =====
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentGalleryScreen(navController: NavController) {
    val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
    var photos by remember { mutableStateOf<List<com.rajeducational.erp.data.GalleryPhoto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(Unit) {
        firestore.collection("gallery_photos").addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(com.rajeducational.erp.data.GalleryPhoto::class.java)?.copy(id = doc.id)
                }
                photos = list
                isLoading = false
            }
        }
    }

    var selectedPhotoIndex by remember { mutableStateOf<Int?>(null) }
    
    // Broadcast receiver for download completion
    var downloadCompleteMessage by remember { mutableStateOf<String?>(null) }
    DisposableEffect(context) {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
                if (intent?.action == android.app.DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
                    downloadCompleteMessage = "Photo downloaded in gallery"
                }
            }
        }
        androidx.core.content.ContextCompat.registerReceiver(context, receiver, android.content.IntentFilter(android.app.DownloadManager.ACTION_DOWNLOAD_COMPLETE), androidx.core.content.ContextCompat.RECEIVER_EXPORTED)
        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gallery") },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = getStudentThemeColor(), titleContentColor = Color.White, navigationIconContentColor = Color.White)
            )
        },
        snackbarHost = {
            if (downloadCompleteMessage != null) {
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    containerColor = Color(0xFF4CAF50),
                    contentColor = Color.White
                ) {
                    Text(downloadCompleteMessage!!)
                }
                LaunchedEffect(downloadCompleteMessage) {
                    kotlinx.coroutines.delay(3000)
                    downloadCompleteMessage = null
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().background(AppColors.Background)) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = getStudentThemeColor())
                }
            } else if (photos.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Image, "No photos", tint = Color.LightGray, modifier = Modifier.size(48.dp))
                        Text("No photos available", color = AppColors.TextSecondary)
                    }
                }
            } else {
                androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                    columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(3),
                    modifier = Modifier.padding(8.dp),
                    contentPadding = PaddingValues(4.dp)
                ) {
                    items(photos.size) { index ->
                        val photo = photos[index]
                        Card(
                            modifier = Modifier
                                .padding(4.dp)
                                .aspectRatio(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            var url = photo.viewUrl
                            if (!url.startsWith("http")) {
                                url = "https://rajapp.matavaishnavieducationaltrust.org/$url"
                            }
                            coil.compose.AsyncImage(
                                model = url,
                                contentDescription = photo.name,
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable { selectedPhotoIndex = index }
                            )
                        }
                    }
                }
            }
        }
        
        if (selectedPhotoIndex != null) {
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { selectedPhotoIndex = null },
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
            ) {
                val pagerState = androidx.compose.foundation.pager.rememberPagerState(
                    initialPage = selectedPhotoIndex!!,
                    pageCount = { photos.size }
                )
                
                Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                    androidx.compose.foundation.pager.HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        val photo = photos[page]
                        val url = if (photo.viewUrl.startsWith("http")) photo.viewUrl else "https://rajapp.matavaishnavieducationaltrust.org/${photo.viewUrl}"

                        Box(modifier = Modifier.fillMaxSize()) {
                            coil.compose.AsyncImage(
                                model = url,
                                contentDescription = "Full Screen Photo",
                                contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    
                    IconButton(
                        onClick = { selectedPhotoIndex = null },
                        modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).background(Color.Black.copy(alpha=0.5f), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                    
                    Button(
                        onClick = {
                            val photo = photos[pagerState.currentPage]
                            val url = if (photo.viewUrl.startsWith("http")) photo.viewUrl else "https://rajapp.matavaishnavieducationaltrust.org/${photo.viewUrl}"
                            val request = android.app.DownloadManager.Request(android.net.Uri.parse(url))
                                .setTitle(photo.name)
                                .setDescription("Downloading photo...")
                                .setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                .setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS, photo.name)
                                .setAllowedOverMetered(true)
                                .setAllowedOverRoaming(true)
                            
                            val downloadManager = context.getSystemService(android.content.Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
                            downloadManager.enqueue(request)
                            android.widget.Toast.makeText(context, "Download started...", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = getStudentThemeColor()),
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = "Download")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Download")
                    }
                }
            }
        }
    }
}

// ===== STUDENT VOTING =====
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentVotingScreen(navController: NavController) {
    com.rajeducational.erp.ui.common.VotingScreen(navController, "student")
}

// ===== DATA CLASSES FOR MESSAGING =====
data class ChatContact(
    val id: String,
    val name: String,
    val detail: String,
    val isTeacher: Boolean,
    val college: String = "",
    val course: String = "",
    val isStaff: Boolean = false,
    val role: String = "Teacher"
)

data class StudentChatMessage(
    val id: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val teacherId: String = "",
    val teacherName: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val message: String = "",
    val attachmentUrl: String = "",
    val attachmentName: String = "",
    val timestamp: Long = 0L,
    val isRead: Boolean = false
)

// ===== STUDENT MESSAGES =====
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentMessagesScreen(navController: NavController) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Get logged in student ID
    val sharedPrefs = context.getSharedPreferences("StudentPrefs", android.content.Context.MODE_PRIVATE)
    val studentId = sharedPrefs.getString("student_id", "") ?: ""
    
    var studentName by remember { mutableStateOf("Student") }
    var selectedContact by remember { mutableStateOf<ChatContact?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    
    // Contacts Loading
    var teachersList by remember { mutableStateOf<List<ChatContact>>(emptyList()) }
    var isContactsLoading by remember { mutableStateOf(true) }
    var allStudentChats by remember { mutableStateOf<List<StudentChatMessage>>(emptyList()) }
    
    // Messaging State
    var chatMessages by remember { mutableStateOf<List<StudentChatMessage>>(emptyList()) }
    var messageText by remember { mutableStateOf("") }
    var attachmentUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var attachmentName by remember { mutableStateOf("") }
    var isUploading by remember { mutableStateOf(false) }
    
    val savedContactId = navController.previousBackStackEntry?.savedStateHandle?.get<String>("contactId")
    
    // Fetch Student Name
    LaunchedEffect(studentId) {
        if (studentId.isNotEmpty()) {
            com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("students").document(studentId)
                .get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot != null && snapshot.exists()) {
                        studentName = snapshot.getString("fullName") ?: "Student"
                    }
                }
        }
    }

    // Load all student chats in real-time for sorting & red dots
    LaunchedEffect(studentId) {
        if (studentId.isNotEmpty()) {
            com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("student_chats")
                .whereEqualTo("studentId", studentId)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        allStudentChats = snapshot.documents.mapNotNull { doc ->
                            StudentChatMessage(
                                id = doc.id,
                                studentId = doc.getString("studentId") ?: "",
                                studentName = doc.getString("studentName") ?: "",
                                teacherId = doc.getString("teacherId") ?: doc.getString("staffId") ?: "",
                                teacherName = doc.getString("teacherName") ?: doc.getString("staffName") ?: "",
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
    }
    
    // Load Teachers and Staffs
    LaunchedEffect(Unit) {
        val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        var loadedTeachers = emptyList<ChatContact>()
        var loadedStaffs = emptyList<ChatContact>()
        
        fun mergeAndSet() {
            teachersList = loadedTeachers + loadedStaffs
            isContactsLoading = false
        }
        
        firestore.collection("teachers")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    loadedTeachers = snapshot.documents.map { doc ->
                        ChatContact(
                            id = doc.id,
                            name = doc.getString("name") ?: "Unknown Teacher",
                            detail = "College: ${doc.getString("collegeName") ?: ""} | Course: ${doc.getString("course") ?: ""}",
                            isTeacher = true,
                            college = doc.getString("collegeName") ?: "",
                            course = doc.getString("course") ?: "",
                            isStaff = false,
                            role = "Teacher"
                        )
                    }
                    mergeAndSet()
                }
            }
            
        firestore.collection("staffs")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    loadedStaffs = snapshot.documents.map { doc ->
                        ChatContact(
                            id = doc.id,
                            name = doc.getString("name") ?: "Unknown Staff",
                            detail = "College: ${doc.getString("collegeName") ?: ""} | Dept: ${doc.getString("departmentName") ?: doc.getString("course") ?: "Staff"}",
                            isTeacher = false,
                            college = doc.getString("collegeName") ?: "",
                            course = doc.getString("course") ?: doc.getString("departmentName") ?: "Staff",
                            isStaff = true,
                            role = "Staff"
                        )
                    }
                    mergeAndSet()
                }
            }
    }
    
    // Real-time Chat listener
    DisposableEffect(studentId, selectedContact) {
        if (studentId.isNotEmpty() && selectedContact != null) {
            val idField = if (selectedContact!!.isStaff) "staffId" to "staffName" else "teacherId" to "teacherName"
            val listener = com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("student_chats")
                .whereEqualTo("studentId", studentId)
                .whereEqualTo(idField.first, selectedContact!!.id)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        // Mark incoming messages as read
                        for (doc in snapshot.documents) {
                            val senderId = doc.getString("senderId") ?: ""
                            val isRead = doc.getBoolean("isRead") ?: false
                            if (senderId != studentId && !isRead) {
                                doc.reference.update("isRead", true)
                            }
                        }
                        chatMessages = snapshot.documents.mapNotNull { doc ->
                            StudentChatMessage(
                                id = doc.id,
                                studentId = doc.getString("studentId") ?: "",
                                studentName = doc.getString("studentName") ?: "",
                                teacherId = doc.getString("teacherId") ?: doc.getString("staffId") ?: "",
                                teacherName = doc.getString("teacherName") ?: doc.getString("staffName") ?: "",
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
    
    // File Picker Launcher
    val filePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            attachmentUri = uri
            val contentResolver = context.contentResolver
            var name = "attachment"
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        name = it.getString(index)
                    }
                }
            }
            attachmentName = name
        }
    }
    
    val adminContact = ChatContact(
        id = "admin",
        name = "Admin",
        detail = "Raj Educational Trust Administrator",
        isTeacher = false,
        isStaff = false,
        role = "Admin"
    )
    
    LaunchedEffect(teachersList, savedContactId) {
        if (savedContactId != null && (teachersList.isNotEmpty() || savedContactId == "admin")) {
            selectedContact = if (savedContactId == "admin") adminContact else teachersList.find { it.id == savedContactId }
            navController.previousBackStackEntry?.savedStateHandle?.remove<String>("contactId")
        }
    }
    
    val filteredContacts = remember(searchQuery, teachersList, allStudentChats) {
        val list = mutableListOf<ChatContact>()
        if (adminContact.name.contains(searchQuery, ignoreCase = true) || adminContact.detail.contains(searchQuery, ignoreCase = true)) {
            list.add(adminContact)
        }
        list.addAll(teachersList.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.college.contains(searchQuery, ignoreCase = true) ||
            it.course.contains(searchQuery, ignoreCase = true)
        })
        
        // Sort descending by the timestamp of the last message in that conversation
        list.sortByDescending { contact ->
            allStudentChats
                .filter { it.teacherId == contact.id }
                .maxOfOrNull { it.timestamp } ?: 0L
        }
        list
    }
    
    // Send Message Handler
    fun sendMessage(text: String, fileUrl: String = "", fileName: String = "") {
        if (text.trim().isEmpty() && fileUrl.isEmpty()) return
        if (selectedContact == null) return
        
        val msgData = hashMapOf<String, Any>(
            "studentId" to studentId,
            "studentName" to studentName,
            "senderId" to studentId,
            "senderName" to studentName,
            "message" to text,
            "attachmentUrl" to fileUrl,
            "attachmentName" to fileName,
            "timestamp" to System.currentTimeMillis(),
            "isRead" to false
        )
        if (selectedContact!!.isStaff) {
            msgData["staffId"] = selectedContact!!.id
            msgData["staffName"] = selectedContact!!.name
        } else {
            msgData["teacherId"] = selectedContact!!.id
            msgData["teacherName"] = selectedContact!!.name
        }
        
        com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("student_chats")
            .add(msgData)
            .addOnSuccessListener {
                messageText = ""
                attachmentUri = null
                attachmentName = ""
            }
            .addOnFailureListener {
                android.widget.Toast.makeText(context, "Failed to send message", android.widget.Toast.LENGTH_SHORT).show()
            }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = selectedContact?.name ?: "Messages",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        if (selectedContact != null) {
                            Text(
                                text = if (selectedContact!!.isTeacher) "Teacher" else "Administrator",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectedContact != null) {
                            selectedContact = null
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
                    containerColor = getStudentThemeColor(),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        if (selectedContact == null) {
            // Contacts List View
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .background(AppColors.Background)
            ) {
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search Teachers or Admin...", color = AppColors.TextSecondary) },
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
                        focusedBorderColor = getStudentThemeColor(),
                        unfocusedBorderColor = Color.LightGray
                    )
                )
                
                if (isContactsLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = getStudentThemeColor())
                    }
                } else if (filteredContacts.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No contacts found.", color = AppColors.TextSecondary)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Header or List
                        items(filteredContacts.size) { index ->
                            val contact = filteredContacts[index]
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedContact = contact },
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
                                    // Contact Avatar Circle
                                    val avatarBg = when {
                                        contact.id == "admin" -> Color(0xFFECEFF1)
                                        contact.isStaff -> AppColors.Staff.copy(alpha = 0.15f)
                                        else -> getStudentThemeColor().copy(alpha = 0.15f)
                                    }
                                    val avatarTint = when {
                                        contact.id == "admin" -> Color(0xFF37474F)
                                        contact.isStaff -> AppColors.Staff
                                        else -> getStudentThemeColor()
                                    }
                                    val avatarIcon = when {
                                        contact.id == "admin" -> Icons.Default.Security
                                        contact.isStaff -> Icons.Default.Person
                                        else -> Icons.Default.Person
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .background(avatarBg, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = avatarIcon,
                                            contentDescription = null,
                                            tint = avatarTint
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.width(16.dp))
                                    
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                            Text(
                                                text = contact.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = AppColors.Navy
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                color = when {
                                                    contact.id == "admin" -> Color(0xFFECEFF1)
                                                    contact.isStaff -> AppColors.Staff.copy(alpha = 0.15f)
                                                    else -> AppColors.Teacher.copy(alpha = 0.15f)
                                                },
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = when {
                                                        contact.id == "admin" -> "Admin"
                                                        contact.isStaff -> "Staff"
                                                        else -> "Teacher"
                                                    },
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = when {
                                                        contact.id == "admin" -> Color(0xFF37474F)
                                                        contact.isStaff -> AppColors.Staff
                                                        else -> AppColors.Teacher
                                                    },
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                            val unreadCount = allStudentChats.count { it.teacherId == contact.id && it.senderId == contact.id && !it.isRead }
                                            if (unreadCount > 0) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .background(Color.Red, CircleShape)
                                                )
                                            }
                                            
                                            Spacer(modifier = Modifier.weight(1f))
                                            
                                            val latestMessage = allStudentChats.filter { it.teacherId == contact.id }.maxByOrNull { it.timestamp }
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
                                            text = contact.detail,
                                            fontSize = 12.sp,
                                            color = AppColors.TextSecondary,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
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
            // Chat Message Thread View
            val listState = androidx.compose.foundation.lazy.rememberLazyListState()
            
            // Auto scroll to latest message when new message is added
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
                    // Message List
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
                                        text = "Send a message to start conversation.",
                                        fontSize = 14.sp,
                                        color = AppColors.TextSecondary,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            items(chatMessages.size) { idx ->
                                val msg = chatMessages[idx]
                                val isMe = msg.senderId == studentId
                                
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
                                ) {
                                    Column(
                                        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
                                        modifier = Modifier.widthIn(max = 280.dp)
                                    ) {
                                        // Sender Name if not me
                                        if (!isMe) {
                                            Text(
                                                text = msg.senderName,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = AppColors.TextSecondary,
                                                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                                            )
                                        }
                                        
                                        // Message bubble
                                        Card(
                                            shape = RoundedCornerShape(
                                                topStart = 12.dp,
                                                topEnd = 12.dp,
                                                bottomStart = if (isMe) 12.dp else 0.dp,
                                                bottomEnd = if (isMe) 0.dp else 12.dp
                                            ),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isMe) getStudentThemeColor() else Color.White
                                            ),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                // Message Text
                                                if (msg.message.isNotEmpty()) {
                                                    Text(
                                                        text = msg.message,
                                                        fontSize = 14.sp,
                                                        color = if (isMe) Color.White else AppColors.TextPrimary
                                                    )
                                                }
                                                
                                                // Attachment Area
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
                                                        coil.compose.AsyncImage(
                                                            model = fullUrl,
                                                            contentDescription = "Attachment Image",
                                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .heightIn(max = 180.dp)
                                                                .clip(RoundedCornerShape(8.dp))
                                                                .background(Color.DarkGray)
                                                                .clickable {
                                                                    // Simple open file
                                                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(fullUrl))
                                                                    context.startActivity(intent)
                                                                }
                                                        )
                                                    } else {
                                                        // Document Box
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .background(
                                                                    if (isMe) Color.White.copy(alpha = 0.15f) else Color(0xFFF0F2F5),
                                                                    RoundedCornerShape(6.dp)
                                                                )
                                                                .clickable {
                                                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(fullUrl))
                                                                    context.startActivity(intent)
                                                                }
                                                                .padding(8.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.InsertDriveFile,
                                                                contentDescription = null,
                                                                tint = if (isMe) Color.White else getStudentThemeColor(),
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
                                                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
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
                                                    
                                                    // Download option
                                                    TextButton(
                                                        onClick = {
                                                            try {
                                                                val request = android.app.DownloadManager.Request(android.net.Uri.parse(fullUrl))
                                                                    .setTitle(msg.attachmentName)
                                                                    .setDescription("Downloading file attachment...")
                                                                    .setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                                                    .setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS, msg.attachmentName)
                                                                    .setAllowedOverMetered(true)
                                                                    .setAllowedOverRoaming(true)
                                                                val dm = context.getSystemService(android.content.Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
                                                                dm.enqueue(request)
                                                                android.widget.Toast.makeText(context, "Download started...", android.widget.Toast.LENGTH_SHORT).show()
                                                            } catch (e: Exception) {
                                                                android.widget.Toast.makeText(context, "Download failed", android.widget.Toast.LENGTH_SHORT).show()
                                                            }
                                                        },
                                                        colors = ButtonDefaults.textButtonColors(contentColor = if (isMe) Color.White else getStudentThemeColor()),
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
                                        
                                        // Timestamp
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
                    
                    // Input Bar
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.White,
                        tonalElevation = 4.dp
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Selected Attachment preview row
                            if (attachmentUri != null) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFE0F2F1))
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.AttachFile, contentDescription = null, tint = getStudentThemeColor())
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = attachmentName,
                                        fontSize = 13.sp,
                                        color = AppColors.Navy,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
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
                                // Attachment Button
                                IconButton(
                                    onClick = { filePickerLauncher.launch("*/*") }
                                ) {
                                    Icon(Icons.Default.AddCircleOutline, contentDescription = "Add attachment", tint = getStudentThemeColor())
                                }
                                
                                // Text Input Field
                                OutlinedTextField(
                                    value = messageText,
                                    onValueChange = { messageText = it },
                                    placeholder = { Text("Write a message...") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(24.dp),
                                    maxLines = 4,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color(0xFFF9FAFB),
                                        unfocusedContainerColor = Color(0xFFF9FAFB),
                                        focusedBorderColor = getStudentThemeColor(),
                                        unfocusedBorderColor = Color.LightGray
                                    )
                                )
                                
                                Spacer(modifier = Modifier.width(4.dp))
                                
                                // Send Button
                                if (isUploading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(36.dp).padding(6.dp),
                                        color = getStudentThemeColor(),
                                        strokeWidth = 3.dp
                                    )
                                } else {
                                    IconButton(
                                        onClick = {
                                            val textToSend = messageText
                                            if (attachmentUri != null) {
                                                isUploading = true
                                                coroutineScope.launch {
                                                    val photo = com.rajeducational.erp.ui.admin.uploadEventPhotoToHostinger(attachmentUri!!, context)
                                                    if (photo != null) {
                                                        sendMessage(textToSend, photo.viewUrl, photo.name)
                                                    } else {
                                                        android.widget.Toast.makeText(context, "Attachment upload failed.", android.widget.Toast.LENGTH_SHORT).show()
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
                                            tint = if (messageText.trim().isNotEmpty() || attachmentUri != null) getStudentThemeColor() else Color.Gray
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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentEventsScreen(navController: NavController) {
    var events by remember { mutableStateOf<List<com.rajeducational.erp.data.Event>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("events").addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                events = snapshot.documents.mapNotNull { doc -> doc.toObject(com.rajeducational.erp.data.Event::class.java)?.copy(id = doc.id) }
                isLoading = false
            }
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Events") }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = getStudentThemeColor(), titleContentColor = Color.White, navigationIconContentColor = Color.White)) }) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().background(AppColors.Background).verticalScroll(rememberScrollState()).padding(16.dp)) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = getStudentThemeColor())
                }
            } else if (events.isEmpty()) {
                Text("No events scheduled.", color = AppColors.TextSecondary)
            } else {
                events.forEach { event ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable {
                            navController.navigate("student_event_detail/${event.id}")
                        }, 
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)), shape = RoundedCornerShape(12.dp)) {
                                Column(modifier = Modifier.padding(12.dp).width(40.dp), horizontalAlignment = Alignment.CenterHorizontally) { 
                                    Text(event.date.take(2), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = getStudentThemeColor()) 
                                    Text(event.month.take(3), fontSize = 12.sp, color = getStudentThemeColor()) 
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) { 
                                Text(event.name, fontWeight = FontWeight.Bold, color = AppColors.Navy, fontSize = 18.sp) 
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(event.place, fontSize = 14.sp, color = AppColors.TextSecondary) 
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("To see the photos click here.", fontSize = 12.sp, color = getStudentThemeColor())
                            }
                            Icon(Icons.Default.ChevronRight, "View Details", tint = Color.LightGray)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentEventDetailScreen(navController: NavController, eventId: String) {
    var event by remember { mutableStateOf<com.rajeducational.erp.data.Event?>(null) }
    var selectedPhotoIndex by remember { mutableStateOf<Int?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    
    LaunchedEffect(eventId) {
        if (eventId.isNotEmpty()) {
            com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("events").document(eventId).addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    event = snapshot.toObject(com.rajeducational.erp.data.Event::class.java)?.copy(id = snapshot.id)
                }
            }
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text(event?.name ?: "Event Details") }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = getStudentThemeColor(), titleContentColor = Color.White, navigationIconContentColor = Color.White)) }) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().background(AppColors.Background).verticalScroll(rememberScrollState())) {
            event?.let { e ->
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(e.name, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = AppColors.Navy)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Event, contentDescription = null, tint = getStudentThemeColor(), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("${e.date} ${e.month}", color = AppColors.TextSecondary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = getStudentThemeColor(), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(e.place, color = AppColors.TextSecondary)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("About Event", fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = AppColors.Navy)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(e.description, color = AppColors.TextPrimary, fontSize = 15.sp, lineHeight = 22.sp)
                    
                    if (e.photos.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text("Event Gallery", fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = AppColors.Navy)
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp, max = 1000.dp), // Using a reasonable max height inside scrollview
                            contentPadding = PaddingValues(top = 8.dp),
                            userScrollEnabled = false
                        ) {
                            items(e.photos.size) { index ->
                                val photo = e.photos[index]
                                Card(
                                    modifier = Modifier.padding(4.dp).aspectRatio(1f).clickable { selectedPhotoIndex = index },
                                    shape = RoundedCornerShape(8.dp),
                                ) {
                                    var url = photo.viewUrl
                                    if (!url.startsWith("http")) {
                                        url = "https://rajapp.matavaishnavieducationaltrust.org/$url"
                                    }
                                    coil.compose.AsyncImage(
                                        model = url,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Full screen image viewer
        if (selectedPhotoIndex != null && event != null) {
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { selectedPhotoIndex = null },
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                    val pagerState = androidx.compose.foundation.pager.rememberPagerState(
                        initialPage = selectedPhotoIndex!!,
                        pageCount = { event!!.photos.size }
                    )
                    
                    androidx.compose.foundation.pager.HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        var url = event!!.photos[page].viewUrl
                        if (!url.startsWith("http")) {
                            url = "https://rajapp.matavaishnavieducationaltrust.org/$url"
                        }
                        coil.compose.AsyncImage(
                            model = url,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit
                        )
                    }
                    
                    IconButton(
                        onClick = { selectedPhotoIndex = null },
                        modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, "Close", tint = Color.White)
                    }
                }
            }
        }
    }
}

// ===== STUDENT NOTICES =====
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentNoticesScreen(navController: NavController) {
    Scaffold(topBar = { TopAppBar(title = { Text("Notices") }, colors = TopAppBarDefaults.topAppBarColors(containerColor = getStudentThemeColor(), titleContentColor = Color.White)) }) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().background(AppColors.Background).verticalScroll(rememberScrollState())) {
            listOf("Exam Schedule Released" to "Check the updated exam timetable", "Holiday Notice" to "College will remain closed on 26th Jan").forEach { (title, body) ->
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(title, fontWeight = FontWeight.Bold, color = AppColors.Navy)
                        Text(body, fontSize = 13.sp, color = AppColors.TextSecondary, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }
    }
}



@Composable
fun getStudentThemeColor(): androidx.compose.ui.graphics.Color {
    val context = androidx.compose.ui.platform.LocalContext.current
    val sharedPrefs = androidx.compose.runtime.remember { context.getSharedPreferences("StudentPrefs", android.content.Context.MODE_PRIVATE) }
    val isAttending = sharedPrefs.getBoolean("is_attending", true)
    return if (isAttending) com.rajeducational.erp.theme.AppColors.Student else androidx.compose.ui.graphics.Color.Red
}
