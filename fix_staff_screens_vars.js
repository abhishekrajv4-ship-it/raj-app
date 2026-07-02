const fs = require('fs');
let file = fs.readFileSync('app/src/main/java/com/rajeducational/erp/ui/staff/StaffScreens.kt', 'utf8');

file = file.replace(/var secondsRemaining by remember \{ mutableIntStateOf\(15\) \}\n    var expandedAdminFeatures by remember \{ mutableStateOf\(false\) \}\n    var adminFeatures by remember \{ mutableStateOf<Map<String, Boolean>>\(emptyMap\(\)\) \}\n\n    LaunchedEffect\(staffId\) \{\n        if \(staffId.isNotEmpty\(\)\) \{\n            try \{\n                val doc = kotlinx\.coroutines\.tasks\.await\(com\.google\.firebase\.firestore\.FirebaseFirestore\.getInstance\(\)\.collection\("staffs"\)\.document\(staffId\)\.get\(\)\)\n                val features = doc\.get\("adminFeatures"\) as\? Map<String, Boolean> \?: emptyMap\(\)\n                adminFeatures = features\n            \} catch\(e: Exception\) \{\}\n        \}\n    \}/, 
`var secondsRemaining by remember { mutableIntStateOf(15) }`);

const searchMoreScreen = `var secondsRemaining by remember { mutableIntStateOf(15) }
    LaunchedEffect(showQrDialog, staffId) {`;

const replaceMoreScreen = `var secondsRemaining by remember { mutableIntStateOf(15) }
    var expandedAdminFeatures by remember { mutableStateOf(false) }
    var adminFeatures by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }

    LaunchedEffect(staffId) {
        if (staffId.isNotEmpty()) {
            try {
                val doc = kotlinx.coroutines.tasks.await(com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("staffs").document(staffId).get())
                val features = doc.get("adminFeatures") as? Map<String, Boolean> ?: emptyMap()
                adminFeatures = features
            } catch(e: Exception) {}
        }
    }

    LaunchedEffect(showQrDialog, staffId) {`;

file = file.replace(searchMoreScreen, replaceMoreScreen);

fs.writeFileSync('app/src/main/java/com/rajeducational/erp/ui/staff/StaffScreens.kt', file);
