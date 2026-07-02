const fs = require('fs');
let file = fs.readFileSync('app/src/main/java/com/rajeducational/erp/ui/teacher/TeacherScreens.kt', 'utf8');

// Remove from TeacherDashboardScreen
file = file.replace(/var secondsRemaining by remember \{ mutableIntStateOf\(15\) \}\n    var expandedApprovals by remember \{ mutableStateOf\(false\) \}\n    var expandedAnnouncements by remember \{ mutableStateOf\(false\) \}\n    var adminFeatures by remember \{ mutableStateOf<Map<String, Boolean>>\(emptyMap\(\)\) \}\n\n    LaunchedEffect\(teacherId\) \{\n        if \(teacherId.isNotEmpty\(\)\) \{\n            val doc = com\.google\.firebase\.firestore\.FirebaseFirestore\.getInstance\(\)\.collection\("teachers"\)\.document\(teacherId\)\.get\(\)\.kotlinx\.coroutines\.tasks\.await\(\)\n            val features = doc\.get\("adminFeatures"\) as\? Map<String, Boolean> \?: emptyMap\(\)\n            adminFeatures = features\n        \}\n    \}/, 
`var secondsRemaining by remember { mutableIntStateOf(15) }`);

// Add to TeacherMoreScreen
const searchMoreScreen = `var secondsRemaining by remember { mutableIntStateOf(15) }
    LaunchedEffect(showQrDialog, teacherId) {`;

const replaceMoreScreen = `var secondsRemaining by remember { mutableIntStateOf(15) }
    var expandedApprovals by remember { mutableStateOf(false) }
    var expandedAnnouncements by remember { mutableStateOf(false) }
    var expandedAdminFeatures by remember { mutableStateOf(false) }
    var adminFeatures by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }

    LaunchedEffect(teacherId) {
        if (teacherId.isNotEmpty()) {
            try {
                val doc = kotlinx.coroutines.tasks.await(com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("teachers").document(teacherId).get())
                val features = doc.get("adminFeatures") as? Map<String, Boolean> ?: emptyMap()
                adminFeatures = features
            } catch(e: Exception) {}
        }
    }

    LaunchedEffect(showQrDialog, teacherId) {`;

file = file.replace(searchMoreScreen, replaceMoreScreen);

fs.writeFileSync('app/src/main/java/com/rajeducational/erp/ui/teacher/TeacherScreens.kt', file);
