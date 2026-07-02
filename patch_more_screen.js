const fs = require('fs');
let file = fs.readFileSync('app/src/main/java/com/rajeducational/erp/ui/teacher/TeacherScreens.kt', 'utf8');

file = file.replace(/var secondsRemaining by remember \{ mutableIntStateOf\(15\) \}\s*LaunchedEffect\(showQrDialog, teacherId\) \{/, 
`var secondsRemaining by remember { mutableIntStateOf(15) }
    var expandedApprovals by remember { mutableStateOf(false) }
    var expandedAnnouncements by remember { mutableStateOf(false) }
    var expandedAdminFeatures by remember { mutableStateOf(false) }
    var adminFeatures by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }

    LaunchedEffect(teacherId) {
        if (teacherId.isNotEmpty()) {
            try {
                val doc = com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("teachers").document(teacherId).get().await()
                val features = doc.get("adminFeatures") as? Map<String, Boolean> ?: emptyMap()
                adminFeatures = features
            } catch(e: Exception) {}
        }
    }

    LaunchedEffect(showQrDialog, teacherId) {`);

fs.writeFileSync('app/src/main/java/com/rajeducational/erp/ui/teacher/TeacherScreens.kt', file);

let staffFile = fs.readFileSync('app/src/main/java/com/rajeducational/erp/ui/staff/StaffScreens.kt', 'utf8');
staffFile = staffFile.replace(/var secondsRemaining by remember \{ mutableIntStateOf\(15\) \}\s*LaunchedEffect\(showQrDialog, staffId\) \{/, 
`var secondsRemaining by remember { mutableIntStateOf(15) }
    var expandedAdminFeatures by remember { mutableStateOf(false) }
    var adminFeatures by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }

    LaunchedEffect(staffId) {
        if (staffId.isNotEmpty()) {
            try {
                val doc = com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("staffs").document(staffId).get().await()
                val features = doc.get("adminFeatures") as? Map<String, Boolean> ?: emptyMap()
                adminFeatures = features
            } catch(e: Exception) {}
        }
    }

    LaunchedEffect(showQrDialog, staffId) {`);
fs.writeFileSync('app/src/main/java/com/rajeducational/erp/ui/staff/StaffScreens.kt', staffFile);
