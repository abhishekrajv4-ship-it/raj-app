const fs = require('fs');
let file = fs.readFileSync('app/src/main/java/com/rajeducational/erp/ui/teacher/TeacherScreens.kt', 'utf8');

const newCode = `    var pendingApprovalsCount by remember { mutableStateOf(0) }
    
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
                            pendingApprovalsCount = snapshot.documents.count { doc ->
                                val college = doc.getString("college") ?: ""
                                val course = doc.getString("course") ?: ""
                                val session = doc.getString("session") ?: ""
                                college == teacherCollege && teacherCourses.contains(course) && teacherYears.contains(session)
                            }
                        }
                    }
            } catch(e: Exception) {}
        }
    }`;

file = file.replace(
    `    LaunchedEffect(teacherId) {
        if (teacherId.isNotEmpty()) {
            com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("student_chats")`,
    newCode + `\n\n    LaunchedEffect(teacherId) {
        if (teacherId.isNotEmpty()) {
            com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("student_chats")`
);

const oldBottomBarBadge = `if (label == "More" && unreadCount > 0) {
                                Badge(containerColor = Color.Red) { Text(unreadCount.toString(), color = Color.White) }
                            }`;
                            
const newBottomBarBadge = `if (label == "More" && (unreadCount > 0 || pendingApprovalsCount > 0)) {
                                Badge(containerColor = Color.Red) { Text((unreadCount + pendingApprovalsCount).toString(), color = Color.White) }
                            }`;
file = file.replace(oldBottomBarBadge, newBottomBarBadge);

const oldMoreApprovals = `                        Text("Approvals", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.weight(1f))`;

const newMoreApprovals = `                        Row(verticalAlignment = Alignment.CenterVertically) {
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
                        Spacer(modifier = Modifier.weight(1f))`;
file = file.replace(oldMoreApprovals, newMoreApprovals);

fs.writeFileSync('app/src/main/java/com/rajeducational/erp/ui/teacher/TeacherScreens.kt', file);
