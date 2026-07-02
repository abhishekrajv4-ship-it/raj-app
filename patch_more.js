const fs = require('fs');

let tFile = fs.readFileSync('app/src/main/java/com/rajeducational/erp/ui/teacher/TeacherScreens.kt', 'utf8');

const tStr = `    var secondsRemaining by remember { mutableIntStateOf(15) }`;
const tRep = `    var secondsRemaining by remember { mutableIntStateOf(15) }
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
    }`;

// Only replace the SECOND occurrence (which is inside TeacherMoreScreen)
let parts = tFile.split(tStr);
if (parts.length >= 3) {
    tFile = parts[0] + tStr + parts[1] + tRep + parts.slice(2).join(tStr);
}

fs.writeFileSync('app/src/main/java/com/rajeducational/erp/ui/teacher/TeacherScreens.kt', tFile);


let sFile = fs.readFileSync('app/src/main/java/com/rajeducational/erp/ui/staff/StaffScreens.kt', 'utf8');

const sRep = `    var secondsRemaining by remember { mutableIntStateOf(15) }
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
    }`;

let sParts = sFile.split(tStr);
if (sParts.length >= 3) {
    sFile = sParts[0] + tStr + sParts[1] + sRep + sParts.slice(2).join(tStr);
}

fs.writeFileSync('app/src/main/java/com/rajeducational/erp/ui/staff/StaffScreens.kt', sFile);
