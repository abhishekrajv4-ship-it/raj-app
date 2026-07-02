package com.rajeducational.erp.ui.landing

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.rajeducational.erp.theme.AppColors

import com.google.firebase.firestore.FirebaseFirestore
import com.rajeducational.erp.data.College

data class RoleTile(val key: String, val label: String, val desc: String, val icon: String, val color: Color)

@Composable
fun LandingScreen(navController: NavController) {
    val roles = listOf(
        RoleTile("student", "Student", "Access your dashboard", "school", AppColors.Student),
        RoleTile("teacher", "Teacher and Staff", "Manage your classes", "easel", AppColors.Teacher),
        RoleTile("guest", "Guest", "Explore our institution", "eye", AppColors.Guest),
        RoleTile("admin", "Admin", "Control centre", "shield", AppColors.Admin),
    )

    var collegesCount by remember { mutableStateOf(0) }
    var coursesCount by remember { mutableStateOf(0) }
    var collegeName by remember { mutableStateOf("Raj Educational Group") }
    var collegeAddress by remember { mutableStateOf("") }
    var collegeDesc by remember { mutableStateOf("Excellence in Education") }
    var collegeLogoUrl by remember { mutableStateOf("") }
    var galleryPhotos by remember { mutableStateOf<List<com.rajeducational.erp.data.GalleryPhoto>>(emptyList()) }
    
    var whatsappLink by remember { mutableStateOf("https://whatsapp.com/channel/0029VamEW996xCSGsYZevr1I") }
    var instagramLink by remember { mutableStateOf("https://www.instagram.com/rajeducationalgroup.official/") }
    var facebookLink by remember { mutableStateOf("https://www.facebook.com/rajeducationalgroup") }
    var youtubeLink by remember { mutableStateOf("https://www.youtube.com/@Rajeducationalgroupofficial") }
    var websiteLink by remember { mutableStateOf("https://rajeducationalgroup.org") }
    var phoneNumber by remember { mutableStateOf("7485036111") }
    
    var hasUnreadGuestReplies by remember { mutableStateOf(false) }
    var hasUnreadAdminMessages by remember { mutableStateOf(false) }
    var showTeacherStaffDialog by remember { mutableStateOf(false) }
    var showStudentTypeDialog by remember { mutableStateOf(false) }
    var showStudentApprovedDialog by remember { mutableStateOf(false) }
    var approvedStudentId by remember { mutableStateOf<String?>(null) }
    var approvedStudentDoc by remember { mutableStateOf<com.google.firebase.firestore.DocumentSnapshot?>(null) }
    
    val context = androidx.compose.ui.platform.LocalContext.current

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { }

    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("colleges").addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc -> doc.toObject(College::class.java) }
                collegesCount = list.size
                coursesCount = list.sumOf { it.courses.size }
            }
        }
        firestore.collection("app_settings").document("general").addSnapshotListener { doc, _ ->
            if (doc != null && doc.exists()) {
                doc.getString("name")?.let { if (it.isNotBlank()) collegeName = it }
                doc.getString("address")?.let { collegeAddress = it }
                doc.getString("description")?.let { if (it.isNotBlank()) collegeDesc = it }
                doc.getString("logoUrl")?.let { collegeLogoUrl = it }
                doc.getString("whatsappLink")?.let { if (it.isNotBlank()) whatsappLink = it }
                doc.getString("instagramLink")?.let { if (it.isNotBlank()) instagramLink = it }
                doc.getString("facebookLink")?.let { if (it.isNotBlank()) facebookLink = it }
                doc.getString("youtubeLink")?.let { if (it.isNotBlank()) youtubeLink = it }
                doc.getString("websiteLink")?.let { if (it.isNotBlank()) websiteLink = it }
                doc.getString("phoneNumber")?.let { if (it.isNotBlank()) phoneNumber = it }
            }
        }
        firestore.collection("gallery_photos").addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                galleryPhotos = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(com.rajeducational.erp.data.GalleryPhoto::class.java)?.copy(id = doc.id)
                }
            }
        }
        
        // Admin unread check
        var adminPrevUnreadCount = -1
        firestore.collection("guest_messages")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val unreadDocs = snapshot.documents.filter { doc ->
                        val rAdmin = doc.getBoolean("readByAdmin")
                        val isRAdmin = doc.getBoolean("isReadByAdmin")
                        rAdmin == false || isRAdmin == false || (rAdmin == null && isRAdmin == null)
                    }
                    hasUnreadAdminMessages = unreadDocs.isNotEmpty()
                    
                    if (adminPrevUnreadCount != -1 && unreadDocs.size > adminPrevUnreadCount) {
                        com.rajeducational.erp.util.NotificationHelper.showNotification(context, "New Guest Message", "You have a new message from a guest.")
                    }
                    adminPrevUnreadCount = unreadDocs.size
                }
            }
            
        // Guest unread check
        var guestPrevUnreadCount = -1
        val sharedPrefs = context.getSharedPreferences("GuestPrefs", android.content.Context.MODE_PRIVATE)
        val guestId = sharedPrefs.getString("guest_id", null)
        if (guestId != null) {
            firestore.collection("guest_messages")
                .whereEqualTo("guestId", guestId)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        val unreadDocs = snapshot.documents.filter { doc ->
                            doc.getBoolean("readByGuest") == false || doc.getBoolean("isReadByGuest") == false
                        }
                        hasUnreadGuestReplies = unreadDocs.isNotEmpty()
                        
                        if (guestPrevUnreadCount != -1 && unreadDocs.size > guestPrevUnreadCount) {
                            com.rajeducational.erp.util.NotificationHelper.showNotification(context, "New Reply", "You have a new reply from the Admin.")
                        }
                        guestPrevUnreadCount = unreadDocs.size
                    }
                }
        }
        
        // Check for pending student approval
        val studentPrefs = context.getSharedPreferences("StudentPrefs", android.content.Context.MODE_PRIVATE)
        val pendingStudentId = studentPrefs.getString("pending_student_id", null)
        if (pendingStudentId != null) {
            firestore.collection("students").document(pendingStudentId)
                .addSnapshotListener { doc, _ ->
                    if (doc != null && doc.exists()) {
                        val status = doc.getString("approvalStatus")
                        if (status == "approved") {
                            showStudentApprovedDialog = true
                            approvedStudentId = pendingStudentId
                            approvedStudentDoc = doc
                        }
                    }
                }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .verticalScroll(rememberScrollState())
    ) {
        // Header with Logo
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                .padding(vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (collegeLogoUrl.isNotEmpty()) {
                var actualUrl = collegeLogoUrl
                if (!actualUrl.startsWith("http")) actualUrl = "https://rajapp.matavaishnavieducationaltrust.org/$actualUrl"
                
                coil.compose.AsyncImage(
                    model = actualUrl,
                    contentDescription = "College Logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(200.dp).clip(RoundedCornerShape(16.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(AppColors.Navy),
                    contentAlignment = Alignment.Center
                ) {
                    Text(collegeName.take(3).uppercase(), color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                collegeName,
                fontSize = 22.sp, fontWeight = FontWeight.Bold, color = AppColors.Navy,
                textAlign = TextAlign.Center
            )
            if (collegeAddress.isNotBlank()) {
                Text(
                    collegeAddress, 
                    fontSize = 14.sp, 
                    color = AppColors.TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
            Text(collegeDesc, fontSize = 14.sp, color = AppColors.TextSecondary)
        }

        // Social Links
        @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
        androidx.compose.foundation.layout.FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SocialButton("WhatsApp", Color(0xFF25D366)) {
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(whatsappLink))
                try { context.startActivity(intent) } catch (e: Exception) { }
            }
            SocialButton("Instagram", Color(0xFFE1306C)) {
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(instagramLink))
                try { context.startActivity(intent) } catch (e: Exception) { }
            }
            SocialButton("Facebook", Color(0xFF1877F2)) {
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(facebookLink))
                try { context.startActivity(intent) } catch (e: Exception) { }
            }
            SocialButton("YouTube", Color(0xFFFF0000)) {
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(youtubeLink))
                try { context.startActivity(intent) } catch (e: Exception) { }
            }
            SocialButton("Website", Color(0xFF333333)) {
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(websiteLink))
                try { context.startActivity(intent) } catch (e: Exception) { }
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text("Phone: $phoneNumber", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = AppColors.Navy)
            Spacer(modifier = Modifier.width(16.dp))
            Button(
                onClick = {
                    val intent = android.content.Intent(android.content.Intent.ACTION_DIAL, android.net.Uri.parse("tel:$phoneNumber"))
                    try { context.startActivity(intent) } catch (e: Exception) { }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                shape = RoundedCornerShape(24.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Icon(androidx.compose.material.icons.Icons.Default.Phone, contentDescription = "Call", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Call Now", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        // Stats Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .background(AppColors.Navy, RoundedCornerShape(16.dp))
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            StatItem(collegesCount.toString(), "Colleges")
            StatItem(coursesCount.toString(), "Courses")
            StatItem("3", "Students")
        }

        // Section Title
        Text(
            "Select your role",
            fontSize = 17.sp, fontWeight = FontWeight.Bold, color = AppColors.Navy,
            modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 12.dp)
        )

        // Role Tiles - 2x2 Grid
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                roles.take(2).forEach { role ->
                    RoleTileCard(
                        role = role,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            when (role.key) {
                                "student" -> {
                                    val studentPrefsLocal = context.getSharedPreferences("StudentPrefs", android.content.Context.MODE_PRIVATE)
                                    val localPendingId = studentPrefsLocal.getString("pending_student_id", null)
                                    val isLoggedIn = studentPrefsLocal.getBoolean("is_logged_in", false)
                                    if (isLoggedIn) {
                                        navController.navigate("student_dashboard")
                                    } else if (localPendingId != null) {
                                        android.widget.Toast.makeText(context, "Kindly wait for your confirmation. You will be approved shortly.", android.widget.Toast.LENGTH_LONG).show()
                                    } else {
                                        showStudentTypeDialog = true
                                    }
                                }
                                "teacher" -> showTeacherStaffDialog = true
                                "guest" -> navController.navigate("guest_portal")
                                "admin" -> navController.navigate("admin_panel")
                            }
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                roles.drop(2).forEach { role ->
                    RoleTileCard(
                        role = role,
                        modifier = Modifier.weight(1f),
                        showNotification = if (role.key == "admin") hasUnreadAdminMessages else false,
                        onClick = {
                            when (role.key) {
                                "guest" -> navController.navigate("guest_portal")
                                "admin" -> navController.navigate("admin_panel")
                            }
                        }
                    )
                }
            }
        }

        if (galleryPhotos.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Gallery",
                fontSize = 17.sp, fontWeight = FontWeight.Bold, color = AppColors.Navy,
                modifier = Modifier.padding(start = 16.dp, bottom = 12.dp)
            )
            val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { galleryPhotos.size })
            LaunchedEffect(pagerState) {
                while (true) {
                    kotlinx.coroutines.delay(2000)
                    if (galleryPhotos.isNotEmpty()) {
                        val nextPage = (pagerState.currentPage + 1) % galleryPhotos.size
                        pagerState.animateScrollToPage(nextPage)
                    }
                }
            }
            androidx.compose.foundation.pager.HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth().height(280.dp).padding(bottom = 24.dp)
            ) { page ->
                val photo = galleryPhotos[page]
                var url = photo.viewUrl
                if (!url.startsWith("http")) url = "https://rajapp.matavaishnavieducationaltrust.org/$url"
                coil.compose.AsyncImage(
                    model = url,
                    contentDescription = photo.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            Spacer(modifier = Modifier.height(24.dp))
        }

        
        Spacer(modifier = Modifier.height(64.dp)) // Added space at the bottom
    }

    if (showTeacherStaffDialog) {
        AlertDialog(
            onDismissRequest = { showTeacherStaffDialog = false },
            title = { Text("Select Role") },
            text = { Text("Are you a teacher or are you staff?") },
            confirmButton = {
                TextButton(onClick = { 
                    showTeacherStaffDialog = false
                    navController.navigate("teacher_auth")
                }) {
                    Text("Teacher")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showTeacherStaffDialog = false
                    navController.navigate("staff_auth")
                }) {
                    Text("Staff", color = AppColors.Staff)
                }
            }
        )
    }

    if (showStudentTypeDialog) {
        AlertDialog(
            onDismissRequest = { showStudentTypeDialog = false },
            title = { Text("Student Selection", fontWeight = FontWeight.Bold, color = AppColors.Navy) },
            text = { Text("Are you an attending student or a non-attending student?") },
            confirmButton = {
                TextButton(onClick = { 
                    showStudentTypeDialog = false
                    navController.navigate("student_auth")
                }) {
                    Text("Attending Student", fontWeight = FontWeight.Bold, color = AppColors.Student)
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showStudentTypeDialog = false
                    navController.navigate("non_attending_student_auth")
                }) {
                    Text("Non-Attending Student", fontWeight = FontWeight.Bold, color = Color.Red)
                }
            }
        )
    }

    if (showStudentApprovedDialog && approvedStudentDoc != null && approvedStudentId != null) {
        AlertDialog(
            onDismissRequest = { }, // Force user to click OK
            title = { Text("Registration Approved", fontWeight = FontWeight.Bold, color = AppColors.Student) },
            text = { Text("You are registered successfully. You can now use your portal.") },
            confirmButton = {
                TextButton(onClick = {
                    showStudentApprovedDialog = false
                    val doc = approvedStudentDoc!!
                    val id = approvedStudentId!!
                    
                    val prefs = context.getSharedPreferences("StudentPrefs", android.content.Context.MODE_PRIVATE)
                    prefs.edit().apply {
                        putString("student_id", doc.id)
                        putString("student_name", doc.getString("fullName"))
                        putString("student_phone", doc.getString("phone"))
                        putString("student_email", doc.getString("email"))
                        putString("student_address", doc.getString("address"))
                        putString("student_college", doc.getString("college"))
                        putString("student_course", doc.getString("course"))
                        putString("student_session", doc.getString("session"))
                        putBoolean("is_logged_in", true)
                        remove("pending_student_id") // Clear pending
                        apply()
                    }
                    val isAttending = doc.getBoolean("isAttending") ?: true
                    if (isAttending) {
                        navController.navigate("face_registration/student/$id") {
                            popUpTo("landing")
                        }
                    } else {
                        navController.navigate("student_dashboard") {
                            popUpTo("landing")
                        }
                    }
                }) {
                    Text("OK", fontWeight = FontWeight.Bold, color = AppColors.Student)
                }
            }
        )
    }
}

@Composable
fun RoleTileCard(role: RoleTile, modifier: Modifier = Modifier, showNotification: Boolean = false, onClick: () -> Unit) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.5.dp, role.color)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(role.color.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    val iconVector = when (role.icon) {
                        "school" -> Icons.Default.School
                        "easel" -> Icons.Default.CoPresent
                        "eye" -> Icons.Default.Visibility
                        "shield" -> Icons.Default.Shield
                        else -> Icons.Default.Person
                    }
                    Icon(iconVector, contentDescription = role.label, tint = role.color, modifier = Modifier.size(28.dp))
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(role.label, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = role.color)
                Text(role.desc, fontSize = 12.sp, color = AppColors.TextSecondary, textAlign = TextAlign.Center)
            }
            if (showNotification) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(12.dp)
                        .background(Color.Red, CircleShape)
                )
            }
        }
    }
}


@Composable
fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text(label, fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
    }
}

@Composable
fun SocialButton(text: String, color: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(24.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}
