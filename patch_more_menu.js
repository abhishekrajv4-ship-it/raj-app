const fs = require('fs');
let file = fs.readFileSync('app/src/main/java/com/rajeducational/erp/ui/teacher/TeacherScreens.kt', 'utf8');

file = file.replace(/var secondsRemaining by remember \{ mutableIntStateOf\(15\) \}/, 
`var secondsRemaining by remember { mutableIntStateOf(15) }
    var expandedApprovals by remember { mutableStateOf(false) }
    var expandedAnnouncements by remember { mutableStateOf(false) }
    var adminFeatures by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(teacherId) {
        if (teacherId.isNotEmpty()) {
            val doc = com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("teachers").document(teacherId).get().kotlinx.coroutines.tasks.await()
            val features = doc.get("adminFeatures") as? List<String> ?: emptyList()
            adminFeatures = features
        }
    }
`);

const search1 = `Card(
                modifier = Modifier.fillMaxWidth().clickable { navController.navigate("teacher_attending_approvals") },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.HowToReg, "Attending Approvals", tint = AppColors.Teacher)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Attending Students Approval", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ChevronRight, "Go", tint = AppColors.TextSecondary)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth().clickable { navController.navigate("teacher_non_attending_approvals") },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.HowToReg, "Non-Attending Approvals", tint = AppColors.Teacher)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Non-Attending Students Approval", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ChevronRight, "Go", tint = AppColors.TextSecondary)
                }
            }`;

const replace1 = `Card(
                modifier = Modifier.fillMaxWidth().clickable { expandedApprovals = !expandedApprovals },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.HowToReg, "Approvals", tint = AppColors.Teacher)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Approvals", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(if (expandedApprovals) Icons.Default.ExpandLess else Icons.Default.ExpandMore, "Expand", tint = AppColors.TextSecondary)
                    }
                    if (expandedApprovals) {
                        Column(modifier = Modifier.padding(start = 56.dp, end = 16.dp, bottom = 16.dp)) {
                            Text(
                                "Attending Students Approval", 
                                modifier = Modifier.fillMaxWidth().clickable { navController.navigate("teacher_attending_approvals") }.padding(vertical = 12.dp),
                                fontSize = 15.sp, color = AppColors.Navy
                            )
                            Divider(color = Color.LightGray.copy(alpha = 0.5f))
                            Text(
                                "Non-Attending Students Approval", 
                                modifier = Modifier.fillMaxWidth().clickable { navController.navigate("teacher_non_attending_approvals") }.padding(vertical = 12.dp),
                                fontSize = 15.sp, color = AppColors.Navy
                            )
                        }
                    }
                }
            }`;

file = file.replace(search1, replace1);

const search2 = `Card(
                modifier = Modifier.fillMaxWidth().clickable { navController.navigate("teacher_send_announcement") },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Campaign, "Send Announcements (All)", tint = AppColors.Teacher)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Send Announcements (All)", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ChevronRight, "Go", tint = AppColors.TextSecondary)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth().clickable { navController.navigate("teacher_send_local_announcement") },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Campaign, "Send Announcements to My Students", tint = AppColors.Teacher)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Send Announcements to My Students", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ChevronRight, "Go", tint = AppColors.TextSecondary)
                }
            }`;

const replace2 = `Card(
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
            }`;

file = file.replace(search2, replace2);

const search3 = `Card(
                modifier = Modifier.fillMaxWidth().clickable { navController.navigate("admin_gallery_upload_center") },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PhotoLibrary, "Gallery Photo Upload Center", tint = AppColors.Teacher)
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
            Card(
                modifier = Modifier.fillMaxWidth().clickable { navController.navigate("teacher_attendance_control_center") },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FactCheck, "Attendance Control Center", tint = AppColors.Teacher)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Attendance Control Center", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ChevronRight, "Go", tint = AppColors.TextSecondary)
                }
            }`;

const replace3 = `var expandedAdminFeatures by remember { mutableStateOf(false) }
            if (adminFeatures.isNotEmpty()) {
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
                                adminFeatures.forEachIndexed { index, feature ->
                                    val route = when (feature) {
                                        "Gallery Upload" -> "admin_gallery_upload_center"
                                        "Event Control" -> "admin_event_control_center"
                                        "Attendance Control" -> "teacher_attendance_control_center"
                                        "Announcements" -> "admin_announcement_control"
                                        "Fee Management" -> "admin_fee_management"
                                        "Time Table" -> "admin_time_table"
                                        "Exam Management" -> "admin_exam_management"
                                        "Staff Control" -> "admin_staff_control"
                                        "Teacher Control" -> "admin_teacher_control"
                                        "Student Control" -> "admin_student_control"
                                        "Voting Center" -> "admin_voting_center"
                                        else -> ""
                                    }
                                    if (route.isNotEmpty()) {
                                        Text(
                                            feature, 
                                            modifier = Modifier.fillMaxWidth().clickable { navController.navigate(route) }.padding(vertical = 12.dp),
                                            fontSize = 15.sp, color = AppColors.Navy
                                        )
                                        if (index < adminFeatures.size - 1) {
                                            Divider(color = Color.LightGray.copy(alpha = 0.5f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }`;

file = file.replace(search3, replace3);

fs.writeFileSync('app/src/main/java/com/rajeducational/erp/ui/teacher/TeacherScreens.kt', file);
