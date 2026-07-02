const fs = require('fs');
let file = fs.readFileSync('app/src/main/java/com/rajeducational/erp/ui/teacher/TeacherScreens.kt', 'utf8');

// remove from line 343
const wrongBlock2 = `    var pendingApprovalsCount by remember { mutableStateOf(0) }
    
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
    }

`;
file = file.replace(wrongBlock2, ``);

// Insert into TeacherMoreScreen properly
const targetInsert = `    var adminFeatures by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }`;
const replacementInsert = `    var adminFeatures by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
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

// Replace the LAST occurrence of targetInsert to hit TeacherMoreScreen
const lastIndex = file.lastIndexOf(targetInsert);
if (lastIndex !== -1) {
    file = file.substring(0, lastIndex) + replacementInsert + file.substring(lastIndex + targetInsert.length);
    fs.writeFileSync('app/src/main/java/com/rajeducational/erp/ui/teacher/TeacherScreens.kt', file);
    console.log("TeacherMoreScreen pendingApprovalsCount fixed");
} else {
    console.log("targetInsert not found");
}

