const fs = require('fs');
let file = fs.readFileSync('app/src/main/java/com/rajeducational/erp/ui/staff/StaffScreens.kt', 'utf8');

const search = `var secondsRemaining by remember { mutableIntStateOf(15) }

    LaunchedEffect(showQrDialog, staffId) {`;

const replace = `var secondsRemaining by remember { mutableIntStateOf(15) }
    var expandedAdminFeatures by remember { mutableStateOf(false) }
    var adminFeatures by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }

    LaunchedEffect(staffId) {
        if (staffId.isNotEmpty()) {
            try {
                val doc = com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("staffs").document(staffId).get().kotlinx.coroutines.tasks.await()
                val features = doc.get("adminFeatures") as? Map<String, Boolean> ?: emptyMap()
                adminFeatures = features
            } catch(e: Exception) {}
        }
    }

    LaunchedEffect(showQrDialog, staffId) {`;

file = file.replace(search, replace);

const search2 = `Card(
                modifier = Modifier.fillMaxWidth().clickable { navController.navigate("admin_gallery_upload_center") },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PhotoLibrary, "Gallery Photo Upload Center", tint = AppColors.Staff)
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
                    Icon(Icons.Default.Event, "Event Control Center", tint = AppColors.Staff)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Event Control Center", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ChevronRight, "Go", tint = AppColors.TextSecondary)
                }
            }`;

const replace2 = `val activeFeatures = adminFeatures.filterValues { it }.keys.toList()
            if (activeFeatures.isNotEmpty()) {
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
                                    "admin_council_voting_control" to "Council Voting Control",
                                    "admin_student_messages_control" to "Student Messages",
                                    "admin_fee_reminder_control" to "Fees Control Panel",
                                    "admin_teacher_qr_control" to "Teacher and Staff Registration by QR Code",
                                    "admin_registered_teachers" to "Registered Teachers and Staff",
                                    "admin_teacher_messages" to "Teacher and Staff Messages",
                                    "admin_statistics_control" to "Dashboard Edit Control Panel",
                                    "admin_developer_options" to "Developer Options",
                                    "admin_teacher_review_criteria_control" to "Teacher Review Criteria Control",
                                    "admin_teacher_reviews" to "Teacher Reviews Monitor",
                                    "admin_management_review_criteria_control" to "Management Review Criteria Control",
                                    "admin_management_review_control" to "Management Reviews Monitor"
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
            }`;

file = file.replace(search2, replace2);

fs.writeFileSync('app/src/main/java/com/rajeducational/erp/ui/staff/StaffScreens.kt', file);
