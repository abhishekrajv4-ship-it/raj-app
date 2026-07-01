package com.rajeducational.erp.ui.student

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.isActive
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.rajeducational.erp.theme.AppColors

@Composable
fun StudentDashboardScreen(navController: NavController) {
    var selectedTab by remember { mutableIntStateOf(0) } // Default to Announce
    var announcements by remember { mutableStateOf<List<com.rajeducational.erp.data.Announcement>>(emptyList()) }
    var studentId by remember { mutableStateOf<String?>(null) }
    var studentProfile by remember { mutableStateOf<Map<String, Any>?>(null) }
    var editFullName by remember { mutableStateOf("") }
    var editPhone by remember { mutableStateOf("") }
    var editEmail by remember { mutableStateOf("") }
    var editAddress by remember { mutableStateOf("") }
    var editCollege by remember { mutableStateOf("") }
    var editCourse by remember { mutableStateOf("") }
    var editSession by remember { mutableStateOf("") }
    
    var colleges by remember { mutableStateOf<List<com.rajeducational.erp.data.College>>(emptyList()) }
    var selectedCollegeObj by remember { mutableStateOf<com.rajeducational.erp.data.College?>(null) }
    var selectedCourseObj by remember { mutableStateOf<com.rajeducational.erp.data.Course?>(null) }
    
    var expandedCollege by remember { mutableStateOf(false) }
    var expandedCourse by remember { mutableStateOf(false) }
    var expandedSession by remember { mutableStateOf(false) }
    
    var selectedPhotoIndex by remember { mutableStateOf<Int?>(null) }
    var unreadMessagesCount by remember { mutableIntStateOf(0) }
    var unreadMessages by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    
    var dashboardVisits by androidx.compose.runtime.saveable.rememberSaveable { mutableIntStateOf(0) }
    var messageFirstSeenVisitStr by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("") }
    var dismissedMessageIdsStr by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("") }
    
    val messageFirstSeenVisit = remember(messageFirstSeenVisitStr) {
        if (messageFirstSeenVisitStr.isEmpty()) emptyMap<String, Int>()
        else messageFirstSeenVisitStr.split(",").associate { 
            val parts = it.split(":")
            parts[0] to parts[1].toInt()
        }
    }
    
    val dismissedMessageIds = remember(dismissedMessageIdsStr) {
        if (dismissedMessageIdsStr.isEmpty()) emptySet<String>()
        else dismissedMessageIdsStr.split(",").toSet()
    }
    
    fun updateFirstSeen(newMap: Map<String, Int>) {
        messageFirstSeenVisitStr = newMap.entries.joinToString(",") { "${it.key}:${it.value}" }
    }
    
    fun addDismissedId(id: String) {
        val newSet = dismissedMessageIds + id
        dismissedMessageIdsStr = newSet.joinToString(",")
    }

    LaunchedEffect(Unit) {
        dashboardVisits++
    }
    
    LaunchedEffect(unreadMessages) {
        val newFirstSeen = messageFirstSeenVisit.toMutableMap()
        var changed = false
        unreadMessages.forEach { msg ->
            val docId = msg["docId"] as? String ?: return@forEach
            if (!newFirstSeen.containsKey(docId)) {
                newFirstSeen[docId] = dashboardVisits
                changed = true
            }
        }
        if (changed) {
            updateFirstSeen(newFirstSeen)
        }
    }
    
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(studentId) {
        if (studentId != null) {
            com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("student_chats")
                .whereEqualTo("studentId", studentId)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        val unreadDocs = snapshot.documents.filter { doc ->
                            val senderId = doc.getString("senderId") ?: ""
                            val isRead = doc.getBoolean("isRead") ?: false
                            senderId != studentId && !isRead
                        }
                        
                        val latestPerSender = unreadDocs.groupBy { it.getString("senderId") ?: "" }
                            .map { (_, docs) ->
                                val latestDoc = docs.maxByOrNull { it.getLong("timestamp") ?: 0L }
                                latestDoc?.data?.plus("docId" to latestDoc.id)
                            }.filterNotNull()
                            
                        unreadMessagesCount = unreadDocs.size
                        unreadMessages = latestPerSender
                    }
                }
        }
    }

    LaunchedEffect(Unit) {
        val sharedPrefs = context.getSharedPreferences("StudentPrefs", android.content.Context.MODE_PRIVATE)
        val sId = sharedPrefs.getString("student_id", null)
        val isLoggedIn = sharedPrefs.getBoolean("is_logged_in", false)
        if (sId == null || !isLoggedIn) {
            navController.navigate("student_auth") {
                popUpTo("landing")
            }
            return@LaunchedEffect
        }
        studentId = sId
        com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("students").document(sId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    studentProfile = snapshot.data
                    editFullName = snapshot.getString("fullName") ?: ""
                    editPhone = snapshot.getString("phone") ?: ""
                    editEmail = snapshot.getString("email") ?: ""
                    editAddress = snapshot.getString("address") ?: ""
                    editCollege = snapshot.getString("college") ?: ""
                    editCourse = snapshot.getString("course") ?: ""
                    editSession = snapshot.getString("session") ?: ""
                }
            }
        
        com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("announcements")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    announcements = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(com.rajeducational.erp.data.Announcement::class.java)?.copy(id = doc.id)
                    }
                }
            }

        com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("colleges").get()
            .addOnSuccessListener { snapshot ->
                if (snapshot != null) {
                    colleges = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(com.rajeducational.erp.data.College::class.java)?.copy(id = doc.id)
                    }
                }
            }
    }

    LaunchedEffect(studentProfile, colleges) {
        if (studentProfile != null && colleges.isNotEmpty()) {
            val collegeName = studentProfile?.get("college")?.toString() ?: ""
            val courseName = studentProfile?.get("course")?.toString() ?: ""
            
            val matchedCollege = colleges.find { it.name.equals(collegeName, ignoreCase = true) }
            selectedCollegeObj = matchedCollege
            
            val matchedCourse = matchedCollege?.courses?.find { it.name.equals(courseName, ignoreCase = true) }
            selectedCourseObj = matchedCourse
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                listOf(
                    "Announce" to Icons.Default.Campaign,
                    "Home" to Icons.Default.Home,
                    "Gallery" to Icons.Default.PhotoLibrary,
                    "Profile" to Icons.Default.Person,
                    "Fees" to Icons.Default.Payment,
                    "More" to Icons.Default.MoreHoriz
                ).forEachIndexed { index, (label, icon) ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { 
                            Box {
                                Icon(icon, label)
                                if (label == "More" && unreadMessagesCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(Color.Red, androidx.compose.foundation.shape.CircleShape)
                                            .align(Alignment.TopEnd)
                                    )
                                }
                            }
                        },
                        label = { Text(label, fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = AppColors.Student, indicatorColor = AppColors.Student.copy(alpha = 0.1f))
                    )
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().background(AppColors.Background).verticalScroll(rememberScrollState())) {
            // Top bar
            Row(modifier = Modifier.fillMaxWidth().background(AppColors.Student).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(if (selectedTab == 0) "Announcements" else "Student Dashboard", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                IconButton(onClick = { 
                    val sharedPrefs = context.getSharedPreferences("StudentPrefs", android.content.Context.MODE_PRIVATE)
                    sharedPrefs.edit().putBoolean("is_logged_in", false).apply()
                    navController.navigate("landing") {
                        popUpTo(0)
                    }
                }) { Icon(Icons.AutoMirrored.Filled.ExitToApp, "Logout", tint = Color.White) }
            }

            val visibleMessages = unreadMessages.filter { msg ->
                val docId = msg["docId"] as? String ?: return@filter false
                val firstSeen = messageFirstSeenVisit[docId] ?: dashboardVisits
                !dismissedMessageIds.contains(docId) && (dashboardVisits - firstSeen) < 2
            }
            
            var showFeeReminderDialog by remember(studentProfile) { mutableStateOf(true) }
            val hasFeeReminder = studentProfile?.get("hasFeeReminder") as? Boolean ?: false
            val feeReminderExpiry = studentProfile?.get("feeReminderExpiry") as? Long ?: 0L
            val feeReminderText = studentProfile?.get("feeReminderText") as? String ?: ""
            val isFeeReminderActive = hasFeeReminder && System.currentTimeMillis() < feeReminderExpiry

            if (selectedTab == 0 && isFeeReminderActive && showFeeReminderDialog) {
                androidx.compose.ui.window.Dialog(onDismissRequest = { showFeeReminderDialog = false }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Red.copy(alpha = 0.85f), shape = RoundedCornerShape(16.dp))
                            .padding(24.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Warning, contentDescription = "Warning", tint = Color.White, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "FEE REMINDER",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                feeReminderText.ifBlank { "Your fees are pending. Please pay immediately to avoid consequences." },
                                color = Color.White,
                                fontSize = 16.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { showFeeReminderDialog = false },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                            ) {
                                Text("Dismiss", color = Color.Red, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            
            if (selectedTab == 0) {
                if (visibleMessages.isNotEmpty()) {
                    MessageCarousel(
                        messages = visibleMessages,
                        onReply = { msg ->
                            val senderId = msg["senderId"]?.toString() ?: ""
                            val contactId = if (senderId == "admin") "admin" else msg["teacherId"]?.toString() ?: ""
                            navController.currentBackStackEntry?.savedStateHandle?.set("contactId", contactId)
                            navController.navigate("student_messages")
                        },
                        onDismiss = { msg ->
                            msg["docId"]?.toString()?.let { addDismissedId(it) }
                        },
                        color = Color(0xFFE3F2FD),
                        iconTint = AppColors.Student
                    )
                }

                // Announcements Section
                val myCollege = studentProfile?.get("college")?.toString() ?: ""
                val myCourse = studentProfile?.get("course")?.toString() ?: ""
                val mySession = studentProfile?.get("session")?.toString() ?: ""
                
                val filteredAnnouncements = announcements.filter { announcement ->
                    !announcement.isLocal || (announcement.targetCollege == myCollege && announcement.targetCourse == myCourse && announcement.targetSession == mySession)
                }

                if (filteredAnnouncements.isEmpty()) {
                    Text("No announcements available.", modifier = Modifier.padding(16.dp), color = AppColors.TextSecondary)
                } else {
                    filteredAnnouncements.forEach { announcement ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                if (announcement.isLocal) {
                                    Text("Local announcement by ${announcement.senderName}", fontSize = 12.sp, color = AppColors.Teacher, fontWeight = FontWeight.SemiBold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                                Text(announcement.subject, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AppColors.Navy)
                                val formatter = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault())
                                val dateString = formatter.format(java.util.Date(announcement.timestamp))
                                val sender = if (announcement.senderName.isNotEmpty()) announcement.senderName else "Admin"
                                Text("By $sender | $dateString", fontSize = 12.sp, color = AppColors.Student)
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                androidx.compose.ui.viewinterop.AndroidView(
                                    factory = { ctx ->
                                        android.widget.TextView(ctx).apply {
                                            textSize = 14f
                                            setTextColor(android.graphics.Color.parseColor("#666666"))
                                            autoLinkMask = android.text.util.Linkify.WEB_URLS
                                            movementMethod = android.text.method.LinkMovementMethod.getInstance()
                                            text = announcement.description
                                        }
                                    },
                                    update = { view ->
                                        view.text = announcement.description
                                    }
                                )
                                
                                if (announcement.url.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = announcement.url,
                                        fontSize = 14.sp,
                                        color = Color.Blue,
                                        modifier = Modifier.clickable {
                                            var validUrl = announcement.url
                                            if (!validUrl.startsWith("http://") && !validUrl.startsWith("https://")) {
                                                validUrl = "http://$validUrl"
                                            }
                                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(validUrl))
                                            context.startActivity(intent)
                                        }
                                    )
                                }

                                if (announcement.attachmentUrl.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "View Attachment",
                                        fontSize = 14.sp,
                                        color = Color.Blue,
                                        modifier = Modifier.clickable {
                                            var validUrl = announcement.attachmentUrl
                                            if (!validUrl.startsWith("http://") && !validUrl.startsWith("https://")) {
                                                validUrl = "http://$validUrl"
                                            }
                                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(validUrl))
                                            context.startActivity(intent)
                                        }
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    coil.compose.AsyncImage(
                                        model = announcement.attachmentUrl,
                                        contentDescription = "Attachment",
                                        contentScale = androidx.compose.ui.layout.ContentScale.FillWidth,
                                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            } else if (selectedTab == 1) {
                // Home
                // Fee Reminder Banner
                Card(modifier = Modifier.fillMaxWidth().padding(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFCE4EC)), shape = RoundedCornerShape(12.dp)) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, "Warning", tint = AppColors.Error, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Fees Pending", fontWeight = FontWeight.Bold, color = AppColors.Error)
                            Text("Your fees payment is pending", fontSize = 12.sp, color = AppColors.TextSecondary)
                        }
                    }
                }
    
                // Welcome Card
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(16.dp)) {
                    Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(50.dp).clip(CircleShape).background(AppColors.Student), contentAlignment = Alignment.Center) {
                            val initial = studentProfile?.get("fullName")?.toString()?.firstOrNull()?.toString() ?: "S"
                            Text(initial.uppercase(), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Welcome back!", fontSize = 14.sp, color = AppColors.TextSecondary)
                            Text(studentProfile?.get("fullName")?.toString() ?: "Student", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppColors.Navy)
                            val college = studentProfile?.get("college")?.toString() ?: ""
                            val course = studentProfile?.get("course")?.toString() ?: ""
                            Text(if (college.isNotBlank() && course.isNotBlank()) "$college | $course" else "Raj Educational Group", fontSize = 13.sp, color = AppColors.TextSecondary)
                        }
                    }
                }
    
                // App ID Card
                Card(modifier = Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(12.dp)) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Badge, "ID", tint = AppColors.Student, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("App ID", fontSize = 13.sp, color = AppColors.TextSecondary)
                            Text(studentId ?: "Unknown", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AppColors.Navy)
                        }
                    }
                }
    

    
                Spacer(modifier = Modifier.height(32.dp))
            } else if (selectedTab == 2) {
                // Gallery
                var photos by remember { mutableStateOf<List<com.rajeducational.erp.data.GalleryPhoto>>(emptyList()) }
                var isGalleryLoading by remember { mutableStateOf(true) }
                
                LaunchedEffect(Unit) {
                    com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("gallery_photos").addSnapshotListener { snapshot, _ ->
                        if (snapshot != null) {
                            val list = snapshot.documents.mapNotNull { doc ->
                                doc.toObject(com.rajeducational.erp.data.GalleryPhoto::class.java)?.copy(id = doc.id)
                            }
                            photos = list
                            isGalleryLoading = false
                        }
                    }
                }
                
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("Gallery", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppColors.Navy)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (isGalleryLoading) {
                        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { 
                            CircularProgressIndicator(color = AppColors.Student) 
                        }
                    } else if (photos.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { 
                            Text("No photos uploaded yet.", color = AppColors.TextSecondary) 
                        }
                    } else {
                        val rows = photos.chunked(3)
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rows.forEach { rowPhotos ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    rowPhotos.forEach { photo ->
                                        val globalIndex = photos.indexOf(photo)
                                        Card(
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1f),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color.White)
                                        ) {
                                            var url = photo.viewUrl
                                            if (!url.startsWith("http")) {
                                                url = "https://rajapp.matavaishnavieducationaltrust.org/$url"
                                            }
                                            Box(modifier = Modifier.fillMaxSize()) {
                                                coil.compose.AsyncImage(
                                                    model = url,
                                                    contentDescription = photo.name,
                                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .clickable { selectedPhotoIndex = globalIndex }
                                                )
                                            }
                                        }
                                    }
                                    // Fill the row with empty space if last row is not full
                                    if (rowPhotos.size < 3) {
                                        repeat(3 - rowPhotos.size) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
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
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(16.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), androidx.compose.foundation.shape.CircleShape)
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
                                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Student),
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 32.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = "Download")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Download")
                            }
                        }
                    }
                }
            } else if (selectedTab == 3) {
                // Profile Editor
                var isEditingProfile by remember { mutableStateOf(false) }
                
                Card(modifier = Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Profile Details", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AppColors.Navy)
                            IconButton(onClick = { 
                                if (isEditingProfile) {
                                    // Save changes
                                    if (studentId != null) {
                                        val updates = hashMapOf<String, Any>(
                                            "fullName" to editFullName,
                                            "phone" to editPhone,
                                            "email" to editEmail,
                                            "address" to editAddress,
                                            "college" to editCollege,
                                            "course" to editCourse,
                                            "session" to editSession
                                        )
                                        com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("students").document(studentId!!).update(updates)
                                        
                                        val sharedPrefs = context.getSharedPreferences("StudentPrefs", android.content.Context.MODE_PRIVATE)
                                        sharedPrefs.edit().apply {
                                            putString("student_name", editFullName)
                                            putString("student_phone", editPhone)
                                            putString("student_email", editEmail)
                                            putString("student_address", editAddress)
                                            putString("student_college", editCollege)
                                            putString("student_course", editCourse)
                                            putString("student_session", editSession)
                                            apply()
                                        }
                                    }
                                }
                                isEditingProfile = !isEditingProfile 
                            }) {
                                Icon(if (isEditingProfile) Icons.Default.Check else Icons.Default.Edit, contentDescription = if (isEditingProfile) "Save" else "Edit", tint = AppColors.Student)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        if (isEditingProfile) {
                            OutlinedTextField(value = editFullName, onValueChange = { editFullName = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(value = editPhone, onValueChange = { editPhone = it }, label = { Text("Phone") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), readOnly = true)
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(value = editEmail, onValueChange = { editEmail = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(value = editAddress, onValueChange = { editAddress = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            @OptIn(ExperimentalMaterial3Api::class)
                            ExposedDropdownMenuBox(
                                expanded = expandedCollege,
                                onExpandedChange = { expandedCollege = !expandedCollege },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = editCollege,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Select College *") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCollege) },
                                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                ExposedDropdownMenu(
                                    expanded = expandedCollege,
                                    onDismissRequest = { expandedCollege = false }
                                ) {
                                    colleges.forEach { college ->
                                        DropdownMenuItem(
                                            text = { Text(college.name) },
                                            onClick = {
                                                editCollege = college.name
                                                selectedCollegeObj = college
                                                editCourse = ""
                                                selectedCourseObj = null
                                                editSession = ""
                                                expandedCollege = false
                                            }
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            @OptIn(ExperimentalMaterial3Api::class)
                            ExposedDropdownMenuBox(
                                expanded = expandedCourse,
                                onExpandedChange = { expandedCourse = !expandedCourse },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = editCourse,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Select Course *") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCourse) },
                                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                ExposedDropdownMenu(
                                    expanded = expandedCourse,
                                    onDismissRequest = { expandedCourse = false }
                                ) {
                                    val courses = selectedCollegeObj?.courses ?: emptyList()
                                    courses.forEach { course ->
                                        DropdownMenuItem(
                                            text = { Text(course.name) },
                                            onClick = {
                                                editCourse = course.name
                                                selectedCourseObj = course
                                                editSession = ""
                                                expandedCourse = false
                                            }
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            @OptIn(ExperimentalMaterial3Api::class)
                            ExposedDropdownMenuBox(
                                expanded = expandedSession,
                                onExpandedChange = { expandedSession = !expandedSession },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = editSession,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Select Course Year (Session) *") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSession) },
                                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                ExposedDropdownMenu(
                                    expanded = expandedSession,
                                    onDismissRequest = { expandedSession = false }
                                ) {
                                    val sessions = selectedCourseObj?.yearBatches ?: emptyList()
                                    sessions.forEach { yearBatch ->
                                        DropdownMenuItem(
                                            text = { Text(yearBatch) },
                                            onClick = {
                                                editSession = yearBatch
                                                expandedSession = false
                                            }
                                        )
                                    }
                                }
                            }
                        } else {
                            ProfileDetailRow("App ID", studentId ?: "N/A")
                            ProfileDetailRow("Full Name", studentProfile?.get("fullName")?.toString() ?: "N/A")
                            ProfileDetailRow("Phone", studentProfile?.get("phone")?.toString() ?: "N/A")
                            ProfileDetailRow("Email", studentProfile?.get("email")?.toString() ?: "N/A")
                            ProfileDetailRow("Address", studentProfile?.get("address")?.toString() ?: "N/A")
                            ProfileDetailRow("College", studentProfile?.get("college")?.toString() ?: "N/A")
                            ProfileDetailRow("Course", studentProfile?.get("course")?.toString() ?: "N/A")
                            ProfileDetailRow("Course Year (Session)", studentProfile?.get("session")?.toString() ?: "N/A")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            } else if (selectedTab == 5) {
                // More Options
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("More Options", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppColors.Navy)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Events
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { navController.navigate("student_events") },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Event, "Events", tint = Color(0xFFFF9800))
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("Events", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(Icons.Default.ChevronRight, "Go", tint = AppColors.TextSecondary)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Voting
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { navController.navigate("student_voting") },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.HowToVote, "Voting", tint = AppColors.Teacher)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("Voting", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(Icons.Default.ChevronRight, "Go", tint = AppColors.TextSecondary)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Messages
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { navController.navigate("student_messages") },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Chat, "Messages", tint = AppColors.Guest)
                            Spacer(modifier = Modifier.width(16.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Messages", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                if (unreadMessagesCount > 0) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(Color.Red, CircleShape)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(Icons.Default.ChevronRight, "Go", tint = AppColors.TextSecondary)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Reviews
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { navController.navigate("student_teacher_reviews") },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, "Review", tint = AppColors.Student)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("Give reviews to teachers", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(Icons.Default.ChevronRight, "Go", tint = AppColors.TextSecondary)
                        }
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("Coming Soon", fontSize = 18.sp, color = AppColors.TextSecondary)
                }
            }
        }
    }
}

@Composable
fun ProfileDetailRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(label, fontSize = 12.sp, color = AppColors.TextSecondary)
        Text(value, fontSize = 16.sp, color = AppColors.TextPrimary, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(4.dp))
        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
    }
}
@Composable
fun QuickMenuItem(label: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(modifier = modifier.clickable(onClick = onClick), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, label, tint = color, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = AppColors.TextPrimary)
        }
    }
}

@Composable
fun MessageCarousel(
    messages: List<Map<String, Any>>,
    onReply: (Map<String, Any>) -> Unit,
    onDismiss: (Map<String, Any>) -> Unit,
    color: Color,
    iconTint: Color
) {
    if (messages.isEmpty()) return

    val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { messages.size })
    
    LaunchedEffect(messages.size) {
        while (isActive) {
            kotlinx.coroutines.delay(2000)
            if (messages.isNotEmpty()) {
                val nextPage = (pagerState.currentPage + 1) % messages.size
                pagerState.animateScrollToPage(
                    nextPage,
                    animationSpec = androidx.compose.animation.core.tween(500, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
                )
            }
        }
    }

    androidx.compose.foundation.pager.HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxWidth()
    ) { page ->
        if (page < messages.size) {
            val msg = messages[page]
            ShakingMessageCard(
                message = msg,
                onReply = { onReply(msg) },
                onDismiss = { onDismiss(msg) },
                color = color,
                iconTint = iconTint
            )
        }
    }
}

@Composable
fun ShakingMessageCard(
    message: Map<String, Any>,
    onReply: () -> Unit,
    onDismiss: () -> Unit,
    color: Color,
    iconTint: Color
) {
    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "shake")
    val xOffset by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(50, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "xOffset"
    )
    
    var isShaking by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (isActive) {
            kotlinx.coroutines.delay(3500)
            isShaking = true
            kotlinx.coroutines.delay(500)
            isShaking = false
        }
    }

    val currentOffset = if (isShaking) xOffset else 0f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .offset(x = currentOffset.dp),
        colors = CardDefaults.cardColors(containerColor = color),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Email, "Message", tint = iconTint)
                Spacer(modifier = Modifier.width(8.dp))
                Text("A new message has come from ${message["senderName"]}", fontWeight = FontWeight.Bold, color = AppColors.Navy, modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, "Close", tint = AppColors.TextSecondary)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(message["message"].toString(), fontSize = 14.sp, color = AppColors.TextPrimary, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(8.dp))
            androidx.compose.material3.TextButton(
                onClick = onReply,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
            ) {
                Text("Click here and reply", color = iconTint, fontWeight = FontWeight.Bold)
            }
        }
    }
}
