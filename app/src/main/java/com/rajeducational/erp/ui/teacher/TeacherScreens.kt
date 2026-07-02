package com.rajeducational.erp.ui.teacher

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.rajeducational.erp.theme.AppColors
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import android.app.DownloadManager
import android.os.Environment
import android.content.Context
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.firestore.FirebaseFirestore
import com.rajeducational.erp.ui.components.AttendancePercentageBadge
import com.rajeducational.erp.ui.components.AttendanceStatsCard

@Composable
fun TeacherBottomNavigationBar(navController: NavController, currentRoute: String) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("TeacherPrefs", android.content.Context.MODE_PRIVATE) }
    val teacherId = prefs.getString("teacher_id", "") ?: ""
    val teacherCollege = prefs.getString("teacher_college", "") ?: ""
    val teacherCourses = prefs.getStringSet("teacher_courses", setOf(prefs.getString("teacher_course", ""))) ?: setOf()
    val teacherYears = prefs.getStringSet("teacher_years", emptySet()) ?: emptySet()
    var studentUnreadCount by remember { mutableStateOf(0) }
    var adminUnreadCount by remember { mutableStateOf(0) }
    val unreadCount = studentUnreadCount + adminUnreadCount

    LaunchedEffect(teacherId) {
        if (teacherId.isNotEmpty()) {
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            db.collection("student_chats")
                .whereEqualTo("teacherId", teacherId)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        studentUnreadCount = snapshot.documents.count { doc ->
                            val senderId = doc.getString("senderId") ?: ""
                            val isRead = doc.getBoolean("isRead") ?: false
                            senderId != teacherId && !isRead
                        }
                    }
                }
            db.collection("teacher_chats")
                .whereEqualTo("teacherId", teacherId)
                .whereEqualTo("adminId", "admin")
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        adminUnreadCount = snapshot.documents.count { doc ->
                            val senderId = doc.getString("senderId") ?: ""
                            val isRead = doc.getBoolean("isRead") ?: false
                            senderId != teacherId && !isRead
                        }
                    }
                }
        }
    }

    NavigationBar(containerColor = Color.White) {
        listOf(
            "Announce" to Icons.Default.Campaign to "teacher_announcements",
            "Dashboard" to Icons.Default.Home to "teacher_dashboard",
            "Gallery" to Icons.Default.PhotoLibrary to "teacher_gallery",
            "Students" to Icons.Default.People to "teacher_students",
            "Ratings" to Icons.Default.Star to "teacher_ratings",
            "Profile" to Icons.Default.Person to "teacher_profile",
            "More" to Icons.Default.MoreHoriz to "teacher_more"
        ).forEach { (pair, route) ->
            val (label, icon) = pair
            NavigationBarItem(
                selected = currentRoute == route,
                onClick = {
                    if (currentRoute != route) {
                        navController.navigate(route) {
                            popUpTo("teacher_announcements") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    Box {
                        Icon(icon, label)
                        if (label == "More" && unreadCount > 0) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color.Red, CircleShape)
                                    .align(Alignment.TopEnd)
                            )
                        }
                    }
                },
                label = { Text(label, fontSize = 10.sp) },
                colors = NavigationBarItemDefaults.colors(selectedIconColor = AppColors.Teacher)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherAnnouncementsScreen(navController: NavController) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var announcements by remember { mutableStateOf<List<com.rajeducational.erp.data.Announcement>>(emptyList()) }
    val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
    val prefs = context.getSharedPreferences("TeacherPrefs", android.content.Context.MODE_PRIVATE)
    val teacherId = prefs.getString("teacher_id", "") ?: ""

    LaunchedEffect(Unit) {
        firestore.collection("announcements")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                announcements = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(com.rajeducational.erp.data.Announcement::class.java)?.copy(id = doc.id)
                }
            }
    }

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
    
    var unreadMessages by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }

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

    LaunchedEffect(teacherId) {
        if (teacherId.isNotEmpty()) {
            firestore.collection("student_chats")
                .whereEqualTo("teacherId", teacherId)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        val unreadDocs = snapshot.documents.filter { doc ->
                            val senderId = doc.getString("senderId") ?: ""
                            val isRead = doc.getBoolean("isRead") ?: false
                            senderId != teacherId && !isRead
                        }
                        
                        val latestPerSender = unreadDocs.groupBy { it.getString("senderId") ?: "" }
                            .map { (_, docs) ->
                                val latestDoc = docs.maxByOrNull { it.getLong("timestamp") ?: 0L }
                                latestDoc?.data?.plus("docId" to latestDoc.id)
                            }.filterNotNull()
                            
                        unreadMessages = latestPerSender
                    }
                }
        }
    }

    Scaffold(
        topBar = { 
            TopAppBar(
                title = { Text("Announcements", fontWeight = FontWeight.Bold) }, 
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Teacher, titleContentColor = Color.White)
            ) 
        },
        bottomBar = {
            TeacherBottomNavigationBar(navController, "teacher_announcements")
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(AppColors.Background)
                .verticalScroll(rememberScrollState())
        ) {
            val visibleMessages = unreadMessages.filter { msg ->
                val docId = msg["docId"] as? String ?: return@filter false
                val firstSeen = messageFirstSeenVisit[docId] ?: dashboardVisits
                !dismissedMessageIds.contains(docId) && (dashboardVisits - firstSeen) < 2
            }
            
            if (visibleMessages.isNotEmpty()) {
                com.rajeducational.erp.ui.student.MessageCarousel(
                    messages = visibleMessages,
                    onReply = { msg ->
                        val contactId = msg["studentId"]?.toString() ?: ""
                        navController.currentBackStackEntry?.savedStateHandle?.set("contactId", contactId)
                        navController.navigate("teacher_messages")
                    },
                    onDismiss = { msg ->
                        msg["docId"]?.toString()?.let { addDismissedId(it) }
                    },
                    color = Color(0xFFE3F2FD),
                    iconTint = AppColors.Teacher
                )
            }

            if (announcements.isEmpty()) {
                Text("No announcements available.", modifier = Modifier.padding(16.dp), color = AppColors.TextSecondary)
            } else {
                announcements.forEach { announcement ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(announcement.subject, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AppColors.Navy)
                            val formatter = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault())
                            val dateString = formatter.format(java.util.Date(announcement.timestamp))
                            val sender = if (announcement.senderName.isNotEmpty()) announcement.senderName else "Admin"
                            Text("By $sender | $dateString", fontSize = 12.sp, color = AppColors.Teacher)
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
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherDashboardScreen(navController: NavController) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = context.getSharedPreferences("TeacherPrefs", android.content.Context.MODE_PRIVATE)
    val teacherName = prefs.getString("teacher_name", "Dr. Test Teacher") ?: "Teacher"
    val teacherCollege = prefs.getString("teacher_college", "Raj Nursing Institute") ?: "Raj Educational"
    val teacherCourses = prefs.getStringSet("teacher_courses", setOf(prefs.getString("teacher_course", "Nursing") ?: "General")) ?: setOf("General")
    val teacherYears = prefs.getStringSet("teacher_years", emptySet()) ?: emptySet()
    val teacherId = prefs.getString("teacher_id", "") ?: ""

    val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()

    var studentRatings by remember { mutableStateOf<List<com.rajeducational.erp.ui.admin.TeacherReview>>(emptyList()) }
    var managementReview by remember { mutableStateOf<com.rajeducational.erp.ui.admin.TeacherReview?>(null) }

    var showQrDialog by remember { mutableStateOf(false) }
    var qrContentString by remember { mutableStateOf("") }
    var qrBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var secondsRemaining by remember { mutableIntStateOf(15) }
    var expandedApprovals by remember { mutableStateOf(false) }
    var expandedAnnouncements by remember { mutableStateOf(false) }
    var expandedAdminFeatures by remember { mutableStateOf(false) }
    
    var adminFeatures by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var holidayState by remember { mutableStateOf<com.rajeducational.erp.utils.HolidayHelper.HolidayCheckResult?>(null) }

    LaunchedEffect(teacherCollege) {
        val tz = java.util.TimeZone.getTimeZone("Asia/Kolkata")
        val cal = java.util.Calendar.getInstance(tz)
        if (cal.get(java.util.Calendar.DAY_OF_WEEK) == java.util.Calendar.SUNDAY) {
            holidayState = com.rajeducational.erp.utils.HolidayHelper.HolidayCheckResult(true, "Sunday")
        } else if (teacherCollege.isNotEmpty()) {
            val res = com.rajeducational.erp.utils.HolidayHelper.checkHolidayForTeacherStaff(teacherCollege)
            holidayState = res
        }
    }

    LaunchedEffect(teacherId) {
        if (teacherId.isNotEmpty()) {
            try {
                val doc = com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("teachers").document(teacherId).get().await()
                val features = (doc.get("adminFeatures") as? Map<String, Any>)?.mapValues { it.value as? Boolean ?: false } ?: emptyMap()
                adminFeatures = features
            } catch(e: Exception) {}
        }
    }

    LaunchedEffect(showQrDialog, teacherId) {
        if (showQrDialog && teacherId.isNotEmpty()) {
            while (showQrDialog) {
                val ts = System.currentTimeMillis()
                qrContentString = "teacher_attendance_qr:${teacherId}:${ts}"
                qrBitmap = com.rajeducational.erp.ui.admin.generateQrCode(qrContentString)
                secondsRemaining = 15
                for (i in 15 downTo 1) {
                    if (!showQrDialog) break
                    secondsRemaining = i
                    kotlinx.coroutines.delay(1000)
                }
            }
        }
    }

    LaunchedEffect(teacherId) {
        if (teacherId.isNotBlank()) {
            firestore.collection("teacher_reviews").whereEqualTo("teacherId", teacherId).addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    studentRatings = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(com.rajeducational.erp.ui.admin.TeacherReview::class.java)?.copy(id = doc.id)
                    }
                }
            }
            firestore.collection("management_reviews").document(teacherId).addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    managementReview = snapshot.toObject(com.rajeducational.erp.ui.admin.TeacherReview::class.java)?.copy(id = snapshot.id)
                }
            }
        }
    }

    val studentReviewCount = studentRatings.size
    val averageStudentRating = if (studentRatings.isNotEmpty()) {
        val allRatings = studentRatings.flatMap { it.ratings.values }
        if (allRatings.isNotEmpty()) allRatings.average() else 0.0
    } else 0.0

    val managementScoreMap = managementReview?.ratings ?: emptyMap()
    val averageManagementRating = if (managementScoreMap.isNotEmpty()) {
        managementScoreMap.values.average()
    } else 0.0

    val combinedScore = if (studentReviewCount > 0 && managementScoreMap.isNotEmpty()) {
        (averageStudentRating * 20 + averageManagementRating * 10) / 2
    } else if (studentReviewCount > 0) {
        averageStudentRating * 20
    } else if (managementScoreMap.isNotEmpty()) {
        averageManagementRating * 10
    } else {
        0.0
    }

    val compositeTextLabel = when {
        combinedScore >= 90 -> "Excellent"
        combinedScore >= 75 -> "Above Average"
        combinedScore >= 50 -> "Average"
        combinedScore > 0 -> "Needs Improvement"
        else -> "N/A"
    }

    Scaffold(bottomBar = {
        TeacherBottomNavigationBar(navController, "teacher_dashboard")
    }) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().background(AppColors.Background).verticalScroll(rememberScrollState())) {
            Row(modifier = Modifier.fillMaxWidth().background(AppColors.Teacher).padding(16.dp)) { 
                Text("Teacher Dashboard", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                IconButton(onClick = { 
                    prefs.edit().clear().apply()
                    navController.navigate("landing") { popUpTo(0) }
                }) { 
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, "Logout", tint = Color.White) 
                } 
            }
            
            Card(modifier = Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(16.dp)) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(50.dp).clip(CircleShape).background(AppColors.Teacher), contentAlignment = Alignment.Center) { Text(teacherName.firstOrNull()?.toString() ?: "T", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(teacherName, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppColors.Navy)
                            Spacer(modifier = Modifier.width(8.dp))
                            AttendancePercentageBadge(
                                studentId = teacherId,
                                backgroundColor = AppColors.Teacher.copy(alpha = 0.15f),
                                textColor = AppColors.Teacher
                            )
                        }
                        Text("${teacherCourses.joinToString(", ")} | $teacherCollege", fontSize = 13.sp, color = AppColors.TextSecondary) 
                        if (teacherYears.isNotEmpty()) {
                            Text("Batches: ${teacherYears.joinToString(", ")}", fontSize = 12.sp, color = AppColors.TextSecondary)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (holidayState?.isHoliday == true) {
                val isSunday = holidayState?.holidayName?.equals("Sunday", ignoreCase = true) == true
                val messageTitle = if (isSunday) "Sunday" else "Holiday Today"
                val messageBody = if (isSunday) {
                    "Today is Sunday, the college will be off, and your attendance won't be affected."
                } else {
                    "Today is a holiday (${holidayState?.holidayName}), the college will be off, and your attendance won't be affected."
                }
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isSunday) Color(0xFFE8F5E9) else Color(0xFFFFF9C4)),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSunday) Color(0xFF81C784) else Color(0xFFFBC02D))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isSunday) Icons.Default.CalendarMonth else Icons.Default.Celebration,
                            contentDescription = "Holiday",
                            tint = if (isSunday) Color(0xFF2E7D32) else Color(0xFFF57F17),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = messageTitle,
                                fontWeight = FontWeight.Bold,
                                color = if (isSunday) Color(0xFF1B5E20) else Color(0xFF5D4037),
                                fontSize = 16.sp
                             )
                             Text(
                                 text = messageBody,
                                 color = if (isSunday) Color(0xFF2E7D32) else Color(0xFF795548),
                                 fontSize = 14.sp
                             )
                         }
                     }
                 }
                 Spacer(modifier = Modifier.height(16.dp))
             }

             AttendanceStatsCard(
                studentId = teacherId,
                modifier = Modifier.padding(horizontal = 16.dp),
                cardColor = Color.White
            )

            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clickable { showQrDialog = true },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCode,
                        contentDescription = "QR Attendance",
                        tint = AppColors.Teacher
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Give Attendance by QR Code",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.Navy
                        )
                        Text(
                            text = "Quickly mark your daily attendance using QR",
                            fontSize = 12.sp,
                            color = AppColors.TextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Go",
                        tint = AppColors.TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clickable { navController.navigate("teacher_daily_teaching_plan") },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Assignment,
                        contentDescription = "Daily Teaching Plan",
                        tint = AppColors.Teacher
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Daily Teaching Plan",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.Navy
                        )
                        Text(
                            text = "Input and manage your daily classroom topic plan",
                            fontSize = 12.sp,
                            color = AppColors.TextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Go",
                        tint = AppColors.TextSecondary
                    )
                }
            }
        }
    }

    if (showQrDialog) {
        val activity = context as? android.app.Activity
        DisposableEffect(Unit) {
            activity?.window?.setFlags(
                android.view.WindowManager.LayoutParams.FLAG_SECURE,
                android.view.WindowManager.LayoutParams.FLAG_SECURE
            )
            onDispose {
                activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
            }
        }

        AlertDialog(
            onDismissRequest = { showQrDialog = false },
            title = {
                Text(
                    text = "Give Attendance by QR Code",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.Navy
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Show this QR code to the school scanner.",
                        fontSize = 14.sp,
                        color = AppColors.TextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    if (qrBitmap != null) {
                        Image(
                            bitmap = qrBitmap!!.asImageBitmap(),
                            contentDescription = "Attendance QR Code",
                            modifier = Modifier
                                .size(240.dp)
                                .background(Color.White)
                                .padding(8.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(240.dp)
                                .background(Color.LightGray.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = AppColors.Teacher)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Screenshots are disabled. You cannot do a screenshot of the QR code. This is illegal.",
                        fontSize = 12.sp,
                        color = Color.Red,
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Text(
                        text = "Refreshing in $secondsRemaining seconds...",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = AppColors.Teacher
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    LinearProgressIndicator(
                        progress = { secondsRemaining / 120f },
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                        color = AppColors.Teacher,
                        trackColor = Color.LightGray.copy(alpha = 0.3f)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showQrDialog = false }
                ) {
                    Text("Close", color = AppColors.Teacher)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherStudentsScreen(navController: NavController) {
    var search by remember { mutableStateOf("") }
    var students by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = context.getSharedPreferences("TeacherPrefs", android.content.Context.MODE_PRIVATE)
    val teacherId = prefs.getString("teacher_id", "") ?: ""
    val teacherCollege = prefs.getString("teacher_college", "") ?: ""
    val teacherCourses = prefs.getStringSet("teacher_courses", setOf(prefs.getString("teacher_course", ""))) ?: setOf()
    val teacherYears = prefs.getStringSet("teacher_years", emptySet()) ?: emptySet()

    
    val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()

    var selectedStudent by remember { mutableStateOf<Map<String, Any>?>(null) }
    var newPasswordInput by remember { mutableStateOf("") }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(teacherId, teacherCollege) {
        if (teacherId.isNotEmpty() && teacherCollege.isNotEmpty()) {
            firestore.collection("students")
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        val fetchedStudents = snapshot.documents.map { doc ->
                            doc.data ?: emptyMap()
                        }
                        students = fetchedStudents.filter {
                            val c = it["course"] as? String ?: ""
                            val s = it["session"] as? String ?: ""
                            val col = it["college"] as? String ?: ""
                            col == teacherCollege && teacherCourses.contains(c) && teacherYears.contains(s)
                        }
                    }
                }
        }
    }

    val filteredStudents = students.filter {
        val name = it["fullName"] as? String ?: ""
        val id = it["id"] as? String ?: ""
        val course = it["course"] as? String ?: ""
        name.contains(search, ignoreCase = true) || id.contains(search, ignoreCase = true) || course.contains(search, ignoreCase = true)
    }

    Scaffold(
        topBar = { 
            TopAppBar(
                title = { Text("My Students") }, 
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }, 
                actions = {
                    TextButton(onClick = { navController.navigate("teacher_all_students") }) {
                        Text("See All Students", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Teacher, titleContentColor = Color.White, navigationIconContentColor = Color.White)
            ) 
        },
        bottomBar = { TeacherBottomNavigationBar(navController, "teacher_students") }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().background(AppColors.Background)) {
            OutlinedTextField(value = search, onValueChange = { search = it }, label = { Text("Search by name, ID, course...") }, leadingIcon = { Icon(Icons.Default.Search, "Search") }, modifier = Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(10.dp))
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                items(filteredStudents.size) { index ->
                    val student = filteredStudents[index]
                    val name = student["fullName"] as? String ?: "Unknown"
                    val id = student["id"] as? String ?: ""
                    val profileUrl = student["profileUrl"] as? String
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { selectedStudent = student }, 
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (profileUrl.isNullOrEmpty()) {
                                Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(AppColors.Teacher), contentAlignment = Alignment.Center) { Text(name.firstOrNull()?.toString() ?: "?", color = Color.White, fontWeight = FontWeight.Bold) }
                            } else {
                                com.rajeducational.erp.ui.components.ProfileImage(
                                    urlOrBase64 = profileUrl,
                                    modifier = Modifier.size(40.dp).clip(CircleShape),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) { 
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(name, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f, fill = false))
                                    val isAttending = student["isAttending"] as? Boolean ?: true
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        color = if (isAttending) AppColors.Student.copy(alpha = 0.1f) else Color.Red.copy(alpha = 0.1f),
                                        contentColor = if (isAttending) AppColors.Student else Color.Red,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = if (isAttending) "Attending" else "Non-attending",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(id, fontSize = 12.sp, color = AppColors.TextSecondary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    AttendancePercentageBadge(
                                        studentId = id,
                                        backgroundColor = AppColors.Teacher.copy(alpha = 0.15f),
                                        textColor = AppColors.Teacher
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedStudent != null) {
        val s = selectedStudent!!
        val name = s["fullName"] as? String ?: "N/A"
        val id = s["id"] as? String ?: "N/A"
        val profileUrl = s["profileUrl"] as? String
        val phone = s["phone"] as? String ?: "N/A"
        val email = s["email"] as? String ?: "N/A"
        val address = s["address"] as? String ?: "N/A"
        val college = s["college"] as? String ?: "N/A"
        val course = s["course"] as? String ?: "N/A"
        val session = s["session"] as? String ?: "N/A"
        val currentPassword = s["password"] as? String ?: "N/A"

        AlertDialog(
            onDismissRequest = { selectedStudent = null; newPasswordInput = "" },
            title = {
                Text(
                    text = "Student Profile Details",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = AppColors.Navy
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (!profileUrl.isNullOrEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            com.rajeducational.erp.ui.components.ProfileImage(
                                urlOrBase64 = profileUrl,
                                modifier = Modifier.size(100.dp).clip(CircleShape),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        }
                    }
                    DetailDialogRow(label = "Full Name", value = name, icon = Icons.Default.Person)
                    DetailDialogRow(label = "App ID", value = id, icon = Icons.Default.Info)
                    DetailDialogRow(label = "Phone", value = phone, icon = Icons.Default.Phone)
                    DetailDialogRow(label = "Email", value = email, icon = Icons.Default.Email)
                    DetailDialogRow(label = "Address", value = address, icon = Icons.Default.Home)
                    DetailDialogRow(label = "College", value = college, icon = Icons.Default.School)
                    DetailDialogRow(label = "Course", value = course, icon = Icons.Default.Book)
                    DetailDialogRow(label = "Session", value = session, icon = Icons.Default.DateRange)
                    DetailDialogRow(label = "Current Password", value = currentPassword, icon = Icons.Default.Lock)

                    Spacer(modifier = Modifier.height(10.dp))
                    AttendanceStatsCard(
                        studentId = id,
                        cardColor = AppColors.Teacher.copy(alpha = 0.05f)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Text(
                        text = "Reset Password",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = AppColors.Navy
                    )

                    OutlinedTextField(
                        value = newPasswordInput,
                        onValueChange = { newPasswordInput = it },
                        label = { Text("New Password") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    Button(
                        onClick = {
                            if (newPasswordInput.isNotBlank()) {
                                if (id.isNotEmpty() && id != "N/A") {
                                    firestore.collection("students").document(id)
                                        .update("password", newPasswordInput.trim())
                                        .addOnSuccessListener {
                                            android.widget.Toast.makeText(context, "Password updated successfully!", android.widget.Toast.LENGTH_SHORT).show()
                                            
                                            // Update local selected student map to reflect the updated password in real time
                                            val updated = s.toMutableMap()
                                            updated["password"] = newPasswordInput.trim()
                                            selectedStudent = updated
                                            
                                            newPasswordInput = ""
                                        }
                                        .addOnFailureListener {
                                            android.widget.Toast.makeText(context, "Failed to update password", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                }
                            } else {
                                android.widget.Toast.makeText(context, "Password cannot be empty", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.Teacher),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Update Password")
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            showDeleteConfirmDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete Student", color = Color.White)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedStudent = null; newPasswordInput = "" }) {
                    Text("Close", color = AppColors.Teacher)
                }
            }
        )
    }

    if (showDeleteConfirmDialog && selectedStudent != null) {
        val s = selectedStudent!!
        val name = s["fullName"] as? String ?: "Unknown"
        val id = s["id"] as? String ?: ""
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Student", fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F)) },
            text = { Text("Are you sure you want to delete student $name ($id)? This action is permanent and cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        if (id.isNotEmpty()) {
                            firestore.collection("students").document(id)
                                .delete()
                                .addOnSuccessListener {
                                    android.widget.Toast.makeText(context, "Student deleted successfully!", android.widget.Toast.LENGTH_SHORT).show()
                                    showDeleteConfirmDialog = false
                                    selectedStudent = null
                                    newPasswordInput = ""
                                }
                                .addOnFailureListener {
                                    android.widget.Toast.makeText(context, "Failed to delete student", android.widget.Toast.LENGTH_SHORT).show()
                                    showDeleteConfirmDialog = false
                                }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel", color = AppColors.Teacher)
                }
            }
        )
    }
}

@Composable
fun DetailDialogRow(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AppColors.Teacher,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(text = label, fontSize = 11.sp, color = AppColors.TextSecondary)
            Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Navy)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherReportsScreen(navController: NavController) {
    var report by remember { mutableStateOf("") }
    Scaffold(
        topBar = { TopAppBar(title = { Text("Daily Report") }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Teacher, titleContentColor = Color.White, navigationIconContentColor = Color.White)) },
        bottomBar = { TeacherBottomNavigationBar(navController, "teacher_reports") }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().background(AppColors.Background).verticalScroll(rememberScrollState()).padding(16.dp)) {
            Card(shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Daily Report - Today", fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(value = report, onValueChange = { report = it }, label = { Text("Describe what you did today...") }, modifier = Modifier.fillMaxWidth().height(120.dp), shape = RoundedCornerShape(10.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = {}, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = AppColors.Teacher), shape = RoundedCornerShape(10.dp)) { Text("Submit Report") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherRatingsScreen(navController: NavController) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = context.getSharedPreferences("TeacherPrefs", android.content.Context.MODE_PRIVATE)
    val teacherId = prefs.getString("teacher_id", "") ?: ""
    val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()

    var studentRatings by remember { mutableStateOf<List<com.rajeducational.erp.ui.admin.TeacherReview>>(emptyList()) }
    var managementReview by remember { mutableStateOf<com.rajeducational.erp.ui.admin.TeacherReview?>(null) }
    
    var showStudentRatings by remember { mutableStateOf(false) }
    var showManagementRatings by remember { mutableStateOf(false) }
    
    val dateFormatter = remember { java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault()) }

    LaunchedEffect(teacherId) {
        if (teacherId.isNotBlank()) {
            firestore.collection("teacher_reviews").whereEqualTo("teacherId", teacherId).addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    studentRatings = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(com.rajeducational.erp.ui.admin.TeacherReview::class.java)?.copy(id = doc.id)
                    }
                }
            }
            firestore.collection("management_reviews").document(teacherId).addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    managementReview = snapshot.toObject(com.rajeducational.erp.ui.admin.TeacherReview::class.java)?.copy(id = snapshot.id)
                }
            }
        }
    }

    val studentReviewCount = studentRatings.size
    val averageStudentRating = if (studentRatings.isNotEmpty()) {
        val allRatings = studentRatings.flatMap { it.ratings.values }
        if (allRatings.isNotEmpty()) allRatings.average() else 0.0
    } else 0.0

    val managementScoreMap = managementReview?.ratings ?: emptyMap()
    val averageManagementRating = if (managementScoreMap.isNotEmpty()) {
        managementScoreMap.values.average()
    } else 0.0

    Scaffold(
        topBar = { TopAppBar(title = { Text("My Ratings") }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Teacher, titleContentColor = Color.White, navigationIconContentColor = Color.White)) },
        bottomBar = { TeacherBottomNavigationBar(navController, "teacher_ratings") }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().background(AppColors.Background).verticalScroll(rememberScrollState()).padding(16.dp)) {
            Card(
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { navController.navigate("teacher_student_ratings_detail") }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Student Rating", fontWeight = FontWeight.SemiBold, color = AppColors.TextSecondary)
                    Row(modifier = Modifier.padding(top = 8.dp)) { 
                        (1..5).forEach { 
                            Icon(if (it <= averageStudentRating.toInt()) Icons.Default.Star else Icons.Default.StarOutline, "Star", tint = AppColors.StarYellow, modifier = Modifier.size(28.dp)) 
                        } 
                    }
                    Text(String.format("%.1f / 5", averageStudentRating), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = AppColors.Student)
                    Text("$studentReviewCount reviews (anonymous)", fontSize = 12.sp, color = AppColors.TextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Click here to see all the ratings",
                        fontSize = 12.sp,
                        color = AppColors.Teacher,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { navController.navigate("teacher_management_ratings_detail") }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Management Rating", fontWeight = FontWeight.SemiBold, color = AppColors.TextSecondary)
                    Text(String.format("%.1f / 10", averageManagementRating), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = AppColors.Teacher)
                    
                    if (managementScoreMap.isEmpty()) {
                        Text("No management reviews yet.", fontSize = 14.sp, color = AppColors.TextSecondary, modifier = Modifier.padding(top = 8.dp))
                    } else {
                        Text(
                            text = "Click here to see all the ratings",
                            fontSize = 12.sp,
                            color = AppColors.Teacher,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Card(shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Composite Score", fontWeight = FontWeight.SemiBold, color = AppColors.TextSecondary)
                    
                    val combinedScore = if (studentReviewCount > 0 && managementScoreMap.isNotEmpty()) {
                        (averageStudentRating * 20 + averageManagementRating * 10) / 2
                    } else if (studentReviewCount > 0) {
                        averageStudentRating * 20
                    } else if (managementScoreMap.isNotEmpty()) {
                        averageManagementRating * 10
                    } else {
                        0.0
                    }
                    
                    Text(String.format("%.1f%%", combinedScore), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = AppColors.Teacher)
                    
                    val textLabel = when {
                        combinedScore >= 90 -> "Excellent"
                        combinedScore >= 75 -> "Above Average"
                        combinedScore >= 50 -> "Average"
                        combinedScore > 0 -> "Needs Improvement"
                        else -> "N/A"
                    }
                    Text(textLabel, fontWeight = FontWeight.SemiBold, color = AppColors.Teacher)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherProfileScreen(navController: NavController) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = context.getSharedPreferences("TeacherPrefs", android.content.Context.MODE_PRIVATE)
    val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
    val coroutineScope = rememberCoroutineScope()
    
    var isEditing by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }

    val teacherId = prefs.getString("teacher_id", "") ?: ""
    
    var teacherName by remember { mutableStateOf(prefs.getString("teacher_name", "Dr. Test Teacher") ?: "Teacher") }
    var teacherPhone by remember { mutableStateOf(prefs.getString("teacher_phone", "N/A") ?: "N/A") }
    var teacherEmail by remember { mutableStateOf(prefs.getString("teacher_email", "N/A") ?: "N/A") }
    var teacherCollege by remember { mutableStateOf(prefs.getString("teacher_college", "Raj Nursing Institute") ?: "Raj Educational") }
    var teacherCourse by remember { mutableStateOf(prefs.getString("teacher_course", "Nursing") ?: "General") }
    var teacherCourses by remember { mutableStateOf(prefs.getStringSet("teacher_courses", setOf(teacherCourse)) ?: setOf(teacherCourse)) }
    var teacherYears by remember { mutableStateOf(prefs.getStringSet("teacher_years", emptySet()) ?: emptySet()) }
    var teacherPassword by remember { mutableStateOf("") } // Used for setting a new password

    var colleges by remember { mutableStateOf<List<com.rajeducational.erp.data.College>>(emptyList()) }
    var selectedCollege by remember { mutableStateOf<com.rajeducational.erp.data.College?>(null) }
    var selectedCourses by remember { mutableStateOf(setOf<com.rajeducational.erp.data.Course>()) }
    var expandedCollege by remember { mutableStateOf(false) }
    var expandedCourse by remember { mutableStateOf(false) }
    var profileUrl by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        try {
            val snapshot = firestore.collection("colleges").get().await()
            colleges = snapshot.documents.mapNotNull { doc ->
                doc.toObject(com.rajeducational.erp.data.College::class.java)?.copy(id = doc.id)
            }
            selectedCollege = colleges.find { it.name == teacherCollege }
            val courseNames = teacherCourses
            selectedCourses = selectedCollege?.courses?.filter { it.name in courseNames }?.toSet() ?: emptySet()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    LaunchedEffect(teacherId) {
        if (teacherId.isNotEmpty()) {
            try {
                val doc = firestore.collection("teachers").document(teacherId).get().await()
                if (doc.exists()) {
                    profileUrl = doc.getString("profileUrl") ?: ""
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Scaffold(
        topBar = { 
            TopAppBar(
                title = { Text("My Profile") }, 
                navigationIcon = { 
                    IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } 
                },
                actions = {
                    if (isEditing) {
                        if (isSaving) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp).padding(end = 16.dp))
                        } else {
                            TextButton(onClick = {
                                if (teacherId.isNotEmpty()) {
                                    isSaving = true
                                    val courseNames = selectedCourses.map { it.name }
                                    val firstCourseName = courseNames.firstOrNull() ?: teacherCourse
                                    val updates = mutableMapOf<String, Any>(
                                        "name" to teacherName,
                                        "phone" to teacherPhone,
                                        "email" to teacherEmail,
                                        "collegeName" to (selectedCollege?.name ?: teacherCollege),
                                        "course" to firstCourseName,
                                        "courses" to courseNames,
                                        "years" to teacherYears.toList()
                                    )
                                    if (teacherPassword.isNotBlank()) {
                                        updates["password"] = teacherPassword
                                    }
                                    
                                    firestore.collection("teachers").document(teacherId).update(updates)
                                        .addOnSuccessListener {
                                            prefs.edit().apply {
                                                putString("teacher_name", teacherName)
                                                putString("teacher_phone", teacherPhone)
                                                putString("teacher_email", teacherEmail)
                                                putString("teacher_college", selectedCollege?.name ?: teacherCollege)
                                                putString("teacher_course", firstCourseName)
                                                putStringSet("teacher_courses", courseNames.toSet())
                                                putStringSet("teacher_years", teacherYears)
                                                apply()
                                            }
                                            teacherCollege = selectedCollege?.name ?: teacherCollege
                                            teacherCourse = firstCourseName
                                            teacherCourses = courseNames.toSet()
                                            isSaving = false
                                            isEditing = false
                                            android.widget.Toast.makeText(context, "Profile updated", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                        .addOnFailureListener {
                                            isSaving = false
                                            android.widget.Toast.makeText(context, "Failed to update profile", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                }
                            }) { Text("Save", color = Color.White) }
                        }
                    } else {
                        IconButton(onClick = { isEditing = true }) { Icon(Icons.Default.Edit, "Edit", tint = Color.White) }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Teacher, titleContentColor = Color.White, navigationIconContentColor = Color.White, actionIconContentColor = Color.White)
            ) 
        },
        bottomBar = { TeacherBottomNavigationBar(navController, "teacher_profile") }
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
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(AppColors.Teacher), 
                contentAlignment = Alignment.Center
            ) { 
                if (profileUrl.isNotEmpty()) {
                    com.rajeducational.erp.ui.components.ProfileImage(
                        urlOrBase64 = profileUrl,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(teacherName.firstOrNull()?.toString() ?: "T", color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.Bold) 
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { navController.navigate("face_registration/teacher/$teacherId") },
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Teacher),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Face, contentDescription = "Scan Face")
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (profileUrl.isNotEmpty()) "Update Face Scan" else "Register Face Photo")
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            if (isEditing) {
                OutlinedTextField(value = teacherName, onValueChange = { teacherName = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = teacherPhone, onValueChange = { teacherPhone = it }, label = { Text("Phone") }, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone), modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = teacherEmail, onValueChange = { teacherEmail = it }, label = { Text("Email") }, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Email), modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                // College Dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedCollege,
                    onExpandedChange = { expandedCollege = !expandedCollege },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedCollege?.name ?: teacherCollege,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Institution") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCollege) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedCollege,
                        onDismissRequest = { expandedCollege = false }
                    ) {
                        colleges.forEach { college ->
                            DropdownMenuItem(
                                text = { Text(college.name) },
                                onClick = {
                                    selectedCollege = college
                                    selectedCourses = emptySet()
                                    teacherYears = emptySet()
                                    expandedCollege = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                // Courses Checkboxes
                if (selectedCollege != null) {
                    Text("Select Courses / Departments", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    val courseOptions = selectedCollege!!.courses
                    if (courseOptions.isEmpty()) {
                        Text("No courses available for this college", color = AppColors.TextSecondary, fontSize = 14.sp)
                    }
                    courseOptions.forEach { course ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (selectedCourses.contains(course)) {
                                        selectedCourses = selectedCourses - course
                                    } else {
                                        selectedCourses = selectedCourses + course
                                    }
                                    teacherYears = emptySet()
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = selectedCourses.contains(course),
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        selectedCourses = selectedCourses + course
                                    } else {
                                        selectedCourses = selectedCourses - course
                                    }
                                    teacherYears = emptySet()
                                },
                                colors = CheckboxDefaults.colors(checkedColor = AppColors.Teacher)
                            )
                            Text(course.name)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Years Checkboxes
                if (selectedCourses.isNotEmpty()) {
                    Text("Select Years", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    val yearOptions = selectedCourses.flatMap { it.yearBatches }.distinct().sorted()
                    if (yearOptions.isEmpty()) {
                        Text("No years available for selected courses", color = AppColors.TextSecondary, fontSize = 14.sp)
                    }
                    yearOptions.forEach { year ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (teacherYears.contains(year)) {
                                        teacherYears = teacherYears - year
                                    } else {
                                        teacherYears = teacherYears + year
                                    }
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = teacherYears.contains(year),
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        teacherYears = teacherYears + year
                                    } else {
                                        teacherYears = teacherYears - year
                                    }
                                },
                                colors = CheckboxDefaults.colors(checkedColor = AppColors.Teacher)
                            )
                            Text(year)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = teacherPassword, onValueChange = { teacherPassword = it }, label = { Text("New Password (optional)") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Delete Profile")
                }
            } else {
                Text(teacherName, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = AppColors.Navy)
                Text(teacherCollege, fontSize = 16.sp, color = AppColors.TextSecondary)
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Contact Information", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AppColors.Teacher)
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Phone, "Phone", tint = AppColors.TextSecondary)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("Phone Number", fontSize = 12.sp, color = AppColors.TextSecondary)
                                Text(teacherPhone, fontSize = 16.sp, color = AppColors.Navy)
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.LightGray)
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Email, "Email", tint = AppColors.TextSecondary)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("Email Address", fontSize = 12.sp, color = AppColors.TextSecondary)
                                Text(teacherEmail.ifEmpty { "Not Provided" }, fontSize = 16.sp, color = AppColors.Navy)
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Academic Details", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AppColors.Teacher)
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.School, "College", tint = AppColors.TextSecondary)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("Institution", fontSize = 12.sp, color = AppColors.TextSecondary)
                                Text(teacherCollege, fontSize = 16.sp, color = AppColors.Navy)
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.LightGray)
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Book, "Department", tint = AppColors.TextSecondary)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("Department / Course", fontSize = 12.sp, color = AppColors.TextSecondary)
                                Text(teacherCourses.joinToString(", "), fontSize = 16.sp, color = AppColors.Navy)
                            }
                        }
                        
                        if (teacherYears.isNotEmpty()) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.LightGray)
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.DateRange, "Years", tint = AppColors.TextSecondary)
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text("Batch Years", fontSize = 12.sp, color = AppColors.TextSecondary)
                                    Text(teacherYears.joinToString(", "), fontSize = 16.sp, color = AppColors.Navy)
                                }
                            }
                        }
                    }
                }
            }
        }
        
        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("Delete Profile") },
                text = { Text("Are you sure you want to delete your profile? This action cannot be undone.") },
                confirmButton = {
                    Button(
                        onClick = {
                            if (teacherId.isNotEmpty()) {
                                isDeleting = true
                                firestore.collection("teachers").document(teacherId).delete()
                                    .addOnSuccessListener {
                                        prefs.edit().clear().apply()
                                        android.widget.Toast.makeText(context, "Profile deleted", android.widget.Toast.LENGTH_SHORT).show()
                                        navController.navigate("landing") {
                                            popUpTo(0)
                                        }
                                    }
                                    .addOnFailureListener {
                                        isDeleting = false
                                        showDeleteConfirm = false
                                        android.widget.Toast.makeText(context, "Failed to delete profile", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        enabled = !isDeleting
                    ) {
                        Text(if (isDeleting) "Deleting..." else "Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }, enabled = !isDeleting) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherGalleryScreen(navController: NavController) {
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Teacher, titleContentColor = Color.White)
            )
        },
        bottomBar = { TeacherBottomNavigationBar(navController, "teacher_gallery") },
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
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = AppColors.Teacher) }
            } else if (photos.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No photos uploaded yet.", color = AppColors.TextSecondary) }
            } else {
                androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                    columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(photos.size) { index ->
                        val photo = photos[index]
                        Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(160.dp)) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                var url = photo.viewUrl
                                if (!url.startsWith("http")) {
                                    url = "https://rajapp.matavaishnavieducationaltrust.org/$url"
                                }
                                coil.compose.AsyncImage(
                                    model = url,
                                    contentDescription = photo.name,
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize().clickable { selectedPhotoIndex = index }
                                )
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
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .align(Alignment.TopCenter),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = { selectedPhotoIndex = null }) {
                            Icon(Icons.Default.Close, "Close", tint = Color.White)
                        }
                        
                        IconButton(onClick = {
                            val photo = photos[pagerState.currentPage]
                            val url = if (photo.viewUrl.startsWith("http")) photo.viewUrl else "https://rajapp.matavaishnavieducationaltrust.org/${photo.viewUrl}"
                            
                            val request = android.app.DownloadManager.Request(android.net.Uri.parse(url))
                                .setTitle(photo.name)
                                .setDescription("Downloading photo")
                                .setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                .setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS, "${photo.name}.jpg")
                                .setAllowedOverMetered(true)
                                .setAllowedOverRoaming(true)
                                
                            val downloadManager = context.getSystemService(android.content.Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
                            downloadManager.enqueue(request)
                            
                            downloadCompleteMessage = "Download started..."
                        }) {
                            Icon(Icons.Default.Download, "Download", tint = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TeacherVotingScreen(navController: NavController) {
    com.rajeducational.erp.ui.common.VotingScreen(navController, "teacher")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherMoreScreen(navController: NavController) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("TeacherPrefs", android.content.Context.MODE_PRIVATE) }
    val teacherId = prefs.getString("teacher_id", "") ?: ""
    var studentUnreadCount by remember { mutableStateOf(0) }
    var adminUnreadCount by remember { mutableStateOf(0) }
    val unreadCount = studentUnreadCount + adminUnreadCount

    var showQrDialog by remember { mutableStateOf(false) }
    var qrContentString by remember { mutableStateOf("") }
    var qrBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var secondsRemaining by remember { mutableIntStateOf(15) }
    var expandedApprovals by remember { mutableStateOf(false) }
    var expandedAnnouncements by remember { mutableStateOf(false) }
    var expandedAdminFeatures by remember { mutableStateOf(false) }
    var adminFeatures by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var pendingAttendingApprovalsCount by remember { mutableStateOf(0) }
    var pendingNonAttendingApprovalsCount by remember { mutableStateOf(0) }
    val pendingApprovalsCount = pendingAttendingApprovalsCount + pendingNonAttendingApprovalsCount

    LaunchedEffect(teacherId) {
        if (teacherId.isNotEmpty()) {
            try {
                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                val teacherDoc = db.collection("teachers").document(teacherId).get().await()
                val teacherCollege = teacherDoc.getString("collegeName") ?: teacherDoc.getString("college") ?: ""
                val teacherCourses = teacherDoc.get("courses") as? List<String> ?: listOf(teacherDoc.getString("course") ?: teacherDoc.getString("departmentName") ?: "")
                val teacherYears = teacherDoc.get("years") as? List<String> ?: emptyList()
                
                db.collection("students")
                    .whereEqualTo("approvalStatus", "pending")
                    .addSnapshotListener { snapshot, _ ->
                        if (snapshot != null) {
                            val matchingDocs = snapshot.documents.filter { doc ->
                                val college = doc.getString("college") ?: ""
                                val course = doc.getString("course") ?: ""
                                val session = doc.getString("session") ?: ""
                                college == teacherCollege && teacherCourses.contains(course) && teacherYears.contains(session)
                            }
                            pendingAttendingApprovalsCount = matchingDocs.count { doc ->
                                doc.getBoolean("isAttending") ?: true
                            }
                            pendingNonAttendingApprovalsCount = matchingDocs.count { doc ->
                                !(doc.getBoolean("isAttending") ?: true)
                            }
                        }
                    }
            } catch(e: Exception) {}
        }
    }

    LaunchedEffect(teacherId) {
        if (teacherId.isNotEmpty()) {
            try {
                val doc = com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("teachers").document(teacherId).get().await()
                val features = (doc.get("adminFeatures") as? Map<String, Any>)?.mapValues { it.value as? Boolean ?: false } ?: emptyMap()
                adminFeatures = features
            } catch(e: Exception) {}
        }
    }

    LaunchedEffect(showQrDialog, teacherId) {
        if (showQrDialog) {
            while (showQrDialog) {
                val ts = System.currentTimeMillis()
                qrContentString = "teacher_attendance_qr:${teacherId}:${ts}"
                qrBitmap = com.rajeducational.erp.ui.admin.generateQrCode(qrContentString)
                secondsRemaining = 15
                for (i in 15 downTo 1) {
                    if (!showQrDialog) break
                    secondsRemaining = i
                    kotlinx.coroutines.delay(1000)
                }
            }
        }
    }

    LaunchedEffect(teacherId) {
        if (teacherId.isNotEmpty()) {
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            db.collection("student_chats")
                .whereEqualTo("teacherId", teacherId)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        studentUnreadCount = snapshot.documents.count { doc ->
                            val senderId = doc.getString("senderId") ?: ""
                            val isRead = doc.getBoolean("isRead") ?: false
                            senderId != teacherId && !isRead
                        }
                    }
                }
            db.collection("teacher_chats")
                .whereEqualTo("teacherId", teacherId)
                .whereEqualTo("adminId", "admin")
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        adminUnreadCount = snapshot.documents.count { doc ->
                            val senderId = doc.getString("senderId") ?: ""
                            val isRead = doc.getBoolean("isRead") ?: false
                            senderId != teacherId && !isRead
                        }
                    }
                }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("More Options") }, colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Teacher, titleContentColor = Color.White)) },
        bottomBar = { TeacherBottomNavigationBar(navController, "teacher_more") }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().background(AppColors.Background).verticalScroll(rememberScrollState()).padding(16.dp)) {
            Card(
                modifier = Modifier.fillMaxWidth().clickable { navController.navigate("teacher_attendance_control_center") },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.QrCodeScanner, "Students Attendance", tint = AppColors.Teacher)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Students Attendance Through QR Code", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ChevronRight, "Go", tint = AppColors.TextSecondary)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth().clickable { navController.navigate("teacher_attendance_report_control") },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PictureAsPdf, "Attendance Report Generation", tint = AppColors.Teacher)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Attendance Report Generation", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ChevronRight, "Go", tint = AppColors.TextSecondary)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth().clickable { expandedApprovals = !expandedApprovals },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.HowToReg, "Approvals", tint = AppColors.Teacher)
                        Spacer(modifier = Modifier.width(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Approvals", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                            if (pendingApprovalsCount > 0) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(Color.Red, CircleShape)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(if (expandedApprovals) Icons.Default.ExpandLess else Icons.Default.ExpandMore, "Expand", tint = AppColors.TextSecondary)
                    }
                    if (expandedApprovals) {
                        Column(modifier = Modifier.padding(start = 56.dp, end = 16.dp, bottom = 16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable { navController.navigate("teacher_attending_approvals") }.padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Attending Students Approval", 
                                    fontSize = 15.sp, color = AppColors.Navy,
                                    modifier = Modifier.weight(1f)
                                )
                                if (pendingAttendingApprovalsCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(Color.Red, CircleShape)
                                    )
                                }
                            }
                            Divider(color = Color.LightGray.copy(alpha = 0.5f))
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable { navController.navigate("teacher_non_attending_approvals") }.padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Non-Attending Students Approval", 
                                    fontSize = 15.sp, color = AppColors.Navy,
                                    modifier = Modifier.weight(1f)
                                )
                                if (pendingNonAttendingApprovalsCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(Color.Red, CircleShape)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth().clickable { expandedAnnouncements = !expandedAnnouncements },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Campaign, "Announcements", tint = AppColors.Teacher)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Announcements", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(if (expandedAnnouncements) Icons.Default.ExpandLess else Icons.Default.ExpandMore, "Expand", tint = AppColors.TextSecondary)
                    }
                    if (expandedAnnouncements) {
                        Column(modifier = Modifier.padding(start = 56.dp, end = 16.dp, bottom = 16.dp)) {
                            Text(
                                "Send Announcements (All)", 
                                modifier = Modifier.fillMaxWidth().clickable { navController.navigate("teacher_send_announcement") }.padding(vertical = 12.dp),
                                fontSize = 15.sp, color = AppColors.Navy
                            )
                            Divider(color = Color.LightGray.copy(alpha = 0.5f))
                            Text(
                                "Send Announcements to My Students", 
                                modifier = Modifier.fillMaxWidth().clickable { navController.navigate("teacher_send_local_announcement") }.padding(vertical = 12.dp),
                                fontSize = 15.sp, color = AppColors.Navy
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth().clickable { navController.navigate("teacher_messages") },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Chat, "Messages", tint = AppColors.Teacher)
                    Spacer(modifier = Modifier.width(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Messages", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        if (unreadCount > 0) {
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
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth().clickable { navController.navigate("teacher_voting") },
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
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth().clickable { navController.navigate("admin_gallery_upload_center") },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Image, "Gallery Photo Upload Center", tint = AppColors.Teacher)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Gallery Photo Upload Center", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ChevronRight, "Go", tint = AppColors.TextSecondary)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth().clickable { navController.navigate("admin_event_control_center") },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Event, "Event Control Center", tint = AppColors.Teacher)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Event Control Center", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ChevronRight, "Go", tint = AppColors.TextSecondary)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            val activeFeatures = adminFeatures.filterValues { it }.keys.toList()
            if (activeFeatures.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { expandedAdminFeatures = !expandedAdminFeatures },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = AppColors.Admin.copy(alpha = 0.1f))
                ) {
                    Column {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AdminPanelSettings, "Admin Control Menu", tint = AppColors.Admin)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("Admin Control Menu", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AppColors.Admin)
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(if (expandedAdminFeatures) Icons.Default.ExpandLess else Icons.Default.ExpandMore, "Expand", tint = AppColors.Admin)
                        }
                        if (expandedAdminFeatures) {
                            Column(modifier = Modifier.padding(start = 56.dp, end = 16.dp, bottom = 16.dp)) {
                                val featureMap = mapOf(
                                    "admin_college_control" to "College Control Centre",
                                    "admin_fee_structure_control" to "College Fee Structure Control Center",
                                    "admin_gallery_upload_center" to "Gallery Photo Upload Center",
                                    "admin_event_control_center" to "Event Control Center",
                                    "admin_guest_messages_control" to "Guest Messages",
                                    "admin_announcement_control" to "Common Announcement Control Center",
                                    "admin_view_students" to "View Students",
                                    "admin_council_voting_control" to "Council Voting Control",
                                    "admin_student_messages_control" to "Student Messages",
                                    "admin_fee_reminder_control" to "Fees Control Panel",
                                    "admin_teacher_qr_control" to "Teacher and Staff Registration by QR Code",
                                    "admin_registered_teachers" to "Registered Teachers and Staff",
                                    "admin_teacher_messages" to "Teacher and Staff Messages",
                                    "admin_teacher_staff_attendance_control" to "Teacher and Staff Attendance Control",
                                    "admin_daily_teaching_plan_control" to "Teacher Daily Teaching Plan",
                                    "admin_statistics_control" to "Dashboard Edit Control Panel",
                                    "admin_developer_options" to "Developer Options",
                                    "admin_give_holiday" to "Give a Holiday",
                                    "admin_teacher_review_criteria_control" to "Teacher Review Criteria Control",
                                    "admin_teacher_reviews" to "Teacher Reviews Monitor",
                                    "admin_management_review_criteria_control" to "Management Review Criteria Control",
                                    "admin_management_review_control" to "Management Reviews Monitor",
                                    "admin_add_admin" to "Add Admin / Monitor Admins",
                                    "admin_generate_reports" to "Generate Reports Control"
                                )
                                activeFeatures.forEachIndexed { index, route ->
                                    val label = featureMap[route] ?: route
                                    Text(
                                        label, 
                                        modifier = Modifier.fillMaxWidth().clickable { navController.navigate(route) }.padding(vertical = 12.dp),
                                        fontSize = 15.sp, color = AppColors.Navy
                                    )
                                    if (index < activeFeatures.size - 1) {
                                        Divider(color = Color.LightGray.copy(alpha = 0.5f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth().clickable { showQrDialog = true },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.QrCode, "QR Attendance", tint = AppColors.Teacher)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Give Attendance by QR Code", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ChevronRight, "Go", tint = AppColors.TextSecondary)
                }
            }
        }
    }

    if (showQrDialog) {
        val activity = context as? android.app.Activity
        DisposableEffect(Unit) {
            activity?.window?.setFlags(
                android.view.WindowManager.LayoutParams.FLAG_SECURE,
                android.view.WindowManager.LayoutParams.FLAG_SECURE
            )
            onDispose {
                activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
            }
        }

        AlertDialog(
            onDismissRequest = { showQrDialog = false },
            title = {
                Text(
                    text = "Give Attendance by QR Code",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.Navy
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Show this QR code to the school scanner.",
                        fontSize = 14.sp,
                        color = AppColors.TextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    if (qrBitmap != null) {
                        Image(
                            bitmap = qrBitmap!!.asImageBitmap(),
                            contentDescription = "Attendance QR Code",
                            modifier = Modifier
                                .size(240.dp)
                                .background(Color.White)
                                .padding(8.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(240.dp)
                                .background(Color.LightGray.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = AppColors.Teacher)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Screenshots are disabled. You cannot do a screenshot of the QR code. This is illegal.",
                        fontSize = 12.sp,
                        color = Color.Red,
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Text(
                        text = "Refreshing in $secondsRemaining seconds...",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = AppColors.Teacher
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    LinearProgressIndicator(
                        progress = { secondsRemaining / 120f },
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                        color = AppColors.Teacher,
                        trackColor = Color.LightGray.copy(alpha = 0.3f)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showQrDialog = false }
                ) {
                    Text("Close", color = AppColors.Teacher)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherSendAnnouncementScreen(navController: NavController) {
    var subject by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var attachmentUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = context.getSharedPreferences("TeacherPrefs", android.content.Context.MODE_PRIVATE)
    val teacherName = prefs.getString("teacher_name", "Teacher") ?: "Teacher"

    val teacherCollege = prefs.getString("teacher_college", "") ?: ""
    val teacherCourses = prefs.getStringSet("teacher_courses", setOf(prefs.getString("teacher_course", ""))) ?: setOf()
    val teacherYears = prefs.getStringSet("teacher_years", emptySet()) ?: emptySet()
    val teacherId = prefs.getString("teacher_id", "") ?: ""
    val coroutineScope = rememberCoroutineScope()
    var myAnnouncements by remember { mutableStateOf<List<com.rajeducational.erp.data.Announcement>>(emptyList()) }
    
    LaunchedEffect(teacherName) {
        if (teacherName.isNotBlank()) {
            firestore.collection("announcements")
                .whereEqualTo("senderName", teacherName)
                .whereEqualTo("senderRole", "Teacher")
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        myAnnouncements = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(com.rajeducational.erp.data.Announcement::class.java)?.copy(id = doc.id)
                        }.sortedByDescending { it.timestamp }
                    }
                }
        }
    }
    
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        attachmentUri = uri
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Send Announcement") },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Teacher, titleContentColor = Color.White, navigationIconContentColor = Color.White)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().background(AppColors.Background).padding(16.dp).verticalScroll(rememberScrollState())) {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("New Announcement", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AppColors.Navy)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it },
                        label = { Text("Subject") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth().height(150.dp),
                        shape = RoundedCornerShape(10.dp),
                        maxLines = 5
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text("Link URL (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = { launcher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray, contentColor = Color.Black),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(if (attachmentUri == null) "Attach Image" else "Image Attached")
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = {
                            if (subject.isNotBlank() && description.isNotBlank()) {
                                isSubmitting = true
                                coroutineScope.launch {
                                    var uploadedUrl = ""
                                    try {
                                        if (attachmentUri != null) {
                                            val photo = com.rajeducational.erp.ui.admin.uploadEventPhotoToHostinger(attachmentUri!!, context)
                                            if (photo != null) {
                                                uploadedUrl = "https://rajapp.matavaishnavieducationaltrust.org/${photo.viewUrl}"
                                            }
                                        }
                                        val announcement = com.rajeducational.erp.data.Announcement(
                                            subject = subject,
                                            description = description,
                                            url = url,
                                            attachmentUrl = uploadedUrl,
                                            timestamp = System.currentTimeMillis(),
                                            senderName = teacherName,
                                            senderRole = "Teacher"
                                        )
                                        firestore.collection("announcements").add(announcement)
                                            .addOnSuccessListener {
                                                android.widget.Toast.makeText(context, "Announcement sent successfully", android.widget.Toast.LENGTH_SHORT).show()
                                                navController.popBackStack()
                                            }
                                            .addOnFailureListener {
                                                isSubmitting = false
                                                android.widget.Toast.makeText(context, "Failed to send", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        isSubmitting = false
                                        android.widget.Toast.makeText(context, "Upload failed", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                android.widget.Toast.makeText(context, "Subject and description required", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.Teacher),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isSubmitting
                    ) {
                        Text(if (isSubmitting) "Sending..." else "Send Announcement", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            if (myAnnouncements.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Text("My Announcements History", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AppColors.Navy, modifier = Modifier.padding(horizontal = 8.dp))
                Spacer(modifier = Modifier.height(8.dp))
                
                myAnnouncements.forEach { announcement ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
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
                                Text("Sent at $dateString", fontSize = 12.sp, color = AppColors.Teacher)
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
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherStudentQRRegistrationScreen(navController: NavController) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = context.getSharedPreferences("TeacherPrefs", android.content.Context.MODE_PRIVATE)
    val teacherId = prefs.getString("teacher_id", "") ?: ""
    var timeRemaining by remember { mutableStateOf(60) }
    var qrContent by remember { mutableStateOf("") }

    LaunchedEffect(teacherId) {
        if (teacherId.isNotEmpty()) {
            val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            while (true) {
                val token = java.util.UUID.randomUUID().toString()
                try {
                    val qrDoc = hashMapOf(
                        "token" to token,
                        "timestamp" to System.currentTimeMillis()
                    )
                    firestore.collection("settings").document("student_qr_$teacherId").set(qrDoc)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                qrContent = "{\"type\":\"teacher_student_reg\",\"teacherId\":\"$teacherId\",\"token\":\"$token\"}"
                timeRemaining = 60
                
                for (i in 60 downTo 1) {
                    kotlinx.coroutines.delay(1000L)
                    timeRemaining = i - 1
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Student QR Registration", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Teacher)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.Background)
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Ask Student to scan this QR Code",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.Navy,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                "This QR code will refresh in ${timeRemaining / 60}:${(timeRemaining % 60).toString().padStart(2, '0')}",
                fontSize = 16.sp,
                color = if (timeRemaining < 15) Color.Red else AppColors.TextSecondary,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            if (teacherId.isNotEmpty() && qrContent.isNotEmpty()) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    val qrBitmap = com.rajeducational.erp.ui.admin.generateQrCode(qrContent)
                    if (qrBitmap != null) {
                        androidx.compose.foundation.Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "QR Code",
                            modifier = Modifier
                                .padding(24.dp)
                                .size(250.dp)
                        )
                    }
                }
            } else if (teacherId.isEmpty()) {
                Text("Teacher ID not found. Please log in again.", color = Color.Red)
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            Text(
                "Note: Scanning this QR code will register the student into your dashboard.",
                fontSize = 14.sp,
                color = AppColors.TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

data class TeacherStudentContact(
    val id: String,
    val name: String,
    val college: String,
    val course: String,
    val session: String,
    val profileUrl: String = "",
    val isAttending: Boolean = true,
    val role: String = "Student"
)

data class TeacherChatMessage(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherMessagesScreen(navController: NavController) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
    
    val prefs = context.getSharedPreferences("TeacherPrefs", android.content.Context.MODE_PRIVATE)
    val teacherId = prefs.getString("teacher_id", "") ?: ""
    val teacherName = prefs.getString("teacher_name", "Teacher") ?: "Teacher"
    var teacherCollege by remember { mutableStateOf(prefs.getString("teacher_college", "") ?: "") }
    var teacherCourses by remember { mutableStateOf(prefs.getStringSet("teacher_courses", setOf(prefs.getString("teacher_course", ""))) ?: setOf()) }
    var teacherYears by remember { mutableStateOf(prefs.getStringSet("teacher_years", emptySet()) ?: emptySet()) }
    
    LaunchedEffect(teacherId) {
        if (teacherId.isNotEmpty()) {
            try {
                val doc = firestore.collection("teachers").document(teacherId).get().await()
                if (doc.exists()) {
                    val col = doc.getString("collegeName") ?: doc.getString("college") ?: ""
                    val crs = doc.get("courses") as? List<String> ?: listOf(doc.getString("course") ?: doc.getString("departmentName") ?: "")
                    val yrs = doc.get("years") as? List<String> ?: emptyList()
                    
                    if (col.isNotEmpty()) teacherCollege = col
                    if (crs.isNotEmpty()) teacherCourses = crs.toSet()
                    if (yrs.isNotEmpty()) teacherYears = yrs.toSet()
                    
                    prefs.edit()
                        .putString("teacher_college", col)
                        .putStringSet("teacher_courses", crs.toSet())
                        .putStringSet("teacher_years", yrs.toSet())
                        .apply()
                }
            } catch(e: Exception) {}
        }
    }
    
    var selectedStudent by remember { mutableStateOf<TeacherStudentContact?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    
    var filterType by remember { mutableStateOf("ALL_CONTACTS") }
    
    // Student list state
    var studentsRaw by remember { mutableStateOf<List<TeacherStudentContact>>(emptyList()) }
    var teachersRaw by remember { mutableStateOf<List<TeacherStudentContact>>(emptyList()) }
    var staffsRaw by remember { mutableStateOf<List<TeacherStudentContact>>(emptyList()) }
    var adminsRaw by remember { mutableStateOf<List<TeacherStudentContact>>(emptyList()) }
    
    val studentList = remember(studentsRaw, teachersRaw, staffsRaw, adminsRaw, teacherId, filterType, teacherCollege, teacherCourses, teacherYears) {
        val baseStudents = studentsRaw.filter { it.id != teacherId }
        val baseTeachers = teachersRaw.filter { it.id != teacherId }
        val baseStaffs = staffsRaw.filter { it.id != teacherId }
        val baseAdmins = if (adminsRaw.isEmpty()) {
            listOf(
                TeacherStudentContact(
                    id = "admin",
                    name = "College Administrator",
                    college = "Central Administration",
                    course = "Admin",
                    session = "Admin",
                    profileUrl = "",
                    role = "Admin"
                )
            )
        } else {
            adminsRaw.filter { it.id != teacherId }
        }

        when (filterType) {
            "MY_STUDENTS" -> {
                baseStudents.filter { s ->
                    s.college == teacherCollege && teacherCourses.contains(s.course) && teacherYears.contains(s.session)
                }
            }
            "ALL_STUDENTS" -> baseStudents
            "TEACHERS" -> baseTeachers
            "STAFFS" -> baseStaffs
            "ADMINS" -> baseAdmins
            else -> { // ALL_CONTACTS
                baseStudents + baseTeachers + baseStaffs + baseAdmins
            }
        }
    }
    var isListLoading by remember { mutableStateOf(true) }
    var allTeacherChats by remember { mutableStateOf<List<TeacherChatMessage>>(emptyList()) }
    
    // Chat messages state
    var adminChatMessages by remember { mutableStateOf<List<TeacherChatMessage>>(emptyList()) }
    var messageText by remember { mutableStateOf("") }
    var attachmentUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var attachmentName by remember { mutableStateOf("") }
    var isUploading by remember { mutableStateOf(false) }
    
    val savedContactId = navController.previousBackStackEntry?.savedStateHandle?.get<String>("contactId")
    
    LaunchedEffect(studentList, savedContactId) {
        if (savedContactId != null && studentList.isNotEmpty()) {
            selectedStudent = studentList.find { it.id == savedContactId }
            navController.previousBackStackEntry?.savedStateHandle?.remove<String>("contactId")
        }
    }
    
    // Load students
    LaunchedEffect(teacherId) {
        if (teacherId.isNotEmpty()) {
            firestore.collection("students")
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        studentsRaw = snapshot.documents.mapNotNull { doc ->
                            val c = doc.getString("course") ?: ""
                            val s = doc.getString("session") ?: ""
                            val col = doc.getString("college") ?: ""
                            
                            TeacherStudentContact(
                                id = doc.id,
                                name = doc.getString("fullName") ?: "Unknown Student",
                                college = col,
                                course = c,
                                session = s,
                                profileUrl = doc.getString("profileUrl") ?: "",
                                isAttending = doc.getBoolean("isAttending") ?: true,
                                role = "Student"
                            )
                        }
                    }
                    isListLoading = false
                }
        } else {
            isListLoading = false
        }
    }

    // Load all teacher chats in real-time
    LaunchedEffect(teacherId) {
        if (teacherId.isNotEmpty()) {
            firestore.collection("student_chats")
                .whereEqualTo("teacherId", teacherId)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        allTeacherChats = snapshot.documents.mapNotNull { doc ->
                            TeacherChatMessage(
                                id = doc.id,
                                studentId = doc.getString("studentId") ?: "",
                                studentName = doc.getString("studentName") ?: "",
                                teacherId = doc.getString("teacherId") ?: "",
                                teacherName = doc.getString("teacherName") ?: "",
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
    
    
    // Load teachers
    LaunchedEffect(teacherId) {
        if (teacherId.isNotEmpty()) {
            firestore.collection("teachers").addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    teachersRaw = snapshot.documents.mapNotNull { doc ->
                        TeacherStudentContact(
                            id = doc.id,
                            name = doc.getString("name") ?: "Teacher",
                            college = doc.getString("collegeName") ?: doc.getString("college") ?: "",
                            course = doc.getString("course") ?: "",
                            session = "Teacher",
                            profileUrl = doc.getString("profileUrl") ?: "",
                            role = "Teacher"
                        )
                    }
                }
            }
        }
    }

    // Load staffs
    LaunchedEffect(teacherId) {
        if (teacherId.isNotEmpty()) {
            firestore.collection("staffs").addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    staffsRaw = snapshot.documents.mapNotNull { doc ->
                        TeacherStudentContact(
                            id = doc.id,
                            name = doc.getString("name") ?: "Staff",
                            college = doc.getString("collegeName") ?: doc.getString("college") ?: "",
                            course = doc.getString("course") ?: doc.getString("departmentName") ?: "",
                            session = "Staff",
                            profileUrl = doc.getString("profileUrl") ?: "",
                            role = "Staff"
                        )
                    }
                }
            }
        }
    }

    // Load admins
    LaunchedEffect(teacherId) {
        if (teacherId.isNotEmpty()) {
            firestore.collection("admins").addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    adminsRaw = snapshot.documents.mapNotNull { doc ->
                        val username = doc.getString("username") ?: ""
                        if (username.isNotEmpty()) {
                            TeacherStudentContact(
                                id = "admin",
                                name = username,
                                college = "Central Administration",
                                course = "Admin",
                                session = "Admin",
                                profileUrl = "",
                                role = "Admin"
                            )
                        } else null
                    }
                }
            }
        }
    }
    
    // Load reverse chats for allTeacherChats
    var reverseTeacherChats by remember { mutableStateOf<List<TeacherChatMessage>>(emptyList()) }
    LaunchedEffect(teacherId) {
        if (teacherId.isNotEmpty()) {
            firestore.collection("student_chats")
                .whereEqualTo("studentId", teacherId)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        reverseTeacherChats = snapshot.documents.mapNotNull { doc ->
                            TeacherChatMessage(
                                id = doc.id,
                                studentId = doc.getString("studentId") ?: "",
                                studentName = doc.getString("studentName") ?: "",
                                teacherId = doc.getString("teacherId") ?: "",
                                teacherName = doc.getString("teacherName") ?: "",
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
    val combinedAllTeacherChats = remember(allTeacherChats, reverseTeacherChats) { allTeacherChats + reverseTeacherChats }

    // Load admin chats in real-time
    LaunchedEffect(teacherId) {
        if (teacherId.isNotEmpty()) {
            firestore.collection("teacher_chats")
                .whereEqualTo("teacherId", teacherId)
                .whereEqualTo("adminId", "admin")
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        adminChatMessages = snapshot.documents.mapNotNull { doc ->
                            TeacherChatMessage(
                                id = doc.id,
                                studentId = "admin",
                                studentName = doc.getString("adminName") ?: "Admin",
                                teacherId = doc.getString("teacherId") ?: "",
                                teacherName = doc.getString("teacherName") ?: "",
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
        }
    }

    val chatMessages = remember(selectedStudent, combinedAllTeacherChats, adminChatMessages) {
        val currentContact = selectedStudent
        if (currentContact == null) {
            emptyList()
        } else if (currentContact.role == "Admin") {
            adminChatMessages
        } else {
            combinedAllTeacherChats.filter { msg ->
                (msg.studentId == currentContact.id && msg.teacherId == teacherId) ||
                (msg.studentId == teacherId && msg.teacherId == currentContact.id)
            }.sortedBy { it.timestamp }
        }
    }

    // Mark messages as read
    LaunchedEffect(selectedStudent, chatMessages) {
        if (selectedStudent != null && teacherId.isNotEmpty()) {
            val unreadDocs = chatMessages.filter { it.senderId != teacherId && !it.isRead }
            for (msg in unreadDocs) {
                val collectionName = if (selectedStudent!!.role == "Admin") "teacher_chats" else "student_chats"
                firestore.collection(collectionName).document(msg.id).update("isRead", true)
            }
        }
    }
    
    // Filtered Students list sorted descending by newest message timestamp
    val filteredStudents = remember(searchQuery, studentList, combinedAllTeacherChats, adminChatMessages) {
        val list = studentList.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.college.contains(searchQuery, ignoreCase = true) ||
            it.course.contains(searchQuery, ignoreCase = true)
        }
        list.sortedByDescending { contact ->
            if (contact.role == "Admin") {
                adminChatMessages.maxOfOrNull { it.timestamp } ?: 0L
            } else {
                combinedAllTeacherChats
                    .filter { it.studentId == contact.id || it.teacherId == contact.id }
                    .maxOfOrNull { it.timestamp } ?: 0L
            }
        }
    }
    
    // File Picker
    val filePickerLauncher = rememberLauncherForActivityResult(
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
    
    // Send Message Handler
    fun sendMessage(text: String, fileUrl: String = "", fileName: String = "") {
        if (text.trim().isEmpty() && fileUrl.isEmpty()) return
        if (selectedStudent == null || teacherId.isEmpty()) return
        
        val msgData = if (selectedStudent!!.role == "Admin") {
            // Message to Admin -> Save to teacher_chats
            hashMapOf(
                "teacherId" to teacherId,
                "teacherName" to teacherName,
                "adminId" to "admin",
                "adminName" to selectedStudent!!.name,
                "senderId" to teacherId,
                "senderName" to teacherName,
                "message" to text,
                "attachmentUrl" to fileUrl,
                "attachmentName" to fileName,
                "timestamp" to System.currentTimeMillis(),
                "isRead" to false
            )
        } else {
            // Message to Student, Teacher, or Staff -> Save to student_chats
            hashMapOf(
                "studentId" to selectedStudent!!.id,
                "studentName" to selectedStudent!!.name,
                "teacherId" to teacherId,
                "teacherName" to teacherName,
                "senderId" to teacherId,
                "senderName" to teacherName,
                "message" to text,
                "attachmentUrl" to fileUrl,
                "attachmentName" to fileName,
                "timestamp" to System.currentTimeMillis(),
                "isRead" to false
            )
        }
        
        val collectionName = if (selectedStudent!!.role == "Admin") "teacher_chats" else "student_chats"
        
        firestore.collection(collectionName)
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
    
    fun uploadAndSend() {
        if (attachmentUri != null) {
            isUploading = true
            coroutineScope.launch {
                try {
                    val photo = com.rajeducational.erp.ui.admin.uploadEventPhotoToHostinger(attachmentUri!!, context)
                    if (photo != null) {
                        sendMessage(messageText, photo.viewUrl, attachmentName)
                    } else {
                        android.widget.Toast.makeText(context, "Attachment upload failed", android.widget.Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    android.widget.Toast.makeText(context, "Error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                } finally {
                    isUploading = false
                }
            }
        } else {
            sendMessage(messageText)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = selectedStudent?.name ?: "Messages",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        if (selectedStudent != null) {
                            Text(
                                text = "Role: ${selectedStudent!!.role} | Dept/Course: ${selectedStudent!!.course}",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectedStudent != null) {
                            selectedStudent = null
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
                actions = {
                    if (selectedStudent == null) {
                        var expanded by remember { mutableStateOf(false) }
                        IconButton(onClick = { expanded = true }) {
                            Icon(Icons.Default.MoreVert, "Filter Students", tint = Color.White)
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            DropdownMenuItem(
                                text = { Text("All Contacts") },
                                onClick = { filterType = "ALL_CONTACTS"; expanded = false }
                            )
                            DropdownMenuItem(
                                text = { Text("My Students") },
                                onClick = { filterType = "MY_STUDENTS"; expanded = false }
                            )
                            DropdownMenuItem(
                                text = { Text("All Students") },
                                onClick = { filterType = "ALL_STUDENTS"; expanded = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Teachers") },
                                onClick = { filterType = "TEACHERS"; expanded = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Staff & Office") },
                                onClick = { filterType = "STAFFS"; expanded = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Administrators") },
                                onClick = { filterType = "ADMINS"; expanded = false }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.Teacher,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        if (selectedStudent == null) {
            // Contacts View
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
                    placeholder = { Text("Search by Student, College or Course...", color = AppColors.TextSecondary) },
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
                        focusedBorderColor = AppColors.Teacher,
                        unfocusedBorderColor = Color.LightGray
                    )
                )
                
                if (isListLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AppColors.Teacher)
                    }
                } else if (filteredStudents.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No students found.", color = AppColors.TextSecondary)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredStudents.size) { index ->
                            val student = filteredStudents[index]
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedStudent = student },
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
                                    if (student.profileUrl.isNotEmpty()) {
                                        com.rajeducational.erp.ui.components.ProfileImage(
                                            urlOrBase64 = student.profileUrl,
                                            modifier = Modifier.size(44.dp).clip(CircleShape),
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .background(
                                                    when (student.role) {
                                                        "Teacher" -> AppColors.Teacher.copy(alpha = 0.15f)
                                                        "Staff" -> Color.Magenta.copy(alpha = 0.15f)
                                                        "Admin" -> Color.DarkGray.copy(alpha = 0.15f)
                                                        else -> AppColors.Teacher.copy(alpha = 0.15f)
                                                    },
                                                    CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = when (student.role) {
                                                    "Student" -> Icons.Default.School
                                                    else -> Icons.Default.Person
                                                },
                                                contentDescription = null,
                                                tint = when (student.role) {
                                                    "Teacher" -> AppColors.Teacher
                                                    "Staff" -> Color.Magenta
                                                    "Admin" -> Color.DarkGray
                                                    else -> AppColors.Teacher
                                                }
                                            )
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.width(16.dp))
                                    
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                            Text(
                                                text = student.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = AppColors.Navy
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            
                                            // Role Badge
                                            Surface(
                                                color = when (student.role) {
                                                    "Teacher" -> AppColors.Teacher.copy(alpha = 0.15f)
                                                    "Staff" -> Color(0xFFE040FB).copy(alpha = 0.15f)
                                                    "Admin" -> AppColors.Admin.copy(alpha = 0.15f)
                                                    else -> AppColors.Student.copy(alpha = 0.15f)
                                                },
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    text = student.role,
                                                    color = when (student.role) {
                                                        "Teacher" -> AppColors.Teacher
                                                        "Staff" -> Color(0xFFE040FB)
                                                        "Admin" -> AppColors.Admin
                                                        else -> AppColors.Student
                                                    },
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 10.sp,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }

                                            val unreadCount = if (student.role == "Admin") {
                                                adminChatMessages.count { it.senderId != teacherId && !it.isRead }
                                            } else {
                                                combinedAllTeacherChats.count { (it.studentId == student.id || it.teacherId == student.id) && it.senderId == student.id && !it.isRead }
                                            }
                                            if (unreadCount > 0) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .background(Color.Red, CircleShape)
                                                )
                                            }
                                            
                                            Spacer(modifier = Modifier.weight(1f))
                                            
                                            val latestMessage = if (student.role == "Admin") {
                                                adminChatMessages.maxByOrNull { it.timestamp }
                                            } else {
                                                combinedAllTeacherChats.filter { it.studentId == student.id || it.teacherId == student.id }.maxByOrNull { it.timestamp }
                                            }
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
                                            text = if (student.role == "Student") {
                                                "College: ${student.college} | Course: ${student.course}"
                                            } else {
                                                "${student.course} | ${student.college}"
                                            },
                                            fontSize = 12.sp,
                                            color = AppColors.TextSecondary,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                        if (student.role == "Student") {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            AttendancePercentageBadge(
                                                studentId = student.id,
                                                backgroundColor = AppColors.Teacher.copy(alpha = 0.15f),
                                                textColor = AppColors.Teacher
                                            )
                                        }
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
                                        text = "Send a message to start conversation.",
                                        fontSize = 14.sp,
                                        color = AppColors.TextSecondary,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            items(chatMessages.size) { idx ->
                                val msg = chatMessages[idx]
                                val isMe = msg.senderId == teacherId
                                
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
                                                containerColor = if (isMe) AppColors.Teacher else Color.White
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
                                                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(fullUrl))
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
                                                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(fullUrl))
                                                                    context.startActivity(intent)
                                                                }
                                                                .padding(8.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.InsertDriveFile,
                                                                contentDescription = null,
                                                                tint = if (isMe) Color.White else AppColors.Teacher,
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
                                                    
                                                    TextButton(
                                                        onClick = {
                                                            try {
                                                                val request = DownloadManager.Request(android.net.Uri.parse(fullUrl))
                                                                    .setTitle(msg.attachmentName)
                                                                    .setDescription("Downloading file attachment...")
                                                                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                                                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, msg.attachmentName)
                                                                    .setAllowedOverMetered(true)
                                                                    .setAllowedOverRoaming(true)
                                                                val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                                                                dm.enqueue(request)
                                                                android.widget.Toast.makeText(context, "Download started...", android.widget.Toast.LENGTH_SHORT).show()
                                                            } catch (e: Exception) {
                                                                android.widget.Toast.makeText(context, "Download failed", android.widget.Toast.LENGTH_SHORT).show()
                                                            }
                                                        },
                                                        colors = ButtonDefaults.textButtonColors(contentColor = if (isMe) Color.White else AppColors.Teacher),
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
                    
                    // Reply bar
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
                                        .background(Color(0xFFE8F5E9))
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.AttachFile, contentDescription = null, tint = AppColors.Teacher)
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
                                        }
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.Red)
                                    }
                                }
                            }
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { filePickerLauncher.launch("*/*") }
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Add Attachment", tint = AppColors.Teacher)
                                }
                                
                                OutlinedTextField(
                                    value = messageText,
                                    onValueChange = { messageText = it },
                                    placeholder = { Text("Write a reply...") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(24.dp),
                                    maxLines = 4,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = AppColors.Teacher,
                                        unfocusedBorderColor = Color.LightGray
                                    )
                                )
                                
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                if (isUploading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = AppColors.Teacher,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    FloatingActionButton(
                                        onClick = { uploadAndSend() },
                                        containerColor = AppColors.Teacher,
                                        contentColor = Color.White,
                                        modifier = Modifier.size(44.dp),
                                        shape = CircleShape
                                    ) {
                                        Icon(Icons.Default.Send, contentDescription = "Send", modifier = Modifier.size(18.dp))
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
fun TeacherSendLocalAnnouncementScreen(navController: NavController) {
    var subject by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var attachmentUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    
    val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = context.getSharedPreferences("TeacherPrefs", android.content.Context.MODE_PRIVATE)
    val teacherName = prefs.getString("teacher_name", "Teacher") ?: "Teacher"
    val teacherId = prefs.getString("teacher_id", "") ?: ""
    val teacherCollege = prefs.getString("teacher_college", "") ?: ""
    val teacherCourses = prefs.getStringSet("teacher_courses", setOf(prefs.getString("teacher_course", ""))) ?: setOf()
    val teacherYears = prefs.getStringSet("teacher_years", emptySet()) ?: emptySet()
    
    var targetCourse by remember { mutableStateOf(teacherCourses.firstOrNull() ?: "") }
    var targetSession by remember { mutableStateOf(teacherYears.firstOrNull() ?: "") }
    
    var expandedCourse by remember { mutableStateOf(false) }
    var expandedSession by remember { mutableStateOf(false) }
    
    val coroutineScope = rememberCoroutineScope()
    var myAnnouncements by remember { mutableStateOf<List<com.rajeducational.erp.data.Announcement>>(emptyList()) }
    
    LaunchedEffect(teacherName) {
        if (teacherName.isNotBlank()) {
            firestore.collection("announcements")
                .whereEqualTo("senderName", teacherName)
                .whereEqualTo("senderRole", "Teacher")
                .whereEqualTo("isLocal", true)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        myAnnouncements = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(com.rajeducational.erp.data.Announcement::class.java)?.copy(id = doc.id)
                        }.sortedByDescending { it.timestamp }
                    }
                }
        }
    }
    
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        attachmentUri = uri
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Send Local Announcement") },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Teacher, titleContentColor = Color.White, navigationIconContentColor = Color.White)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().background(AppColors.Background).padding(16.dp).verticalScroll(rememberScrollState())) {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("New Local Announcement", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AppColors.Navy)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("Target College: $teacherCollege", fontSize = 14.sp, color = AppColors.TextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Course selection
                    Box {
                        OutlinedTextField(
                            value = targetCourse,
                            onValueChange = {},
                            label = { Text("Target Course") },
                            modifier = Modifier.fillMaxWidth().clickable { expandedCourse = true },
                            readOnly = true,
                            trailingIcon = {
                                IconButton(onClick = { expandedCourse = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Course")
                                }
                            }
                        )
                        DropdownMenu(expanded = expandedCourse, onDismissRequest = { expandedCourse = false }) {
                            teacherCourses.forEach { course ->
                                DropdownMenuItem(text = { Text(course) }, onClick = { targetCourse = course; expandedCourse = false })
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Session selection
                    Box {
                        OutlinedTextField(
                            value = targetSession,
                            onValueChange = {},
                            label = { Text("Target Session") },
                            modifier = Modifier.fillMaxWidth().clickable { expandedSession = true },
                            readOnly = true,
                            trailingIcon = {
                                IconButton(onClick = { expandedSession = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Session")
                                }
                            }
                        )
                        DropdownMenu(expanded = expandedSession, onDismissRequest = { expandedSession = false }) {
                            teacherYears.forEach { session ->
                                DropdownMenuItem(text = { Text(session) }, onClick = { targetSession = session; expandedSession = false })
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it },
                        label = { Text("Subject") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth().height(150.dp),
                        shape = RoundedCornerShape(10.dp),
                        maxLines = 5
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text("Link URL (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = { launcher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray, contentColor = Color.Black),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(if (attachmentUri == null) "Attach Image" else "Image Attached")
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = {
                            if (subject.isNotBlank() && description.isNotBlank() && targetCourse.isNotBlank() && targetSession.isNotBlank()) {
                                isSubmitting = true
                                coroutineScope.launch {
                                    var uploadedUrl = ""
                                    try {
                                        if (attachmentUri != null) {
                                            val photo = com.rajeducational.erp.ui.admin.uploadEventPhotoToHostinger(attachmentUri!!, context)
                                            if (photo != null) {
                                                uploadedUrl = "https://rajapp.matavaishnavieducationaltrust.org/${photo.viewUrl}"
                                            }
                                        }
                                        val announcement = com.rajeducational.erp.data.Announcement(
                                            subject = subject,
                                            description = description,
                                            url = url,
                                            attachmentUrl = uploadedUrl,
                                            timestamp = System.currentTimeMillis(),
                                            senderName = teacherName,
                                            senderRole = "Teacher",
                                            isLocal = true,
                                            targetCollege = teacherCollege,
                                            targetCourse = targetCourse,
                                            targetSession = targetSession
                                        )
                                        firestore.collection("announcements").add(announcement)
                                            .addOnSuccessListener {
                                                android.widget.Toast.makeText(context, "Announcement sent successfully", android.widget.Toast.LENGTH_SHORT).show()
                                                navController.popBackStack()
                                            }
                                            .addOnFailureListener {
                                                isSubmitting = false
                                                android.widget.Toast.makeText(context, "Failed to send", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        isSubmitting = false
                                        android.widget.Toast.makeText(context, "Upload failed", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                android.widget.Toast.makeText(context, "Subject, description, target course, and session are required", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.Teacher),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isSubmitting
                    ) {
                        Text(if (isSubmitting) "Sending..." else "Send Local Announcement", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            if (myAnnouncements.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Text("My Local Announcements History", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AppColors.Navy, modifier = Modifier.padding(horizontal = 8.dp))
                Spacer(modifier = Modifier.height(8.dp))
                
                myAnnouncements.forEach { announcement ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(announcement.subject, fontWeight = FontWeight.Bold, color = AppColors.Navy)
                                val dateStr = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(announcement.timestamp))
                                Text(dateStr, fontSize = 12.sp, color = AppColors.TextSecondary)
                                Text("To: ${announcement.targetCourse} | ${announcement.targetSession}", fontSize = 12.sp, color = AppColors.Teacher)
                            }
                            IconButton(onClick = {
                                firestore.collection("announcements").document(announcement.id).delete()
                                    .addOnSuccessListener {
                                        android.widget.Toast.makeText(context, "Deleted", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                            }) {
                                Icon(Icons.Default.Delete, "Delete", tint = Color.Red)
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherStudentRatingsDetailScreen(navController: NavController) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("TeacherPrefs", android.content.Context.MODE_PRIVATE)
    val teacherId = prefs.getString("teacher_id", "") ?: ""
    val firestore = FirebaseFirestore.getInstance()
    var studentRatings by remember { mutableStateOf<List<com.rajeducational.erp.ui.admin.TeacherReview>>(emptyList()) }
    val dateFormatter = remember { java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault()) }

    LaunchedEffect(teacherId) {
        if (teacherId.isNotBlank()) {
            firestore.collection("teacher_reviews")
                .whereEqualTo("teacherId", teacherId)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        studentRatings = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(com.rajeducational.erp.ui.admin.TeacherReview::class.java)?.copy(id = doc.id)
                        }
                    }
                }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Student Ratings Detail") }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Teacher, titleContentColor = Color.White, navigationIconContentColor = Color.White)) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().background(AppColors.Background).verticalScroll(rememberScrollState()).padding(16.dp)) {
            if (studentRatings.isEmpty()) {
                Text("No ratings available.", fontSize = 14.sp, color = AppColors.TextSecondary)
            } else {
                studentRatings.forEachIndexed { index, review ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Student ${index + 1}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AppColors.Navy)
                                val dateStr = if (review.timestamp > 0) dateFormatter.format(java.util.Date(review.timestamp)) else "N/A"
                                Text(dateStr, fontSize = 12.sp, color = AppColors.TextSecondary)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            review.ratings.forEach { (criterion, rating) ->
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(criterion, fontSize = 13.sp, color = AppColors.TextSecondary)
                                    Text("$rating/5", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Navy)
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
fun TeacherManagementRatingsDetailScreen(navController: NavController) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("TeacherPrefs", android.content.Context.MODE_PRIVATE)
    val teacherId = prefs.getString("teacher_id", "") ?: ""
    val firestore = FirebaseFirestore.getInstance()
    var managementReview by remember { mutableStateOf<com.rajeducational.erp.ui.admin.TeacherReview?>(null) }
    val dateFormatter = remember { java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault()) }

    LaunchedEffect(teacherId) {
        if (teacherId.isNotBlank()) {
            firestore.collection("management_reviews")
                .document(teacherId)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null && snapshot.exists()) {
                        managementReview = snapshot.toObject(com.rajeducational.erp.ui.admin.TeacherReview::class.java)?.copy(id = snapshot.id)
                    }
                }
        }
    }

    val managementScoreMap = managementReview?.ratings ?: emptyMap()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Management Ratings Detail") }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Teacher, titleContentColor = Color.White, navigationIconContentColor = Color.White)) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().background(AppColors.Background).verticalScroll(rememberScrollState()).padding(16.dp)) {
            if (managementScoreMap.isEmpty()) {
                Text("No management reviews yet.", fontSize = 14.sp, color = AppColors.TextSecondary)
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        val dateStr = if (managementReview != null && managementReview!!.timestamp > 0) {
                            dateFormatter.format(java.util.Date(managementReview!!.timestamp))
                        } else "N/A"
                        
                        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Submission Date:", fontSize = 13.sp, color = AppColors.TextSecondary)
                            Text(dateStr, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Navy)
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        managementScoreMap.forEach { (name, score) ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(name, fontSize = 12.sp, color = AppColors.TextSecondary, modifier = Modifier.weight(1f))
                                LinearProgressIndicator(progress = { score / 10f }, modifier = Modifier.weight(1f).height(8.dp).padding(horizontal = 8.dp), color = AppColors.Teacher, trackColor = Color(0xFFE0E0E0))
                                Text("$score/10", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(40.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End)
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
fun TeacherAllStudentsScreen(navController: NavController) {
    var search by remember { mutableStateOf("") }
    var allStudents by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()

    LaunchedEffect(Unit) {
        firestore.collection("students")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    allStudents = snapshot.documents.map { doc -> doc.data?.plus("docId" to doc.id) ?: emptyMap() }
                }
            }
    }

    val filteredStudents = allStudents.filter {
        val name = it["fullName"] as? String ?: ""
        val id = it["id"] as? String ?: ""
        val course = it["course"] as? String ?: ""
        name.contains(search, ignoreCase = true) || id.contains(search, ignoreCase = true) || course.contains(search, ignoreCase = true)
    }

    var selectedStudent by remember { mutableStateOf<Map<String, Any>?>(null) }
    var showResetPassword by remember { mutableStateOf<Map<String, Any>?>(null) }
    var todayAttendanceRecords by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(selectedStudent) {
        if (selectedStudent != null) {
            val startCal = java.util.Calendar.getInstance()
            startCal.set(java.util.Calendar.HOUR_OF_DAY, 0)
            startCal.set(java.util.Calendar.MINUTE, 0)
            val startTimestamp = startCal.timeInMillis
            
            val endCal = java.util.Calendar.getInstance()
            endCal.set(java.util.Calendar.HOUR_OF_DAY, 23)
            endCal.set(java.util.Calendar.MINUTE, 59)
            val endTimestamp = endCal.timeInMillis

            firestore.collection("attendance")
                .whereEqualTo("studentId", selectedStudent!!["id"])
                .whereGreaterThanOrEqualTo("timestamp", startTimestamp)
                .whereLessThanOrEqualTo("timestamp", endTimestamp)
                .get()
                .addOnSuccessListener { snapshot ->
                    todayAttendanceRecords = snapshot.documents.mapNotNull { it.data }
                }
        } else {
            todayAttendanceRecords = emptyList()
        }
    }

    Scaffold(
        topBar = { 
            TopAppBar(
                title = { Text("All Students") }, 
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }, 
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Teacher, titleContentColor = Color.White, navigationIconContentColor = Color.White)
            ) 
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().background(AppColors.Background)) {
            OutlinedTextField(
                value = search, 
                onValueChange = { search = it }, 
                label = { Text("Search by name, ID, course...") }, 
                leadingIcon = { Icon(Icons.Default.Search, "Search") }, 
                modifier = Modifier.fillMaxWidth().padding(16.dp), 
                shape = RoundedCornerShape(10.dp)
            )
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                items(filteredStudents.size) { index ->
                    val student = filteredStudents[index]
                    val name = student["fullName"] as? String ?: "Unknown"
                    val id = student["id"] as? String ?: ""
                    val profileUrl = student["profileUrl"] as? String
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { selectedStudent = student }, 
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (profileUrl.isNullOrEmpty()) {
                                Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(AppColors.Teacher), contentAlignment = Alignment.Center) { Text(name.firstOrNull()?.toString() ?: "?", color = Color.White, fontWeight = FontWeight.Bold) }
                            } else {
                                com.rajeducational.erp.ui.components.ProfileImage(
                                    urlOrBase64 = profileUrl,
                                    modifier = Modifier.size(40.dp).clip(CircleShape),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) { 
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(name, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f, fill = false))
                                    val isAttending = student["isAttending"] as? Boolean ?: true
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        color = if (isAttending) AppColors.Student.copy(alpha = 0.1f) else Color.Red.copy(alpha = 0.1f),
                                        contentColor = if (isAttending) AppColors.Student else Color.Red,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = if (isAttending) "Attending" else "Non-attending",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(id, fontSize = 12.sp, color = AppColors.TextSecondary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    AttendancePercentageBadge(
                                        studentId = id,
                                        backgroundColor = AppColors.Teacher.copy(alpha = 0.15f),
                                        textColor = AppColors.Teacher
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedStudent != null) {
        val student = selectedStudent!!
        AlertDialog(
            onDismissRequest = { selectedStudent = null },
            title = { Text("Student Details") },
            text = {
                Column {
                    Text("Name: ${student["fullName"] ?: ""}", fontWeight = FontWeight.Bold)
                    Text("ID: ${student["id"] ?: ""}")
                    Text("Email: ${student["email"] ?: ""}")
                    Text("Course: ${student["course"] ?: ""}")
                    Text("Session: ${student["session"] ?: ""}")
                    Text("Batch: ${student["batch"] ?: ""}")
                    Text("Year: ${student["year"] ?: ""}")
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    AttendanceStatsCard(
                        studentId = student["id"] as? String ?: "",
                        cardColor = AppColors.Teacher.copy(alpha = 0.05f)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Today's Attendance Status", fontWeight = FontWeight.Bold, color = AppColors.Teacher)
                    if (todayAttendanceRecords.isEmpty()) {
                        Text("No attendance recorded today.", fontSize = 14.sp)
                    } else {
                        val timeFormat = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
                        todayAttendanceRecords.sortedBy { it["timestamp"] as? Long ?: 0L }.forEach { record ->
                            val type = record["type"] as? String ?: ""
                            val status = record["status"] as? String ?: ""
                            val ts = record["timestamp"] as? Long ?: 0L
                            val timeStr = if (ts > 0) timeFormat.format(java.util.Date(ts)) else "--"
                            Text("• $type ($timeStr) - $status", fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedStudent = null }) {
                    Text("Close")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetPassword = student; selectedStudent = null }) {
                    Text("Reset Password", color = AppColors.Teacher)
                }
            }
        )
    }

    if (showResetPassword != null) {
        val student = showResetPassword!!
        val docId = student["docId"] as? String
        var newPassword by remember { mutableStateOf("") }
        var isSaving by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showResetPassword = null },
            title = { Text("Reset Password") },
            text = {
                Column {
                    Text("Student: ${student["fullName"]}")
                    Text("Current Password: ${student["password"] ?: "Not Set"}")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("New Password") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newPassword.isNotBlank() && docId != null) {
                            isSaving = true
                            firestore.collection("students").document(docId)
                                .update("password", newPassword)
                                .addOnSuccessListener {
                                    isSaving = false
                                    android.widget.Toast.makeText(context, "Password reset successfully", android.widget.Toast.LENGTH_SHORT).show()
                                    showResetPassword = null
                                }
                        }
                    },
                    enabled = !isSaving
                ) {
                    Text(if (isSaving) "Saving..." else "Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetPassword = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}