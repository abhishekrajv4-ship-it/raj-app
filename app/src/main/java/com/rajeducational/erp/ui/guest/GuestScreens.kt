package com.rajeducational.erp.ui.guest

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.ui.graphics.graphicsLayer
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore
import com.rajeducational.erp.data.College
import com.rajeducational.erp.data.GalleryPhoto
import com.rajeducational.erp.data.Event
import com.rajeducational.erp.theme.AppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuestPortalScreen(navController: NavController) {
    var selectedTab by remember { mutableStateOf(0) }
    var colleges by remember { mutableStateOf<List<College>>(emptyList()) }
    var totalCourses by remember { mutableStateOf(0) }
    var photos by remember { mutableStateOf<List<GalleryPhoto>>(emptyList()) }
    var announcements by remember { mutableStateOf<List<com.rajeducational.erp.data.Announcement>>(emptyList()) }
    var hasUnreadGuestReplies by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    
    LaunchedEffect(Unit) {
        FirebaseFirestore.getInstance().collection("colleges").addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc -> doc.toObject(College::class.java)?.copy(id = doc.id) }
                colleges = list
                totalCourses = list.sumOf { it.courses.size }
            }
        }
        FirebaseFirestore.getInstance().collection("gallery_photos").addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                photos = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(GalleryPhoto::class.java)?.copy(id = doc.id)
                }
            }
        }
        FirebaseFirestore.getInstance().collection("announcements")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    announcements = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(com.rajeducational.erp.data.Announcement::class.java)?.copy(id = doc.id)
                    }
                }
            }
            
        val sharedPrefs = context.getSharedPreferences("GuestPrefs", android.content.Context.MODE_PRIVATE)
        val guestId = sharedPrefs.getString("guest_id", null)
        if (guestId != null) {
            FirebaseFirestore.getInstance().collection("guest_messages")
                .whereEqualTo("guestId", guestId)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        hasUnreadGuestReplies = snapshot.documents.any { doc ->
                            doc.getBoolean("readByGuest") == false || doc.getBoolean("isReadByGuest") == false
                        }
                    }
                }
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                listOf("Overview" to Icons.Default.Home, "Courses" to Icons.Default.Book, "Fees" to Icons.Default.Payment, "Gallery" to Icons.Default.Image, "Events" to Icons.Default.Event, "Contact" to Icons.Default.Email).forEachIndexed { i, (l, ic) ->
                    NavigationBarItem(
                        selected = i == selectedTab, 
                        onClick = { selectedTab = i }, 
                        icon = { 
                            Icon(ic, l, modifier = Modifier.size(20.dp)) 
                        }, 
                        label = { Text(l, fontSize = 9.sp) }, 
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = AppColors.Guest)
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                0 -> {
                    var galleryDescription by remember { mutableStateOf("") }
                    
                    LaunchedEffect(Unit) {
                        FirebaseFirestore.getInstance().collection("app_settings").document("general").addSnapshotListener { doc, _ ->
                            if (doc != null && doc.exists()) {
                                doc.getString("galleryDescription")?.let { galleryDescription = it }
                            }
                        }
                    }
                    Scaffold(topBar = { TopAppBar(title = { Text("Guest Portal", fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, "Back") } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Guest, titleContentColor = Color.White, navigationIconContentColor = Color.White)) }) { innerPadding ->
                        Column(modifier = Modifier.padding(innerPadding).fillMaxSize().background(AppColors.Background).verticalScroll(rememberScrollState())) {
                            if (photos.isNotEmpty()) {
                                val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { photos.size })
                                LaunchedEffect(pagerState) {
                                    while (true) {
                                        kotlinx.coroutines.delay(2000)
                                        val nextPage = (pagerState.currentPage + 1) % photos.size
                                        pagerState.animateScrollToPage(nextPage)
                                    }
                                }
                                androidx.compose.foundation.pager.HorizontalPager(
                                    state = pagerState,
                                    modifier = Modifier.fillMaxWidth().height(280.dp)
                                ) { page ->
                                    val photo = photos[page]
                                    var url = photo.viewUrl
                                    if (!url.startsWith("http")) url = "https://rajapp.matavaishnavieducationaltrust.org/$url"
                                    coil.compose.AsyncImage(
                                        model = url,
                                        contentDescription = photo.name,
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                if (galleryDescription.isNotBlank()) {
                                    Text(
                                        text = galleryDescription,
                                        fontSize = 14.sp,
                                        color = AppColors.TextSecondary,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                    )
                                }
                            }
                            
                            // Stats
                            Row(modifier = Modifier.fillMaxWidth().padding(16.dp).background(AppColors.Navy, RoundedCornerShape(16.dp)).padding(16.dp), horizontalArrangement = Arrangement.SpaceAround) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(colleges.size.toString(), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White); Text("Colleges", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f)) }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(totalCourses.toString(), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White); Text("Courses", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f)) }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("3", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White); Text("Students", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f)) }
                            }
                            // Colleges
                            Text("Our Colleges", fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.padding(start = 16.dp, top = 8.dp))
                            colleges.forEach { college ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).clickable {
                                        navController.navigate("guest_college_detail/${college.id}")
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Business, college.name, tint = AppColors.Guest); Spacer(modifier = Modifier.width(12.dp)); Text(college.name, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                            if (colleges.isEmpty()) {
                                Text("No colleges available.", modifier = Modifier.padding(16.dp), color = AppColors.TextSecondary)
                            }
                            
                            // Announcements
                            if (announcements.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Common Announcements", fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.padding(start = 16.dp, bottom = 8.dp))
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
                                            Text("By $sender | $dateString", fontSize = 12.sp, color = AppColors.Guest)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            
                                            // Handle description with possible URL
                                            val context = androidx.compose.ui.platform.LocalContext.current
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

                            Spacer(modifier = Modifier.height(64.dp)) // Added space at the bottom
                        }
                    }
                }
                1 -> GuestCoursesScreen(navController)
                2 -> GuestFeesScreen(navController)
                3 -> GuestGalleryScreen(navController)
                4 -> GuestEventsScreen(navController)
                5 -> GuestContactScreen(navController)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuestCollegeDetailScreen(navController: NavController, collegeId: String) {
    var college by remember { mutableStateOf<College?>(null) }
    
    LaunchedEffect(collegeId) {
        if (collegeId.isNotEmpty()) {
            FirebaseFirestore.getInstance().collection("colleges").document(collegeId).addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    college = snapshot.toObject(College::class.java)?.copy(id = snapshot.id)
                }
            }
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text(college?.name ?: "Loading...") }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, "Back") } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Guest, titleContentColor = Color.White, navigationIconContentColor = Color.White)) }) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().background(AppColors.Background).verticalScroll(rememberScrollState()).padding(16.dp)) {
            college?.let { c ->
                Text("Courses Offered", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AppColors.Navy, modifier = Modifier.padding(bottom = 12.dp))
                if (c.courses.isEmpty()) {
                    Text("No courses available.", color = AppColors.TextSecondary)
                } else {
                    c.courses.forEach { course ->
                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), shape = RoundedCornerShape(12.dp)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(course.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AppColors.Navy)
                                if (course.yearBatches.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Batches: ${course.yearBatches.joinToString(", ")}", fontSize = 14.sp, color = AppColors.TextSecondary)
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
fun GuestCoursesScreen(navController: NavController) {
    var colleges by remember { mutableStateOf<List<College>>(emptyList()) }
    
    LaunchedEffect(Unit) {
        FirebaseFirestore.getInstance().collection("colleges").addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc -> doc.toObject(College::class.java)?.copy(id = doc.id) }
                colleges = list
            }
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Courses") }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, "Back") } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Guest, titleContentColor = Color.White, navigationIconContentColor = Color.White)) }) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().background(AppColors.Background).verticalScroll(rememberScrollState()).padding(16.dp)) {
            colleges.forEach { college ->
                if (college.courses.isNotEmpty()) {
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), shape = RoundedCornerShape(12.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(college.name, fontWeight = FontWeight.Bold, color = AppColors.Navy, modifier = Modifier.padding(bottom = 8.dp))
                            college.courses.forEach { c -> 
                                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                                    Text("• ${c.name}", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = AppColors.TextPrimary)
                                    if (c.yearBatches.isNotEmpty()) {
                                        Text("  Batches: ${c.yearBatches.joinToString(", ")}", fontSize = 13.sp, color = AppColors.TextSecondary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (colleges.isEmpty()) {
                Text("No courses available.", modifier = Modifier.padding(16.dp), color = AppColors.TextSecondary)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuestFeesScreen(navController: NavController) {
    var colleges by remember { mutableStateOf<List<College>>(emptyList()) }
    
    LaunchedEffect(Unit) {
        FirebaseFirestore.getInstance().collection("colleges").addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc -> doc.toObject(College::class.java)?.copy(id = doc.id) }
                colleges = list
            }
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Fee Structure") }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, "Back") } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Guest, titleContentColor = Color.White, navigationIconContentColor = Color.White)) }) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().background(AppColors.Background).verticalScroll(rememberScrollState()).padding(16.dp)) {
            Text("Select a College", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AppColors.Navy, modifier = Modifier.padding(bottom = 12.dp))
            if (colleges.isEmpty()) {
                Text("No colleges available.", color = AppColors.TextSecondary)
            } else {
                colleges.forEach { college ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable {
                            navController.navigate("guest_college_fee_detail/${college.id}")
                        }, 
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Business, contentDescription = null, tint = AppColors.Guest)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(college.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AppColors.Navy)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuestCollegeFeeDetailScreen(navController: NavController, collegeId: String) {
    var college by remember { mutableStateOf<College?>(null) }
    
    LaunchedEffect(collegeId) {
        if (collegeId.isNotEmpty()) {
            FirebaseFirestore.getInstance().collection("colleges").document(collegeId).addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    college = snapshot.toObject(College::class.java)?.copy(id = snapshot.id)
                }
            }
        }
    }

    val context = LocalContext.current

    Scaffold(topBar = { TopAppBar(title = { Text(college?.name ?: "Fee Structure") }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, "Back") } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Guest, titleContentColor = Color.White, navigationIconContentColor = Color.White)) }) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().background(AppColors.Background).verticalScroll(rememberScrollState()).padding(16.dp)) {
            college?.let { c ->
                if (c.courses.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 32.dp), contentAlignment = Alignment.Center) {
                        Text("No courses found for this college.", color = AppColors.TextSecondary)
                    }
                } else {
                    c.courses.forEach { course ->
                        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), shape = RoundedCornerShape(12.dp)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(course.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AppColors.Navy)
                                Spacer(modifier = Modifier.height(12.dp))

                                if (course.feeStructureText.isNotEmpty()) {
                                    Text("Description", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = AppColors.Navy)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(course.feeStructureText, fontSize = 14.sp, color = AppColors.TextPrimary)
                                    Spacer(modifier = Modifier.height(12.dp))
                                }

                                if (course.feeFiles.isNotEmpty()) {
                                    Text("Attachments", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = AppColors.Navy, modifier = Modifier.padding(bottom = 4.dp))
                                    course.feeFiles.forEach { file ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable {
                                                try {
                                                    var url = file.viewUrl
                                                    if (!url.startsWith("http")) {
                                                        url = "https://rajapp.matavaishnavieducationaltrust.org/$url"
                                                    }
                                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
                                                    intent.data = android.net.Uri.parse(url)
                                                    context.startActivity(intent)
                                                } catch (e: Exception) {
                                                    android.widget.Toast.makeText(context, "No app found to open this file", android.widget.Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                                        ) {
                                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Description, contentDescription = null, tint = AppColors.Guest)
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(file.originalName, modifier = Modifier.weight(1f), fontSize = 14.sp, color = AppColors.Navy)
                                            }
                                        }
                                    }
                                } else if (course.feeStructureText.isEmpty()) {
                                    Text("Fee information not yet published for this course", color = AppColors.TextSecondary, fontSize = 13.sp)
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
fun GuestGalleryScreen(navController: NavController) {
    val firestore = FirebaseFirestore.getInstance()
    var photos by remember { mutableStateOf<List<GalleryPhoto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
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

    var selectedPhotoIndex by remember { mutableStateOf<Int?>(null) }
    
    // Broadcast receiver for download completion
    var downloadCompleteMessage by remember { mutableStateOf<String?>(null) }
    DisposableEffect(context) {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
                if (intent?.action == android.app.DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
                    downloadCompleteMessage = "Downloaded successfully"
                }
            }
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, android.content.IntentFilter(android.app.DownloadManager.ACTION_DOWNLOAD_COMPLETE), android.content.Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(receiver, android.content.IntentFilter(android.app.DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        }
        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gallery") },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Guest, titleContentColor = Color.White, navigationIconContentColor = Color.White)
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
                    CircularProgressIndicator(color = AppColors.Guest)
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
                        modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).background(Color.Black.copy(alpha=0.5f), androidx.compose.foundation.shape.CircleShape)
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
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.Guest),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuestEventsScreen(navController: NavController) {
    var events by remember { mutableStateOf<List<Event>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        FirebaseFirestore.getInstance().collection("events").addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                events = snapshot.documents.mapNotNull { doc -> doc.toObject(Event::class.java)?.copy(id = doc.id) }
                isLoading = false
            }
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Events") }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, "Back") } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Guest, titleContentColor = Color.White, navigationIconContentColor = Color.White)) }) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().background(AppColors.Background).verticalScroll(rememberScrollState()).padding(16.dp)) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AppColors.Guest)
                }
            } else if (events.isEmpty()) {
                Text("No events scheduled.", color = AppColors.TextSecondary)
            } else {
                events.forEach { event ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable {
                            navController.navigate("guest_event_detail/${event.id}")
                        }, 
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)), shape = RoundedCornerShape(12.dp)) {
                                Column(modifier = Modifier.padding(12.dp).width(40.dp), horizontalAlignment = Alignment.CenterHorizontally) { 
                                    Text(event.date.take(2), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AppColors.Guest) 
                                    Text(event.month.take(3), fontSize = 12.sp, color = AppColors.Guest) 
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) { 
                                Text(event.name, fontWeight = FontWeight.Bold, color = AppColors.Navy, fontSize = 18.sp) 
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(event.place, fontSize = 14.sp, color = AppColors.TextSecondary) 
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("To see the photos click here.", fontSize = 12.sp, color = AppColors.Guest)
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
fun GuestEventDetailScreen(navController: NavController, eventId: String) {
    var event by remember { mutableStateOf<Event?>(null) }
    var selectedPhotoIndex by remember { mutableStateOf<Int?>(null) }
    val context = LocalContext.current
    
    LaunchedEffect(eventId) {
        if (eventId.isNotEmpty()) {
            FirebaseFirestore.getInstance().collection("events").document(eventId).addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    event = snapshot.toObject(Event::class.java)?.copy(id = snapshot.id)
                }
            }
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text(event?.name ?: "Event Details") }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, "Back") } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Guest, titleContentColor = Color.White, navigationIconContentColor = Color.White)) }) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().background(AppColors.Background).verticalScroll(rememberScrollState())) {
            event?.let { e ->
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(e.name, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = AppColors.Navy)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Event, contentDescription = null, tint = AppColors.Guest, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("${e.date} ${e.month}", color = AppColors.TextSecondary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = AppColors.Guest, modifier = Modifier.size(16.dp))
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
                        androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                            columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(3),
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
                                        contentDescription = photo.name,
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        
        if (selectedPhotoIndex != null && event != null) {
            val photos = event!!.photos
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
                        modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).background(Color.Black.copy(alpha=0.5f), androidx.compose.foundation.shape.CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuestContactScreen(navController: NavController) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val sharedPrefs = context.getSharedPreferences("GuestPrefs", android.content.Context.MODE_PRIVATE)
    var guestId = sharedPrefs.getString("guest_id", null)
    if (guestId == null) {
        guestId = java.util.UUID.randomUUID().toString()
        sharedPrefs.edit().putString("guest_id", guestId).apply()
    }
    
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }

    var collegeName by remember { mutableStateOf("") }
    var collegeAddress by remember { mutableStateOf("") }
    var collegeDesc by remember { mutableStateOf("") }
    
    var pastMessages by remember { mutableStateOf<List<com.rajeducational.erp.data.GuestMessage>>(emptyList()) }
    val firestore = FirebaseFirestore.getInstance()

    LaunchedEffect(Unit) {
        firestore.collection("app_settings").document("general").addSnapshotListener { doc, _ ->
            if (doc != null && doc.exists()) {
                doc.getString("name")?.let { if (it.isNotBlank()) collegeName = it }
                doc.getString("address")?.let { collegeAddress = it }
                doc.getString("description")?.let { if (it.isNotBlank()) collegeDesc = it }
            }
        }
        
        firestore.collection("guest_messages")
            .whereEqualTo("guestId", guestId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val msgs = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(com.rajeducational.erp.data.GuestMessage::class.java)?.copy(id = doc.id)
                    }
                    pastMessages = msgs.sortedByDescending { it.timestamp }
                }
            }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Contact Us") }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, "Back") } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Guest, titleContentColor = Color.White, navigationIconContentColor = Color.White)) }) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().background(AppColors.Background).verticalScroll(rememberScrollState()).padding(16.dp)) {
            Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(if (collegeName.isNotBlank()) collegeName else "Raj Educational Group", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AppColors.Navy)
                    if (collegeAddress.isNotBlank()) {
                        Row(modifier = Modifier.padding(top = 4.dp)) { 
                            Icon(Icons.Default.LocationOn, "Loc", tint = AppColors.Guest, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(collegeAddress, fontSize = 14.sp, color = AppColors.TextSecondary) 
                        }
                    }
                    if (collegeDesc.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(collegeDesc, fontSize = 14.sp, color = AppColors.TextPrimary)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Send us a message", fontWeight = FontWeight.SemiBold, color = AppColors.Navy)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Your name (compulsory)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone number (compulsory)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone))
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Your email (optional)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = message, onValueChange = { message = it }, label = { Text("Your message (optional)") }, modifier = Modifier.fillMaxWidth().height(100.dp), shape = RoundedCornerShape(10.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank() && phone.isNotBlank()) {
                                isSending = true
                                val newMsg = com.rajeducational.erp.data.GuestMessage(
                                    guestId = guestId!!,
                                    name = name,
                                    phone = phone,
                                    email = email,
                                    message = message,
                                    timestamp = System.currentTimeMillis()
                                )
                                FirebaseFirestore.getInstance().collection("guest_messages").add(newMsg)
                                    .addOnSuccessListener {
                                        isSending = false
                                        name = ""
                                        phone = ""
                                        email = ""
                                        message = ""
                                        android.widget.Toast.makeText(context, "Message sent!", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                    .addOnFailureListener {
                                        isSending = false
                                        android.widget.Toast.makeText(context, "Failed to send", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                            } else {
                                android.widget.Toast.makeText(context, "Please fill compulsory fields", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }, 
                        modifier = Modifier.fillMaxWidth(), 
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.Guest), 
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isSending
                    ) { 
                        Text(if (isSending) "Sending..." else "Send Message") 
                    }
                }
            }
            
            if (pastMessages.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Text("Your Messages", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AppColors.Navy)
                Spacer(modifier = Modifier.height(12.dp))
                
                pastMessages.forEach { msg ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .clickable {
                                if (!msg.readByGuest) {
                                    firestore.collection("guest_messages").document(msg.id).update("readByGuest", true)
                                }
                            }, 
                        shape = RoundedCornerShape(12.dp), 
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text("To: Admin", fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                if (!msg.readByGuest) {
                                    androidx.compose.foundation.layout.Box(modifier = Modifier.size(10.dp).background(Color.Red, androidx.compose.foundation.shape.CircleShape))
                                }
                            }
                            Text(if (msg.message.isNotBlank()) msg.message else "(No message content)", fontSize = 14.sp, color = AppColors.TextSecondary)
                            
                            if (msg.reply.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Card(colors = CardDefaults.cardColors(containerColor = AppColors.Guest.copy(alpha = 0.1f)), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("Reply from Admin:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = AppColors.Guest)
                                        Text(msg.reply, fontSize = 14.sp, color = AppColors.TextPrimary)
                                    }
                                }
                            } else {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Awaiting reply...", fontSize = 12.sp, color = Color.Gray, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
