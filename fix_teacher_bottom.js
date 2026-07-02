const fs = require('fs');
let file = fs.readFileSync('app/src/main/java/com/rajeducational/erp/ui/teacher/TeacherScreens.kt', 'utf8');

const targetInsert = `fun TeacherBottomNavigationBar(navController: NavController, currentRoute: String) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = context.getSharedPreferences("TeacherPrefs", android.content.Context.MODE_PRIVATE)
    val teacherId = prefs.getString("teacher_id", "") ?: ""
    var unreadCount by remember { mutableStateOf(0) }`;

const replacementInsert = `fun TeacherBottomNavigationBar(navController: NavController, currentRoute: String) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = context.getSharedPreferences("TeacherPrefs", android.content.Context.MODE_PRIVATE)
    val teacherId = prefs.getString("teacher_id", "") ?: ""
    var unreadCount by remember { mutableStateOf(0) }
    var pendingApprovalsCount by remember { mutableStateOf(0) }
    
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

file = file.replace(targetInsert, replacementInsert);
fs.writeFileSync('app/src/main/java/com/rajeducational/erp/ui/teacher/TeacherScreens.kt', file);
